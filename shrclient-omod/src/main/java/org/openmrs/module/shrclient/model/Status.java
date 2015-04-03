package org.openmrs.module.shrclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Status {
    @JsonProperty("type")
    private Character type;

    @JsonProperty("date_of_death")
    private String dateOfDeath;

    public Character getType() {
        return type;
    }

    public void setType(Character type) {
        this.type = type;
    }

    public String getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(String dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Status)) return false;

        Status status1 = (Status) o;

        if (!type.equals(status1.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Status{");
        sb.append("status=").append(type);
        sb.append(", dateOfDeath='").append(dateOfDeath).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
