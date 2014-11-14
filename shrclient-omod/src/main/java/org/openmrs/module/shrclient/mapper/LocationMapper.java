package org.openmrs.module.shrclient.mapper;

import org.openmrs.Location;
import org.openmrs.module.shrclient.mci.api.model.FRLocationEntry;

public class LocationMapper {

    public Location updateExisting(Location location, FRLocationEntry locationEntry) {
        return writeChanges(location, locationEntry);
    }

    public Location create(FRLocationEntry locationEntry) {
        return writeChanges(new Location(), locationEntry);
    }

    private Location writeChanges(Location location, FRLocationEntry locationEntry) {
        location.setName(locationEntry.getName());
        location.setRetired("0".equals(locationEntry.getActive()));
        return location;
    }
}
