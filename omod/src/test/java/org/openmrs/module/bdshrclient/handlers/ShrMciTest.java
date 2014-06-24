package org.openmrs.module.bdshrclient.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class ShrMciTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    private ShrPatientCreator shrPatientCreator;

    @Before
    public void setup() throws IOException {
        shrPatientCreator = new ShrPatientCreator(null, null, null, null);
    }

    @Test
    public void shouldPostToMciEndpoint() throws IOException {
        final String contentTypeHeader = "Content-Type";
        final String contentTypeJson = "application/json";
        final String authHeader = "Authorization";
        final String authHeaderValue = shrPatientCreator.getAuthHeader();


        stubFor(post(urlEqualTo("/patient"))
                .withHeader(contentTypeHeader, equalTo(contentTypeJson))
                .withHeader(authHeader, equalTo(authHeaderValue))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hid-100")));

        final Patient patient = new Patient();
        patient.setFirstName("John");
        final Address address = new Address();
        address.setDivisionId("div-100");
        patient.setAddress(address);

        String hid = shrPatientCreator.httpPostToMci(patient);
        assertEquals("hid-100", hid);

        verify(1, postRequestedFor(urlMatching("/patient"))
                .withRequestBody(equalToJson(toJson(patient)))
                .withHeader(contentTypeHeader, matching(contentTypeJson))
                .withHeader(authHeader, matching(authHeaderValue)));
    }

    private String toJson(Patient patient) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(patient);
    }
}
