/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.dq;
import io.cdap.wrangler.api.TransientVariableScope;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A directive for aggregating statistics on byte sizes and time durations.
 *
 * This directive processes columns containing byte sizes (KB, MB, GB, TB) and
 * time durations (ms, s, m, h) and calculates aggregated statistics.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates statistics on byte sizes and time durations")
public class AggregateStats implements Directive {
    private String byteSizeColumn;
    private String timeDurationColumn;
    private String totalSizeColumn;
    private String totalTimeColumn;
    private String sizeOutputUnit;
    private String timeOutputUnit;
    private String aggregationType;

    // Flag to track if this is the first batch
    private boolean isFirstBatch = true;

    /**
     * Defines the usage and arguments for the aggregate-stats directive.
     */
    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        builder.define("sizeOutputUnit", TokenType.TEXT, true);
        builder.define("timeOutputUnit", TokenType.TEXT, true);
        builder.define("aggregationType", TokenType.TEXT, true);
        return builder.build();
    }

    /**
     * Initializes the directive with the provided arguments.
     */
    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.byteSizeColumn = ((ColumnName) args.value("byteSizeColumn")).value();
        this.timeDurationColumn = ((ColumnName) args.value("timeDurationColumn")).value();
        this.totalSizeColumn = ((ColumnName) args.value("totalSizeColumn")).value();
        this.totalTimeColumn = ((ColumnName) args.value("totalTimeColumn")).value();

        // Handle optional arguments with defaults
        if (args.contains("sizeOutputUnit")) {
            this.sizeOutputUnit = ((Text) args.value("sizeOutputUnit")).value();
        } else {
            this.sizeOutputUnit = "MB"; // Default to MB
        }

        if (args.contains("timeOutputUnit")) {
            this.timeOutputUnit = ((Text) args.value("timeOutputUnit")).value();
        } else {
            this.timeOutputUnit = "s"; // Default to seconds
        }

        if (args.contains("aggregationType")) {
            this.aggregationType = ((Text) args.value("aggregationType")).value();
        } else {
            this.aggregationType = "total"; // Default to total
        }

        // Validate output units
        if (!isValidSizeUnit(sizeOutputUnit)) {
            throw new DirectiveParseException(
                    String.format("Invalid size output unit '%s'. Valid units are: B, KB, MB, GB, TB", sizeOutputUnit)
            );
        }

        if (!isValidTimeUnit(timeOutputUnit)) {
            throw new DirectiveParseException(
                    String.format("Invalid time output unit '%s'. Valid units are: ms, s, m, h", timeOutputUnit)
            );
        }

        // Validate aggregation type
        if (!isValidAggregationType(aggregationType)) {
            throw new DirectiveParseException(
                    String.format("Invalid aggregation type '%s'. Valid types are: total, average", aggregationType)
            );
        }
    }

    /**
     * Executes the directive on the provided rows.
     */
    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        // Use context.getTransientStore() to accumulate totals
        String storeKey = "aggregate-stats";
        AggregateStore store = (AggregateStore) context.getTransientStore().get(storeKey);
        if (store == null) {
            store = new AggregateStore();
            context.getTransientStore().set(TransientVariableScope.GLOBAL, storeKey, store);
        }

        // Process each row, extract byte sizes and time durations, add to totals
        for (Row row : rows) {
            Object byteSizeObj = row.getValue(byteSizeColumn);
            Object timeDurationObj = row.getValue(timeDurationColumn);

            if (byteSizeObj != null) {
                long bytes = 0;
                if (byteSizeObj instanceof ByteSize) {
                    bytes = ((ByteSize) byteSizeObj).value();
                } else if (byteSizeObj instanceof String) {
                    bytes = new ByteSize((String) byteSizeObj).value();
                } else {
                    // Try to convert to string and parse
                    try {
                        bytes = new ByteSize(byteSizeObj.toString()).value();
                    } catch (Exception e) {
                        // Skip invalid values
                    }
                }
                store.addBytes(bytes);
            }

            if (timeDurationObj != null) {
                long millis = 0;
                if (timeDurationObj instanceof TimeDuration) {
                    millis = ((TimeDuration) timeDurationObj).value();
                } else if (timeDurationObj instanceof String) {
                    millis = new TimeDuration((String) timeDurationObj).value();
                } else {
                    // Try to convert to string and parse
                    try {
                        millis = new TimeDuration(timeDurationObj.toString()).value();
                    } catch (Exception e) {
                        // Skip invalid values
                    }
                }
                store.addMillis(millis);
            }

            store.incrementRowCount();
        }

        // Check if we should output results
        // we'll output results with every batch
        // The downstream system will need to handle the continuously updated results

        // Create a new row with the current aggregated values
        Row result = new Row();

        // Convert to appropriate output units based on aggregation type
        double sizeValue;
        double timeValue;

        if ("average".equalsIgnoreCase(aggregationType) && store.getRowCount() > 0) {
            sizeValue = convertBytes(store.getTotalBytes() / (double) store.getRowCount(), sizeOutputUnit);
            timeValue = convertMillis(store.getTotalMillis() / (double) store.getRowCount(), timeOutputUnit);
        } else {
            sizeValue = convertBytes(store.getTotalBytes(), sizeOutputUnit);
            timeValue = convertMillis(store.getTotalMillis(), timeOutputUnit);
        }

        result.add(totalSizeColumn, sizeValue);
        result.add(totalTimeColumn, timeValue);

        // For the first batch, we'll return an empty list to avoid outputting partial results
        // For subsequent batches, we'll return the current aggregation
        if (isFirstBatch) {
            isFirstBatch = false;
            return new ArrayList<>();
        } else {
            return Collections.singletonList(result);
        }
    }

    /**
     * Cleanup method called when the directive is no longer needed.
     */
    @Override
    public void destroy() {
        // No resources to clean up
    }

    /**
     * Converts bytes to the specified output unit.
     */
    private double convertBytes(double bytes, String outputUnit) {
        switch (outputUnit.toUpperCase()) {
            case "B":
                return bytes;
            case "KB":
                return bytes / 1024.0;
            case "MB":
                return bytes / (1024.0 * 1024.0);
            case "GB":
                return bytes / (1024.0 * 1024.0 * 1024.0);
            case "TB":
                return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
            default:
                return bytes;
        }
    }

    /**
     * Converts milliseconds to the specified output unit.
     */
    private double convertMillis(double millis, String outputUnit) {
        switch (outputUnit.toLowerCase()) {
            case "ms":
                return millis;
            case "s":
                return millis / 1000.0;
            case "m":
                return millis / (60.0 * 1000.0);
            case "h":
                return millis / (60.0 * 60.0 * 1000.0);
            default:
                return millis;
        }
    }

    /**
     * Validates if the provided size unit is supported.
     */
    private boolean isValidSizeUnit(String unit) {
        return "B".equalsIgnoreCase(unit) ||
                "KB".equalsIgnoreCase(unit) ||
                "MB".equalsIgnoreCase(unit) ||
                "GB".equalsIgnoreCase(unit) ||
                "TB".equalsIgnoreCase(unit);
    }

    /**
     * Validates if the provided time unit is supported.
     */
    private boolean isValidTimeUnit(String unit) {
        return "ms".equalsIgnoreCase(unit) ||
                "s".equalsIgnoreCase(unit) ||
                "m".equalsIgnoreCase(unit) ||
                "h".equalsIgnoreCase(unit);
    }

    /**
     * Validates if the provided aggregation type is supported.
     */
    private boolean isValidAggregationType(String type) {
        return "total".equalsIgnoreCase(type) ||
                "average".equalsIgnoreCase(type);
    }

    /**
     * Helper class to store aggregated values across batches.
     */
    private static class AggregateStore {
        private long totalBytes = 0;
        private long totalMillis = 0;
        private long rowCount = 0;

        public void addBytes(long bytes) {
            totalBytes += bytes;
        }

        public void addMillis(long millis) {
            totalMillis += millis;
        }

        public void incrementRowCount() {
            rowCount++;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public long getTotalMillis() {
            return totalMillis;
        }

        public long getRowCount() {
            return rowCount;
        }
    }
}
