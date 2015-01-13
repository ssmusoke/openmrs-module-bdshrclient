package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.*;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class FHIRImmunizationMapper implements FHIRResourceMapper {

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private FHIRResourceValueMapper resourceValueMapper;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Override
    public boolean canHandle(Resource resource) {
        return ResourceType.Immunization.equals(resource.getResourceType());
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        Immunization immunization = (Immunization) resource;

        if (isAlreadyProcessed(immunization, processedList))
            return;

        Obs immunizationIncidentObs = new Obs();
        immunizationIncidentObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_IMMUNIZATION_INCIDENT));


        immunizationIncidentObs.addGroupMember(getVaccinationDate(immunization));
        immunizationIncidentObs.addGroupMember(getVaccineReported(immunization));
        immunizationIncidentObs.addGroupMember(getVaccineRefused(immunization));
        immunizationIncidentObs.addGroupMember(getDosage(immunization));
        immunizationIncidentObs.addGroupMember(getQuantityUnits(immunization));
        immunizationIncidentObs.addGroupMember(getVaccineType(immunization));
        immunizationIncidentObs.addGroupMember(getRoute(immunization));
        immunizationIncidentObs.addGroupMember(getImmunizationReason(immunization));
        immunizationIncidentObs.addGroupMember(getImmunizationRefusalReason(immunization));

        newEmrEncounter.addObs(immunizationIncidentObs);
        processedList.put(immunization.getIdentifier().get(0).getValueSimple(), asList(immunizationIncidentObs.getUuid()));

    }

    private boolean isAlreadyProcessed(Immunization immunization, Map<String, List<String>> processedList) {
        return processedList.containsKey(immunization.getIdentifier().get(0).getValueSimple());
    }

    private Obs getImmunizationReason(Immunization immunization) {
        Immunization.ImmunizationExplanationComponent explanation = immunization.getExplanation();
        if (explanation != null) {
            List<CodeableConcept> reason = explanation.getReason();
            if (!reason.isEmpty()) {
                return resourceValueMapper.mapObservationForConcept(reason.get(0), VALUESET_IMMUNIZATION_REASON);
            }
        }
        return null;
    }

    private Obs getImmunizationRefusalReason(Immunization immunization) {
        Immunization.ImmunizationExplanationComponent explanation = immunization.getExplanation();
        if(explanation != null){
            List<CodeableConcept> reason = explanation.getRefusalReason();
            if (!reason.isEmpty()) {
                return resourceValueMapper.mapObservationForConcept(reason.get(0), VALUESET_IMMUNIZATION_REFUSAL_REASON);
            }
        }
        return null;
    }

    private Obs getRoute(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getRoute(), VALUESET_ROUTE);
    }

    private Obs getVaccineType(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getVaccineType(), MRS_CONCEPT_VACCINE);
    }

    private Obs getQuantityUnits(Immunization immunization) {
        Quantity doseQuantity = immunization.getDoseQuantity();
        Obs quantityUnitsObs = new Obs();
        quantityUnitsObs.setConcept(conceptService.getConceptByName(VALUESET_QUANTITY_UNITS));
        quantityUnitsObs.setValueCoded(omrsConceptLookup.findConceptFromValueSetCode(doseQuantity.getSystemSimple(), doseQuantity.getCodeSimple()));

        return quantityUnitsObs;
    }

    private Obs getDosage(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getDoseQuantity().getValue(), MRS_CONCEPT_DOSAGE);
    }

    private Obs getVaccineReported(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getReported(), MRS_CONCEPT_VACCINATION_REPORTED);
    }

    private Obs getVaccineRefused(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getRefusedIndicator(), MRS_CONCEPT_VACCINATION_REFUSED);
    }

    private Obs getVaccinationDate(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getDate(), MRS_CONCEPT_VACCINATION_DATE);
    }
}
