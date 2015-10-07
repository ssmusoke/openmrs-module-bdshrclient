package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
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
        Patient patient = new Patient(1000);
        encounter.setPatient(patient);
        encounter.setVisit(getVisit());
        String healthId = "1234";
        ca.uhn.fhir.model.dstu2.resource.Encounter fhirEncounter = encounterMapper.map(encounter, healthId, getSystemProperties("1"));

        ResourceReferenceDt subject = fhirEncounter.getPatient();
        assertEquals(healthId, subject.getDisplay().getValue());
        assertEquals("http://public.com/api/default/patients/" + healthId, subject.getReference().getValue());
    }

    private Visit getVisit() {
        Visit visit = new Visit(2000);
        visit.setVisitType(new VisitType("foo", "bar"));
        return visit;
    }
}