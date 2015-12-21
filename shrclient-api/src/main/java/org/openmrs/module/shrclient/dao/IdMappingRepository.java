package org.openmrs.module.shrclient.dao;

import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdMappingRepository {

    EncounterIdMappingDao encounterIdMappingDao;
    PatientIdMappingDao patientIdMappingDao;
    SHRIdMappingDao shrIdMappingDao;

    @Autowired
    public IdMappingRepository(EncounterIdMappingDao encounterIdMappingDao, PatientIdMappingDao patientIdMappingDao, SHRIdMappingDao shrIdMappingDao) {
        this.encounterIdMappingDao = encounterIdMappingDao;
        this.patientIdMappingDao = patientIdMappingDao;
        this.shrIdMappingDao = shrIdMappingDao;
    }

    public void saveOrUpdateIdMapping(final IdMapping idMapping) {
        idMappingDao(idMapping.getType()).saveOrUpdateIdMapping(idMapping);
    }

    public IdMapping findByExternalId(final String externalId, String idMappingType){
        return idMappingDao(idMappingType).findByExternalId(externalId);
    }

    public IdMapping findByInternalId(final String internalId, String idMappingType) {
       return idMappingDao(idMappingType).findByInternalId(internalId);
    }

    private IdMappingDao idMappingDao(String idMappingType){
        if(IdMappingType.ENCOUNTER.equals(idMappingType))
            return encounterIdMappingDao;
        else if(IdMappingType.PATIENT.equals(idMappingType))
            return patientIdMappingDao;
        else
            return shrIdMappingDao;
    }


}
