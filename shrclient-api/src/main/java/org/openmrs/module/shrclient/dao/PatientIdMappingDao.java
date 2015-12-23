package org.openmrs.module.shrclient.dao;

import org.apache.log4j.Logger;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.DatabaseConstants;
import org.openmrs.module.shrclient.model.EncounterIdMapping;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.PatientIdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component("patientIdMappingDao")
public class PatientIdMappingDao extends IdMappingDao {

    @Autowired
    public PatientIdMappingDao(Database database) {
        super(database, Logger.getLogger(PatientIdMappingDao.class));
    }

    @Override
    public String getMappingTable() {
        return DatabaseConstants.PATIENT_ID_MAPPING_TABLE;
    }

    @Override
    public PreparedStatement getInsertIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PatientIdMapping patientIdMapping = (PatientIdMapping) idMapping;
        PreparedStatement statement = connection.prepareStatement(getInsertMappingSql());
        statement.setString(1, patientIdMapping.getInternalId());
        statement.setString(2, patientIdMapping.getExternalId());
        statement.setString(3, patientIdMapping.getUri());
        statement.setTimestamp(4, patientIdMapping.getLastSyncTimestamp());
        statement.setTimestamp(5, patientIdMapping.getServerUpdateTimestamp());

        return statement;
    }

    @Override
    public PreparedStatement getUpdateIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        PatientIdMapping patientIdMapping = (PatientIdMapping) idMapping;
        PreparedStatement statement = connection.prepareStatement(getUpdateMappingSql());
        statement.setTimestamp(1, patientIdMapping.getLastSyncTimestamp());
        statement.setTimestamp(2, patientIdMapping.getServerUpdateTimestamp());
        statement.setString(3, patientIdMapping.getInternalId());

        return statement;
    }

    @Override
    public PatientIdMapping buildIdMapping(ResultSet resultSet) throws SQLException {
        return new PatientIdMapping(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                new Date(resultSet.getTimestamp(4).getTime()),DateUtil.getDateFromTimestamp(resultSet.getTimestamp(5)));
    }

    @Override
    public String getInsertMappingSql() {
        return String.format("insert into %s (internal_id, external_id, uri, last_sync_datetime, server_update_datetime) values (?,?,?,?,?)", getMappingTable());
    }

    @Override
    public String getUpdateMappingSql() {
        return String.format("update %s set last_sync_datetime = ?, server_update_datetime = ? where internal_id = ?", getMappingTable());
    }

    @Override
    public String getFetchByExternalIdSql(){
        return String.format("select distinct map.internal_id, map.external_id, map.uri, map.last_sync_datetime, map.server_update_datetime " +
                "from %s map where map.external_id=?", getMappingTable());
    }

    public String getFetchByInternalIdSql(){
        return String.format("select distinct map.internal_id, map.external_id, map.uri, map.last_sync_datetime, map.server_update_datetime from %s map where map.internal_id=?", getMappingTable());
    }

}
