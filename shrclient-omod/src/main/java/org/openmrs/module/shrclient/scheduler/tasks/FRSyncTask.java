package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.handlers.FacilityRegistry;
import org.openmrs.module.shrclient.handlers.ServiceClientRegistry;
import org.openmrs.module.shrclient.mapper.LocationMapper;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;
import org.openmrs.scheduler.tasks.AbstractTask;

public class FRSyncTask extends AbstractTask {
    @Override
    public void execute() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        new FacilityRegistry(propertiesReader, new ServiceClientRegistry(propertiesReader).getFRClient(),
                Context.getService(LocationService.class), new ScheduledTaskHistory(), new IdMappingsRepository(),
                new LocationMapper()).synchronize();
    }
}
