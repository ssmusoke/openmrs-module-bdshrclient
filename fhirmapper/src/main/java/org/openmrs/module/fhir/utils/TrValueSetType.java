package org.openmrs.module.fhir.utils;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

public enum TrValueSetType {
    RELATIONSHIP_TYPE(TR_VALUESET_RELATIONSHIP_TYPE, GLOBAL_PROPERTY_CONCEPT_RELATIONSHIP_TYPE),
    QUANTITY_UNITS(TR_VALUESET_QUANTITY_UNITS, GLOBAL_PROPERTY_CONCEPT_QUANTITY_UNITS),
    IMMUNIZATION_REASON(TR_VALUESET_IMMUNIZATION_REASON, GLOBAL_PROPERTY_CONCEPT_IMMUNIZATION_REASON),
    IMMUNIZATION_REFUSAL_REASON(TR_VALUESET_IMMUNIZATION_REFUSAL_REASON, GLOBAL_PROPERTY_CONCEPT_IMMUNIZATION_REFUSAL_REASON),
    ROUTE_OF_ADMINISTRATION(TR_VALUESET_ROUTE_OF_ADMINSTRATION, GLOBAL_PROPERTY_CONCEPT_ROUTE_OF_ADMINISTRATION);

    private final String defaultConceptName;
    private final String globalPropertyKey;

    TrValueSetType(String defaultConceptName, String globalPropertyKey) {
        this.defaultConceptName = defaultConceptName;
        this.globalPropertyKey = globalPropertyKey;
    }

    public String getDefaultConceptName() {
        return defaultConceptName;
    }

    public String getGlobalPropertyKey() {
        return globalPropertyKey;
    }
}
