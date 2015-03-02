package org.openmrs.module.shrclient.identity;

import org.openmrs.module.shrclient.util.IdentityProviderClient;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class IdentityProviderService {
    PropertiesReader propertiesReader;
    private IdentityStore identityStore;

    public IdentityProviderService(PropertiesReader propertiesReader, IdentityStore identityStore) {
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
    }

    @Deprecated
    public IdentityToken oldGetOrCreateToken() throws IdentityUnauthorizedException {
        IdentityToken identityToken = identityStore.getToken();
        if (identityToken == null) {
            identityToken = oldGetIdentityServiceClient().post("/login", propertiesReader.getIdentity(),
                    IdentityToken.class);
            identityStore.setToken(identityToken);
        }
        return identityToken;
    }

    private RestClient oldGetIdentityServiceClient() {
        HashMap<String, String> headers = new HashMap<>();
        return new RestClient(propertiesReader.getIdentityServerBaseUrl(), headers);
    }

    private IdentityProviderClient getIdentityServiceClient(Properties facilityInstanceProperties, Properties identityProperties) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(identityProperties.getProperty("idP.clientIdName"), facilityInstanceProperties.getProperty("facility.clientId"));
        headers.put(identityProperties.getProperty("idP.tokenName"), facilityInstanceProperties.getProperty("facility.apiToken"));
        return new IdentityProviderClient(propertiesReader.getIdentityServerBaseUrl(), headers);
    }

    public IdentityToken getOrCreateToken() throws IdentityUnauthorizedException {
        IdentityToken identityToken = identityStore.getToken();

        Properties facilityInstanceProperties = propertiesReader.getFacilityInstanceProperties();
        Properties identityProperties = propertiesReader.getIdentityProperties();
        Map<String, String> clientCredentials = new HashMap<>();
        clientCredentials.put("email", facilityInstanceProperties.getProperty("facility.email"));
        clientCredentials.put("password", facilityInstanceProperties.getProperty("facility.password"));

        if (identityToken == null) {
            String url = "/" + identityProperties.getProperty("idP.signinPath");
            identityToken = getIdentityServiceClient(facilityInstanceProperties, identityProperties)
                    .post(url, clientCredentials, IdentityToken.class);
            identityStore.setToken(identityToken);
        }
        return identityToken;
    }
}
