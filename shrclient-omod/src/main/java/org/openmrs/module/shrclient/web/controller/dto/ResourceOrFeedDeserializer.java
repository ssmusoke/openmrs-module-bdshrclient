package org.openmrs.module.shrclient.web.controller.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hl7.fhir.instance.formats.XmlParser;
import org.hl7.fhir.instance.model.AtomFeed;

import java.io.ByteArrayInputStream;

public class ResourceOrFeedDeserializer extends JsonDeserializer<AtomFeed> {

    @Override
    public AtomFeed deserialize(JsonParser jp, DeserializationContext ctx) {
        try {
            final String xml = ((TextNode) jp.readValueAsTree()).textValue();
            return new XmlParser(true).parseGeneral(new ByteArrayInputStream(xml.getBytes())).getFeed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
