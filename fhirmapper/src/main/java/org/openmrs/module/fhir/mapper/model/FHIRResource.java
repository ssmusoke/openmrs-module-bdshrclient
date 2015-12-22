package org.openmrs.module.fhir.mapper.model;


import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;

import java.util.List;

public class FHIRResource {
    private String resourceName;
    private List<IdentifierDt> identifierList;
    private IResource resource;

    public FHIRResource(String resourceName, List<IdentifierDt> identifierList, IResource resource) {
        this.resourceName = resourceName;
        this.identifierList = identifierList;
        this.resource = resource;
    }

    public String getResourceName() {
        return resourceName;
    }

    public List<IdentifierDt> getIdentifierList() {
        return identifierList;
    }

    public IResource getResource() {
        return resource;
    }

    public IdentifierDt getIdentifier() {
        if ((identifierList != null) && !identifierList.isEmpty()) {
            return identifierList.get(0);
        }
        return null;
    }
}
