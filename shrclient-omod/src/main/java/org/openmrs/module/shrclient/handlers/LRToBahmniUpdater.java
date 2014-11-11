package org.openmrs.module.shrclient.handlers;

import org.apache.log4j.Logger;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.mapper.AddressHierarchyEntryMapper;
import org.openmrs.module.shrclient.mci.api.model.LRAddressHierarchyEntry;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.util.DatabaseUpdater;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LRToBahmniUpdater {
    public static final int EMPTY = 0;
    public static final int INITIAL_OFFSET = 0;
    public static final int DEFAULT_LIMIT = 100;
    public static final String EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE = "?offset=%d&limit=%d";
    public static final String EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE = "?offset=%d&limit=%d&updatedSince=%s";
    private final Logger logger = Logger.getLogger(LRToBahmniUpdater.class);

    private AddressHierarchyEntryMapper addressHierarchyEntryMapper;
    private AddressHierarchyService addressHierarchyService;

    public LRToBahmniUpdater() {
        this.addressHierarchyEntryMapper = new AddressHierarchyEntryMapper();
        this.addressHierarchyService = Context.getService(AddressHierarchyService.class);

    }

    public void update() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        RestClient lrWebClient = propertiesReader.getLrWebClient();

        String lastRanDateAndTime = getLastRanDateAndTime();

        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForDivision = getLrAddressHierarchyEntryList(propertiesReader, "lr.divisions", lrWebClient, lastRanDateAndTime);
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForDistrict = getLrAddressHierarchyEntryList(propertiesReader, "lr.districts", lrWebClient, lastRanDateAndTime);
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForUpazila = getLrAddressHierarchyEntryList(propertiesReader, "lr.upazilas", lrWebClient, lastRanDateAndTime);
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForPaurasavas = getLrAddressHierarchyEntryList(propertiesReader, "lr.paurasavas", lrWebClient, lastRanDateAndTime);
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntriesForUnions = getLrAddressHierarchyEntryList(propertiesReader, "lr.unions", lrWebClient, lastRanDateAndTime);


        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForDivision);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForDistrict);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForUpazila);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForPaurasavas);
        saveOrUpdateAddressHierarchyEntries(lrAddressHierarchyEntriesForUnions);

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

    private List<LRAddressHierarchyEntry> getLrAddressHierarchyEntryList(PropertiesReader propertiesReader, String hierarchy, RestClient lrWebClient, String lastRanDateAndTime) {
        List<LRAddressHierarchyEntry> lrAddressHierarchyEntries = new ArrayList<>();
        List<LRAddressHierarchyEntry> lastRetrievedPartOfList;
        int offset = INITIAL_OFFSET;

        do {
            lastRetrievedPartOfList = Arrays.asList(lrWebClient.get(getUrl(propertiesReader, hierarchy, offset, DEFAULT_LIMIT, lastRanDateAndTime), LRAddressHierarchyEntry[].class));
            offset += lastRetrievedPartOfList.size();
            lrAddressHierarchyEntries.addAll(lastRetrievedPartOfList);
        } while (lastRetrievedPartOfList.size() != EMPTY);
        return lrAddressHierarchyEntries;
    }

    private String getUrl(PropertiesReader propertiesReader, String hierarchy, int offset, int limit, String lastRanDateAndTime) {
        return propertiesReader.getLrProperties().getProperty(hierarchy) + getExtraFilters(offset, limit, lastRanDateAndTime);
    }

    private String getExtraFilters(int offset, int limit, String lastRanDateAndTime) {
        if (lastRanDateAndTime == null)
            return String.format(EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE, offset, limit);
        else
            return String.format(EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE, offset, limit, lastRanDateAndTime).replace(" ", "%20");
    }

    public String getLastRanDateAndTime() {
        String query = "select last_execution_time from scheduler_task_config where name = 'LR Sync Task'";
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String lastExecutionTime;
        try {
            conn = DatabaseUpdater.getConnection();
            statement = conn.prepareStatement(query);
            resultSet = statement.executeQuery();
            lastExecutionTime = resultSet.next() ? resultSet.getString(1) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while querying scheduler_task_config : ", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("Could not close db statement or resultset", e);
            }
        }
        return removeUnwantedCharactersAtTheEnd(lastExecutionTime);
    }

    private String removeUnwantedCharactersAtTheEnd(String lastExecutionTime) {
        return lastExecutionTime == null ? null : lastExecutionTime.substring(0, lastExecutionTime.length() - 2);
    }
}
