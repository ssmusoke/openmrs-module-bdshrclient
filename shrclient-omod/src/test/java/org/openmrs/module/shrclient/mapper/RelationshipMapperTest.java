package org.openmrs.module.shrclient.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.model.Relation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openmrs.module.fhir.utils.Constants.FATHER_NAME_ATTRIBUTE_TYPE;
import static org.openmrs.module.fhir.utils.Constants.SPOUSE_NAME_ATTRIBUTE_TYPE;

public class RelationshipMapperTest {

    @Mock
    PersonService personService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        when(personService.getPersonAttributeTypeByName(Constants.FATHER_NAME_ATTRIBUTE_TYPE)).thenReturn(getAttributeType(Constants.FATHER_NAME_ATTRIBUTE_TYPE));
        when(personService.getPersonAttributeTypeByName(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE)).thenReturn(getAttributeType(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE));

        Context context = new Context();
        ServiceContext serviceContext = ServiceContext.getInstance();
        serviceContext.setService(PersonService.class, personService);
        context.setServiceContext(serviceContext);

    }

    private PersonAttributeType getAttributeType(String attributeType) {
        PersonAttributeType type = new PersonAttributeType();
        type.setName(attributeType);
        return type;
    }

    @Test
    public void shouldMapGivenNameSurnameAndTypeOfARelation() throws Exception {
        org.openmrs.Patient patientWithFather = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "GivenName SurName");
        }}));

        List<Relation> relations = new RelationshipMapper().map(patientWithFather);
        assertEquals(1,relations.size());
        assertRelation(relations.get(0), "GivenName", "SurName", "FTH");

    }

    @Test
    public void shouldMapAllRelations() throws Exception {
        org.openmrs.Patient patientWithSpouseAndFather = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "GivenName SurName");
            put(SPOUSE_NAME_ATTRIBUTE_TYPE, "GivenSpouseName SurNameOfSpouse");
        }}));

        List<Relation> relations = new RelationshipMapper().map(patientWithSpouseAndFather);
        assertEquals(2,relations.size());
        assertRelation(relations.get(0), "GivenName", "SurName", "FTH");
        assertRelation(relations.get(1), "GivenSpouseName", "SurNameOfSpouse", "SPS");

    }

    @Test
    public void shouldMapWhenNoRelationsArePresent() throws Exception {
        org.openmrs.Patient orphanPatient = new org.openmrs.Patient();

        List<Relation> relations = new RelationshipMapper().map(orphanPatient);
        assertEquals(0, relations.size());

    }

    @Test
    public void shouldMapGivenNameWhenThereIsOnlyOneNameProvided() throws Exception {
        org.openmrs.Patient patientWithOneName = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "OneName");
        }}));

        List<Relation> relations = new RelationshipMapper().map(patientWithOneName);
        assertEquals(1,relations.size());
        assertRelation(relations.get(0), "OneName", null, "FTH");

    }

    @Test
    public void shouldMapSurnameWithLastWordWhenNameHasMultipleWords() throws Exception {
        org.openmrs.Patient patientWithOneName = getPatient(createOpenMrsPersonAttributes(new HashMap<String, String>(){{
            put(FATHER_NAME_ATTRIBUTE_TYPE, "One Two Three Four         LastName");
        }}));

        List<Relation> relations = new RelationshipMapper().map(patientWithOneName);
        assertEquals(1,relations.size());
        assertRelation(relations.get(0), "One Two Three Four", "LastName", "FTH");

    }

    @Test
    public void shouldNotMapRelationsIfNotPresent() throws Exception {
        org.openmrs.module.shrclient.model.Patient orphanPatient = getPatientFromJson("patients_response/by_hid.json");
        assertEquals(0, new RelationshipMapper().map(orphanPatient.getRelations()).size());
    }

    @Test
    public void shouldMapOnlyFatherAndSpouseRelations() throws Exception {

        org.openmrs.module.shrclient.model.Patient patientWithOtherRelations = getPatientFromJson("patients_response/patientWithRelations.json");
        Set<PersonAttribute> relationAttributes = new RelationshipMapper().map(patientWithOtherRelations.getRelations());
        assertEquals(2, relationAttributes.size());

        PersonAttribute father = (PersonAttribute) CollectionUtils.find(relationAttributes, new BeanPropertyValueEqualsPredicate("attributeType.name", Constants.FATHER_NAME_ATTRIBUTE_TYPE));
        assertEquals("Md. Sakib Ali Khan", father.getValue());

        PersonAttribute spouse = (PersonAttribute) CollectionUtils.find(relationAttributes, new BeanPropertyValueEqualsPredicate("attributeType.name", Constants.SPOUSE_NAME_ATTRIBUTE_TYPE));
        assertEquals("Azad", spouse.getValue());

    }

    @Test
    public void shouldMapOnlyRelationPresent() throws Exception {
        org.openmrs.module.shrclient.model.Patient patientWithOtherRelations = getPatientFromJson("patients_response/patientWithFather.json");
        Set<PersonAttribute> relationAttributes = new RelationshipMapper().map(patientWithOtherRelations.getRelations());
        assertEquals(1, relationAttributes.size());

        PersonAttribute father = (PersonAttribute) CollectionUtils.find(relationAttributes, new BeanPropertyValueEqualsPredicate("attributeType.name", Constants.FATHER_NAME_ATTRIBUTE_TYPE));
        assertEquals("Md. Sakib Ali Khan", father.getValue());

        PersonAttribute spouse = (PersonAttribute) CollectionUtils.find(relationAttributes, new BeanPropertyValueEqualsPredicate("attributeType.name", Constants.SPOUSE_NAME_ATTRIBUTE_TYPE));
        assertNull(spouse);

    }

    private void assertRelation(Relation relation, String givenName, String surName, String type) {
        assertEquals(givenName, relation.getGivenName());
        assertEquals(surName, relation.getSurName());
        assertEquals(type, relation.getType());
    }

    private org.openmrs.module.shrclient.model.Patient getPatientFromJson(String patientJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        URL resource = URLClassLoader.getSystemResource(patientJson);
        final String patientResponse = FileUtils.readFileToString(new File(resource.getPath()));

        return mapper.readValue(patientResponse, org.openmrs.module.shrclient.model.Patient.class);
    }

    private Set<PersonAttribute> createOpenMrsPersonAttributes(Map<String, String> attributeValueMap) {
        Set<PersonAttribute> attributes = new HashSet<>();
        for (String attributeTypeName : attributeValueMap.keySet()) {
            PersonAttributeType attributeType = getAttributeType(attributeTypeName);
            attributes.add(new PersonAttribute(attributeType, attributeValueMap.get(attributeTypeName)));
        }
        return attributes;
    }

    private Patient getPatient(Set<PersonAttribute> attributes) {
        Patient openMrsPatient = new Patient();
        openMrsPatient.setAttributes(attributes);
        return openMrsPatient;
    }
}