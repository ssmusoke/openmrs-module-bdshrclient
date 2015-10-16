package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.DurationDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.valueset.MedicationOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.fhir.utils.DurationMapperUtil;
import org.openmrs.module.fhir.utils.FrequencyMapperUtil;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.TrValueSetType;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_DRUG_ORDER_TYPE;

@Component
public class DrugOrderMapper implements EmrOrderResourceHandler {
    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private FrequencyMapperUtil frequencyMapperUtil;
    @Autowired
    private DurationMapperUtil durationMapperUtil;
    @Autowired
    private CodeableConceptService codeableConceptService;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    private static final Logger logger = Logger.getLogger(DrugOrderMapper.class);

    @Override
    public boolean canHandle(Order order) {
        return ((order instanceof DrugOrder) && (order.getOrderType().getName().equalsIgnoreCase(MRS_DRUG_ORDER_TYPE)));
    }

    @Override
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
        List<FHIRResource> fhirResources = new ArrayList<>();
        DrugOrder drugOrder = (DrugOrder) order;
        MedicationOrder medicationOrder = new MedicationOrder();
        medicationOrder.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValueAsString()));
        setPatient(fhirEncounter, medicationOrder);
        medicationOrder.setDateWritten(drugOrder.getDateCreated(), TemporalPrecisionEnum.MILLI);
        medicationOrder.setMedication(getMedication(drugOrder));
        medicationOrder.setPrescriber(getOrdererReference(drugOrder, fhirEncounter, systemProperties));
        setDoseInstructions(drugOrder, medicationOrder, systemProperties);
        setStatus(drugOrder, medicationOrder);
        setDispenseRequest(drugOrder, medicationOrder);
        medicationOrder.setNote(getNotes(drugOrder));

        String id = new EntityReference().build(Order.class, systemProperties, order.getUuid());
        medicationOrder.addIdentifier().setValue(id);
        medicationOrder.setId(id);
        fhirResources.add(new FHIRResource("Medication Order", medicationOrder.getIdentifier(), medicationOrder));
        return fhirResources;
    }

    private String getNotes(DrugOrder drugOrder) {
        return readFromDoseInstructions(drugOrder, MRSProperties.BAHMNI_DRUG_ORDER_ADDITIONAL_INSTRCTIONS_KEY);
    }

    private void setDispenseRequest(DrugOrder drugOrder, MedicationOrder medicationOrder) {
        MedicationOrder.DispenseRequest dispenseRequest = new MedicationOrder.DispenseRequest();
        SimpleQuantityDt quantity = new SimpleQuantityDt();
        quantity.setValue(drugOrder.getQuantity());
        quantity.setUnit(drugOrder.getQuantityUnits().getName().getName());
        dispenseRequest.setQuantity(quantity);
        medicationOrder.setDispenseRequest(dispenseRequest);
    }

    private void setStatus(DrugOrder drugOrder, MedicationOrder medicationOrder) {
        if (drugOrder.getDateStopped() != null) {
            medicationOrder.setStatus(MedicationOrderStatusEnum.STOPPED);
            medicationOrder.setDateEnded(drugOrder.getDateStopped(), TemporalPrecisionEnum.MILLI);
        } else {
            medicationOrder.setStatus(MedicationOrderStatusEnum.ACTIVE);
        }
    }

    private ResourceReferenceDt getOrdererReference(Order order, Encounter encounter, SystemProperties systemProperties) {
        if (order.getOrderer() != null) {
            String providerUrl = new EntityReference().build(Provider.class, systemProperties, order.getOrderer().getIdentifier());
            if (providerUrl != null) {
                return new ResourceReferenceDt().setReference(providerUrl);
            }
        }
        List<Encounter.Participant> participants = encounter.getParticipant();
        if (!CollectionUtils.isEmpty(participants)) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private void setDoseInstructions(DrugOrder drugOrder, MedicationOrder medicationOrder, SystemProperties systemProperties) {
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.addDosageInstruction();
        if (null != drugOrder.getRoute()) {
            dosageInstruction.setRoute(
                    codeableConceptService.getTRValueSetCodeableConcept(drugOrder.getRoute(),
                            TrValueSetType.ROUTE_OF_ADMINISTRATION.getTrPropertyValueSetUrl(systemProperties)));
        }
        setDoseQuantity(drugOrder, dosageInstruction, systemProperties);
        dosageInstruction.setAdditionalInstructions(getAdditionalInstructions(drugOrder));
        dosageInstruction.setTiming(getTiming(drugOrder));
        dosageInstruction.setAsNeeded(new BooleanDt(drugOrder.getAsNeeded()));
    }

    private CodeableConceptDt getAdditionalInstructions(DrugOrder drugOrder) {
        String doseInstructionsConceptName = readFromDoseInstructions(drugOrder, MRSProperties.BAHMNI_DRUG_ORDER_INSTRCTIONS_KEY);
        if (doseInstructionsConceptName != null) {
            Concept additionalInstructionsConcept = conceptService.getConceptByName(doseInstructionsConceptName);
            return codeableConceptService.addTRCodingOrDisplay(additionalInstructionsConcept);
        }
        return null;
    }

    private String readFromDoseInstructions(DrugOrder drugOrder, String key) {
        if (StringUtils.isBlank(drugOrder.getDosingInstructions())) return null;
        try {
            Map map = new ObjectMapper().readValue(drugOrder.getDosingInstructions(), Map.class);
            return (String) map.get(key);
        } catch (IOException e) {
            logger.warn(String.format("Unable to map the dosing instructions for order [%s].", drugOrder.getUuid()));
        }
        return null;
    }

    private TimingDt getTiming(DrugOrder drugOrder) {
        TimingDt timing = new TimingDt();
        TimingDt.Repeat repeat = new TimingDt.Repeat();

        setBounds(drugOrder, repeat);
        setFrequencyAndPeriod(drugOrder, repeat);
        timing.setRepeat(repeat);

        if (drugOrder.getScheduledDate() != null) {
            DateTimeDt dateTimeDt = new DateTimeDt(drugOrder.getScheduledDate(), TemporalPrecisionEnum.MILLI);
            ExtensionDt extensionDt = new ExtensionDt(false, FHIRProperties.getFhirExtensionUrl(FHIRProperties.SCHEDULED_DATE_EXTENSION_NAME), dateTimeDt);
            timing.addUndeclaredExtension(extensionDt);
        }
        return timing;
    }

    private void setFrequencyAndPeriod(DrugOrder drugOrder, TimingDt.Repeat repeat) {
        String frequencyConceptName = drugOrder.getFrequency().getConcept().getName().getName();
        FrequencyMapperUtil.FrequencyUnit frequencyUnit = frequencyMapperUtil.getFrequencyUnits(frequencyConceptName);
        repeat.setFrequency(frequencyUnit.getFrequency());
        repeat.setPeriod(frequencyUnit.getFrequencyPeriod());
        repeat.setPeriodUnits(frequencyUnit.getUnitOfTime());
    }

    private void setBounds(DrugOrder drugOrder, TimingDt.Repeat repeat) {
        DurationDt duration = new DurationDt();
        duration.setValue(drugOrder.getDuration());
        String durationUnit = drugOrder.getDurationUnits().getName().getName();
        UnitsOfTimeEnum unitOfTime = durationMapperUtil.getUnitOfTime(durationUnit);
        duration.setCode(unitOfTime.getCode());
        duration.setSystem(unitOfTime.getSystem());
        repeat.setBounds(duration);
    }

    private void setDoseQuantity(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction, SystemProperties systemProperties) {
        if (null != drugOrder.getDose()) {
            SimpleQuantityDt doseQuantity = new SimpleQuantityDt();
            DecimalDt dose = new DecimalDt();
            dose.setValue(new BigDecimal(drugOrder.getDose()));
            Concept doseUnits = drugOrder.getDoseUnits();
            if (null != doseUnits) {
                TrValueSetType trValueSetType = determineTrValueSet(doseUnits);
                if (null != idMappingsRepository.findByInternalId(doseUnits.getUuid())) {
                    String code = codeableConceptService.getTRValueSetCode(doseUnits);
                    doseQuantity.setCode(code);
                    doseQuantity.setSystem(trValueSetType.getTrPropertyValueSetUrl(systemProperties));
                    doseQuantity.setUnit(doseUnits.getName().getName());
                }
            }
            dosageInstruction.setDose(doseQuantity.setValue(dose));
        }
    }

    private TrValueSetType determineTrValueSet(Concept doseUnits) {
        Concept quantityUnitsConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.QUANTITY_UNITS);
        Concept orderableFormsConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.ORDERABLE_DRUG_FORMS);
        if (omrsConceptLookup.isSetMemberOf(quantityUnitsConcept, doseUnits))
            return TrValueSetType.QUANTITY_UNITS;
        else if (omrsConceptLookup.isSetMemberOf(orderableFormsConcept, doseUnits)) {
            return TrValueSetType.ORDERABLE_DRUG_FORMS;
        }
        logger.warn(String.format("Invalid Dose Unit[concept : %s]. Must belong to %s or %s valueset.",
                doseUnits.getUuid(), TrValueSetType.QUANTITY_UNITS.getDefaultConceptName(), TrValueSetType.ORDERABLE_DRUG_FORMS.getDefaultConceptName()));
        return null;
    }

    private CodeableConceptDt getMedication(DrugOrder drugOrder) {
        CodeableConceptDt codeableConcept = new CodeableConceptDt();
        String uuid = drugOrder.getDrug().getUuid();
        IdMapping idMapping = idMappingsRepository.findByInternalId(uuid);
        String displayName = drugOrder.getDrug().getDisplayName();
        if (null != idMapping) {
            codeableConcept.addCoding()
                    .setCode(idMapping.getExternalId())
                    .setSystem(idMapping.getUri())
                    .setDisplay(displayName);
        } else {
            codeableConcept.addCoding().setDisplay(displayName);
        }
        return codeableConcept;
    }

    private void setPatient(Encounter fhirEncounter, MedicationOrder medicationOrder) {
        medicationOrder.setPatient(fhirEncounter.getPatient());
    }
}
