package org.openmrs.module.fhir.utils;


import org.hl7.fhir.instance.model.*;

import java.util.ArrayList;
import java.util.List;

public class FHIRFeedHelper {

    public static Composition getComposition(AtomFeed bundle) {
        Resource resource = identifyResource(bundle.getEntryList(), ResourceType.Composition);
        return resource != null ? (Composition) resource : null;
    }

    private static Resource identifyResource(List<AtomEntry<? extends Resource>> encounterBundleEntryList, ResourceType resourceType) {
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if (atomEntry.getResource().getResourceType().equals(resourceType)) {
                return atomEntry.getResource();
            }
        }
        return null;
    }

    public static Encounter getEncounter(AtomFeed bundle) {
        Resource resource = identifyResource(bundle.getEntryList(), ResourceType.Encounter);
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
}
