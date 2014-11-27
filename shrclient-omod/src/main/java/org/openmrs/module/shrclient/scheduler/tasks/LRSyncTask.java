package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.handlers.LocationPull;
import org.openmrs.module.shrclient.mapper.AddressHierarchyEntryMapper;
import org.openmrs.module.shrclient.util.Database;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.DatabaseUpdater;

import java.sql.Connection;

public class LRSyncTask extends AbstractTask {

    @Override
    public void execute() {
        PropertiesReader propertiesReader;
        RestClient lrClient;
        Connection connection;
        try {
            propertiesReader = PlatformUtil.getPropertiesReader();
            lrClient = new ClientRegistry(propertiesReader).getLRClient();
            connection = DatabaseUpdater.getConnection();

            new LocationPull(propertiesReader, lrClient, Context.getService(AddressHierarchyService.class),
                    new ScheduledTaskHistory(new Database(connection)), new AddressHierarchyEntryMapper()).synchronize();

            if (connection != null) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
