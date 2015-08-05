package org.openmrs.module.shrclient.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConceptCacheTest {
    @Mock
    private ConceptService conceptService;
    @Mock
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldLookupConceptFromGlobalProperty() throws Exception {
        ConceptCache conceptCache = new ConceptCache(conceptService, globalPropertyLookUpService);
        String propertyName = "property.name";
        Concept concept = new Concept(12);
        
        when(globalPropertyLookUpService.getGlobalPropertyValue(propertyName)).thenReturn(10);
        when(conceptService.getConcept(10)).thenReturn(concept);
        
        assertEquals(concept, conceptCache.getConceptFromGlobalProperty(propertyName));
    }

    @Test
    public void shouldCacheTheConceptAfterLookup() throws Exception {
        ConceptCache conceptCache = new ConceptCache(conceptService, globalPropertyLookUpService);
        String propertyName = "property.name";
        Concept concept = new Concept(12);
        
        when(globalPropertyLookUpService.getGlobalPropertyValue(propertyName)).thenReturn(10);
        when(conceptService.getConcept(10)).thenReturn(concept);

        assertEquals(concept, conceptCache.getConceptFromGlobalProperty(propertyName));
        assertEquals(concept, conceptCache.getConceptFromGlobalProperty(propertyName));

        verify(globalPropertyLookUpService, times(1)).getGlobalPropertyValue(propertyName);
        verify(conceptService, times(1)).getConcept(10);
    }
}