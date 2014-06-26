package org.openmrs.module.bdshrclient.util;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FreeShrClientProperties {

    Properties properties;

    public FreeShrClientProperties() {
        this.properties = new Properties();
        try {
            final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("freeshrclient.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMciBaseUrl() {
        final String mciHost = properties.getProperty("mci.host");
        final String mciPort = properties.getProperty("mci.port");
        return String.format("http://%s:%s/patient", mciHost, mciPort);
    }

    public String getMciUser() {
        return properties.getProperty("mci.user");
    }

    public String getMciPassword() {
        return properties.getProperty("mci.password");
    }

}
