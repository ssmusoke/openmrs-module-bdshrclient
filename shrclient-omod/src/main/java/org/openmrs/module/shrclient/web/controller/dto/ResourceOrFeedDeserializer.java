package org.openmrs.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;

import java.io.ByteArrayInputStream;

public class ResourceOrFeedDeserializer extends JsonDeserializer<ParserBase.ResourceOrFeed> {

    @Override
    public ParserBase.ResourceOrFeed deserialize(JsonParser jp, DeserializationContext ctx) {
        try {
            final String xml = jp.readValueAsTree().toString();
            return new XmlParser(true).parseGeneral(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
