package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.RelatedObservation;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_ENC_TYPE_LAB_RESULT;
import static org.openmrs.module.fhir.mapper.model.ObservationType.*;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.addReferenceCodes;

@Component("FHIRObservationMapper")
public class ObservationMapper implements EmrObsResourceHandler {

    private ObservationValueMapper observationValueMapper;
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    public ObservationMapper(ObservationValueMapper observationValueMapper, IdMappingsRepository idMappingsRepository) {
        this.observationValueMapper = observationValueMapper;
        this.idMappingsRepository = idMappingsRepository;
    }

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        if (obs.isOfType(HISTORY_AND_EXAMINATION) || obs.isOfType(VISIT_DIAGNOSES) || obs.isOfType(FAMILY_HISTORY)) {
            return false;
        }
        String encounterType = observation.getEncounter().getEncounterType().getName();
        return !MRS_ENC_TYPE_LAB_RESULT.equals(encounterType);
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<EmrResource> result = new ArrayList<>();
        if (null != obs) {
            result  = mapToFhirObservation(obs, fhirEncounter, systemProperties);
        }
        return result;
    }

    private EmrResource buildResource(Observation observation, Obs obs) {
        return new EmrResource(obs.getConcept().getName().getName(), asList(observation.getIdentifier()), observation);
    }

    public List<EmrResource> mapToFhirObservation(Obs observation, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<EmrResource> result = new ArrayList<>();
        Observation fhirObservation = createObservation(observation, fhirEncounter, systemProperties);
        EmrResource entry = buildResource(fhirObservation, observation);
        for (Obs member : observation.getGroupMembers()) {
            mapGroupMember(member, fhirEncounter, fhirObservation, result, systemProperties);
        }
        result.add(entry);
        return result;
    }

    private void mapGroupMember(Obs obs, Encounter fhirEncounter, Observation parentObservation, List<EmrResource> result, SystemProperties systemProperties) {
        Observation observation = createObservation(obs, fhirEncounter, systemProperties);
        EmrResource entry = buildResource(observation, obs);
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
        CodeableConcept observationName = addReferenceCodes(observation.getConcept(), idMappingsRepository);
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
