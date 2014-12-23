package org.openmrs.module.shrclient.util;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HeadersTest {

    @Test
    public void shouldCreateBasicAuthHeader() throws Exception {
        Map<String, String> basicAuthHeader = Headers.getBasicAuthHeader("foo", "bar");
        assertTrue(basicAuthHeader.containsKey(Headers.AUTH_HEADER_KEY));
        assertTrue(basicAuthHeader.get(Headers.AUTH_HEADER_KEY).startsWith("Basic "));

        byte[] encodedAuth = Base64.encodeBase64("foo:bar".getBytes(Charset.forName("UTF-8")));
        assertEquals(basicAuthHeader.get(Headers.AUTH_HEADER_KEY), "Basic " + new String(encodedAuth));

    }
}