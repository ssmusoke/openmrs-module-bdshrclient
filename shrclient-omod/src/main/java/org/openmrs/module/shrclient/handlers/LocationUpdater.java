package org.openmrs.module.shrclient.handlers;

import org.apache.log4j.Logger;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyServiceImpl;
import org.openmrs.module.shrclient.mci.api.model.DownloadedAddressHierarchyEntry;
import org.openmrs.module.shrclient.util.PlatformUtil;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationUpdater {
    private final Logger logger = Logger.getLogger(LocationUpdater.class);

    public void update() {
        PropertiesReader propertiesReader = PlatformUtil.getPropertiesReader();
        RestClient lrWebClient = propertiesReader.getLrWebClient();

        List<DownloadedAddressHierarchyEntry> downloadedAddressHierarchyEntriesForDivisions = Arrays.asList(lrWebClient.get(propertiesReader.getLrProperties().getProperty("lr.divisions"), DownloadedAddressHierarchyEntry[].class));
        for (DownloadedAddressHierarchyEntry downloadedAddressHierarchyEntry : downloadedAddressHierarchyEntriesForDivisions) {
            downloadedAddressHierarchyEntry.setLevelId("1");
            downloadedAddressHierarchyEntry.setParentId(null);
        }

        List<DownloadedAddressHierarchyEntry> downloadedAddressHierarchyEntriesForDistricts = Arrays.asList(lrWebClient.get(propertiesReader.getLrProperties().getProperty("lr.districts"), DownloadedAddressHierarchyEntry[].class));
        for (DownloadedAddressHierarchyEntry downloadedAddressHierarchyEntry : downloadedAddressHierarchyEntriesForDistricts) {
            downloadedAddressHierarchyEntry.setLevelId("2");
//            downloadedAddressHierarchyEntry.setParent_id("1");
        }

        List<DownloadedAddressHierarchyEntry> downloadedAddressHierarchyEntriesForUpazilas = Arrays.asList(lrWebClient.get(propertiesReader.getLrProperties().getProperty("lr.upazilas"), DownloadedAddressHierarchyEntry[].class));
        for (DownloadedAddressHierarchyEntry downloadedAddressHierarchyEntry : downloadedAddressHierarchyEntriesForUpazilas) {
            downloadedAddressHierarchyEntry.setLevelId("3");
//            downloadedAddressHierarchyEntry.setParent_id("2");
        }

        List<DownloadedAddressHierarchyEntry> allDownloadedAddressHierarchyEntries = new ArrayList<>();
        allDownloadedAddressHierarchyEntries.addAll(downloadedAddressHierarchyEntriesForDivisions);
        allDownloadedAddressHierarchyEntries.addAll(downloadedAddressHierarchyEntriesForDistricts);
        allDownloadedAddressHierarchyEntries.addAll(downloadedAddressHierarchyEntriesForUpazilas);


        AddressHierarchyServiceImpl addressHierarchyService = PlatformUtil.getRegisteredComponent(AddressHierarchyServiceImpl.class);

        List<AddressHierarchyServiceImpl> addressHierarchyServices = Context.getRegisteredComponents(AddressHierarchyServiceImpl.class);

        addressHierarchyServices.size();



//        addressHierarchyService.setAddressHierarchyLevelParents();
        AddressHierarchyEntry addressHierarchyEntryByUserGenId = addressHierarchyService.getAddressHierarchyEntryByUserGenId("4001");
        System.out.println(addressHierarchyEntryByUserGenId.getName());
        System.out.println(addressHierarchyEntryByUserGenId.getAddressHierarchyLevel());
        System.out.println(addressHierarchyEntryByUserGenId.getAddressHierarchyLevel().getParent());

        allDownloadedAddressHierarchyEntries.size();




//        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
//        addressHierarchyEntry.setName("new");
//        AddressHierarchyLevel addressHierarchyLevel = new AddressHierarchyLevel();
//        addressHierarchyLevel.se
//        addressHierarchyEntry.setLevel();
//        addressHierarchyEntry.setUserGeneratedId("6091949989");


//        new AddressHierarchyLevel().
//        addressHierarchyService.getAddressHierarchyEntriesByLevel()
//        ArrayList<String> encounterFeedUrls = getEncounterFeedUrls(propertiesReader);
//        Map<String, String> requestProperties = getRequestProperties();
//        DefaultEncounterFeedWorker defaultEncounterFeedWorker = getEncounterFeedWorker();
//        for (String encounterFeedUrl : encounterFeedUrls) {
//            ShrEncounterFeedProcessor feedProcessor =
//                    new ShrEncounterFeedProcessor(encounterFeedUrl, requestProperties, defaultEncounterFeedWorker);
//            try {
//                feedProcessor.process();
//            } catch (URISyntaxException e) {
//                logger.error("Couldn't download catchment encounters. Error: ", e);
//            }
//        }
    }
}
