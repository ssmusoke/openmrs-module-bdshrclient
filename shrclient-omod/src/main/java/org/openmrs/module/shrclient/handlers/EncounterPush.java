package org.openmrs.module.shrclient.handlers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomFeed;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.exceptions.AtomFeedClientException;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAttribute;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;
import org.openmrs.module.fhir.mapper.bundler.CompositionBundleCreator;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mci.api.model.EncounterResponse;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SHRClient;
import org.openmrs.module.shrclient.util.PropertiesReader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EncounterPush implements EventWorker {

    private static final Logger log = Logger.getLogger(EncounterPush.class);

    private CompositionBundleCreator bundleCreator;
    private IdMappingsRepository idMappingsRepository;

    private EncounterService encounterService;
    private PropertiesReader propertiesReader;
    private SHRClient shrClient;
    private UserService userService;

    public EncounterPush(EncounterService encounterService, UserService userService, PropertiesReader propertiesReader,
                         CompositionBundleCreator bundleCreator, IdMappingsRepository idMappingsRepository,
                         ClientRegistry clientRegistry) {
        this.encounterService = encounterService;
        this.propertiesReader = propertiesReader;
        this.shrClient = clientRegistry.getSHRClient();
        this.userService = userService;
        this.bundleCreator = bundleCreator;
        this.idMappingsRepository = idMappingsRepository;
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
            PersonAttribute healthIdAttribute = emrPatient.getAttribute(org.openmrs.module.fhir.utils.Constants.HEALTH_ID_ATTRIBUTE);
            if ((healthIdAttribute == null) || (StringUtils.isBlank(healthIdAttribute.getValue()))) {
                throw new AtomFeedClientException(String.format("Patient [%s] is not yet synced to MCI.", emrPatient.getUuid()));
            }

            String healthId = healthIdAttribute.getValue();

            log.debug("Uploading patient encounter to SHR : [ " + openMrsEncounter.getUuid() + "]");
            AtomFeed atomFeed = bundleCreator.compose(openMrsEncounter);
            String shrEncounterUuid = shrClient.post(String.format("/patients/%s/encounters", healthId), atomFeed);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            EncounterResponse encounterResponse = objectMapper.readValue(shrEncounterUuid, EncounterResponse.class);
            //TODO : set the right url
            String externalUuid = encounterResponse.getEncounterId();

            String url = propertiesReader.getShrBaseUrl() +
                    "/patients/" + healthId + "/encounters/" + externalUuid;
            idMappingsRepository.saveMapping(new IdMapping(openMrsEncounter.getUuid(), externalUuid, Constants.ID_MAPPING_ENCOUNTER_TYPE, url));
        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private boolean shouldSyncEncounter(org.openmrs.Encounter openMrsEncounter) {
        if (idMappingsRepository.findByInternalId(openMrsEncounter.getUuid()) == null) {
            User changedByUser = openMrsEncounter.getChangedBy();
            if (changedByUser == null) {
                changedByUser = openMrsEncounter.getCreator();
            }
            User shrClientSystemUser = userService.getUserByUsername(org.openmrs.module.fhir.utils.Constants.SHR_CLIENT_SYSTEM_NAME);
            return !shrClientSystemUser.getId().equals(changedByUser.getId());
        }
        return false;
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
