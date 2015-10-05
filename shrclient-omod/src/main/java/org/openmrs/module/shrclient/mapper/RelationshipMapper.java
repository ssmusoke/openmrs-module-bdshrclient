package org.openmrs.module.shrclient.mapper;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.PersonAttribute;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.model.Relation;

import java.util.*;

import static java.util.Arrays.asList;

public class RelationshipMapper {

    private static String RELATIONSHIP_FATHER_TYPE = "FTH";
    private static String RELATIONSHIP_SPS_TYPE = "SPS";
    private PersonAttributeMapper personAttributeMapper;


    public RelationshipMapper() {
        this.personAttributeMapper = new PersonAttributeMapper();
    }

    public List<Relation> map(org.openmrs.Patient openMrsPatient) {

        List<Relation> relations = new ArrayList<>();

        Set<String> attributeNames = attributeRelationshipTypeMapping.keySet();
        for (String attributeName : attributeNames) {
            String attributeValue = personAttributeMapper.getAttributeValue(openMrsPatient, attributeName);
            if (StringUtils.isNotEmpty(attributeValue)) {
                relations.add(getRelation(attributeValue, attributeRelationshipTypeMapping.get(attributeName)));
            }

        }
        return relations;
    }

    public  Set<PersonAttribute> map(Relation[] relations) {
        Set<PersonAttribute> relationAttributes = new TreeSet<>();
        if (relations != null && relations.length > 0) {
            List<Relation> relationsToPersistAsAttributes = getRelationsToPersistAsAttributes(relations);
            for (Relation relationToPersistAsAttribute : relationsToPersistAsAttributes) {
                PersonAttribute relationAttribute = getPersonAttribute(relationToPersistAsAttribute);
                if(relationAttribute != null){
                    relationAttributes.add(relationAttribute);
                }
            }
        }
        return relationAttributes;
    }

    private PersonAttribute getPersonAttribute(Relation relationToPersistAsAttribute) {
       return  personAttributeMapper.getAttribute(getAttributeName(relationToPersistAsAttribute.getType()), getName(relationToPersistAsAttribute));
    }

    private String getName(Relation relation) {
        return StringUtils.trim(String.format("%s %s", StringUtils.trimToEmpty(relation.getGivenName()), StringUtils.trimToEmpty(relation.getSurName())));
    }

    private static List<Relation> getRelationsToPersistAsAttributes(Relation[] relations) {
        List<Relation> relationList = asList(relations);
        List<Relation> relationsToPersistAsAttributes = new ArrayList<>();
        relationsToPersistAsAttributes.addAll(CollectionUtils.select(relationList, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                Relation relation = (Relation) o;
                return attributeRelationshipTypeMapping.values().contains(relation.getType());
            }
        }));

        return relationsToPersistAsAttributes;

    }

    private static Map<String, String> attributeRelationshipTypeMapping = new HashMap<String, String>() {{
        put(Constants.FATHER_NAME_ATTRIBUTE_TYPE, RELATIONSHIP_FATHER_TYPE);
        put(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE, RELATIONSHIP_SPS_TYPE);
    }};

    private String getAttributeName(String relationName){
        for (String attributeName : attributeRelationshipTypeMapping.keySet()) {
            if(relationName.equals(attributeRelationshipTypeMapping.get(attributeName))){
                return attributeName;
            }
        }
        return null;
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
