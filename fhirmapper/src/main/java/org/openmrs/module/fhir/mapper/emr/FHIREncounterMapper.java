package org.openmrs.module.fhir.mapper.emr;


import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.apache.commons.collections4.CollectionUtils;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.fhir.utils.VisitLookupService;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.openmrs.module.fhir.Constants.ORGANIZATION_ATTRIBUTE_TYPE_NAME;

@Component
public class FHIREncounterMapper {
    @Autowired
    private EncounterService encounterService;

    @Autowired
    public IdMappingRepository idMappingRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    public VisitLookupService visitLookupService;

    @Autowired
    private ProviderLookupService providerLookupService;

    @Autowired
    private FHIRSubResourceMapper fhirSubResourceMapper;

    public org.openmrs.Encounter map(Patient emrPatient, ShrEncounterBundle shrEncounterBundle, SystemProperties systemProperties) throws ParseException {
        final ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = FHIRBundleHelper.getEncounter(shrEncounterBundle.getBundle());
        Composition composition = FHIRBundleHelper.getComposition(shrEncounterBundle.getBundle());
        Date encounterDate = composition.getDate();
        org.openmrs.Encounter openmrsEncounter = getOrCreateEmrEncounter(shrEncounterBundle.getShrEncounterId());
        openmrsEncounter.setEncounterDatetime(encounterDate);

        openmrsEncounter.setPatient(emrPatient);

        fhirSubResourceMapper.map(openmrsEncounter, shrEncounterBundle, systemProperties);

        final String encounterTypeName = fhirEncounter.getType().get(0).getText();
        final EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        openmrsEncounter.setEncounterType(encounterType);

        ResourceReferenceDt serviceProvider = fhirEncounter.getServiceProvider();
        if (serviceProvider != null && !serviceProvider.isEmpty()) {
            setInternalFacilityId(openmrsEncounter, new EntityReference().parse(Location.class, serviceProvider.getReference().getValue()));
        } else {
            setFacilityIdFromProvider(fhirEncounter, openmrsEncounter);
        }
        Visit visit = visitLookupService.findOrInitializeVisit(emrPatient, encounterDate, fhirEncounter.getClassElement());
        openmrsEncounter.setVisit(visit);
        visit.addEncounter(openmrsEncounter);

        setEncounterProvider(openmrsEncounter, fhirEncounter);
        return openmrsEncounter;
    }

    public org.openmrs.Encounter getOrCreateEmrEncounter(String fhirEncounterId) {
        org.openmrs.Encounter openmrsEncounter = null;
        IdMapping mapping = idMappingRepository.findByExternalId(fhirEncounterId, IdMappingType.ENCOUNTER);
        if (mapping != null) {
            openmrsEncounter = encounterService.getEncounterByUuid(mapping.getInternalId());
        }
        if (openmrsEncounter == null) {
            openmrsEncounter = new org.openmrs.Encounter();
        }
        return openmrsEncounter;
    }

    public void setEncounterProvider(org.openmrs.Encounter newEmrEncounter, ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter) {
        List<ca.uhn.fhir.model.dstu2.resource.Encounter.Participant> participants = fhirEncounter.getParticipant();
        if (!org.apache.commons.collections.CollectionUtils.isEmpty(participants)) {
            for (Encounter.Participant participant : participants) {
                String providerUrl = participant.getIndividual().getReference().getValue();
                Provider provider = providerLookupService.getProviderByReferenceUrl(providerUrl);
                if (provider != null)
                    newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID),
                            provider);
            }
        }
        if (CollectionUtils.isEmpty(newEmrEncounter.getEncounterProviders())) {
            Provider provider = providerLookupService.getShrClientSystemProvider();
            newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID),
                    provider);
        }
    }

    private void setFacilityIdFromProvider(Encounter fhirEncounter, org.openmrs.Encounter emrEncounter) {
        List<Encounter.Participant> participant = fhirEncounter.getParticipant();
        if (CollectionUtils.isEmpty(participant)) return;

        String providerUrl = participant.get(0).getIndividual().getReference().getValue();
        Provider provider = providerLookupService.getProviderByReferenceUrl(providerUrl);
        Set<ProviderAttribute> attributes = provider.getAttributes();
        for (ProviderAttribute attribute : attributes) {
            if (attribute.getAttributeType().getName().equals(ORGANIZATION_ATTRIBUTE_TYPE_NAME)) {
                String facilityId = attribute.getValueReference();
                setInternalFacilityId(emrEncounter, facilityId);
            }
        }
    }

    private void setInternalFacilityId(org.openmrs.Encounter emrEncounter, String facilityId) {
        IdMapping idMapping = idMappingRepository.findByExternalId(facilityId, IdMappingType.FACILITY);
        if (idMapping == null) return;
        Location location = locationService.getLocationByUuid(idMapping.getInternalId());
        emrEncounter.setLocation(location);
    }
}
