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
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.fhir.utils.UnitsHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FHIRMedicationOrderMapper implements FHIRResourceMapper {
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
        List<MedicationOrder.DosageInstruction> dosageInstructions = medicationOrder.getDosageInstruction();
        drugOrder.setRoute(mapRoute(dosageInstructions.get(0)));
        mapFrequencyAndDose(drugOrder, dosageInstructions);
        setOrderDuration(drugOrder, dosageInstructions.get(0));
        setScheduledDateAndUrgency(drugOrder, dosageInstructions.get(0));
        drugOrder.setAsNeeded(((BooleanDt) dosageInstructions.get(0).getAsNeeded()).getValue());
        drugOrder.setOrderer(getOrderer(medicationOrder));
        drugOrder.setNumRefills(DEFAULT_NUM_REFILLS);
        drugOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));

        newEmrEncounter.addOrder(drugOrder);
    }

    private void setScheduledDateAndUrgency(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        drugOrder.setAction(Order.Action.NEW);

        TimingDt timing = dosageInstruction.getTiming();
        if (timing.getUndeclaredExtensions().size() > 0
                && timing.getUndeclaredExtensionsByUrl(FHIRProperties.SCHEDULED_DATE_EXTENSION_URL).size() > 0) {
            ExtensionDt scheduledDateExtension = timing.getUndeclaredExtensionsByUrl(FHIRProperties.SCHEDULED_DATE_EXTENSION_URL).get(0);
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

    private void mapFrequencyAndDose(DrugOrder drugOrder, List<MedicationOrder.DosageInstruction> dosageInstructions) {
        for (MedicationOrder.DosageInstruction dosageInstruction : dosageInstructions) {
            TimingDt.Repeat repeat = dosageInstruction.getTiming().getRepeat();
            if (repeat.getFrequency() != null) {
                UnitsHelpers.FrequencyUnit frequencyUnit = unitsHelpers.getFrequencyUnitsFromRepeat(repeat);
                Concept frequencyConcept = conceptService.getConceptByName(frequencyUnit.getConceptName());
                OrderFrequency orderFrequency = orderService.getOrderFrequencyByConcept(frequencyConcept);
                drugOrder.setFrequency(orderFrequency);

                SimpleQuantityDt dose = (SimpleQuantityDt) dosageInstruction.getDose();
                drugOrder.setDose(dose.getValue().doubleValue());
                Concept doseUnitConcept = omrsConceptLookup.findConceptFromValueSetCode(dose.getSystem(), dose.getCode());
                drugOrder.setDoseUnits(doseUnitConcept);
                break;
            }
        }
    }

    private void setOrderDuration(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction) {
        DurationDt duration = (DurationDt) dosageInstruction.getTiming().getRepeat().getBounds();
        drugOrder.setDuration(duration.getValue().intValue());
        drugOrder.setDurationUnits(conceptService.getConceptByName(unitsHelpers.getConceptNameFromUnitOfTime(UnitsOfTimeEnum.VALUESET_BINDER.fromCodeString(duration.getCode()))));
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
