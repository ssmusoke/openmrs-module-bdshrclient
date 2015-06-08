package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.ResourceReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.*;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.fhir.utils.OMRSLocationService;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

public class EncounterMapperTest {

    @Mock
    private OMRSLocationService omrsLocationService;

    @InjectMocks
    private EncounterMapper encounterMapper;

    @Before
    public void setUp(){
        initMocks(this);
    }

    @Test
    public void shouldSetSubject() throws Exception {
        Encounter encounter = new Encounter();
        encounter.setEncounterType(new EncounterType("foo", "bar"));
        encounter.setPatient(getPatient("1234"));
        encounter.setVisit(getVisit());

        String encounterId = encounter.getUuid();

        org.hl7.fhir.instance.model.Encounter fhirEncounter = encounterMapper.map(encounter, getSystemProperties("1"));
        ResourceReference subject = fhirEncounter.getSubject();
        assertEquals("1234", subject.getDisplaySimple());
        //The subject URL must be the public url reference.
        assertEquals("http://public.com/api/default/patients/1234", subject.getReferenceSimple());
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