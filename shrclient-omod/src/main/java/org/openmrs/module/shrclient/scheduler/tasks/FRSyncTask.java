package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.module.shrclient.handlers.FRToBahmniUpdater;
import org.openmrs.scheduler.tasks.AbstractTask;

public class FRSyncTask extends AbstractTask {
    @Override
    public void execute() {
        new FRToBahmniUpdater().update();
    }
}
