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
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;
import static org.openmrs.module.fhir.mapper.TrValueSetKeys.QUANTITY_UNITS;

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
        Immunization immunization = mapObservation(obs, fhirEncounter, systemProperties);

        FHIRResource immunizationResource = new FHIRResource("Immunization", immunization.getIdentifier(), immunization);
        resources.add(immunizationResource);

        return resources;
    }

    private Immunization mapObservation(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        Immunization immunization = new Immunization();
        immunization.setSubject(fhirEncounter.getSubject());
        Set<Obs> groupMembers = obs.getGroupMembers();

        Obs vaccineObs = getObsForConcept(MRS_CONCEPT_VACCINE, groupMembers);
        List<Drug> drugs = conceptService.getDrugsByConcept(vaccineObs.getValueCoded());
        if (CollectionUtils.isEmpty(drugs)) {
            return null;
        }
        immunization.setVaccineType(getVaccineType(drugs));
        setIdentifier(obs, systemProperties, immunization);
        immunization.setDate(getVaccinationDate(groupMembers));
        immunization.setRefusedIndicator(getRefusedIndicator(groupMembers));
        immunization.setRequester(getRequester(fhirEncounter));
        immunization.setReported((Boolean) obsValueMapper.map(getObsForConcept(MRS_CONCEPT_VACCINATION_REPORTED, groupMembers)));
        immunization.setDoseQuantity(getDosage(groupMembers, systemProperties));
        immunization.setExplanation(getExplation(groupMembers, systemProperties));
        immunization.setRoute(getRoute(groupMembers, systemProperties));

        return immunization;
    }

    private CodeableConcept getRoute(Set<Obs> groupMembers, SystemProperties systemProperties) {
        Obs routeObs = getObsForConcept(VALUESET_ROUTE, groupMembers);
        if(routeObs != null) {
            return codableConceptService.getTRValueSetCodeableConcept(routeObs.getValueCoded(),
                    systemProperties.getTrValuesetUrl(TrValueSetKeys.ROUTE));
        }
        return null;
    }

    private Immunization.ImmunizationExplanationComponent getExplation(Set<Obs> groupMembers, SystemProperties systemProperties) {
        Immunization.ImmunizationExplanationComponent explanationComponent = new Immunization.ImmunizationExplanationComponent();
        populateReason(groupMembers, systemProperties, explanationComponent, VALUESET_IMMUNIZATION_REASON, TrValueSetKeys.IMMUNIZATION_REASON);
        populateReason(groupMembers, systemProperties, explanationComponent, VALUESET_REFUSAL_REASON, TrValueSetKeys.REFUSAL_REASON);
        return explanationComponent;
    }

    private void populateReason(Set<Obs> groupMembers, SystemProperties systemProperties,
                                Immunization.ImmunizationExplanationComponent explanationComponent,
                                String reasonConceptName, String trVSKey) {
        Obs immunizationReasonObs = getObsForConcept(reasonConceptName, groupMembers);
        if(immunizationReasonObs != null) {
            CodeableConcept reason = getReason(reasonConceptName, explanationComponent);
            codableConceptService.getTRValueSetCodeableConcept(immunizationReasonObs.getValueCoded(),
                    systemProperties.getTrValuesetUrl(trVSKey),
                    reason);
        }
    }

    private CodeableConcept getReason(String reasonConceptName, Immunization.ImmunizationExplanationComponent explanationComponent){
        return VALUESET_REFUSAL_REASON.equals(reasonConceptName) ? explanationComponent.addRefusalReason() : explanationComponent.addReason();
    }

    private Quantity getDosage(Set<Obs> groupMembers, SystemProperties systemProperties) {
        Quantity dose = new Quantity();
        dose.setValue((Decimal) obsValueMapper.map(getObsForConcept(MRS_CONCEPT_DOSAGE, groupMembers)));
        Obs quantityUnitsObs = getObsForConcept(VALUESET_QUANTITY_UNITS, groupMembers);
        dose.setCodeSimple(codableConceptService.getTRValueSetCode(quantityUnitsObs.getValueCoded()));
        dose.setSystemSimple(systemProperties.getTrValuesetUrl(QUANTITY_UNITS));
        return dose;
    }

    private ResourceReference getRequester(Encounter fhirEncounter) {
        List<Encounter.EncounterParticipantComponent> participants = fhirEncounter.getParticipant();
        return CollectionUtils.isNotEmpty(participants) ? participants.get(0).getIndividual() : null;
    }

    private void setIdentifier(Obs obs, SystemProperties systemProperties, Immunization immunization) {
        Identifier identifier = immunization.addIdentifier();
        identifier.setValueSimple(new EntityReference().build(Obs.class, systemProperties, obs.getUuid()));
    }

    private DateTime getVaccinationDate(Set<Obs> groupMembers) {
        DateTime vaccinationDate = new DateTime();
        vaccinationDate.setValue(((Date) obsValueMapper.map(getObsForConcept(MRS_CONCEPT_VACCINATION_DATE, groupMembers))).getValue());
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

    private Boolean getRefusedIndicator(Set<Obs> groupMembers) {
        return (Boolean) obsValueMapper.map(getObsForConcept(MRS_CONCEPT_VACCINATION_REFUSED, groupMembers));
    }

    private Obs getObsForConcept(String conceptName, Set<Obs> groupMembers) {
        for (Obs groupMember : groupMembers) {
            if (conceptName.equals(groupMember.getConcept().getName().getName())) {
                return groupMember;
            }
        }

        return null;
    }
}
