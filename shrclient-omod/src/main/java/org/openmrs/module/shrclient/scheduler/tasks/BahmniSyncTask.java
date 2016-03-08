package org.openmrs.module.shrclient.scheduler.tasks;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.handlers.EncounterPush;
import org.openmrs.module.shrclient.handlers.PatientPush;
import org.openmrs.module.shrclient.util.PropertiesReader;

import java.net.URISyntaxException;

public class BahmniSyncTask extends AbstractBahmniSyncTask {
    private static final Logger log = Logger.getLogger(BahmniSyncTask.class);

    @Override
    protected void executeBahmniTask(PatientPush patientPush, EncounterPush encounterPush, PropertiesReader propertiesReader) {
        log.debug("SCHEDULED JOB : SHR Patient Sync Task");
        try {
            getFeedClient(OPENMRS_PATIENT_FEED_URI, patientPush, propertiesReader.getMciMaxFailedEvent()).processEvents();
            getFeedClient(OPENMRS_ENCOUNTER_FEED_URI, encounterPush, propertiesReader.getShrMaxFailedEvent()).processEvents();
        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
    }
}
