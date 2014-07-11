package org.bahmni.module.shrclient.mapper;

import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.*;
import org.junit.Test;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIREncounterMapperIntegrationTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    ApplicationContext springContext;
    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    public AtomFeed loadSampleFHIREncounter() throws Exception {
        Resource resource = springContext.getResource("classpath:testFHIREncounter.json");
        final ParserBase.ResourceOrFeed parsedResource =
                new JsonParser().parseGeneral(resource.getInputStream());
        return parsedResource.getFeed();
    }

    @Test
    public void shouldMapFhirEncounter() throws Exception {
        executeDataSet("shrClientEncounterReverseSyncTestDS.xml");
        final AtomFeed encounterBundle = loadSampleFHIREncounter();
        assertEquals("dc1f5f99-fb2f-4ba8-bf24-14ccdee498f9", encounterBundle.getId());
        final List<AtomEntry<? extends org.hl7.fhir.instance.model.Resource>> encounterBundleEntryList = encounterBundle.getEntryList();
        final Composition composition = (Composition) identifyComposition(encounterBundleEntryList);
        assertNotNull(composition);

        assertEquals("2014-07-10T16:05:09+05:30", composition.getDateSimple().toString());
        final Encounter encounter = (Encounter) identifyEncounter(encounterBundleEntryList);
        assertNotNull(encounter);
        assertEquals("26504add-2d96-44d0-a2f6-d849dc090254", encounter.getIndication().getReferenceSimple());

        org.openmrs.Encounter emrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString());

        assertNotNull(emrEncounter);
        assertEquals("26504add-2d96-44d0-a2f6-d849dc090254", emrEncounter.getUuid());
        assertNotNull(emrEncounter.getEncounterDatetime());
        assertNotNull(emrEncounter.getEncounterType());

        assertNotNull(emrEncounter.getVisit());
//        Assert.assertNotNull(emrEncounter.getVisit());
//        Assert.assertNotNull(emrEncounter.getEncounterDatetime());
//        Assert.assertEquals("uuid", emrEncounter.getUuid());
//        Assert.assertNotNull(emrEncounter.getEncounterProviders());
//        Assert.assertNotNull(emrEncounter.getEncounterType());
    }



    private org.hl7.fhir.instance.model.Resource identifyEncounter(List<AtomEntry<? extends org.hl7.fhir.instance.model.Resource>> encounterBundleEntryList) {
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if(atomEntry.getResource().getResourceType().equals(ResourceType.Encounter)) {
                return atomEntry.getResource();
            }
        }
        return null;
    }

    private org.hl7.fhir.instance.model.Resource identifyComposition(List<AtomEntry<? extends org.hl7.fhir.instance.model.Resource>> encounterBundleEntryList) {
        for (AtomEntry<? extends org.hl7.fhir.instance.model.Resource> atomEntry : encounterBundleEntryList) {
            if (atomEntry.getResource().getResourceType().equals(ResourceType.Composition)) {
                return atomEntry.getResource();
            }
        }
        return null;
    }

    private Encounter createFhirEncounter() {
        Encounter encounter = new Encounter();
        final String encounterUuid = "uuid";
        encounter.setIndication(new ResourceReference().setReferenceSimple(encounterUuid));
        encounter.setServiceProvider(new ResourceReference().setReferenceSimple("provider-uuid"));
        encounter.setStatus(new Enumeration<Encounter.EncounterState>(Encounter.EncounterState.finished));
        encounter.setClass_(new Enumeration<Encounter.EncounterClass>(Encounter.EncounterClass.outpatient));
        encounter.setSubject(new ResourceReference().setReferenceSimple("health-id"));
        encounter.addParticipant().setIndividual(new ResourceReference().setReferenceSimple("provider-uuid"));
        encounter.setServiceProvider(new ResourceReference().setReferenceSimple("Bahmni-1"));
        encounter.addIdentifier().setValueSimple(encounterUuid);
        encounter.addType().setTextSimple("OPD");
        return encounter;
    }
}
