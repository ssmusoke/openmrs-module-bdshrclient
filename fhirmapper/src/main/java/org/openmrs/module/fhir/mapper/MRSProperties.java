package org.openmrs.module.fhir.mapper;


public class MRSProperties {
    public static final String MRS_OUT_PATIENT_VISIT_TYPE = "OPD";
    public static final String MRS_IN_PATIENT_VISIT_TYPE = "IPD";

    public static final String MRS_DIAGNOSIS_STATUS_PRESUMED = "Presumed";
    public static final String MRS_DIAGNOSIS_STATUS_CONFIRMED = "Confirmed";
    public static final String MRS_DIAGNOSIS_SEVERITY_PRIMARY = "Primary";
    public static final String MRS_CONCEPT_NAME_VISIT_DIAGNOSES = "Visit Diagnoses";
    public static final String MRS_CONCEPT_NAME_CODED_DIAGNOSIS = "Coded Diagnosis";
    public static final String MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY = "Diagnosis Certainty";
    public static final String MRS_CONCEPT_NAME_DIAGNOSIS_ORDER = "Diagnosis order";

    public static final String MRS_CONCEPT_NAME_HISTORY_AND_EXAMINATION = "History and Examination";
    public static final String MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA = "Chief Complaint Data";
    public static final String MRS_CONCEPT_NAME_CHIEF_COMPLAINT = "Chief Complaint";
    public static final String MRS_CONCEPT_NAME_NON_CODED_CHIEF_COMPLAINT = "Non-Coded Chief Complaint";
    public static final String MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION = "Chief Complaint Duration";

    public static final String MRS_CONCEPT_NAME_FAMILY_HISTORY = "Family History";
    public static final String MRS_CONCEPT_NAME_PERSON = "Person";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP = "Relationship Type";
    public static final String MRS_CONCEPT_NAME_BORN_ON = "Born On";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION = "Relationship Condition";
    public static final String MRS_CONCEPT_NAME_ONSET_AGE = "Onset Age";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP_NOTES = "Relationship Notes";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS = "Relationship Diagnosis";

    public static final String MRS_CONCEPT_NAME_LAB_NOTES = "LAB_NOTES";

    public static final String MRS_CARE_SETTING_FOR_OUTPATIENT = "Outpatient";
    public static final String MRS_CARE_SETTING_FOR_INPATIENT = "Inpatient";

    public static final String MRS_ENC_TYPE_LAB_RESULT = "LAB_RESULT";

    public static final String MRS_CONCEPT_CLASS_LAB_SET = "LabSet";

    public static final String MRS_DRUG_ORDER_TYPE = "Drug Order";
    public static final String MRS_LAB_ORDER_TYPE = "Lab Order";

    public static final String DRUG_ORDER_QUANTITY_UNITS_CONCEPT_NAME = "Unit(s)";

    public static final String MRS_CONCEPT_IMMUNIZATION_INCIDENT = "Immunization Incident";
    public static final String MRS_CONCEPT_VACCINATION_REFUSED = "Vaccination Refused";
    public static final String MRS_CONCEPT_VACCINE = "Vaccine";
    public static final String MRS_CONCEPT_VACCINATION_DATE = "Vaccination Date";
    public static final String MRS_CONCEPT_VACCINATION_REPORTED = "Vaccination Reported";
    public static final String MRS_CONCEPT_DOSAGE = "Dosage";

    public static final String MRS_DIAGNOSIS_REPORT_RESOURCE_NAME= "Diagnostic Report";
    public static final String MRS_CONCEPT_PROCEDURES = "Procedures";
    public static final String MRS_CONCEPT_PROCEDURE_TYPE = "Procedure";
    public static final String MRS_CONCEPT_PROCEDURE_OUTCOME = "Outcome";
    public static final String MRS_CONCEPT_PROCEDURE_FOLLOW_UP = "Follow up";
    public static final String MRS_CONCEPT_PROCEDURE_START_DATE = "Procedure Start Date";
    public static final String MRS_CONCEPT_PROCEDURE_END_DATE = "Procedure End Date";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSIS = "Diagnosis";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY = "Diagnostic Study";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST = "Diagnostic Test";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT = "Result";



    public static final String VALUESET_QUANTITY_UNITS = "Quantity Units";
    public static final String VALUESET_IMMUNIZATION_REASON = "Immunization Reason";
    public static final String VALUESET_IMMUNIZATION_REFUSAL_REASON = "No Immunization Reason";
    public static final String VALUESET_ROUTE = "Route of Administration";

    public static final String GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH = "concept.causeOfDeath";
    public static final String GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH = "concept.unspecifiedCauseOfDeath";
    public static final String GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG = "shr.hie.facilities.location.tag";
    public static final String GLOBAL_PROPERTY__SHR_SYSTEM_USER_TAG = "shr.system.user";
    public static final String GLOBAL_PROPERTY_EMR_PRIMARY_IDENTIFIER_TYPE = "emr.primaryIdentifierType";
}
