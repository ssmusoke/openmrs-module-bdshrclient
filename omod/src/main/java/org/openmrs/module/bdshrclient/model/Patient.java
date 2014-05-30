package org.openmrs.module.bdshrclient.model;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

public class Patient {

    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("present_address")
    private Address address;
    @JsonProperty("gender")
    private String gender;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String givenName, String middleName, String familyName) {
        this.fullName = StringUtils.isBlank(middleName) ? (givenName + " " + familyName)
                : (givenName + " " + middleName + " " + familyName);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
