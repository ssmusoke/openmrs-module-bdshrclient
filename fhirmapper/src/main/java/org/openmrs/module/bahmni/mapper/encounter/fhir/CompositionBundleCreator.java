package org.openmrs.module.bahmni.mapper.encounter.fhir;

import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.DateAndTime;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CompositionBundleCreator {
    private static final Logger log = Logger.getLogger(CompositionBundleCreator.class);

    @Autowired
    EncounterMapper encounterMapper;

    @Autowired
    DiagnosisMapper diagnosisMapper;

    @Autowired
    ChiefComplaintMapper chiefComplaintMapper;


    public AtomFeed compose(org.openmrs.Encounter openMrsEncounter) {
        Encounter encounter = encounterMapper.map(openMrsEncounter);
        log.debug("Uploading patient encounter to SHR : [ " + encounter + "]");
        final List<Condition> diagConditionList = diagnosisMapper.map(openMrsEncounter, encounter);
        final List<Condition> chiefComplaintConditionList = chiefComplaintMapper.map(openMrsEncounter, encounter);
        Composition composition = createComposition(openMrsEncounter, encounter);
        addEncounterSection(encounter, composition);
        addConditionSections(diagConditionList, composition, "Diagnosis");
        addConditionSections(chiefComplaintConditionList, composition, "Complaint");

        AtomFeed atomFeed = new AtomFeed();
        addEntriesToDocument(atomFeed, composition, encounter, diagConditionList, chiefComplaintConditionList);
        return atomFeed;
    }

    private void addEntriesToDocument(AtomFeed atomFeed, Composition composition, Encounter encounter, List<Condition> diagConditionList, List<Condition> chiefComplaintConditionList) {
        atomFeed.setTitle("Encounter");
        atomFeed.setUpdated(composition.getDateSimple());
        atomFeed.setId(UUID.randomUUID().toString());

        AtomEntry compositionEntry = new AtomEntry();
        compositionEntry.setResource(composition);
        compositionEntry.setId(composition.getIdentifier().getValueSimple());
        compositionEntry.setTitle("Composition");
        atomFeed.addEntry(compositionEntry);

        AtomEntry encounterEntry = new AtomEntry();
        encounterEntry.setResource(encounter);
        encounterEntry.setId(encounter.getIndication().getReferenceSimple());
        encounterEntry.setTitle("Encounter");
        atomFeed.addEntry(encounterEntry);

        for (Condition condition : diagConditionList) {
            createNewAtomEntry(atomFeed, condition, "Diagnosis");
        }

        for (Condition condition : chiefComplaintConditionList) {
            createNewAtomEntry(atomFeed, condition, "Complaint");
        }
    }

    private void createNewAtomEntry(AtomFeed atomFeed, Condition condition, String title) {
        AtomEntry conditionEntry = new AtomEntry();
        conditionEntry.setId(condition.getIdentifier().get(0).getValueSimple());
        conditionEntry.setTitle(title);
        conditionEntry.setResource(condition);
        atomFeed.addEntry(conditionEntry);
    }

    private void addConditionSections(List<Condition> conditionList, Composition composition, String display) {
        for (Condition condition : conditionList) {
            List<Identifier> identifiers = condition.getIdentifier();
            String conditionUuid = identifiers.get(0).getValueSimple();
            ResourceReference conditionRef = new ResourceReference();
            conditionRef.setReferenceSimple(conditionUuid);
            conditionRef.setDisplaySimple(display);
            composition.addSection().setContent(conditionRef);
        }
    }

    private void addEncounterSection(Encounter encounter, Composition composition) {
        final Composition.SectionComponent encounterSection = composition.addSection();
        encounterSection.setContent(encounter.getIndication());
    }

    private Composition createComposition(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        DateAndTime encounterDateTime = new DateAndTime(openMrsEncounter.getEncounterDatetime());
        Composition composition = new Composition().setDateSimple(encounterDateTime);
        composition.setEncounter(encounter.getIndication());
        composition.setStatus(new Enumeration<Composition.CompositionStatus>(Composition.CompositionStatus.final_));
        composition.setIdentifier(new Identifier().setValueSimple("Encounter - " + openMrsEncounter.getUuid()));
        return composition;
    }

}
