package org.openmrs.module.shrclient.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


@Component("bdshrPropertiesReader")
public class PropertiesReader {

    public static final String REGEX_TO_MATCH_PORT_NO = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
    public static final String URL_SEPARATOR_FOR_CONTEXT_PATH = "/";
    public static final String URL_SEPARATOR_FOR_PORT_NO = ":";


    public RestClient getMciWebClient() {
        Properties properties = getMciProperties();
        return new RestClient(getMciBaseUrl(properties),
                properties.getProperty("mci.user"),
                properties.getProperty("mci.password")
        );
    }

    public RestClient getLrWebClient() {
        Properties properties = getLrProperties();
        return new RestClient(getLrBaseUrl(properties), "", "");
    }

    private String getLrBaseUrl(Properties properties) {
        return getBaseUrl(properties.getProperty("lr.scheme"), properties.getProperty("lr.host"), properties.getProperty("lr.context"));
    }

    public Properties getLrProperties() {
        return getProperties("lr.properties");
    }

    public String getMciBaseUrl(Properties properties) {
        return getBaseUrl(properties.getProperty("mci.scheme"), properties.getProperty("mci.host"),
                properties.getProperty("mci.port"));
    }

    public FhirRestClient getShrWebClient() {
        Properties properties = getShrProperties();
        return new FhirRestClient(getShrBaseUrl(properties),
                properties.getProperty("shr.user"),
                properties.getProperty("shr.password")
        );
    }

    public String getShrBaseUrl(Properties properties) {
        return getBaseUrl(properties.getProperty("shr.scheme"),
                properties.getProperty("shr.host"),
                properties.getProperty("shr.port"));
    }

    private String getBaseUrl(String scheme, String host, String portOrContext) {
        boolean isPortNo = portOrContext.matches(REGEX_TO_MATCH_PORT_NO);
        return isPortNo ? String.format("%s://%s" + URL_SEPARATOR_FOR_PORT_NO + "%s", scheme, host, portOrContext)
                : String.format("%s://%s" + URL_SEPARATOR_FOR_CONTEXT_PATH + "%s", scheme, host, portOrContext);
    }

    public Properties getShrProperties() {
        return getProperties("shr.properties");
    }

    public Properties getMciProperties() {
        return getProperties("mci.properties");
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
