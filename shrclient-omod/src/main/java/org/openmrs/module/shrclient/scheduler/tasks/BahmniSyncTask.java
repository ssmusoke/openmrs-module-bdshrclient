package org.openmrs.module.shrclient.scheduler.tasks;


import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.handlers.EncounterPush;
import org.openmrs.module.shrclient.handlers.PatientPush;

import java.net.URISyntaxException;

public class BahmniSyncTask extends AbstractBahmniSyncTask {
    private static final Logger log = Logger.getLogger(BahmniSyncTask.class);

    @Override
    protected void executeBahmniTask(PatientPush patientPush, EncounterPush encounterPush) {
        log.debug("SCHEDULED JOB : SHR Patient Sync Task");
        try {

            getFeedClient(OPENMRS_PATIENT_FEED_URI, patientPush).processEvents();
            getFeedClient(OPENMRS_ENCOUNTER_FEED_URI, encounterPush).processEvents();

        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
    }

}
