package org.openmrs.module.bdshrclient.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openmrs.module.bdshrclient.handlers.ShrPatientCreator;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class MciWebClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void shouldGetPatient() throws Exception {
        MciWebClient webClient = new MciWebClient();
        final String acceptHeader = "accept";
        final String contentTypeJson = "application/json";
        final String authHeader = "Authorization";
        final String authHeaderValue = webClient.getAuthHeader();
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

        Patient response = webClient.get("http://localhost:8089" + url, Patient.class);
        assertEquals(patient, response);

        verify(1, getRequestedFor(urlMatching(url))
                .withHeader(acceptHeader, matching(contentTypeJson))
                .withHeader(authHeader, matching(authHeaderValue)));
    }

    @Test
    public void shouldPostPatient() throws Exception {
        MciWebClient webClient = new MciWebClient();
        final String contentTypeHeader = "Content-Type";
        final String contentTypeJson = "application/json";
        final String authHeader = "Authorization";
        final String authHeaderValue = webClient.getAuthHeader();
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

        String hid = webClient.post("http://localhost:8089" + url, patient);
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
