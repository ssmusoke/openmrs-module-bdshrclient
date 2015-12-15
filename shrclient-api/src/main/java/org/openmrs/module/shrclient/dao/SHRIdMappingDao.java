package org.openmrs.module.shrclient.dao;

import org.apache.log4j.Logger;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.DatabaseConstants;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component("shrIdMappingDao")
public class SHRIdMappingDao extends IdMappingDao{


    @Autowired
    public SHRIdMappingDao(Database database) {
        super(database, Logger.getLogger(SHRIdMappingDao.class));
    }

    @Override
    public String getMappingTable() {
        return DatabaseConstants.SHR_ID_MAPPING_TABLE;
    }

    @Override
    public PreparedStatement getInsertIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(getInsertMappingSql());
        statement.setString(1, idMapping.getInternalId());
        statement.setString(2, idMapping.getExternalId());
        statement.setString(3, idMapping.getType());
        statement.setString(4, idMapping.getUri());
        statement.setTimestamp(5, idMapping.getLastSyncTimestamp());
        statement.setTimestamp(6, idMapping.getServerUpdateTimestamp());

        return statement;
    }

    @Override
    public PreparedStatement getUpdateIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(getUpdateMappingSql());
        statement.setTimestamp(1, idMapping.getLastSyncTimestamp());
        statement.setTimestamp(2, idMapping.getServerUpdateTimestamp());
        statement.setString(3, idMapping.getInternalId());

        return statement;
    }

    @Override
    public IdMapping buildIdMappingByExternalId(ResultSet resultSet, String externalId) throws SQLException {
        return new IdMapping(resultSet.getString(1), externalId, resultSet.getString(2),
                resultSet.getString(3), new Date(resultSet.getTimestamp(4).getTime()), DateUtil.getDateFromTimestamp(resultSet.getTimestamp(5)));
    }

    @Override
    public IdMapping buildIdMappingByInternalId(ResultSet resultSet, String internalId) throws SQLException {
        return new IdMapping(internalId, resultSet.getString(1), resultSet.getString(2),
                resultSet.getString(3), new Date(resultSet.getTimestamp(4).getTime()),DateUtil.getDateFromTimestamp(resultSet.getTimestamp(5)));
    }

    @Override
    public String getInsertMappingSql() {
        return String.format("insert into %s (internal_id, external_id, type, uri, last_sync_datetime, server_update_datetime) values (?,?,?,?,?,?)", getMappingTable());
    }

    @Override
    public String getUpdateMappingSql() {
        return String.format("update %s set last_sync_datetime = ?,server_update_datetime = ? where internal_id = ?", getMappingTable());
    }

    @Override
    public String getFetchByExternalIdSql() {
        return String.format("select distinct map.internal_id, map.type, map.uri, map.last_sync_datetime, map.server_update_datetime " +
                "from %s map where map.external_id=?", getMappingTable());
    }

    @Override
    public String getFetchByInternalIdSql() {
        return String.format("select distinct map.external_id, map.type, map.uri, map.last_sync_datetime,map.server_update_datetime from %s map where map.internal_id=?", getMappingTable());
    }
}
