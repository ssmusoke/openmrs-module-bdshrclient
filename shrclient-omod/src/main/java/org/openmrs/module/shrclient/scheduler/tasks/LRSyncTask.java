package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.module.shrclient.handlers.LRToBahmniUpdater;
import org.openmrs.module.shrclient.mapper.AddressHierarchyEntryMapper;
import org.openmrs.scheduler.tasks.AbstractTask;

public class LRSyncTask extends AbstractTask {

    @Override
    public void execute() {
        new LRToBahmniUpdater().update();
    }
}
