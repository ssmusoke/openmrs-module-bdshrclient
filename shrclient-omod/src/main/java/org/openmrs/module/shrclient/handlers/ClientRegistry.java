package org.openmrs.module.shrclient.handlers;

import org.openmrs.module.shrclient.identity.IdentityServiceClient;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.identity.IdentityToken;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.util.Headers;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.SHRClient;

import java.util.HashMap;
import java.util.Properties;


public class ClientRegistry {
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;

    public ClientRegistry(PropertiesReader propertiesReader, IdentityStore identityStore) {
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
    }

    public RestClient getMCIClient() throws IdentityUnauthorizedException {
        IdentityToken token = getOrCreateIdentityToken();
        Properties properties = propertiesReader.getMciProperties();
        String user = properties.getProperty("mci.user");
        String password = properties.getProperty("mci.password");

        return new RestClient(propertiesReader.getMciBaseUrl(),
                Headers.getBasicAuthAndIdentityHeader(user, password, token));
    }

    public SHRClient getSHRClient() throws IdentityUnauthorizedException {
        IdentityToken token = getOrCreateIdentityToken();
        return new SHRClient(propertiesReader.getShrBaseUrl(),
                Headers.getIdentityHeader(token));
    }

    public IdentityToken getOrCreateIdentityToken() throws IdentityUnauthorizedException {
        return new IdentityServiceClient(propertiesReader, identityStore).getOrCreateToken();
    }

    public void clearIdentityToken() {
        identityStore.clearToken();
    }

    public RestClient getLRClient() {
        Properties properties = propertiesReader.getLrProperties();
        HashMap<String, String> headers = new HashMap<>();
        headers.put(properties.getProperty("lr.tokenName"), properties.getProperty("lr.tokenValue"));
        return new RestClient(propertiesReader.getLrBaseUrl(), headers);
    }

    public RestClient getFRClient() {
        Properties properties = propertiesReader.getFrProperties();
        HashMap<String, String> headers = new HashMap<>();
        headers.put(properties.getProperty("fr.tokenName"), properties.getProperty("fr.tokenValue"));
        return new RestClient(propertiesReader.getFrBaseUrl(), headers);
    }

    public RestClient getIdentityServiceClient() {
        HashMap<String, String> headers = new HashMap<>();
        return new RestClient(propertiesReader.getIdentityServerBaseUrl(), headers);
    }
}
