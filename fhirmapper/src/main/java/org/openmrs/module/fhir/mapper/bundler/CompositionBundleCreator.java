package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class CompositionBundleCreator {

    @Autowired
    private EncounterMapper encounterMapper;

    @Autowired
    private List<EmrResourceHandler> resourceHandlers;

    public AtomFeed compose(org.openmrs.Encounter emrEncounter) {
        AtomFeed atomFeed = new AtomFeed();
        Encounter fhirEncounter = encounterMapper.map(emrEncounter);
        Composition composition = createComposition(emrEncounter, fhirEncounter);

        atomFeed.setTitle("Encounter");
        atomFeed.setUpdated(composition.getDateSimple());
        atomFeed.setId(UUID.randomUUID().toString());
        final EmrResource encounterResource = new EmrResource("Encounter", fhirEncounter.getIdentifier(), fhirEncounter);
        addResourceSectionToComposition(composition, encounterResource);
        addAtomEntry(atomFeed, new EmrResource("Composition", Arrays.asList(composition.getIdentifier()), composition));
        addAtomEntry(atomFeed, encounterResource);

        final Set<Obs> observations = emrEncounter.getObsAtTopLevel(false);
        for (Obs obs : observations) {
            for (EmrResourceHandler handler : resourceHandlers) {
                if (handler.handles(obs)) {
                    addResourcesToBundle(fhirEncounter, obs, handler, composition, atomFeed);
                }

            }
        }

        return atomFeed;
    }

    private void addResourcesToBundle(Encounter fhirEncounter, Obs obs, EmrResourceHandler handler, Composition composition, AtomFeed atomFeed) {
        final List<EmrResource> mappedResources = handler.map(obs, fhirEncounter);
        for (EmrResource mappedResource : mappedResources) {
            addResourceSectionToComposition(composition, mappedResource);
            addAtomEntry(atomFeed, mappedResource);
        }
    }

    private void addResourceSectionToComposition(Composition composition, EmrResource resource) {
        String resourceId = resource.getIdentifier().getValueSimple();
        ResourceReference conditionRef = new ResourceReference();
        conditionRef.setReferenceSimple(resourceId);
        conditionRef.setDisplaySimple(resource.getResourceName());
        composition.addSection().setContent(conditionRef);
    }

    private void addAtomEntry(AtomFeed atomFeed, EmrResource resource) {
        AtomEntry resourceEntry = new AtomEntry();
        resourceEntry.setId(resource.getIdentifier().getValueSimple());
        resourceEntry.setTitle(resource.getResourceName());
        resourceEntry.setResource(resource.getResource());
        atomFeed.addEntry(resourceEntry);
    }

    private Composition createComposition(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        DateAndTime encounterDateTime = new DateAndTime(openMrsEncounter.getEncounterDatetime());
        Composition composition = new Composition().setDateSimple(encounterDateTime);
        composition.setEncounter(encounter.getIndication());
        composition.setStatus(new Enumeration<Composition.CompositionStatus>(Composition.CompositionStatus.final_));
        composition.setIdentifier(new Identifier().setValueSimple("Encounter - " + openMrsEncounter.getUuid()));

        return composition;
    }
}
