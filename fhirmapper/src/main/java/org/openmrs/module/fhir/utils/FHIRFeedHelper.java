package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FHIRFeedHelper {

    public static Composition getComposition(Bundle bundle) {
        IResource resource = identifyResource(bundle.getEntry(), "Composition");
        return resource != null ? (Composition) resource : null;
    }

    public static IResource identifyResource(List<Bundle.Entry> bundleEntryList, String resourceName) {
        for (Bundle.Entry bundleEntry : bundleEntryList) {
            if (bundleEntry.getResource().getResourceName().equals(resourceName)) {
                return bundleEntry.getResource();
            }
        }
        return null;
    }

    public static List<IResource> identifyResources(List<Bundle.Entry> bundleEntryList, String resourceName) {
        List<IResource> resources = new ArrayList<>();
        for (Bundle.Entry bundleEntry : bundleEntryList) {
            if (bundleEntry.getResource().getResourceName().equals(resourceName)) {
                resources.add(bundleEntry.getResource());
            }
        }
        return resources;
    }

    public static Encounter getEncounter(Bundle bundle) {
        IResource resource = findResourceByReference(bundle, getComposition(bundle).getEncounter());
        return resource != null ? (Encounter) resource : null;
    }

    public static IResource findResourceByReference(Bundle bundle, ResourceReferenceDt reference) {
        for (Bundle.Entry bundleEntry : bundle.getEntry()) {
            if (StringUtils.equals(bundleEntry.getResource().getId().getValue(), reference.getReference().getValue())) {
                return bundleEntry.getResource();
            }
        }
        return null;
    }
}
