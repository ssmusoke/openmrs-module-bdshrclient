package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.DurationDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.module.bahmniemrapi.drugorder.dosinginstructions.FlexibleDosingInstructions;
import org.openmrs.module.fhir.Constants;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.utils.DurationMapperUtil;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.fhir.utils.FrequencyMapperUtil;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FHIRMedicationOrderMapper implements FHIRResourceMapper {
    private static final int DEFAULT_NUM_REFILLS = 0;
    private static final String ROUTE_NOT_SPECIFIED = "NOT SPECIFIED";
    private final ObjectMapper objectMapper;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private FrequencyMapperUtil frequencyMapperUtil;
    @Autowired
    private DurationMapperUtil durationMapperUtil;
    @Autowired
    private ProviderLookupService providerLookupService;
    @Autowired
    private OrderCareSettingLookupService orderCareSettingLookupService;
    @Autowired
    private GlobalPropertyLookUpService globalPropertyLookUpService;
    @Autowired
    private IdMappingsRepository idMappingsRepository;

    private static final Logger logger = Logger.getLogger(FHIRMedicationOrderMapper.class);

    public FHIRMedicationOrderMapper() {
        objectMapper = new ObjectMapper();
    }

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof MedicationOrder;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter) {
        DrugOrder drugOrder = mapDrugOrder(bundle, (MedicationOrder) resource, emrPatient, newEmrEncounter);
        newEmrEncounter.addOrder(drugOrder);
    }

    private DrugOrder mapDrugOrder(Bundle bundle, MedicationOrder medicationOrder, Patient emrPatient, Encounter newEmrEncounter) {
        DrugOrder drugOrder = new DrugOrder();
        DrugOrder previousDrugOrder = createOrFetchPreviousOrder(bundle, medicationOrder, emrPatient, newEmrEncounter);
        if (previousDrugOrder != null) {
            drugOrder.setPreviousOrder(previousDrugOrder);
        }

        drugOrder.setPatient(emrPatient);
        Drug drug = mapDrug(medicationOrder);
        if (drug == null) return drugOrder;
        drugOrder.setDrug(drug);
        if (medicationOrder.getDosageInstruction().isEmpty()) return null;
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstructionFirstRep();
        mapFrequency(drugOrder, dosageInstruction);
        HashMap<String, Object> dosingInstructionsMap = new HashMap<>();
        addNotesAndInstructionsToDosingInstructions(medicationOrder, dosingInstructionsMap);
        setOrderDuration(drugOrder, dosageInstruction);
        if (((SimpleQuantityDt) dosageInstruction.getDose()).getValue() != null) {
            drugOrder.setDose(((SimpleQuantityDt) dosageInstruction.getDose()).getValue().doubleValue());
        } else {
            addCustomDosageToDosingInstructions(dosageInstruction, dosingInstructionsMap);
        }
        setDoseUnits(drugOrder, dosageInstruction);
        setQuantity(drugOrder, medicationOrder.getDispenseRequest());
        setScheduledDateAndUrgency(drugOrder, dosageInstruction);
        setOrderAction(drugOrder, medicationOrder);

        drugOrder.setRoute(mapRoute(dosageInstruction));
        drugOrder.setAsNeeded(((BooleanDt) dosageInstruction.getAsNeeded()).getValue());
        drugOrder.setOrderer(getOrderer(medicationOrder));
        drugOrder.setNumRefills(DEFAULT_NUM_REFILLS);
        drugOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));
        try {
            drugOrder.setDosingInstructions(objectMapper.writeValueAsString(dosingInstructionsMap));
        } catch (IOException e) {
            logger.warn("Unable to set dosageInstruction");
        }
        drugOrder.setDosingType(FlexibleDosingInstructions.class);

        addDrugOrderToIdMapping(drugOrder, medicationOrder, newEmrEncounter);
        return drugOrder;
    }

    private void setOrderAction(DrugOrder drugOrder, MedicationOrder medicationOrder) {
        List<ExtensionDt> extensions = medicationOrder.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.MEDICATIONORDER_ACTION_EXTENSION_NAME));
        if(extensions == null || extensions.isEmpty()) {
            drugOrder.setAction(Order.Action.NEW);
            return;
        }
        ExtensionDt orderActionExtension = extensions.get(0);
        StringDt orderAction = (StringDt) orderActionExtension.getValue();
        drugOrder.setAction(getOrderAction(orderAction.getValue()));
    }

    private Order.Action getOrderAction(String orderAction) {
        for (Order.Action action : Order.Action.values()) {
            if(action.name().equals(orderAction)) return action;
        }
        return Order.Action.NEW;
    }

    private DrugOrder createOrFetchPreviousOrder(Bundle bundle, MedicationOrder medicationOrder, Patient emrPatient, Encounter newEmrEncounter) {
        if (hasPriorPrescription(medicationOrder)) {
            DrugOrder previousDrugOrder;
            if (shouldCreatePreviousOrder(medicationOrder)) {
                previousDrugOrder = mapDrugOrder(bundle, (MedicationOrder) FHIRFeedHelper.findResourceByReference(bundle, medicationOrder.getPriorPrescription()), emrPatient, newEmrEncounter);
                newEmrEncounter.addOrder(previousDrugOrder);
            } else {
                String previousOrderRefId = StringUtils.substringAfterLast(medicationOrder.getPriorPrescription().getReference().getValue(), "/");
                IdMapping previousOrderMapping = idMappingsRepository.findByExternalId(previousOrderRefId);
                if (previousOrderMapping == null) {
                    throw new RuntimeException(String.format("The previous order with SHR reference [%s] is not yet synced to SHR", medicationOrder.getPriorPrescription().getReference().getValue()));
                }
                previousDrugOrder = (DrugOrder) orderService.getOrderByUuid(previousOrderMapping.getInternalId());
            }
            return previousDrugOrder;
        }
        return null;
    }

    private void addDrugOrderToIdMapping(DrugOrder drugOrder, MedicationOrder medicationOrder, Encounter newEmrEncounter) {
        IdMapping encounterIdMapping = idMappingsRepository.findByInternalId(newEmrEncounter.getUuid());
        String externalId = StringUtils.substringAfter(medicationOrder.getId().getValue(), "urn:uuid:");
        IdMapping orderIdMapping = new IdMapping(drugOrder.getUuid(), externalId,
                Constants.ID_MAPPING_ORDER_TYPE,
                String.format("%s#%s/%s", encounterIdMapping.getUri(),
                        new MedicationOrder().getResourceName(), externalId));
        idMappingsRepository.saveOrUpdateMapping(orderIdMapping);
    }

    private boolean shouldCreatePreviousOrder(MedicationOrder medicationOrder) {
        ResourceReferenceDt priorPrescription = medicationOrder.getPriorPrescription();
        if (priorPrescription != null && !priorPrescription.isEmpty()) {
            if (priorPrescription.getReference().getValue().startsWith("urn:uuid")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPriorPrescription(MedicationOrder medicationOrder) {
        return medicationOrder.getPriorPrescription() != null && !medicationOrder.getPriorPrescription().getReference().isEmpty();
    }

    private void addCustomDosageToDosingInstructions(MedicationOrder.DosageInstruction dosageInstruction, HashMap<String, Object> dosingInstructionsMap) {
        List<ExtensionDt> extensions = dosageInstruction.getUndeclaredExtensionsByUrl(FHIRProperties.getFhirExtensionUrl(FHIRProperties.DOSAGEINSTRUCTION_CUSTOM_DOSAGE_EXTENSION_NAME));
        if (CollectionUtils.isNotEmpty(extensions)) {
            String value = ((StringDt) extensions.get(0).getValue()).getValue();
            if (StringUtils.isNotBlank(value)) {
                try {
                    Map map = objectMapper.readValue(value, Map.class);
                    if (map.containsKey(FHIRProperties.FHIR_DRUG_ORDER_MORNING_DOSE_KEY)) {
                        Integer morningDose = (Integer) map.get(FHIRProperties.FHIR_DRUG_ORDER_MORNING_DOSE_KEY);
                        dosingInstructionsMap.put(MRSProperties.BAHMNI_DRUG_ORDER_MORNING_DOSE_KEY, morningDose);
                    }
                    if (map.containsKey(FHIRProperties.FHIR_DRUG_ORDER_AFTERNOON_DOSE_KEY)) {
                        Integer afternoonDose = (Integer) map.get(FHIRProperties.FHIR_DRUG_ORDER_AFTERNOON_DOSE_KEY);
                        dosingInstructionsMap.put(MRSProperties.BAHMNI_DRUG_ORDER_AFTERNOON_DOSE_KEY, afternoonDose);
                    }
                    if (map.containsKey(FHIRProperties.FHIR_DRUG_ORDER_EVENING_DOSE_KEY)) {
                        Integer eveningDose = (Integer) map.get(FHIRProperties.FHIR_DRUG_ORDER_EVENING_DOSE_KEY);
                        dosingInstructionsMap.put(MRSProperties.BAHMNI_DRUG_ORDER_EVENING_DOSE_KEY, eveningDose);
                    }
                } catch (IOException e) {
                    logger.warn("Unable to map the Dosage Instructions extension value");
                }
            }
        }
    }

    private void setDoseUnits(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        SimpleQuantityDt dose = (SimpleQuantityDt) dosageInstruction.getDose();
        String dosingUnitsConceptUuid = globalPropertyLookUpService.getGlobalPropertyValue(MRSProperties.GLOBAL_PROPERTY_DOSING_FORMS_CONCEPT_UUID);
        if (StringUtils.isBlank(dosingUnitsConceptUuid)) {
            throw new RuntimeException(String.format("Global property %s is not set", MRSProperties.GLOBAL_PROPERTY_DOSING_FORMS_CONCEPT_UUID));
        }
        Concept dosingUnitsConcept = conceptService.getConceptByUuid(dosingUnitsConceptUuid);
        Concept doseUnitConcept = null;
        if (StringUtils.isNotBlank(dose.getCode())) {
            doseUnitConcept = omrsConceptLookup.findMemberConceptFromValueSetCode(dosingUnitsConcept, dose.getCode());
        }
        if (doseUnitConcept == null) {
            doseUnitConcept = omrsConceptLookup.findMemberFromDisplayName(dosingUnitsConcept, dose.getUnit());
        }
        if (doseUnitConcept == null) {
            throw new RuntimeException(String.format("Unable to find the dose units [%s] under dosing units.", StringUtils.isNotBlank(dose.getCode()) ? dose.getCode() : dose.getUnit()));
        }
        drugOrder.setDoseUnits(doseUnitConcept);
    }

    private void addNotesAndInstructionsToDosingInstructions(MedicationOrder medicationOrder, HashMap<String, Object> map) {
        CodeableConceptDt additionalInstructions = medicationOrder.getDosageInstructionFirstRep().getAdditionalInstructions();
        if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
            Concept additionalInstructionsConcept = omrsConceptLookup.findConceptByCodeOrDisplay(additionalInstructions.getCoding());
            if (additionalInstructionsConcept != null)
                map.put(MRSProperties.BAHMNI_DRUG_ORDER_INSTRCTIONS_KEY, additionalInstructionsConcept.getName().getName());
        }

        if (StringUtils.isNotBlank(medicationOrder.getNote()))
            map.put(MRSProperties.BAHMNI_DRUG_ORDER_ADDITIONAL_INSTRCTIONS_KEY, medicationOrder.getNote());
    }

    private void setQuantity(DrugOrder drugOrder, MedicationOrder.DispenseRequest dispenseRequest) {
        SimpleQuantityDt quantity = dispenseRequest.getQuantity();
        drugOrder.setQuantity(quantity.getValue().doubleValue());
        Concept unitConcept = conceptService.getConceptByName(quantity.getUnit());
        if (unitConcept == null)
            unitConcept = conceptService.getConceptByName(MRSProperties.DRUG_ORDER_QUANTITY_UNITS_CONCEPT_NAME);
        drugOrder.setQuantityUnits(unitConcept);
    }

    private void setScheduledDateAndUrgency(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        TimingDt timing = dosageInstruction.getTiming();
        String fhirScheduledDateExtensionUrl = FHIRProperties.getFhirExtensionUrl(FHIRProperties.SCHEDULED_DATE_EXTENSION_NAME);
        if (timing.getUndeclaredExtensions().size() > 0
                && timing.getUndeclaredExtensionsByUrl(fhirScheduledDateExtensionUrl).size() > 0) {
            ExtensionDt scheduledDateExtension = timing.getUndeclaredExtensionsByUrl(fhirScheduledDateExtensionUrl).get(0);
            drugOrder.setScheduledDate(((DateTimeDt) scheduledDateExtension.getValue()).getValue());
            drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
        } else {
            drugOrder.setUrgency(Order.Urgency.ROUTINE);
        }
    }

    private Provider getOrderer(MedicationOrder medicationOrder) {
        ResourceReferenceDt prescriber = medicationOrder.getPrescriber();
        Provider provider = null;
        if (prescriber != null) {
            String presciberReferenceUrl = prescriber.getReference().getValue();
            provider = providerLookupService.getProviderByReferenceUrl(presciberReferenceUrl);
        }
        return provider;
    }

    private void mapFrequency(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        TimingDt.Repeat repeat = dosageInstruction.getTiming().getRepeat();
        if (repeat.getFrequency() != null) {
            FrequencyMapperUtil.FrequencyUnit frequencyUnit = frequencyMapperUtil.getFrequencyUnitsFromRepeat(repeat);
            Concept frequencyConcept = conceptService.getConceptByName(frequencyUnit.getConceptName());
            OrderFrequency orderFrequency = orderService.getOrderFrequencyByConcept(frequencyConcept);
            drugOrder.setFrequency(orderFrequency);
        }
    }

    private void setOrderDuration(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        DurationDt duration = (DurationDt) dosageInstruction.getTiming().getRepeat().getBounds();
        drugOrder.setDuration(duration.getValue().intValue());
        drugOrder.setDurationUnits(conceptService.getConceptByName(durationMapperUtil.getConceptNameFromUnitOfTime(UnitsOfTimeEnum.VALUESET_BINDER.fromCodeString(duration.getCode()))));
    }

    private Concept mapRoute(MedicationOrder.DosageInstruction dosageInstruction) {
        Concept route = null;
        if (null != dosageInstruction.getRoute() && !dosageInstruction.getRoute().getCoding().isEmpty()) {
            route = omrsConceptLookup.findConceptByCode(dosageInstruction.getRoute().getCoding());
            if (route == null) {
                route = conceptService.getConceptByName(dosageInstruction.getRoute().getCoding().get(0).getDisplay());
            }
        }
        if (route == null) {
            route = conceptService.getConceptByName(ROUTE_NOT_SPECIFIED);
        }
        return route;
    }

    private Drug mapDrug(MedicationOrder medicationOrder) {
        CodeableConceptDt medication = (CodeableConceptDt) medicationOrder.getMedication();
        Drug drug = omrsConceptLookup.findDrug(medication.getCoding());
        if (drug == null) {
            drug = conceptService.getDrugByNameOrId(medication.getCodingFirstRep().getDisplay());
        }
        return drug;
    }
}
