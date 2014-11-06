package org.openmrs.module.shrclient.mci.api.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public class DownloadedAddressHierarchyEntry {

    @JsonProperty("id")
    @JsonInclude(NON_EMPTY)
    private String id;

    @JsonProperty("name")
    @JsonInclude(NON_EMPTY)
    private String name;

    private String levelId;
    private String parentId;

    @JsonProperty("code")
    @JsonInclude(NON_EMPTY)
    private String code;

    @JsonProperty("latitude")
    @JsonInclude(NON_EMPTY)
    private String latitude;

    @JsonProperty("longitude")
    @JsonInclude(NON_EMPTY)
    private String longitude;

    @JsonProperty("updated_by")
    @JsonInclude(NON_EMPTY)
    private String updatedBy;

    @JsonProperty("created_at")
    @JsonInclude(NON_EMPTY)
    private String createdAt;

    @JsonProperty("updated_at")
    @JsonInclude(NON_EMPTY)
    private String updatedAt;

    @JsonProperty("is_active")
    @JsonInclude(NON_EMPTY)
    private String isActive;

    @JsonProperty("deleted_at")
    @JsonInclude(NON_EMPTY)
    private String deletedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }


    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadedAddressHierarchyEntry that = (DownloadedAddressHierarchyEntry) o;

        if (!code.equals(that.code)) return false;
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) return false;
        if (deletedAt != null ? !deletedAt.equals(that.deletedAt) : that.deletedAt != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (isActive != null ? !isActive.equals(that.isActive) : that.isActive != null) return false;
        if (latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) return false;
        if (longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) return false;
        if (!name.equals(that.name)) return false;
        if (updatedAt != null ? !updatedAt.equals(that.updatedAt) : that.updatedAt != null) return false;
        if (updatedBy != null ? !updatedBy.equals(that.updatedBy) : that.updatedBy != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + code.hashCode();
        result = 31 * result + (updatedBy != null ? updatedBy.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        result = 31 * result + (isActive != null ? isActive.hashCode() : 0);
        result = 31 * result + (deletedAt != null ? deletedAt.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DownloadedAddressHierarchyEntry{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", latitude='").append(latitude).append('\'');
        sb.append(", longitude='").append(longitude).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append(", updated_by='").append(updatedBy).append('\'');
        sb.append(", created_at='").append(createdAt).append('\'');
        sb.append(", updated_at='").append(updatedAt).append('\'');
        sb.append(", is_active='").append(isActive).append('\'');
        sb.append(", deleted_at='").append(deletedAt).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
