package org.bahmni.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hl7.fhir.instance.formats.ParserBase;

public class EncounterBundle {

    private String encounterId;
    private String healthId;
    private String date;

    @JsonProperty("content")
    @JsonDeserialize(using = ResourceOrFeedDeserializer.class)
    private ParserBase.ResourceOrFeed resourceOrFeed;

    public String getEncounterId() {
        return encounterId;
    }

    public String getHealthId() {
        return healthId;
    }

    public String getDate() {
        return date;
    }

    public ParserBase.ResourceOrFeed getResourceOrFeed() {
        return resourceOrFeed;
    }
}


