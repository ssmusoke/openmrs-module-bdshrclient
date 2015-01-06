package org.openmrs.module.fhir.utils;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.*;

import java.util.ArrayList;
import java.util.List;

public class FHIRFeedHelper {

    public static Composition getComposition(AtomFeed bundle) {
        Resource resource = identifyResource(bundle.getEntryList(), ResourceType.Composition);
        return resource != null ? (Composition) resource : null;
    }

    public static Resource identifyResource(List<AtomEntry<? extends Resource>> encounterBundleEntryList, ResourceType resourceType) {
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if (atomEntry.getResource().getResourceType().equals(resourceType)) {
                return atomEntry.getResource();
            }
        }
        return null;
    }

    public static List<Resource> identifyResources(List<AtomEntry<? extends Resource>> encounterBundleEntryList, ResourceType resourceType) {
        List<Resource> resources = new ArrayList<>();
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if (atomEntry.getResource().getResourceType().equals(resourceType)) {
                resources.add(atomEntry.getResource());
            }
        }
        return resources;
    }

    public static Encounter getEncounter(AtomFeed bundle) {
        Resource resource = findResourceByReference(bundle, getComposition(bundle).getEncounter());
        return resource != null ? (Encounter) resource : null;
    }

    public static List<Condition> getConditions(AtomFeed bundle) {
        List<Condition> conditions = new ArrayList<Condition>();
        List<AtomEntry<? extends Resource>> entryList = bundle.getEntryList();
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : entryList) {
            Resource resource = atomEntry.getResource();
            if (resource.getResourceType().equals(ResourceType.Condition)) {
                conditions.add((Condition) resource);
            }
        }
        return conditions;
    }

    public static Resource findResourceByReference(AtomFeed bundle, ResourceReference reference) {
        for (AtomEntry<? extends Resource> atomEntry : bundle.getEntryList()) {
            if (StringUtils.equals(atomEntry.getId(), reference.getReferenceSimple())) {
                return atomEntry.getResource();
            }
        }
        return null;
    }
}
