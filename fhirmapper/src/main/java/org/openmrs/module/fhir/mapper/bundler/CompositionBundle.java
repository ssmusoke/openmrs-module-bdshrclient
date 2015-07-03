package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.EntityReference;
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
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class CompositionBundle {

    public static final String FHIR_CONFIDENTIALITY_SYSTEM = "http://hl7.org/fhir/v3/Confidentiality";
    public static final String CONFIDENTIALITY_NORMAL = "N";
    @Autowired
    private EncounterMapper encounterMapper;

    @Autowired
    private List<EmrObsResourceHandler> obsResourceHandlers;

    @Autowired
    private List<EmrOrderResourceHandler> orderResourceHandlers;

    public AtomFeed create(org.openmrs.Encounter emrEncounter, SystemProperties systemProperties) {
        AtomFeed atomFeed = new AtomFeed();
        Encounter fhirEncounter = encounterMapper.map(emrEncounter, systemProperties);
        Composition composition = createComposition(emrEncounter.getEncounterDatetime(), fhirEncounter, systemProperties);
        atomFeed.setTitle("Encounter");
        atomFeed.setAuthorUri(fhirEncounter.getServiceProvider().getReferenceSimple());
        atomFeed.setUpdated(composition.getDateSimple());
        atomFeed.setId(new FHIRIdentifier(UUID.randomUUID().toString()).getExternalForm());
        final FHIRResource encounterResource = new FHIRResource("Encounter", fhirEncounter.getIdentifier(), fhirEncounter);
        addResourceSectionToComposition(composition, encounterResource);
        addAtomEntry(atomFeed, new FHIRResource("Composition", asList(composition.getIdentifier()), composition), systemProperties);
        addAtomEntry(atomFeed, encounterResource, systemProperties);

        final Set<Obs> observations = emrEncounter.getObsAtTopLevel(false);
        for (Obs obs : observations) {
            for (EmrObsResourceHandler handler : obsResourceHandlers) {
                if (handler.canHandle(obs)) {
                    List<FHIRResource> mappedResources = handler.map(obs, fhirEncounter, systemProperties);
                    if (CollectionUtils.isNotEmpty(mappedResources)) {
                        addResourcesToBundle(mappedResources, composition, atomFeed, systemProperties);
                    }
                }
            }
        }

        Set<org.openmrs.Order> orders = emrEncounter.getOrders();
        for (org.openmrs.Order order : orders) {
            for (EmrOrderResourceHandler handler : orderResourceHandlers) {
                if (handler.canHandle(order)) {
                    List<FHIRResource> mappedResources = handler.map(order, fhirEncounter, atomFeed, systemProperties);
                    if (CollectionUtils.isNotEmpty(mappedResources)) {
                        addResourcesToBundle(mappedResources, composition, atomFeed, systemProperties);
                    }
                }
            }
        }

        return atomFeed;
    }

    private void addResourcesToBundle(List<FHIRResource> mappedResources, Composition composition, AtomFeed atomFeed, SystemProperties systemProperties) {
        for (FHIRResource mappedResource : mappedResources) {
            addResourceSectionToComposition(composition, mappedResource);
            addAtomEntry(atomFeed, mappedResource, systemProperties);
        }
    }

    //TODO: reference should be a relative URL
    private void addResourceSectionToComposition(Composition composition, FHIRResource resource) {
        String resourceId = resource.getIdentifier().getValueSimple();
        ResourceReference conditionRef = new ResourceReference();
        conditionRef.setReferenceSimple(resourceId);
        conditionRef.setDisplaySimple(resource.getResourceName());
        composition.addSection().setContent(conditionRef);
    }

    @SuppressWarnings("unchecked")
    private void addAtomEntry(AtomFeed atomFeed, FHIRResource resource, SystemProperties systemProperties) {
        AtomEntry resourceEntry = new AtomEntry();
        resourceEntry.setId(new FHIRIdentifier(resource.getIdentifier().getValueSimple()
        ).getExternalForm());
        resourceEntry.setUpdated(new DateAndTime(new Date()));
        resourceEntry.setTitle(resource.getResourceName());
        resourceEntry.setResource(resource.getResource());
        atomFeed.addEntry(resourceEntry);
    }

    private Composition createComposition(Date encounterDateTime, Encounter encounter, SystemProperties systemProperties) {
        DateAndTime encounterDateAndTime = new DateAndTime(encounterDateTime);
        Composition composition = new Composition().setDateSimple(encounterDateAndTime);
        composition.setEncounter(encounter.getIndication());
        composition.setStatus(new Enumeration<>(Composition.CompositionStatus.final_));
        composition.setIdentifier(new Identifier().setValueSimple(new EntityReference().build(Composition.class, systemProperties, UUID.randomUUID().toString())));
        composition.setSubject(encounter.getSubject());
        ResourceReference resourceReferenceAuthor = composition.addAuthor();
        resourceReferenceAuthor.setReferenceSimple(encounter.getServiceProvider().getReferenceSimple());
        composition.setConfidentiality(getConfidentialityCoding());
        return composition;
    }

    private Coding getConfidentialityCoding() {
        Coding coding = new Coding();
        coding.setCodeSimple(CONFIDENTIALITY_NORMAL);
        coding.setSystemSimple(FHIR_CONFIDENTIALITY_SYSTEM);
        return coding;
    }
}
