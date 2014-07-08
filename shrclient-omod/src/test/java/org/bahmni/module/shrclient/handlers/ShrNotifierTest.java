package org.bahmni.module.shrclient.handlers;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.Properties;

public class ShrNotifierTest {

    private File configDir;

    @Before
    public void setup() throws IOException, URISyntaxException {
        File src = new File(URLClassLoader.getSystemResource("shr-test.properties").toURI());
        configDir = new File(System.getProperty("user.home") + File.separator + "shr" + File.separator + "config");
        FileUtils.copyFile(src, new File (configDir, File.separator + "shr.properties"));
    }

    //is there any benefit from a test like this?
    public void shouldGetPropertiesFromLocation() {
        final Properties properties = new ShrNotifier().getProperties("shr.properties");
        Assert.assertEquals("shrhost", properties.getProperty("shr.host"));
        Assert.assertEquals("8383", properties.getProperty("shr.port"));
        Assert.assertEquals("scott", properties.getProperty("shr.user"));
        Assert.assertEquals("tiger", properties.getProperty("shr.password"));
    }

    //is there any benefit from a test like this?
    public void shouldGetPropertiesFromClasspath() {
        final Properties properties = new ShrNotifier().getProperties("mci.properties");
        Assert.assertEquals("localhost", properties.getProperty("mci.host"));
        Assert.assertEquals("8080", properties.getProperty("mci.port"));
        Assert.assertEquals("mci", properties.getProperty("mci.user"));
        Assert.assertEquals("password", properties.getProperty("mci.password"));
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(configDir);
    }

}