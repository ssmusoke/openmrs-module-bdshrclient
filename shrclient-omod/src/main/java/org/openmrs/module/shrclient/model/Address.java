package org.openmrs.module.shrclient.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Address {
    @JsonProperty("address_line")
    private String addressLine;

    @JsonProperty("division_id")
    private String divisionId;

    @JsonProperty("district_id")
    private String districtId;

    @JsonProperty("upazilla_id")
    private String upazillaId;

    @JsonProperty("city_corporation")
    private String cityCorporationId;

    @JsonProperty("ward")
    private String wardId;

    public Address() {
    }

    public Address(String addressLine, String divisionId, String districtId, String upazillaId, String cityCorporationId, String wardId) {
        this.addressLine = addressLine;
        this.divisionId = divisionId;
        this.districtId = districtId;
        this.upazillaId = upazillaId;
        this.cityCorporationId = cityCorporationId;
        this.wardId = wardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;

        Address address = (Address) o;

        if (addressLine != null ? !addressLine.equals(address.addressLine) : address.addressLine != null) return false;
        if (divisionId != null ? !divisionId.equals(address.divisionId) : address.divisionId != null) return false;
        if (districtId != null ? !districtId.equals(address.districtId) : address.districtId != null) return false;
        if (upazillaId != null ? !upazillaId.equals(address.upazillaId) : address.upazillaId != null) return false;
        if (cityCorporationId != null ? !cityCorporationId.equals(address.cityCorporationId) : address.cityCorporationId != null)
            return false;
        if (wardId != null ? !wardId.equals(address.wardId) : address.wardId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = addressLine != null ? addressLine.hashCode() : 0;
        result = 31 * result + (divisionId != null ? divisionId.hashCode() : 0);
        result = 31 * result + (districtId != null ? districtId.hashCode() : 0);
        result = 31 * result + (upazillaId != null ? upazillaId.hashCode() : 0);
        result = 31 * result + (cityCorporationId != null ? cityCorporationId.hashCode() : 0);
        result = 31 * result + (wardId != null ? wardId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Address{");
        sb.append("addressLine='").append(addressLine).append('\'');
        sb.append(", divisionId='").append(divisionId).append('\'');
        sb.append(", districtId='").append(districtId).append('\'');
        sb.append(", upazillaId='").append(upazillaId).append('\'');
        sb.append(", cityCorporationId='").append(cityCorporationId).append('\'');
        sb.append(", wardId='").append(wardId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getUpazillaId() {
        return upazillaId;
    }

    public void setUpazillaId(String upazillaId) {
        this.upazillaId = upazillaId;
    }

    public String getCityCorporationId() {
        return cityCorporationId;
    }

    public void setCityCorporationId(String cityCorporationId) {
        this.cityCorporationId = cityCorporationId;
    }

    public String getWardId() {
        return wardId;
    }

    public void setWardId(String wardId) {
        this.wardId = wardId;
    }

    public String createUserGeneratedDistrictId() {
        return divisionId + districtId;
    }

    public String createUserGeneratedUpazillaId() {
        return divisionId + districtId + upazillaId;
    }

    public String createUserGeneratedCityCorporationId() {
        return divisionId + districtId + upazillaId + cityCorporationId;
    }

    public String createUserGeneratedWardId() {
        return divisionId + districtId + upazillaId + cityCorporationId + wardId;
    }
}
