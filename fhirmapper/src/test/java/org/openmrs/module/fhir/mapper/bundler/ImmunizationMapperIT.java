package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
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
    public void shouldMapSubjectToImmunizationAndSetIdentifier() throws Exception {
        Encounter fhirEncounter = new Encounter();
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("Hid");
        fhirEncounter.setSubject(subject);

        Immunization immunization = mapImmunization(11, fhirEncounter);

        assertEquals(subject, immunization.getSubject());
        assertTrue(CollectionUtils.isNotEmpty(immunization.getIdentifier()));
    }


    @Test
    public void shouldMapRefusedIndicator() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        assertTrue(immunization.getRefusedIndicator().getValue());
    }

    @Test
    public void shouldMapVaccine() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());

        Coding vaccineTypeCoding = immunization.getVaccineType().getCoding().get(0);
        assertEquals("Paracetamol 500", vaccineTypeCoding.getDisplaySimple());
        assertEquals("ABC", vaccineTypeCoding.getCodeSimple());
        assertEquals("http://tr.com/ABC", vaccineTypeCoding.getSystemSimple());
    }


    @Test
    public void shouldMapVaccinationDate() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        DateTime vaccinationDate = immunization.getDate();
        assertEquals("2014-01-02T00:00:00+05:30", vaccinationDate.getValue().toString());
    }

    @Test
    public void shouldMapReported() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        assertTrue(immunization.getReported().getValue());
    }

    @Test
    public void shouldSetTheRequesterOfTheImmunization() throws Exception {
        Encounter fhirEncounter = new Encounter();
        Encounter.EncounterParticipantComponent requester = fhirEncounter.addParticipant();
        ResourceReference doctor = new ResourceReference().setReferenceSimple("Life Saver");
        requester.setIndividual(doctor);

        Immunization immunization = mapImmunization(11, fhirEncounter);

        assertEquals(doctor, immunization.getRequester());

    }

    @Test
    public void shouldSetDosageUnits() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        assertTrue(immunization.getDoseQuantity().getValue().getValue().doubleValue() == 10);
        assertEquals("mg", immunization.getDoseQuantity().getCodeSimple());
        assertEquals("http://tr/openmrs/ws/rest/v1/tr/vs/sample-units", immunization.getDoseQuantity().getSystemSimple());
    }

    @Test
    public void shouldMapImmunizationReason() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        CodeableConcept immunizationReason = immunization.getExplanation().getReason().get(0);

        assertEquals("http://tr/openmrs/ws/rest/v1/tr/vs/sample-reason",immunizationReason.getCoding().get(0).getSystemSimple());
        assertEquals("Travel vaccinations",immunizationReason.getCoding().get(0).getCodeSimple());
        assertEquals("Travel vaccinations",immunizationReason.getCoding().get(0).getDisplaySimple());
    }

    @Test
    public void shouldMapImmunizationRefusalReason() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        CodeableConcept immunizationRefusalReason = immunization.getExplanation().getRefusalReason().get(0);

        assertEquals("http://tr/openmrs/ws/rest/v1/tr/vs/refusal-reason", immunizationRefusalReason.getCoding().get(0).getSystemSimple());
        assertEquals("patient objection", immunizationRefusalReason.getCoding().get(0).getCodeSimple());
        assertEquals("patient objection",immunizationRefusalReason.getCoding().get(0).getDisplaySimple());
    }

    @Test
    public void shouldMapRouteOfAdministration() throws Exception {
        Immunization immunization = mapImmunization(11, new Encounter());
        CodeableConcept route = immunization.getRoute();

        assertEquals("http://tr/openmrs/ws/rest/v1/tr/vs/sample-route", route.getCoding().get(0).getSystemSimple());
        assertEquals("Oral", route.getCoding().get(0).getCodeSimple());
        assertEquals("Oral",route.getCoding().get(0).getDisplaySimple());
    }

    private Immunization mapImmunization(int observationId, Encounter fhirEncounter) {
        Obs obs = obsService.getObs(observationId);
        List<FHIRResource> fhirResources = mapper.map(obs, fhirEncounter, getSystemProperties("1"));

        assertEquals(1, fhirResources.size());
        assertTrue(fhirResources.get(0).getResource() instanceof Immunization);

        return (Immunization) fhirResources.get(0).getResource();
    }
}