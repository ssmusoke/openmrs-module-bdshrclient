package org.openmrs.module.bdshrclient.util;

import org.junit.Test;


import static org.junit.Assert.*;

public class FreeShrClientPropertiesTest {

    @Test
    public void testGetMciUrl() {
        FreeShrClientProperties freeShrClientProperties = new FreeShrClientProperties();
        assertEquals("http://localhost:8089/patient", freeShrClientProperties.getMciBaseUrl());
    }
}