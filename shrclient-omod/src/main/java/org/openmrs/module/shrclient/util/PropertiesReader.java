package org.openmrs.module.shrclient.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component("bdshrPropertiesReader")
public class PropertiesReader {
    public RestClient getMciWebClient() {
        Properties properties = getMciProperties();
        return new RestClient(getMciBaseUrl(properties),
                properties.getProperty("mci.user"),
                properties.getProperty("mci.password")
                );
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

    private String getBaseUrl(String scheme, String host, String port) {
        return String.format("%s://%s:%s", scheme, host, port);
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
