package org.openmrs.module.fhir.mapper.bundler;


import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.ResourceReference;
import org.openmrs.*;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.OMRSLocationService;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Set;

@Component
public class EncounterMapper {

    @Autowired
    private OMRSLocationService omrsLocationService;


    private Logger logger = Logger.getLogger(EncounterMapper.class);
    public Encounter map(org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {
        Encounter encounter = new Encounter();
        encounter.setStatus(new Enumeration<>(Encounter.EncounterState.finished));
        setEncounterReference(openMrsEncounter, encounter, systemProperties);
        setClass(openMrsEncounter, encounter);
        setSubject(openMrsEncounter, encounter, systemProperties);
        setParticipant(openMrsEncounter, encounter, systemProperties);
        encounter.setServiceProvider(getServiceProvider(openMrsEncounter, systemProperties));
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

    public ResourceReference getServiceProvider(org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {

        boolean isHIEFacility = omrsLocationService.isLocationHIEFacility(openMrsEncounter.getLocation());
        String serviceProviderId = null;
        serviceProviderId = isHIEFacility ? omrsLocationService.getLocationHIEIdentifier(openMrsEncounter.getLocation()) : systemProperties.getFacilityId();
        return new ResourceReference().setReferenceSimple(
                getReference(Location.class, systemProperties, serviceProviderId));
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
            EncounterProvider encounterProvider = encounterProviders.iterator().next();
            Provider provider = encounterProvider.getProvider();
            if (provider == null) return;
            String identifier = provider.getIdentifier();
            String providerUrl = getReference(EncounterProvider.class, systemProperties, identifier);
            if (providerUrl == null)
                return;
            Encounter.EncounterParticipantComponent encounterParticipantComponent = encounter.addParticipant();
            encounterParticipantComponent.setIndividual(new ResourceReference().setReferenceSimple(providerUrl));
        }
    }


}
