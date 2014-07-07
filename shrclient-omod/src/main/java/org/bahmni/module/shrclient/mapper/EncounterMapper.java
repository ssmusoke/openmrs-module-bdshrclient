package org.bahmni.module.shrclient.mapper;


import org.bahmni.module.shrclient.util.Constants;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.EncounterProvider;
import org.openmrs.VisitType;

public class EncounterMapper {

    // TODO: Not complete yet
    public Encounter map(org.openmrs.Encounter openMrsEncounter) {
        Encounter encounter = new Encounter();
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
        encounter.setSubject(new ResourceReference().setReferenceSimple(openMrsEncounter.getPatient().getAttribute(Constants.HEALTH_ID_ATTRIBUTE).getValue()));
    }

    private void setParticipant(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        Encounter.EncounterParticipantComponent encounterParticipantComponent = encounter.addParticipant();
        EncounterProvider encounterProvider = openMrsEncounter.getEncounterProviders().iterator().next();
        encounterParticipantComponent.setIndividual(new ResourceReference().setReferenceSimple(encounterProvider.getProvider().getUuid()));
    }
}
