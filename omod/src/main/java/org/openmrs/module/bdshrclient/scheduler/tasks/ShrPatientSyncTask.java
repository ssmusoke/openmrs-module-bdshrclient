package org.openmrs.module.bdshrclient.scheduler.tasks;


import org.openmrs.api.context.Context;
import org.openmrs.module.bdshrclient.handlers.EmrPatientNotifier;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

public class ShrPatientSyncTask extends AbstractTask {
    @Override
    public void execute() {
        System.out.println("SCHEDULED JOB:SHR Patient Sync Task");
        EmrPatientNotifier emrPatientNotifier = new EmrPatientNotifier();
        emrPatientNotifier.process();
    }

}
