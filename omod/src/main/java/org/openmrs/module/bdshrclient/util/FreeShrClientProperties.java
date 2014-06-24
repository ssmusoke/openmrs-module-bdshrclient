package org.openmrs.module.bdshrclient.util;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FreeShrClientProperties {

    Properties properties;

    public FreeShrClientProperties() throws IOException {
        this.properties = new Properties();
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("freeshrclient.properties");
        properties.load(inputStream);
    }

    public String getMciUrl() throws IOException {
        final String mciHost = properties.getProperty("mci.host");
        final String mciPort = properties.getProperty("mci.port");
        return String.format("http://%s:%s/patient", mciHost, mciPort);
    }

    public String getMciUser() throws IOException {
        return properties.getProperty("mci.user");
    }

    public String getMciPassword() throws IOException {
        return properties.getProperty("mci.password");
    }

}
