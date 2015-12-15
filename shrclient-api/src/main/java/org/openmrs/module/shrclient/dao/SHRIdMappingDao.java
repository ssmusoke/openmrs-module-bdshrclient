package org.openmrs.module.shrclient.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.openmrs.module.shrclient.util.Database.TxWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component("shrIdMappingDao")
public class SHRIdMappingDao {

    private Logger logger = Logger.getLogger(SHRIdMappingDao.class);

    @Autowired
    private Database database;

    private static String INSERT_SHR_ID_MAPPING="insert into shr_id_mapping (internal_id, external_id, type, uri, last_sync_datetime) values (?,?,?,?,?)";
    private static String UPDATE_SHR_ID_MAPPING="update shr_id_mapping set last_sync_datetime = ? where internal_id = ?";

    protected void saveOrUpdateIdMapping(final IdMapping idMapping) {
        database.executeInTransaction(new TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                PreparedStatement statement = null;
                try{
                    if (!mappingExists(idMapping)) {
                        statement = getInsertSHRIdMappingStatement(connection, idMapping);
                        statement.execute();
                    }else {
                        statement = getUpdateSHRIdMappingStatement(connection, idMapping);
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

    private PreparedStatement getInsertSHRIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(INSERT_SHR_ID_MAPPING);
        statement.setString(1, idMapping.getInternalId());
        statement.setString(2, idMapping.getExternalId());
        statement.setString(3, idMapping.getType());
        statement.setString(4, idMapping.getUri());
        statement.setTimestamp(5, idMapping.getLastSyncDateTimestamp());

        return statement;
    }

    private PreparedStatement getUpdateSHRIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(UPDATE_SHR_ID_MAPPING);
        statement.setTimestamp(1, idMapping.getLastSyncDateTimestamp());
        statement.setString(2, idMapping.getInternalId());

        return statement;
    }

    protected IdMapping findByExternalId(final String externalId) {
        return database.executeInTransaction(new TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                String query = "select distinct map.internal_id, map.type, map.uri, map.last_sync_datetime " +
                        "from shr_id_mapping map where map.external_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, externalId);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            result = new IdMapping(resultSet.getString(1), externalId, resultSet.getString(2),
                                    resultSet.getString(3), new Date(resultSet.getTimestamp(4).getTime()));
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
        return database.executeInTransaction(new TxWork<IdMapping>() {
            @Override
            public IdMapping execute(Connection connection) {
                String query = "select distinct map.external_id, map.type, map.uri, map.last_sync_datetime from shr_id_mapping map where map.internal_id=?";
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                IdMapping result = null;
                try {
                    statement = connection.prepareStatement(query);
                    statement.setString(1, internalId);
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        if (StringUtils.isNotBlank(resultSet.getString(1))) {
                            result = new IdMapping(internalId, resultSet.getString(1), resultSet.getString(2),
                                    resultSet.getString(3), new Date(resultSet.getTimestamp(4).getTime()));
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
