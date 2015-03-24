package org.openmrs.module.shrclient.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;


//purpose: reads properties from property files
@Component("bdshrPropertiesReader")
public class PropertiesReader {

    public static final String REGEX_TO_MATCH_PORT_NO = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
    public static final String URL_SEPARATOR_FOR_CONTEXT_PATH = "/";
    public static final String URL_SEPARATOR_FOR_PORT_NO = ":";

    public Properties getMciProperties() {
        return getProperties("mci.properties");
    }

    public Properties getShrProperties() {
        return getProperties("shr.properties");
    }

    public Properties getLrProperties() {
        return getProperties("lr.properties");
    }

    public Properties getFrProperties() {
        return getProperties("fr.properties");
    }

    public Properties getPrProperties() {
        return getProperties("pr.properties");
    }

    public Properties getTrProperties() {
        return getProperties("tr_atomfeed_properties.properties");
    }

    public Properties getIdentityProperties() {
        return getProperties("identity.properties");
    }

    public Properties getFacilityInstanceProperties() {
        return getProperties("facility_instance.properties");
    }

    public HashMap<String, String> getBaseUrls(){
        HashMap<String, String> baseUrls = new HashMap<>();
        baseUrls.put("mci", getMciBaseUrl()) ;
        baseUrls.put("fr", getFrBaseUrl()) ;
        baseUrls.put("shr", getShrBaseUrl()) ;
        baseUrls.put("lr", getLrBaseUrl()) ;
        baseUrls.put("tr",getTrBaseUrl());
        baseUrls.put("pr", getPrBaseUrl());
        return baseUrls;
    }

    private String getTrBaseUrl() {
        Properties properties = getTrProperties();
        return getBaseUrl(properties.getProperty("tr.scheme"), properties.getProperty("tr.host"),
                properties.getProperty("tr.port"));
    }

    public String getMciBaseUrl() {
        Properties properties = getMciProperties();
        return getBaseUrl(properties.getProperty("mci.scheme"), properties.getProperty("mci.host"),
                properties.getProperty("mci.port"));
    }

    public String getShrBaseUrl() {
        Properties properties = getShrProperties();
        String shrUrl = getBaseUrl(properties.getProperty("shr.scheme"), properties.getProperty("shr.host"),
                properties.getProperty("shr.port"));
        String shrVersion = properties.getProperty("shr.version");

        return StringUtils.isEmpty(shrVersion)? shrUrl : String.format("%s/%s", shrUrl, shrVersion);
    }

    public String getLrBaseUrl() {
        Properties properties = getLrProperties();
        return getBaseUrl(properties.getProperty("lr.scheme"),
                properties.getProperty("lr.host"),
                properties.getProperty("lr.context"));
    }

    public String getFrBaseUrl() {
        Properties properties = getFrProperties();
        return getBaseUrl(properties.getProperty("fr.scheme"),
                properties.getProperty("fr.host"),
                properties.getProperty("fr.context"));
    }

    public String getPrBaseUrl() {
        Properties properties = getPrProperties();
        return getBaseUrl(properties.getProperty("pr.scheme"),
                properties.getProperty("pr.host"),
                properties.getProperty("pr.context"));
    }

    public String getIdentityServerBaseUrl() {
        Properties properties = getIdentityProperties();
        return getBaseUrl(properties.getProperty("idP.scheme"),
                properties.getProperty("idP.host"),
                properties.getProperty("idP.port"));
    }

    private String getBaseUrl(String scheme, String host, String portNoOrContextPath) {
        boolean isPortNo = portNoOrContextPath.matches(REGEX_TO_MATCH_PORT_NO);
        return isPortNo ? String.format("%s://%s" + URL_SEPARATOR_FOR_PORT_NO + "%s", scheme, host, portNoOrContextPath)
                : String.format("%s://%s" + URL_SEPARATOR_FOR_CONTEXT_PATH + "%s", scheme, host, portNoOrContextPath);
    }

    private Properties getProperties(String resource) {
        try {
            Properties properties = new Properties();
            final File file = new File(System.getProperty("user.home") + File.separator + ".OpenMRS" + File.separator + resource);
            final InputStream inputStream;
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            } else {
                inputStream = getClass().getClassLoader().getResourceAsStream(resource);
            }
            properties.load(inputStream);
            return properties;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
