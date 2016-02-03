package org.openmrs.module.shrclient.mapper;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.module.fhir.Constants;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.module.shrclient.model.Relation;

import java.util.*;

import static java.util.Arrays.asList;

public class RelationshipMapper {

    private static String RELATIONSHIP_FATHER_TYPE = "FTH";
    private static String RELATIONSHIP_SPS_TYPE = "SPS";

    private PersonAttributeMapper personAttributeMapper;

    private static Map<String, String> attributeRelationshipTypeMapping = new HashMap<String, String>() {{
        put(Constants.FATHER_NAME_ATTRIBUTE_TYPE, RELATIONSHIP_FATHER_TYPE);
        put(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE, RELATIONSHIP_SPS_TYPE);
    }};

    public RelationshipMapper() {
        this.personAttributeMapper = new PersonAttributeMapper();
    }

    public List<Relation> map(Patient openMrsPatient, IdMappingRepository idMappingRepository) {
        List<Relation> relations = new ArrayList<>();

        Set<String> attributeNames = attributeRelationshipTypeMapping.keySet();
        for (String attributeName : attributeNames) {
            PersonAttribute relationshipAttribute = openMrsPatient.getAttribute(attributeName);
            String attributeValue = relationshipAttribute != null ? relationshipAttribute.getValue() : null;
            if (StringUtils.isNotEmpty(attributeValue)) {
                Relation relation = getRelation(attributeValue, attributeRelationshipTypeMapping.get(attributeName));
                IdMapping idMapping = idMappingRepository.findByInternalId(getRelationInternalId(openMrsPatient, relationshipAttribute.getAttributeType().getUuid()),IdMappingType.PERSON_RELATION);
                if (idMapping == null) {
                    relation.setId(UUID.randomUUID().toString());
                    idMapping = new IdMapping(getRelationInternalId(openMrsPatient, relationshipAttribute.getAttributeType().getUuid()), relation.getId(), IdMappingType.PERSON_RELATION, null, new Date());
                    idMappingRepository.saveOrUpdateIdMapping(idMapping);
                } else {
                    relation.setId(idMapping.getExternalId());
                }
                relations.add(relation);
            }
        }
        return relations;
    }

    private String getRelationInternalId(Patient openMrsPatient, String attributeUuid) {
        return String.format("%s:%s", openMrsPatient.getUuid(), attributeUuid);
    }

    public void addRelationAttributes(Relation[] relations, Patient emrPatient, IdMappingRepository idMappingsRepository) {
        if (relations != null && relations.length > 0) {
            List<Relation> relationsToPersistAsAttributes = getRelationsToPersistAsAttributes(relations);
            for (Relation relationToPersistAsAttribute : relationsToPersistAsAttributes) {
                PersonAttribute relationAttribute = getPersonAttribute(relationToPersistAsAttribute, emrPatient, idMappingsRepository);
                if (relationAttribute != null) {
                    emrPatient.addAttribute(relationAttribute);
                }
            }
        }
    }

    private PersonAttribute getPersonAttribute(Relation relationToPersistAsAttribute, Patient patient, IdMappingRepository idMappingsRepository) {
        String attributeName = getAttributeName(relationToPersistAsAttribute.getType());
        String name = getName(relationToPersistAsAttribute);
        IdMapping idMapping = idMappingsRepository.findByExternalId(relationToPersistAsAttribute.getId(), IdMappingType.PERSON_RELATION);
        PersonAttribute personAttribute = personAttributeMapper.getAttribute(attributeName, name);
        if (idMapping == null) {
            String relationInternalId = getRelationInternalId(patient, personAttribute.getAttributeType().getUuid());
            idMapping = new IdMapping(relationInternalId, relationToPersistAsAttribute.getId(), IdMappingType.PERSON_RELATION, null, new Date());
            idMappingsRepository.saveOrUpdateIdMapping(idMapping);
        }
        return personAttribute;
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

    private String getAttributeName(String relationName) {
        for (String attributeName : attributeRelationshipTypeMapping.keySet()) {
            if (relationName.equals(attributeRelationshipTypeMapping.get(attributeName))) {
                return attributeName;
            }
        }
        return null;
    }

    private Relation getRelation(String relationshipValue, String relationshipType) {
        String givenName = StringUtils.trimToNull(StringUtils.substringBeforeLast(relationshipValue, " "));
        String surName = StringUtils.trimToNull(StringUtils.substringAfterLast(relationshipValue, " "));

        Relation relation = new Relation();
        relation.setType(relationshipType);
        relation.setGivenName(givenName);
        relation.setSurName(surName);
        return relation;
    }
}
