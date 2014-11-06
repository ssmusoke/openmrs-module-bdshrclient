package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.module.shrclient.handlers.LocationUpdater;
import org.openmrs.module.shrclient.handlers.ShrDownloader;
import org.openmrs.scheduler.tasks.AbstractTask;

public class LRSyncTask extends AbstractTask {

    @Override
    public void execute() {
        new LocationUpdater().update();
    }
}
