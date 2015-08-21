package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class VisitLookupServiceTest {
    @Mock
    private VisitService visitService;

    private VisitLookupService visitLookupService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        visitLookupService = new VisitLookupService(visitService);
    }

    @Test
    public void shouldMapAmbulatoryVisitType() throws Exception {
        VisitType visitType = new VisitType("ambulatory", "ambulatory");
        when(visitService.getAllVisitTypes()).thenReturn(asList(visitType));
        VisitType actualVisitType = visitLookupService.getVisitType(EncounterClassEnum.AMBULATORY.getCode());
        assertEquals(visitType, actualVisitType);
    }

    @Test
    public void shouldMapOPDVisitType() throws Exception {
        VisitType visitType = new VisitType("OPD", "OPD");
        when(visitService.getAllVisitTypes()).thenReturn(asList(visitType));
        VisitType actualVisitType = visitLookupService.getVisitType(EncounterClassEnum.OUTPATIENT.getCode());
        assertEquals(visitType, actualVisitType);
    }

    @Test
    public void shouldMapIPDVisitType() throws Exception {
        VisitType visitType = new VisitType("IPD", "IPD");
        when(visitService.getAllVisitTypes()).thenReturn(asList(visitType));
        VisitType actualVisitType = visitLookupService.getVisitType(EncounterClassEnum.INPATIENT.getCode());
        assertEquals(visitType, actualVisitType);
    }

    @Test
    public void shouldReturnNullIfVisitTypeNotFound() throws Exception {
        VisitType visitType = new VisitType("IPD-1", "IPD-1");
        when(visitService.getAllVisitTypes()).thenReturn(asList(visitType));
        VisitType actualVisitType = visitLookupService.getVisitType(EncounterClassEnum.INPATIENT.getCode());
        assertNull(actualVisitType);
    }
}