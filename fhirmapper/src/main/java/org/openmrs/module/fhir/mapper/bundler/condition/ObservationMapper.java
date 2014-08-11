package org.openmrs.module.fhir.mapper.bundler.condition;

import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.RelatedObservation;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Component("FHIRObservationMapper")
public class ObservationMapper {

    private ObservationValueMapper observationValueMapper;
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    public ObservationMapper(ObservationValueMapper observationValueMapper, IdMappingsRepository idMappingsRepository) {
        this.observationValueMapper = observationValueMapper;
        this.idMappingsRepository = idMappingsRepository;
    }

    public List<Observation> map(Encounter encounter, Obs observation) {
        List<Observation> result = new ArrayList<Observation>();
        Observation entry = observation(encounter, observation);
        if (isNotEmpty(observation.getGroupMembers())) {
            for (Obs part : observation.getGroupMembers()) {
                entry = mapRelatedObservation(encounter, part).mergeWith(entry);
                result.addAll(map(encounter, part));
            }
        }
        result.add(entry);
        return result;
    }

    private Observation observation(Encounter encounter, Obs observation) {
        Observation entry = new Observation();
        entry.setSubject(encounter.getSubject());
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
            return null;
        } else {
            CodeableConcept result = new CodeableConcept();
            Coding coding = result.addCoding();
            coding.setSystemSimple(mapping.getUri());
            coding.setDisplaySimple(observation.getConcept().getName().getName());
            coding.setCodeSimple(mapping.getExternalId());
            return result;
        }
    }

    private RelatedObservation mapRelatedObservation(Encounter encounter, Obs observation) {
        return new RelatedObservation(observation(encounter, observation));
    }
}
