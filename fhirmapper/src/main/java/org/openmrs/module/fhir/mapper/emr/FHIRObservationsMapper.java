package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;

@Component
public class FHIRObservationsMapper implements FHIRResourceMapper {

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Autowired
    private FHIRObservationValueMapper resourceValueMapper;

    @Override
    public boolean canHandle(IResource resource) {
        return (resource instanceof Observation);
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter) {
        Observation observation = (Observation) resource;
        Obs result = mapObs(bundle, newEmrEncounter, observation);
        if (result == null) return;
        newEmrEncounter.addObs(result);
    }

    private void mapRelatedObservations(Bundle bundle, Observation observation, Obs obs, Encounter emrEncounter) throws ParseException {
        for (Observation.Related component : observation.getRelated()) {
            Obs member;
            Observation relatedObs = (Observation) findResourceByReference(bundle, asList(component.getTarget()));
            member = mapObs(bundle, emrEncounter, relatedObs);
            if (member != null) {
                obs.addGroupMember(member);
                emrEncounter.addObs(member);
            }
        }
    }

    Obs mapObs(Bundle bundle, Encounter emrEncounter, Observation observation) {
        Concept concept = mapConcept(observation);
        if (concept == null) return null;
        Obs result = new Obs();
        result.setConcept(concept);
        try {
            mapValue(observation, result);
            mapRelatedObservations(bundle, observation, result, emrEncounter);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void mapValue(Observation relatedObs, Obs result) throws ParseException {
        IDatatype value = relatedObs.getValue();
        resourceValueMapper.map(value, result);
    }

    private Concept mapConcept(Observation observation) {
        CodeableConceptDt observationName = observation.getCode();
        if (observationName.getCoding() != null && observationName.getCoding().isEmpty()) {
            return null;
        }
        Concept concept = omrsConceptLookup.findConceptByCode(observationName.getCoding());
        if (concept == null) {
            return conceptService.getConceptByName(observationName.getCoding().get(0).getDisplay());
        }
        return concept;
    }
}
