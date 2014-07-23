package org.openmrs.module.shrclient.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.Patient;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class RestClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void shouldGetPatient() throws Exception {
        RestClient restClient = new RestClient("user", "password", "localhost", "8089");
        final String acceptHeader = "accept";
        final String contentTypeJson = "application/json";
        final String authHeader = "Authorization";
        final String authHeaderValue = restClient.getAuthHeader();
        final String url = "/patient/100";

        final Patient patient = new Patient();
        patient.setFirstName("John");
        final Address address = new Address();
        address.setDivisionId("div-100");
        patient.setAddress(address);

        stubFor(get(urlEqualTo(url))
                .withHeader(acceptHeader, equalTo(contentTypeJson))
                .withHeader(authHeader, equalTo(authHeaderValue))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", contentTypeJson)
                        .withBody(toJson(patient))));

        Patient response = restClient.get(url, Patient.class);
        assertEquals(patient, response);

        verify(1, getRequestedFor(urlMatching(url))
                .withHeader(acceptHeader, matching(contentTypeJson))
                .withHeader(authHeader, matching(authHeaderValue)));
    }

    @Test
    public void shouldPostPatient() throws Exception {
        RestClient restClient = new RestClient("user", "password", "localhost", "8089");
        final String contentTypeHeader = "Content-Type";
        final String contentTypeJson = "application/json";
        final String authHeader = "Authorization";
        final String authHeaderValue = restClient.getAuthHeader();
        String url = "/patient";

        stubFor(post(urlEqualTo(url))
                .withHeader(contentTypeHeader, equalTo(contentTypeJson))
                .withHeader(authHeader, equalTo(authHeaderValue))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(contentTypeHeader, contentTypeJson)
                        .withBody("hid-100")));

        final Patient patient = new Patient();
        patient.setFirstName("John");
        final Address address = new Address();
        address.setDivisionId("div-100");
        patient.setAddress(address);

        String hid = restClient.post(url, patient);
        assertEquals("hid-100", hid);

        verify(1, postRequestedFor(urlMatching(url))
                .withRequestBody(equalToJson(toJson(patient)))
                .withHeader(contentTypeHeader, matching(contentTypeJson))
                .withHeader(authHeader, matching(authHeaderValue)));
    }

    private String toJson(Patient patient) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(patient);
    }
}
