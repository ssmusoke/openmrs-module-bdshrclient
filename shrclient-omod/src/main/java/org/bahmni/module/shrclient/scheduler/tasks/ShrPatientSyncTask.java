package org.bahmni.module.shrclient.scheduler.tasks;


import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.handlers.ShrNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;

public class ShrPatientSyncTask extends AbstractTask {
    private static final Logger log = Logger.getLogger(ShrPatientSyncTask.class);

    @Override
    public void execute() {
        log.debug("SCHEDULED JOB:SHR Patient Sync Task");
        ShrNotifier shrNotifier = new ShrNotifier();
        shrNotifier.processPatient();
    }

}
