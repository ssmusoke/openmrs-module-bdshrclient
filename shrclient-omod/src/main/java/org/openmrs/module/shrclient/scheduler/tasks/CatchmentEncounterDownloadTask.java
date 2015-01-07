package org.openmrs.module.shrclient.scheduler.tasks;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.handlers.EncounterPull;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.scheduler.tasks.AbstractTask;

public class CatchmentEncounterDownloadTask extends AbstractTask {
    private static final Logger log = Logger.getLogger(CatchmentEncounterDownloadTask.class);

    @Override
    public void execute() {
        ClientRegistry clientRegistry = new ClientRegistry(PlatformUtil.getPropertiesReader(),
                PlatformUtil.getIdentityStore());
        try {
            new EncounterPull(clientRegistry).download();
        } catch (IdentityUnauthorizedException e) {
            clearIdentity(e);
        }
        try {
            new EncounterPull(clientRegistry).retry();
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
