package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.handlers.FacilityRegistry;
import org.openmrs.module.shrclient.handlers.ServiceClientRegistry;
import org.openmrs.module.shrclient.mapper.LocationMapper;
import org.openmrs.module.shrclient.util.Database;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.DatabaseUpdater;

import java.sql.Connection;

public class FRSyncTask extends AbstractTask {
    @Override
    public void execute() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        Connection connection;
        try {
            connection = DatabaseUpdater.getConnection();
            new FacilityRegistry(propertiesReader, new ServiceClientRegistry(propertiesReader).getFRClient(),
                    Context.getService(LocationService.class), new ScheduledTaskHistory(new Database(connection)), new IdMappingsRepository(),
                    new LocationMapper()).synchronize();
            if (connection != null) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
