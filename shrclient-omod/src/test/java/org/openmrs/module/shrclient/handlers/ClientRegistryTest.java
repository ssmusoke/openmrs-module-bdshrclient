package org.openmrs.module.shrclient.handlers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.shrclient.identity.Identity;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.identity.IdentityToken;
import org.openmrs.module.shrclient.util.Headers;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.SHRClient;

import java.util.Properties;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ClientRegistryTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Mock
    private PropertiesReader propertiesReader;

    @Mock
    private IdentityStore identityStore;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testCreateMCIClient() throws Exception {
        Properties mciProperties = getSecureServerProperties("mci.user", "mci.password");
        when(propertiesReader.getMciProperties()).thenReturn(mciProperties);
        when(propertiesReader.getMciBaseUrl()).thenReturn("http://localhost:8089");
        UUID token = UUID.randomUUID();
        when(identityStore.getToken()).thenReturn(new IdentityToken(token.toString()));
        stubFor(get(urlEqualTo("http://localhost:8089/mci"))
                .withHeader(Headers.AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(Headers.AUTH_HEADER_KEY, equalTo(getAuthHeader().get(Headers.AUTH_HEADER_KEY)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, identityStore);

        RestClient mciClient = clientRegistry.getMCIClient();
        assertNotNull(mciClient);
        mciClient.get("/mci", String.class);
        verify(1, getRequestedFor(urlEqualTo("/mci"))
                .withHeader(Headers.AUTH_HEADER_KEY, matching(getAuthHeader().get(Headers.AUTH_HEADER_KEY)))
                .withHeader(Headers.AUTH_TOKEN_KEY, matching(token.toString())));
    }

    @Test
    public void testCreateMCIClientWhenIdentityTokenAbsent() throws Exception {
        Properties mciProperties = getSecureServerProperties("mci.user", "mci.password");
        when(propertiesReader.getMciProperties()).thenReturn(mciProperties);
        when(propertiesReader.getMciBaseUrl()).thenReturn("http://localhost:8089");
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");

        when(propertiesReader.getIdentity()).thenReturn(new Identity("foo", "bar"));

        //here the token is set to null, so ClientRegistry should get a new token
        when(identityStore.getToken()).thenReturn(null);

        UUID token = UUID.randomUUID();
        String response = "{\"token\" : \"" + token.toString() + "\"}";

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Headers.AUTH_TOKEN_KEY, token.toString())
                        .withBody(response)));



        stubFor(get(urlEqualTo("http://localhost:8089/mci"))
                .withHeader(Headers.AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(Headers.AUTH_HEADER_KEY, equalTo(getAuthHeader().get(Headers.AUTH_HEADER_KEY)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, identityStore);

        RestClient mciClient = clientRegistry.getMCIClient();
        assertNotNull(mciClient);
        mciClient.get("/mci", String.class);
        verify(1, getRequestedFor(urlEqualTo("/mci"))
                .withHeader(Headers.AUTH_HEADER_KEY, matching(getAuthHeader().get(Headers.AUTH_HEADER_KEY)))
                .withHeader(Headers.AUTH_TOKEN_KEY, matching(token.toString())));
    }

    @Test
    public void testCreateSHRClient() throws Exception {
        Properties shrProperties = getSecureServerProperties("shr.user", "shr.password");
        when(propertiesReader.getShrProperties()).thenReturn(shrProperties);
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://localhost:8089");

        UUID token = UUID.randomUUID();
        when(identityStore.getToken()).thenReturn(new IdentityToken(token.toString()));

        stubFor(get(urlEqualTo("/patients/hid01/encounters"))
                .withHeader(Headers.AUTH_TOKEN_KEY, equalTo(token.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                                "    <title>Patient Encounters</title>\n" +
                                "    <link rel=\"self\" type=\"application/atom+xml\" href=\"http://192.168.33" +
                                ".10:8081/patients/5926602583484399617/encounters\" />\n" +
                                "    <link rel=\"via\" type=\"application/atom+xml\" href=\"http://192.168.33" +
                                ".10:8081/patients/5926602583484399617/encounters\" />\n" +
                                "    <author>\n" +
                                "        <name>FreeSHR</name>\n" +
                                "    </author>\n" +
                                "    <id>2885d2c2-b534-4544-8958-0cef4b8fd1db</id>\n" +
                                "    <generator uri=\"https://github.com/ICT4H/atomfeed\">Atomfeed</generator>\n" +
                                "    <updated>2014-10-27T12:08:57Z</updated>\n" +
                                "</feed>")));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, identityStore);

        SHRClient shrClient = clientRegistry.getSHRClient();
        assertNotNull(shrClient);
        shrClient.getEncounters("/patients/hid01/encounters");
        verify(1, getRequestedFor(urlEqualTo("/patients/hid01/encounters"))
                .withHeader(Headers.AUTH_TOKEN_KEY, matching(token.toString())));
    }

    @Test
    public void testCreateSHRClientWhenIdentityTokenAbsent() throws Exception {
        Properties shrProperties = getSecureServerProperties("shr.user", "shr.password");
        when(propertiesReader.getShrProperties()).thenReturn(shrProperties);
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://localhost:8089");
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");

        when(propertiesReader.getIdentity()).thenReturn(new Identity("foo", "bar"));

        //here the token is set to null, so ClientRegistry should get a new token
        when(identityStore.getToken()).thenReturn(null);

        UUID token = UUID.randomUUID();
        String authRequest = "{\"user\" : \"foo\",\"password\" : \"bar\"}";
        String response = "{\"token\" : \"" + token.toString() + "\"}";

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Headers.AUTH_TOKEN_KEY, token.toString())
                        .withBody(response)));



        stubFor(get(urlEqualTo("/patients/hid01/encounters"))
                .withHeader(Headers.AUTH_TOKEN_KEY, equalTo(token.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                                "    <title>Patient Encounters</title>\n" +
                                "    <link rel=\"self\" type=\"application/atom+xml\" href=\"http://192.168.33" +
                                ".10:8081/patients/5926602583484399617/encounters\" />\n" +
                                "    <link rel=\"via\" type=\"application/atom+xml\" href=\"http://192.168.33" +
                                ".10:8081/patients/5926602583484399617/encounters\" />\n" +
                                "    <author>\n" +
                                "        <name>FreeSHR</name>\n" +
                                "    </author>\n" +
                                "    <id>2885d2c2-b534-4544-8958-0cef4b8fd1db</id>\n" +
                                "    <generator uri=\"https://github.com/ICT4H/atomfeed\">Atomfeed</generator>\n" +
                                "    <updated>2014-10-27T12:08:57Z</updated>\n" +
                                "</feed>")));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, identityStore);

        SHRClient shrClient = clientRegistry.getSHRClient();
        assertNotNull(shrClient);
        shrClient.getEncounters("/patients/hid01/encounters");
        verify(1, getRequestedFor(urlEqualTo("/patients/hid01/encounters"))
                .withHeader(Headers.AUTH_TOKEN_KEY, matching(token.toString())));
    }

    @Test
    public void testCreateFRClient() throws Exception {
        Properties frProperties = new Properties();
        when(propertiesReader.getFrProperties()).thenReturn(frProperties);
        when(propertiesReader.getFrBaseUrl()).thenReturn("http://localhost:8089");

        String xAuthTokenKey = "X-Auth-Token";
        String xAuthToken = "foobarbazboom";

        frProperties.setProperty("fr.tokenName", xAuthTokenKey);
        frProperties.setProperty("fr.tokenValue", xAuthToken);

        stubFor(get(urlEqualTo("http://localhost:8089/fr"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, null);

        RestClient frWebClient = clientRegistry.getFRClient();
        assertNotNull(frWebClient);
        frWebClient.get("/fr", String.class);
        verify(1, getRequestedFor(urlEqualTo("/fr")).withHeader(xAuthTokenKey, matching(xAuthToken)));
    }

    @Test
    public void testCreateLRClient() throws Exception {
        Properties lrProperties = new Properties();
        String xAuthTokenKey = "X-Auth-Token";
        String xAuthToken = "foobarbazboom";

        lrProperties.setProperty("lr.tokenName", xAuthTokenKey);
        lrProperties.setProperty("lr.tokenValue", xAuthToken);

        when(propertiesReader.getLrProperties()).thenReturn(lrProperties);
        when(propertiesReader.getLrBaseUrl()).thenReturn("http://localhost:8089");

        stubFor(get(urlEqualTo("http://localhost:8089/lr"))
                .withHeader(xAuthTokenKey, equalTo(xAuthToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, null);

        RestClient lrWebClient = clientRegistry.getLRClient();
        assertNotNull(lrWebClient);
        lrWebClient.get("/lr", String.class);
        verify(1, getRequestedFor(urlEqualTo("/lr")).withHeader(xAuthTokenKey, matching(xAuthToken)));
    }

    @Test
    public void testCreateIdentityServiceClient() throws Exception {
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");

        UUID token = UUID.randomUUID();
        String authRequest = "{\"user\" : \"foo\",\"password\" : \"bar\"}";
        String response = "{\"token\" : \"" + token.toString() + "\"}";

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Headers.AUTH_TOKEN_KEY, token.toString())
                        .withBody(response)));

        ClientRegistry clientRegistry = new ClientRegistry(propertiesReader, null);

        RestClient isWebClient = clientRegistry.getIdentityServiceClient();
        assertNotNull(isWebClient);
        IdentityToken responseToken = isWebClient.post("/login", authRequest, IdentityToken.class );
        assertEquals(responseToken.toString(), token.toString());
    }

    private java.util.Map<String, String> getAuthHeader() {
        return Headers.getBasicAuthHeader("champoo", "*****");
    }

    private Properties getSecureServerProperties(String userKey, String passwordKey) {
        Properties properties = new Properties();
        properties.setProperty(userKey, "champoo");
        properties.setProperty(passwordKey, "*****");
        return properties;
    }

}