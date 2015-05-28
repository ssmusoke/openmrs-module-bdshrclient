package org.openmrs.module.shrclient.model;

public class FacilityCatchment {
    int locationId;
    String catchment;
    boolean active;

    public FacilityCatchment(int locationId, String catchment) {
        this.locationId = locationId;
        this.catchment = catchment;
    }

    public String getCatchment() {
        return catchment;
    }

    public int getLocationId() {
        return locationId;
    }

}
