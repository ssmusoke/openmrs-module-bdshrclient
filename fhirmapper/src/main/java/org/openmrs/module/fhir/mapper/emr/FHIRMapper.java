package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private ProviderLookupService providerLookupService;

    public Encounter map(Patient emrPatient, Bundle bundle) throws ParseException {
        Composition composition = bundle.getAllPopulatedChildElementsOfType(Composition.class).get(0);
        final ca.uhn.fhir.model.dstu2.resource.Encounter encounter = bundle.getAllPopulatedChildElementsOfType(ca.uhn.fhir.model.dstu2.resource.Encounter.class).get(0);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDate(), emrPatient, bundle);
        setEncounterProvider(newEmrEncounter, encounter);
        return newEmrEncounter;
    }

    public void setEncounterProvider(org.openmrs.Encounter newEmrEncounter, ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter) {
        List<ca.uhn.fhir.model.dstu2.resource.Encounter.Participant> participants = fhirEncounter.getParticipant();
        String providerUrl = null;
        if (!CollectionUtils.isEmpty(participants)) {
            providerUrl = participants.get(0).getIndividual().getReference().getValue();
        }
        newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID),
                providerLookupService.getProviderByReferenceUrl(providerUrl));
    }
}
