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
public class OMRSHelperIT extends BaseModuleWebContextSensitiveTest {

    private static final String CONCEPT_URI = "http://www.bdshr-tr.com/concepts/";
    private static final String REF_TERM_URI = "http://www.bdshr-tr.com/refterms/";
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSHelper omrsHelper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/omrsHelperTestDS.xml");
    }

    @Test
    public void shouldFindConceptFromCoding_ThatHasConcept() {
        List<Coding> codings = asList(buildCoding(REF_TERM_URI, "1101", "A001", "some concept"),
                buildCoding(REF_TERM_URI, "1102", "B001", "some ref term 2"),
                buildCoding(CONCEPT_URI, "101", "101", "Fever"));
        Concept concept = omrsHelper.findConcept(codings);
        assertNotNull(concept);
        assertEquals(conceptService.getConcept(398).getUuid(), concept.getUuid());
    }

    @Test
    public void shouldFindConceptFromCoding_ThatHasReferenceTermsWithMatchingConceptPreferredName() {
        List<Coding> codings = asList(buildCoding(REF_TERM_URI, "1101", "A001", "xyz concept"),
                buildCoding(REF_TERM_URI, "1102", "B001", "Fever"));
        Concept concept = omrsHelper.findConcept(codings);
        assertNotNull(concept);
        assertEquals(conceptService.getConcept(398).getUuid(), concept.getUuid());
    }

    @Test
    public void shouldFindConceptFromCoding_ThatHasReferenceTermsWithoutAnyMatchingConceptPreferredName() {
        List<Coding> codings = asList(buildCoding(REF_TERM_URI, "1101", "A001", "xyz concept"),
                buildCoding(REF_TERM_URI, "1102", "B001", "pqr concept"));
        Concept concept = omrsHelper.findConcept(codings);
        assertNotNull(concept);
        assertTrue(concept.getName().getName().equals("xyz concept") || concept.getName().getName().equals("pqr concept"));
    }

    @Test
    public void shouldMapConceptGivenCodeOfFullySpecifiedName() {
        assertEquals(conceptService.getConcept(401), omrsHelper.findConceptFromValueSetCode("http://tr.com/Value-Set-Concept", "Value Set Answer 1"));
    }

    @Test
    public void shouldMapConceptGivenCodeOfShortName() {
        assertEquals(conceptService.getConcept(401), omrsHelper.findConceptFromValueSetCode("http://tr.com/Value-Set-Concept", "VSA-1"));
    }

    @Test
    public void shouldMapConceptGivenCodeOfReferenceTerm(){
        assertEquals(conceptService.getConcept(401), omrsHelper.findConceptFromValueSetCode("http://tr.com/Value-Set-Concept", "VSA-ref"));
    }

    @Test
    public void shouldMatchBasedOnShortName() {
        Concept conceptWithShortName = conceptService.getConcept(401);
        assertTrue(omrsHelper.shortNameFound(conceptWithShortName, "VSA-1"));
        assertFalse(omrsHelper.shortNameFound(conceptWithShortName, "irrelevant short name"));
    }

    @Test
    public void shouldMatchBasedOnReferenceTerm(){
        Concept conceptWithReferenceTerm = conceptService.getConcept(401);
        assertTrue(omrsHelper.referenceTermCodeFound(conceptWithReferenceTerm, "VSA-ref"));
        assertFalse(omrsHelper.referenceTermCodeFound(conceptWithReferenceTerm, "invalid ref term"));

    }

    private Coding buildCoding(String uri, String externalId, String code, String display) {
        final Coding coding = new Coding();
        coding.setSystemSimple(uri + externalId);
        coding.setCodeSimple(code);
        coding.setDisplaySimple(display);
        return coding;
    }

}