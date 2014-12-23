package org.openmrs.module.shrclient.scheduler.tasks;

import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.handlers.FacilityPull;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.mapper.LocationMapper;
import org.openmrs.module.shrclient.util.Database;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.DatabaseUpdater;

import java.sql.Connection;

public class FRSyncTask extends AbstractTask {
    @Override
    public void execute() {
        PropertiesReader propertiesReader;
        RestClient frClient;
        Connection connection;
        try {
            propertiesReader = PlatformUtil.getPropertiesReader();
            IdentityStore identityStore = PlatformUtil.getIdentityStore();
            frClient = new ClientRegistry(propertiesReader, identityStore).getFRClient();
            connection = DatabaseUpdater.getConnection();

            new FacilityPull(propertiesReader, frClient,
                    Context.getService(LocationService.class), new ScheduledTaskHistory(new Database(connection)), new IdMappingsRepository(),
                    new LocationMapper()).synchronize();
            
            if (connection != null) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
