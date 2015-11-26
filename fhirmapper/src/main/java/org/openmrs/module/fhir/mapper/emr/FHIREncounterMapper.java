package org.openmrs.module.fhir.mapper.emr;


import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.apache.commons.collections4.CollectionUtils;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ShrEncounterComposition;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.fhir.utils.VisitLookupService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
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
    public IdMappingsRepository idMappingsRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    public VisitLookupService visitLookupService;

    @Autowired
    private ProviderLookupService providerLookupService;

    @Autowired
    private FHIRSubResourceMapper fhirSubResourceMapper;

    public org.openmrs.Encounter map(Patient emrPatient, ShrEncounterComposition encounterComposition, SystemProperties systemProperties) throws ParseException {
        final ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = FHIRBundleHelper.getEncounter(encounterComposition.getBundle());
        Composition composition = FHIRBundleHelper.getComposition(encounterComposition.getBundle());
        Date encounterDate = composition.getDate();
        org.openmrs.Encounter emrEncounter = getOrCreateEncounter(encounterComposition.getShrEncounterId());
        emrEncounter.setEncounterDatetime(encounterDate);

        emrEncounter.setPatient(emrPatient);

        fhirSubResourceMapper.map(emrEncounter, encounterComposition, systemProperties);

        final String encounterTypeName = fhirEncounter.getType().get(0).getText();
        final EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        emrEncounter.setEncounterType(encounterType);

        ResourceReferenceDt serviceProvider = fhirEncounter.getServiceProvider();
        if (serviceProvider != null && !serviceProvider.isEmpty()) {
            setInternalFacilityId(emrEncounter, new EntityReference().parse(Location.class, serviceProvider.getReference().getValue()));
        } else {
            setFacilityIdFromProvider(fhirEncounter, emrEncounter);
        }
        Visit visit = visitLookupService.findOrInitializeVisit(emrPatient, encounterDate, fhirEncounter.getClassElement());
        emrEncounter.setVisit(visit);
        visit.addEncounter(emrEncounter);

        setEncounterProvider(emrEncounter, fhirEncounter);
        return emrEncounter;
    }

    public org.openmrs.Encounter getOrCreateEncounter(String fhirEncounterId) {
        org.openmrs.Encounter emrEncounter = null;
        IdMapping mapping = idMappingsRepository.findByExternalId(fhirEncounterId);
        if (mapping != null) {
            emrEncounter = encounterService.getEncounterByUuid(mapping.getInternalId());
        }
        if (emrEncounter == null) {
            return new org.openmrs.Encounter();
        }
        return emrEncounter;
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
        IdMapping idMapping = idMappingsRepository.findByExternalId(facilityId);
        if (idMapping == null) return;
        Location location = locationService.getLocationByUuid(idMapping.getInternalId());
        emrEncounter.setLocation(location);
    }
}
