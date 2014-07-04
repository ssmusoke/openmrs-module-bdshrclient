package org.bahmni.module.shrclient.scheduler.tasks;

import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.handlers.ShrNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;

public class ShrEncounterSyncTask extends AbstractTask {
    private static final Logger log = Logger.getLogger(ShrEncounterSyncTask.class);

    @Override
    public void execute() {
        log.debug("SCHEDULED JOB:SHR Encounter Sync Task");
        new ShrNotifier().processEncounter();
    }
}
