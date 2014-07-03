package org.bahmni.module.shrclient.web.controller;

public class MciPatientSearchRequest {

    private String nid;

    @Override
    public String toString() {
        return "MciPatientDownloadRequest{" +
                "nid='" + nid + '\'' +
                ", hid='" + hid + '\'' +
                '}';
    }

    private String hid;

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
}
