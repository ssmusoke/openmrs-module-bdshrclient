package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.BoundCodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Immunization;
import ca.uhn.fhir.model.dstu2.valueset.ImmunizationReasonCodesEnum;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.TrValueSetType;
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
    public boolean canHandle(IResource resource) {
        return resource instanceof Immunization;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        Immunization immunization = (Immunization) resource;

        if (isAlreadyProcessed(immunization, processedList))
            return;

        Obs immunizationIncidentObs = new Obs();
        immunizationIncidentObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_IMMUNIZATION_INCIDENT_TEMPLATE));


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
        processedList.put(immunization.getId().getValue(), asList(immunizationIncidentObs.getUuid()));

    }

    private boolean isAlreadyProcessed(Immunization immunization, Map<String, List<String>> processedList) {
        return processedList.containsKey(immunization.getId().getValue());
    }

    private Obs getImmunizationReason(Immunization immunization) {
        Immunization.Explanation explanation = immunization.getExplanation();
        if (explanation != null) {
            List<BoundCodeableConceptDt<ImmunizationReasonCodesEnum>> reason = explanation.getReason();
            if (!reason.isEmpty()) {
                Concept immunizationReasonConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.IMMUNIZATION_REASON);
                Obs immunizationReasonObs = new Obs();
                immunizationReasonObs.setConcept(immunizationReasonConcept);
                return resourceValueMapper.map(reason.get(0), immunizationReasonObs);
            }
        }
        return null;
    }

    private Obs getImmunizationRefusalReason(Immunization immunization) {
        Immunization.Explanation explanation = immunization.getExplanation();
        if (explanation != null) {
            List<CodeableConceptDt> reason = explanation.getReasonNotGiven();
            if (!reason.isEmpty()) {
                Concept immunizationRefusalReasonConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.IMMUNIZATION_REFUSAL_REASON);
                Obs immunizationRefusalReasonObs = new Obs();
                immunizationRefusalReasonObs.setConcept(immunizationRefusalReasonConcept);
                return resourceValueMapper.map(reason.get(0), immunizationRefusalReasonObs);
            }
        }
        return null;
    }

    private Obs getRoute(Immunization immunization) {
        Concept routeOfAdministrationConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.ROUTE_OF_ADMINISTRATION);
        Obs routeOfObservationObs = new Obs();
        routeOfObservationObs.setConcept(routeOfAdministrationConcept);
        return resourceValueMapper.map(immunization.getRoute(), routeOfObservationObs);
    }

    private Obs getVaccineType(Immunization immunization) {
        return resourceValueMapper.mapObservationForConcept(immunization.getVaccineType(), MRS_CONCEPT_VACCINE);
    }

    private Obs getQuantityUnits(Immunization immunization) {
        QuantityDt doseQuantity = immunization.getDoseQuantity();
        Obs quantityUnitsObs = null;
        if (doseQuantity != null) {
            quantityUnitsObs = new Obs();
            Concept quantityUnitsConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.QUANTITY_UNITS);
            quantityUnitsObs.setConcept(quantityUnitsConcept);
            quantityUnitsObs.setValueCoded(omrsConceptLookup.findConceptFromValueSetCode(doseQuantity.getSystem(), doseQuantity.getCode()));

        }
        return quantityUnitsObs;

    }

    private Obs getDosage(Immunization immunization) {
        QuantityDt doseQuantity = immunization.getDoseQuantity();
        if (doseQuantity != null) {
            return resourceValueMapper.mapObservationForConcept(doseQuantity, MRS_CONCEPT_DOSAGE);
        }
        return null;
    }

    private Obs getVaccineReported(Immunization immunization) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_VACCINATION_REPORTED));
        obs.setValueBoolean(immunization.getReported());
        return obs;
    }

    private Obs getVaccineRefused(Immunization immunization) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_VACCINATION_REFUSED));
        obs.setValueBoolean(immunization.getWasNotGiven());
        return obs;
    }

    private Obs getVaccinationDate(Immunization immunization) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_VACCINATION_DATE));
        obs.setValueDate(immunization.getDate());
        return obs;
    }
}
