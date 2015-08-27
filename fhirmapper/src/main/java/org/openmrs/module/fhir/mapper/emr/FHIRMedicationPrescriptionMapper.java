package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationPrescription;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.fhir.utils.UnitsHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FHIRMedicationPrescriptionMapper implements FHIRResourceMapper {
    private static final int DEFAULT_NUM_REFILLS = 0;
    private static final String URL_SEPERATOR = "/";
    private static final String ROUTE_NOT_SPECIFIED = "NOT SPECIFIED";

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UnitsHelpers unitsHelpers;
    @Autowired
    private ProviderLookupService providerLookupService;
    @Autowired
    private OrderCareSettingLookupService orderCareSettingLookupService;

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof MedicationPrescription;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
//        MedicationPrescription prescription = (MedicationPrescription) resource;
//
//        if (isAlreadyProcessed(prescription, processedList))
//            return;
//
//        DrugOrder drugOrder = new DrugOrder();
//        drugOrder.setPatient(emrPatient);
//        Drug drug = mapDrug(prescription);
//        if (drug == null) return;
//        drugOrder.setDrug(drug);
//        if (prescription.getDosageInstruction().isEmpty()) return;
//        MedicationPrescriptionDosageInstructionComponent dosageInstruction = prescription.getDosageInstruction().get(0);
//        mapDosageAndRoute(drugOrder, dosageInstruction);
//        mapFrequencyAndDurationAndScheduledDate(drugOrder, dosageInstruction);
//        Provider orderer = getOrderer(prescription);
//        drugOrder.setOrderer(orderer);
//        drugOrder.setNumRefills(DEFAULT_NUM_REFILLS);
//        drugOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));
//
//        processedList.put(((MedicationPrescription) resource).getIdentifier().get(0).getValueSimple(), asList(drugOrder.getUuid()));
//        newEmrEncounter.addOrder(drugOrder);
    }

//    private boolean isAlreadyProcessed(MedicationPrescription prescription, Map<String, List<String>> processedList) {
//        return processedList.containsKey(prescription.getIdentifier().get(0).getValueSimple());
//    }
//
//    private void mapQuantity(DrugOrder drugOrder, UnitToDaysConverter unit) {
//        double quantity = drugOrder.getDose() * drugOrder.getDuration() * drugOrder.getFrequency().getFrequencyPerDay() * unit.getInDays();
//        drugOrder.setQuantity(quantity);
//        drugOrder.setQuantityUnits(conceptService.getConceptByName(DRUG_ORDER_QUANTITY_UNITS_CONCEPT_NAME));
//    }
//
//    private Provider getOrderer(MedicationPrescription prescription) {
//        ResourceReference prescriber = prescription.getPrescriber();
//        Provider provider = null;
//        if (prescriber != null) {
//            String presciberReferenceUrl = prescriber.getReferenceSimple();
//            provider = providerLookupService.getProviderByReferenceUrl(presciberReferenceUrl);
//        }
//        return provider;
//    }
//
//    private void mapFrequencyAndDurationAndScheduledDate(DrugOrder drugOrder, MedicationPrescriptionDosageInstructionComponent dosageInstruction) {
//        if (null != dosageInstruction.getTiming() && dosageInstruction.getTiming() instanceof Schedule) {
//            Schedule schedule = (Schedule) dosageInstruction.getTiming();
//            UnitToDaysConverter unit = unitsHelpers.getUnitsOfTimeMapper().get(schedule.getRepeat().getUnitsSimple());
//            setOrderDuration(drugOrder, schedule, unit);
//            setOrderFrequency(drugOrder, schedule, unit);
//            mapQuantity(drugOrder, unit);
//            setScheduledDate(drugOrder, schedule);
//        }
//    }
//
//    private void setScheduledDate(DrugOrder drugOrder, Schedule schedule) {
//        drugOrder.setUrgency(Order.Urgency.ROUTINE);
//        if (!schedule.getEvent().isEmpty()) {
//            Period period = schedule.getEvent().get(0);
//            if (period.getStartSimple() != null) {
//                drugOrder.setScheduledDate(parseDate(period.getStartSimple().toString()));
//                drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
//            }
//        }
//    }
//
//    private void setOrderDuration(DrugOrder drugOrder, Schedule schedule, UnitToDaysConverter unit) {
//        drugOrder.setDuration(schedule.getRepeat().getCountSimple());
//        String units = unit.getUnits();
//        Concept durationUnits = conceptService.getConceptByName(units);
//        drugOrder.setDurationUnits(durationUnits);
//    }
//
//    private void setOrderFrequency(DrugOrder drugOrder, Schedule schedule, UnitToDaysConverter unit) {
//        double frequencyPerDay = schedule.getRepeat().getFrequencySimple() / unit.getInDays();
//        List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies(false);
//        if (!orderFrequencies.isEmpty()) {
//            OrderFrequency closestOrderFrequency = getClosestOrderFrequency(frequencyPerDay, orderFrequencies);
//            drugOrder.setFrequency(closestOrderFrequency);
//        }
//    }
//
//    private OrderFrequency getClosestOrderFrequency(double frequencyPerDay, List<OrderFrequency> orderFrequencies) {
//        double minDifference = orderFrequencies.get(0).getFrequencyPerDay();
//        OrderFrequency closestOrderFrequency = null;
//        for (OrderFrequency orderFrequency : orderFrequencies) {
//            double difference = orderFrequency.getFrequencyPerDay() - frequencyPerDay;
//            if (abs(difference) <= abs(minDifference)) {
//                closestOrderFrequency = orderFrequency;
//                minDifference = difference;
//            }
//        }
//        return closestOrderFrequency;
//    }
//
//    private void mapDosageAndRoute(DrugOrder drugOrder, MedicationPrescriptionDosageInstructionComponent dosageInstruction) {
//        Quantity doseQuantity = dosageInstruction.getDoseQuantity();
//        if (doseQuantity != null) {
//            drugOrder.setDose(doseQuantity.getValueSimple().doubleValue());
//            drugOrder.setDoseUnits(omrsConceptLookup.findConceptFromValueSetCode(doseQuantity.getSystemSimple(), doseQuantity.getCodeSimple()));
//        }
//        drugOrder.setRoute(mapRoute(dosageInstruction));
//    }
//
//    private Concept mapRoute(MedicationPrescriptionDosageInstructionComponent dosageInstruction) {
//        Concept route = null;
//        if (null != dosageInstruction.getRoute() && !dosageInstruction.getRoute().getCoding().isEmpty()) {
//            route = omrsConceptLookup.findConceptByCode(dosageInstruction.getRoute().getCoding());
//            if (route == null) {
//                route = conceptService.getConceptByName(dosageInstruction.getRoute().getCoding().get(0).getDisplaySimple());
//            }
//        }
//        if(route == null) {
//            route = conceptService.getConceptByName(ROUTE_NOT_SPECIFIED);
//        }
//        return route;
//    }
//
//    private Drug mapDrug(MedicationPrescription prescription) {
//        String drugExternalId = substringAfterLast(prescription.getMedication().getReferenceSimple(), URL_SEPERATOR);
//        Drug drug = omrsConceptLookup.findDrug(drugExternalId);
//        if (drug == null) {
//            drug = conceptService.getDrugByNameOrId(prescription.getMedication().getDisplaySimple());
//        }
//        return drug;
//    }
}
