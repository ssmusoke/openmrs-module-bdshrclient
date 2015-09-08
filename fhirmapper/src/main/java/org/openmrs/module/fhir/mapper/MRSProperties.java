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

    public static final String MRS_CONCEPT_NAME_COMPLAINT_CONDITION_TEMPLATE = "Condition Complaint Template";
    public static final String MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA = "Condition Chief Complaint Data";
    public static final String MRS_CONCEPT_NAME_CHIEF_COMPLAINT = "Condition Chief Complaint";
    public static final String MRS_CONCEPT_NAME_NON_CODED_CHIEF_COMPLAINT = "Condition Non-Coded Chief Complaint";
    public static final String MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION = "Condition Chief Complaint Duration";

    public static final String MRS_CONCEPT_NAME_FAMILY_HISTORY = "Family History Template";
    public static final String MRS_CONCEPT_NAME_PERSON = "Family Member History";
    public static final String MRS_CONCEPT_NAME_BORN_ON = "Family Member Born On";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION = "Family Member Condition";
    public static final String MRS_CONCEPT_NAME_ONSET_AGE = "Family Member Condition Onset Age";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP_NOTES = "Family Member Condition Notes";
    public static final String MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS = "Family Member Condition Diagnosis";

    public static final String MRS_CONCEPT_IMMUNIZATION_INCIDENT_TEMPLATE = "Immunization Incident Template";
    public static final String MRS_CONCEPT_VACCINATION_REFUSED = "Immunization Incident Vaccination Refused";
    public static final String MRS_CONCEPT_VACCINE = "Immunization Incident Vaccine";
    public static final String MRS_CONCEPT_VACCINATION_DATE = "Immunization Incident Vaccination Date";
    public static final String MRS_CONCEPT_VACCINATION_REPORTED = "Immunization Incident Vaccination Reported";
    public static final String MRS_CONCEPT_DOSAGE = "Immunization Incident Vaccination Dosage";

    public static final String MRS_CONCEPT_PROCEDURES_TEMPLATE = "Procedure Template";
    public static final String MRS_CONCEPT_PROCEDURE_TYPE = "Procedure Type";
    public static final String MRS_CONCEPT_PROCEDURE_START_DATE = "Procedure Start Date";
    public static final String MRS_CONCEPT_PROCEDURE_END_DATE = "Procedure End Date";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSIS = "Procedure Diagnosis";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY = "Procedure Diagnostic Study";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST = "Procedure Diagnostic Test";
    public static final String MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT = "Procedure Diagnostic Result";
    public static final String MRS_CONCEPT_PROCEDURE_NOTES = "Procedure Notes";

    public static final String MRS_CONCEPT_NAME_LAB_NOTES = "LAB_NOTES";
    public static final String MRS_CARE_SETTING_FOR_OUTPATIENT = "Outpatient";

    public static final String MRS_CARE_SETTING_FOR_INPATIENT = "Inpatient";

    public static final String MRS_ENC_TYPE_LAB_RESULT = "LAB_RESULT";

    public static final String MRS_CONCEPT_CLASS_LAB_SET = "LabSet";
    public static final String MRS_DRUG_ORDER_TYPE = "Drug Order";

    public static final String MRS_LAB_ORDER_TYPE = "Lab Order";

    public static final String DRUG_ORDER_QUANTITY_UNITS_CONCEPT_NAME = "Unit(s)";

    public static final String TR_VALUESET_RELATIONSHIP_TYPE = "Relationship Type";
    public static final String TR_VALUESET_QUANTITY_UNITS = "Quantity Units";
    public static final String TR_VALUESET_IMMUNIZATION_REASON = "Immunization Reason";
    public static final String TR_VALUESET_IMMUNIZATION_REFUSAL_REASON = "No Immunization Reason";
    public static final String TR_VALUESET_ROUTE_OF_ADMINSTRATION = "Route of Administration";
    public static final String TR_VALUESET_PROCEDURE_OUTCOME = "Procedure Outcome";
    public static final String TR_VALUESET_PROCEDURE_FOLLOWUP = "Procedure Follow up";
    public static final String TR_VALUESET_PROCEDURE_STATUS = "Procedure Status";

    public static final String TR_CONCEPT_CAUSE_OF_DEATH = "Cause of Death";
    public static final String TR_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH = "Unspecified cause of death";

    public static final String GLOBAL_PROPERTY_CONCEPT_RELATIONSHIP_TYPE = "shr.concept.relationshipType";
    public static final String GLOBAL_PROPERTY_CONCEPT_QUANTITY_UNITS = "shr.concept.quantityUnits";
    public static final String GLOBAL_PROPERTY_CONCEPT_IMMUNIZATION_REASON = "shr.concept.immunizationReason";
    public static final String GLOBAL_PROPERTY_CONCEPT_IMMUNIZATION_REFUSAL_REASON = "shr.concept.immunizationRefusalReason";
    public static final String GLOBAL_PROPERTY_CONCEPT_ROUTE_OF_ADMINISTRATION = "shr.concept.routeOfAdministration";
    public static final String GLOBAL_PROPERTY_CONCEPT_PROCEDURE_OUTCOME = "shr.concept.procedureOutcome";
    public static final String GLOBAL_PROPERTY_CONCEPT_PROCEDURE_FOLLOWUP = "shr.concept.procedureFollowUp";
    public static final String GLOBAL_PROPERTY_CONCEPT_PROCEDURE_STATUS = "shr.concept.procedureStatus";

    public static final String GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH = "concept.causeOfDeath";
    public static final String GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH = "concept.unspecifiedCauseOfDeath";

    public static final String GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG = "shr.hie.facilities.location.tag";
    public static final String GLOBAL_PROPERTY_SHR_SYSTEM_USER_TAG = "shr.system.user";
    public static final String GLOBAL_PROPERTY_EMR_PRIMARY_IDENTIFIER_TYPE = "emr.primaryIdentifierType";
}