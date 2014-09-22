package org.openmrs.module.shrclient.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.shrclient.service.BbsCodeService;

import static org.junit.Assert.assertEquals;

public class BbsCodeServiceImplTest {

    private static BbsCodeService service;

    @Before
    public void setup() throws Exception {
        service = new BbsCodeServiceImpl();
    }

    @Test
    public void shouldGetGenderCodes() {
        assertEquals("1", service.getGenderCode("M"));
        assertEquals("2", service.getGenderCode("F"));
        assertEquals("3", service.getGenderCode("O"));
    }

    @Test
    public void shouldGetGenderConceptNames() {
        assertEquals("M", service.getGenderConcept("1"));
        assertEquals("F", service.getGenderConcept("2"));
        assertEquals("O", service.getGenderConcept("3"));
    }

    @Test
    public void shouldGetEducationCodes() {
        assertEquals("01", service.getEducationCode("Uneducated"));
        assertEquals("02", service.getEducationCode("5th Pass and Below"));
        assertEquals("03", service.getEducationCode("6th to 9th"));
        assertEquals("04", service.getEducationCode("10th pass"));
        assertEquals("05", service.getEducationCode("12th pass"));
        assertEquals("06", service.getEducationCode("Graduate and Above"));
    }

    @Test
    public void shouldGetEducationConceptNames() {
        assertEquals("Uneducated", service.getEducationConceptName("01"));
        assertEquals("5th Pass and Below", service.getEducationConceptName("02"));
        assertEquals("6th to 9th", service.getEducationConceptName("03"));
        assertEquals("10th pass", service.getEducationConceptName("04"));
        assertEquals("12th pass", service.getEducationConceptName("05"));
        assertEquals("Graduate and Above", service.getEducationConceptName("06"));
    }

    @Test
    public void shouldGetOccupationCodes() {
        assertEquals("01", service.getOccupationCode("Unemployed"));
        assertEquals("02", service.getOccupationCode("Agriculture"));
        assertEquals("03", service.getOccupationCode("Student"));
        assertEquals("04", service.getOccupationCode("Government"));
        assertEquals("05", service.getOccupationCode("Business"));
        assertEquals("06", service.getOccupationCode("Housewife"));
        assertEquals("07", service.getOccupationCode("Labour"));
        assertEquals("08", service.getOccupationCode("Other"));
    }

    @Test
    public void shouldGetOccupationConceptNames() {
        assertEquals("Unemployed", service.getOccupationConceptName("01"));
        assertEquals("Agriculture", service.getOccupationConceptName("02"));
        assertEquals("Student", service.getOccupationConceptName("03"));
        assertEquals("Government", service.getOccupationConceptName("04"));
        assertEquals("Business", service.getOccupationConceptName("05"));
        assertEquals("Housewife", service.getOccupationConceptName("06"));
        assertEquals("Labour", service.getOccupationConceptName("07"));
        assertEquals("Other", service.getOccupationConceptName("08"));
    }

}