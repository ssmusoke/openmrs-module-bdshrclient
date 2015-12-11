package org.openmrs.module.shrclient.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.Constants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HIEPatientServiceIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    HIEPatientService hiePatientService;

    @Test
    public void shouldMapRelationsToPatientAttributesWhenPresent() throws Exception {
        executeDataSet("testDataSets/patientUpdateDS.xml");
        org.openmrs.module.shrclient.model.Patient patient = getPatientFromJson("patients_response/patientWithRelations.json");

        hiePatientService.createOrUpdatePatient(patient);

        Patient savedPatient = patientService.getPatient(1);

        PersonAttribute fatherName = savedPatient.getAttribute(Constants.FATHER_NAME_ATTRIBUTE_TYPE);
        assertNotNull(fatherName);
        assertEquals(Constants.FATHER_NAME_ATTRIBUTE_TYPE, fatherName.getAttributeType().getName());
        assertEquals("Md. Sakib Ali Khan", fatherName.getValue());

        PersonAttribute spouseName = savedPatient.getAttribute(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE);
        assertNotNull(spouseName);
        assertEquals(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE, spouseName.getAttributeType().getName());
        assertEquals("Azad", spouseName.getValue());
    }

    @Test
    public void shouldUpdatePatientAttributesOnDownloadIfPresentAlready() throws Exception {
        executeDataSet("testDataSets/patientUpdateDS.xml");
        org.openmrs.module.shrclient.model.Patient patient = getPatientFromJson("patients_response/patientWithRelations.json");

        hiePatientService.createOrUpdatePatient(patient);
        //updated twice
        hiePatientService.createOrUpdatePatient(patient);

        Patient savedPatient = patientService.getPatient(1);

        List<PersonAttribute> fatherName = savedPatient.getAttributes(Constants.FATHER_NAME_ATTRIBUTE_TYPE);
        assertEquals(1, fatherName.size());

        List<PersonAttribute> spouseName = savedPatient.getAttributes(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE);
        assertEquals(1, spouseName.size());
    }

    private org.openmrs.module.shrclient.model.Patient getPatientFromJson(String patientJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        URL resource = URLClassLoader.getSystemResource(patientJson);
        final String patientResponse = FileUtils.readFileToString(new File(resource.getPath()));

        return mapper.readValue(patientResponse, org.openmrs.module.shrclient.model.Patient.class);
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }
}
