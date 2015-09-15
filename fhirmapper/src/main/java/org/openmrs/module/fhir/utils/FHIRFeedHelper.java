package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.primitive.IdDt;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;

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

    public static List<IResource> identifyTopLevelResources(Bundle bundle) {
        List<IResource> compositionRefResources = getCompositionRefResources(bundle);
        HashSet<ResourceReferenceDt> childRef = getChildReferences(compositionRefResources);

        List<IResource> topLevelResources = new ArrayList<>();

        for (IResource compositionRefResource : compositionRefResources) {
            if (!isChildReference(childRef, compositionRefResource.getId().getValue())) {
                topLevelResources.add(compositionRefResource);
            }
        }
        return topLevelResources;
    }

    public static List<IResource> identifyResourcesByName(List<Bundle.Entry> bundleEntryList, String resourceName) {
        List<IResource> resources = new ArrayList<>();
        for (Bundle.Entry bundleEntry : bundleEntryList) {
            if (bundleEntry.getResource().getResourceName().equals(resourceName)) {
                resources.add(bundleEntry.getResource());
            }
        }
        return resources;
    }

    public static Encounter getEncounter(Bundle bundle) {
        IResource resource = findResourceByReference(bundle, asList(getComposition(bundle).getEncounter()));
        return resource != null ? (Encounter) resource : null;
    }


    public static IResource findResourceByReference(Bundle bundle, List<ResourceReferenceDt> references) {
        for (Bundle.Entry entry : bundle.getEntry()) {
            for (ResourceReferenceDt reference : references) {
                IdDt resourceReference = reference.getReference();
                    boolean hasFullUrlDefined = !StringUtils.isBlank(entry.getFullUrl());
                    IResource entryResource = entry.getResource();

                    if (hasFullUrlDefined) {
                        if (entry.getFullUrl().equals(resourceReference.getValue())) {
                            return entryResource;
                        }
                    } else if (resourceReference.hasResourceType()) {
                        if (entryResource.getId().getValue().equals(resourceReference.getValue())) {
                            return entryResource;
                        }
                    } else {
                        if (entryResource.getId().getIdPart().equals(resourceReference.getIdPart())) {
                            return entryResource;
                        }
                    }
            }
        }
        return null;
    }

    private static boolean isChildReference(HashSet<ResourceReferenceDt> childReferenceDts, String resourceRef) {
        for (ResourceReferenceDt childRef : childReferenceDts) {
            if (!childRef.getReference().isEmpty() && childRef.getReference().getValue().equals(resourceRef)) {
                return true;
            }
        }
        return false;
    }

    private static List<IResource> getCompositionRefResources(Bundle bundle) {
        List<IResource> resources = new ArrayList<>();
        Composition composition = getComposition(bundle);
        for (Composition.Section section : composition.getSection()) {
            IResource resourceForReference = findResourceByReference(bundle, section.getEntry());
            if (!(resourceForReference instanceof Encounter)) {
                resources.add(resourceForReference);
            }
        }
        return resources;
    }

    private static HashSet<ResourceReferenceDt> getChildReferences(List<IResource> compositionRefResources) {
        List<ResourceReferenceDt> childResourceReferences = new ArrayList<>();
        for (IResource compositionRefResource : compositionRefResources) {
            childResourceReferences.addAll(compositionRefResource.getAllPopulatedChildElementsOfType(ResourceReferenceDt.class));
        }
        HashSet<ResourceReferenceDt> childRef = new HashSet<>();
        childRef.addAll(childResourceReferences);
        return childRef;
    }
}
