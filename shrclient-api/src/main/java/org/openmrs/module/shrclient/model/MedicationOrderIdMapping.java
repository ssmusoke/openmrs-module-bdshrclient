package org.openmrs.module.shrclient.model;

import java.util.Date;

public class MedicationOrderIdMapping extends IdMapping {

    public MedicationOrderIdMapping(String internalId, String externalId, String uri, Date createdAt) {
        super(internalId, externalId, IdMappingType.MEDICATION_ORDER, uri, createdAt);
    }
}