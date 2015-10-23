package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.resource.*;
import org.openmrs.*;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

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

    public Obs mapObs(Bundle bundle, Encounter emrEncounter, Observation observation) {
        final ca.uhn.fhir.model.dstu2.resource.Encounter shrEncounter = getEncounter(bundle);
        String facilityId = new EntityReference().parse(Location.class, shrEncounter.getServiceProvider().getReference().getValue());
        Concept concept = mapConcept(observation, facilityId);
        if (concept == null) return null;
        Obs result = new Obs();
        result.setConcept(concept);
        try {
            if (isLocallyCreatedConcept(concept)) {
                mapValueAsString(observation, result);
            } else {
                mapValue(observation, result);
            }
            mapRelatedObservations(bundle, observation, result, emrEncounter);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean isLocallyCreatedConcept(Concept concept) {
        return concept.getVersion() != null && concept.getVersion().startsWith(Constants.LOCAL_CONCEPT_VERSION_PREFIX);
    }

    private void mapValueAsString(Observation relatedObs, Obs result) throws ParseException {
        IDatatype value = relatedObs.getValue();
        if (value != null)
            result.setValueAsString(ObservationValueConverter.convertToText(value));
    }


    private void mapValue(Observation relatedObs, Obs result) throws ParseException {
        IDatatype value = relatedObs.getValue();
        resourceValueMapper.map(value, result);
    }

    private Concept mapConcept(Observation observation, String facilityId) {
        CodeableConceptDt observationName = observation.getCode();
        if (observationName.getCoding() != null && observationName.getCoding().isEmpty()) {
            return null;
        }
        Concept concept = omrsConceptLookup.findConceptByCode(observationName.getCoding());
        if (concept != null) {
            return concept;
        }

        return omrsConceptLookup.findConceptByCodings(observationName.getCoding(), facilityId);
    }
}
