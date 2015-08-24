package org.openmrs.module.fhir.mapper.bundler;


import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;
import org.apache.log4j.Logger;
import org.openmrs.EncounterProvider;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.Provider;
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
        encounter.setStatus(EncounterStateEnum.FINISHED);
        setClass(openMrsEncounter, encounter);
        setPatientReference(openMrsEncounter, encounter, systemProperties);
        setParticipant(openMrsEncounter, encounter, systemProperties);
        encounter.setServiceProvider(getServiceProvider(openMrsEncounter, systemProperties));
        setIdentifiers(encounter, openMrsEncounter, systemProperties);
        setType(encounter, openMrsEncounter);
        return encounter;
    }

    private void setType(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        encounter.addType().setText(openMrsEncounter.getEncounterType().getName());
    }

    private void setIdentifiers(Encounter encounter, org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {
        String id = new EntityReference().build(IResource.class, systemProperties, openMrsEncounter.getUuid());
        encounter.setId(id);
        encounter.addIdentifier().setValue(id);
    }

    public ResourceReferenceDt getServiceProvider(org.openmrs.Encounter openMrsEncounter, SystemProperties systemProperties) {
        boolean isHIEFacility = omrsLocationService.isLocationHIEFacility(openMrsEncounter.getLocation());
        String serviceProviderId = null;
        serviceProviderId = isHIEFacility ? omrsLocationService.getLocationHIEIdentifier(openMrsEncounter.getLocation()) : systemProperties.getFacilityId();
        return new ResourceReferenceDt().setReference(
                getReference(Location.class, systemProperties, serviceProviderId));
    }

    private void setClass(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        String visitType = openMrsEncounter.getVisit().getVisitType().getName().toLowerCase();
        EncounterClassEnum encClass = identifyEncounterClass(visitType);
        if (encClass != null) {
            encounter.setClassElement(encClass);
        } else if (visitType.contains("ipd")) {
            encounter.setClassElement(EncounterClassEnum.INPATIENT);
        } else if (visitType.contains("emergency")) {
            encounter.setClassElement(EncounterClassEnum.EMERGENCY);
        } else {
            encounter.setClassElement(EncounterClassEnum.OUTPATIENT);
        }
    }

    private EncounterClassEnum identifyEncounterClass(final String visitType) {
        try {
            return EncounterClassEnum.valueOf(visitType);
        } catch (Exception e) {
            logger.warn("Could not identify FHIR Encounter.class for MRS visitType:" + visitType);
        }
        return null;
    }

    private void setPatientReference(org.openmrs.Encounter openMrsEncounter, Encounter encounter, SystemProperties systemProperties) {
        PersonAttribute healthId = openMrsEncounter.getPatient().getAttribute(Constants.HEALTH_ID_ATTRIBUTE);
        if (null != healthId) {
            ResourceReferenceDt subject = new ResourceReferenceDt()
                    .setDisplay(healthId.getValue())
                    .setReference(getReference(Patient.class, systemProperties, healthId.getValue()));
            encounter.setPatient(subject);
        } else {
            throw new RuntimeException("The patient has not been synced yet");
        }
    }

    private String getReference(Type type, SystemProperties systemProperties, String id) {
        return new EntityReference().build(type, systemProperties, id);
    }

    private void setParticipant(org.openmrs.Encounter openMrsEncounter, Encounter encounter, SystemProperties systemProperties) {
        final Set<EncounterProvider> encounterProviders = openMrsEncounter.getEncounterProviders();
        for (EncounterProvider encounterProvider : encounterProviders) {
            Provider provider = encounterProvider.getProvider();
            if (provider == null) return;
            String identifier = provider.getIdentifier();
            if (!isHIEProvider(identifier)) continue;
            String providerUrl = getReference(Provider.class, systemProperties, identifier);
            if (providerUrl == null)
                continue;
            Encounter.Participant participant = encounter.addParticipant();
            participant.setIndividual(new ResourceReferenceDt().setReference(providerUrl));
        }
    }

    private boolean isHIEProvider(String identifier) {
        try {
            Integer.parseInt(identifier);
        } catch (Exception e) {
            logger.warn("Provider is not an HIE provider.");
            return false;
        }
        return true;
    }
}
