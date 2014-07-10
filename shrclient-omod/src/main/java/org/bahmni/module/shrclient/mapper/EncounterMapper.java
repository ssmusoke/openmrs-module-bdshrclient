package org.bahmni.module.shrclient.mapper;


import org.bahmni.module.shrclient.util.Constants;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.EncounterProvider;
import org.openmrs.PersonAttribute;
import org.openmrs.VisitType;

public class EncounterMapper {

    // TODO: Not complete yet
    public Encounter map(org.openmrs.Encounter openMrsEncounter) {
        Encounter encounter = new Encounter();
        encounter.setIndication(new ResourceReference().setReferenceSimple(openMrsEncounter.getUuid()));
        setStatus(encounter);
        setClass(openMrsEncounter, encounter);
        setSubject(openMrsEncounter, encounter);
        setParticipant(openMrsEncounter, encounter);
        setServiceProvider(encounter);
        setIdentifiers(encounter, openMrsEncounter);
        setType(encounter, openMrsEncounter);
        //setDiagnosis(openMrsEncounter, encounter);
        return encounter;
    }

//    private void setDiagnosis(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
//        Set<Obs> allObs = openMrsEncounter.getAllObs(true);
//        ConceptClass diagnosisClass = Context.getConceptService().getConceptClassByName("Diagnosis");
//        for (Obs obs : allObs) {
//            if(obs.getConcept().getConceptClass().getName().equals("Diagnosis")) {
//                dignosisMapper.map(obs);
//            }
//        }
//    }

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
        EncounterProvider encounterProvider = openMrsEncounter.getEncounterProviders().iterator().next();
        encounterParticipantComponent.setIndividual(new ResourceReference().setReferenceSimple(encounterProvider.getProvider().getUuid()));
    }
}
