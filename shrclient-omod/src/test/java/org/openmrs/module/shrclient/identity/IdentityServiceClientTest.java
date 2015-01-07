package org.openmrs.module.shrclient.identity;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.shrclient.util.Headers;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class IdentityServiceClientTest {
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
    public void testGetToken() throws Exception {
        UUID token = UUID.randomUUID();
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        when(identityStore.getToken()).thenReturn(new IdentityToken(token.toString()));

        IdentityServiceClient identityServiceClient = new IdentityServiceClient(propertiesReader, identityStore);
        IdentityToken identityToken = identityServiceClient.getOrCreateToken();
        assertEquals(identityToken.toString(), token.toString());
    }

    @Test
    public void testCreateToken() throws Exception {
        UUID token = UUID.randomUUID();
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        when(identityStore.getToken()).thenReturn(null);
        when(propertiesReader.getIdentity()).thenReturn(new Identity("foo", "bar"));
        String response = "{\"token\" : \"" + token.toString() + "\"}";

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(Headers.AUTH_TOKEN_KEY, token.toString())
                        .withBody(response)));

        IdentityServiceClient identityServiceClient = new IdentityServiceClient(propertiesReader, identityStore);
        IdentityToken identityToken = identityServiceClient.getOrCreateToken();
        assertEquals(identityToken.toString(), token.toString());
    }

    @Test(expected = IdentityUnauthorizedException.class)
    public void testInvalidCredentials() throws Exception {
        when(propertiesReader.getIdentityServerBaseUrl()).thenReturn("http://localhost:8089");
        when(identityStore.getToken()).thenReturn(null);
        when(propertiesReader.getIdentity()).thenReturn(new Identity("foo", "bar"));

        stubFor(post(urlMatching("/login"))
                .withRequestBody(containing("foo"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())));

        new IdentityServiceClient(propertiesReader, identityStore).getOrCreateToken();
    }
}