package org.openmrs.module.shrclient.util;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.shrclient.model.Address;

import java.util.List;

import static org.openmrs.module.shrclient.model.Address.getAddressCodeForLevel;
import static org.openmrs.module.shrclient.util.AddressLevel.*;

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

        String divisionId = getAddressHierarchyLevelUserGenId(Division, openMrsPersonAddress.getStateProvince());
        String districtId = getAddressHierarchyLevelUserGenId(Zilla, openMrsPersonAddress.getCountyDistrict());
        String upazillaId = getAddressHierarchyLevelUserGenId(Upazilla, openMrsPersonAddress.getAddress5());
        String cityCorporationId = getAddressHierarchyLevelUserGenId(Paurasava, openMrsPersonAddress.getAddress4());
        String wardOrUnionId = getAddressHierarchyLevelUserGenId(UnionOrWard, openMrsPersonAddress.getAddress3());
        String ruralWardId = getAddressHierarchyLevelUserGenId(RuralWard, openMrsPersonAddress.getAddress2());


        Address presentAddress = new Address(addressLine,
                getAddressCodeForLevel(divisionId, Division.getLevelNumber()),
                getAddressCodeForLevel(districtId, Zilla.getLevelNumber()),
                getAddressCodeForLevel(upazillaId, Upazilla.getLevelNumber()),
                getAddressCodeForLevel(cityCorporationId, Paurasava.getLevelNumber()),
                getAddressCodeForLevel(wardOrUnionId, UnionOrWard.getLevelNumber()),
                getAddressCodeForLevel(ruralWardId, RuralWard.getLevelNumber()),
                null);
        return presentAddress;
    }

    public Address toMciAddress(PersonAddress emrAddress) {
          AddressHierarchyEntry division = getAddressEntry(Division, emrAddress.getStateProvince(), null);
          AddressHierarchyEntry district = getAddressEntry(Zilla, emrAddress.getCountyDistrict(), division);
          AddressHierarchyEntry upazila = getAddressEntry(Upazilla, emrAddress.getAddress5(), district);
          AddressHierarchyEntry cityCorporation = getAddressEntry(Paurasava, emrAddress.getAddress4(), upazila);
          AddressHierarchyEntry wardOrUnion = getAddressEntry(UnionOrWard, emrAddress.getAddress3(), cityCorporation);
          AddressHierarchyEntry ruralWard = getAddressEntry(RuralWard, emrAddress.getAddress2(), wardOrUnion);
          String addressLine = emrAddress.getAddress1();

          Address presentAddress = new Address(addressLine,
                    getAddressCodeForLevel(division.getUserGeneratedId(), Division.getLevelNumber()),
                    getAddressCodeForLevel(district.getUserGeneratedId(), Zilla.getLevelNumber()),
                    getAddressCodeForLevel(upazila.getUserGeneratedId(), Upazilla.getLevelNumber()),
                    getAddressCodeForLevel(cityCorporation.getUserGeneratedId(), Paurasava.getLevelNumber()),
                    getAddressCodeForLevel(wardOrUnion.getUserGeneratedId(), UnionOrWard.getLevelNumber()),
                    getAddressCodeForLevel(ruralWard.getUserGeneratedId(), RuralWard.getLevelNumber()),
                    null);
          return presentAddress;
    }

    private AddressHierarchyEntry getAddressEntry(AddressLevel addressLevel, String name, AddressHierarchyEntry parent) {
        if (StringUtils.isNotBlank(name)) {
            AddressHierarchyLevel hierarchyLevel = getAddressHierarchyLevel(addressLevel);
            List<AddressHierarchyEntry> entries;
            if ((parent == null) && (addressLevel.getLevelNumber() ==1)) {
                entries = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(hierarchyLevel, name);
            } else {
                entries = addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(hierarchyLevel, name, parent);
            }
            return entries.isEmpty() ? null : entries.get(0);
        }
        return null;
    }


    private String getAddressHierarchyLevelUserGenId(AddressLevel level, String name) {
        if (StringUtils.isNotBlank(name)) {
            AddressHierarchyLevel hierarchyLevel = getAddressHierarchyLevel(level);
            List<AddressHierarchyEntry> entries = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(hierarchyLevel, name);
            return entries.isEmpty() ? null : entries.get(0).getUserGeneratedId();
        }
        return null;
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
