package org.openmrs.module.fhir.utils;


import static java.util.Arrays.asList;
import org.hl7.fhir.instance.model.Coding;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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
        executeDataSet("omrsHelperTestDS.xml");
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

    private Coding buildCoding(String uri, String externalId, String code, String display) {
        final Coding coding = new Coding();
        coding.setSystemSimple(uri + externalId);
        coding.setCodeSimple(code);
        coding.setDisplaySimple(display);
        return coding;
    }

}