package org.openmrs.module.shrclient.identity;

import org.openmrs.module.shrclient.util.Headers;
import org.openmrs.module.shrclient.util.IdentityProviderClient;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.openmrs.module.shrclient.util.PropertiesReader.URL_SEPARATOR_FOR_CONTEXT_PATH;

public class IdentityProviderService {
    private static final String EMAIL_KEY = "email";
    private static final String PASSWORD_KEY = "password";
    private static final String SIGNIN_PATH_KEY = "idP.signinPath";
    private static final String FACILITY_EMAIL_KEY = "facility.email";
    private static final String FACILITY_PASSWORD_KEY = "facility.password";
    PropertiesReader propertiesReader;
    private IdentityStore identityStore;

    public IdentityProviderService(PropertiesReader propertiesReader, IdentityStore identityStore) {
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
    }

    private IdentityProviderClient getIdentityServiceClient(Properties facilityInstanceProperties) {
        HashMap<String, String> headers = Headers.getHrmIdentityHeaders(facilityInstanceProperties);
        return new IdentityProviderClient(propertiesReader.getIdentityServerBaseUrl(), headers);
    }

    public IdentityToken getOrCreateToken() throws IdentityUnauthorizedException {
        IdentityToken identityToken = identityStore.getToken();
        if (identityToken == null) {
            Properties facilityInstanceProperties = propertiesReader.getFacilityInstanceProperties();
            Properties identityProperties = propertiesReader.getIdentityProperties();
            Map<String, String> clientCredentials = getClientCredentials(facilityInstanceProperties);
            String url = URL_SEPARATOR_FOR_CONTEXT_PATH + identityProperties.getProperty(SIGNIN_PATH_KEY);
            identityToken = getIdentityServiceClient(facilityInstanceProperties)
                    .post(url, clientCredentials, IdentityToken.class);
            identityStore.setToken(identityToken);
        }
        return identityToken;
    }

    private Map<String, String> getClientCredentials(Properties facilityInstanceProperties) {
        Map<String, String> clientCredentials = new HashMap<>();
        clientCredentials.put(EMAIL_KEY, facilityInstanceProperties.getProperty(FACILITY_EMAIL_KEY));
        clientCredentials.put(PASSWORD_KEY, facilityInstanceProperties.getProperty(FACILITY_PASSWORD_KEY));
        return clientCredentials;
    }
}
