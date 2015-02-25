package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getComposition;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;
import static org.openmrs.module.fhir.utils.ParticipantHelper.extractProviderId;
import static org.openmrs.module.fhir.utils.ParticipantHelper.getOpenMRSDeamonUser;
import static org.openmrs.module.fhir.utils.ParticipantHelper.setCreator;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    UserService userService;

    @Autowired
    private ProviderLookupService providerLookupService;

    public Encounter map(Patient emrPatient, AtomFeed feed) throws ParseException {
        Composition composition = getComposition(feed);
        final org.hl7.fhir.instance.model.Encounter encounter = getEncounter(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, feed);
        setEncounterProviderAndCreator(newEmrEncounter, encounter);
        return newEmrEncounter;
    }

    public void setEncounterProviderAndCreator(org.openmrs.Encounter newEmrEncounter, org.hl7.fhir.instance.model.Encounter fhirEncounter) {
        User systemUser = getOpenMRSDeamonUser(userService);
        setCreator(newEmrEncounter, systemUser);
        setCreator(newEmrEncounter.getVisit(), systemUser);
        List<org.hl7.fhir.instance.model.Encounter.EncounterParticipantComponent> participants = fhirEncounter.getParticipant();
        String providerUrl = null;
        if (!CollectionUtils.isEmpty(participants)) {
            providerUrl = participants.get(0).getIndividual().getReferenceSimple();
        }
        String providerId = extractProviderId(providerUrl);
        newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID),
                providerLookupService.getShrClientSystemProvider(providerId));
    }
}
