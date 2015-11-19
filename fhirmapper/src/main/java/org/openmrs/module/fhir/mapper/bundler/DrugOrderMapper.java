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
import ca.uhn.fhir.model.dstu2.valueset.TimingAbbreviationEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.fhir.utils.DurationMapperUtil;
import org.openmrs.module.fhir.utils.FrequencyMapperUtil;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.fhir.utils.TrValueSetType;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openmrs.module.fhir.FHIRProperties.*;
import static org.openmrs.module.fhir.MRSProperties.*;

@Component
public class DrugOrderMapper implements EmrOrderResourceHandler {
    private final ObjectMapper objectMapper;
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
    @Autowired
    private ProviderLookupService providerLookupService;

    private static final Logger logger = Logger.getLogger(DrugOrderMapper.class);

    public DrugOrderMapper() {
        objectMapper = new ObjectMapper();
    }

    @Override
    public boolean canHandle(Order order) {
        return order.getOrderType().getName().equalsIgnoreCase(MRS_DRUG_ORDER_TYPE);
    }

    @Override
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
        List<FHIRResource> fhirResources = new ArrayList<>();
        DrugOrder drugOrder = (DrugOrder) order;
        MedicationOrder medicationOrder = new MedicationOrder();
        medicationOrder.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValueAsString()));
        setPatient(fhirEncounter, medicationOrder);
        medicationOrder.setDateWritten(drugOrder.getDateActivated(), TemporalPrecisionEnum.SECOND);
        medicationOrder.setMedication(getMedication(drugOrder));
        medicationOrder.setPrescriber(getOrdererReference(drugOrder, fhirEncounter, systemProperties));
        medicationOrder.addDosageInstruction(getDoseInstructions(drugOrder, medicationOrder, systemProperties));
        setStatusAndPriorPrescriptionAndOrderAction(drugOrder, medicationOrder, systemProperties);
        setDispenseRequest(drugOrder, medicationOrder);
        medicationOrder.setNote(getNotes(drugOrder));

        String id = new EntityReference().build(Order.class, systemProperties, order.getUuid());
        medicationOrder.addIdentifier().setValue(id);
        medicationOrder.setId(id);
        fhirResources.add(new FHIRResource("Medication Order", medicationOrder.getIdentifier(), medicationOrder));
        return fhirResources;
    }

    private void setOrderAction(DrugOrder drugOrder, MedicationOrder medicationOrder) {
        ExtensionDt orderActionExtension = new ExtensionDt();
        orderActionExtension.setUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.MEDICATIONORDER_ACTION_EXTENSION_NAME));
        Order.Action action = drugOrder.getAction();
        orderActionExtension.setValue(new StringDt(action.name()));
        medicationOrder.addUndeclaredExtension(orderActionExtension);
    }

    private String getNotes(DrugOrder drugOrder) {
        return (String) readFromDoseInstructions(drugOrder, MRSProperties.BAHMNI_DRUG_ORDER_ADDITIONAL_INSTRCTIONS_KEY);
    }

    private void setDispenseRequest(DrugOrder drugOrder, MedicationOrder medicationOrder) {
        MedicationOrder.DispenseRequest dispenseRequest = new MedicationOrder.DispenseRequest();
        SimpleQuantityDt quantity = new SimpleQuantityDt();
        quantity.setValue(drugOrder.getQuantity());
        quantity.setUnit(drugOrder.getQuantityUnits().getName().getName());
        dispenseRequest.setQuantity(quantity);
        medicationOrder.setDispenseRequest(dispenseRequest);
    }

    private void setStatusAndPriorPrescriptionAndOrderAction(DrugOrder drugOrder, MedicationOrder medicationOrder, SystemProperties systemProperties) {
        if (drugOrder.getDateStopped() != null || drugOrder.getAction().equals(Order.Action.DISCONTINUE)) {
            medicationOrder.setStatus(MedicationOrderStatusEnum.STOPPED);
            if (drugOrder.getDateStopped() != null)
                medicationOrder.setDateEnded(drugOrder.getDateStopped(), TemporalPrecisionEnum.MILLI);
            else medicationOrder.setDateEnded(drugOrder.getAutoExpireDate(), TemporalPrecisionEnum.MILLI);
        } else {
            medicationOrder.setStatus(MedicationOrderStatusEnum.ACTIVE);
        }
        setOrderAction(drugOrder, medicationOrder);
        if (drugOrder.getPreviousOrder() != null) {
            String priorPresecription = setPriorPrescriptionReference(drugOrder, systemProperties);
            medicationOrder.setPriorPrescription(new ResourceReferenceDt(priorPresecription));
        }
    }

    private String setPriorPrescriptionReference(DrugOrder drugOrder, SystemProperties systemProperties) {
        if (isEditedInDifferentEncounter(drugOrder)) {
            IdMapping orderIdMapping = idMappingsRepository.findByInternalId(drugOrder.getPreviousOrder().getUuid());
            if (orderIdMapping == null) {
                throw new RuntimeException("Previous order encounter with id [" + drugOrder.getPreviousOrder().getEncounter().getUuid() + "] is not synced to SHR yet.");
            }
            return orderIdMapping.getUri();
        } else {
            return new EntityReference().build(Order.class, systemProperties, drugOrder.getPreviousOrder().getUuid());
        }
    }

    private boolean isEditedInDifferentEncounter(DrugOrder drugOrder) {
        return !drugOrder.getEncounter().equals(drugOrder.getPreviousOrder().getEncounter());
    }

    private ResourceReferenceDt getOrdererReference(Order order, Encounter encounter, SystemProperties systemProperties) {
        if (order.getOrderer() != null) {
            String providerUrl = providerLookupService.getProviderRegistryUrl(systemProperties, order.getOrderer());
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

    private MedicationOrder.DosageInstruction getDoseInstructions(DrugOrder drugOrder, MedicationOrder medicationOrder, SystemProperties systemProperties) {
        CodeableConceptDt route = getRoute(drugOrder, systemProperties);
        CodeableConceptDt additionalInstructions = getAdditionalInstructions(drugOrder);
        BooleanDt asNeeded = new BooleanDt(drugOrder.getAsNeeded());
        SimpleQuantityDt doseQuantity = getDoseQuantityWithUnitsOnly(drugOrder, systemProperties);
        ExtensionDt scheduledDateExtension = getScheduledDateExtension(drugOrder);
        DurationDt bounds = getBounds(drugOrder);

        MedicationOrder.DosageInstruction dosageInstruction = null;
        if (drugOrder.getDose() != null) {
            dosageInstruction = getDosageInstructionsWithOrderFrequencyGiven(drugOrder, doseQuantity);
        } else {
            dosageInstruction = getDosageInstructionsWithPredifinedFrequency(drugOrder, doseQuantity);
        }
        if (dosageInstruction == null) return null;
        if (route != null) dosageInstruction.setRoute(route);
        dosageInstruction.setAdditionalInstructions(additionalInstructions);
        dosageInstruction.setAsNeeded(asNeeded);
        if (scheduledDateExtension != null)
            dosageInstruction.getTiming().addUndeclaredExtension(scheduledDateExtension);
        dosageInstruction.getTiming().getRepeat().setBounds(bounds);
        return dosageInstruction;
    }

    private MedicationOrder.DosageInstruction getDosageInstructionsWithPredifinedFrequency(DrugOrder drugOrder, SimpleQuantityDt doseQuantity) {
        MedicationOrder.DosageInstruction dosageInstruction = new MedicationOrder.DosageInstruction();
        int count = 0;
        HashMap<String, Integer> map = new HashMap<>();
        Integer morningDose = (Integer) readFromDoseInstructions(drugOrder, BAHMNI_DRUG_ORDER_MORNING_DOSE_KEY);
        if (morningDose != null && morningDose > 0) {
            count++;
            map.put(FHIR_DRUG_ORDER_MORNING_DOSE_KEY, morningDose);
        }
        Integer afternoonDose = (Integer) readFromDoseInstructions(drugOrder, BAHMNI_DRUG_ORDER_AFTERNOON_DOSE_KEY);
        if (afternoonDose != null && afternoonDose > 0) {
            count++;
            map.put(FHIR_DRUG_ORDER_AFTERNOON_DOSE_KEY, afternoonDose);
        }
        Integer eveningDose = (Integer) readFromDoseInstructions(drugOrder, BAHMNI_DRUG_ORDER_EVENING_DOSE_KEY);
        if (eveningDose != null && eveningDose > 0) {
            count++;
            map.put(FHIR_DRUG_ORDER_EVENING_DOSE_KEY, eveningDose);
        }
        TimingAbbreviationEnum timingAbbreviationEnum = null;
        if (count == 0) return null;
        else if (count == 1) timingAbbreviationEnum = TimingAbbreviationEnum.QD;
        else if (count == 2) timingAbbreviationEnum = TimingAbbreviationEnum.BID;
        else if (count == 3) timingAbbreviationEnum = TimingAbbreviationEnum.TID;

        TimingDt timing = new TimingDt();
        timing.setCode(timingAbbreviationEnum);
        dosageInstruction.setTiming(timing);
        dosageInstruction.setDose(doseQuantity);

        try {
            String json = objectMapper.writeValueAsString(map);
            String fhirExtensionUrl = FHIRProperties.getFhirExtensionUrl(FHIRProperties.DOSAGEINSTRUCTION_CUSTOM_DOSAGE_EXTENSION_NAME);
            dosageInstruction.addUndeclaredExtension(false, fhirExtensionUrl, new StringDt(json));
        } catch (IOException e) {
            logger.warn("Not able to set dose.");
        }
        return dosageInstruction;
    }

    private MedicationOrder.DosageInstruction getDosageInstructionsWithOrderFrequencyGiven(DrugOrder drugOrder, SimpleQuantityDt doseQuantity) {
        MedicationOrder.DosageInstruction dosageInstruction = new MedicationOrder.DosageInstruction();
        doseQuantity.setValue(getDoseQuantityValue(drugOrder.getDose()));
        dosageInstruction.setDose(doseQuantity);
        TimingDt timing = getTimingForOrderFrequencyGiven(drugOrder);
        dosageInstruction.setTiming(timing);
        return dosageInstruction;
    }

    private ExtensionDt getScheduledDateExtension(DrugOrder drugOrder) {
        if (drugOrder.getScheduledDate() != null) {
            DateTimeDt dateTimeDt = new DateTimeDt(drugOrder.getScheduledDate(), TemporalPrecisionEnum.MILLI);
            return new ExtensionDt(false, FHIRProperties.getFhirExtensionUrl(FHIRProperties.SCHEDULED_DATE_EXTENSION_NAME), dateTimeDt);
        }
        return null;
    }

    private DecimalDt getDoseQuantityValue(Double drugOrderDose) {
        DecimalDt dose = new DecimalDt();
        dose.setValue(new BigDecimal(drugOrderDose));
        return dose;
    }

    private CodeableConceptDt getRoute(DrugOrder drugOrder, SystemProperties systemProperties) {
        CodeableConceptDt route = null;
        if (null != drugOrder.getRoute()) {
            route = codeableConceptService.getTRValueSetCodeableConcept(drugOrder.getRoute(),
                    TrValueSetType.ROUTE_OF_ADMINISTRATION.getTrPropertyValueSetUrl(systemProperties));
        }
        return route;
    }

    private CodeableConceptDt getAdditionalInstructions(DrugOrder drugOrder) {
        String doseInstructionsConceptName = (String) readFromDoseInstructions(drugOrder, MRSProperties.BAHMNI_DRUG_ORDER_INSTRCTIONS_KEY);
        if (doseInstructionsConceptName != null) {
            Concept additionalInstructionsConcept = conceptService.getConceptByName(doseInstructionsConceptName);
            return codeableConceptService.addTRCodingOrDisplay(additionalInstructionsConcept);
        }
        return null;
    }

    private Object readFromDoseInstructions(DrugOrder drugOrder, String key) {
        if (StringUtils.isBlank(drugOrder.getDosingInstructions())) return null;
        try {
            Map map = objectMapper.readValue(drugOrder.getDosingInstructions(), Map.class);
            return map.get(key);
        } catch (IOException e) {
            logger.warn(String.format("Unable to map the dosing instructions for order [%s].", drugOrder.getUuid()));
        }
        return null;
    }

    private TimingDt getTimingForOrderFrequencyGiven(DrugOrder drugOrder) {
        TimingDt timing = new TimingDt();
        TimingDt.Repeat repeat = new TimingDt.Repeat();

        setFrequencyAndPeriod(drugOrder, repeat);
        timing.setRepeat(repeat);
        return timing;
    }

    private void setFrequencyAndPeriod(DrugOrder drugOrder, TimingDt.Repeat repeat) {
        String frequencyConceptName = drugOrder.getFrequency().getConcept().getName().getName();
        FrequencyMapperUtil.FrequencyUnit frequencyUnit = frequencyMapperUtil.getFrequencyUnits(frequencyConceptName);
        repeat.setFrequency(frequencyUnit.getFrequency());
        repeat.setPeriod(frequencyUnit.getFrequencyPeriod());
        repeat.setPeriodUnits(frequencyUnit.getUnitOfTime());
    }

    private DurationDt getBounds(DrugOrder drugOrder) {
        DurationDt duration = new DurationDt();
        duration.setValue(drugOrder.getDuration());
        String durationUnit = drugOrder.getDurationUnits().getName().getName();
        UnitsOfTimeEnum unitOfTime = durationMapperUtil.getUnitOfTime(durationUnit);
        duration.setCode(unitOfTime.getCode());
        duration.setSystem(unitOfTime.getSystem());
        return duration;
    }

    private SimpleQuantityDt getDoseQuantityWithUnitsOnly(DrugOrder drugOrder, SystemProperties systemProperties) {
        SimpleQuantityDt doseQuantity = new SimpleQuantityDt();
        Concept doseUnits = drugOrder.getDoseUnits();
        if (null != doseUnits) {
            TrValueSetType trValueSetType = determineTrValueSet(doseUnits);
            if (null != trValueSetType && null != idMappingsRepository.findByInternalId(doseUnits.getUuid())) {
                String code = codeableConceptService.getTRValueSetCode(doseUnits);
                doseQuantity.setCode(code);
                doseQuantity.setSystem(trValueSetType.getTrPropertyValueSetUrl(systemProperties));
            }
            doseQuantity.setUnit(doseUnits.getName().getName());
        }
        return doseQuantity;
    }

    private TrValueSetType determineTrValueSet(Concept doseUnits) {
        Concept medicationFormsConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.MEDICATION_FORMS);
        Concept medicationPackageFormsConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.MEDICATION_PACKAGE_FORMS);
        if (omrsConceptLookup.isAnswerOf(medicationPackageFormsConcept, doseUnits)) {
            return TrValueSetType.MEDICATION_PACKAGE_FORMS;
        } else if (omrsConceptLookup.isAnswerOf(medicationFormsConcept, doseUnits)) {
            return TrValueSetType.MEDICATION_FORMS;
        }
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
