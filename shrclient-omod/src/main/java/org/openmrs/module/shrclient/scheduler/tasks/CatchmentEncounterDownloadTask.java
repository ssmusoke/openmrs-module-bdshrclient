package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.module.shrclient.handlers.EncounterPull;
import org.openmrs.scheduler.tasks.AbstractTask;

public class CatchmentEncounterDownloadTask extends AbstractTask {
    @Override
    public void execute() {
        new EncounterPull().download();
        new EncounterPull().retry();

    }
}
