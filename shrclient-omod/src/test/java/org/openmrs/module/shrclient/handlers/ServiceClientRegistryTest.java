package org.openmrs.module.shrclient.handlers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.shrclient.util.FhirRestClient;
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

        stubFor(get(urlEqualTo("http://localhost:8089/shr"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        ServiceClientRegistry serviceClientRegistry = new ServiceClientRegistry(propertiesReader);

        FhirRestClient shrClient = serviceClientRegistry.getSHRClient();
        assertNotNull(shrClient);
        shrClient.get("/shr", null);
        verify(1, getRequestedFor(urlEqualTo("/shr"))
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