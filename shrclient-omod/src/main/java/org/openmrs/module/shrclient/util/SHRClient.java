package org.openmrs.module.shrclient.util;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlComposer;
import org.hl7.fhir.instance.formats.XmlParser;
import org.hl7.fhir.instance.model.AtomFeed;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SHRClient {

    private static final Logger log = Logger.getLogger(RestClient.class);

    private String baseUrl;
    private Map<String, String> headers;

    public SHRClient(String baseUrl, Map<String, String> headers) {
        this.baseUrl = baseUrl;
        this.headers = headers;
    }

    @SuppressWarnings("unchecked")
    public List<EncounterBundle> getEncounters(final String url) throws IdentityUnauthorizedException {
        try {
            Map<String, String> requestHeaders = new HashMap<>(headers);
            requestHeaders.put("accept", "application/atom+xml");
            WebClient webClient = new WebClient(baseUrl, requestHeaders);
            String response = webClient.get(url);
            WireFeedInput input = new WireFeedInput();
            Feed feed = (Feed) input.build(new StringReader(response));
            List<Entry> entries = feed.getEntries();
            List<EncounterBundle> encounterBundles = new ArrayList<>();
            for (Entry entry : entries) {
                String entryContent = getEntryContent(entry);
                EncounterBundle bundle = new EncounterBundle();
                bundle.setEncounterId(entry.getId());
                bundle.setTitle(entry.getTitle());
                bundle.addContent(getResourceOrFeed(entryContent).getFeed());
                encounterBundles.add(bundle);
            }
            return encounterBundles;

        } catch (FeedException e) {
            log.error("Error fetching encounters for : " + url, e);
            throw new RuntimeException(e);
        }
    }

    private ParserBase.ResourceOrFeed getResourceOrFeed(String entryContent) {
        ParserBase.ResourceOrFeed resource;
        try {
            resource = new XmlParser(true).parseGeneral(new ByteArrayInputStream(entryContent.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse XML", e);
        }
        return resource;
    }

    private String getEntryContent(Entry entry) {
        if (entry.getContents().isEmpty()) {
            return null;
        }
        String value = ((Content) (entry.getContents().get(0))).getValue();
        return value.replaceFirst("^<!\\[CDATA\\[", "").replaceFirst("\\]\\]>$", "");
    }

    public String post(final String url, AtomFeed bundle) throws IdentityUnauthorizedException {
        try {
            StringEntity entity = getPayload(bundle);
            WebClient webClient = new WebClient(baseUrl, headers);
            log.debug(String.format("Posting data %s to url %s", bundle, url));
            return webClient.post(url, entity);
        } catch (IdentityUnauthorizedException e) {
            log.error("Unauthorized identity. URL: " + url, e);
            throw e;
        } catch (Exception e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
    }

    public String put(final String url, AtomFeed bundle) throws IdentityUnauthorizedException {
        try {
            StringEntity entity = getPayload(bundle);
            WebClient webClient = new WebClient(baseUrl, headers);
            log.debug(String.format("Put request %s to url %s", bundle, url));
            return webClient.put(url, entity);
        } catch (IdentityUnauthorizedException e) {
            log.error("Unauthorized identity. URL: " + url, e);
            throw e;
        } catch (Exception e) {
            log.error("Error during http post. URL: " + url, e);
            throw new RuntimeException(e);
        }
    }

    private StringEntity getPayload(AtomFeed bundle) throws Exception {
        XmlComposer composer = new XmlComposer();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        composer.compose(byteArrayOutputStream, bundle, true);
        StringEntity entity = new StringEntity(byteArrayOutputStream.toString());
        entity.setContentType("application/xml;charset=UTF-8");
        return entity;
    }
}
