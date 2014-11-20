package org.openmrs.module.shrclient.util;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
    private final Logger logger = Logger.getLogger(Database.class);

    private Connection connection;

    public Database(Connection connection) {
        this.connection = connection;
    }

    public ResultSet get(String query) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();
            return resultSet;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while executing " + query + " : ", e);
        }
//
//        finally {
//            try {
//                if (resultSet != null) resultSet.close();
//                if (statement != null) statement.close();
//            } catch (SQLException e) {
//                logger.warn("Could not close db statement ", e);
//            }
//        }
    }

    public boolean save(String query) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            return !statement.execute();

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while executing " + query + " : ", e);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement ", e);
            }
        }
    }
}
