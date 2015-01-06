package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Integer;
import org.hl7.fhir.instance.model.Schedule.ScheduleRepeatComponent;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.fhir.utils.UnitsHelpers;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_DRUG_ORDER_TYPE;
import static org.openmrs.module.fhir.mapper.TrValueSetKeys.QUANTITY_UNITS;
import static org.openmrs.module.fhir.mapper.TrValueSetKeys.ROUTE;

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
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, AtomFeed feed, SystemProperties systemProperties) {
        ArrayList<FHIRResource> FHIRResources = new ArrayList<>();
        DrugOrder drugOrder = (DrugOrder) order;
        MedicationPrescription prescription = new MedicationPrescription();
        prescription.setEncounter(fhirEncounter.getIndication());
        setPatient(fhirEncounter, prescription);
        prescription.setDateWritten(getDateWritten(drugOrder));
        prescription.setMedication(getMedication(drugOrder));
        prescription.setPrescriber(getParticipant(fhirEncounter));
        setDoseInstructions(drugOrder, prescription, systemProperties);
        Identifier identifier = prescription.addIdentifier();
        identifier.setValueSimple(new EntityReference().build(Order.class, systemProperties, order.getUuid()));
        FHIRResources.add(new FHIRResource("Medication Prescription", prescription.getIdentifier(), prescription));
        return FHIRResources;
    }

    protected ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private void setDoseInstructions(DrugOrder drugOrder, MedicationPrescription prescription, SystemProperties systemProperties) {
        MedicationPrescription.MedicationPrescriptionDosageInstructionComponent dosageInstruction = prescription.addDosageInstruction();
        dosageInstruction.setRoute(codableConceptService.getTRValueSetCodeableConcept(drugOrder.getRoute(), systemProperties.getTrValuesetUrl(ROUTE)));
        setDoseQuantity(drugOrder, dosageInstruction, systemProperties);
        dosageInstruction.setTiming(getSchedule(drugOrder));
    }


    private Schedule getSchedule(DrugOrder drugOrder) {
        Schedule schedule = new Schedule();
        setEvent(drugOrder, schedule);
        setRepeatComponent(drugOrder, schedule);
        return schedule;
    }

    private void setEvent(DrugOrder drugOrder, Schedule schedule) {
        java.util.Date scheduledDate = drugOrder.getScheduledDate();
        if (scheduledDate != null) {
            Period period = schedule.addEvent();
            period.setStartSimple(new DateAndTime(scheduledDate));
        }
    }

    private void setRepeatComponent(DrugOrder drugOrder, Schedule schedule) {
        Decimal duration = getDuration();
        String conceptName = drugOrder.getFrequency().getConcept().getName().getName();
        Schedule.UnitsOfTime frequencyUnit = unitsHelpers.getUnitsOfTime(conceptName);
        ScheduleRepeatComponent repeatComponent = new ScheduleRepeatComponent(duration, new Enumeration<>(frequencyUnit));
        repeatComponent.setFrequencySimple(getFrequencyPerUnit(drugOrder, frequencyUnit, unitsHelpers));
        repeatComponent.setUnitsSimple(frequencyUnit);
        setCount(drugOrder, unitsHelpers, schedule, frequencyUnit, repeatComponent);
        schedule.setRepeat(repeatComponent);
    }

    private Decimal getDuration() {
        Decimal duration = new Decimal();
        duration.setValue(new BigDecimal(DEFAULT_DURATION));
        return duration;
    }

    private void setCount(DrugOrder drugOrder, UnitsHelpers unitsHelpers, Schedule schedule, Schedule.UnitsOfTime frequencyUnit, ScheduleRepeatComponent repeatComponent) {
        double unitsToDaysMultiplier = getUnitsToDaysMultiplier(drugOrder, unitsHelpers, frequencyUnit);
        Integer integer = new Integer();
        repeatComponent.setCount(integer);
        int countValue = (int) Math.round(drugOrder.getDuration().intValue() * unitsToDaysMultiplier);
        integer.setValue(countValue);
    }

    private double getUnitsToDaysMultiplier(DrugOrder drugOrder, UnitsHelpers unitsHelpers, Schedule.UnitsOfTime frequnecyUnit) {
        UnitsHelpers.UnitToDaysConverter unitToDaysConverter = unitsHelpers.getDurationUnitToUnitMapper().get(drugOrder.getDurationUnits().getName().getName());
        double unitsToDaysMultiplier = 1;
        if (!unitToDaysConverter.getUnitsOfTime().toCode().equals(frequnecyUnit.toCode())) {
            unitsToDaysMultiplier = unitToDaysConverter.getInDays() * unitsHelpers.getUnitsOfTimeMapper().get(frequnecyUnit).getInDays();
        }
        return unitsToDaysMultiplier;
    }

    private int getFrequencyPerUnit(DrugOrder drugOrder, Schedule.UnitsOfTime frequencyUnit, UnitsHelpers unitsHelpers) {
        double frequencyPerUnit = (drugOrder.getFrequency().getFrequencyPerDay()) * unitsHelpers.getUnitsOfTimeMapper().get(frequencyUnit).getInDays();
        return (int) Math.round(frequencyPerUnit);
    }


    private void setDoseQuantity(DrugOrder drugOrder, MedicationPrescription.MedicationPrescriptionDosageInstructionComponent dosageInstruction, SystemProperties systemProperties) {
        Quantity doseQuantity = new Quantity();
        Decimal dose = new Decimal();
        dose.setValue(new BigDecimal(drugOrder.getDose()));
        String code = codableConceptService.getTRValueSetCode(drugOrder.getDoseUnits());
        doseQuantity.setCodeSimple(code);
        if (null != idMappingsRepository.findByInternalId(drugOrder.getDoseUnits().getUuid())) {
            doseQuantity.setSystemSimple(systemProperties.getTrValuesetUrl(QUANTITY_UNITS));
        }
        dosageInstruction.setDoseQuantity(doseQuantity.setValue(dose));
    }

    private ResourceReference getMedication(DrugOrder drugOrder) {
        ResourceReference resourceReference = new ResourceReference();
        String uuid = drugOrder.getDrug().getUuid();
        IdMapping idMapping = idMappingsRepository.findByInternalId(uuid);
        if (null != idMapping)
            resourceReference.setReferenceSimple(idMapping.getUri());
        resourceReference.setDisplaySimple(drugOrder.getDrug().getDisplayName());
        return resourceReference;
    }

    private DateTime getDateWritten(DrugOrder drugOrder) {
        DateTime dateTime = new DateTime();
        dateTime.setValue(new DateAndTime(drugOrder.getDateCreated()));
        return dateTime;
    }

    private void setPatient(Encounter fhirEncounter, MedicationPrescription medicationPrescription) {
        medicationPrescription.setPatient(fhirEncounter.getSubject());
    }
}
