package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.ResourceReference;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class EncounterMapperTest {
    @Test
    public void shouldSetSubject() throws Exception {
        EncounterMapper encounterMapper = new EncounterMapper();
        Encounter encounter = new Encounter();
        encounter.setEncounterType(new EncounterType("foo", "bar"));
        encounter.setPatient(getPatient("1234"));
        encounter.setVisit(getVisit());

        org.hl7.fhir.instance.model.Encounter fhirEncounter = encounterMapper.map(encounter, getSystemProperties("1"));
        ResourceReference subject = fhirEncounter.getSubject();
        assertEquals("1234", subject.getDisplaySimple());
        assertEquals("http://mci/patients/1234", subject.getReferenceSimple());
    }

    private Visit getVisit() {
        Visit visit = new Visit(2000);
        visit.setVisitType(new VisitType("foo", "bar"));
        return visit;
    }

    private Patient getPatient(String healthId) {
        Patient patient = new Patient(1000);
        PersonAttribute healthIdAttribute = getHealthIdAttribute(healthId);
        patient.addAttribute(healthIdAttribute);
        return patient;
    }

    private PersonAttribute getHealthIdAttribute(String healthId) {
        PersonAttribute healthIdAttribute = new PersonAttribute();
        PersonAttributeType healthAttributeType = new PersonAttributeType();
        healthAttributeType.setName(Constants.HEALTH_ID_ATTRIBUTE);
        healthIdAttribute.setAttributeType(healthAttributeType);
        healthIdAttribute.setValue(healthId);
        return healthIdAttribute;
    }

    private SystemProperties getSystemProperties(String facilityId) {
        Properties shrProperties = new Properties();
        shrProperties.setProperty(SystemProperties.FACILITY_ID, facilityId);
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", "http://mci");
        return new SystemProperties(baseUrls, shrProperties);
    }
}