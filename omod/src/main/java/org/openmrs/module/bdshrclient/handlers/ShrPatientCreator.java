package org.openmrs.module.bdshrclient.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAddress;
import org.openmrs.api.PatientService;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.util.GenderEnum;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrPatientCreator implements EventWorker {
    private static final Log log = LogFactory.getLog(ShrPatientCreator.class);

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
        log.debug("Patient sync event. Event: [" + event + "]");
        try {
            Patient patient = populatePatient(event);
            log.debug("Patient sync event. Patient: [ " + patient + "]");
            int responseCode = httpPost(getMciUrl(), patient);
            log.debug("Patient sync event. Response code: " + responseCode);
        } catch (IOException e) {
            log.error("Error while processing patient sync event.", e);
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
        patient.setNationalId(openMrsPatient.getAttribute("National ID").getValue());
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
        final String json = jsonMapper.writeValueAsString(patient);
        log.debug(String.format("Patient sync event. HTTP post. \nURL: [%s] \nJSON:[%s]", url, json));
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(json);
        entity.setContentType("application/json");
        post.setEntity(entity);

        HttpResponse response = httpClient.execute(post);
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public void cleanUp(Event event) {
    }
}
