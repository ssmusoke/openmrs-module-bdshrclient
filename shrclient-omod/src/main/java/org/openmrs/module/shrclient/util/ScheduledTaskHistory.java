package org.openmrs.module.shrclient.util;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

//purpose: represents run history of scheduled tasks
public class ScheduledTaskHistory {
    private final Logger logger = Logger.getLogger(ScheduledTaskHistory.class);

    public static final String QUERY_FORMAT_TO_GET_UPDATED_SINCE = "select feed_uri_for_last_read_entry from markers where feed_uri = '%s'";
    public static final String QUERY_FORMAT_TO_SET_UPDATED_SINCE = "update markers set feed_uri_for_last_read_entry = '%s' where feed_uri = '%s'";
    public static final String QUERY_FORMAT_TO_GET_OFFSET = "select last_read_entry_id from markers where feed_uri = '%s'";
    public static final String QUERY_FORMAT_TO_SET_OFFSET = "update markers set last_read_entry_id = %d where feed_uri = '%s'";
    private Database database;


    public ScheduledTaskHistory(Database database) {
        this.database = database;
    }

    public String getUpdatedSinceDateAndTime(String levelName) {
        String query = String.format(QUERY_FORMAT_TO_GET_UPDATED_SINCE, levelName);
        ResultSet resultSet = database.get(query);
        String updatedSinceDataAndTime = null;
        try {
            updatedSinceDataAndTime = resultSet.next() ? resultSet.getString(1) : StringUtils.EMPTY;
        } catch (SQLException e) {
            logger.error("Error while fetching Last Execution Date And Time");
            e.printStackTrace();
        } finally {
            try {
                Statement statement = resultSet.getStatement();
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or resultSet ", e);
                e.printStackTrace();
            }
        }
        return updatedSinceDataAndTime;
    }

    public int getOffset(String levelName) {
        String query = String.format(QUERY_FORMAT_TO_GET_OFFSET, levelName);
        ResultSet resultSet = database.get(query);
        int offset = 0;
        try {
            offset = resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            logger.error("Error while fetching Offset");
            e.printStackTrace();
        } finally {
            try {
                Statement statement = resultSet.getStatement();
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or resultSet ", e);
                e.printStackTrace();
            }
        }
        return offset;
    }

    public boolean setOffset(String level, int offset) {
        String query = String.format(QUERY_FORMAT_TO_SET_OFFSET, offset, level);
        return database.save(query);
    }

    public boolean setUpdatedSinceDateAndTime(String levelName) {
        String query = String.format(QUERY_FORMAT_TO_SET_UPDATED_SINCE, getCurrentDateAndTime(), levelName);
        return database.save(query);
    }

    private String getCurrentDateAndTime() {
        String date = new Timestamp(new Date().getTime()).toString();
        int indexOfPeriod = date.indexOf(".");
        return indexOfPeriod != -1 ? date.substring(0, indexOfPeriod) : date;
    }
}
