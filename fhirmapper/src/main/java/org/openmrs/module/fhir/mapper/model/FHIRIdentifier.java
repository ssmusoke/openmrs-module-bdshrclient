package org.openmrs.module.fhir.mapper.model;

public class FHIRIdentifier {

    private static String prefix = "urn:";

    private String internalForm;

    public FHIRIdentifier(String identifier) {
        this.internalForm = sanitize(identifier);
    }

    public String getInternalForm() {
        return internalForm;
    }

    public String getExternalForm() {
        return prefix + internalForm;
    }

    private String sanitize(String identifier) {
        if (identifier.startsWith(prefix)) {
            return identifier.substring(prefix.length());
        } else {
            return identifier;
        }
    }
}
