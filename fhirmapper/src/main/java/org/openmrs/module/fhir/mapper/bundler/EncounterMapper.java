package org.openmrs.module.fhir.mapper.bundler;


import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;
import org.apache.log4j.Logger;
import org.openmrs.*;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.utils.OMRSLocationService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Set;

@Component
public class EncounterMapper {

    @Autowired
    private OMRSLocationService omrsLocationService;
    @Autowired
    private ProviderLookupService providerLookupService;

    private Logger logger = Logger.getLogger(EncounterMapper.class);

    public FHIREncounter map(org.openmrs.Encounter openMrsEncounter, String healthId, SystemProperties systemProperties) {
        Encounter encounter = new Encounter();
        encounter.setStatus(EncounterStateEnum.FINISHED);
        setClass(openMrsEncounter, encounter);
        setPatientReference(healthId, encounter, systemProperties);
        setParticipant(openMrsEncounter, encounter);
        encounter.setServiceProvider(getServiceProvider(openMrsEncounter, systemProperties));
        setIdentifiers(encounter, openMrsEncounter, systemProperties);
        setType(encounter, openMrsEncounter);
        setPeriod(encounter, openMrsEncounter);
        return new FHIREncounter(encounter);
    }

    private void setPeriod(Encounter encounter, org.openmrs.Encounter openMrsEncounter) {
        Visit encounterVisit = openMrsEncounter.getVisit();
        PeriodDt visitPeriod = new PeriodDt();
        visitPeriod.setStart(encounterVisit.getStartDatetime(), TemporalPrecisionEnum.MILLI);
        visitPeriod.setEnd(encounterVisit.getStopDatetime(), TemporalPrecisionEnum.MILLI);
        encounter.setPeriod(visitPeriod);
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
            return EncounterClassEnum.forCode(visitType);
        } catch (Exception e) {
            logger.warn("Could not identify FHIR Encounter.class for MRS visitType:" + visitType);
        }
        return null;
    }

    private void setPatientReference(String healthId, Encounter encounter, SystemProperties systemProperties) {
        if (null != healthId) {
            ResourceReferenceDt subject = new ResourceReferenceDt()
                    .setDisplay(healthId)
                    .setReference(getReference(Patient.class, systemProperties, healthId));
            encounter.setPatient(subject);
        } else {
            throw new RuntimeException("The patient has not been synced yet");
        }
    }

    private String getReference(Type type, SystemProperties systemProperties, String id) {
        return new EntityReference().build(type, systemProperties, id);
    }

    private void setParticipant(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        final Set<EncounterProvider> encounterProviders = openMrsEncounter.getEncounterProviders();
        for (EncounterProvider encounterProvider : encounterProviders) {
            Provider provider = encounterProvider.getProvider();
            String providerUrl = providerLookupService.getProviderRegistryUrl(provider);
            if (providerUrl == null)
                continue;
            Encounter.Participant participant = encounter.addParticipant();
            participant.setIndividual(new ResourceReferenceDt().setReference(providerUrl));
        }
    }
}
