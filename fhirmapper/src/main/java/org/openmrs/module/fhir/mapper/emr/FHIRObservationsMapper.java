package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;

@Component
public class FHIRObservationsMapper implements FHIRResourceMapper {

    @Autowired
    ConceptService conceptService;

    @Autowired
    IdMappingsRepository idMappingsRepository;

    @Autowired
    OMRSConceptLookup omrsConceptLookup;

    @Autowired
    FHIRResourceValueMapper resourceValueMapper;

    @Override
    public boolean canHandle(Resource resource) {
        return (resource instanceof Observation);
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        Observation observation = (Observation) resource;
        if (isAlreadyProcessed(observation, processedList))
            return;
        Obs result = mapObs(feed, newEmrEncounter, observation, processedList);
        if (result == null) return;
        newEmrEncounter.addObs(result);
    }

    private void mapRelatedObservations(AtomFeed feed, Observation observation, Map<String, List<String>> processedList, Obs obs, Encounter emrEncounter) throws ParseException {
        for (Observation.ObservationRelatedComponent component : observation.getRelated()) {
            Obs member;
            Observation relatedObs = (Observation) findResourceByReference(feed, component.getTarget());
            if (isAlreadyProcessed(relatedObs, processedList)) {
                member = findObsInEncounter(emrEncounter, processedList.get(relatedObs.getIdentifier().getValueSimple()));
            } else {
                member = mapObs(feed, emrEncounter, relatedObs, processedList);
            }
            if (member != null) {
                obs.addGroupMember(member);
            }
        }
    }

    Obs mapObs(AtomFeed feed, Encounter emrEncounter, Observation observation, Map<String, List<String>> processedList) {
        Concept concept = mapConcept(observation);
        if (concept == null) return null;
        Obs result = new Obs();
        result.setConcept(concept);
        try {
            mapValue(observation, result);
            processedList.put(observation.getIdentifier().getValueSimple(), Arrays.asList(result.getUuid()));
            mapRelatedObservations(feed, observation, processedList, result, emrEncounter);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Obs findObsInEncounter(Encounter emrEncounter, List<String> processedItems) {
        for (Obs obs : emrEncounter.getAllObs()) {
            for (String processedItem : processedItems) {
                if (processedItem.equals(obs.getUuid())) {
                    return obs;
                }
            }
        }
        return null;
    }

    private boolean isAlreadyProcessed(Observation observation, Map<String, List<String>> processedList) {
        return processedList.containsKey(observation.getIdentifier().getValueSimple());
    }

    private void mapValue(Observation relatedObs, Obs result) throws ParseException {
        Type value = relatedObs.getValue();
        resourceValueMapper.map(value, result);
    }

    private Concept mapConcept(Observation observation) {
        CodeableConcept observationName = observation.getName();
        if (observationName.getCoding().isEmpty()) {
            return null;
        }
        Concept concept = omrsConceptLookup.findConcept(observationName.getCoding());
        if(concept == null) {
            return conceptService.getConceptByName(observationName.getCoding().get(0).getDisplaySimple());
        }
        return concept;
    }
}
