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
        assertEquals("Uneducated", service.getEducationConcept("01"));
        assertEquals("5th Pass and Below", service.getEducationConcept("02"));
        assertEquals("6th to 9th", service.getEducationConcept("03"));
        assertEquals("10th pass", service.getEducationConcept("04"));
        assertEquals("12th pass", service.getEducationConcept("05"));
        assertEquals("Graduate and Above", service.getEducationConcept("06"));
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
        assertEquals("Unemployed", service.getOccupationConcept("01"));
        assertEquals("Agriculture", service.getOccupationConcept("02"));
        assertEquals("Student", service.getOccupationConcept("03"));
        assertEquals("Government", service.getOccupationConcept("04"));
        assertEquals("Business", service.getOccupationConcept("05"));
        assertEquals("Housewife", service.getOccupationConcept("06"));
        assertEquals("Labour", service.getOccupationConcept("07"));
        assertEquals("Other", service.getOccupationConcept("08"));
    }

}