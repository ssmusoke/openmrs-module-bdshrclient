package org.openmrs.module.fhir;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.module.fhir.mapper.bundler.FHIRResource;

import java.util.ArrayList;
import java.util.List;

public class TestFhirFeedHelper {

    public static List<IResource> getResourceByType(Bundle bundle, String resourceName) {
        List<IResource> resources = new ArrayList<>();
        List<Bundle.Entry> entryList = bundle.getEntry();
        for (Bundle.Entry bundleEntry : entryList) {
            IResource resource = bundleEntry.getResource();
            if (resource.getResourceName().equals(resourceName)) {
                resources.add(resource);
            }
        }
        return resources;
    }

    public static FHIRResource getResourceByReference(ResourceReferenceDt reference, List<FHIRResource> FHIRResources) {
        for (FHIRResource FHIRResource : FHIRResources) {
            if(FHIRResource.getIdentifier().getValue().equals(reference.getReference().getValue())) {
                return FHIRResource;
            }
        }
        return null;
    }

    public static FHIRResource getFirstResourceByType(String resourceName, List<FHIRResource> FHIRResources) {
        for (FHIRResource FHIRResource : FHIRResources) {
            if(resourceName.equals(FHIRResource.getResource().getResourceName())) {
                return FHIRResource;
            }
        }
        return null;
    }

    public static ArrayList<FHIRResource> getResourceByType(String resourceName, List<FHIRResource> fhirResources) {
        ArrayList<FHIRResource> mappedFhirResources = new ArrayList<>();
        for (FHIRResource fhirResource : fhirResources) {
            if(resourceName.equals(fhirResource.getResource().getResourceName())) {
                mappedFhirResources.add(fhirResource);
            }
        }
        return mappedFhirResources;
    }
}
