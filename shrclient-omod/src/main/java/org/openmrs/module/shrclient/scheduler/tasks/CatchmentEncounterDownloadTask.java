package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.module.shrclient.handlers.ShrDownloader;
import org.openmrs.scheduler.tasks.AbstractTask;

public class CatchmentEncounterDownloadTask extends AbstractTask {
    @Override
    public void execute() {
        new ShrDownloader().download();
        new ShrDownloader().retry();

    }
}
