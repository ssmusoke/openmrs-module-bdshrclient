package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.Boolean;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Drug;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.TrValueSetKeys;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ObservationType;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.fhir.utils.PropertyKeyConstants;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class ImmunizationMapper implements EmrObsResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private ObservationValueMapper obsValueMapper;
    @Autowired
    private CodableConceptService codableConceptService;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(ObservationType.IMMUNIZATION);
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> resources = new ArrayList<>();
        Immunization immunization = mapObservation(new CompoundObservation(obs), fhirEncounter, systemProperties);
        if(immunization != null) {
            FHIRResource immunizationResource = new FHIRResource("Immunization", immunization.getIdentifier(), immunization);
            resources.add(immunizationResource);
        }
        return resources;
    }

    private Immunization mapObservation(CompoundObservation immunizationIncidentObs, Encounter fhirEncounter, SystemProperties systemProperties) {
        Immunization immunization = new Immunization();
        immunization.setSubject(fhirEncounter.getSubject());

        Obs vaccineObs = immunizationIncidentObs.getMemberObsForConceptName(MRS_CONCEPT_VACCINE);
        List<Drug> drugs = conceptService.getDrugsByConcept(vaccineObs.getValueCoded());
        if (CollectionUtils.isEmpty(drugs)) {
            return null;
        }
        immunization.setVaccineType(getVaccineType(drugs));
        setIdentifier(immunizationIncidentObs, systemProperties, immunization);
        immunization.setDate(getVaccinationDate(immunizationIncidentObs));
        immunization.setRefusedIndicator(getIndicator(immunizationIncidentObs, MRS_CONCEPT_VACCINATION_REFUSED));
        immunization.setRequester(getRequester(fhirEncounter));
        immunization.setReported(getIndicator(immunizationIncidentObs, MRS_CONCEPT_VACCINATION_REPORTED));
        immunization.setDoseQuantity(getDosage(immunizationIncidentObs, systemProperties));
        immunization.setExplanation(getExplation(immunizationIncidentObs, systemProperties));
        immunization.setRoute(getRoute(immunizationIncidentObs, systemProperties));

        return immunization;
    }

    private CodeableConcept getRoute(CompoundObservation immunizationIncidentObs, SystemProperties systemProperties) {
        Obs routeObs = immunizationIncidentObs.getMemberObsForConceptName(VALUESET_ROUTE);
        if (routeObs != null) {
            return codableConceptService.getTRValueSetCodeableConcept(routeObs.getValueCoded(),
                    systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_ROUTE));
        }
        return null;
    }

    private Immunization.ImmunizationExplanationComponent getExplation(CompoundObservation immunizationIncidentObs, SystemProperties systemProperties) {
        Immunization.ImmunizationExplanationComponent explanationComponent = new Immunization.ImmunizationExplanationComponent();
        populateReason(immunizationIncidentObs, systemProperties, explanationComponent, VALUESET_IMMUNIZATION_REASON, PropertyKeyConstants.TR_VALUESET_IMMUNIZATION_REASON);
        populateReason(immunizationIncidentObs, systemProperties, explanationComponent, VALUESET_IMMUNIZATION_REFUSAL_REASON, PropertyKeyConstants.TR_VALUESET_REFUSAL_REASON);
        return hasNoReasons(explanationComponent) ? null : explanationComponent;
    }

    private boolean hasNoReasons(Immunization.ImmunizationExplanationComponent explanationComponent){
        return CollectionUtils.isEmpty(explanationComponent.getReason()) && CollectionUtils.isEmpty(explanationComponent.getRefusalReason());
    }

    private void populateReason(CompoundObservation immunizationIncidentObs, SystemProperties systemProperties,
                                Immunization.ImmunizationExplanationComponent explanationComponent,
                                String reasonConceptName, String valueSetKeyName) {
        Obs immunizationReasonObs = immunizationIncidentObs.getMemberObsForConceptName(reasonConceptName);
        if (immunizationReasonObs != null && idMappingsRepository.findByInternalId(immunizationReasonObs.getValueCoded().getUuid()) != null) {
            CodeableConcept reason = getReason(reasonConceptName, explanationComponent);
            codableConceptService.getTRValueSetCodeableConcept(immunizationReasonObs.getValueCoded(),
                    systemProperties.getTrValuesetUrl(valueSetKeyName),
                    reason);
        }
    }

    private CodeableConcept getReason(String reasonConceptName, Immunization.ImmunizationExplanationComponent explanationComponent) {
        return VALUESET_IMMUNIZATION_REFUSAL_REASON.equals(reasonConceptName) ? explanationComponent.addRefusalReason() : explanationComponent.addReason();
    }

    private Quantity getDosage(CompoundObservation immunizationIncidentObs, SystemProperties systemProperties) {
        Obs doseObs = immunizationIncidentObs.getMemberObsForConceptName(MRS_CONCEPT_DOSAGE);
        if (doseObs == null) return null;
        Quantity dose = new Quantity();
        dose.setValue((Decimal) obsValueMapper.map(doseObs));
        populateQuantityUnits(immunizationIncidentObs, systemProperties, dose);
        return dose;
    }

    private void populateQuantityUnits(CompoundObservation immunizationIncidentObs, SystemProperties systemProperties, Quantity dose) {
        Obs quantityUnitsObs = immunizationIncidentObs.getMemberObsForConceptName(VALUESET_QUANTITY_UNITS);
        if (quantityUnitsObs != null) {
            dose.setCodeSimple(codableConceptService.getTRValueSetCode(quantityUnitsObs.getValueCoded()));
            if (idMappingsRepository.findByInternalId(quantityUnitsObs.getValueCoded().getUuid()) != null)
                dose.setSystemSimple(systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_QTY_UNITS));
        }
    }

    private ResourceReference getRequester(Encounter fhirEncounter) {
        List<Encounter.EncounterParticipantComponent> participants = fhirEncounter.getParticipant();
        return CollectionUtils.isNotEmpty(participants) ? participants.get(0).getIndividual() : null;
    }

    private void setIdentifier(CompoundObservation obs, SystemProperties systemProperties, Immunization immunization) {
        Identifier identifier = immunization.addIdentifier();
        identifier.setValueSimple(new EntityReference().build(Obs.class, systemProperties, obs.getUuid()));
    }

    private DateTime getVaccinationDate(CompoundObservation immunizationIncidentObs) {
        Obs vaccinationDateObs = immunizationIncidentObs.getMemberObsForConceptName(MRS_CONCEPT_VACCINATION_DATE);
        if (vaccinationDateObs == null) return null;
        DateTime vaccinationDate = new DateTime();
        vaccinationDate.setValue(((Date) obsValueMapper.map(vaccinationDateObs)).getValue());
        return vaccinationDate;
    }

    private CodeableConcept getVaccineType(List<Drug> drugs) {
        Drug drugsByConcept = drugs.get(0);
        IdMapping idMapping = idMappingsRepository.findByInternalId(drugsByConcept.getUuid());
        CodeableConcept codeableConcept = new CodeableConcept();
        if (idMapping != null) {
            codableConceptService.addFHIRCoding(codeableConcept, idMapping.getExternalId(), idMapping.getUri(), drugsByConcept.getDisplayName());
        } else {
            Coding coding = codeableConcept.addCoding();
            coding.setDisplaySimple(drugsByConcept.getDisplayName());
        }
        return codeableConcept;
    }

    private Boolean getIndicator(CompoundObservation immunizationIncidentObs, String conceptName) {
        Obs indicatorObs = immunizationIncidentObs.getMemberObsForConceptName(conceptName);
        if(indicatorObs != null) {
            return (Boolean) obsValueMapper.map(indicatorObs);
        }
        return null;
    }

}
