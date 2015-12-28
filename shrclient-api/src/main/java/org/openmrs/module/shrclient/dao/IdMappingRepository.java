package org.openmrs.module.shrclient.dao;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.module.shrclient.util.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

@Component
public class IdMappingRepository {

    EncounterIdMappingDao encounterIdMappingDao;
    PatientIdMappingDao patientIdMappingDao;
    SHRIdMappingDao shrIdMappingDao;
    Database database;
    Logger logger = Logger.getLogger(IdMappingRepository.class);


    @Autowired
    public IdMappingRepository(Database database) {
        this.database = database;
        this.encounterIdMappingDao = new EncounterIdMappingDao(database);
        this.patientIdMappingDao = new PatientIdMappingDao(database);
        this.shrIdMappingDao = new SHRIdMappingDao(database);
    }

    public void saveOrUpdateIdMapping(IdMapping idMapping) {
        idMappingDao(idMapping.getType()).saveOrUpdateIdMapping(idMapping);
    }

    public IdMapping findByExternalId(String externalId, String idMappingType) {
        return idMappingDao(idMappingType).findByExternalId(externalId);
    }

    public IdMapping findByInternalId(String internalId, String idMappingType) {
        return idMappingDao(idMappingType).findByInternalId(internalId);
    }

    public List<IdMapping> findByHealthId(String healthId, String idMappingType) {
        return idMappingDao(idMappingType).findByHealthId(healthId);
    }

    public void replaceHealthId(final String toBeReplaced, final String toReplaceWith) {
        final List<IdMapping> reassignedSHRIdMappings = updateHealthIds(findByHealthId(toBeReplaced, "other"), toBeReplaced, toReplaceWith);
        final List<IdMapping> reassignedEncounterIdMappings = updateHealthIds(findByHealthId(toBeReplaced, IdMappingType.ENCOUNTER), toBeReplaced, toReplaceWith);
        database.executeInTransaction(new Database.TxWork<Object>() {
            @Override
            public Object execute(Connection connection) {
                PreparedStatement updateEncounterIdMappingBatch = null;
                PreparedStatement updateShrIdMappingBatch = null;
                try {
                    connection.setAutoCommit(false);
                    updateEncounterIdMappingBatch = getBatchStatement(connection, reassignedEncounterIdMappings);
                    updateShrIdMappingBatch = getBatchStatement(connection, reassignedSHRIdMappings);
                    executeBatch(updateEncounterIdMappingBatch, updateShrIdMappingBatch);
                    connection.commit();
                } catch (Exception e) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    throw new RuntimeException("Error occurred while replacing healthids of id mapping", e);
                } finally {
                    try {
                        close(updateShrIdMappingBatch, updateEncounterIdMappingBatch);
                        connection.setAutoCommit(true);
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or resultset", e);
                    }
                }
                return null;
            }
        });
    }

    private List<IdMapping> updateHealthIds(List<IdMapping> idMappings, String originalHID, String replaceHID) {
        Date modifiedDate = new Date();
        for (IdMapping idMapping : idMappings) {
            String uri = idMapping.getUri();
            idMapping.setUri(uri.replace(originalHID, replaceHID));
            idMapping.setLastSyncDateTime(modifiedDate);
        }
        return idMappings;
    }


    private PreparedStatement getBatchStatement(Connection connection, List<IdMapping> idMappings) throws SQLException {
        if(idMappings.size() == 0){
            return null;
        }
        String idMappingType = idMappings.get(0).getType();
        String updateURIByInternalIdSql = idMappingDao(idMappingType).getUpdateURIByInternalIdSql();
        PreparedStatement preparedStatement = connection.prepareStatement(updateURIByInternalIdSql);
        for (IdMapping idMapping : idMappings) {
            preparedStatement.setString(1, idMapping.getUri());
            preparedStatement.setTimestamp(2, idMapping.getLastSyncTimestamp());
            preparedStatement.setString(3, idMapping.getInternalId());
            preparedStatement.addBatch();
        }
        return preparedStatement;
    }

    private void close(Statement... statements) throws SQLException {
        for (Statement statement : statements) {
            if(statement!=null)
                statement.close();
        }
    }

    private void executeBatch(Statement... statements) throws SQLException {
        for (Statement statement : statements) {
            if(statement!= null){
                statement.executeBatch();
            }
        }
    }


    private IdMappingDao idMappingDao(String idMappingType) {
        if (IdMappingType.ENCOUNTER.equals(idMappingType))
            return encounterIdMappingDao;
        else if (IdMappingType.PATIENT.equals(idMappingType))
            return patientIdMappingDao;
        else
            return shrIdMappingDao;
    }
}
