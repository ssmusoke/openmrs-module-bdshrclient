package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRObservationValueMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private FHIRObservationValueMapper valueMapper;

    @Autowired
    private ConceptService conceptService;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapStringDt() throws Exception {
        String value = "No problems";
        StringDt stringDt = new StringDt(value);
        Obs obs = valueMapper.map(stringDt, new Obs());

        assertEquals(value, obs.getValueText());
    }

    @Test
    public void shouldMapQuantityDt() throws Exception {
        QuantityDt quantityDt = new QuantityDt(200.0);
        Obs obs = valueMapper.map(quantityDt, new Obs());

        assertThat(obs.getValueNumeric(), is(200.0));
    }

    @Test
    public void shouldMapDateTimeDt() throws Exception {
        Date date = new Date();
        DateTimeDt dateTimeDt = new DateTimeDt(date);
        Obs obs = valueMapper.map(dateTimeDt, new Obs());

        assertEquals(date, obs.getValueDatetime());
    }

    @Test
    public void shouldMapDateDt() throws Exception {
        Date date = new Date();
        DateDt dateDt = new DateDt(date);
        Obs obs = valueMapper.map(dateDt, new Obs());

        assertEquals(date, obs.getValueDate());
    }

    @Test
    public void shouldMapBooleanCodings() throws Exception {
        Concept concept = new Concept(10);
        concept.setDatatype(conceptService.getConceptDatatypeByName("Boolean"));
        CodeableConceptDt noBooleanCoding = new CodeableConceptDt("http://hl7.org/fhir/v2/0136", "N");

        Obs noObs = new Obs();
        noObs.setConcept(concept);
        noObs = valueMapper.map(noBooleanCoding, noObs);

        assertFalse(noObs.getValueBoolean());

        CodeableConceptDt yesBooleanCoding = new CodeableConceptDt("http://hl7.org/fhir/v2/0136", "Y");
        Obs yesObs = new Obs();
        yesObs.setConcept(concept);
        yesObs = valueMapper.map(yesBooleanCoding, yesObs);

        assertTrue(yesObs.getValueBoolean());
    }

    @Test
    public void shouldMapDrugCodings() throws Exception {
        executeDataSet("testDataSets/fhirObservationValueMapperTestDs.xml");

        CodeableConceptDt codeableConceptDt = new CodeableConceptDt(
                "http://tr.com/ws/rest/v1/tr/drugs/drugs/104", "104");

        Obs obs = valueMapper.map(codeableConceptDt, new Obs());

        assertEquals(conceptService.getDrug(301), obs.getValueDrug());
        assertEquals(conceptService.getConcept(301), obs.getValueCoded());
    }

    @Test
    public void shouldMapConceptCodings() throws Exception {
        executeDataSet("testDataSets/fhirObservationValueMapperTestDs.xml");

        CodeableConceptDt codeableConceptDt = new CodeableConceptDt(
                "http://tr.com/ws/rest/v1/tr/concepts/102", "102");

        Obs obs = valueMapper.map(codeableConceptDt, new Obs());

        assertEquals(conceptService.getConcept(301), obs.getValueCoded());
        assertNull(obs.getValueDrug());
    }
}