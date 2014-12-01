package org.openmrs.module.fhir.mapper.bundler;


import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.*;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Set;

@Component
public class EncounterMapper {
    public Encounter map(org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {
        Encounter encounter = new Encounter();
        setEncounterReference(openMrsEncounter, encounter, systemProperties);
        setStatus(encounter);
        setClass(openMrsEncounter, encounter);
        setSubject(openMrsEncounter, encounter, systemProperties);
        setParticipant(openMrsEncounter, encounter);
        setServiceProvider(encounter, systemProperties);
        setIdentifiers(encounter, openMrsEncounter);
        setType(encounter, openMrsEncounter);
        return encounter;
    }

    private void setEncounterReference(org.openmrs.Encounter openMrsEncounter, Encounter encounter,
                                       SystemProperties systemProperties) {
        final ResourceReference encounterRef = new ResourceReference();
        String encounterId = openMrsEncounter.getUuid();
        encounterRef.setReferenceSimple(getReference(org.openmrs.Encounter.class, systemProperties, encounterId));
        encounterRef.setDisplaySimple("Encounter - " + encounterId);
        encounter.setIndication(encounterRef);
    }

    private void setType(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        encounter.addType().setTextSimple(openMrsEncounter.getEncounterType().getName());
    }

    private void setIdentifiers(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        encounter.addIdentifier().setValueSimple(openMrsEncounter.getUuid());
    }

    private void setServiceProvider(Encounter encounter, SystemProperties systemProperties) {
        encounter.setServiceProvider(new ResourceReference().setReferenceSimple(
                getReference(Location.class, systemProperties, systemProperties.getFacilityId())
        ));
    }

    private void setStatus(Encounter encounter) {
        encounter.setStatus(new Enumeration<>(Encounter.EncounterState.finished));
    }

    private void setClass(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        VisitType visitType = openMrsEncounter.getVisit().getVisitType();
        if ("IPD".equals(visitType.getName())) {
            encounter.setClass_(new Enumeration<>(Encounter.EncounterClass.inpatient));
        } else {
            encounter.setClass_(new Enumeration<>(Encounter.EncounterClass.outpatient));
        }
    }

    private void setSubject(org.openmrs.Encounter openMrsEncounter, Encounter encounter, SystemProperties systemProperties) {
        PersonAttribute healthId = openMrsEncounter.getPatient().getAttribute(Constants.HEALTH_ID_ATTRIBUTE);
        if (null != healthId) {
            ResourceReference subject = new ResourceReference()
                    .setDisplaySimple(healthId.getValue())
                    .setReferenceSimple(getReference(Patient.class, systemProperties, healthId.getValue()));

            encounter.setSubject(subject);
        } else {
            throw new RuntimeException("The patient has not been synced yet");
        }
    }

    private String getReference(Type type, SystemProperties systemProperties, String healthId) {
        return new EntityReference().build(type, systemProperties, healthId);
    }

    private void setParticipant(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        final Set<EncounterProvider> encounterProviders = openMrsEncounter.getEncounterProviders();
        if (!encounterProviders.isEmpty()) {
            Encounter.EncounterParticipantComponent encounterParticipantComponent = encounter.addParticipant();
            EncounterProvider encounterProvider = encounterProviders.iterator().next();
            encounterParticipantComponent.setIndividual(new ResourceReference().setReferenceSimple(encounterProvider.getProvider().getUuid()));
        }
    }
}
