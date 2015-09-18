package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.valueset.MedicationOrderStatusEnum;
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
        MedicationOrder medicationOrder = new MedicationOrder();
        medicationOrder.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValueAsString()));
        setPatient(fhirEncounter, medicationOrder);
        medicationOrder.setDateWritten(drugOrder.getDateCreated(), TemporalPrecisionEnum.MILLI);
        medicationOrder.setMedication(getMedication(drugOrder));
        medicationOrder.setPrescriber(getOrdererReference(drugOrder, fhirEncounter, systemProperties));
        setDoseInstructions(drugOrder, medicationOrder, systemProperties);
        setStatus(drugOrder, medicationOrder);
        String id = new EntityReference().build(Order.class, systemProperties, order.getUuid());
        medicationOrder.addIdentifier().setValue(id);
        medicationOrder.setId(id);
        FHIRResources.add(new FHIRResource("Medication Prescription", medicationOrder.getIdentifier(), medicationOrder));
        return FHIRResources;
    }

    private void setStatus(DrugOrder drugOrder, MedicationOrder medicationOrder) {
        if (drugOrder.getDateStopped() != null) {
            medicationOrder.setStatus(MedicationOrderStatusEnum.STOPPED);
            medicationOrder.setDateEnded(drugOrder.getDateStopped(), TemporalPrecisionEnum.MILLI);
        } else {
            medicationOrder.setStatus(MedicationOrderStatusEnum.ACTIVE);
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

    private void setDoseInstructions(DrugOrder drugOrder, MedicationOrder medicationOrder, SystemProperties systemProperties) {
        MedicationOrder.DosageInstruction dosageInstruction = medicationOrder.addDosageInstruction();
        if (null != drugOrder.getRoute()) {
            dosageInstruction.setRoute(
                    codableConceptService.getTRValueSetCodeableConcept(drugOrder.getRoute(),
                            systemProperties.getTrValuesetUrl(PropertyKeyConstants.TR_VALUESET_ROUTE)));
        }
        setDoseQuantity(drugOrder, dosageInstruction, systemProperties);
        dosageInstruction.setTiming(getTiming(drugOrder));
        dosageInstruction.setAsNeeded(new BooleanDt(drugOrder.getAsNeeded()));
    }

    private TimingDt getTiming(DrugOrder drugOrder) {
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

    private void setDoseQuantity(DrugOrder drugOrder, MedicationOrder.DosageInstruction dosageInstruction, SystemProperties systemProperties) {
        if (null != drugOrder.getDose()) {
            SimpleQuantityDt doseQuantity = new SimpleQuantityDt();
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
