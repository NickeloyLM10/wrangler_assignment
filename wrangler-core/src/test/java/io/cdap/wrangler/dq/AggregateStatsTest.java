package io.cdap.wrangler.dq;

import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.TestingRig;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for the AggregateStats directive.
 * This class tests various scenarios for aggregating byte sizes and time durations.
 */

//@ExtendWith(TestingRig.class)
public class AggregateStatsTest {

    @Test
    public void testBasicAggregation() throws Exception {
        // Create test data
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", "10KB");
        row1.add("response_time", "150ms");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "20KB");
        row2.add("response_time", "250ms");
        rows.add(row2);

        Row row3 = new Row();
        row3.add("data_transfer_size", "1.5MB");
        row3.add("response_time", "1.2s");
        rows.add(row3);

        // Define the recipe
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Calculate expected values
        // 10KB + 20KB + 1.5MB = 30KB + 1.5MB = 1566720 bytes = 1.4941 MB
        // 150ms + 250ms + 1.2s = 400ms + 1.2s = 1600ms = 1.6 seconds
        double expectedTotalSizeInMB = 1.4941;
        double expectedTotalTimeInSeconds = 1.6;

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInMB, (double)results.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, (double)results.get(0).getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testAverageAggregation() throws Exception {
        // Create test data
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", "10KB");
        row1.add("response_time", "150ms");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "20KB");
        row2.add("response_time", "250ms");
        rows.add(row2);

        Row row3 = new Row();
        row3.add("data_transfer_size", "1.5MB");
        row3.add("response_time", "1.2s");
        rows.add(row3);

        // Define the recipe with average aggregation
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time avg_size_mb avg_time_sec 'MB' 's' 'average'"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Calculate expected values
        // Average of 10KB, 20KB, 1.5MB = 1566720/3 bytes = 0.4980 MB
        // Average of 150ms, 250ms, 1.2s = 1600/3 ms = 0.5333 seconds
        double expectedAvgSizeInMB = 0.4980;
        double expectedAvgTimeInSeconds = 0.5333;

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedAvgSizeInMB, (double)results.get(0).getValue("avg_size_mb"), 0.001);
        Assert.assertEquals(expectedAvgTimeInSeconds, (double)results.get(0).getValue("avg_time_sec"), 0.001);
    }

    @Test
    public void testDifferentOutputUnits() throws Exception {
        // Create test data
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", "10KB");
        row1.add("response_time", "150ms");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "20KB");
        row2.add("response_time", "250ms");
        rows.add(row2);

        Row row3 = new Row();
        row3.add("data_transfer_size", "1.5MB");
        row3.add("response_time", "1.2s");
        rows.add(row3);

        // Define the recipe with different output units
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_kb total_time_ms 'KB' 'ms'"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Calculate expected values
        // 10KB + 20KB + 1.5MB = 30KB + 1.5MB = 1566720 bytes = 1530 KB
        // 150ms + 250ms + 1.2s = 400ms + 1.2s = 1600ms
        double expectedTotalSizeInKB = 1530.0;
        double expectedTotalTimeInMS = 1600.0;

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInKB, (double)results.get(0).getValue("total_size_kb"), 0.1);
        Assert.assertEquals(expectedTotalTimeInMS, (double)results.get(0).getValue("total_time_ms"), 0.1);
    }

    @Test
    public void testEmptyInput() throws Exception {
        // Create empty test data
        List<Row> rows = new ArrayList<>();

        // Define the recipe
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Expected values for empty input
        double expectedTotalSizeInMB = 0.0;
        double expectedTotalTimeInSeconds = 0.0;

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInMB, (double)results.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, (double)results.get(0).getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testMixedUnits() throws Exception {
        // Create test data with mixed units
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", "10KB");
        row1.add("response_time", "150ms");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "20KB");
        row2.add("response_time", "0.25s"); // Same as 250ms but different unit
        rows.add(row2);

        Row row3 = new Row();
        row3.add("data_transfer_size", "0.0015GB"); // Same as 1.5MB but different unit
        row3.add("response_time", "1.2s");
        rows.add(row3);

        // Define the recipe
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Calculate expected values (same as first test)
        double expectedTotalSizeInMB = 1.4941;
        double expectedTotalTimeInSeconds = 1.6;

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInMB, (double)results.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, (double)results.get(0).getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testInvalidInputHandling() throws Exception {
        // Create test data with invalid inputs
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", "invalid");
        row1.add("response_time", "invalid");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "20KB");
        row2.add("response_time", "250ms");
        rows.add(row2);

        // Define the recipe
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Only valid values should be aggregated
        double expectedTotalSizeInMB = 0.0195; // 20KB = 20480 bytes = 0.0195 MB
        double expectedTotalTimeInSeconds = 0.25; // 250ms = 0.25 seconds

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInMB, (double)results.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, (double)results.get(0).getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testLargeValues() throws Exception {
        // Create test data with large values
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", "1TB");
        row1.add("response_time", "1h");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "2TB");
        row2.add("response_time", "2h");
        rows.add(row2);

        // Define the recipe with TB and hour units
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_tb total_time_h 'TB' 'h'"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Calculate expected values
        double expectedTotalSizeInTB = 3.0; // 1TB + 2TB = 3TB
        double expectedTotalTimeInHours = 3.0; // 1h + 2h = 3h

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInTB, (double)results.get(0).getValue("total_size_tb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInHours, (double)results.get(0).getValue("total_time_h"), 0.001);
    }

    @Test
    public void testNullValues() throws Exception {
        // Create test data with null values
        List<Row> rows = new ArrayList<>();

        Row row1 = new Row();
        row1.add("data_transfer_size", null);
        row1.add("response_time", "150ms");
        rows.add(row1);

        Row row2 = new Row();
        row2.add("data_transfer_size", "20KB");
        row2.add("response_time", null);
        rows.add(row2);

        Row row3 = new Row();
        row3.add("data_transfer_size", "30KB");
        row3.add("response_time", "250ms");
        rows.add(row3);

        // Define the recipe
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, rows);

        // Calculate expected values (only non-null values should be aggregated)
        double expectedTotalSizeInMB = 0.0488; // 20KB + 30KB = 50KB = 51200 bytes = 0.0488 MB
        double expectedTotalTimeInSeconds = 0.4; // 150ms + 250ms = 400ms = 0.4 seconds

        // Assert results
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(expectedTotalSizeInMB, (double)results.get(0).getValue("total_size_mb"), 0.001);
        Assert.assertEquals(expectedTotalTimeInSeconds, (double)results.get(0).getValue("total_time_sec"), 0.001);
    }

}
