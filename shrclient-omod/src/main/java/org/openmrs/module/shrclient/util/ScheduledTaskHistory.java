package org.openmrs.module.shrclient.util;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.util.DatabaseUpdater;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//purpose: represents run history of scheduled tasks
public class ScheduledTaskHistory {
    private final Logger logger = Logger.getLogger(ScheduledTaskHistory.class);
    public static final int GARBAGE_CHARACTER_LENGTH_IN_DATETIME_FIELD = 2;

    public String getLastExecutionDateAndTime(String taskName) {
        String query = "select last_execution_time from scheduler_task_config where name = ?";
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String lastExecutionTime;
        try {
            conn = DatabaseUpdater.getConnection();
            statement = conn.prepareStatement(query);
            statement.setString(1, taskName);
            resultSet = statement.executeQuery();
            lastExecutionTime = resultSet.next() ? resultSet.getString(1) : StringUtils.EMPTY;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while querying scheduler_task_config : ", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or resultset", e);
            }
        }
        return removeUnwantedCharactersAtTheEnd(lastExecutionTime);
    }

    private String removeUnwantedCharactersAtTheEnd(String lastExecutionTime) {
        return lastExecutionTime == null ?
                StringUtils.EMPTY
                :
                lastExecutionTime.substring(0, lastExecutionTime.length() - GARBAGE_CHARACTER_LENGTH_IN_DATETIME_FIELD);
    }
}
