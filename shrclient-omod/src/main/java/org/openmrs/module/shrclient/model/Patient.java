package org.openmrs.module.shrclient.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openmrs.module.fhir.utils.DateUtil;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Patient {

    @JsonProperty("nid")
    private String nationalId;

    @JsonProperty("hid")
    @JsonInclude(NON_EMPTY)
    private String healthId;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("sur_name")
    private String surName;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("dob_type")
    private String dobType;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("edu_level")
    private String educationLevel;

    @JsonProperty("present_address")
    private Address address;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("bin_brn")
    private String birthRegNumber;

    @JsonProperty("household_code")
    private String houseHoldCode;

    @JsonProperty("relations")
    @JsonInclude(NON_NULL)
    private Relation[] relations;

    @JsonProperty("provider")
    @JsonInclude(NON_NULL)
    private String providerReference;

    @JsonProperty("name_bangla")
    @JsonInclude(NON_EMPTY)
    private String banglaName;

    @JsonProperty("phone_number")
    @JsonInclude(NON_EMPTY)
    private PhoneNumber phoneNumber;

    @JsonProperty("active")
    @JsonInclude(NON_EMPTY)
    private Boolean active;

    @JsonProperty("merged_with")
    @JsonInclude(NON_EMPTY)
    private String mergedWith;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;

        Patient patient = (Patient) o;

        if (address != null ? !address.equals(patient.address) : patient.address != null) return false;
        if (dateOfBirth != null ? !DateUtil.isEqualTo(getDateOfBirth(), patient.getDateOfBirth()) : patient.dateOfBirth != null)
            return false;
        if (educationLevel != null ? !educationLevel.equals(patient.educationLevel) : patient.educationLevel != null)
            return false;
        if (gender != null ? !gender.equals(patient.gender) : patient.gender != null) return false;
        if (healthId != null ? !healthId.equals(patient.healthId) : patient.healthId != null) return false;
        if (surName != null ? !surName.equals(patient.surName) : patient.surName != null) return false;
        if (surName != null ? !surName.equals(patient.surName) : patient.surName != null) return false;
        if (nationalId != null ? !nationalId.equals(patient.nationalId) : patient.nationalId != null) return false;
        if (occupation != null ? !occupation.equals(patient.occupation) : patient.occupation != null) return false;
        if (birthRegNumber != null ? !birthRegNumber.equals(patient.birthRegNumber) : patient.birthRegNumber != null)
            return false;
        if (houseHoldCode != null ? !houseHoldCode.equals(patient.houseHoldCode) : patient.houseHoldCode != null)
            return false;
        if (dobType != null ? !dobType.equals(patient.dobType) : patient.dobType != null)
            return false;
        if (providerReference != null ? !providerReference.equals(patient.providerReference) : patient.providerReference != null)
            return false;
        if (phoneNumber != null ? !phoneNumber.equals(patient.phoneNumber) : patient.phoneNumber != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = nationalId != null ? nationalId.hashCode() : 0;
        result = 31 * result + (healthId != null ? healthId.hashCode() : 0);
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (surName != null ? surName.hashCode() : 0);
        result = 31 * result + (banglaName != null ? banglaName.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + (dobType != null ? dobType.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (occupation != null ? occupation.hashCode() : 0);
        result = 31 * result + (educationLevel != null ? educationLevel.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (birthRegNumber != null ? birthRegNumber.hashCode() : 0);
        result = 31 * result + (houseHoldCode != null ? houseHoldCode.hashCode() : 0);
        result = 31 * result + (providerReference != null ? providerReference.hashCode() : 0);
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Patient{");
        sb.append("nationalId='").append(nationalId).append('\'');
        sb.append(", healthId='").append(healthId).append('\'');
        sb.append(", givenName='").append(givenName).append('\'');
        sb.append(", surName='").append(surName).append('\'');
        sb.append(", banglaName='").append(banglaName).append('\'');
        sb.append(", dateOfBirth='").append(dateOfBirth).append('\'');
        sb.append(", dobType='").append(dobType).append('\'');
        sb.append(", address=").append(address);
        sb.append(", gender='").append(gender).append('\'');
        sb.append(", occupation='").append(occupation).append('\'');
        sb.append(", educationLevel='").append(educationLevel).append('\'');
        sb.append(", birthRegNumber='").append(birthRegNumber).append('\'');
        sb.append(", houseHoldCode='").append(houseHoldCode).append('\'');
        sb.append(", providerReference='").append(providerReference).append('\'');
        sb.append(", phoneNumber='").append(phoneNumber).append('\'');
        sb.append('}');
        return sb.toString();
    }


    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth == null ? null : DateUtil.parseDate(dateOfBirth);
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth == null ? null : DateUtil.toDateString(dateOfBirth, DateUtil.ISO_8601_DATE_IN_SECS_FORMAT2);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getBirthRegNumber() {
        return birthRegNumber;
    }

    public void setBirthRegNumber(String birthRegNumber) {
        this.birthRegNumber = birthRegNumber;
    }

    public Relation[] getRelations() {
        return relations;
    }

    public void setRelations(Relation[] relations) {
        this.relations = relations;
    }

    public String getHouseHoldCode() {
        return houseHoldCode;
    }

    public void setHouseHoldCode(String houseHoldCode) {
        this.houseHoldCode = houseHoldCode;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }


    public String getProviderReference() {
        return providerReference;
    }

    public String getBanglaName() {
        return banglaName;
    }

    public void setBanglaName(String banglaName) {
        this.banglaName = banglaName;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @JsonIgnore
    public Boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDobType() {
        return dobType;
    }

    public void setDobType(String dobType) {
        this.dobType = dobType;

    }

    public String getMergedWith() {
        return mergedWith;
    }

    public void setMergedWith(String mergedWith) {
        this.mergedWith = mergedWith;
    }
}

