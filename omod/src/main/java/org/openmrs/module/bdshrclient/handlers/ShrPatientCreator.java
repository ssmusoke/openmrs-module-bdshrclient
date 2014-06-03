package org.openmrs.module.bdshrclient.handlers;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.map.ObjectMapper;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAddress;
import org.openmrs.api.PatientService;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.util.GenderEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrPatientCreator implements EventWorker {
    private static final Logger logger = LoggerFactory.getLogger(ShrPatientCreator.class);

    private static ObjectMapper jsonMapper = new ObjectMapper();
    private static HttpClient httpClient = HttpClientBuilder.create().build();

    private AddressHierarchyService addressHierarchyService;
    private PatientService patientService;

    public ShrPatientCreator(AddressHierarchyService addressHierarchyService, PatientService patientService) {
        this.addressHierarchyService = addressHierarchyService;
        this.patientService = patientService;
    }

    @Override
    public void process(Event event) {
        try {
            Patient patient = populatePatient(event);
            int responseCode = httpPost(getMciUrl(), patient);
            logger.debug("Processed create patient event. Response code: " + responseCode);
        } catch (IOException e) {
            logger.error("Error while processing create patient event.", e);
        }
    }

    private String getMciUrl() throws IOException {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("freeshrclient.properties");
        Properties properties = new Properties();
        properties.load(inputStream);

        final String mciHost = properties.getProperty("mci.host");
        final String mciPort = properties.getProperty("mci.port");
        return String.format("http://%s:%s/patient", mciHost, mciPort);
    }

    Patient populatePatient(Event event) {
        String patientUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/patient\\/(.*)\\?v=full");
        Matcher m = p.matcher(event.getContent());
        if (m.matches()) {
            patientUuid = m.group(1);
        }
        org.openmrs.Patient openMrsPatient = patientService.getPatientByUuid(patientUuid);

        Patient patient = new Patient();
        patient.setFirstName(openMrsPatient.getGivenName());
        patient.setMiddleName(openMrsPatient.getMiddleName());
        patient.setLastName(openMrsPatient.getFamilyName());

        patient.setGender(GenderEnum.getCode(openMrsPatient.getGender()));

        PersonAddress openMrsPersonAddress = openMrsPatient.getPersonAddress();
        String division = openMrsPersonAddress.getStateProvince();
        String district = openMrsPersonAddress.getCountyDistrict();
        String upazilla = openMrsPersonAddress.getAddress3();
        String union = openMrsPersonAddress.getCityVillage();

        List<AddressHierarchyLevel> levels = addressHierarchyService.getAddressHierarchyLevels();
        String divisionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(0), division).get(0).getUserGeneratedId();
        String districtId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(1), district).get(0).getUserGeneratedId();
        String upazillaId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(2), upazilla).get(0).getUserGeneratedId();
        String unionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(3), union).get(0).getUserGeneratedId();

        Address address = new Address(divisionId, districtId, upazillaId, unionId);
        patient.setAddress(address);

        return patient;
    }

    public int httpPost(String url, Patient patient) throws IOException {
        //TODO: HttpAsyncClient
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(jsonMapper.writeValueAsString(patient));
        entity.setContentType("application/json");
        post.setEntity(entity);

        HttpResponse response = httpClient.execute(post);
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public void cleanUp(Event event) {
    }
}
