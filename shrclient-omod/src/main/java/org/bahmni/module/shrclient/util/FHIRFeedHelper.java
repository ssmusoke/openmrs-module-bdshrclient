package org.bahmni.module.shrclient.util;


import org.hl7.fhir.instance.model.*;

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
}
