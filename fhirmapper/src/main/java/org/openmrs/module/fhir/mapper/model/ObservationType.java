package org.openmrs.module.fhir.mapper.model;


public enum ObservationType {

    HISTORY_AND_EXAMINATION("History and Examination"),
    VITALS("Vitals");

    private String displayName;

    ObservationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
