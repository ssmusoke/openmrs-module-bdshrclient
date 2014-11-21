package org.openmrs.module.shrclient.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.mapper.AddressHierarchyEntryMapper;
import org.openmrs.module.shrclient.mci.api.model.LRAddressHierarchyEntry;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.ScheduledTaskHistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//purpose: this class represents a location repository and provides synchronization to local OpenMRS server
public class LocationRegistry {
    private final Logger logger = Logger.getLogger(LocationRegistry.class);

    public static final String LR_DIVISIONS_LEVEL_PROPERTY_NAME = "lr.divisions";
    public static final String LR_DISTRICTS_LEVEL_PROPERTY_NAME = "lr.districts";
    public static final String LR_UPAZILAS_LEVEL_PROPERTY_NAME = "lr.upazilas";
    public static final String LR_PAURASAVAS_LEVEL_PROPERTY_NAME = "lr.paurasavas";
    public static final String LR_UNIONS_LEVEL_PROPERTY_NAME = "lr.unions";
    public static final String LR_WARDS_LEVEL_PROPERTY_NAME = "lr.wards";

    public static final String ENCODED_SINGLE_SPACE = "%20";
    public static final String SINGLE_SPACE = " ";
    private static final int DEFAULT_LIMIT = 100;
    private static final String EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE = "?offset=%s&limit=%s";
    private static final String EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE = "?offset=%s&limit=%s&updatedSince=%s";
    public static final String LR_SYNC_TASK = "LR Sync Task";

    private final String lastExecutionDateTime;
    private ScheduledTaskHistory scheduledTaskHistory;
    private AddressHierarchyEntryMapper addressHierarchyEntryMapper;
    private AddressHierarchyService addressHierarchyService;
    private RestClient lrWebClient;
    private PropertiesReader propertiesReader;

    public LocationRegistry(PropertiesReader propertiesReader, RestClient lrWebClient, AddressHierarchyService addressHierarchyService,
                            ScheduledTaskHistory scheduledTaskHistory, AddressHierarchyEntryMapper addressHierarchyEntryMapper) {
        this.lrWebClient = lrWebClient;
        this.propertiesReader = propertiesReader;
        this.scheduledTaskHistory = scheduledTaskHistory;
        this.addressHierarchyEntryMapper = addressHierarchyEntryMapper;
        this.addressHierarchyService = addressHierarchyService;
        this.lastExecutionDateTime = scheduledTaskHistory.getLastExecutionDateAndTime(LR_SYNC_TASK);
    }

    public void synchronize() {

        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntriesForDivisions = synchronizeUpdatesByLevel(LR_DIVISIONS_LEVEL_PROPERTY_NAME);
        logger.info(synchronizedAddressHierarchyEntriesForDivisions.size() + " entries updated");

        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntriesForDistricts = synchronizeUpdatesByLevel(LR_DISTRICTS_LEVEL_PROPERTY_NAME);
        logger.info(synchronizedAddressHierarchyEntriesForDistricts.size() + " entries updated");

        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntriesForUpazilas = synchronizeUpdatesByLevel(LR_UPAZILAS_LEVEL_PROPERTY_NAME);
        logger.info(synchronizedAddressHierarchyEntriesForUpazilas.size() + " entries updated");

        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntriesForPaurasavas = synchronizeUpdatesByLevel(LR_PAURASAVAS_LEVEL_PROPERTY_NAME);
        logger.info(synchronizedAddressHierarchyEntriesForPaurasavas.size() + " entries updated");

        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntriesForUnions = synchronizeUpdatesByLevel(LR_UNIONS_LEVEL_PROPERTY_NAME);
        logger.info(synchronizedAddressHierarchyEntriesForUnions.size() + " entries updated");

//        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntriesForWards = synchronizeUpdatesByLevel(LR_WARDS_LEVEL_PROPERTY_NAME);
//        logger.info(synchronizedAddressHierarchyEntriesForWards.size() + " entries updated");

        reinitializeMarkerTableToZero();
    }

    private void reinitializeMarkerTableToZero() {
        updateMarkerTableToStoreOffset(0, LR_DIVISIONS_LEVEL_PROPERTY_NAME);
        updateMarkerTableToStoreOffset(0, LR_DISTRICTS_LEVEL_PROPERTY_NAME);
        updateMarkerTableToStoreOffset(0, LR_UPAZILAS_LEVEL_PROPERTY_NAME);
        updateMarkerTableToStoreOffset(0, LR_PAURASAVAS_LEVEL_PROPERTY_NAME);
        updateMarkerTableToStoreOffset(0, LR_UNIONS_LEVEL_PROPERTY_NAME);
        updateMarkerTableToStoreOffset(0, LR_WARDS_LEVEL_PROPERTY_NAME);
    }

    private List<LRAddressHierarchyEntry> synchronizeUpdatesByLevel(String levelName) {
        String baseContextPath = propertiesReader.getLrProperties().getProperty(levelName);
        List<LRAddressHierarchyEntry> synchronizedAddressHierarchyEntries = new ArrayList<>();
        List<LRAddressHierarchyEntry> lastRetrievedPartOfList;
        int offset = scheduledTaskHistory.getOffset(levelName, LR_SYNC_TASK);
        do {
            String completeContextPath = buildCompleteContextPath(baseContextPath, offset);
            lastRetrievedPartOfList = getNextChunkOfUpdatesFromLR(completeContextPath);
            if (lastRetrievedPartOfList != null) {
                saveOrUpdateAddressHierarchyEntries(lastRetrievedPartOfList);
                synchronizedAddressHierarchyEntries.addAll(lastRetrievedPartOfList);
                offset += lastRetrievedPartOfList.size();
                updateMarkerTableToStoreOffset(offset, levelName);
            } else {
                logger.info(synchronizedAddressHierarchyEntries.size() + " entries synchronized");
                throw new RuntimeException("Failed to Synchronize updates from LR");
            }
        } while (lastRetrievedPartOfList != null && lastRetrievedPartOfList.size() == DEFAULT_LIMIT);

        logger.info(synchronizedAddressHierarchyEntries.size() + " entries synchronized");
        return synchronizedAddressHierarchyEntries;
    }

    private List<LRAddressHierarchyEntry> getNextChunkOfUpdatesFromLR(String completeContextPath) {
        List<LRAddressHierarchyEntry> downloadedData = null;
        try {
            downloadedData = Arrays.asList(lrWebClient.get(completeContextPath, LRAddressHierarchyEntry[].class));
        } catch (Exception e) {
            logger.error("Error while downloading chunk of Updates from LR : " + e);
        }
        return downloadedData;
    }

    private boolean updateMarkerTableToStoreOffset(int offset, String level) {
        boolean isMarkerUpdated = scheduledTaskHistory.setOffset(level, LR_SYNC_TASK, offset);
        logger.info(isMarkerUpdated ? "Marker Table Updated" : "Failed to Update Marker Table");
        return isMarkerUpdated;
    }

    private String buildCompleteContextPath(String baseContextPath, int offset) {
        return baseContextPath + getExtraFilters(offset);
    }

    private String getExtraFilters(int offset) {
        return StringUtils.isEmpty(lastExecutionDateTime) ?
                String.format(EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE, offset, DEFAULT_LIMIT) :
                String.format(EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE, offset, DEFAULT_LIMIT, lastExecutionDateTime).replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE);
    }

    private void saveOrUpdateAddressHierarchyEntries(List<LRAddressHierarchyEntry> lrAddressHierarchyEntries) {
        for (LRAddressHierarchyEntry lrAddressHierarchyEntry : lrAddressHierarchyEntries) {
            AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(lrAddressHierarchyEntry.getFullLocationCode());
            addressHierarchyEntry = addressHierarchyEntryMapper.map(addressHierarchyEntry, lrAddressHierarchyEntry, addressHierarchyService);
            try {
                if (addressHierarchyEntry.getId() == null)
                    logger.info("Saving Address Hierarchy Entry to Local DB : \n" + addressHierarchyEntry.toString());
                else
                    logger.info("Updating Address Hierarchy Entry to Local Db : " + addressHierarchyEntry.toString());

                addressHierarchyService.saveAddressHierarchyEntry(addressHierarchyEntry);
            } catch (Exception e) {
                logger.error("Error during Save Or Update to Local Db : " + e.toString());
            }
        }
    }
}
