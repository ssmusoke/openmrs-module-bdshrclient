package org.openmrs.module.shrclient.identity;

import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;

import java.util.HashMap;
import java.util.Properties;

public class IdentityServiceClient {
    PropertiesReader propertiesReader;
    private IdentityStore identityStore;

    public IdentityServiceClient(PropertiesReader propertiesReader, IdentityStore identityStore) {
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
    }

    public IdentityToken getOrCreateToken() throws IdentityUnauthorizedException {
        IdentityToken identityToken = identityStore.getToken();
        if(identityToken == null) {
            identityToken = getIdentityServiceClient().post("/login", propertiesReader.getIdentity(),
                    IdentityToken.class);
            identityStore.setToken(identityToken);
        }
        return identityToken;
    }


    private RestClient getIdentityServiceClient() {
        HashMap<String, String> headers = new HashMap<>();
        return new RestClient(propertiesReader.getIdentityServerBaseUrl(), headers);
    }
}
