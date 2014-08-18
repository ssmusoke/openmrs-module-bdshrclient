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
        Properties properties = getProperties("mci.properties");
        return new RestClient(properties.getProperty("mci.user"),
                properties.getProperty("mci.password"),
                properties.getProperty("mci.host"),
                properties.getProperty("mci.port"));
    }

    public FhirRestClient getShrWebClient() {
        Properties properties = getProperties("shr.properties");
        return new FhirRestClient(properties.getProperty("shr.user"),
                properties.getProperty("shr.password"),
                properties.getProperty("shr.host"),
                properties.getProperty("shr.port"));
    }

    public Properties getProperties(String resource) {
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
