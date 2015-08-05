package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.util.ConceptCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getComposition;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private ProviderLookupService providerLookupService;

    public Encounter map(Patient emrPatient, AtomFeed feed, ConceptCache conceptCache) throws ParseException {
        Composition composition = getComposition(feed);
        final org.hl7.fhir.instance.model.Encounter encounter = getEncounter(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, feed, conceptCache);
        setEncounterProvider(newEmrEncounter, encounter);
        return newEmrEncounter;
    }

    public void setEncounterProvider(org.openmrs.Encounter newEmrEncounter, org.hl7.fhir.instance.model.Encounter fhirEncounter) {
        List<org.hl7.fhir.instance.model.Encounter.EncounterParticipantComponent> participants = fhirEncounter.getParticipant();
        String providerUrl = null;
        if (!CollectionUtils.isEmpty(participants)) {
            providerUrl = participants.get(0).getIndividual().getReferenceSimple();
        }
        newEmrEncounter.addProvider(encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID),
                providerLookupService.getProviderByReferenceUrl(providerUrl));
    }
}
