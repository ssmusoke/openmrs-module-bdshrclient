package org.openmrs.module.fhir.mapper.bundler;


import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Resource;

import java.util.List;

public class EmrResource {
    private String resourceName;
    private List<Identifier> identifierList;
    private Resource resource;

    public EmrResource(String resourceName, List<Identifier> identifierList, Resource resource) {
        this.resourceName = resourceName;
        this.identifierList = identifierList;
        this.resource = resource;
    }

    public String getResourceName() {
        return resourceName;
    }

    public List<Identifier> getIdentifierList() {
        return identifierList;
    }

    public Resource getResource() {
        return resource;
    }

    public Identifier getIdentifier() {
        if ((identifierList != null) && !identifierList.isEmpty()) {
            return identifierList.get(0);
        }
        return null;
    }
}
