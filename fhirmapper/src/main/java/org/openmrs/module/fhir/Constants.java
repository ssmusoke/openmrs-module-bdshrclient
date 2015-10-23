package org.openmrs.module.fhir;


public class Constants {
    public static final String NATIONAL_ID_ATTRIBUTE = "nationalId";

    public static final String HEALTH_ID_ATTRIBUTE = "healthId";

    public static final String BIRTH_REG_NO_ATTRIBUTE = "birthRegistrationId";

    public static final String GIVEN_NAME_LOCAL = "givenNameLocal";

    public static final String FAMILY_NAME_LOCAL = "familyNameLocal";

    public static final String PHONE_NUMBER = "phoneNumber";

    public static final String OCCUPATION_ATTRIBUTE = "occupation";

    public static final String EDUCATION_ATTRIBUTE = "education";
    
    public static final String HOUSE_HOLD_CODE_ATTRIBUTE = "householdCode";

    public static final String IDENTIFIER_SOURCE_NAME = "BDH";

    public static final String ID_MAPPING_ENCOUNTER_TYPE = "encounter";

    public static final String ID_MAPPING_REFERENCE_TERM_TYPE = "concept_reference_term";

    public static final String ID_MAPPING_CONCEPT_TYPE = "concept";

    public static final String ID_MAPPING_PATIENT_TYPE = "patient";

    public static final String ID_MAPPING_PERSON_RELATION_TYPE = "PERSON_RELATION";

    public final static String ORGANIZATION_ATTRIBUTE_TYPE_NAME = "Organization";

    public static final String ID_MAPPING_ORDER_TYPE = "order";

    public final static String FATHER_NAME_ATTRIBUTE_TYPE = "fatherName";
    public final static String SPOUSE_NAME_ATTRIBUTE_TYPE = "spouseName";

    public final static String UNVERIFIED_BY_TR = "(TR Unverified)";
    public final static String LOCAL_CONCEPT_VERSION_PREFIX = "DLN-H";
    public final static String CONCEPT_MAP_TYPE_MAY_BE_A = "MAY BE A";

    public final static int CACHE_TTL = 60 * 1000 * 5;
}
