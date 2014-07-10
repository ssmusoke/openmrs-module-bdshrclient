package org.bahmni.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hl7.fhir.instance.formats.ParserBase;

import java.io.ByteArrayInputStream;

public class ResourceOrFeedDeserializer extends JsonDeserializer<ParserBase.ResourceOrFeed> {

    @Override
    public ParserBase.ResourceOrFeed deserialize(JsonParser jp, DeserializationContext ctx) {
        try {
            final String json = jp.readValueAsTree().toString();
            return new org.hl7.fhir.instance.formats.JsonParser().parseGeneral(new ByteArrayInputStream(json.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
