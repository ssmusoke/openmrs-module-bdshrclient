package org.openmrs.module.shrclient.scheduler.tasks;

import org.apache.log4j.Logger;

import org.openmrs.module.shrclient.handlers.ShrNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;

public class BahmniSyncRetryTask extends AbstractTask {

    private static final Logger log = Logger.getLogger(BahmniSyncRetryTask.class);

    @Override
    public void execute() {
        log.debug("SCHEDULED JOB:SHR Patient retry Sync Task");
        new ShrNotifier().retryPatient();
        new ShrNotifier().retryEncounter();
    }
}
