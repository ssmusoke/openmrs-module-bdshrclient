package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.fhir.utils.UnitsHelpers;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_DRUG_ORDER_TYPE;

@Component
public class DrugOrderMapper implements EmrOrderResourceHandler {
    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private UnitsHelpers unitsHelpers;

    @Autowired
    private CodableConceptService codableConceptService;
    private final int DEFAULT_DURATION = 1;

    @Override
    public boolean canHandle(Order order) {
        return ((order instanceof DrugOrder) && (order.getOrderType().getName().equalsIgnoreCase(MRS_DRUG_ORDER_TYPE)));
    }

    @Override
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
//        ArrayList<FHIRResource> FHIRResources = new ArrayList<>();
//        DrugOrder drugOrder = (DrugOrder) order;
//        MedicationPrescription prescription = new MedicationPrescription();
//        prescription.setEncounter(fhirEncounter.getIndication());
//        setPatient(fhirEncounter, prescription);
//        prescription.setDateWritten(getDateWritten(drugOrder));
//        prescription.setMedication(getMedication(drugOrder));
//        prescription.setPrescriber(getParticipant(drugOrder, fhirEncounter, systemProperties));
//        setDoseInstructions(drugOrder, prescription, systemProperties);
//        Identifier identifier = prescription.addIdentifier();
//        identifier.setValueSimple(new EntityReference().build(Order.class, systemProperties, order.getUuid()));
//        FHIRResources.add(new FHIRResource("Medication Prescription", prescription.getIdentifier(), prescription));
//        return FHIRResources;
        return Collections.emptyList();
    }

//    protected ResourceReference getParticipant(DrugOrder order, Encounter encounter, SystemProperties systemProperties) {
//        if (order.getOrderer() != null) {
//            String providerUrl = new EntityReference().build(Provider.class, systemProperties, order.getOrderer().getIdentifier());
//            if (providerUrl != null) {
//                return new ResourceReference().setReferenceSimple(providerUrl);
//            }
//        }
//        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
//        if (!CollectionUtils.isEmpty(participants)) {
//            return participants.get(0).getIndividual();
//        }
//        return null;
//    }
//
//    private void setDoseInstructions(DrugOrder drugOrder, MedicationPrescription prescription, SystemProperties systemProperties) {
//        MedicationPrescription.MedicationPrescriptionDosageInstructionComponent dosageInstruction = prescription.addDosageInstruction();
//        if (null != drugOrder.getRoute()) {
//            dosageInstruction.setRoute(
//                    codableConceptService.getTRValueSetCodeableConcept(drugOrder.getRoute(),
//                            systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_ROUTE)));
//        }
//        setDoseQuantity(drugOrder, dosageInstruction, systemProperties);
//        dosageInstruction.setTiming(getSchedule(drugOrder));
//    }
//
//
//    private Schedule getSchedule(DrugOrder drugOrder) {
//        Schedule schedule = new Schedule();
//        setEvent(drugOrder, schedule);
//        setRepeatComponent(drugOrder, schedule);
//        return schedule;
//    }
//
//    private void setEvent(DrugOrder drugOrder, Schedule schedule) {
//        java.util.Date scheduledDate = drugOrder.getScheduledDate();
//        if (null != scheduledDate) {
//            Period period = schedule.addEvent();
//            period.setStartSimple(new DateAndTime(scheduledDate));
//        }
//    }
//
//    private void setRepeatComponent(DrugOrder drugOrder, Schedule schedule) {
//        if (null != drugOrder.getFrequency() && null != drugOrder.getDuration()) {
//            Decimal duration = getDuration();
//            String conceptName = drugOrder.getFrequency().getConcept().getName().getName();
//            Schedule.UnitsOfTime frequencyUnit = unitsHelpers.getUnitsOfTime(conceptName);
//            ScheduleRepeatComponent repeatComponent = new ScheduleRepeatComponent(duration, new Enumeration<>(frequencyUnit));
//            repeatComponent.setFrequencySimple(getFrequencyPerUnit(drugOrder, frequencyUnit, unitsHelpers));
//            repeatComponent.setUnitsSimple(frequencyUnit);
//            setCount(drugOrder, unitsHelpers, schedule, frequencyUnit, repeatComponent);
//            schedule.setRepeat(repeatComponent);
//        }
//    }
//
//    private Decimal getDuration() {
//        Decimal duration = new Decimal();
//        duration.setValue(new BigDecimal(DEFAULT_DURATION));
//        return duration;
//    }
//
//    private void setCount(DrugOrder drugOrder, UnitsHelpers unitsHelpers, Schedule schedule, Schedule.UnitsOfTime frequencyUnit, ScheduleRepeatComponent repeatComponent) {
//        double unitsToDaysMultiplier = getUnitsToDaysMultiplier(drugOrder, unitsHelpers, frequencyUnit);
//        Integer integer = new Integer();
//        repeatComponent.setCount(integer);
//        int countValue = (int) Math.round(drugOrder.getDuration().intValue() * unitsToDaysMultiplier);
//        integer.setValue(countValue);
//    }
//
//    private double getUnitsToDaysMultiplier(DrugOrder drugOrder, UnitsHelpers unitsHelpers, Schedule.UnitsOfTime frequnecyUnit) {
//        UnitsHelpers.UnitToDaysConverter unitToDaysConverter = unitsHelpers.getDurationUnitToUnitMapper().get(drugOrder.getDurationUnits().getName().getName());
//        double unitsToDaysMultiplier = 1;
//        if (!unitToDaysConverter.getUnitsOfTime().toCode().equals(frequnecyUnit.toCode())) {
//            unitsToDaysMultiplier = unitToDaysConverter.getInDays() * unitsHelpers.getUnitsOfTimeMapper().get(frequnecyUnit).getInDays();
//        }
//        return unitsToDaysMultiplier;
//    }
//
//    private int getFrequencyPerUnit(DrugOrder drugOrder, Schedule.UnitsOfTime frequencyUnit, UnitsHelpers unitsHelpers) {
//        double frequencyPerUnit = (drugOrder.getFrequency().getFrequencyPerDay()) * unitsHelpers.getUnitsOfTimeMapper().get(frequencyUnit).getInDays();
//        return (int) Math.round(frequencyPerUnit);
//    }
//
//
//    private void setDoseQuantity(DrugOrder drugOrder, MedicationPrescription.MedicationPrescriptionDosageInstructionComponent dosageInstruction, SystemProperties systemProperties) {
//        if (null != drugOrder.getDose()) {
//            Quantity doseQuantity = new Quantity();
//            Decimal dose = new Decimal();
//            dose.setValue(new BigDecimal(drugOrder.getDose()));
//            if(null != drugOrder.getDoseUnits()) {
//                if (null != idMappingsRepository.findByInternalId(drugOrder.getDoseUnits().getUuid())) {
//                    String code = codableConceptService.getTRValueSetCode(drugOrder.getDoseUnits());
//                    doseQuantity.setCodeSimple(code);
//                    doseQuantity.setSystemSimple(systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_QTY_UNITS));
//                }
//            }
//            dosageInstruction.setDoseQuantity(doseQuantity.setValue(dose));
//        }
//    }
//
//    private ResourceReference getMedication(DrugOrder drugOrder) {
//        ResourceReference resourceReference = new ResourceReference();
//        String uuid = drugOrder.getDrug().getUuid();
//        IdMapping idMapping = idMappingsRepository.findByInternalId(uuid);
//        if (null != idMapping)
//            resourceReference.setReferenceSimple(idMapping.getUri());
//        resourceReference.setDisplaySimple(drugOrder.getDrug().getDisplayName());
//        return resourceReference;
//    }
//
//    private DateTime getDateWritten(DrugOrder drugOrder) {
//        DateTime dateTime = new DateTime();
//        dateTime.setValue(new DateAndTime(drugOrder.getDateCreated()));
//        return dateTime;
//    }
//
//    private void setPatient(Encounter fhirEncounter, MedicationPrescription medicationPrescription) {
//        medicationPrescription.setPatient(fhirEncounter.getSubject());
//    }
}
