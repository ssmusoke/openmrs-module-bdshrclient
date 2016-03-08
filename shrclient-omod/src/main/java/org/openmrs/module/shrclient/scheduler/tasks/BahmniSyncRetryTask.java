package org.openmrs.module.shrclient.scheduler.tasks;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.handlers.EncounterPush;
import org.openmrs.module.shrclient.handlers.PatientPush;
import org.openmrs.module.shrclient.util.PropertiesReader;

import java.net.URISyntaxException;

public class BahmniSyncRetryTask extends AbstractBahmniSyncTask {
    private static final Logger log = Logger.getLogger(BahmniSyncRetryTask.class);

    @Override
    protected void executeBahmniTask(PatientPush patientPush, EncounterPush encounterPush, PropertiesReader propertiesReader) {
        log.debug("SCHEDULED JOB : SHR Patient retry Sync Task");
        try {
            getFeedClient(OPENMRS_PATIENT_FEED_URI, patientPush, propertiesReader.getMciMaxFailedEvent()).processFailedEvents();
            getFeedClient(OPENMRS_ENCOUNTER_FEED_URI, encounterPush, propertiesReader.getShrMaxFailedEvent()).processFailedEvents();
        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
    }
}
