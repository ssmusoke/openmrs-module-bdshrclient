package org.openmrs.module.shrclient.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.mapper.AddressHierarchyEntryMapper;
import org.openmrs.module.shrclient.mci.api.model.LRAddressHierarchyEntry;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.*;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_DISTRICTS_LEVEL_PROPERTY_NAME;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_DIVISIONS_LEVEL_PROPERTY_NAME;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_PAURASAVAS_LEVEL_PROPERTY_NAME;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_SYNC_TASK;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_UNIONS_LEVEL_PROPERTY_NAME;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_UPAZILAS_LEVEL_PROPERTY_NAME;
import static org.openmrs.module.shrclient.handlers.LocationRegistry.LR_WARDS_LEVEL_PROPERTY_NAME;

public class LocationRegistryTest {

    private String fileContainingDivisionLevelResponse = "DivisionLevelResponseFromLocationRegistry";
    private String fileContainingDistrictLevelResponse = "DistrictLevelResponseFromLocationRegistry";
    private String fileContainingUpazilaLevelResponse = "UpazilaLevelResponseFromLocationRegistry";
    private String fileContainingPaurasavaLevelResponse = "PaurasavaLevelResponseFromLocationRegistry";
    private String fileContainingUnionLevelResponse = "UnionLevelResponseFromLocationRegistry";
    private String fileContainingWardLevelResponse = "WardLevelResponseFromLocationRegistry";


    @Mock
    PropertiesReader propertiesReader;

    @Mock
    RestClient lrWebClient;

    @Mock
    AddressHierarchyService addressHierarchyService;

    @Mock
    ScheduledTaskHistory scheduledTaskHistory;

    @Mock
    AddressHierarchyEntryMapper addressHierarchyEntryMapper;

    LRAddressHierarchyEntry[] lrAddressHierarchyEntriesForDivisions;
    LRAddressHierarchyEntry[] lrAddressHierarchyEntriesForDistricts;
    LRAddressHierarchyEntry[] lrAddressHierarchyEntriesForUpazilas;
    LRAddressHierarchyEntry[] lrAddressHierarchyEntriesForPaurasavas;
    LRAddressHierarchyEntry[] lrAddressHierarchyEntriesForUnions;
//    LRAddressHierarchyEntry[] lrAddressHierarchyEntriesForWards;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        lrAddressHierarchyEntriesForDivisions = getAddressHierarchyEntries(fileContainingDivisionLevelResponse);
        lrAddressHierarchyEntriesForDistricts = getAddressHierarchyEntries(fileContainingDistrictLevelResponse);
        lrAddressHierarchyEntriesForUpazilas = getAddressHierarchyEntries(fileContainingUpazilaLevelResponse);
        lrAddressHierarchyEntriesForPaurasavas = getAddressHierarchyEntries(fileContainingPaurasavaLevelResponse);
        lrAddressHierarchyEntriesForUnions = getAddressHierarchyEntries(fileContainingUnionLevelResponse);
//        lrAddressHierarchyEntriesForWards = getAddressHierarchyEntries(fileContainingWardLevelResponse);
    }

    @Test
    public void shouldSynchronizeAllTheUpdatesForFirstTime() throws Exception {

        Properties properties = new Properties();
        properties.put(LR_DIVISIONS_LEVEL_PROPERTY_NAME, "/locations/list/division");
        properties.put(LR_DISTRICTS_LEVEL_PROPERTY_NAME, "/locations/list/district");
        properties.put(LR_UPAZILAS_LEVEL_PROPERTY_NAME, "/locations/list/upazila");
        properties.put(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, "/locations/list/paurasava");
        properties.put(LR_UNIONS_LEVEL_PROPERTY_NAME, "/locations/list/union");
//        properties.put(LR_WARDS_LEVEL_PROPERTY_NAME, "/locations/list/ward");

        when(scheduledTaskHistory.getLastExecutionDateAndTime(LR_SYNC_TASK)).thenReturn(StringUtils.EMPTY);
        when(propertiesReader.getLrProperties()).thenReturn(properties);
        when(scheduledTaskHistory.getOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
//        when(scheduledTaskHistory.getOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);

        String divisionContextPath = "/locations/list/division?offset=0&limit=100";
        String districtContextPath = "/locations/list/district?offset=0&limit=100";
        String upazilaContextPath = "/locations/list/upazila?offset=0&limit=100";
        String paurasavaContextPath = "/locations/list/paurasava?offset=0&limit=100";
        String unionContextPath = "/locations/list/union?offset=0&limit=100";
//        String wardContextPath = "/locations/list/ward?offset=0&limit=100";

        when(lrWebClient.get(divisionContextPath, LRAddressHierarchyEntry[].class)).thenReturn(lrAddressHierarchyEntriesForDivisions);
        when(lrWebClient.get(districtContextPath, LRAddressHierarchyEntry[].class)).thenReturn(lrAddressHierarchyEntriesForDistricts);
        when(lrWebClient.get(upazilaContextPath, LRAddressHierarchyEntry[].class)).thenReturn(lrAddressHierarchyEntriesForUpazilas);
        when(lrWebClient.get(paurasavaContextPath, LRAddressHierarchyEntry[].class)).thenReturn(lrAddressHierarchyEntriesForPaurasavas);
        when(lrWebClient.get(unionContextPath, LRAddressHierarchyEntry[].class)).thenReturn(lrAddressHierarchyEntriesForUnions);
//        when(lrWebClient.get(wardContextPath, LRAddressHierarchyEntry[].class)).thenReturn(lrAddressHierarchyEntriesForWards);

        when(scheduledTaskHistory.setOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForDivisions.length)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForDistricts.length)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForUpazilas.length)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForPaurasavas.length)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForUnions.length)).thenReturn(true);
//        when(scheduledTaskHistory.setOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForWards.length)).thenReturn(true);

        LocationRegistry locationRegistry = new LocationRegistry(propertiesReader, lrWebClient, addressHierarchyService, scheduledTaskHistory, addressHierarchyEntryMapper);
        locationRegistry.synchronize();

        verify(scheduledTaskHistory, times(1)).getLastExecutionDateAndTime(LR_SYNC_TASK);
        verify(propertiesReader, times(5)).getLrProperties();
        verify(scheduledTaskHistory, times(1)).getOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
//        verify(scheduledTaskHistory, times(1)).getOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);

        verify(lrWebClient).get(divisionContextPath, LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(districtContextPath, LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(upazilaContextPath, LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(paurasavaContextPath, LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(unionContextPath, LRAddressHierarchyEntry[].class);
//        verify(lrWebClient).get(wardContextPath, LRAddressHierarchyEntry[].class);

        verify(scheduledTaskHistory, times(1)).setOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForDivisions.length);
        verify(scheduledTaskHistory, times(1)).setOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForDistricts.length);
        verify(scheduledTaskHistory, times(1)).setOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForUpazilas.length);
        verify(scheduledTaskHistory, times(1)).setOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForPaurasavas.length);
        verify(scheduledTaskHistory, times(1)).setOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForUnions.length);
//        verify(scheduledTaskHistory, times(1)).setOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, lrAddressHierarchyEntriesForWards.length);
    }

    @Test
    public void shouldNotSynchronizeAnyUpdatesWhenAllUpdatesHaveTakenPlaceBeforeLastExecutionDataAndTime() throws Exception {
        Properties properties = new Properties();
        properties.put(LR_DIVISIONS_LEVEL_PROPERTY_NAME, "/locations/list/division");
        properties.put(LR_DISTRICTS_LEVEL_PROPERTY_NAME, "/locations/list/district");
        properties.put(LR_UPAZILAS_LEVEL_PROPERTY_NAME, "/locations/list/upazila");
        properties.put(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, "/locations/list/paurasava");
        properties.put(LR_UNIONS_LEVEL_PROPERTY_NAME, "/locations/list/union");
//        properties.put(LR_WARDS_LEVEL_PROPERTY_NAME, "/locations/list/ward");

        String stringedDate = "2014-11-11 12:00:00";

        when(scheduledTaskHistory.getLastExecutionDateAndTime(LR_SYNC_TASK)).thenReturn(stringedDate);
        when(propertiesReader.getLrProperties()).thenReturn(properties);
        when(scheduledTaskHistory.getOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
        when(scheduledTaskHistory.getOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);
//        when(scheduledTaskHistory.getOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK)).thenReturn(0);

        String divisionContextPath = "/locations/list/division?offset=0&limit=100&updatedSince=" + stringedDate;
        String districtContextPath = "/locations/list/district?offset=0&limit=100&updatedSince=" + stringedDate;
        String upazilaContextPath = "/locations/list/upazila?offset=0&limit=100&updatedSince=" + stringedDate;
        String paurasavaContextPath = "/locations/list/paurasava?offset=0&limit=100&updatedSince=" + stringedDate;
        String unionContextPath = "/locations/list/union?offset=0&limit=100&updatedSince=" + stringedDate;
//        String wardContextPath = "/locations/list/ward?offset=0&limit=100&updatedSince=" + stringedDate;

        when(lrWebClient.get(divisionContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class)).thenReturn(new LRAddressHierarchyEntry[]{});
        when(lrWebClient.get(districtContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class)).thenReturn(new LRAddressHierarchyEntry[]{});
        when(lrWebClient.get(upazilaContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class)).thenReturn(new LRAddressHierarchyEntry[]{});
        when(lrWebClient.get(paurasavaContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class)).thenReturn(new LRAddressHierarchyEntry[]{});
        when(lrWebClient.get(unionContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class)).thenReturn(new LRAddressHierarchyEntry[]{});
//        when(lrWebClient.get(wardContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class)).thenReturn(new LRAddressHierarchyEntry[]{});

        when(scheduledTaskHistory.setOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0)).thenReturn(true);
        when(scheduledTaskHistory.setOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0)).thenReturn(true);
//        when(scheduledTaskHistory.setOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0)).thenReturn(true);

        LocationRegistry locationRegistry = new LocationRegistry(propertiesReader, lrWebClient, addressHierarchyService, scheduledTaskHistory, addressHierarchyEntryMapper);
        locationRegistry.synchronize();

        verify(scheduledTaskHistory, times(1)).getLastExecutionDateAndTime(LR_SYNC_TASK);
        verify(propertiesReader, times(5)).getLrProperties();
        verify(scheduledTaskHistory, times(1)).getOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
        verify(scheduledTaskHistory, times(1)).getOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);
//        verify(scheduledTaskHistory, times(1)).getOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK);

        verify(lrWebClient).get(divisionContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(districtContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(upazilaContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(paurasavaContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class);
        verify(lrWebClient).get(unionContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class);
//        verify(lrWebClient).get(wardContextPath.replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE), LRAddressHierarchyEntry[].class);

        verify(scheduledTaskHistory, times(2)).setOffset(LR_DIVISIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0);
        verify(scheduledTaskHistory, times(2)).setOffset(LR_DISTRICTS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0);
        verify(scheduledTaskHistory, times(2)).setOffset(LR_UPAZILAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0);
        verify(scheduledTaskHistory, times(2)).setOffset(LR_PAURASAVAS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0);
        verify(scheduledTaskHistory, times(2)).setOffset(LR_UNIONS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0);
//        verify(scheduledTaskHistory, times(2)).setOffset(LR_WARDS_LEVEL_PROPERTY_NAME, LR_SYNC_TASK, 0);
    }

    public LRAddressHierarchyEntry[] getAddressHierarchyEntries(String responseFileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL resource = URLClassLoader.getSystemResource("LRResponse/" + responseFileName + ".json");
        final String response = FileUtils.readFileToString(new File(resource.getPath()));
        return mapper.readValue(response, LRAddressHierarchyEntry[].class);
    }
}