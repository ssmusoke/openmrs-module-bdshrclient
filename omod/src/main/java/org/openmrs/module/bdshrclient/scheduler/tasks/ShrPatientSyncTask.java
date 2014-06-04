package org.openmrs.module.bdshrclient.scheduler.tasks;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.bdshrclient.handlers.EmrPatientNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;

public class ShrPatientSyncTask extends AbstractTask {
    private static final Log log = LogFactory.getLog(ShrPatientSyncTask.class);

    @Override
    public void execute() {
        log.debug("SCHEDULED JOB:SHR Patient Sync Task");
        EmrPatientNotifier emrPatientNotifier = new EmrPatientNotifier();
        emrPatientNotifier.process();
    }

}
