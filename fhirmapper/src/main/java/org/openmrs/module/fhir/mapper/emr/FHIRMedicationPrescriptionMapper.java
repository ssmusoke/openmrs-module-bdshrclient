package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.*;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.utils.OMRSHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FHIRMedicationPrescriptionMapper implements FHIRResource {
    @Autowired
    OMRSHelper omrsHelper;

    @Override
    public boolean canHandle(Resource resource) {
        return ResourceType.MedicationPrescription.equals(resource.getResourceType());
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        MedicationPrescription prescription = (MedicationPrescription) resource;

        DrugOrder drugOrder = new DrugOrder();
        MedicationPrescription.MedicationPrescriptionDosageInstructionComponent dosageInstruction = prescription.getDosageInstruction().get(0);
        Quantity doseQuantity = dosageInstruction.getDoseQuantity();
        drugOrder.setDose(doseQuantity.getValueSimple().doubleValue());
        drugOrder.setDoseUnits(omrsHelper.findConceptFromValueSetCode(doseQuantity.getSystemSimple(), doseQuantity.getCodeSimple()));
        Coding routeCoding = dosageInstruction.getRoute().getCoding().get(0);
        drugOrder.setRoute(omrsHelper.findConceptFromValueSetCode(routeCoding.getSystemSimple(), routeCoding.getCodeSimple()));

        newEmrEncounter.addOrder(drugOrder);
    }
}
