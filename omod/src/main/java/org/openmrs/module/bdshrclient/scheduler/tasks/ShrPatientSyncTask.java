package org.openmrs.module.bdshrclient.scheduler.tasks;


import org.openmrs.api.context.Context;
import org.openmrs.module.bdshrclient.handlers.EmrPatientNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

public class ShrPatientSyncTask extends AbstractTask {
    private static final Logger logger = LoggerFactory.getLogger(ShrPatientSyncTask.class);

    @Override
    public void execute() {
        logger.debug("SCHEDULED JOB:SHR Patient Sync Task");
        EmrPatientNotifier emrPatientNotifier = new EmrPatientNotifier();
        emrPatientNotifier.process();
    }

}
