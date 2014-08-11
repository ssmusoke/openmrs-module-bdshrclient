package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.mapper.model.Resources;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;


@Component
public class FHIRVitalsMapper implements FHIRResource {

    @Autowired
    ConceptService conceptService;

    @Autowired
    IdMappingsRepository idMappingsRepository;

    @Override
    public boolean handles(Resource resource) {
        return (resource instanceof Observation);
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter) {
        Observation vitals = (Observation) resource;
        Obs result = new Obs();
        boolean foundVitalsConcept = mapConcept(result);
        try {
            for (Obs obs : mapRelatedObservations(feed, vitals)) {
                if (foundVitalsConcept)
                    result.addGroupMember(obs);
                else
                    newEmrEncounter.addObs(obs);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (foundVitalsConcept)
            newEmrEncounter.addObs(result);
    }

    private List<Obs> mapRelatedObservations(AtomFeed feed, Observation vitals) throws ParseException {
        List<Obs> result = new ArrayList<Obs>();
        Resources resources = new Resources(feed);

        for (Observation.ObservationRelatedComponent component : vitals.getRelated()) {
            Observation relatedObs = (Observation) resources.find(component.getTarget());
            result.add(mapRelatedObservation(feed, relatedObs));
        }
        return result;
    }

    private Obs mapRelatedObservation(AtomFeed feed, Observation relatedObs) throws ParseException {
        Obs result = new Obs();
        mapConcept(relatedObs, result);
        mapValue(relatedObs, result);
        if (isNotEmpty(relatedObs.getRelated())) {
            mapRelatedObservations(feed, relatedObs);
        }
        return result;
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
                result.setValueCoded(conceptService.getConceptByName(codings.get(codings.size() - 1).getDisplaySimple()));
            }
        }
    }

    private void mapConcept(Observation relatedObs, Obs result) {
        if (null != relatedObs.getName()) {
            String externalId = relatedObs.getName().getCoding().get(0).getCodeSimple();
            IdMapping mapping = idMappingsRepository.findByExternalId(externalId);
            result.setConcept(conceptService.getConceptByUuid(mapping.getInternalId()));
        }
    }

    private boolean mapConcept(Obs result) {
        Concept vitalsConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_VITALS);
        if (null != vitalsConcept) {
            result.setConcept(vitalsConcept);
            return true;
        }
        return false;
    }
}
