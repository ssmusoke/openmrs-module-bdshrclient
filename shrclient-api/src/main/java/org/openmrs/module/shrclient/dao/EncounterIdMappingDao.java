package org.openmrs.module.shrclient.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.model.EncounterIdMapping;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.openmrs.module.shrclient.util.Database.TxWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.Date;

@Component("encounterIdMappingDao")
public class EncounterIdMappingDao {

    private Logger logger = Logger.getLogger(EncounterIdMappingDao.class);

    @Autowired
    private Database database;

    private static String INSERT_ENCOUNTER_ID_MAPPING="insert into encounter_id_mapping (internal_id, external_id, uri, last_sync_datetime, server_update_datetime) values (?,?,?,?,?)";
    private static String UPDATE_ENCOUNTER_ID_MAPPING="update encounter_id_mapping set last_sync_datetime = ?, server_update_datetime = ? where internal_id = ?";

    protected void saveOrUpdateIdMapping(final EncounterIdMapping encounterIdMapping) {
        database.executeInTransaction(new TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                PreparedStatement statement = null;
                try{
                    if (!mappingExists(encounterIdMapping)) {
                        statement = getInsertEncounterIdMapping(connection, encounterIdMapping);
                        statement.execute();
                    }else {
                        statement = getUpdateEncounterIdMappingStatement(connection, encounterIdMapping);
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

    private PreparedStatement getInsertEncounterIdMapping(Connection connection, EncounterIdMapping encounterIdMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(INSERT_ENCOUNTER_ID_MAPPING);
        statement.setString(1, encounterIdMapping.getInternalId());
        statement.setString(2, encounterIdMapping.getExternalId());
        statement.setString(3, encounterIdMapping.getUri());
        statement.setTimestamp(4, getLastSyncDateTime(encounterIdMapping));
        statement.setTimestamp(5, getServerUpdateDateTime(encounterIdMapping));

        return statement;
    }

    private PreparedStatement getUpdateEncounterIdMappingStatement(Connection connection, EncounterIdMapping encounterIdMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(UPDATE_ENCOUNTER_ID_MAPPING);
        statement.setTimestamp(1, getLastSyncDateTime(encounterIdMapping));
        statement.setTimestamp(2, getServerUpdateDateTime(encounterIdMapping));
        statement.setString(3, encounterIdMapping.getInternalId());

        return statement;
    }

    private Timestamp getServerUpdateDateTime(EncounterIdMapping idMapping) {
        return idMapping .getServerUpdateDateTime() != null ? new Timestamp(idMapping.getServerUpdateDateTime().getTime()) : null;
    }

    private Timestamp getLastSyncDateTime(IdMapping idMapping) {
        return idMapping.getLastSyncDateTime() != null ? new Timestamp(idMapping.getLastSyncDateTime().getTime()) : new Timestamp(new Date().getTime());
    }

    protected EncounterIdMapping findByExternalId(final String externalId) {
        return database.executeInTransaction(new TxWork<EncounterIdMapping>() {
            @Override
            public EncounterIdMapping execute(Connection connection) {
                String query = "select distinct map.internal_id, map.uri, map.last_sync_datetime, map.server_update_datetime " +
                        "from encounter_id_mapping map where map.external_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                EncounterIdMapping encounterIdMapping = null;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, externalId);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            encounterIdMapping = new EncounterIdMapping(resultSet.getString(1), externalId, resultSet.getString(2),
                                     new Date(resultSet.getTimestamp(3).getTime()),getDateFromTimestamp(resultSet.getTimestamp(4)));
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
                return encounterIdMapping;
            }
        });
    }

    protected EncounterIdMapping findByInternalId(final String internalId) {
        return database.executeInTransaction(new TxWork<EncounterIdMapping>() {
            @Override
            public EncounterIdMapping execute(Connection connection) {
                String query = "select distinct map.external_id, map.uri, map.last_sync_datetime, map.server_update_datetime from encounter_id_mapping map where map.internal_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                EncounterIdMapping encounterIdMapping = null;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, internalId);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            encounterIdMapping = new EncounterIdMapping(internalId, resultSet.getString(1),
                                    resultSet.getString(2), new Date(resultSet.getTimestamp(3).getTime()), getDateFromTimestamp(resultSet.getTimestamp(4)));
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
                return encounterIdMapping;
            }
        });
    }

    private Date getDateFromTimestamp(Timestamp timestamp) {
        return timestamp!=null ? new Date(timestamp.getTime()) : null;
    }

    private boolean mappingExists(final IdMapping idMapping) {
        return database.executeInTransaction(new TxWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) {
                String query = "select distinct map.internal_id from encounter_id_mapping map where map.internal_id=? and map.external_id=?";
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
