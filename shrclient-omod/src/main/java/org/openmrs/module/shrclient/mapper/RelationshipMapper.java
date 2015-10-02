package org.openmrs.module.shrclient.mapper;


import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.model.Relation;

import java.util.*;

public class RelationshipMapper {

    private static String RELATIONSHIP_FATHER_TYPE = "FTH";
    private static String RELATIONSHIP_SPS_TYPE = "SPS";

    private static Map<String, String> attributeRelationshipTypeMapping = new HashMap<String, String>(){{
       put(Constants.FATHER_NAME_ATTRIBUTE_TYPE, RELATIONSHIP_FATHER_TYPE);
       put(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE, RELATIONSHIP_SPS_TYPE);
    }};

    public static List<Relation> map(org.openmrs.Patient openMrsPatient) {

        List<Relation> relations = new ArrayList<>();

        Set<String> attributeNames = attributeRelationshipTypeMapping.keySet();
        for (String attributeName : attributeNames) {
            String attributeValue = PersonAttributeMapper.getAttributeValue(openMrsPatient, attributeName);
            if (StringUtils.isNotEmpty(attributeValue)) {
                relations.add(getRelation(attributeValue, attributeRelationshipTypeMapping.get(attributeName)));
            }

        }
        return relations;
    }

    private static Relation getRelation(String relationshipValue, String relationshipType) {
        String givenName = StringUtils.trimToNull(StringUtils.substringBeforeLast(relationshipValue, " "));
        String surName = StringUtils.trimToNull(StringUtils.substringAfterLast(relationshipValue, " "));

        Relation relation = new Relation();
        relation.setType(relationshipType);
        relation.setGivenName(givenName);
        relation.setSurName(surName);
        return relation;
    }


}
