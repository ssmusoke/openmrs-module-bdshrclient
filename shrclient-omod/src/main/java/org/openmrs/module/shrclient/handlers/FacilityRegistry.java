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
    private ScheduledTaskHistory scheduledTaskHistory;
    private PropertiesReader propertiesReader;
    private RestClient frWebClient;
    private LocationTag shrLocationTag;
    private String facilityUrlFormat;


    public FacilityRegistry(PropertiesReader propertiesReader, RestClient frWebClient, LocationService locationService,
                            ScheduledTaskHistory scheduledTaskHistory, IdMappingsRepository idMappingsRepository,
                            LocationMapper locationMapper) {
        this.propertiesReader = propertiesReader;
        this.frWebClient = frWebClient;
        this.locationService = locationService;
        this.idMappingsRepository = idMappingsRepository;
        this.scheduledTaskHistory = scheduledTaskHistory;
        this.locationMapper = locationMapper;
        this.lastExecutionDateTime = scheduledTaskHistory.getLastExecutionDateAndTime(FR_SYNC_TASK);
        this.shrLocationTag = locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME);
    }

    public void synchronize() {
        facilityUrlFormat = getFacilityUrlFormat();
        List<FRLocationEntry> frLocationEntries = synchronizeUpdates(FACILITY_CONTEXT);
        logger.info(frLocationEntries.size() + " entries updated");

        reinitializeMarkerTableToZero();
    }

    private void reinitializeMarkerTableToZero() {
        updateMarkerTableToStoreOffset(0, FACILITY_CONTEXT);
    }

    private List<FRLocationEntry> getNextChunkOfUpdatesFromFR(String completeContextPath) {
        List<FRLocationEntry> downloadedData = null;
        try {
            downloadedData = Arrays.asList(frWebClient.get(completeContextPath, FRLocationEntry[].class));
        } catch (Exception e) {
            logger.error("Error while downloading chunk of Updates from LR : " + e);
        }
        return downloadedData;
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

    private List<FRLocationEntry> synchronizeUpdates(String facilityContext) {
        String baseContextPath = propertiesReader.getFrProperties().getProperty(facilityContext);
        List<FRLocationEntry> synchronizedLocationEntries = new ArrayList<>();
        List<FRLocationEntry> lastRetrievedPartOfList;
        int offset = scheduledTaskHistory.getOffset(facilityContext, FR_SYNC_TASK);

        do {
            String completeContextPath = buildCompleteContextPath(baseContextPath, offset);
            lastRetrievedPartOfList = getNextChunkOfUpdatesFromFR(completeContextPath);
            if (lastRetrievedPartOfList != null) {
                saveOrUpdateAddressHierarchyEntries(lastRetrievedPartOfList);
                synchronizedLocationEntries.addAll(lastRetrievedPartOfList);
                offset += lastRetrievedPartOfList.size();
                updateMarkerTableToStoreOffset(offset, facilityContext);
            } else {
                logger.info(synchronizedLocationEntries.size() + " entries synchronized");
                throw new RuntimeException("Failed to Synchronize updates from FR");
            }
        }
        while (lastRetrievedPartOfList != null && lastRetrievedPartOfList.size() == DEFAULT_LIMIT);

        logger.info(synchronizedLocationEntries.size() + " entries synchronized");
        return synchronizedLocationEntries;
    }

    private boolean updateMarkerTableToStoreOffset(int offset, String level) {
        boolean isMarkerUpdated = scheduledTaskHistory.setOffset(level, FR_SYNC_TASK, offset);
        logger.info(isMarkerUpdated ? "Marker Table Updated" : "Failed to Update Marker Table");
        return isMarkerUpdated;
    }

    private void saveOrUpdateAddressHierarchyEntries(List<FRLocationEntry> frLocationEntries) {
        for (FRLocationEntry frLocationEntry : frLocationEntries) {
            IdMapping idMapping = idMappingsRepository.findByExternalId(frLocationEntry.getId());
            if (idMapping != null)
                updateExistingLocation(frLocationEntry, idMapping);
            else {
                createNewLocation(frLocationEntry, shrLocationTag, facilityUrlFormat);
            }
        }
    }

    private String buildCompleteContextPath(String baseContextPath, int offset) {
        return baseContextPath + getExtraFilters(offset);
    }

    private String getExtraFilters(int offset) {
        return StringUtils.isEmpty(lastExecutionDateTime) ?
                String.format(EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE, offset, DEFAULT_LIMIT) :
                String.format(EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE, offset, DEFAULT_LIMIT, lastExecutionDateTime)
                        .replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE);
    }

}
