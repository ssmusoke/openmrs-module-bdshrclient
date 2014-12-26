package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.MedicationPrescription;
import org.hl7.fhir.instance.model.Quantity;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.model.Schedule;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.OrderFrequency;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.utils.OMRSHelper;
import org.openmrs.module.fhir.utils.UnitsHelpers;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;
import static org.hl7.fhir.instance.model.MedicationPrescription.MedicationPrescriptionDosageInstructionComponent;

@Component
public class FHIRMedicationPrescriptionMapper implements FHIRResource {
    @Autowired
    private OMRSHelper omrsHelper;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private OrderService orderService;

    @Override
    public boolean canHandle(Resource resource) {
        return ResourceType.MedicationPrescription.equals(resource.getResourceType());
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        MedicationPrescription prescription = (MedicationPrescription) resource;

        DrugOrder drugOrder = new DrugOrder();
        drugOrder.setPatient(emrPatient);
        mapDrug(prescription, drugOrder);
        MedicationPrescriptionDosageInstructionComponent dosageInstruction = prescription.getDosageInstruction().get(0);
        mapDosageAndRoute(drugOrder, dosageInstruction);
        mapFrequencyAndDuration(drugOrder, dosageInstruction);
        newEmrEncounter.addOrder(drugOrder);
    }

    private void mapFrequencyAndDuration(DrugOrder drugOrder, MedicationPrescriptionDosageInstructionComponent dosageInstruction) {
        if (dosageInstruction.getTiming() instanceof Schedule) {
            Schedule schedule = (Schedule) dosageInstruction.getTiming();
            drugOrder.setDuration(schedule.getRepeat().getCountSimple());
            UnitsHelpers unitsHelpers = new UnitsHelpers();
            UnitsHelpers.UnitToDaysConverter unit = unitsHelpers.getUnitsOfTimeMapper().get(schedule.getRepeat().getUnitsSimple());
            String units = unit.getUnits();
            Concept durationUnits = conceptService.getConceptByName(units);
            drugOrder.setDurationUnits(durationUnits);
            setOrderFrequency(drugOrder, schedule, unit);
        }
    }

    private void setOrderFrequency(DrugOrder drugOrder, Schedule schedule, UnitsHelpers.UnitToDaysConverter unit) {
        double frequencyPerDay = schedule.getRepeat().getFrequencySimple() / unit.getInDays();
        List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies(false);
        if (!orderFrequencies.isEmpty()) {
            OrderFrequency closestOrderFrequency = getClosestOrderFrequency(frequencyPerDay, orderFrequencies);
            drugOrder.setFrequency(closestOrderFrequency);
        }
    }

    private OrderFrequency getClosestOrderFrequency(double frequencyPerDay, List<OrderFrequency> orderFrequencies) {
        double minDifference = orderFrequencies.get(0).getFrequencyPerDay();
        OrderFrequency closestOrderFrequency = null;
        for (OrderFrequency orderFrequency : orderFrequencies) {
            double difference = orderFrequency.getFrequencyPerDay() - frequencyPerDay;
            if (abs(difference) <= abs(minDifference)) {
                closestOrderFrequency = orderFrequency;
                minDifference = difference;
            }
        }
        return closestOrderFrequency;
    }

    private void mapDosageAndRoute(DrugOrder drugOrder, MedicationPrescriptionDosageInstructionComponent dosageInstruction) {
        Quantity doseQuantity = dosageInstruction.getDoseQuantity();
        drugOrder.setDose(doseQuantity.getValueSimple().doubleValue());
        drugOrder.setDoseUnits(omrsHelper.findConceptFromValueSetCode(doseQuantity.getSystemSimple(), doseQuantity.getCodeSimple()));
        Coding routeCoding = dosageInstruction.getRoute().getCoding().get(0);
        drugOrder.setRoute(omrsHelper.findConceptFromValueSetCode(routeCoding.getSystemSimple(), routeCoding.getCodeSimple()));
    }

    private void mapDrug(MedicationPrescription prescription, DrugOrder drugOrder) {
        String drugExternalId = StringUtils.substringAfterLast(prescription.getMedication().getReferenceSimple(), "/");
        IdMapping idMapping = idMappingsRepository.findByExternalId(drugExternalId);
        Drug drug = null;
        if (idMapping != null) {
            drug = conceptService.getDrugByUuid(idMapping.getInternalId());
        } else {
            drug = conceptService.getDrugByNameOrId(prescription.getMedication().getDisplaySimple());
        }
        drugOrder.setDrug(drug);
    }
}
