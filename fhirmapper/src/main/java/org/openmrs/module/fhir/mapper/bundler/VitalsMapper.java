package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Observation;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.mapper.model.ObservationType.VITALS;

@Component
public class VitalsMapper implements EmrResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    @Qualifier("FHIRObservationMapper")
    private ObservationMapper observationMapper;

    @Override
    public boolean handles(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(VITALS);
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter FHIREncounter) {
        List<EmrResource> result = new ArrayList<EmrResource>();
        if (null != obs) {
            List<Observation> observations = observationMapper.map(FHIREncounter, obs);
            for (Observation observation : observations) {
                result.add(buildResource(observation));
            }
        }
        return result;
    }

    private EmrResource buildResource(Observation observation) {
        return new EmrResource(FHIRProperties.FHIR_CONDITION_VITAL, asList(observation.getIdentifier()), observation);
    }
}
