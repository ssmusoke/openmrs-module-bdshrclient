package org.openmrs.module.shrclient.scheduler.tasks;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.handlers.EncounterPull;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.scheduler.tasks.AbstractTask;

public class CatchmentEncounterDownloadTask extends AbstractTask {
    private static final Logger log = Logger.getLogger(CatchmentEncounterDownloadTask.class);

    @Override
    public void execute() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        IdentityStore identityStore = PlatformUtil.getIdentityStore();
        new EncounterPull(propertiesReader, identityStore).download();
        try {
            new EncounterPull(propertiesReader, identityStore).retry();
        } catch (IdentityUnauthorizedException e) {
            clearIdentity(e);
        }
    }

    private void clearIdentity(IdentityUnauthorizedException e) {
        log.error("Invalid credentials or expired token. Clearing existing token if any.");
        PlatformUtil.getIdentityStore().clearToken();
        e.printStackTrace();
    }
}
