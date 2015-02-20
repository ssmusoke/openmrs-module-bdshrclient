package org.openmrs.module.fhir.mapper.bundler;


import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.EncounterProvider;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Set;

@Component
public class EncounterMapper {

    private Logger logger = Logger.getLogger(EncounterMapper.class);

    public Encounter map(org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {
        Encounter encounter = new Encounter();
        setEncounterReference(openMrsEncounter, encounter, systemProperties);
        setStatus(encounter);
        setClass(openMrsEncounter, encounter);
        setSubject(openMrsEncounter, encounter, systemProperties);
        setParticipant(openMrsEncounter, encounter, systemProperties);
        setServiceProvider(encounter, systemProperties);
        setIdentifiers(encounter, openMrsEncounter, systemProperties);
        setType(encounter, openMrsEncounter);
        return encounter;
    }

    private void setEncounterReference(org.openmrs.Encounter openMrsEncounter, Encounter encounter,
                                       SystemProperties systemProperties) {
        final ResourceReference encounterRef = new ResourceReference();
        String encounterId = openMrsEncounter.getUuid();
        encounterRef.setReferenceSimple(getReference(org.openmrs.Encounter.class, systemProperties, encounterId));
        encounterRef.setDisplaySimple("Encounter");
        encounter.setIndication(encounterRef);
    }

    private void setType(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        encounter.addType().setTextSimple(openMrsEncounter.getEncounterType().getName());
    }

    private void setIdentifiers(Encounter encounter, org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {
        encounter.addIdentifier().setValueSimple(getReference(org.openmrs.Encounter.class, systemProperties, openMrsEncounter.getUuid()));
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
        String visitType = openMrsEncounter.getVisit().getVisitType().getName().toLowerCase();
        Encounter.EncounterClass encClass = identifyEncounterClass(visitType);
        if (encClass != null) {
            encounter.setClass_(new Enumeration<>(encClass));
        } else if (visitType.contains("ipd")) {
            encounter.setClass_(new Enumeration<>(Encounter.EncounterClass.inpatient));
        } else if (visitType.contains("emergency")) {
            encounter.setClass_(new Enumeration<>(Encounter.EncounterClass.emergency));
        } else {
            encounter.setClass_(new Enumeration<>(Encounter.EncounterClass.outpatient));
        }
    }

    private Encounter.EncounterClass identifyEncounterClass(final String visitType) {
        try {
            return Encounter.EncounterClass.fromCode(visitType);
        } catch (Exception e) {
            logger.warn("Could not identify FHIR Encounter.class for MRS visitType:" + visitType);
        }
        return null;
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

    private String getReference(Type type, SystemProperties systemProperties, String id) {
        return new EntityReference().build(type, systemProperties, id);
    }

    private void setParticipant(org.openmrs.Encounter openMrsEncounter, Encounter encounter, SystemProperties systemProperties) {
        final Set<EncounterProvider> encounterProviders = openMrsEncounter.getEncounterProviders();
        if (!encounterProviders.isEmpty()) {
            Encounter.EncounterParticipantComponent encounterParticipantComponent = encounter.addParticipant();
            EncounterProvider encounterProvider = encounterProviders.iterator().next();
            String providerUrl = createProviderUrl(systemProperties, encounterProvider);
            encounterParticipantComponent.setIndividual(
                    new ResourceReference().setReferenceSimple(providerUrl));
        }
    }

    private String createProviderUrl(SystemProperties systemProperties, EncounterProvider encounterProvider) {
        String identifier = encounterProvider.getProvider().getIdentifier();
        String providerUrl = String.format(systemProperties.getProviderUrlFormat(), identifier);
        return providerUrl;
    }
}
