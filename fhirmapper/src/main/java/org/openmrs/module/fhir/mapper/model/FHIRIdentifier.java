package org.openmrs.module.fhir.mapper.model;

public class FHIRIdentifier {

    private String prefix = "";

    private String internalForm;

    @Deprecated
    public FHIRIdentifier(String identifier, String prefix) {
        this.prefix = prefix;
        this.internalForm = sanitize(identifier);
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
