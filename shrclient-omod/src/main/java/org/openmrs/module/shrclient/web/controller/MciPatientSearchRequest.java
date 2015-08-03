package org.openmrs.module.shrclient.web.controller;

public class MciPatientSearchRequest {
    private String nid;
    private String hid;

    private String uid;
    private String brn;
    private String houseHoldCode;

    private String phoneNo;

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getHid() {
        return hid;
    }

    public void setHid(String hid) {
        this.hid = hid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getBrn() {
        return brn;
    }

    public void setBrn(String brn) {
        this.brn = brn;
    }

    public String getHouseHoldCode() {
        return houseHoldCode;
    }

    public void setHouseHoldCode(String houseHoldCode) {
        this.houseHoldCode = houseHoldCode;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    @Override
    public String toString() {
        return "MciPatientSearchRequest{" +
                "nid='" + nid + '\'' +
                ", hid='" + hid + '\'' +
                ", uid='" + uid + '\'' +
                ", brn='" + brn + '\'' +
                ", houseHoldCode='" + houseHoldCode + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                '}';
    }
}
