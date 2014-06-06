package org.openmrs.module.bdshrclient.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Properties;


import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MciPropertiesTest {

    @Mock
    Properties properties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

    }

    @Test
    public void testGetMciPatientBaseURL() throws Exception {
        MciProperties mciProperties = new MciProperties(properties);
        when(properties.getProperty("mci.host")).thenReturn("localhost");
        when(properties.getProperty("mci.port")).thenReturn("8080");
        assertEquals("http://localhost:8080/patient", mciProperties.getMciPatientBaseURL());

    }
}