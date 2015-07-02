package org.openmrs.module.fhir;

import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceReference;
import org.hl7.fhir.instance.model.ResourceType;
import org.openmrs.module.fhir.mapper.bundler.FHIRResource;

import java.util.ArrayList;
import java.util.List;

public class TestFhirFeedHelper {

    public static List<Resource> getResourceByType(AtomFeed bundle, ResourceType resourceType) {
        List<Resource> resources = new ArrayList<>();
        List<AtomEntry<? extends Resource>> entryList = bundle.getEntryList();
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : entryList) {
            Resource resource = atomEntry.getResource();
            if (resource.getResourceType().equals(resourceType)) {
                resources.add(resource);
            }
        }
        return resources;
    }

    public static FHIRResource getResourceByReference(ResourceReference reference, List<FHIRResource> FHIRResources) {
        for (FHIRResource FHIRResource : FHIRResources) {
            if(FHIRResource.getIdentifier().getValueSimple().equals(reference.getReferenceSimple())) {
                return FHIRResource;
            }
        }
        return null;
    }

    public static FHIRResource getResourceByType(ResourceType type, List<FHIRResource> FHIRResources) {
        for (FHIRResource FHIRResource : FHIRResources) {
            if(type.equals(FHIRResource.getResource().getResourceType())) {
                return FHIRResource;
            }
        }
        return null;
    }
}
