package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Decimal;
import org.hl7.fhir.instance.model.Observation;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.String_;
import org.hl7.fhir.instance.model.Type;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.utils.OMRSHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;

@Component
public class FHIRObservationsMapper implements FHIRResource {

    @Autowired
    ConceptService conceptService;

    @Autowired
    IdMappingsRepository idMappingsRepository;

    @Autowired
    OMRSHelper omrsHelper;

    @Override
    public boolean canHandle(Resource resource) {
        return (resource instanceof Observation);
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, HashMap<String, String> processedList) {
        Observation observation = (Observation) resource;
        if (isAlreadyProcessed(observation, processedList))
            return;
        Obs result = mapObs(feed, processedList, observation, newEmrEncounter);
        if (result == null) return;
        newEmrEncounter.addObs(result);
    }

    private void mapRelatedObservations(AtomFeed feed, Observation observation, HashMap<String, String> processedList, Obs obs, Encounter emrEncounter) throws ParseException {
        for (Observation.ObservationRelatedComponent component : observation.getRelated()) {
            Obs member;
            Observation relatedObs = (Observation) findResourceByReference(feed, component.getTarget());
            if (isAlreadyProcessed(relatedObs, processedList)) {
                member = findObsInEncounter(emrEncounter, processedList.get(relatedObs.getIdentifier().getValueSimple()));
            } else {
                member = mapObs(feed, processedList, relatedObs, emrEncounter);
            }
            if (member != null) {
                obs.addGroupMember(member);
            }
        }
    }

    private Obs mapObs(AtomFeed feed, HashMap<String, String> processedList, Observation observation, Encounter emrEncounter) {
        Obs result = new Obs();
        Concept concept = mapConcept(observation);
        if (concept == null) return null;
        result.setConcept(concept);
        try {
            mapValue(observation, result);
            processedList.put(observation.getIdentifier().getValueSimple(), result.getUuid());
            mapRelatedObservations(feed, observation, processedList, result, emrEncounter);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Obs findObsInEncounter(Encounter emrEncounter, String uuid) {
        for (Obs obs : emrEncounter.getAllObs()) {
            if (uuid.equals(obs.getUuid())) {
                return obs;
            }
        }
        return null;
    }

    private boolean isAlreadyProcessed(Observation observation, HashMap<String, String> processedList) {
        return processedList.containsKey(observation.getIdentifier().getValueSimple());
    }

    private void mapValue(Observation relatedObs, Obs result) throws ParseException {
        Type value = relatedObs.getValue();
        if (null != value) {
            if (value instanceof String_) {
                result.setValueAsString(((String_) value).getValue());
            } else if (value instanceof Decimal) {
                result.setValueNumeric(((Decimal) value).getValue().doubleValue());
            } else if (value instanceof CodeableConcept) {
                List<Coding> codings = ((CodeableConcept) value).getCoding();
                /* TODO: The last element of codings is the concept. Make this more explicit*/
                Concept concept = omrsHelper.findConcept(codings);
                if (concept != null) {
                    result.setValueCoded(concept);
                } else {
                    result.setValueCoded(conceptService.getConceptByName(codings.get(codings.size() - 1).getDisplaySimple()));
                }
            }
        }
    }

    private Concept mapConcept(Observation observation) {
        Coding observationName = observation.getName().getCoding().get(0);
        if (null != observationName) {
            String externalId = observationName.getCodeSimple();
            if (externalId != null) {
                IdMapping mapping = idMappingsRepository.findByExternalId(externalId);
                if (mapping != null){
                    Concept concept = conceptService.getConceptByUuid(mapping.getInternalId());
                    if (concept != null) return concept;
                }
            }
            return conceptService.getConceptByName(observationName.getDisplaySimple());
        }
        return null;
    }
}
