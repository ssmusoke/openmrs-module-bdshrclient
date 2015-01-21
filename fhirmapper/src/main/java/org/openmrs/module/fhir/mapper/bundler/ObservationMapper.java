package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.RelatedObservation;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_ENC_TYPE_LAB_RESULT;
import static org.openmrs.module.fhir.mapper.model.ObservationType.*;

@Component("FHIRObservationMapper")
public class ObservationMapper implements EmrObsResourceHandler {

    private ObservationValueMapper observationValueMapper;
    private IdMappingsRepository idMappingsRepository;
    private final CodableConceptService codableConceptService;

    @Autowired
    public ObservationMapper(ObservationValueMapper observationValueMapper, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
        this.observationValueMapper = observationValueMapper;
        this.idMappingsRepository = idMappingsRepository;
        this.codableConceptService = codableConceptService;
    }

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        if (isNotOfKnownTypes(obs)) {
            return false;
        }
        String encounterType = observation.getEncounter().getEncounterType().getName();
        return !MRS_ENC_TYPE_LAB_RESULT.equals(encounterType);
    }

    private boolean isNotOfKnownTypes(CompoundObservation obs) {
        return obs.isOfType(HISTORY_AND_EXAMINATION)
                || obs.isOfType(VISIT_DIAGNOSES)
                || obs.isOfType(FAMILY_HISTORY)
                || obs.isOfType(IMMUNIZATION)
                || obs.isOfType(PROCEDURES);
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> result = new ArrayList<>();
        if (null != obs) {
            result  = mapToFhirObservation(obs, fhirEncounter, systemProperties);
        }
        return result;
    }

    private FHIRResource buildResource(Observation observation, Obs obs) {
        return new FHIRResource(obs.getConcept().getName().getName(), asList(observation.getIdentifier()), observation);
    }

    public List<FHIRResource> mapToFhirObservation(Obs observation, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> result = new ArrayList<>();
        Observation fhirObservation = createObservation(observation, fhirEncounter, systemProperties);
        FHIRResource entry = buildResource(fhirObservation, observation);
        for (Obs member : observation.getGroupMembers()) {
            mapGroupMember(member, fhirEncounter, fhirObservation, result, systemProperties);
        }
        result.add(entry);
        return result;
    }

    private void mapGroupMember(Obs obs, Encounter fhirEncounter, Observation parentObservation, List<FHIRResource> result, SystemProperties systemProperties) {
        Observation observation = createObservation(obs, fhirEncounter, systemProperties);
        FHIRResource entry = buildResource(observation, obs);
        mapRelatedObservation(observation).mergeWith(parentObservation);
        for (Obs member : obs.getGroupMembers()) {
            mapGroupMember(member, fhirEncounter, observation, result, systemProperties);
        }
        result.add(entry);
    }

    private Observation createObservation(Obs observation, Encounter fhirEncounter, SystemProperties systemProperties) {
        Observation entry = new Observation();
        entry.setSubject(fhirEncounter.getSubject());
        mapName(observation, entry);
        entry.setStatusSimple(Observation.ObservationStatus.final_);
        entry.setReliabilitySimple(Observation.ObservationReliability.ok);
        entry.setIdentifier(new Identifier().setValueSimple(new EntityReference().build(Obs.class, systemProperties, observation.getUuid())));
        mapValue(observation, entry);
        return entry;
    }

    private void mapValue(Obs observation, Observation entry) {
        Type value = observationValueMapper.map(observation);
        if (null != value) {
            entry.setValue(value);
        }
    }

    private void mapName(Obs observation, Observation entry) {
        CodeableConcept name = buildName(observation);
        if (null != name) {
            entry.setName(name);
        }
    }

    private CodeableConcept buildName(Obs observation) {
        if (null == observation.getConcept()) {
            return null;
        }
        CodeableConcept observationName = codableConceptService.addTRCoding(observation.getConcept(), idMappingsRepository);
        if (CollectionUtils.isEmpty(observationName.getCoding())) {
            Coding coding = observationName.addCoding();
            coding.setDisplaySimple(observation.getConcept().getName().getName());
        }
        return observationName;
    }

    private RelatedObservation mapRelatedObservation(Observation observation) {
        return new RelatedObservation(observation);
    }

    //TODO : how do we identify this individual?
    protected ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
