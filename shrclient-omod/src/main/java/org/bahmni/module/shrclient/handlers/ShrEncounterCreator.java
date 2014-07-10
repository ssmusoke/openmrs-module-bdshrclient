package org.bahmni.module.shrclient.handlers;

import org.apache.log4j.Logger;
import org.bahmni.module.shrclient.mapper.EncounterMapper;
import org.bahmni.module.shrclient.util.FhirRestClient;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.openmrs.*;
import org.openmrs.ConceptMap;
import org.openmrs.api.EncounterService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShrEncounterCreator implements EventWorker {

    private static final Logger log = Logger.getLogger(ShrEncounterCreator.class);
    private EncounterService encounterService;
    private EncounterMapper encounterMapper;
    private FhirRestClient fhirRestClient;
    private Map<String, String> severityCodes = new HashMap<String, String>();

    public ShrEncounterCreator(EncounterService encounterService, EncounterMapper encounterMapper, FhirRestClient fhirRestClient) {
        this.encounterService = encounterService;
        this.encounterMapper = encounterMapper;
        this.fhirRestClient = fhirRestClient;

        severityCodes.put("Moderate", "6736007");
        severityCodes.put("Severe", "24484000");
    }

    @Override
    public void process(Event event) {
        log.debug("Event: [" + event + "]");
        try {
            String uuid = getUuid(event.getContent());
            org.openmrs.Encounter openMrsEncounter = encounterService.getEncounterByUuid(uuid);
            if (openMrsEncounter == null) {
                log.debug(String.format("No OpenMRS encounter exists with uuid: [%s].", uuid));
                return;
            }
            Encounter encounter = encounterMapper.map(openMrsEncounter);
            log.debug("Encounter: [ " + encounter + "]");

            Composition composition = getComposition(openMrsEncounter, encounter);
            fhirRestClient.post("/encounter", composition);

        } catch (Exception e) {
            log.error("Error while processing patient sync event.", e);
            throw new RuntimeException(e);
        }
    }

    private List<Condition> getConditions(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        Set<Obs> allObs = openMrsEncounter.getAllObs(true);
        List<Condition> diagnoses = new ArrayList<Condition>();
        for (Obs obs : allObs) {
            if (obs.getConcept().getConceptClass().getName().equalsIgnoreCase("Diagnosis")) {
                Condition condition = new Condition();
                condition.setEncounter(encounter.getIndication());
                condition.setSubject(encounter.getSubject());
                condition.setAsserter(getParticipant(encounter));
                //TODO find diagnosis status by iterating through the obs
                condition.setStatus(new Enumeration<Condition.ConditionStatus>(Condition.ConditionStatus.confirmed));
                condition.setCategory(getDiagnosisCategory());
                condition.setSeverity(getDiagnosisSeverity());
                DateTime onsetDate = new DateTime();
                onsetDate.setValue(new DateAndTime(obs.getObsDatetime()));
                condition.setOnset(onsetDate);
                condition.setCode(getDiagnosisCode(obs));
                diagnoses.add(condition);
            }
        }
        return diagnoses;
    }

    private CodeableConcept getDiagnosisCode(Obs obs) {
        CodeableConcept diagnosisCode = new CodeableConcept();
        Coding coding = diagnosisCode.addCoding();
        //TODO to change to reference term code
        Concept obsConcept = obs.getConcept();
        coding.setCodeSimple(getReferenceCode(obsConcept));
        //TODO: put in the right URL. To be mapped
        coding.setSystemSimple("http://192.168.33.18/openmrs/ws/rest/v1/concept/" + obsConcept.getUuid());
        coding.setDisplaySimple(obsConcept.getDisplayString());
        return diagnosisCode;
    }

    private String getReferenceCode(Concept obsConcept) {
        Collection<org.openmrs.ConceptMap> conceptMappings = obsConcept.getConceptMappings();
        for (ConceptMap mapping : conceptMappings) {
            return mapping.getConceptReferenceTerm().getCode();
        }
        return obsConcept.getUuid();
    }

    private CodeableConcept getDiagnosisSeverity() {
        CodeableConcept conditionSeverity = new CodeableConcept();
        Coding coding = conditionSeverity.addCoding();
        //TODO map from bahmni severity order
        coding.setCodeSimple(severityCodes.get("Moderate"));
        coding.setSystemSimple("http://hl7.org/fhir/vs/condition-severity");
        coding.setDisplaySimple("Moderate");
        return conditionSeverity;
    }

    private CodeableConcept getDiagnosisCategory() {
        CodeableConcept conditionCategory = new CodeableConcept();
        Coding coding = conditionCategory.addCoding();
        coding.setCodeSimple("diagnosis");
        coding.setSystemSimple("http://hl7.org/fhir/vs/condition-category");
        coding.setDisplaySimple("diagnosis");
        return conditionCategory;
    }

    private ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) || participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private Composition getComposition(org.openmrs.Encounter openMrsEncounter, Encounter encounter) {
        DateAndTime encounterDateTime = new DateAndTime(openMrsEncounter.getEncounterDatetime());
        Composition composition = new Composition().setDateSimple(encounterDateTime);
        composition.setEncounter(encounter.getIndication());
        composition.setStatus(new Enumeration<Composition.CompositionStatus>(Composition.CompositionStatus.final_));
        composition.setIdentifier(new Identifier().setValueSimple("Encounter - " + openMrsEncounter.getUuid()));
        return composition;
    }

    Encounter populateEncounter(org.openmrs.Encounter openMrsEncounter) {
        return new Encounter();
    }

    String getUuid(String content) {
        String patientUuid = null;
        Pattern p = Pattern.compile("^\\/openmrs\\/ws\\/rest\\/v1\\/encounter\\/(.*)\\?v=.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            patientUuid = m.group(1);
        }
        return patientUuid;
    }

    @Override
    public void cleanUp(Event event) {
    }
}
