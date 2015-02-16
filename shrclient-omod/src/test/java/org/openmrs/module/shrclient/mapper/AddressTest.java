package org.openmrs.module.shrclient.mapper;

import org.junit.Test;
import org.openmrs.module.shrclient.model.Address;

import static org.junit.Assert.assertEquals;

public class AddressTest {
    @Test
    public void shouldGetCorrectAddressCode() {
        assertEquals("10", Address.getAddressCodeForLevel("1004092001", 1));
        assertEquals("04", Address.getAddressCodeForLevel("1004092001", 2));
        assertEquals("09", Address.getAddressCodeForLevel("1004092001", 3));
        assertEquals("20", Address.getAddressCodeForLevel("1004092001", 4));
        assertEquals("01", Address.getAddressCodeForLevel("1004092001", 5));
    }

}
