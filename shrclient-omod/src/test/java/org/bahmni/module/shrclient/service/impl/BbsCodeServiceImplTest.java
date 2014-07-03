package org.bahmni.module.shrclient.service.impl;

import org.bahmni.module.shrclient.service.impl.BbsCodeServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.bahmni.module.shrclient.service.BbsCodeService;

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
        assertEquals("1", service.getEducationCode("Uneducated"));
        assertEquals("2", service.getEducationCode("5th Pass and Below"));
        assertEquals("3", service.getEducationCode("6th to 9th"));
        assertEquals("4", service.getEducationCode("10th pass"));
        assertEquals("5", service.getEducationCode("12th pass"));
        assertEquals("6", service.getEducationCode("Graduate and Above"));
    }

    @Test
    public void shouldGetEducationConceptNames() {
        assertEquals("Uneducated", service.getEducationConcept("1"));
        assertEquals("5th Pass and Below", service.getEducationConcept("2"));
        assertEquals("6th to 9th", service.getEducationConcept("3"));
        assertEquals("10th pass", service.getEducationConcept("4"));
        assertEquals("12th pass", service.getEducationConcept("5"));
        assertEquals("Graduate and Above", service.getEducationConcept("6"));
    }

    @Test
    public void shouldGetOccupationCodes() {
        assertEquals("1", service.getOccupationCode("Unemployed"));
        assertEquals("2", service.getOccupationCode("Agriculture"));
        assertEquals("3", service.getOccupationCode("Student"));
        assertEquals("4", service.getOccupationCode("Government"));
        assertEquals("5", service.getOccupationCode("Business"));
        assertEquals("6", service.getOccupationCode("Housewife"));
        assertEquals("7", service.getOccupationCode("Labour"));
        assertEquals("8", service.getOccupationCode("Other"));
    }

    @Test
    public void shouldGetOccupationConceptNames() {
        assertEquals("Unemployed", service.getOccupationConcept("1"));
        assertEquals("Agriculture", service.getOccupationConcept("2"));
        assertEquals("Student", service.getOccupationConcept("3"));
        assertEquals("Government", service.getOccupationConcept("4"));
        assertEquals("Business", service.getOccupationConcept("5"));
        assertEquals("Housewife", service.getOccupationConcept("6"));
        assertEquals("Labour", service.getOccupationConcept("7"));
        assertEquals("Other", service.getOccupationConcept("8"));
    }

}