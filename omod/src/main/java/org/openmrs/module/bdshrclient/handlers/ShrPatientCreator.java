package org.openmrs.module.bdshrclient.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.User;
import org.openmrs.api.PatientService;
import org.openmrs.api.UserService;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.util.GenderEnum;
import org.openmrs.module.bdshrclient.util.MciProperties;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrPatientCreator implements EventWorker {

    //TODO: need to pull out to common constant
    private static final String SHR_CLIENT_SYSTEM_NAME = "shrclientsystem";
    private static final Logger log = Logger.getLogger(ShrPatientCreator.class);
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    private static ObjectMapper jsonMapper = new ObjectMapper();
    private static HttpClient httpClient = HttpClientBuilder.create().build();

    private AddressHierarchyService addressHierarchyService;
    private PatientService patientService;
    private UserService userService;

    public ShrPatientCreator(AddressHierarchyService addressHierarchyService, PatientService patientService, UserService userService) {
        this.addressHierarchyService = addressHierarchyService;
        this.patientService = patientService;
        this.userService = userService;
    }

    @Override
    public void process(Event event) {
        log.debug("Patient sync event. Event: [" + event + "]");
        try {
            String patientUuid = getPatientUuid(event);
            org.openmrs.Patient openMrsPatient = patientService.getPatientByUuid(patientUuid);
            if (!shouldSyncPatient(openMrsPatient)) {
                log.debug("Patient %s was created from SHR. Ignoring Patient Sync");
                return;
            }

            Patient patient = populatePatient(openMrsPatient);
            log.debug("Patient sync event. Patient: [ " + patient + "]");
            int responseCode = httpPost(getMciUrl(), patient);
            log.debug("Patient sync event. Response code: " + responseCode);
        } catch (IOException e) {
            log.error("Error while processing patient sync event.", e);
        }
    }

    private String getMciUrl() throws IOException {
        MciProperties mciProperties = new MciProperties();
        mciProperties.loadProperties();
        return mciProperties.getMciPatientBaseURL();
    }

    Patient populatePatient(org.openmrs.Patient openMrsPatient) {
        Patient patient = new Patient();

        String nationalId = getAttributeValue(openMrsPatient, "National ID");
        if (nationalId != null) {
            patient.setNationalId(nationalId);
        }

        String healthId = getAttributeValue(openMrsPatient, "Health ID");
        if (healthId != null) {
            patient.setHealthId(healthId);
        }

        String occupation = getAttributeValue(openMrsPatient, "occupation");
        if (occupation != null) {
            patient.setOccupation(occupation);
        }

        String educationLevel = getAttributeValue(openMrsPatient, "education");
        if (educationLevel != null) {
            patient.setEducationLevel(educationLevel);
        }

        String primaryContact = getAttributeValue(openMrsPatient, "primaryContact");
        if (primaryContact != null) {
            patient.setPrimaryContact(primaryContact);
        }

        patient.setFirstName(openMrsPatient.getGivenName());
        patient.setMiddleName(openMrsPatient.getMiddleName());
        patient.setLastName(openMrsPatient.getFamilyName());
        patient.setGender(GenderEnum.getCode(openMrsPatient.getGender()));
        patient.setDateOfBirth(new SimpleDateFormat(ISO_DATE_FORMAT).format(openMrsPatient.getBirthdate()));


        patient.setAddress(getAddress(openMrsPatient));
        return patient;
    }

    private String getAttributeValue(org.openmrs.Patient openMrsPatient, String attributeName) {
        PersonAttribute attribute = openMrsPatient.getAttribute(attributeName);
        return attribute != null ? attribute.getValue() : null;
    }

    private boolean shouldSyncPatient(org.openmrs.Patient openMrsPatient) {
        User changedByUser = openMrsPatient.getChangedBy();
        if (changedByUser == null) {
            changedByUser = openMrsPatient.getCreator();
        }
        User shrClientSystemUser = userService.getUserByUsername(SHR_CLIENT_SYSTEM_NAME);
        return shrClientSystemUser.getId() != changedByUser.getId();
    }

    public String getPatientUuid(Event event) {
        String patientUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/patient\\/(.*)\\?v=full");
        Matcher m = p.matcher(event.getContent());
        if (m.matches()) {
            patientUuid = m.group(1);
        }
        return patientUuid;
    }

    private Address getAddress(org.openmrs.Patient openMrsPatient) {
        PersonAddress openMrsPersonAddress = openMrsPatient.getPersonAddress();
        String addressLine = openMrsPersonAddress.getAddress1();
        String division = openMrsPersonAddress.getStateProvince();
        String district = openMrsPersonAddress.getCountyDistrict();
        String upazilla = openMrsPersonAddress.getAddress3();
        String union = openMrsPersonAddress.getCityVillage();

        List<AddressHierarchyLevel> levels = addressHierarchyService.getAddressHierarchyLevels();
        String divisionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(0), division).get(0).getUserGeneratedId();
        String districtId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(1), district).get(0).getUserGeneratedId();
        String upazillaId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(2), upazilla).get(0).getUserGeneratedId();
        String unionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(3), union).get(0).getUserGeneratedId();

        return new Address(addressLine, divisionId, districtId, upazillaId, unionId);
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
