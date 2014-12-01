package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.*;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.model.FHIRIdentifier;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;

@Component
public class CompositionBundle {

    @Autowired
    private EncounterMapper encounterMapper;

    @Autowired
    private List<EmrObsResourceHandler> obsResourceHandlers;

    @Autowired
    private List<EmrOrderResourceHandler> orderResourceHandlers;

    public AtomFeed create(org.openmrs.Encounter emrEncounter, SystemProperties systemProperties) {
        AtomFeed atomFeed = new AtomFeed();
        Encounter fhirEncounter = encounterMapper.map(emrEncounter, systemProperties);
        Composition composition = createComposition(emrEncounter, fhirEncounter);

        atomFeed.setTitle("Encounter");
        atomFeed.setUpdated(composition.getDateSimple());
        atomFeed.setId(new FHIRIdentifier(UUID.randomUUID().toString()).getExternalForm());
        final EmrResource encounterResource = new EmrResource("Encounter", fhirEncounter.getIdentifier(), fhirEncounter);
        addResourceSectionToComposition(composition, encounterResource);
        addAtomEntry(atomFeed, new EmrResource("Composition", asList(composition.getIdentifier()), composition));
        addAtomEntry(atomFeed, encounterResource);

        final Set<Obs> observations = emrEncounter.getObsAtTopLevel(false);
        for (Obs obs : observations) {
            for (EmrObsResourceHandler handler : obsResourceHandlers) {
                if (handler.handles(obs)) {
                    List<EmrResource> mappedResources = handler.map(obs, fhirEncounter);
                    if (CollectionUtils.isNotEmpty(mappedResources)) {
                        addResourcesToBundle(mappedResources, composition, atomFeed);
                    }
                }
            }
        }

        Set<org.openmrs.Order> orders = emrEncounter.getOrders();
        for (org.openmrs.Order order : orders) {
            for (EmrOrderResourceHandler handler : orderResourceHandlers) {
                if (handler.handles(order)) {
                    List<EmrResource> mappedResources = handler.map(order, fhirEncounter, atomFeed);
                    if (CollectionUtils.isNotEmpty(mappedResources)) {
                        addResourcesToBundle(mappedResources, composition, atomFeed);
                    }
                }
            }
        }

        return atomFeed;
    }

    private void addResourcesToBundle(List<EmrResource> mappedResources, Composition composition, AtomFeed atomFeed) {
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
        resourceEntry.setId(new FHIRIdentifier(resource.getIdentifier().getValueSimple()
        ).getExternalForm());
        resourceEntry.setAuthorName(FHIRProperties.FHIR_AUTHOR);
        resourceEntry.setUpdated(new DateAndTime(new Date()));
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
