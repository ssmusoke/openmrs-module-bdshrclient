package org.bahmni.module.shrclient.util;

import org.bahmni.module.shrclient.util.FreeShrClientProperties;
import org.junit.Test;


import static org.junit.Assert.*;

public class FreeShrClientPropertiesTest {

    @Test
    public void testGetMciUrl() {
        FreeShrClientProperties freeShrClientProperties = new FreeShrClientProperties();
        assertEquals("http://localhost:8089/patient", freeShrClientProperties.getMciBaseUrl());
    }
}