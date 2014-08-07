package org.openmrs.module.fhir.mapper.model;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceReference;

public class Resources {

    private AtomFeed feed;

    public Resources(AtomFeed feed) {
        this.feed = feed;
    }

    public Resource find(ResourceReference reference) {
        for (AtomEntry<? extends Resource> atomEntry : feed.getEntryList()) {
            if (StringUtils.equals(atomEntry.getId(), reference.getReferenceSimple())) {
                return atomEntry.getResource();
            }
        }
        return null;
    }
}
