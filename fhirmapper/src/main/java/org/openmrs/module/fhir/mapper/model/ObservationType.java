package org.openmrs.module.fhir.mapper.model;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_FAMILY_HISTORY;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_HISTORY_AND_EXAMINATION;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES;

public enum ObservationType {

    HISTORY_AND_EXAMINATION(MRS_CONCEPT_NAME_HISTORY_AND_EXAMINATION),
    CHIEF_COMPLAINT_DATA(MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA),
    VISIT_DIAGNOSES(MRS_CONCEPT_NAME_VISIT_DIAGNOSES),
    FAMILY_HISTORY(MRS_CONCEPT_NAME_FAMILY_HISTORY);

    private String displayName;

    ObservationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
