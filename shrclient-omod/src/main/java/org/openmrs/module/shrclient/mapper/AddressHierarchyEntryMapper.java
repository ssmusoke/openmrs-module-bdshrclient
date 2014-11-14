package org.openmrs.module.shrclient.mapper;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.mci.api.model.LRAddressHierarchyEntry;

import java.util.HashMap;

public class AddressHierarchyEntryMapper {


    public static final int SHORT_LOCATION_ID_LENGTH = 2;
    public static final int BEGIN_INDEX = 0;


    public final static HashMap<String, Integer> LOCATION_LEVELS = new HashMap<String, Integer>() {{
        put("division", 1);
        put("district", 2);
        put("upazila", 3);
        put("paurasava", 4);
        put("union", 5);
    }};


    public AddressHierarchyEntry map(AddressHierarchyEntry addressHierarchyEntry,
                                     LRAddressHierarchyEntry lrAddressHierarchyEntry,
                                     AddressHierarchyService addressHierarchyService) {

        addressHierarchyEntry = addressHierarchyEntry == null ? new AddressHierarchyEntry() : addressHierarchyEntry;
        AddressHierarchyLevel addressHierarchyLevel = createFromLocationLevelName(lrAddressHierarchyEntry.getLocationLevelName());
        addressHierarchyLevel.setLevelId(LOCATION_LEVELS.get(lrAddressHierarchyEntry.getLocationLevelName()));
        String parentUserGeneratedId = getParentUserGeneratedId(lrAddressHierarchyEntry.getFullLocationCode());
        AddressHierarchyEntry parentAddressHierarchyEntry = getParent(parentUserGeneratedId, addressHierarchyService);
        addressHierarchyEntry.setName(lrAddressHierarchyEntry.getLocationName());
        addressHierarchyEntry.setLevel(addressHierarchyLevel);
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId(lrAddressHierarchyEntry.getFullLocationCode());
        return addressHierarchyEntry;
    }

    public AddressHierarchyEntry getParent(String parentUserGeneratedId,
                                           AddressHierarchyService addressHierarchyService) {
        return addressHierarchyService.getAddressHierarchyEntryByUserGenId(parentUserGeneratedId);
    }

    private AddressHierarchyLevel createFromLocationLevelName(String locationLevelName) {
        AddressHierarchyLevel addressHierarchyLevel = new AddressHierarchyLevel();
        addressHierarchyLevel.setLevelId(LOCATION_LEVELS.get(locationLevelName));
        addressHierarchyLevel.setName(locationLevelName);
        return addressHierarchyLevel;
    }

    public String getParentUserGeneratedId(String fullLocationCode) {
        String parentUserGeneratedId = fullLocationCode.substring(BEGIN_INDEX, fullLocationCode.length() - SHORT_LOCATION_ID_LENGTH);
        return StringUtils.isEmpty(parentUserGeneratedId) ? null : parentUserGeneratedId;
    }

}
