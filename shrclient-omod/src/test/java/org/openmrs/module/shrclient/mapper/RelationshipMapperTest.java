package org.openmrs.module.shrclient.mapper;

import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.shrclient.model.Relation;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.openmrs.module.fhir.utils.Constants.FATHER_NAME_ATTRIBUTE_TYPE;
import static org.openmrs.module.fhir.utils.Constants.SPOUSE_NAME_ATTRIBUTE_TYPE;

public class RelationshipMapperTest {

    @Test
    public void shouldMapGivenNameSurnameAndTypeOfARelation() throws Exception {
        org.openmrs.Patient patientWithFather = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "GivenName SurName");
        }}));

        List<Relation> relations = RelationshipMapper.map(patientWithFather);
        assertEquals(1,relations.size());
        assertRelation(relations.get(0), "GivenName", "SurName", "FTH");

    }

    @Test
    public void shouldMapAllRelations() throws Exception {
        org.openmrs.Patient patientWithSpouseAndFather = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "GivenName SurName");
            put(SPOUSE_NAME_ATTRIBUTE_TYPE, "GivenSpouseName SurNameOfSpouse");
        }}));

        List<Relation> relations = RelationshipMapper.map(patientWithSpouseAndFather);
        assertEquals(2,relations.size());
        assertRelation(relations.get(0), "GivenName", "SurName", "FTH");
        assertRelation(relations.get(1), "GivenSpouseName", "SurNameOfSpouse", "SPS");

    }

    @Test
    public void shouldMapWhenNoRelationsArePresent() throws Exception {
        org.openmrs.Patient orphanPatient = new org.openmrs.Patient();

        List<Relation> relations = RelationshipMapper.map(orphanPatient);
        assertEquals(0, relations.size());

    }

    @Test
    public void shouldMapGivenNameWhenThereIsOnlyOneNameProvided() throws Exception {
        org.openmrs.Patient patientWithOneName = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "OneName");
        }}));

        List<Relation> relations = RelationshipMapper.map(patientWithOneName);
        assertEquals(1,relations.size());
        assertRelation(relations.get(0), "OneName", null, "FTH");

    }

    @Test
    public void shouldMapSurnameWithLastWordWhenNameHasMultipleWords() throws Exception {
        org.openmrs.Patient patientWithOneName = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "One Two Three Four         LastName");
        }}));

        List<Relation> relations = RelationshipMapper.map(patientWithOneName);
        assertEquals(1,relations.size());
        assertRelation(relations.get(0), "One Two Three Four", "LastName", "FTH");

    }

    private void assertRelation(Relation relation, String givenName, String surName, String type) {
        assertEquals(givenName, relation.getGivenName());
        assertEquals(surName, relation.getSurName());
        assertEquals(type, relation.getType());
    }


    private Set<PersonAttribute> createOpenMrsPersonAttributes(Map<String, String> attributeValueMap) {
        Set<PersonAttribute> attributes = new HashSet<>();
        for (String attributeName : attributeValueMap.keySet()) {
            PersonAttributeType attributeType = new PersonAttributeType();
            attributeType.setName(attributeName);
            attributes.add(new PersonAttribute(attributeType, attributeValueMap.get(attributeName)));
        }
        return attributes;
    }

    private Patient getPatient(Set<PersonAttribute> attributes) {
        Patient openMrsPatient = new Patient();
        openMrsPatient.setAttributes(attributes);
        return openMrsPatient;
    }
}