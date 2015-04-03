package org.openmrs.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.mapper.LocationMapper;
import org.openmrs.module.shrclient.model.FRLocationEntry;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.openmrs.module.shrclient.util.URLParser.parseURL;

public class FacilityPull {
    private final Logger logger = Logger.getLogger(FacilityPull.class);

    private static final int INITIAL_OFFSET = 0;
    private static final String OFFSET = "offset";
    private static final String UPDATED_SINCE = "updatedSince";
    private static final int DEFAULT_LIMIT = 100;
    private static final String EXTRA_FILTER_PATTERN = "?offset=%d&limit=%d&updatedSince=%s";
    private static final String ENCODED_SINGLE_SPACE = "%20";
    private static final String SINGLE_SPACE = " ";
    public static final String FR_FACILITY_LEVEL_FEED_URI = "urn://fr/facilities";
    public static final String FR_FACILITIES = "fr.facilities";
    public static final String SHR_LOCATION_TAG_NAME = "DGHS Facilities";
    public static final String ID_MAPPING_TYPE = "fr_location";
    public static final String INDIVIDUAL_FACILITY_CONTEXT = "fr.facilityUrlFormat";
    private static final int MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED = 1000;
    private static final String INITIAL_DATETIME = "0000-00-00 00:00:00";

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

    public void synchronize() throws IOException {
        noOfEntriesSynchronizedSoFar = 0;
        facilityUrlFormat = getFacilityUrlFormat();
        List<FRLocationEntry> frLocationEntries = synchronizeUpdates(FR_FACILITIES, FR_FACILITY_LEVEL_FEED_URI);
        logger.info(frLocationEntries.size() + " entries updated");
    }

    private List<FRLocationEntry> synchronizeUpdates(String facilityContext, String feedUri) throws IOException {
        String baseContextPath = propertiesReader.getFrProperties().getProperty(facilityContext);
        List<FRLocationEntry> synchronizedLocationEntries = new ArrayList<>();
        List<FRLocationEntry> lastRetrievedPartOfList;

        if (noOfEntriesSynchronizedSoFar >= MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED) {
            return synchronizedLocationEntries;
        }

        String feedUriForLastReadEntry = scheduledTaskHistory.getFeedUriForLastReadEntryByFeedUri(feedUri);
        int offset;
        String updatedSince;

        if (StringUtils.isBlank(feedUriForLastReadEntry)) {
            offset = INITIAL_OFFSET;
            updatedSince = INITIAL_DATETIME;
        } else {
            Map<String, String> parameters = parseURL(new URL(feedUriForLastReadEntry));
            offset = Integer.parseInt(parameters.get(OFFSET));
            updatedSince = parameters.get(UPDATED_SINCE);
        }

        String baseUrl = getBaseUrl(propertiesReader.getFrProperties());
        String completeContextPath;
        do {
            completeContextPath = buildCompleteContextPath(baseContextPath, offset, updatedSince);
            lastRetrievedPartOfList = getNextChunkOfUpdatesFromFR(completeContextPath);
            if (lastRetrievedPartOfList != null) {
                saveOrUpdateFacilityEntries(lastRetrievedPartOfList);
                synchronizedLocationEntries.addAll(lastRetrievedPartOfList);
                offset += lastRetrievedPartOfList.size();
                noOfEntriesSynchronizedSoFar += lastRetrievedPartOfList.size();
            } else {
                logger.info(synchronizedLocationEntries.size() + " entries synchronized");
                throw new RuntimeException("Failed to Synchronize updates from FR");
            }
        }
        while (lastRetrievedPartOfList != null && lastRetrievedPartOfList.size() == DEFAULT_LIMIT && noOfEntriesSynchronizedSoFar < MAX_NUMBER_OF_ENTRIES_TO_BE_SYNCHRONIZED);

        String nextCompleteContextPath;
        if (lastRetrievedPartOfList != null) {
            if (lastRetrievedPartOfList.size() == DEFAULT_LIMIT) {
                nextCompleteContextPath = buildCompleteContextPath(baseContextPath, offset, INITIAL_DATETIME);
                scheduledTaskHistory.setFeedUriForLastReadEntryByFeedUri(baseUrl + nextCompleteContextPath, feedUri);
            } else {
                nextCompleteContextPath = buildCompleteContextPath(baseContextPath, INITIAL_OFFSET, getCurrentDateAndTime());
                scheduledTaskHistory.setFeedUriForLastReadEntryByFeedUri(baseUrl + nextCompleteContextPath, feedUri);
            }

            if (!synchronizedLocationEntries.isEmpty()) {
                FRLocationEntry frLocationEntry = lastRetrievedPartOfList.get(lastRetrievedPartOfList.size() - 1);
                scheduledTaskHistory.setLastReadEntryId(frLocationEntry.getId(), feedUri);
            }
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
            logger.error("Error while downloading chunk of Updates from FR : " + e);
        }
        return downloadedData;
    }

    private String getFacilityUrlFormat() {
        return propertiesReader.getFrBaseUrl() + propertiesReader.getFrProperties().getProperty(INDIVIDUAL_FACILITY_CONTEXT);
    }

    private String getBaseUrl(Properties properties) {
        return properties.getProperty("fr.scheme") + "://" + properties.getProperty("fr.host") + "/" + properties.getProperty("fr.context");
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

    private String getCurrentDateAndTime() {
        return DateUtil.toDateString(new Date(), DateUtil.SIMPLE_DATE_WITH_SECS_FORMAT);
    }

}
