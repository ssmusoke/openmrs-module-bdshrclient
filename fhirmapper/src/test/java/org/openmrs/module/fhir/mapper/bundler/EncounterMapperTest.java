package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.ResourceReference;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.module.fhir.utils.Constants;

import static org.junit.Assert.assertEquals;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

public class EncounterMapperTest {
    @Test
    public void shouldSetSubject() throws Exception {
        EncounterMapper encounterMapper = new EncounterMapper();
        Encounter encounter = new Encounter();
        encounter.setEncounterType(new EncounterType("foo", "bar"));
        encounter.setPatient(getPatient("1234"));
        encounter.setVisit(getVisit());

        String encounterId = encounter.getUuid();

        org.hl7.fhir.instance.model.Encounter fhirEncounter = encounterMapper.map(encounter, getSystemProperties("1"));
        ResourceReference subject = fhirEncounter.getSubject();
        assertEquals("1234", subject.getDisplaySimple());
        assertEquals("http://mci/api/v1/patients/1234", subject.getReferenceSimple());
        assertEquals("Encounter", fhirEncounter.getIndication().getDisplaySimple());
        assertEquals("urn:" + encounterId, fhirEncounter.getIndication().getReferenceSimple());

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
}