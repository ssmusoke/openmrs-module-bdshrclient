package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.primitive.IdDt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;

public class FHIRBundleHelper {

    public static Composition getComposition(Bundle bundle) {
        IResource resource = identifyFirstResourceWithName(bundle, "Composition");
        return resource != null ? (Composition) resource : null;
    }

    public static IResource identifyFirstResourceWithName(Bundle bundle, String resourceName) {
        for (Bundle.Entry bundleEntry : bundle.getEntry()) {
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

    public static List<IResource> identifyResourcesByName(Bundle bundle, String resourceName) {
        List<IResource> resources = new ArrayList<>();
        for (Bundle.Entry bundleEntry : bundle.getEntry()) {
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
        List<IResource> matchedResources = findResourcesByReference(bundle, asList(reference));
        return matchedResources != null && !matchedResources.isEmpty() ? matchedResources.get(0) : null;
    }


    public static IResource findResourceByFirstReference(Bundle bundle, List<ResourceReferenceDt> references) {
        List<IResource> matchedResources = findResourcesByReference(bundle, references);
        return matchedResources != null && !matchedResources.isEmpty() ? matchedResources.get(0) : null;
    }

    public static List<IResource> findResourcesByReference(Bundle bundle, List<ResourceReferenceDt> references) {
        ArrayList<IResource> matchedResources = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            for (ResourceReferenceDt reference : references) {
                IdDt resourceReference = reference.getReference();
                IResource entryResource = entry.getResource();
                IdDt entryResourceId = entryResource.getId();
                boolean hasFullUrlDefined = !org.apache.commons.lang3.StringUtils.isBlank(entry.getFullUrl());
                if (resourceReference.hasResourceType() && entryResourceId.hasResourceType()
                        && entryResourceId.getValue().equals(resourceReference.getValue())) {
                    matchedResources.add(entryResource);
                } else if (entryResourceId.getIdPart().equals(resourceReference.getIdPart())) {
                    matchedResources.add(entryResource);
                } else if (hasFullUrlDefined) {
                    if (entry.getFullUrl().endsWith(resourceReference.getIdPart())) {
                        matchedResources.add(entryResource);
                    }
                }
            }
        }
        return matchedResources.isEmpty() ? null : matchedResources;
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
            IResource resourceForReference = findResourceByFirstReference(bundle, section.getEntry());
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
