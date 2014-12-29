package org.openmrs.module.fhir.utils;


import org.hl7.fhir.instance.model.Coding;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class OMRSConceptLookupIT extends BaseModuleWebContextSensitiveTest {

    private static final String CONCEPT_URI = "http://www.bdshr-tr.com/concepts/";
    private static final String REF_TERM_URI = "http://www.bdshr-tr.com/refterms/";
    private static final String VALUE_SET_URI = "http://www.bdshr-tr.com/tr/vs/";
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/omrsConceptLookupTestDS.xml");
    }

    @Test
    public void shouldFindConceptFromCoding_ThatHasConcept() {
        List<Coding> codings = asList(buildCoding(REF_TERM_URI, "1101", "A001", "some concept"),
                buildCoding(REF_TERM_URI, "1102", "B001", "some ref term 2"),
                buildCoding(CONCEPT_URI, "101", "101", "Fever"));
        Concept concept = omrsConceptLookup.findConcept(codings);
        assertNotNull(concept);
        assertEquals(conceptService.getConcept(398).getUuid(), concept.getUuid());
    }

    @Test
    public void shouldFindConceptFromCoding_ThatHasReferenceTermsWithMatchingConceptPreferredName() {
        List<Coding> codings = asList(buildCoding(REF_TERM_URI, "1101", "A001", "xyz concept"),
                buildCoding(REF_TERM_URI, "1102", "B001", "Fever"));
        Concept concept = omrsConceptLookup.findConcept(codings);
        assertNotNull(concept);
        assertEquals(conceptService.getConcept(398).getUuid(), concept.getUuid());
    }

    @Test
    public void shouldFindConceptFromCoding_ThatHasReferenceTermsWithoutAnyMatchingConceptPreferredName() {
        List<Coding> codings = asList(buildCoding(REF_TERM_URI, "1101", "A001", "xyz concept"),
                buildCoding(REF_TERM_URI, "1102", "B001", "pqr concept"));
        Concept concept = omrsConceptLookup.findConcept(codings);
        assertNotNull(concept);
        assertTrue(concept.getName().getName().equals("xyz concept") || concept.getName().getName().equals("pqr concept"));
    }

    @Test
    public void shouldMapConceptGivenCodeOfFullySpecifiedName() {
        assertEquals(conceptService.getConcept(401), omrsConceptLookup.findConceptFromValueSetCode("http://tr.com/Value-Set-Concept", "Value Set Answer 1"));
    }

    @Test
    public void shouldMapConceptGivenCodeOfShortName() {
        assertEquals(conceptService.getConcept(401), omrsConceptLookup.findConceptFromValueSetCode("http://tr.com/Value-Set-Concept", "VSA-1"));
    }

    @Test
    public void shouldMapConceptGivenCodeOfReferenceTerm(){
        assertEquals(conceptService.getConcept(401), omrsConceptLookup.findConceptFromValueSetCode("http://tr.com/Value-Set-Concept", "VSA-ref"));
    }

    @Test
    public void shouldMatchBasedOnShortName() {
        Concept conceptWithShortName = conceptService.getConcept(401);
        assertTrue(omrsConceptLookup.shortNameFound(conceptWithShortName, "VSA-1"));
        assertFalse(omrsConceptLookup.shortNameFound(conceptWithShortName, "irrelevant short name"));
    }

    @Test
    public void shouldMatchBasedOnReferenceTerm(){
        Concept conceptWithReferenceTerm = conceptService.getConcept(401);
        assertTrue(omrsConceptLookup.referenceTermCodeFound(conceptWithReferenceTerm, "VSA-ref"));
        assertFalse(omrsConceptLookup.referenceTermCodeFound(conceptWithReferenceTerm, "invalid ref term"));

    }

    @Test
    public void shouldMapConceptFromValueSetUrl() throws Exception {
        Concept mappedConcept = omrsConceptLookup.findConcept(asList(buildCoding(VALUE_SET_URI,
                "Value-Set-Concept",
                "Value Set Answer 1",
                "Value Set Answer 1")));
        assertEquals(conceptService.getConcept(401), mappedConcept);
    }

    private Coding buildCoding(String uri, String externalId, String code, String display) {
        final Coding coding = new Coding();
        coding.setSystemSimple(uri + externalId);
        coding.setCodeSimple(code);
        coding.setDisplaySimple(display);
        return coding;
    }

}