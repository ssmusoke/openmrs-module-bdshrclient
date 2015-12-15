package org.openmrs.module.fhir.utils;


import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Location;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static org.openmrs.module.fhir.MRSProperties.GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG;

@Component
public class OMRSLocationService {

    @Autowired
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    @Autowired
    private IdMappingRepository idMappingRepository;

    public boolean isLocationHIEFacility(Location location) {
        Integer hieLocationTagId = getHIEFacilityLocationTag();
        if (location != null && location.getTags() != null && hieLocationTagId != null) {
            Collection locationTagIds = CollectionUtils.collect(location.getTags(), new BeanToPropertyValueTransformer("locationTagId"));
            return locationTagIds.contains(hieLocationTagId);
        }
        return false;
    }

    public String getLocationHIEIdentifier(Location location) {
        IdMapping locationMapping = idMappingRepository.findByInternalId(location.getUuid(), IdMappingType.FACILITY);
        return locationMapping != null ? locationMapping.getExternalId() : null;
    }

    public Integer getHIEFacilityLocationTag() {
        String hieFacilityLocationTag = globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG);
        return hieFacilityLocationTag != null ? Integer.parseInt(hieFacilityLocationTag) : null;
    }
}
