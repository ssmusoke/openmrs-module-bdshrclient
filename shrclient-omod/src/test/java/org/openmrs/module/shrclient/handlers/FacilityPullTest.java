package org.openmrs.module.shrclient.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mapper.LocationMapper;
import org.openmrs.module.shrclient.model.FRLocationEntry;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.shrclient.handlers.FacilityPull.*;

public class FacilityPullTest {
    @Mock
    LocationService locationService;
    @Mock
    private PropertiesReader propertiesReader;
    @Mock
    private RestClient frWebClient;
    @Mock
    private ScheduledTaskHistory scheduledTaskHistory;

    @Mock
    private IdMappingsRepository idMappingsRepository;

    private LocationMapper locationMapper = new LocationMapper();

    FRLocationEntry[] locationEntries;
    Properties properties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        locationEntries = getFacilityEntries();
        properties = new Properties();
        properties.put("fr.scheme", "http");
        properties.put("fr.host", "hrmtest.dghs.gov.bd");
        properties.put("fr.context", "api/1.0");
        properties.put(FR_FACILITIES, "/facilities/list");
        properties.put("fr.facilityUrlFormat", "facilities/%s.json");
    }

    @Test
    public void shouldSyncAllDataWhenFirstTime() throws Exception {
        final String existingLocationUuid = UUID.randomUUID().toString();

        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=0000-00-00%2000:00:00";

        when(propertiesReader.getFrProperties()).thenReturn(properties);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=0000-00-00%2000:00:00", FRLocationEntry[].class)).thenReturn(locationEntries);
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(idMappingsRepository.findByExternalId(any(String.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return getIdMapping(arguments[0].toString(), existingLocationUuid);
            }
        });

        when(locationService.getLocationByUuid(existingLocationUuid)).thenReturn(getFacilityLocation(existingLocationUuid));
        when(locationService.saveLocation(any(Location.class))).thenReturn(null);


        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);
        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        verify(frWebClient).get("/facilities/list?offset=0&limit=100&updatedSince=0000-00-00%2000:00:00", FRLocationEntry[].class);
        verify(scheduledTaskHistory).getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI);

    }

    @Test
    public void shouldSyncDeltaForSubsequentRun() throws Exception {
        final String existingLocationUuid = UUID.randomUUID().toString();
        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=2000-12-31 23:55:55";

        when(propertiesReader.getFrProperties()).thenReturn(properties);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class)).thenReturn(locationEntries);
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(idMappingsRepository.findByExternalId(any(String.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return getIdMapping(arguments[0].toString(), existingLocationUuid);
            }
        });
        when(locationService.getLocationByUuid(existingLocationUuid)).thenReturn(getFacilityLocation(existingLocationUuid));
        when(locationService.saveLocation(any(Location.class))).thenReturn(null);


        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);

        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        verify(frWebClient).get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class);
        verify(scheduledTaskHistory, times(1)).getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI);

    }

    @Test
    public void shouldUpdateExistingLocation() throws Exception {
        String existingLocationUuid = UUID.randomUUID().toString();
        String frLocationEntryId = "10000001";
        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=2000-12-31 23:55:55";


        when(propertiesReader.getFrProperties()).thenReturn(properties);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class)).thenReturn(oneLocationEntry(frLocationEntryId));
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(idMappingsRepository.findByExternalId(frLocationEntryId)).thenReturn(getIdMapping(frLocationEntryId, existingLocationUuid));
        when(locationService.getLocationByUuid(existingLocationUuid)).thenReturn(getFacilityLocation(existingLocationUuid));
        when(locationService.saveLocation(any(Location.class))).thenReturn(null);

        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);

        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        verify(frWebClient).get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class);
        verify(scheduledTaskHistory).getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI);
        verify(idMappingsRepository).findByExternalId(frLocationEntryId);
        verify(locationService).getLocationByUuid(existingLocationUuid);
        verify(locationService).saveLocation(any(Location.class));
    }

    @Test
    public void shouldCreateNewLocation() throws Exception {
        String frLocationEntryId = "10000001";
        String newLocationUuid = UUID.randomUUID().toString();
        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=2000-12-31 23:55:55";

        when(propertiesReader.getFrProperties()).thenReturn(properties);
        FRLocationEntry[] entries = oneLocationEntry(frLocationEntryId);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class)).thenReturn(entries);
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(idMappingsRepository.findByExternalId(frLocationEntryId)).thenReturn(null);
        when(locationService.saveLocation(any(Location.class))).thenReturn(getFacilityLocation(newLocationUuid));
        when(locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME)).thenReturn(
                new LocationTag(SHR_LOCATION_TAG_NAME, "foo bar baz"));

        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);

        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        verify(frWebClient).get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class);
        verify(scheduledTaskHistory).getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI);
        verify(idMappingsRepository).findByExternalId(frLocationEntryId);

        ArgumentCaptor<Location> locationArgumentCaptor = ArgumentCaptor.forClass(Location.class);
        verify(locationService).saveLocation(locationArgumentCaptor.capture());
        Location location = locationArgumentCaptor.getValue();
        assertEquals(1, location.getTags().size());
        assertEquals(SHR_LOCATION_TAG_NAME,
                new ArrayList<>(location.getTags()).get(0).getName());

        ArgumentCaptor<IdMapping> idMappingArgumentCaptor = ArgumentCaptor.forClass(IdMapping.class);
        verify(idMappingsRepository).saveMapping(idMappingArgumentCaptor.capture());
        IdMapping idMapping = idMappingArgumentCaptor.getValue();
        assertEquals(frLocationEntryId, idMapping.getExternalId());
        assertEquals(newLocationUuid, idMapping.getInternalId());
    }

    @Test
    public void shouldSyncMultipleNew() throws Exception {
        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=2000-12-31 23:55:55";

        when(propertiesReader.getFrProperties()).thenReturn(properties);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class)).thenReturn(locationEntries);
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(idMappingsRepository.findByExternalId(any(String.class))).thenReturn(null);
        when(locationService.saveLocation(any(Location.class))).thenReturn(getFacilityLocation(UUID.randomUUID().toString()));
        when(locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME)).thenReturn(
                new LocationTag(SHR_LOCATION_TAG_NAME, "foo bar baz"));

        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);

        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        verify(frWebClient).get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class);
        verify(scheduledTaskHistory).getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI);
        verify(locationService).getLocationTagByName(SHR_LOCATION_TAG_NAME);
        verify(idMappingsRepository, times(10)).findByExternalId(any(String.class));
        verify(locationService, times(10)).saveLocation(any(Location.class));
        verify(idMappingsRepository, times(10)).saveMapping(any(IdMapping.class));

    }

    @Test
    public void shouldUpdateNothingIfWeGetNothing() throws Exception {
        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=2000-12-31 23:55:55";

        when(propertiesReader.getFrProperties()).thenReturn(properties);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class)).thenReturn(new FRLocationEntry[]{});
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(locationService.saveLocation(any(Location.class))).thenReturn(getFacilityLocation(UUID.randomUUID().toString()));
        when(locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME)).thenReturn(
                new LocationTag(SHR_LOCATION_TAG_NAME, "foo bar baz"));

        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);

        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        verify(frWebClient).get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class);
        verify(scheduledTaskHistory).getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI);
        verify(idMappingsRepository, times(0)).findByExternalId(any(String.class));
        verify(locationService, times(1)).getLocationTagByName(SHR_LOCATION_TAG_NAME);
        verify(locationService, times(0)).saveLocation(any(Location.class));
        verify(idMappingsRepository, times(0)).saveMapping(any(IdMapping.class));

    }

    @Test
    public void shouldCreateIndividualUriForMappedIds() throws Exception {
        String feedUri = "http://hrmtest.dghs.gov.bd/api/1.0/facilities/list?offset=0&limit=100&updatedSince=2000-12-31 23:55:55";

        when(propertiesReader.getFrBaseUrl()).thenReturn("http://foo.com");
        when(propertiesReader.getFrProperties()).thenReturn(properties);
        when(frWebClient.get("/facilities/list?offset=0&limit=100&updatedSince=2000-12-31%2023:55:55", FRLocationEntry[].class)).thenReturn(oneLocationEntry("100001"));
        when(scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(FR_FACILITY_LEVEL_FEED_URI)).thenReturn(feedUri);
        when(locationService.saveLocation(any(Location.class))).thenReturn(getFacilityLocation(UUID.randomUUID().toString()));
        when(locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME)).thenReturn(
                new LocationTag(SHR_LOCATION_TAG_NAME, "foo bar baz"));

        FacilityPull facilityPull = new FacilityPull(propertiesReader, frWebClient,
                locationService, scheduledTaskHistory, idMappingsRepository, locationMapper);

        facilityPull.synchronize();

        verify(propertiesReader, times(3)).getFrProperties();
        ArgumentCaptor<IdMapping> captor = ArgumentCaptor.forClass(IdMapping.class);
        verify(idMappingsRepository).saveMapping(captor.capture());
        assertEquals("http://foo.comfacilities/100001.json", captor.getValue().getUri());
    }

    private IdMapping getIdMapping(String externalId, String existingLocationUuid) {
        return new IdMapping(existingLocationUuid, externalId, "fr_location", StringUtils.EMPTY);
    }

    private Location getFacilityLocation(String locationUuid) {
        Location location = new Location();
        location.setUuid(locationUuid);
        return location;
    }

    public FRLocationEntry[] getFacilityEntries() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        URL resource = URLClassLoader.getSystemResource("FRResponse/ResponseFromFacilityRegistry.json");
        final String response = FileUtils.readFileToString(new File(resource.getPath()));
        return mapper.readValue(response, FRLocationEntry[].class);
    }

    public FRLocationEntry[] oneLocationEntry(String id) throws IOException {
        FRLocationEntry frLocationEntry = new FRLocationEntry();
        frLocationEntry.setId(id);
        frLocationEntry.setName("bar");
        frLocationEntry.setActive("0");
        return new FRLocationEntry[]{frLocationEntry};
    }
}