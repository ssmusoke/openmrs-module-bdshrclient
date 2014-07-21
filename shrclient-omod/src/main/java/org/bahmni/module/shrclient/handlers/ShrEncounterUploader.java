package org.bahmni.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.mapper.ChiefComplaintMapper;
import org.bahmni.module.shrclient.mapper.DiagnosisMapper;
import org.bahmni.module.shrclient.mapper.EncounterMapper;
import org.bahmni.module.shrclient.util.Constants;
import org.bahmni.module.shrclient.util.FhirRestClient;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Encounter;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrEncounterUploader implements EventWorker {

    private static final Logger log = Logger.getLogger(ShrEncounterUploader.class);

    private EncounterService encounterService;
    private EncounterMapper encounterMapper;
    private DiagnosisMapper diagnosisMapper;
    private FhirRestClient fhirRestClient;
    private UserService userService;
    private ChiefComplaintMapper chiefComplaintMapper;


    public ShrEncounterUploader(EncounterService encounterService, EncounterMapper encounterMapper, DiagnosisMapper diagnosisMapper, ChiefComplaintMapper chiefComplaintMapper, UserService userService, FhirRestClient fhirRestClient) {
        this.encounterService = encounterService;
        this.encounterMapper = encounterMapper;
        this.diagnosisMapper = diagnosisMapper;
        this.chiefComplaintMapper = chiefComplaintMapper;
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
            org.openmrs.Patient emrPatient = openMrsEncounter.getPatient();
            PersonAttribute healthIdAttribute = emrPatient.getAttribute(org.bahmni.module.shrclient.util.Constants.HEALTH_ID_ATTRIBUTE);
            if ((healthIdAttribute == null) || (StringUtils.isBlank(healthIdAttribute.getValue()))) {
                throw new AtomFeedClientException(String.format("Patient [%s] is not yet synced to MCI.", emrPatient.getUuid()));
            }

            String healthId = healthIdAttribute.getValue();
            Encounter encounter = encounterMapper.map(openMrsEncounter);
            log.debug("Uploading patient encounter to SHR : [ " + encounter + "]");
            final List<Condition> diagConditionList = diagnosisMapper.map(openMrsEncounter, encounter);
            final List<Condition> chiefComplaintConditionList = chiefComplaintMapper.map(openMrsEncounter, encounter);
            Composition composition = createComposition(openMrsEncounter, encounter);
            addEncounterSection(encounter, composition);
            addConditionSections(diagConditionList, composition, "Diagnosis");
            addConditionSections(chiefComplaintConditionList, composition, "Complaint");

            AtomFeed atomFeed = new AtomFeed();
            addEntriesToDocument(atomFeed, composition, encounter, diagConditionList, chiefComplaintConditionList);
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

    private void addEntriesToDocument(AtomFeed atomFeed, Composition composition, Encounter encounter, List<Condition> diagConditionList, List<Condition> chiefComplaintConditionList) {
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

        for (Condition condition : diagConditionList) {
            createNewAtomEntry(atomFeed, condition, "Diagnosis");
        }

        for (Condition condition : chiefComplaintConditionList) {
            createNewAtomEntry(atomFeed, condition, "Complaint");
        }
    }

    private void createNewAtomEntry(AtomFeed atomFeed, Condition condition, String title) {
        AtomEntry conditionEntry = new AtomEntry();
        conditionEntry.setId(condition.getIdentifier().get(0).getValueSimple());
        conditionEntry.setTitle(title);
        conditionEntry.setResource(condition);
        atomFeed.addEntry(conditionEntry);
    }

    private void addConditionSections(List<Condition> conditionList, Composition composition, String display) {
        for (Condition condition : conditionList) {
            List<Identifier> identifiers = condition.getIdentifier();
            String conditionUuid = identifiers.get(0).getValueSimple();
            ResourceReference conditionRef = new ResourceReference();
            conditionRef.setReferenceSimple(conditionUuid);
            conditionRef.setDisplaySimple(display);
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
        String encounterUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/encounter\\/(.*)\\?v=.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            encounterUuid = m.group(1);
        }
        return encounterUuid;
    }

    @Override
    public void cleanUp(Event event) {
    }
}
