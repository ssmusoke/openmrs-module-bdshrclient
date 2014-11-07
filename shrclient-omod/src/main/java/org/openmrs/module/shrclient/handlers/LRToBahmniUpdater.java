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

public class LRToBahmniUpdater {
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


//      TODO : use Arrays.asList(lrWebClient.get(propertiesReader.getLrProperties().getProperty("lr.upazilas"), lrAddressHierarchyEntry[].class)); when hitting lists
        LRAddressHierarchyEntry lrAddressHierarchyEntry = lrWebClient.get(propertiesReader.getLrProperties().getProperty("lr.locationId"), LRAddressHierarchyEntry.class);


        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(lrAddressHierarchyEntry.getFullLocationCode());
        addressHierarchyEntry = addressHierarchyEntryMapper.map(addressHierarchyEntry, lrAddressHierarchyEntry, addressHierarchyService);

        try {
            if (addressHierarchyEntry.getId() == null)
                logger.info("Saving Address Hierarchy Entry to Local DB : \n" + addressHierarchyEntry.toString());
            else
                logger.info("Updating Address Hierarchy Entry to Local Db : " + addressHierarchyEntry.toString());

            addressHierarchyService.saveAddressHierarchyEntry(addressHierarchyEntry);
        }
        catch (Exception e) {
            logger.error("Error during Save Or Update to Local Db : " + e.toString());
        }


    }
}
