package org.openmrs.module.shrclient.mapper;

import org.junit.Test;
import org.openmrs.module.shrclient.model.Address;

public class AddressMapperTest {
    @Test
    public void shouldGetCorrectAddressCode() {
        System.out.println("Division:" + Address.getAddressCodeForLevel("1004092001", 1));
        System.out.println("District:" + Address.getAddressCodeForLevel("1004092001", 2));
        System.out.println("Upazila:" + Address.getAddressCodeForLevel("1004092001", 3));

    }

}
