package org.openmrs.module.fhir.mapper;

import java.util.HashMap;
import java.util.Map;

public class FHIRProperties {

    public static final String LOINC_SOURCE_NAME = "LOINC";

    public static final String FHIR_CONDITION_CODE_DIAGNOSIS = "diagnosis";
    public static final String FHIR_CONDITION_CODE_DIAGNOSIS_DISPLAY = "Diagnosis";

    public static final String FHIR_CONDITION_CODE_CHIEF_COMPLAINT = "complaint";
    public static final String FHIR_CONDITION_CODE_CHIEF_COMPLAINT_DISPLAY = "Complaint";

    public static final String FHIR_CONDITION_CATEGORY_URL = "http://hl7.org/fhir/condition-category";

    public static final String FHIR_SYSTEM_RELATIONSHIP_ROLE = "http://hl7.org/fhir/v3/RoleCode";

    public static final String UCUM_URL = "http://unitsofmeasure.org";

    public static final String SNOMED_VALUE_MODERATE_SEVERTY = "6736007";
    public static final String SNOMED_VALUE_SEVERE_SEVERITY = "24484000";

    public static final String FHIR_SEVERITY_MODERATE = "Moderate";
    public static final String FHIR_SEVERITY_SEVERE = "Severe";

    public static final String UCUM_UNIT_FOR_YEARS = "a";

    public static final String FHIR_DOC_TYPECODES_URL = "http://hl7.org/fhir/vs/doc-typecodes";
    public static final String LOINC_CODE_DETAILS_NOTE = "51899-3";
    public static final String LOINC_DETAILS_NOTE_DISPLAY = "Details Document";

    private Map<String, String> severityCodes = new HashMap<String, String>();

    public FHIRProperties() {
        severityCodes.put(FHIR_SEVERITY_MODERATE, SNOMED_VALUE_MODERATE_SEVERTY);
        severityCodes.put(FHIR_SEVERITY_SEVERE, SNOMED_VALUE_SEVERE_SEVERITY);
    }
}
