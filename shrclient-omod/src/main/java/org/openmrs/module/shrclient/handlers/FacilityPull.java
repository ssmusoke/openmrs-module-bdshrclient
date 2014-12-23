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

public class FacilityPull {
    private final Logger logger = Logger.getLogger(FacilityPull.class);

    private static final int DEFAULT_LIMIT = 100;
    private static final String EXTRA_FILTER_PATTERN = "?offset=%d&limit=%d&updatedSince=%s";
    private static final String ENCODED_SINGLE_SPACE = "%20";
    private static final String SINGLE_SPACE = " ";
    public static final String FACILITY_CONTEXT = "fr.facilities";
    public static final String SHR_LOCATION_TAG_NAME = "DGHS Facilities";
    public static final String ID_MAPPING_TYPE = "fr_location";
    public static final String INDIVIDUAL_FACILITY_CONTEXT = "fr.facilityUrlFormat";
    private static final int MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED = 1000;


    private LocationService locationService;
    private IdMappingsRepository idMappingsRepository;
    private LocationMapper locationMapper;
    private ScheduledTaskHistory scheduledTaskHistory;
    private PropertiesReader propertiesReader;
    private RestClient frWebClient;
    private LocationTag shrLocationTag;
    private String facilityUrlFormat;
    private List<String> failedDuringSaveOrUpdateOperation;
    private int noOfEntriesSynchronizedSoFar;


    public FacilityPull(PropertiesReader propertiesReader, RestClient frWebClient, LocationService locationService,
                        ScheduledTaskHistory scheduledTaskHistory, IdMappingsRepository idMappingsRepository,
                        LocationMapper locationMapper) {
        this.propertiesReader = propertiesReader;
        this.frWebClient = frWebClient;
        this.locationService = locationService;
        this.idMappingsRepository = idMappingsRepository;
        this.scheduledTaskHistory = scheduledTaskHistory;
        this.locationMapper = locationMapper;
        this.shrLocationTag = locationService.getLocationTagByName(SHR_LOCATION_TAG_NAME);
        this.failedDuringSaveOrUpdateOperation = new ArrayList<>();
    }

    public void synchronize() {
        noOfEntriesSynchronizedSoFar = 0;
        facilityUrlFormat = getFacilityUrlFormat();
        List<FRLocationEntry> frLocationEntries = synchronizeUpdates(FACILITY_CONTEXT);
        logger.info(frLocationEntries.size() + " entries updated");
    }

    private List<FRLocationEntry> synchronizeUpdates(String facilityContext) {
        String baseContextPath = propertiesReader.getFrProperties().getProperty(facilityContext);
        List<FRLocationEntry> synchronizedLocationEntries = new ArrayList<>();
        List<FRLocationEntry> lastRetrievedPartOfList;

        if (noOfEntriesSynchronizedSoFar >= MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED) {
            return synchronizedLocationEntries;
        }

        int offset = scheduledTaskHistory.getOffset(facilityContext);
        String updatedSince = scheduledTaskHistory.getUpdatedSinceDateAndTime(facilityContext);

        do {
            String completeContextPath = buildCompleteContextPath(baseContextPath, offset, updatedSince);
            lastRetrievedPartOfList = getNextChunkOfUpdatesFromFR(completeContextPath);
            if (lastRetrievedPartOfList != null) {
                saveOrUpdateFacilityEntries(lastRetrievedPartOfList);
                synchronizedLocationEntries.addAll(lastRetrievedPartOfList);
                offset += lastRetrievedPartOfList.size();
                updateMarkerTableToStoreOffset(offset, facilityContext);
                noOfEntriesSynchronizedSoFar += lastRetrievedPartOfList.size();
            } else {
                logger.info(synchronizedLocationEntries.size() + " entries synchronized");
                throw new RuntimeException("Failed to Synchronize updates from FR");
            }
        }
        while (lastRetrievedPartOfList != null && lastRetrievedPartOfList.size() == DEFAULT_LIMIT && noOfEntriesSynchronizedSoFar < MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED);

        if (lastRetrievedPartOfList != null && lastRetrievedPartOfList.size() < DEFAULT_LIMIT && noOfEntriesSynchronizedSoFar < MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED) {
            scheduledTaskHistory.setUpdatedSinceDateAndTime(facilityContext);
            updateMarkerTableToStoreOffset(0, facilityContext);
        }

        logger.info(synchronizedLocationEntries.size() + " entries synchronized");
        logger.info(failedDuringSaveOrUpdateOperation.size() + " entries failed during synchronization");
        logger.info("Synchronization Failed for the following Facilities");
        logger.info(failedDuringSaveOrUpdateOperation.toString());

        return synchronizedLocationEntries;
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
        Location location = null;
        try {
            location = locationMapper.create(frLocationEntry);
            location.addTag(shrLocationTag);
            location = locationService.saveLocation(location);
            String locationUrl = String.format(locationUrlFormat, frLocationEntry.getId());
            idMappingsRepository.saveMapping(new IdMapping(location.getUuid(), frLocationEntry.getId(),
                    ID_MAPPING_TYPE, locationUrl));
        } catch (Exception e) {
            logger.error("Error while creating a new Location : " + e);
            logger.info("Logging the failed event : " + frLocationEntry.toString());
            failedDuringSaveOrUpdateOperation.add(frLocationEntry.toString());
        }
        return location;
    }

    private Location updateExistingLocation(FRLocationEntry frLocationEntry, IdMapping idMapping) {
        logger.info("Updating existing location: " + frLocationEntry.getName());
        Location location = null;
        try {
            location = locationMapper.updateExisting(
                    locationService.getLocationByUuid(idMapping.getInternalId()), frLocationEntry);

        } catch (Exception e) {
            logger.error("Error while updating an old Location : " + e);
            logger.info("Logging the failed event : " + frLocationEntry.toString());
            failedDuringSaveOrUpdateOperation.add(frLocationEntry.toString());
        }
        return locationService.saveLocation(location);
    }

    private boolean updateMarkerTableToStoreOffset(int offset, String level) {
        boolean isMarkerUpdated = scheduledTaskHistory.setOffset(level, offset);
        logger.info(isMarkerUpdated ? "Marker Table Updated" : "Failed to Update Marker Table");
        return isMarkerUpdated;
    }

    private void saveOrUpdateFacilityEntries(List<FRLocationEntry> frLocationEntries) {
        for (FRLocationEntry frLocationEntry : frLocationEntries) {
            IdMapping idMapping = idMappingsRepository.findByExternalId(frLocationEntry.getId());
            if (idMapping != null)
                updateExistingLocation(frLocationEntry, idMapping);
            else {
                createNewLocation(frLocationEntry, shrLocationTag, facilityUrlFormat);
            }
        }
    }

    private String buildCompleteContextPath(String baseContextPath, int offset, String updatedSince) {
        return baseContextPath + getExtraFilters(offset, updatedSince);
    }

    private String getExtraFilters(int offset, String updatedSince) {
        return String.format(EXTRA_FILTER_PATTERN, offset, DEFAULT_LIMIT, updatedSince)
                        .replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE);
    }

}
