package org.openmrs.module.fhir.mapper.bundler.condition;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Boolean.FALSE;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ObservationValueMapperTest extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ObservationValueMapper observationValueMapper;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapDateValues() throws Exception {
        Obs obs = new Obs();
        Concept concept = new Concept();
        concept.setDatatype(conceptService.getConceptDatatypeByName("Date"));
        obs.setConcept(concept);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date obsDate = dateFormat.parse("2014-03-12");
        obs.setValueDate(obsDate);
        IDatatype value = observationValueMapper.map(obs);
        assertTrue(value instanceof DateDt);
        java.util.Date actualDate = ((DateDt) value).getValue();
        assertEquals(obsDate, actualDate);
    }

    @Test
    public void shouldMapNumericValues() throws Exception {
        Obs obs = new Obs();
        Concept concept = new Concept();
        concept.setDatatype(conceptService.getConceptDatatypeByName("Numeric"));
        obs.setConcept(concept);
        double valueNumeric = 10.0;
        obs.setValueNumeric(valueNumeric);
        IDatatype value = observationValueMapper.map(obs);
        assertTrue(value instanceof DecimalDt);
        double mappedValue = ((DecimalDt) value).getValue().doubleValue();
        assertTrue(mappedValue == valueNumeric);
    }

    @Test
    public void shouldMapTextValues() throws Exception {
        Obs obs = new Obs();
        Concept concept = new Concept();
        concept.setDatatype(conceptService.getConceptDatatypeByName("Text"));
        obs.setConcept(concept);
        String valueText = "Hello";
        obs.setValueText(valueText);
        IDatatype value = observationValueMapper.map(obs);
        assertTrue(value instanceof StringDt);
        assertEquals(valueText, ((StringDt) value).getValue());
    }

    @Test
    public void shouldMapCodedValues() throws Exception {
        Obs obs = new Obs();
        Concept concept = new Concept();
        concept.setDatatype(conceptService.getConceptDatatypeByName("Coded"));
        obs.setConcept(concept);
        Concept codedConcept = new Concept(10);
        String conceptName = "Concept";
        codedConcept.addName(new ConceptName(conceptName, conceptService.getLocalesOfConceptNames().iterator().next()));
        obs.setValueCoded(codedConcept);
        IDatatype value = observationValueMapper.map(obs);
        assertTrue(value instanceof CodeableConceptDt);
        assertEquals(conceptName, ((CodeableConceptDt) value).getCoding().get(0).getDisplay());
    }

    @Test
    public void shouldMapBooleanValues() throws Exception {
        Obs obs = new Obs();
        Concept concept = new Concept();
        concept.setDatatype(conceptService.getConceptDatatypeByName("Boolean"));
        obs.setConcept(concept);
        obs.setValueBoolean(FALSE);
        IDatatype value = observationValueMapper.map(obs);
        assertTrue(value instanceof BooleanDt);
        assertFalse(((BooleanDt) value).getValue());
    }
}