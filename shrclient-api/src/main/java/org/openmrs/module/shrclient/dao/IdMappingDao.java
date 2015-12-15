package org.openmrs.module.shrclient.dao;


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class IdMappingDao {

    protected Logger logger;
    protected Database database;

    public IdMappingDao(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public abstract String getMappingTable();
    public abstract String getInsertMappingSql();
    public abstract String getUpdateMappingSql();
    public abstract String getFetchByExternalIdSql();
    public abstract String getFetchByInternalIdSql();
    public abstract PreparedStatement getInsertIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException;
    public abstract PreparedStatement getUpdateIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException;
    public abstract IdMapping buildIdMappingByExternalId(ResultSet resultSet, String externalId) throws SQLException;
    public abstract IdMapping buildIdMappingByInternalId(ResultSet resultSet, String internalId) throws SQLException;

    protected boolean mappingExists(final IdMapping idMapping) {
        return database.executeInTransaction(new Database.TxWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) {
                String query = getMappingExistsQuery();
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                boolean result = false;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, idMapping.getInternalId());
                    statement.setString(2, idMapping.getExternalId());
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            result = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error occurred while querying id mapping", e);
                } finally {
                    try {
                        if (resultSet != null) resultSet.close();
                        if (statement != null) statement.close();
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or result set", e);
                    }
                }
                return result;
            }
        });
    }

    protected void saveOrUpdateIdMapping(final IdMapping idMapping) {
        database.executeInTransaction(new Database.TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                PreparedStatement statement = null;
                try{
                    if (!mappingExists(idMapping)) {
                        statement = getInsertIdMappingStatement(connection, idMapping);
                        statement.execute();
                    }else {
                        statement = getUpdateIdMappingStatement(connection, idMapping);
                        statement.executeUpdate();
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Error occurred while creating id mapping", e);
                } finally {
                    try {
                        if (statement != null) statement.close();
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or resultset", e);
                    }
                }
                return null;
            }

        });
    }

    protected IdMapping findByExternalId(final String externalId) {
        return database.executeInTransaction(new Database.TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(getFetchByExternalIdSql());
                    statement.setString(1, externalId);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            result = buildIdMappingByExternalId(resultSet,externalId) ;
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error occurred while querying id mapping", e);
                } finally {
                    try {
                        if (resultSet != null) resultSet.close();
                        if (statement != null) statement.close();
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or result set", e);
                    }
                }
                return result;
            }
        });
    }

    protected IdMapping findByInternalId(final String internalId) {
        return database.executeInTransaction(new Database.TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping idMapping = null;
                try {
                    statement = connection.prepareStatement(getFetchByInternalIdSql());
                    statement.setString(1, internalId);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            idMapping = buildIdMappingByInternalId(resultSet, internalId) ;
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error occurred while querying id mapping", e);
                } finally {
                    try {
                        if (resultSet != null) resultSet.close();
                        if (statement != null) statement.close();
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or encounterIdMapping set", e);
                    }
                }
                return idMapping;
            }
        });
    }

    private String getMappingExistsQuery() {
        return String.format("select distinct map.internal_id from %s map where map.internal_id=? and map.external_id=?", getMappingTable());
    }

}
