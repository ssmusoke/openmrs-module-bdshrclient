package org.bahmni.module.shrclient.handlers;

import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.mapper.EncounterMapper;
import org.bahmni.module.shrclient.model.Encounter;
import org.bahmni.module.shrclient.util.WebClient;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.api.EncounterService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrEncounterCreator implements EventWorker {

    private static final Logger log = Logger.getLogger(ShrEncounterCreator.class);
    private EncounterService encounterService;
    private EncounterMapper encounterMapper;
    private WebClient webClient;

    public ShrEncounterCreator(EncounterService encounterService, EncounterMapper encounterMapper, WebClient webClient) {
        this.encounterService = encounterService;
        this.encounterMapper = encounterMapper;
        this.webClient = webClient;
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
            Encounter encounter = encounterMapper.map(openMrsEncounter);
            log.debug("Encounter: [ " + encounter + "]");

            webClient.post("/encounter", encounter);

        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    Encounter populateEncounter(org.openmrs.Encounter openMrsEncounter) {
        return new Encounter();
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
