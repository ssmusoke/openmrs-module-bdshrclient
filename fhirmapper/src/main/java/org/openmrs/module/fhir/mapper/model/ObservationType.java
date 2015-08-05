package org.openmrs.module.fhir.mapper.model;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

public enum ObservationType {

    HISTORY_AND_EXAMINATION(GLOBAL_PROPERTY_CONCEPT_HISTORY_AND_EXAMINATION, null),
    CHIEF_COMPLAINT_DATA(GLOBAL_PROPERTY_CONCEPT_CHIEF_COMPLAINT_DATA, null),
    VISIT_DIAGNOSES(null, MRS_CONCEPT_NAME_VISIT_DIAGNOSES),
    FAMILY_HISTORY(null, MRS_CONCEPT_NAME_FAMILY_HISTORY),
    IMMUNIZATION(null, MRS_CONCEPT_IMMUNIZATION_INCIDENT),
    PROCEDURES(null, MRS_CONCEPT_PROCEDURES);

    private String conceptIdKey;
    private String conceptName;

    ObservationType(String conceptIdKey, String conceptName) {
        this.conceptIdKey = conceptIdKey;
        this.conceptName = conceptName;
    }

    public String getConceptIdKey() {
        return conceptIdKey;
    }

    public String getConceptName() {
        return conceptName;
    }
}
