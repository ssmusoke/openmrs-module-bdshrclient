package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Immunization;
import org.hl7.fhir.instance.model.ResourceReference;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.fhir.TestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class ImmunizationMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    ObsService obsService;

    @Autowired
    private ImmunizationMapper mapper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/immunizationDS.xml");
    }

    @Test
    public void shouldHandleImmunizationTypeObservations() throws Exception {
        Obs observation = obsService.getObs(11);
        assertTrue(mapper.canHandle(observation));
    }

    @Test
    public void shouldMapSubjectToImmunization() throws Exception {
        Obs obs = obsService.getObs(11);
        Encounter fhirEncounter = new Encounter();
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("Hid");
        fhirEncounter.setSubject(subject);
        List<FHIRResource> fhirResources = mapper.map(obs, fhirEncounter, getSystemProperties("1"));
        Immunization immunization = getImmunization(fhirResources);
        assertEquals(subject, immunization.getSubject());
    }


    @Test
    public void shouldMapRefusedIndicator() throws Exception {
        Obs obs = obsService.getObs(11);
        List<FHIRResource> fhirResources = mapper.map(obs, new Encounter(), getSystemProperties("1"));
        Immunization immunization = getImmunization(fhirResources);
        assertTrue(immunization.getRefusedIndicator().getValue());
    }

    @Test
    public void shouldMapVaccine() throws Exception {
        Obs obs = obsService.getObs(11);
        List<FHIRResource> fhirResources = mapper.map(obs, new Encounter(), getSystemProperties("1"));
        Immunization immunization = getImmunization(fhirResources);

        Coding vaccineTypeCoding = immunization.getVaccineType().getCoding().get(0);
        assertEquals("Paracetamol 500", vaccineTypeCoding.getDisplaySimple());
        assertEquals("ABC", vaccineTypeCoding.getCodeSimple());
        assertEquals("http://tr.com/ABC", vaccineTypeCoding.getSystemSimple());

    }

    private Immunization getImmunization(List<FHIRResource> fhirResources) {
        assertEquals(1, fhirResources.size());
        assertTrue(fhirResources.get(0).getResource() instanceof Immunization);
        return (Immunization) fhirResources.get(0).getResource();
    }
}