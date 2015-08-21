package org.openmrs.module.fhir;

import ca.uhn.fhir.context.FhirContext;

public class FhirContextHelper {
    private static FhirContext fhirContext = FhirContext.forDstu2();

    public static FhirContext getFhirContext() {
        return fhirContext;
    }
}
