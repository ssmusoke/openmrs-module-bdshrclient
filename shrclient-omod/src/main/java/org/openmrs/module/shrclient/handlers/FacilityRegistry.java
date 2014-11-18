package org.openmrs.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mapper.LocationMapper;
import org.openmrs.module.shrclient.mci.api.model.FRLocationEntry;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//purpose: this class represents a facility repository and provides synchronization to local OpenMRS server
public class FacilityRegistry {
    private final Logger logger = Logger.getLogger(FacilityRegistry.class);

    private static final int INITIAL_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 100;
    private static final String EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE = "?offset=%d&limit=%d";
    private static final String EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE = "?offset=%d&limit=%d&updatedSince=%s";
    public static final String FR_SYNC_TASK = "FR Sync Task";
    private static final String ENCODED_SINGLE_SPACE = "%20";
    private static final String SINGLE_SPACE = " ";
    public static final String FACILITY_CONTEXT = "fr.facilities";
    public static final String SHR_LOCATION_TAG_NAME = "SharedHealth Locations";
    public static final String ID_MAPPING_TYPE = "fr_location";
    public static final String INDIVIDUAL_FACILITY_CONTEXT = "fr.facilityUrlFormat";


    private final String lastExecutionDateTime;
    private LocationService locationService;
    private IdMappingsRepository idMappingsRepository;
    private LocationMapper locationMapper;
    private PropertiesReader propertiesReader;
    private RestClient frWebClient;

    public FacilityRegistry(PropertiesReader propertiesReader, RestClient frWebClient, LocationService locationService,
                            ScheduledTaskHistory scheduledTaskHistory, IdMappingsRepository idMappingsRepository,
                            LocationMapper locationMapper) {
        this.propertiesReader = propertiesReader;
        this.frWebClient = frWebClient;
        this.locationService = locationService;
        this.idMappingsRepository = idMappingsRepository;
        this.locationMapper = locationMapper;
        this.lastExecutionDateTime = scheduledTaskHistory.getLastExecutionDateAndTime(FR_SYNC_TASK);
    }

    public void synchronize() {
        logger.info("Starting FR location synchronization");
        List<FRLocationEntry> frLocationEntries = getUpdatesFromFR(FACILITY_CONTEXT);
        logger.info("Found " + frLocationEntries.size() + " entries to be synced");
        if (frLocationEntries.size() == 0) return;
        LocationTag shrLocationTag = locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME);
        String facilityUrlFormat = getFacilityUrlFormat();

        for (FRLocationEntry frLocationEntry : frLocationEntries) {
            IdMapping idMapping = idMappingsRepository.findByExternalId(frLocationEntry.getId());
            if (idMapping != null)
                updateExistingLocation(frLocationEntry, idMapping);
            else {
                createNewLocation(frLocationEntry, shrLocationTag, facilityUrlFormat);
            }
        }
    }

    private String getFacilityUrlFormat() {
        return propertiesReader.getFrBaseUrl() + propertiesReader.getFrProperties().getProperty(INDIVIDUAL_FACILITY_CONTEXT);
    }

    private Location createNewLocation(FRLocationEntry frLocationEntry, LocationTag shrLocationTag, String locationUrlFormat) {
        logger.info("Creating new location: " + frLocationEntry.getName());
        Location location = locationMapper.create(frLocationEntry);
        location.addTag(shrLocationTag);
        location = locationService.saveLocation(location);
        String locationUrl = String.format(locationUrlFormat, frLocationEntry.getId());
        idMappingsRepository.saveMapping(new IdMapping(location.getUuid(), frLocationEntry.getId(),
                ID_MAPPING_TYPE, locationUrl));
        return location;
    }

    private Location updateExistingLocation(FRLocationEntry frLocationEntry, IdMapping idMapping) {
        logger.info("Updating existing location: " + frLocationEntry.getName());
        Location location = locationMapper.updateExisting(
                locationService.getLocationByUuid(idMapping.getInternalId()), frLocationEntry);
        return locationService.saveLocation(location);
    }

    private List<FRLocationEntry> getUpdatesFromFR(String facilityContext) {
        String baseContextPath = propertiesReader.getFrProperties().getProperty(facilityContext);
        List<FRLocationEntry> frLocationEntries = new ArrayList<>();
        List<FRLocationEntry> lastRetrievedPartOfList;
        int offset = INITIAL_OFFSET;

        do {
            String completeContextPath = buildCompleteContextPath(baseContextPath, offset);
            lastRetrievedPartOfList = Arrays.asList(frWebClient.get(completeContextPath, FRLocationEntry[].class));
            offset += lastRetrievedPartOfList.size();
            frLocationEntries.addAll(lastRetrievedPartOfList);
        } while (lastRetrievedPartOfList.size() == DEFAULT_LIMIT);
        return frLocationEntries;
    }

    private String buildCompleteContextPath(String baseContextPath, int offset) {
        return baseContextPath + getExtraFilters(offset);
    }

    private String getExtraFilters(int offset) {
        return StringUtils.isEmpty(lastExecutionDateTime) ?
                String.format(EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE, offset, DEFAULT_LIMIT)
                :
                String.format(EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE, offset, DEFAULT_LIMIT, lastExecutionDateTime)
                        .replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE);
    }

}
