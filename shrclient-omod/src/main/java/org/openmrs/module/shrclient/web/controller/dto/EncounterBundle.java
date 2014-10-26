package org.openmrs.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hl7.fhir.instance.formats.ParserBase;

public class EncounterBundle {

    @JsonProperty("id")
    private String encounterId;
    private String healthId;
    private String publishedDate;

    @JsonProperty("title")
    private String title;

    @JsonProperty("link")
    private String link;

    @JsonProperty("categories")
    private String[] categories;

    @JsonProperty("content")
    @JsonDeserialize(using = ResourceOrFeedDeserializer.class)
    private ParserBase.ResourceOrFeed resourceOrFeed;

    public String getEncounterId() {
        return encounterId;
    }

    public String getHealthId() {
        return healthId;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public ParserBase.ResourceOrFeed getResourceOrFeed() {
        return resourceOrFeed;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public void setPublishedDate(String date) {
        this.publishedDate = date;
    }

    public void addContent(ParserBase.ResourceOrFeed resourceOrFeed) {
        this.resourceOrFeed = resourceOrFeed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
