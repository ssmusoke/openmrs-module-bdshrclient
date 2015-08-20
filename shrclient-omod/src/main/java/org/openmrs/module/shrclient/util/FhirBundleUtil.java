package org.openmrs.module.shrclient.util;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.stereotype.Component;

@Component
public class FhirBundleUtil {
    private FhirContext fhirContext;

    public FhirBundleUtil() {
        fhirContext = FhirContext.forDstu2();
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }
}
