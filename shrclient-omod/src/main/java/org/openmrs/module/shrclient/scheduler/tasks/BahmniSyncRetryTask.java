package org.openmrs.module.shrclient.scheduler.tasks;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.handlers.EncounterRegistry;
import org.openmrs.module.shrclient.handlers.PatientRegistry;

import java.net.URISyntaxException;

public class BahmniSyncRetryTask extends AbstractBahmniSyncTask {
    private static final Logger log = Logger.getLogger(BahmniSyncRetryTask.class);


    @Override
    protected void executeBahmniTask(PatientRegistry patientRegistry, EncounterRegistry encounterRegistry) {
        log.debug("SCHEDULED JOB:SHR Patient retry Sync Task");
        try {

            feedClient(OPENMRS_PATIENT_FEED_URI, patientRegistry).processFailedEvents();
            feedClient(OPENMRS_ENCOUNTER_FEED_URI, encounterRegistry).processFailedEvents();

        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
    }
}
