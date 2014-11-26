package org.openmrs.module.fhir.mapper.bundler;


import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.EncounterProvider;
import org.openmrs.PersonAttribute;
import org.openmrs.VisitType;
import org.openmrs.module.fhir.utils.Constants;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
        Properties properties = getProperties("shr.properties");
        String facilityId = properties.getProperty("shr.facilityId");
        encounter.setServiceProvider(new ResourceReference().setReferenceSimple(facilityId));
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
        final Set<EncounterProvider> encounterProviders = openMrsEncounter.getEncounterProviders();
        if (!encounterProviders.isEmpty()) {
            Encounter.EncounterParticipantComponent encounterParticipantComponent = encounter.addParticipant();
            EncounterProvider encounterProvider = encounterProviders.iterator().next();
            encounterParticipantComponent.setIndividual(new ResourceReference().setReferenceSimple(encounterProvider.getProvider().getUuid()));
        }
    }
    private Properties getProperties(String resource) {
        try {
            Properties properties = new Properties();
            final File file = new File(System.getProperty("user.home") + File.separator + ".OpenMRS" + File.separator + resource);
            final InputStream inputStream;
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            } else {
                inputStream = getClass().getClassLoader().getResourceAsStream(resource);
            }
            properties.load(inputStream);
            return properties;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
