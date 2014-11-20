package org.openmrs.module.shrclient.util;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//purpose: represents run history of scheduled tasks
public class ScheduledTaskHistory {
    private final Logger logger = Logger.getLogger(ScheduledTaskHistory.class);

    public static final String QUERY_FORMAT_TO_GET_LAST_EXECUTION_TIME = "select last_execution_time from scheduler_task_config where name = '%s'";
    public static final String QUERY_FORMAT_TO_GET_OFFSET = "select feed_uri_for_last_read_entry from markers where feed_uri = '%s' and last_read_entry_id = '%s'";
    public static final String QUERY_FORMAT_TO_SET_OFFSET = "update markers set feed_uri_for_last_read_entry = %d where feed_uri = '%s' and last_read_entry_id = '%s'";
    public static final int GARBAGE_CHARACTER_LENGTH_IN_DATETIME_FIELD = 2;
    private Database database;


    public ScheduledTaskHistory(Database database) {
        this.database = database;
    }

    public String getLastExecutionDateAndTime(String taskName) {
        String query = String.format(QUERY_FORMAT_TO_GET_LAST_EXECUTION_TIME, taskName);
        ResultSet resultSet = database.get(query);
        String lastExecutionTime = null;
        try {
            lastExecutionTime = resultSet.next() ? resultSet.getString(1) : StringUtils.EMPTY;
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
        return removeUnwantedCharactersAtTheEnd(lastExecutionTime);
    }

    private String removeUnwantedCharactersAtTheEnd(String lastExecutionTime) {
        return lastExecutionTime == null ?
                StringUtils.EMPTY :
                lastExecutionTime.substring(0, lastExecutionTime.length() - GARBAGE_CHARACTER_LENGTH_IN_DATETIME_FIELD);
    }

    public int getOffset(String levelName, String taskName) {
        String query = String.format(QUERY_FORMAT_TO_GET_OFFSET, levelName, taskName);
        ResultSet resultSet = database.get(query);
        int offset = 0;
        try {
            offset = resultSet.next() ? getInteger(resultSet) : 0;
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

    private int getInteger(ResultSet resultSet) throws SQLException {
        String result = resultSet.getString(1);
        return StringUtils.isBlank(result) ? 0 : Integer.parseInt(result);
    }

    public boolean setOffset(String level, String taskName, int offset) {
        String query = String.format(QUERY_FORMAT_TO_SET_OFFSET, offset, level, taskName);
        return database.save(query);
    }
}
