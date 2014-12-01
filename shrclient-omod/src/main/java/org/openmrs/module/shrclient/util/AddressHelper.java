package org.openmrs.module.shrclient.util;

import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.mci.api.model.Address;

import static org.openmrs.module.shrclient.mci.api.model.Address.getAddressCodeForLevel;
import static org.openmrs.module.shrclient.util.AddressLevel.*;
import static org.openmrs.module.shrclient.util.AddressLevel.Division;
import static org.openmrs.module.shrclient.util.AddressLevel.LOCATION_LEVELS;

public class AddressHelper {

    private AddressHierarchyService addressHierarchyService;


    public AddressHelper() {
        this.addressHierarchyService = Context.getService(AddressHierarchyService.class);
    }

    public AddressHelper(AddressHierarchyService addressHierarchyService) {
        this.addressHierarchyService = addressHierarchyService;
    }

    public AddressHierarchyLevel getAddressHierarchyLevelFromLrLevel(String lrLevel) {
        AddressLevel addressLevel = LOCATION_LEVELS.get(lrLevel);
        return getAddressHierarchyLevel(addressLevel);
    }

    public AddressHierarchyLevel getAddressHierarchyLevel(AddressLevel addressLevel) {
        AddressField addressField = addressLevel.getAddressField();
        return addressHierarchyService.getAddressHierarchyLevelByAddressField(addressField);
    }

    public Address getMciAddress(org.openmrs.Patient openMrsPatient) {
        PersonAddress openMrsPersonAddress = openMrsPatient.getPersonAddress();
        String addressLine = openMrsPersonAddress.getAddress1();
        String division = openMrsPersonAddress.getStateProvince();
        String district = openMrsPersonAddress.getCountyDistrict();
        String upazilla = openMrsPersonAddress.getAddress5();
        String cityCorporation = openMrsPersonAddress.getAddress4();
        String wardOrUnion = openMrsPersonAddress.getAddress3();
        String ruralWard = openMrsPersonAddress.getAddress2();

        String divisionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(getAddressHierarchyLevel(Division), division).get(0).getUserGeneratedId();
        String districtId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(getAddressHierarchyLevel(Zilla), district).get(0).getUserGeneratedId();
        String upazillaId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(getAddressHierarchyLevel(Upazilla), upazilla).get(0).getUserGeneratedId();
        String cityCorporationId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(getAddressHierarchyLevel(Paurasava), cityCorporation).get(0).getUserGeneratedId();
        String wardOrUnionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(getAddressHierarchyLevel(UnionOrWard), wardOrUnion).get(0).getUserGeneratedId();
        String ruralWardId = null;
        if(ruralWard != null) {
            ruralWardId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(getAddressHierarchyLevel(RuralWard), ruralWard).get(0).getUserGeneratedId();
        }

        Address presentAddress = new Address(addressLine,
                getAddressCodeForLevel(divisionId, Division.getLevelNumber()),
                getAddressCodeForLevel(districtId, Zilla.getLevelNumber()),
                getAddressCodeForLevel(upazillaId, Upazilla.getLevelNumber()),
                getAddressCodeForLevel(cityCorporationId, Paurasava.getLevelNumber()),
                getAddressCodeForLevel(wardOrUnionId, UnionOrWard.getLevelNumber()),
                ruralWardId != null ? getAddressCodeForLevel(ruralWardId, RuralWard.getLevelNumber()) : null,
                null);
        return presentAddress;
    }

    public PersonAddress setPersonAddress(PersonAddress emrPatientAddress, Address address) {
        if (emrPatientAddress == null) {
            emrPatientAddress = new PersonAddress();
        }

        AddressHierarchyEntry division = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getDivisionId());
        if (division != null) {
            emrPatientAddress.setStateProvince(division.getName());
        }
        AddressHierarchyEntry district = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedDistrictId());
        if (district != null) {
            emrPatientAddress.setCountyDistrict(district.getName());
        }
        AddressHierarchyEntry upazilla = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedUpazillaId());
        if (upazilla != null) {
            emrPatientAddress.setAddress5(upazilla.getName());
        }
        AddressHierarchyEntry cityCorporation = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedCityCorporationId());
        if (cityCorporation != null) {
            emrPatientAddress.setAddress4(cityCorporation.getName());
        }
        AddressHierarchyEntry urbanWardOrUnion = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedUnionOrUrbanWardId());
        if (urbanWardOrUnion != null) {
            emrPatientAddress.setAddress3(urbanWardOrUnion.getName());
        }
        if(address.getRuralWardId() != null) {
            AddressHierarchyEntry ruralWard = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.createUserGeneratedRuralWardId());
            if (ruralWard != null) {
                emrPatientAddress.setAddress2(ruralWard.getName());
            }
        }
        if (!"".equals(address.getAddressLine())) {
            emrPatientAddress.setAddress1(address.getAddressLine());
        }

        emrPatientAddress.setPreferred(true);
        return emrPatientAddress;
    }
}
