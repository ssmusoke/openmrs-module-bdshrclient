package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Observation;
import org.hl7.fhir.instance.model.Type;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.RelatedObservation;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_ENC_TYPE_LAB_RESULT;
import static org.openmrs.module.fhir.mapper.model.ObservationType.FAMILY_HISTORY;
import static org.openmrs.module.fhir.mapper.model.ObservationType.HISTORY_AND_EXAMINATION;
import static org.openmrs.module.fhir.mapper.model.ObservationType.VISIT_DIAGNOSES;

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
    public boolean handles(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        if (obs.isOfType(HISTORY_AND_EXAMINATION) || obs.isOfType(VISIT_DIAGNOSES) || obs.isOfType(FAMILY_HISTORY)) {
            return false;
        }
        String encounterType = observation.getEncounter().getEncounterType().getName();
        return !MRS_ENC_TYPE_LAB_RESULT.equals(encounterType);
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> result = new ArrayList<EmrResource>();
        if (null != obs) {
            List<Observation> observations = mapToFhirObservation(obs, fhirEncounter);
            for (Observation observation : observations) {
                result.add(buildResource(observation, obs));
            }
        }
        return result;
    }

    private EmrResource buildResource(Observation observation, Obs obs) {
        return new EmrResource(obs.getConcept().getName().getName(), asList(observation.getIdentifier()), observation);
    }

    public List<Observation> mapToFhirObservation(Obs observation, Encounter fhirEncounter) {
        List<Observation> result = new ArrayList<Observation>();
        Observation entry = createObservation(observation, fhirEncounter);
        for (Obs member : observation.getGroupMembers()) {
            mapGroupMember(member, fhirEncounter, entry, result);
        }
        result.add(entry);
        return result;
    }

    private void mapGroupMember(Obs obs, Encounter fhirEncounter, Observation parentObservation, List<Observation> result) {
        Observation observation = createObservation(obs, fhirEncounter);
        parentObservation = mapRelatedObservation(observation).mergeWith(parentObservation);
        for (Obs member : obs.getGroupMembers()) {
            mapGroupMember(member, fhirEncounter, observation, result);
        }
        result.add(observation);
    }

    private Observation createObservation(Obs observation, Encounter fhirEncounter) {
        Observation entry = new Observation();
        entry.setSubject(fhirEncounter.getSubject());
        mapName(observation, entry);
        entry.setStatusSimple(Observation.ObservationStatus.final_);
        entry.setReliabilitySimple(Observation.ObservationReliability.ok);
        entry.setIdentifier(new Identifier().setValueSimple(observation.getUuid()));
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
        IdMapping mapping = idMappingsRepository.findByInternalId(observation.getConcept().getUuid());
        if (null == mapping) {
            CodeableConcept result = new CodeableConcept();
            Coding coding = result.addCoding();
            coding.setDisplaySimple(observation.getConcept().getName().getName());
            return result;
        } else {
            CodeableConcept result = new CodeableConcept();
            Coding coding = result.addCoding();
            coding.setSystemSimple(mapping.getUri());
            coding.setDisplaySimple(observation.getConcept().getName().getName());
            coding.setCodeSimple(mapping.getExternalId());
            return result;
        }
    }

    private RelatedObservation mapRelatedObservation(Observation observation) {
        return new RelatedObservation(observation);
    }
}
