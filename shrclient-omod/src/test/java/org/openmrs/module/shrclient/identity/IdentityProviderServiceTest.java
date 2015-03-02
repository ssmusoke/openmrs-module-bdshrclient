package org.openmrs.module.shrclient.identity;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.shrclient.util.Headers;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.springframework.http.HttpStatus;

import java.util.Properties;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class IdentityProviderServiceTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Mock
    private PropertiesReader propertiesReader;

    @Mock
    private IdentityStore identityStore;
    private String xAuthTokenKey;
    private String xAuthToken;
    private String clientId;
    private String clientIdValue;
    private String email;
    private String password;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        xAuthToken = "foobarbazboom";
        clientId = "client_id";
        clientIdValue = "18549";
        email = "email@thoughtworks.com";
        password = "thoughtworks";
        xAuthTokenKey = "X-Auth-Token";
    }

    @Test
    public void testGetToken() throws Exception {
        UUID token = UUID.randomUUID();
        setUpIdentityProperties();
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        when(identityStore.getToken()).thenReturn(new IdentityToken(token.toString()));

        IdentityProviderService identityProviderService = new IdentityProviderService(propertiesReader, identityStore);
        IdentityToken identityToken = identityProviderService.oldGetOrCreateToken();
        assertEquals(identityToken.toString(), token.toString());
    }

    @Test
    public void testCreateToken() throws Exception {
        UUID token = UUID.randomUUID();
        setUpIdentityProperties();

        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        when(identityStore.getToken()).thenReturn(null);
        when(propertiesReader.getIdentity()).thenReturn(new Identity("foo", "bar"));
        String response = "\"" + token.toString() + "\"";

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Headers.AUTH_TOKEN_KEY, token.toString())
                        .withBody(response)));

        IdentityProviderService identityProviderService = new IdentityProviderService(propertiesReader, identityStore);
        IdentityToken identityToken = identityProviderService.oldGetOrCreateToken();
        assertEquals(identityToken.toString(), token.toString());
    }

    @Test(expected = IdentityUnauthorizedException.class)
    public void testInvalidCredentials() throws Exception {
        setUpIdentityProperties();

        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        when(identityStore.getToken()).thenReturn(null);
        when(propertiesReader.getIdentity()).thenReturn(new Identity("foo", "bar"));

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())));

        new IdentityProviderService(propertiesReader, identityStore).oldGetOrCreateToken();
    }

    @Test
    public void shouldFetchToken() throws Exception {
        UUID token = UUID.randomUUID();
        setUpIdentityProperties();
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        String response = "{\"access_token\" : \"" + token.toString() + "\"}";
        stubFor(post(urlMatching("/signin"))
                .withHeader(xAuthTokenKey, equalTo(xAuthToken))
                .withHeader(clientId, equalTo(clientIdValue))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withBody(response)
                ));

        IdentityToken identityToken = new IdentityProviderService(propertiesReader, identityStore).getOrCreateToken();
        assertEquals(token.toString(), identityToken.toString());
    }

    private void setUpIdentityProperties() {
        Properties idpProperties = getIdpProperties(xAuthTokenKey, clientId);
        Properties facilityInstanceProperties = getFacilityInstanceProperties(xAuthToken, clientIdValue, email, password);

        when(propertiesReader.getIdentityProperties()).thenReturn(idpProperties);
        when(propertiesReader.getFacilityInstanceProperties()).thenReturn(facilityInstanceProperties);
    }

    private Properties getFacilityInstanceProperties(String xAuthToken, String clientIdValue, String email, String password) {
        Properties facilityInstanceProperties = new Properties();
        facilityInstanceProperties.setProperty("facility.apiToken", xAuthToken);
        facilityInstanceProperties.setProperty("facility.clientId", clientIdValue);
        facilityInstanceProperties.setProperty("facility.email", email);
        facilityInstanceProperties.setProperty("facility.password", password);
        return facilityInstanceProperties;
    }

    private Properties getIdpProperties(String xAuthTokenKey, String clientId) {
        Properties idpProperties = new Properties();
        idpProperties.setProperty("idP.tokenName", xAuthTokenKey);
        idpProperties.setProperty("idP.clientIdName", clientId);
        idpProperties.setProperty("idP.signinPath", "signin");
        return idpProperties;
    }
}