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

    private static final String ENCODED_SINGLE_SPACE = "%20";
    private static final String SINGLE_SPACE = " ";
    private static final int INITIAL_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 100;
    private static final String EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE = "?offset=%d&limit=%d";
    private static final String EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE = "?offset=%d&limit=%d&updatedSince=%s";
    private static final String LR_SYNC_TASK = "LR Sync Task";

    private final String lastExecutionDateTime;
    private AddressHierarchyEntryMapper addressHierarchyEntryMapper;
    private AddressHierarchyService addressHierarchyService;
    private RestClient lrWebClient;
    private PropertiesReader propertiesReader;

    public LocationRegistry(PropertiesReader propertiesReader, RestClient lrWebClient, AddressHierarchyService addressHierarchyService, ScheduledTaskHistory scheduledTaskHistory, AddressHierarchyEntryMapper addressHierarchyEntryMapper) {
        this.lrWebClient = lrWebClient;
        this.propertiesReader = propertiesReader;
        this.addressHierarchyEntryMapper = addressHierarchyEntryMapper;
        this.addressHierarchyService = addressHierarchyService;
        this.lastExecutionDateTime = scheduledTaskHistory.getLastExecutionDateAndTime(LR_SYNC_TASK);

    }

    public void synchronize() {
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForDivision = getUpdatesFromLR("lr.divisions");
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForDistrict = getUpdatesFromLR("lr.districts");
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForUpazila = getUpdatesFromLR("lr.upazilas");
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForPaurasavas = getUpdatesFromLR("lr.paurasavas");
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForUnions = getUpdatesFromLR("lr.unions");

        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForDivision);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForDistrict);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForUpazila);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForPaurasavas);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForUnions);
    }

    private List<LRAddressHierarchyEntry> getUpdatesFromLR(String level) {
        String baseContextPath = propertiesReader.getLrProperties().getProperty(level);
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntries = new ArrayList<>();
        List<LRAddressHierarchyEntry> lastRetrievedPartOfList;
        int offset = INITIAL_OFFSET;
        do {
            String completeContextPath = buildCompleteContextPath(baseContextPath, offset);
            lastRetrievedPartOfList = Arrays.asList(lrWebClient.get(completeContextPath, LRAddressHierarchyEntry[].class));
            offset += lastRetrievedPartOfList.size();
            lrAddressHierarchyEntries.addAll(lastRetrievedPartOfList);
        } while (lastRetrievedPartOfList.size() == DEFAULT_LIMIT);
        return lrAddressHierarchyEntries;
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
