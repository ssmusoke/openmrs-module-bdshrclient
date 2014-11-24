package org.openmrs.module.shrclient.handlers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.shrclient.util.SHRClient;
import org.openmrs.module.shrclient.util.Headers;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;

import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServiceClientRegistryTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Mock
    private PropertiesReader propertiesReader;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testCreateMCIClient() throws Exception {
        Properties mciProperties = getSecureServerProperties("mci.user", "mci.password");
        when(propertiesReader.getMciProperties()).thenReturn(mciProperties);
        when(propertiesReader.getMciBaseUrl()).thenReturn("http://localhost:8089");

        stubFor(get(urlEqualTo("http://localhost:8089/mci"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        ServiceClientRegistry serviceClientRegistry = new ServiceClientRegistry(propertiesReader);

        RestClient mciClient = serviceClientRegistry.getMCIClient();
        assertNotNull(mciClient);
        mciClient.get("/mci", String.class);
        verify(1, getRequestedFor(urlEqualTo("/mci"))
                .withHeader(Headers.AUTH_HEADER_KEY, matching(getAuthHeader().get(Headers.AUTH_HEADER_KEY))));
    }

    @Test
    public void testCreateSHRClient() throws Exception {
        Properties shrProperties = getSecureServerProperties("shr.user", "shr.password");
        when(propertiesReader.getShrProperties()).thenReturn(shrProperties);
        when(propertiesReader.getShrBaseUrl()).thenReturn("http://localhost:8089");

        stubFor(get(urlEqualTo("/patients/hid01/encounters"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                                "    <title>Patient Encounters</title>\n" +
                                "    <link rel=\"self\" type=\"application/atom+xml\" href=\"http://192.168.33.10:8081/patients/5926602583484399617/encounters\" />\n" +
                                "    <link rel=\"via\" type=\"application/atom+xml\" href=\"http://192.168.33.10:8081/patients/5926602583484399617/encounters\" />\n" +
                                "    <author>\n" +
                                "        <name>FreeSHR</name>\n" +
                                "    </author>\n" +
                                "    <id>2885d2c2-b534-4544-8958-0cef4b8fd1db</id>\n" +
                                "    <generator uri=\"https://github.com/ICT4H/atomfeed\">Atomfeed</generator>\n" +
                                "    <updated>2014-10-27T12:08:57Z</updated>\n" +
                                "</feed>")));

        ServiceClientRegistry serviceClientRegistry = new ServiceClientRegistry(propertiesReader);

        SHRClient shrClient = serviceClientRegistry.getSHRClient();
        assertNotNull(shrClient);
        shrClient.getEncounters("/patients/hid01/encounters");
        verify(1, getRequestedFor(urlEqualTo("/patients/hid01/encounters"))
                .withHeader(Headers.AUTH_HEADER_KEY, matching(getAuthHeader().get(Headers.AUTH_HEADER_KEY))));
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

        ServiceClientRegistry serviceClientRegistry = new ServiceClientRegistry(propertiesReader);

        RestClient frWebClient = serviceClientRegistry.getFRClient();
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

        ServiceClientRegistry serviceClientRegistry = new ServiceClientRegistry(propertiesReader);

        RestClient lrWebClient = serviceClientRegistry.getLRClient();
        assertNotNull(lrWebClient);
        lrWebClient.get("/lr", String.class);
        verify(1, getRequestedFor(urlEqualTo("/lr")).withHeader(xAuthTokenKey, matching(xAuthToken)));
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