package org.openmrs.module.bdshrclient.util;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MciProperties {
    Properties properties = new Properties();
    public void loadProperties() throws IOException {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("freeshrclient.properties");
        properties.clear();
        properties.load(inputStream);
    }

    public String getMciPatientBaseURL() {
        final String mciHost = properties.getProperty("mci.host");
        final String mciPort = properties.getProperty("mci.port");
        return String.format("http://%s:%s/patient", mciHost, mciPort);
    }

}
