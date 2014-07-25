package org.openmrs.module.fhir.mapper.bundler;


import org.openmrs.module.fhir.utils.Constants;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.EncounterProvider;
import org.openmrs.PersonAttribute;
import org.openmrs.VisitType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EncounterMapper {

    // TODO: Not complete yet
    public Encounter map(org.openmrs.Encounter openMrsEncounter) {
        Encounter encounter = new Encounter();
        final ResourceReference encounterRef = new ResourceReference();
        encounterRef.setReferenceSimple(openMrsEncounter.getUuid());
        encounterRef.setDisplaySimple("encounter");
        encounter.setIndication(encounterRef);
        setStatus(encounter);
        setClass(openMrsEncounter, encounter);
        setSubject(openMrsEncounter, encounter);
        setParticipant(openMrsEncounter, encounter);
        setServiceProvider(encounter);
        setIdentifiers(encounter, openMrsEncounter);
        setType(encounter, openMrsEncounter);
        return encounter;
    }

    private void setType(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        encounter.addType().setTextSimple(openMrsEncounter.getEncounterType().getName());
    }

    private void setIdentifiers(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        encounter.addIdentifier().setValueSimple(openMrsEncounter.getUuid());
    }

    private void setServiceProvider(Encounter encounter) {
        encounter.setServiceProvider(new ResourceReference().setReferenceSimple("Bahmni"));
    }

    private void setStatus(Encounter encounter) {
        encounter.setStatus(new Enumeration<Encounter.EncounterState>(Encounter.EncounterState.finished));
    }

    private void setClass(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        VisitType visitType = openMrsEncounter.getVisit().getVisitType();
        if ("IPD".equals(visitType.getName())) {
            encounter.setClass_(new Enumeration<Encounter.EncounterClass>(Encounter.EncounterClass.inpatient));
        } else {
            encounter.setClass_(new Enumeration<Encounter.EncounterClass>(Encounter.EncounterClass.outpatient));
        }
    }

    private void setSubject(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        PersonAttribute healthId = openMrsEncounter.getPatient().getAttribute(Constants.HEALTH_ID_ATTRIBUTE);
        if (null != healthId) {
            encounter.setSubject(new ResourceReference().setReferenceSimple(healthId.getValue()));
        } else {
            throw new RuntimeException("The patient has not been synced yet");
        }
    }

    private void setParticipant(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        Encounter.EncounterParticipantComponent encounterParticipantComponent = encounter.addParticipant();
        final Set<EncounterProvider> encounterProviders = openMrsEncounter.getEncounterProviders();
        if (!encounterProviders.isEmpty()) {
            EncounterProvider encounterProvider = encounterProviders.iterator().next();
            encounterParticipantComponent.setIndividual(new ResourceReference().setReferenceSimple(encounterProvider.getProvider().getUuid()));
        }
    }
}
