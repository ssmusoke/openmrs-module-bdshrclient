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
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.DurationMapperUtil;
import org.openmrs.module.fhir.utils.FrequencyMapperUtil;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

@Component
public class FHIRMedicationOrderMapper implements FHIRResourceMapper {
    private static final int DEFAULT_NUM_REFILLS = 0;
    private static final String ROUTE_NOT_SPECIFIED = "NOT SPECIFIED";

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

    private static final Logger logger = Logger.getLogger(FHIRMedicationOrderMapper.class);

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof MedicationOrder;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter) {
        MedicationOrder medicationOrder = (MedicationOrder) resource;

        DrugOrder drugOrder = new DrugOrder();
        drugOrder.setPatient(emrPatient);
        Drug drug = mapDrug(medicationOrder);
        if (drug == null) return;
        drugOrder.setDrug(drug);
        if (medicationOrder.getDosageInstruction().isEmpty()) return;
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.getDosageInstructionFirstRep();
        mapFrequency(drugOrder, dosageInstruction);
        setOrderDurationAndDose(drugOrder, dosageInstruction);
        setQuantity(drugOrder, medicationOrder.getDispenseRequest());
        setScheduledDateAndUrgency(drugOrder, dosageInstruction);
        drugOrder.setRoute(mapRoute(dosageInstruction));
        drugOrder.setAsNeeded(((BooleanDt) dosageInstruction.getAsNeeded()).getValue());
        drugOrder.setOrderer(getOrderer(medicationOrder));
        drugOrder.setNumRefills(DEFAULT_NUM_REFILLS);
        drugOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));
        drugOrder.setDosingInstructions(getDosingInstructions(medicationOrder));
        drugOrder.setDosingType(FlexibleDosingInstructions.class);

        newEmrEncounter.addOrder(drugOrder);
    }

    private String getDosingInstructions(MedicationOrder medicationOrder) {
        HashMap<String, String> map = new HashMap<>();
        CodeableConceptDt additionalInstructions = medicationOrder.getDosageInstructionFirstRep().getAdditionalInstructions();
        if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
            Concept additionalInstructionsConcept = omrsConceptLookup.findConceptByCodeOrDisplay(additionalInstructions.getCoding());
            if (additionalInstructionsConcept != null)
                map.put(MRSProperties.BAHMNI_DRUG_ORDER_INSTRCTIONS_KEY, additionalInstructionsConcept.getName().getName());
        }

        if(StringUtils.isNotBlank(medicationOrder.getNote()))
            map.put(MRSProperties.BAHMNI_DRUG_ORDER_ADDITIONAL_INSTRCTIONS_KEY, medicationOrder.getNote());
        try {
            return map.size() != 0 ? new ObjectMapper().writeValueAsString(map) : null;
        } catch (IOException e) {
            logger.warn("Not able to convert dosingInstructions to JSON.");
            return null;
        }
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
        drugOrder.setAction(Order.Action.NEW);

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

    private void setOrderDurationAndDose(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        DurationDt duration = (DurationDt) dosageInstruction.getTiming().getRepeat().getBounds();
        drugOrder.setDuration(duration.getValue().intValue());
        drugOrder.setDurationUnits(conceptService.getConceptByName(durationMapperUtil.getConceptNameFromUnitOfTime(UnitsOfTimeEnum.VALUESET_BINDER.fromCodeString(duration.getCode()))));

        SimpleQuantityDt dose = (SimpleQuantityDt) dosageInstruction.getDose();
        drugOrder.setDose(dose.getValue().doubleValue());
        Concept doseUnitConcept = omrsConceptLookup.findConceptFromValueSetCode(dose.getSystem(), dose.getCode());
        drugOrder.setDoseUnits(doseUnitConcept);
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
