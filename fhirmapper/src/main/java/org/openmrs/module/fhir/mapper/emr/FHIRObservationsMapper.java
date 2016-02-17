package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.openmrs.*;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

import static org.openmrs.module.fhir.MRSProperties.LOCAL_CONCEPT_VERSION_PREFIX;
import static org.openmrs.module.fhir.utils.FHIRBundleHelper.findResourceByReference;

@Component
public class FHIRObservationsMapper implements FHIRResourceMapper {
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Autowired
    private FHIRObservationValueMapper resourceValueMapper;

    @Override
    public boolean canHandle(IResource resource) {
        return (resource instanceof Observation);
    }

    @Override
    public void map(IResource resource, EmrEncounter emrEncounter, ShrEncounterBundle shrEncounterBundle, SystemProperties systemProperties) {
        Observation observation = (Observation) resource;
        Obs result = mapObs(shrEncounterBundle, emrEncounter, observation);
        if (result == null) return;
        emrEncounter.addObs(result);
    }

    public Obs mapObs(ShrEncounterBundle encounterComposition, EmrEncounter emrEncounter, Observation observation) {
        final ca.uhn.fhir.model.dstu2.resource.Encounter shrEncounter = FHIRBundleHelper.getEncounter(encounterComposition.getBundle());
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
            mapRelatedObservations(encounterComposition, observation, result, emrEncounter);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void mapRelatedObservations(ShrEncounterBundle encounterComposition, Observation observation, Obs obs, EmrEncounter emrEncounter) throws ParseException {
        for (Observation.Related component : observation.getRelated()) {
            Obs member;
            Observation relatedObs = (Observation) findResourceByReference(encounterComposition.getBundle(), component.getTarget());
            member = mapObs(encounterComposition, emrEncounter, relatedObs);
            if (member != null) {
                obs.addGroupMember(member);
                emrEncounter.addObs(member);
            }
        }
    }

    private boolean isLocallyCreatedConcept(Concept concept) {
        return concept.getVersion() != null && concept.getVersion().startsWith(LOCAL_CONCEPT_VERSION_PREFIX);
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
        return omrsConceptLookup.findOrCreateLocalConceptByCodings(observationName.getCoding(), facilityId, ConceptClass.MISC_UUID, ConceptDatatype.TEXT_UUID);
    }
}
