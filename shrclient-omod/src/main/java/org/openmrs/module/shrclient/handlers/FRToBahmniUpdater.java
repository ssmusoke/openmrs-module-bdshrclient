package org.openmrs.module.shrclient.handlers;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.mci.api.model.FRLocationEntry;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.util.SchedulerTaskConfigQueryUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FRToBahmniUpdater {
    private final Logger logger = Logger.getLogger(FRToBahmniUpdater.class);

    private static final int INITIAL_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 100;
    private static final int EMPTY = 0;
    private static final String EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE = "?offset=%d&limit=%d";
    private static final String EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE = "?offset=%d&limit=%d&updatedSince=%s";
    private static final String FR_SYNC_TASK = "FR Sync Task";
    private static final String ENCODED_SINGLE_SPACE = "%20";
    private static final String SINGLE_SPACE = " ";

    public FRToBahmniUpdater() {
    }

    public void update() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        RestClient frWebClient = propertiesReader.getFrWebClient();

        String lastExecutionDateAndTime = new SchedulerTaskConfigQueryUtil().getLastExecutionDateAndTime(FR_SYNC_TASK);

        List<FRLocationEntry> frLocationEntries = getUpdatesFromFR100(propertiesReader, "fr.facilities", frWebClient, lastExecutionDateAndTime);

        System.out.println(frLocationEntries.size());



    }

    // TODO : use only during development
    private List<FRLocationEntry> getUpdatesFromFR100(PropertiesReader propertiesReader, String facilityContext, RestClient frWebClient, String lastExecutionDateAndTime) {
        List<FRLocationEntry> frLocationEntries = Arrays.asList(frWebClient.get(getUrl(propertiesReader, facilityContext, 0, DEFAULT_LIMIT, lastExecutionDateAndTime), FRLocationEntry[].class));
        return frLocationEntries;
    }

    private List<FRLocationEntry> getUpdatesFromFR(PropertiesReader propertiesReader, String facilityContext, RestClient frWebClient, String lastExecutionDateAndTime) {
        List<FRLocationEntry> frLocationEntries = new ArrayList<>();
        List<FRLocationEntry> lastRetrievedPartOfList;
        int offset = INITIAL_OFFSET;

        do {
            lastRetrievedPartOfList = Arrays.asList(frWebClient.get(getUrl(propertiesReader, facilityContext, offset, DEFAULT_LIMIT, lastExecutionDateAndTime), FRLocationEntry[].class));
            offset += lastRetrievedPartOfList.size();
            frLocationEntries.addAll(lastRetrievedPartOfList);
        } while (lastRetrievedPartOfList.size() != EMPTY);
        return frLocationEntries;
    }

    private String getUrl(PropertiesReader propertiesReader, String facilityContext, int offset, int limit, String lastExecutionDateAndTime) {
        return propertiesReader.getFrProperties().getProperty(facilityContext) + getExtraFilters(offset, limit, lastExecutionDateAndTime);
    }

    private String getExtraFilters(int offset, int limit, String lastRanDateAndTime) {
        if (lastRanDateAndTime == null)
            return String.format(EXTRA_FILTER_PATTERN_WITHOUT_UPDATED_SINCE, offset, limit);
        else
            return String.format(EXTRA_FILTER_PATTERN_WITH_UPDATED_SINCE, offset, limit, lastRanDateAndTime).replace(SINGLE_SPACE, ENCODED_SINGLE_SPACE);
    }
}
