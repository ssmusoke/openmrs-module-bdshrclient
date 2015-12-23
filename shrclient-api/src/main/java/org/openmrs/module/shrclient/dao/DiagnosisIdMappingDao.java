package org.openmrs.module.shrclient.dao;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.DatabaseConstants;
import org.openmrs.module.shrclient.model.DiagnosisIdMapping;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component("diagnosisIdMappingDao")
public class DiagnosisIdMappingDao extends IdMappingDao {
    @Autowired
    public DiagnosisIdMappingDao(Database database) {
        super(database, Logger.getLogger(DiagnosisIdMappingDao.class));
    }

    @Override
    public String getMappingTable() {
        return DatabaseConstants.DIAGNOSIS_ID_MAPPING_TABLE;
    }

    @Override
    public PreparedStatement getInsertIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        DiagnosisIdMapping diagnosisIdMapping = (DiagnosisIdMapping) idMapping;
        PreparedStatement statement = connection.prepareStatement(getInsertMappingSql());
        statement.setString(1, diagnosisIdMapping.getInternalId());
        statement.setString(2, diagnosisIdMapping.getExternalId());
        statement.setString(3, diagnosisIdMapping.getUri());

        return statement;
    }

    @Override
    public PreparedStatement getUpdateIdMappingStatement(Connection connection, IdMapping idMapping) throws SQLException {
        return null;
    }

    @Override
    public DiagnosisIdMapping buildIdMapping(ResultSet resultSet) throws SQLException {
        return new DiagnosisIdMapping(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3));
    }

    @Override
    public String getInsertMappingSql() {
        return String.format("insert into %s (internal_id, external_id, uri) values (?,?,?)", getMappingTable());
    }

    @Override
    public String getUpdateMappingSql() {
        return null;
    }

    @Override
    public String getFetchByExternalIdSql() {
        return String.format("select distinct map.internal_id, map.external_id, map.uri from %s map where map.external_id=?", getMappingTable());
    }

    @Override
    public String getFetchByInternalIdSql() {
        return String.format("select distinct map.internal_id, map.external_id, map.uri from %s map where map.internal_id=?", getMappingTable());
    }
}
