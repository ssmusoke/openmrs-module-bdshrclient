package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu2.valueset.MedicationPrescriptionStatusEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.fhir.utils.PropertyKeyConstants;
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

@Component
public class DrugOrderMapper implements EmrOrderResourceHandler {
    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private UnitsHelpers unitsHelpers;

    @Autowired
    private CodableConceptService codableConceptService;

    @Override
    public boolean canHandle(Order order) {
        return ((order instanceof DrugOrder) && (order.getOrderType().getName().equalsIgnoreCase(MRS_DRUG_ORDER_TYPE)));
    }

    @Override
    public List<FHIRResource> map(Order order, Encounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
        List<FHIRResource> FHIRResources = new ArrayList<>();
        DrugOrder drugOrder = (DrugOrder) order;
        MedicationPrescription prescription = new MedicationPrescription();
        prescription.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValueAsString()));
        setPatient(fhirEncounter, prescription);
        prescription.setDateWritten(drugOrder.getDateCreated(), TemporalPrecisionEnum.MILLI);
        prescription.setMedication(getMedication(drugOrder));
        prescription.setPrescriber(getOrdererReference(drugOrder, fhirEncounter, systemProperties));
        setDoseInstructions(drugOrder, prescription, systemProperties);
        setStatus(drugOrder, prescription);
        String id = new EntityReference().build(Order.class, systemProperties, order.getUuid());
        prescription.addIdentifier().setValue(id);
        prescription.setId(id);
        FHIRResources.add(new FHIRResource("Medication Prescription", prescription.getIdentifier(), prescription));
        return FHIRResources;
    }

    private void setStatus(DrugOrder drugOrder, MedicationPrescription prescription) {
        if(drugOrder.getDateStopped() != null) {
            prescription.setStatus(MedicationPrescriptionStatusEnum.STOPPED);
            //TODO : set the end date for MedicationOrder
        } else {
            prescription.setStatus(MedicationPrescriptionStatusEnum.ACTIVE);
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

    private void setDoseInstructions(DrugOrder drugOrder, MedicationPrescription prescription, SystemProperties systemProperties) {
        MedicationPrescription.DosageInstruction dosageInstruction = prescription.addDosageInstruction();
        if (null != drugOrder.getRoute()) {
            dosageInstruction.setRoute(
                    codableConceptService.getTRValueSetCodeableConcept(drugOrder.getRoute(),
                            systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_ROUTE)));
        }
        setDoseQuantity(drugOrder, dosageInstruction, systemProperties);
        dosageInstruction.setScheduled(getSchedule(drugOrder));
        dosageInstruction.setAsNeeded(new BooleanDt(drugOrder.getAsNeeded()));
    }

    private IDatatype getSchedule(DrugOrder drugOrder) {
        TimingDt timing = new TimingDt();
        TimingDt.Repeat repeat = new TimingDt.Repeat();

        setBounds(drugOrder, repeat);
        setFrequencyAndPeriod(drugOrder, repeat);

        timing.setRepeat(repeat);
        return timing;
    }

    private void setFrequencyAndPeriod(DrugOrder drugOrder, TimingDt.Repeat repeat) {
        String frequencyConceptName = drugOrder.getFrequency().getConcept().getName().getName();
        UnitsHelpers.FrequencyUnits frequencyUnit = unitsHelpers.getFrequencyUnits(frequencyConceptName);
        repeat.setFrequency(frequencyUnit.getFrequency());
        repeat.setPeriod(frequencyUnit.getFrequencyPeriod());
        repeat.setPeriodUnits(frequencyUnit.getUnitOfTime());
    }

    private void setBounds(DrugOrder drugOrder, TimingDt.Repeat repeat) {
        PeriodDt period = new PeriodDt();
        period.setStart(drugOrder.getEffectiveStartDate(), TemporalPrecisionEnum.MILLI);
        period.setEnd(drugOrder.getEffectiveStopDate(), TemporalPrecisionEnum.MILLI);
        repeat.setBounds(period);
    }

    private void setDoseQuantity(DrugOrder drugOrder, MedicationPrescription.DosageInstruction dosageInstruction, SystemProperties systemProperties) {
        if (null != drugOrder.getDose()) {
            QuantityDt doseQuantity = new QuantityDt();
            DecimalDt dose = new DecimalDt();
            dose.setValue(new BigDecimal(drugOrder.getDose()));
            if (null != drugOrder.getDoseUnits()) {
                if (null != idMappingsRepository.findByInternalId(drugOrder.getDoseUnits().getUuid())) {
                    String code = codableConceptService.getTRValueSetCode(drugOrder.getDoseUnits());
                    doseQuantity.setCode(code);
                    doseQuantity.setSystem(systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_QUANTITY_UNITS));
                }
            }
            dosageInstruction.setDose(doseQuantity.setValue(dose));
        }
    }

    private ResourceReferenceDt getMedication(DrugOrder drugOrder) {
        ResourceReferenceDt resourceReference = new ResourceReferenceDt();
        String uuid = drugOrder.getDrug().getUuid();
        IdMapping idMapping = idMappingsRepository.findByInternalId(uuid);
        if (null != idMapping)
            resourceReference.setReference(idMapping.getUri());
        resourceReference.setDisplay(drugOrder.getDrug().getDisplayName());
        return resourceReference;
    }

    private void setPatient(Encounter fhirEncounter, MedicationPrescription medicationPrescription) {
        medicationPrescription.setPatient(fhirEncounter.getPatient());
    }
}
