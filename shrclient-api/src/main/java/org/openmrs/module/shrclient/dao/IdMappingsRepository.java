package org.openmrs.module.shrclient.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.openmrs.module.shrclient.util.Database.TxWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.Date;

@Component("bdShrClientIdMappingRepository")
public class IdMappingsRepository {

    private Logger logger = Logger.getLogger(IdMappingsRepository.class);

    @Autowired
    private Database database;

    public void saveOrUpdateMapping(final IdMapping idMapping) {
        database.executeInTransaction(new TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                Timestamp lastSyncedDateTime = getLastSyncDateTime(idMapping);
                Timestamp serverUpdateDateTime = getServerUpdateDateTime(idMapping);
                if (!mappingExists(idMapping)) {
                    String query = "insert into shr_id_mapping (internal_id, external_id, type, uri, last_sync_datetime, server_update_datetime) values (?,?,?,?,?,?)";

                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement(query);
                        statement.setString(1, idMapping.getInternalId());
                        statement.setString(2, idMapping.getExternalId());
                        statement.setString(3, idMapping.getType());
                        statement.setString(4, idMapping.getUri());
                        statement.setTimestamp(5, lastSyncedDateTime);
                        statement.setTimestamp(6, serverUpdateDateTime);
                        statement.execute();
                    } catch (Exception e) {
                        throw new RuntimeException("Error occurred while creating id mapping", e);
                    } finally {
                        try {
                            if (statement != null) statement.close();
                        } catch (SQLException e) {
                            logger.warn("Could not close db statement or resultset", e);
                        }
                    }
                } else {
                    String updateQuery = "update shr_id_mapping set last_sync_datetime = ?, server_update_datetime = ? where internal_id = ?";
                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement(updateQuery);
                        statement.setTimestamp(1, lastSyncedDateTime);
                        statement.setTimestamp(2, serverUpdateDateTime);
                        statement.setString(3, idMapping.getInternalId());
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException("Error occurred while creating id mapping", e);
                    } finally {
                        try {
                            if (statement != null) statement.close();
                        } catch (SQLException e) {
                            logger.warn("Could not close db statement or resultset", e);
                        }
                    }
                }
                return null;
            }

        });
    }

    public Timestamp getServerUpdateDateTime(IdMapping idMapping) {
        return idMapping.getServerUpdateDateTime() != null ? new Timestamp(idMapping.getServerUpdateDateTime().getTime()) : null;
    }

    public Timestamp getLastSyncDateTime(IdMapping idMapping) {
        return idMapping.getLastSyncDateTime() != null ? new Timestamp(idMapping.getLastSyncDateTime().getTime()) : new Timestamp(new Date().getTime());
    }

    public IdMapping findByExternalId(final String uuid) {
        return database.executeInTransaction(new TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                String query = "select distinct map.internal_id, map.type, map.uri, map.last_sync_datetime, map.server_update_datetime " +
                        "from shr_id_mapping map where map.external_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, uuid);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            result = new IdMapping(resultSet.getString(1), uuid, resultSet.getString(2),
                                    resultSet.getString(3), new Date(resultSet.getTimestamp(4).getTime()));
                            Timestamp serverDateTime = resultSet.getTimestamp(5);
                            if (serverDateTime != null) {
                                result.setServerUpdateDateTime(new Date(serverDateTime.getTime()));
                            }
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

    public IdMapping findByInternalId(final String uuid) {
        return database.executeInTransaction(new TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                String query = "select distinct map.external_id, map.type, map.uri, map.last_sync_datetime, map.server_update_datetime from shr_id_mapping map where map.internal_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, uuid);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            result = new IdMapping(uuid, resultSet.getString(1), resultSet.getString(2), 
                                    resultSet.getString(3), new Date(resultSet.getTimestamp(4).getTime()));
                            Timestamp serverDateTime = resultSet.getTimestamp(5);
                            if (serverDateTime != null) {
                                result.setServerUpdateDateTime(new Date(serverDateTime.getTime()));
                            }
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

    private boolean mappingExists(final IdMapping idMapping) {
        return database.executeInTransaction(new TxWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) {
                String query = "select distinct map.internal_id from shr_id_mapping map where map.internal_id=? and map.external_id=?";
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

}
