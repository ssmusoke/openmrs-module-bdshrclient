package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.module.shrclient.handlers.LocationRegistry;
import org.openmrs.module.shrclient.handlers.ServiceClientRegistry;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.scheduler.tasks.AbstractTask;

public class LRSyncTask extends AbstractTask {

    @Override
    public void execute() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        RestClient lrClient = new ServiceClientRegistry(propertiesReader).getLRClient();
        new LocationRegistry(propertiesReader, lrClient).synchronize();
    }
}
