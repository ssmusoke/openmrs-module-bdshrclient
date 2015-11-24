package org.openmrs.module.shrclient.web.controller.dto;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.StringUtils;

public class EncounterBundle {

    @JsonProperty("id")
    private String eventId;
    private String healthId;
    private String publishedDate;

    @JsonProperty("title")
    private String title;

    @JsonProperty("link")
    private String link;

    @JsonProperty("categories")
    private String[] categories;

    @JsonProperty("content")
    @JsonDeserialize(using = BundleDeserializer.class)
    private Bundle bundle;
    private String encounterId;

    public String getEncounterId() {
        return encounterId;
    }

    public String getHealthId() {
        return healthId;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public void setPublishedDate(String date) {
        this.publishedDate = date;
    }

    public void addContent(Bundle bundle) {
        this.bundle = bundle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.encounterId = StringUtils.substringAfter(title, "Encounter:");
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }
}
