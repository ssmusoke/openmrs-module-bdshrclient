package org.openmrs.module.fhir.utils;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateUtilTest {

    @Test
    public void testIsLaterThan() throws Exception {
        Date firstDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS").parse("21-06-2015 15:30:20:123");
        Date secondDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS").parse("21-06-2015 15:30:20:245");
        assertFalse(DateUtil.isLaterThan(firstDate, secondDate));

        firstDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS").parse("21-06-2015 15:30:21:123");
        secondDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS").parse("21-06-2015 15:30:20:245");
        assertTrue(DateUtil.isLaterThan(firstDate, secondDate));

        firstDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS").parse("21-06-2015 15:30:19:123");
        secondDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS").parse("21-06-2015 15:30:20:245");
        assertFalse(DateUtil.isLaterThan(firstDate, secondDate));
    }
}