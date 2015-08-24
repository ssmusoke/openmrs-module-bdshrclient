package org.openmrs.module.shrclient.service.impl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.identity.IdentityToken;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.web.controller.MciPatientSearchRequest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.shrclient.util.Headers.*;

public class MCIPatientLookupServiceImplTest {
    @Mock
    private MciPatientService mciPatientService;
    @Mock
    private PropertiesReader propertiesReader;
    @Mock
    private IdentityStore identityStore;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private ConceptService conceptService;
    @Mock
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);
    private MCIPatientLookupServiceImpl lookupService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(propertiesReader.getMciBaseUrl()).thenReturn("http://localhost:9997");
        when(propertiesReader.getMciPatientContext()).thenReturn("/api/default/patients");

        lookupService = new MCIPatientLookupServiceImpl(mciPatientService, propertiesReader, identityStore);
        Context context = new Context();
        ServiceContext serviceContext = ServiceContext.getInstance();
        serviceContext.setService(AddressHierarchyService.class, addressHierarchyService);
        context.setServiceContext(serviceContext);
    }

    @Test
    public void shouldSearchPatientByHealthId() throws Exception {
        String xAuthToken = "xyz";
        String clientIdValue = "12345";
        String email = "email@gmail.com";
        String token = UUID.randomUUID().toString();
        MciPatientSearchRequest request = new MciPatientSearchRequest();
        String hid = "11421467785";
        request.setHid(hid);
        AddressHierarchyEntry entry = new AddressHierarchyEntry();
        entry.setName("testEntry");

        String patientContext = StringUtil.ensureSuffix(propertiesReader.getMciPatientContext(), "/");

        givenThat(get(urlEqualTo(patientContext + hid))
                .withHeader(FROM_KEY, equalTo(email))
                .withHeader(CLIENT_ID_KEY, equalTo(clientIdValue))
                .withHeader(AUTH_TOKEN_KEY, equalTo(token))
                .willReturn(aResponse().withBody(asString("patients_response/by_hid.json"))));

        when(identityStore.getToken()).thenReturn(new IdentityToken(token));

        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(anyString())).thenReturn(entry);
        when(propertiesReader.getFacilityInstanceProperties()).thenReturn(getFacilityInstanceProperties(xAuthToken, clientIdValue, email, "password"));

        Object[] patients = (Object[]) lookupService.searchPatientInRegistry(request);
        Map<String, Object> patient = (Map<String, Object>) patients[0];
        assertPatient(patient, hid, "HouseHold", "F");
    }

    @Test
    public void shouldSearchPatientsByNid() throws Exception {
        String xAuthToken = "xyz";
        String clientIdValue = "12345";
        String email = "email@gmail.com";
        String token = UUID.randomUUID().toString();
        MciPatientSearchRequest request = new MciPatientSearchRequest();
        String nid = "9000001191832";
        request.setNid(nid);
        AddressHierarchyEntry entry = new AddressHierarchyEntry();
        entry.setName("testEntry");

        String patientContext = StringUtil.removeSuffix(propertiesReader.getMciPatientContext(), "/");

        givenThat(get(urlEqualTo(patientContext + "?nid=" + nid))
                .withHeader(FROM_KEY, equalTo(email))
                .withHeader(CLIENT_ID_KEY, equalTo(clientIdValue))
                .withHeader(AUTH_TOKEN_KEY, equalTo(token))
                .willReturn(aResponse().withBody(asString("patients_response/by_nid.json"))));

        when(identityStore.getToken()).thenReturn(new IdentityToken(token));

        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(anyString())).thenReturn(entry);
        when(propertiesReader.getFacilityInstanceProperties()).thenReturn(getFacilityInstanceProperties(xAuthToken, clientIdValue, email, "password"));

        Object[] patients = (Object[]) lookupService.searchPatientInRegistry(request);
        assertEquals(3, patients.length);
        assertPatient((Map<String, Object>) patients[0], "11408769630", "brn", "F");
        assertPatient((Map<String, Object>) patients[1], "11408953847", "brn1", "F");
        assertPatient((Map<String, Object>) patients[2], "11420126616", "New", "M");

    }

    @Test
    public void shouldSearchPatientsByHouseHoldId() throws Exception {
        String xAuthToken = "xyz";
        String clientIdValue = "12345";
        String email = "email@gmail.com";
        String token = UUID.randomUUID().toString();
        MciPatientSearchRequest request = new MciPatientSearchRequest();
        String houseHoleId = "12";
        request.setHouseHoldCode(houseHoleId);
        AddressHierarchyEntry entry = new AddressHierarchyEntry();
        entry.setName("testEntry");

        when(identityStore.getToken()).thenReturn(new IdentityToken(token));

        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(anyString())).thenReturn(entry);
        when(propertiesReader.getFacilityInstanceProperties()).thenReturn(getFacilityInstanceProperties(xAuthToken, clientIdValue, email, "password"));
        String patientContext = StringUtil.removeSuffix(propertiesReader.getMciPatientContext(), "/");

        givenThat(get(urlEqualTo(patientContext + "?household_code=" + houseHoleId))
                .withHeader(FROM_KEY, equalTo(email))
                .withHeader(CLIENT_ID_KEY, equalTo(clientIdValue))
                .withHeader(AUTH_TOKEN_KEY, equalTo(token))
                .willReturn(aResponse().withBody(asString("patients_response/by_house_hold_code.json"))));


        Object[] patients = (Object[]) lookupService.searchPatientInRegistry(request);
        assertEquals(3, patients.length);
        assertPatient((Map<String, Object>) patients[0], "11408769630", "house 1", "F");
        assertPatient((Map<String, Object>) patients[1], "11408923337", "house 2", "F");
        assertPatient((Map<String, Object>) patients[2], "11408953847", "house 3", "M");

    }

    @Test
    public void shouldSearchPatientsByPhoneNumber() throws Exception {
        String xAuthToken = "xyz";
        String clientIdValue = "12345";
        String email = "email@gmail.com";
        String token = UUID.randomUUID().toString();
        MciPatientSearchRequest request = new MciPatientSearchRequest();
        String phoneNumber = "12345";
        request.setPhoneNo(phoneNumber);
        AddressHierarchyEntry entry = new AddressHierarchyEntry();
        entry.setName("testEntry");

        when(identityStore.getToken()).thenReturn(new IdentityToken(token));

        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(anyString())).thenReturn(entry);
        when(propertiesReader.getFacilityInstanceProperties()).thenReturn(getFacilityInstanceProperties(xAuthToken, clientIdValue, email, "password"));
        String patientContext = StringUtil.removeSuffix(propertiesReader.getMciPatientContext(), "/");

        givenThat(get(urlEqualTo(patientContext + "?phone_no=" + phoneNumber))
                .withHeader(FROM_KEY, equalTo(email))
                .withHeader(CLIENT_ID_KEY, equalTo(clientIdValue))
                .withHeader(AUTH_TOKEN_KEY, equalTo(token))
                .willReturn(aResponse().withBody(asString("patients_response/by_phone_number.json"))));


        Object[] patients = (Object[]) lookupService.searchPatientInRegistry(request);
        assertEquals(3, patients.length);
        assertPatient((Map<String, Object>) patients[0], "98001000317", "A89 805045", "M");
        assertPatient((Map<String, Object>) patients[1], "98001000333", "A89 47125", "F");
        assertPatient((Map<String, Object>) patients[2], "98001000341", "A89 560325", "M");

    }

    @Test
    public void shouldIgnoreInactivePatients() throws Exception {
        String xAuthToken = "xyz";
        String clientIdValue = "12345";
        String email = "email@gmail.com";
        String token = UUID.randomUUID().toString();
        MciPatientSearchRequest request = new MciPatientSearchRequest();
        String phoneNumber = "12345";
        request.setPhoneNo(phoneNumber);
        AddressHierarchyEntry entry = new AddressHierarchyEntry();
        entry.setName("testEntry");

        when(identityStore.getToken()).thenReturn(new IdentityToken(token));

        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(anyString())).thenReturn(entry);
        when(propertiesReader.getFacilityInstanceProperties()).thenReturn(getFacilityInstanceProperties(xAuthToken, clientIdValue, email, "password"));
        String patientContext = StringUtil.removeSuffix(propertiesReader.getMciPatientContext(), "/");

        givenThat(get(urlEqualTo(patientContext + "?phone_no=" + phoneNumber))
                .withHeader(FROM_KEY, equalTo(email))
                .withHeader(CLIENT_ID_KEY, equalTo(clientIdValue))
                .withHeader(AUTH_TOKEN_KEY, equalTo(token))
                .willReturn(aResponse().withBody(asString("patients_response/including_inactive.json"))));


        Object[] patients = (Object[]) lookupService.searchPatientInRegistry(request);
        assertEquals(3, patients.length);
        assertPatient((Map<String, Object>) patients[0], "98001000317", "A89 805045", "M");
        assertPatient((Map<String, Object>) patients[1], "98001000333", "A89 47125", "F");
        assertPatient((Map<String, Object>) patients[2], "98001000341", "A89 560325", "M");

    }


    private void assertPatient(Map<String, Object> patient, String hid, String firstName, String gender) {
        assertEquals(firstName, patient.get("firstName"));
        assertEquals(hid, patient.get("healthId"));
        assertEquals(gender, patient.get("gender"));
    }

    private String asString(String filePath) throws IOException {
        URL resource = URLClassLoader.getSystemResource(filePath);
        return FileUtils.readFileToString(new File(resource.getPath()));
    }

    private Properties getFacilityInstanceProperties(String xAuthToken, String clientIdValue, String email, String password) {
        Properties facilityInstanceProperties = new Properties();
        facilityInstanceProperties.setProperty("facility.apiToken", xAuthToken);
        facilityInstanceProperties.setProperty("facility.clientId", clientIdValue);
        facilityInstanceProperties.setProperty("facility.email", email);
        facilityInstanceProperties.setProperty("facility.password", password);
        return facilityInstanceProperties;
    }
}