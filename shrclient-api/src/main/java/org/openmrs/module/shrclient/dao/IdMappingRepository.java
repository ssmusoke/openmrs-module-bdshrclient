package org.openmrs.module.shrclient.dao;

import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdMappingRepository {

    EncounterIdMappingDao encounterIdMappingDao;
    SHRIdMappingDao shrIdMappingDao;

    @Autowired
    public IdMappingRepository(EncounterIdMappingDao encounterIdMappingDao, SHRIdMappingDao shrIdMappingDao) {
        this.encounterIdMappingDao = encounterIdMappingDao;
        this.shrIdMappingDao = shrIdMappingDao;
    }

    public void saveOrUpdateIdMapping(final IdMapping idMapping) {
        if(IdMappingType.ENCOUNTER.equals(idMapping.getType()))
            encounterIdMappingDao.saveOrUpdateIdMapping(idMapping);
        else
            shrIdMappingDao.saveOrUpdateIdMapping(idMapping);
    }

    public IdMapping findByExternalId(final String externalId, String idMappingType){
        if(IdMappingType.ENCOUNTER.equals(idMappingType))
            return encounterIdMappingDao.findByExternalId(externalId);
        else
            return shrIdMappingDao.findByExternalId(externalId);
    }

    public IdMapping findByInternalId(final String internalId, String idMappingType) {
        if(IdMappingType.ENCOUNTER.equals(idMappingType))
            return encounterIdMappingDao.findByInternalId(internalId);
        else
            return shrIdMappingDao.findByInternalId(internalId);
    }


}
