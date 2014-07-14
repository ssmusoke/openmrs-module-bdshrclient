package org.bahmni.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.mapper.DiagnosisMapper;
import org.bahmni.module.shrclient.mapper.EncounterMapper;
import org.bahmni.module.shrclient.util.Constants;
import org.bahmni.module.shrclient.util.FhirRestClient;
import org.hl7.fhir.instance.model.*;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAttribute;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrEncounterCreator implements EventWorker {

    private static final Logger log = Logger.getLogger(ShrEncounterCreator.class);

    private EncounterService encounterService;
    private EncounterMapper encounterMapper;
    private DiagnosisMapper diagnosisMapper;
    private FhirRestClient fhirRestClient;
    private UserService userService;


    public ShrEncounterCreator(EncounterService encounterService, EncounterMapper encounterMapper, DiagnosisMapper diagnosisMapper, FhirRestClient fhirRestClient, UserService userService) {
        this.encounterService = encounterService;
        this.encounterMapper = encounterMapper;
        this.diagnosisMapper = diagnosisMapper;
        this.fhirRestClient = fhirRestClient;
        this.userService = userService;
    }

    @Override
    public void process(Event event) {
        log.debug("Event: [" + event + "]");
        try {
            String uuid = getUuid(event.getContent());
            org.openmrs.Encounter openMrsEncounter = encounterService.getEncounterByUuid(uuid);
            if (openMrsEncounter == null) {
                log.debug(String.format("No OpenMRS encounter exists with uuid: [%s].", uuid));
                return;
            }

            if (!shouldSyncEncounter(openMrsEncounter)) {
                return;
            }


            Encounter encounter = encounterMapper.map(openMrsEncounter);
            log.debug("Encounter: [ " + encounter + "]");
            final List<Condition> conditionList = diagnosisMapper.map(openMrsEncounter, encounter);
            Composition composition = createComposition(openMrsEncounter, encounter);
            addEncounterSection(encounter, composition);
            addConditionSections(conditionList, composition);
            PersonAttribute healthIdAttribute = openMrsEncounter.getPatient().getAttribute(org.bahmni.module.shrclient.util.Constants.HEALTH_ID_ATTRIBUTE);
            if (healthIdAttribute == null) {
                return;
            }

            String healthId = healthIdAttribute.getValue();
            if (StringUtils.isBlank(healthId)) {
                return;
            }

            AtomFeed atomFeed = new AtomFeed();
            addEntriesToDocument(atomFeed, composition, encounter, conditionList);
//            fhirRestClient.post("/encounter", composition);
            fhirRestClient.post(String.format("/patients/%s/encounters", healthId), atomFeed);

        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private boolean shouldSyncEncounter(org.openmrs.Encounter openMrsEncounter) {
        User changedByUser = openMrsEncounter.getChangedBy();
        if (changedByUser == null) {
            changedByUser = openMrsEncounter.getCreator();
        }
        User shrClientSystemUser = userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
        return !shrClientSystemUser.getId().equals(changedByUser.getId());
    }

    private void addEntriesToDocument(AtomFeed atomFeed, Composition composition, Encounter encounter, List<Condition> conditionList) {
        atomFeed.setTitle("Encounter");
        atomFeed.setUpdated(composition.getDateSimple());
        atomFeed.setId(UUID.randomUUID().toString());

        AtomEntry compositionEntry = new AtomEntry();
        compositionEntry.setResource(composition);
        compositionEntry.setId(composition.getIdentifier().getValueSimple());
        compositionEntry.setTitle("Composition");
        atomFeed.addEntry(compositionEntry);

        AtomEntry encounterEntry = new AtomEntry();
        encounterEntry.setResource(encounter);
        encounterEntry.setId(encounter.getIndication().getReferenceSimple());
        encounterEntry.setTitle("Encounter");
        atomFeed.addEntry(encounterEntry);

        for (Condition condition : conditionList) {
            AtomEntry conditionEntry = new AtomEntry();
            conditionEntry.setId(condition.getIdentifier().get(0).getValueSimple());
            conditionEntry.setTitle("diagnosis");
            conditionEntry.setResource(condition);
            atomFeed.addEntry(conditionEntry);
        }
    }

    private void addConditionSections(List<Condition> conditionList, Composition composition) {
        for (Condition condition : conditionList) {
            List<Identifier> identifiers = condition.getIdentifier();
            String conditionUuid = identifiers.get(0).getValueSimple();
            ResourceReference conditionRef = new ResourceReference();
            conditionRef.setReferenceSimple(conditionUuid);
            conditionRef.setDisplaySimple("diagnosis");
            composition.addSection().setContent(conditionRef);
        }
    }

    private void addEncounterSection(Encounter encounter, Composition composition) {
        final Composition.SectionComponent encounterSection = composition.addSection();
        encounterSection.setContent(encounter.getIndication());
    }

    private Composition createComposition(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        DateAndTime encounterDateTime = new DateAndTime(openMrsEncounter.getEncounterDatetime());
        Composition composition = new Composition().setDateSimple(encounterDateTime);
        composition.setEncounter(encounter.getIndication());
        composition.setStatus(new Enumeration<Composition.CompositionStatus>(Composition.CompositionStatus.final_));
        composition.setIdentifier(new Identifier().setValueSimple("Encounter - " + openMrsEncounter.getUuid()));
        return composition;
    }

    String getUuid(String content) {
        String patientUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/encounter\\/(.*)\\?v=.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            patientUuid = m.group(1);
        }
        return patientUuid;
    }

    @Override
    public void cleanUp(Event event) {
    }
}
