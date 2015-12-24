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
import java.util.ArrayList;
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

    public List<IdMapping> findByHealthId(String healthId) {
        List<IdMapping> idMappings = new ArrayList<>();
        idMappings.addAll(encounterIdMappingDao.findByHealthId(healthId));
        idMappings.addAll(shrIdMappingDao.findByHealthId(healthId));

        return idMappings;
    }

    public void replaceHealthId(final String toBeReplaced, final String toReplaceWith) {
        final List<IdMapping> modifiedSHRIdMappings = modifyIdMappings(shrIdMappingDao.findByHealthId(toBeReplaced), toBeReplaced, toReplaceWith);
        final List<IdMapping> modifiedEncounterIdMappings = modifyIdMappings(encounterIdMappingDao.findByHealthId(toBeReplaced), toBeReplaced, toReplaceWith);
        database.executeInTransaction(new Database.TxWork<Object>() {
            @Override
            public Object execute(Connection connection){
                PreparedStatement updateEncounterIdMappingBatch = null;
                PreparedStatement updateShrIdMappingBatch = null;
                try {
                    connection.setAutoCommit(false);
                    updateEncounterIdMappingBatch = getBatchStatement(connection, modifiedEncounterIdMappings, IdMappingType.ENCOUNTER);
                    updateShrIdMappingBatch = getBatchStatement(connection, modifiedSHRIdMappings, "");
                    updateEncounterIdMappingBatch.executeBatch();
                    updateShrIdMappingBatch.executeBatch();
                    connection.commit();
                } catch (Exception e) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    throw new RuntimeException("Error occurred while creating id mapping", e);
                } finally {
                    try {
                        if (updateEncounterIdMappingBatch != null) updateEncounterIdMappingBatch.close();
                        if (updateShrIdMappingBatch != null) updateEncounterIdMappingBatch.close();
                        connection.setAutoCommit(true);
                    } catch (SQLException e) {
                        logger.warn("Could not close db statement or resultset", e);
                    }
                }
                return null;
            }
        });
    }

    private List<IdMapping> modifyIdMappings(List<IdMapping> idMappings, String originalHID, String replaceHID) {
        for (IdMapping idMapping : idMappings) {
            String uri = idMapping.getUri();
            idMapping.setUri(uri.replace(originalHID, replaceHID));
            idMapping.setLastSyncDateTime(new Date());
        }
        return idMappings;
    }


    private PreparedStatement getBatchStatement(Connection connection, List<IdMapping> idMappings, String idMappingType) throws SQLException {
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


    private IdMappingDao idMappingDao(String idMappingType) {
        if (IdMappingType.ENCOUNTER.equals(idMappingType))
            return encounterIdMappingDao;
        else if (IdMappingType.PATIENT.equals(idMappingType))
            return patientIdMappingDao;
        else
            return shrIdMappingDao;
    }
}
