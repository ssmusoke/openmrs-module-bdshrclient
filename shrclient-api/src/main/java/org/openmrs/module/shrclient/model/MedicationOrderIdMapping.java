package org.openmrs.module.shrclient.model;

public class MedicationOrderIdMapping extends IdMapping {

    public MedicationOrderIdMapping(String internalId, String externalId, String uri) {
        super(internalId, externalId, IdMappingType.MEDICATION_ORDER, uri);
    }
}