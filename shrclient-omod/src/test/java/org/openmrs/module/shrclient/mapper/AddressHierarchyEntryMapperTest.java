package org.openmrs.module.shrclient.mapper;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.mci.api.model.LRAddressHierarchyEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AddressHierarchyEntryMapperTest {

    public static final String NON_EXISTING_UGID = "9999";
    @Mock
    private AddressHierarchyService addressHierarchyService;

    LRAddressHierarchyEntry lrAddressHierarchyEntry;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        lrAddressHierarchyEntry = new LRAddressHierarchyEntry();
        lrAddressHierarchyEntry.setShortLocationCode("10");
        lrAddressHierarchyEntry.setFullLocationCode("10");
        lrAddressHierarchyEntry.setLocationName("BARISAL");
        lrAddressHierarchyEntry.setActive("1");
        lrAddressHierarchyEntry.setLocationLevelName("division");
    }

    @Test
    public void shouldCreateAndProperlyPopulateAllTheFieldsIfNullAddressHierarchyEntryIsGiven() throws Exception {
        AddressHierarchyEntry addressHierarchyEntry = null;

        addressHierarchyEntry = new AddressHierarchyEntryMapper().map(addressHierarchyEntry, lrAddressHierarchyEntry, addressHierarchyService);
        assertTrue(StringUtils.isNotBlank(addressHierarchyEntry.getUuid()));
        AddressHierarchyLevel level = addressHierarchyEntry.getLevel();
        int actualLevelId = level.getId();
        int expectedLevelId = 1;
        assertEquals(expectedLevelId, actualLevelId);
        assertTrue(addressHierarchyEntry.getName().equalsIgnoreCase("Barisal"));
        assertEquals("10", addressHierarchyEntry.getUserGeneratedId());
        assertEquals(null, addressHierarchyEntry.getParent());
    }

    @Test
    public void shouldUpdateFieldsIfNonNUllAddressHierarchyEntryIsGiven() throws Exception {
        when(addressHierarchyService.getAddressHierarchyEntry(anyInt())).thenReturn(new AddressHierarchyEntry());
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setName("Some Random Name");
        addressHierarchyEntry.setUserGeneratedId("randomId");

        addressHierarchyEntry = new AddressHierarchyEntryMapper().map(addressHierarchyEntry, lrAddressHierarchyEntry, addressHierarchyService);
        assertTrue(StringUtils.isNotBlank(addressHierarchyEntry.getUuid()));
        AddressHierarchyLevel level = addressHierarchyEntry.getLevel();
        int actualLevelId = level.getId();
        int expectedLevelId = 1;
        assertEquals(expectedLevelId, actualLevelId);
        assertTrue(addressHierarchyEntry.getName().equalsIgnoreCase("Barisal"));
        assertEquals("10", addressHierarchyEntry.getUserGeneratedId());
        assertEquals(null, addressHierarchyEntry.getParent());
    }

    @Test
    public void shouldReturnNullParentUGIDIfChildIsAtDivisionLevel() throws Exception {
        AddressHierarchyEntryMapper addressHierarchyEntryMapper = new AddressHierarchyEntryMapper();
        String actualParentUserGeneratedId = addressHierarchyEntryMapper.getParentUserGeneratedId(lrAddressHierarchyEntry.getFullLocationCode());
        Integer expectedParentUserGeneratedId = null;
        assertEquals(expectedParentUserGeneratedId, actualParentUserGeneratedId);
    }

    @Test
    public void shouldReturnParentUGIDIfChildIsNotAtDivisionLevel() throws Exception {
        lrAddressHierarchyEntry.setFullLocationCode("1020");
        AddressHierarchyEntryMapper addressHierarchyEntryMapper = new AddressHierarchyEntryMapper();
        String actualParentUserGeneratedId = addressHierarchyEntryMapper.getParentUserGeneratedId(lrAddressHierarchyEntry.getFullLocationCode());
        String expectedParentUserGeneratedId = "10";
        assertEquals(expectedParentUserGeneratedId, actualParentUserGeneratedId);
    }

    @Test
    public void shouldReturnParentAddressHierarchyEntry() throws Exception {
        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(anyString())).thenReturn(new AddressHierarchyEntry());
        AddressHierarchyEntryMapper addressHierarchyEntryMapper = new AddressHierarchyEntryMapper();
        AddressHierarchyEntry parentAddressHierarchyEntry = addressHierarchyEntryMapper.getParent("1010", addressHierarchyService);
        assertNotNull(parentAddressHierarchyEntry);
    }

    @Test
    public void shouldReturnNullIfUGIDIsNotFound() throws Exception {
        when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(NON_EXISTING_UGID)).thenReturn(null);
        AddressHierarchyEntryMapper addressHierarchyEntryMapper = new AddressHierarchyEntryMapper();
        AddressHierarchyEntry parentAddressHierarchyEntry = addressHierarchyEntryMapper.getParent(NON_EXISTING_UGID, addressHierarchyService);
        assertNull(parentAddressHierarchyEntry);
    }
}

