package org.openmrs.module.shrclient.dao;

import org.apache.log4j.Logger;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.DatabaseConstants;
import org.openmrs.module.shrclient.model.EncounterIdMapping;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component("encounterIdMappingDao")
public class EncounterIdMappingDao extends IdMappingDao {

    @Autowired
    public EncounterIdMappingDao(Database database) {
        super(database, Logger.getLogger(EncounterIdMappingDao.class));
    }

    @Override
    public String getMappingTable() {
        return DatabaseConstants.ENCOUNTER_ID_MAPPING_TABLE;
    }

    @Override
    public PreparedStatement getInsertIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        EncounterIdMapping encounterIdMapping = (EncounterIdMapping) idMapping;
        PreparedStatement statement = connection.prepareStatement(getInsertMappingSql());
        statement.setString(1, encounterIdMapping.getInternalId());
        statement.setString(2, encounterIdMapping.getExternalId());
        statement.setString(3, encounterIdMapping.getUri());
        statement.setTimestamp(4, encounterIdMapping.getLastSyncTimestamp());
        statement.setTimestamp(5, encounterIdMapping.getServerUpdateTimestamp());

        return statement;
    }

    @Override
    public PreparedStatement getUpdateIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        EncounterIdMapping encounterIdMapping = (EncounterIdMapping) idMapping;
        PreparedStatement statement = connection.prepareStatement(getUpdateMappingSql());
        statement.setTimestamp(1, encounterIdMapping.getLastSyncTimestamp());
        statement.setTimestamp(2, encounterIdMapping.getServerUpdateTimestamp());
        statement.setString(3, encounterIdMapping.getInternalId());

        return statement;
    }

    @Override
    public PreparedStatement getCheckMappingExistsStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(getMappingExistsQuery());
        statement.setString(1, idMapping.getInternalId());
        statement.setString(2, idMapping.getExternalId());
        return statement;
    }

    @Override
    public EncounterIdMapping buildIdMapping(ResultSet resultSet) throws SQLException {
        return new EncounterIdMapping(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                new Date(resultSet.getTimestamp(4).getTime()),DateUtil.getDateFromTimestamp(resultSet.getTimestamp(5)));
    }

    @Override
    public String getFetchByExternalIdSql(){
        return String.format("select distinct map.internal_id, map.external_id, map.uri, map.last_sync_datetime, map.server_update_datetime " +
                "from %s map where map.external_id=?", getMappingTable());
    }
    
    @Override
    public String getFetchByInternalIdSql(){
        return String.format("select distinct map.internal_id, map.external_id, map.uri, map.last_sync_datetime, map.server_update_datetime from %s map where map.internal_id=?", getMappingTable());
    }

    private String getInsertMappingSql() {
        return String.format("insert into %s (internal_id, external_id, uri, last_sync_datetime, server_update_datetime) values (?,?,?,?,?)", getMappingTable());
    }

    private String getUpdateMappingSql() {
        return String.format("update %s set last_sync_datetime = ?, server_update_datetime = ? where internal_id = ?", getMappingTable());
    }

    private String getMappingExistsQuery() {
        return String.format("select distinct map.internal_id from %s map where map.internal_id=? and map.external_id=?", getMappingTable());
    }
}
