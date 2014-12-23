package org.openmrs.module.shrclient.util;

import org.apache.commons.codec.binary.Base64;
import org.openmrs.module.shrclient.identity.IdentityToken;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Headers {
    public static final String AUTH_HEADER_KEY = "Authorization";
    public static final String AUTH_TOKEN_KEY = "X-Auth-Token";

    public static Map<String, String> getBasicAuthHeader(String user, String password) {
        HashMap<String, String> header = new HashMap<>();

        String auth = user + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
        header.put(AUTH_HEADER_KEY, "Basic " + new String(encodedAuth));
        return header;
    }

    public static Map<String, String> getIdentityHeader(IdentityToken token) {
        HashMap<String, String> header = new HashMap<>();
        header.put(AUTH_TOKEN_KEY, token.toString());
        return header;
    }
}
