package org.bahmni.module.shrclient.scheduler.tasks;


import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.handlers.ShrNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;

public class BahmniSyncTask extends AbstractTask {
    private static final Logger log = Logger.getLogger(BahmniSyncTask.class);

    @Override
    public void execute() {
        log.debug("SCHEDULED JOB:SHR Patient Sync Task");
        new ShrNotifier().processPatient();
        new ShrNotifier().processEncounter();
    }

}
