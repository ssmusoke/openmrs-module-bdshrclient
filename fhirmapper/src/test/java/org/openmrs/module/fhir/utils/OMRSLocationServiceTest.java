package org.openmrs.module.fhir.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;

import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.mapper.MRSProperties.GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG;

public class OMRSLocationServiceTest {

    @Mock
    GlobalPropertyLookUpService globalPropertyLookUpService;

    @Mock
    IdMappingsRepository idMappingsRepository;

    @InjectMocks
    OMRSLocationService omrsLocationService;

    @Before
    public void setUp(){
        initMocks(this);
    }

    @Test
    public void shouldMarkALocationAsHIEFacilityIfTaggedAsHIEFacility() throws Exception {
        Location HIELocation = new Location();
        HIELocation.setTags(new HashSet<LocationTag>() {{
            LocationTag HIEFacilityTag = new LocationTag();
            HIEFacilityTag.setId(10);
            add(HIEFacilityTag);
        }});
        when(globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG)).thenReturn("10");

        assertTrue(omrsLocationService.isLocationHIEFacility(HIELocation));
    }

    @Test
    public void shouldNotMarkLocationAsHIEFacilityIfNotTaggedAsHIEFacility() throws Exception {
        Location someOtherLocation = new Location();
        when(globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG)).thenReturn("10");

        assertFalse(omrsLocationService.isLocationHIEFacility(someOtherLocation));
    }

    @Test
    public void shouldNotMarkAnInvalidLocationAsHIEFacility() throws Exception {
        assertFalse(omrsLocationService.isLocationHIEFacility(null));
    }

    @Test
    public void shouldNotMarkALocationAsHIEFacilityIfNoGlobalPropertyIsConfiguredAsHIELocationTagId() throws Exception {
        when(globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_SHR_HIE_FACILITY_LOCATION_TAG)).thenReturn(null);

        assertFalse(omrsLocationService.isLocationHIEFacility(new Location()));
    }

    @Test
    public void shouldReturnHIEIdentifierOfALocation() throws Exception {
        String uuid = "location uuid";
        when(idMappingsRepository.findByInternalId(uuid)).thenReturn(new IdMapping(uuid,"hie id","",""));

        Location hieLocation = new Location();
        hieLocation.setUuid(uuid);
        assertEquals("hie id",omrsLocationService.getLocationHIEIdentifier(hieLocation));

    }

    @Test
    public void shouldReturnNullIfALocationHasNoHIEIdentifierMapping() throws Exception {
        String uuid = "location uuid";
        when(idMappingsRepository.findByInternalId(uuid)).thenReturn(null);

        Location hieLocation = new Location();
        hieLocation.setUuid(uuid);
        assertNull(omrsLocationService.getLocationHIEIdentifier(hieLocation));

    }
}