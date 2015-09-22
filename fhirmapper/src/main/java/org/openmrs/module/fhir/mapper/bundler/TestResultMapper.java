package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.openmrs.Obs;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component("testResultMapper")
public class TestResultMapper implements EmrObsResourceHandler {

    @Autowired
    private ObservationMapper observationMapper;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private DiagnosticReportBuilder diagnosticReportBuilder;

    @Autowired
    private CodeableConceptService codeableConceptService;

    @Override
    public boolean canHandle(Obs observation) {
        return MRS_ENC_TYPE_LAB_RESULT.equals(observation.getEncounter().getEncounterType().getName());
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> FHIRResourceList = new ArrayList<>();
        if (obs != null) {
            if (!isPanel(obs)) {
                buildTestResult(obs, fhirEncounter, FHIRResourceList, systemProperties);
            } else {
                for (Obs observation : obs.getGroupMembers()) {
                    buildTestResult(observation, fhirEncounter, FHIRResourceList, systemProperties);
                }
            }
        }
        return FHIRResourceList;
    }

    private Boolean isPanel(Obs obs) {
        return obs.getConcept().getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET);
    }

    private void buildTestResult(Obs obs, Encounter fhirEncounter, List<FHIRResource> FHIRResourceList, SystemProperties systemProperties) {
        for (Obs observation : obs.getGroupMembers()) {
            DiagnosticReport diagnosticReport = build(observation, fhirEncounter, FHIRResourceList, systemProperties);
            if (diagnosticReport != null) {
                FHIRResource FHIRResource = new FHIRResource("Diagnostic Report", diagnosticReport.getIdentifier(), diagnosticReport);
                FHIRResourceList.add(FHIRResource);
            }
        }
    }

    private DiagnosticReport build(Obs obs, Encounter fhirEncounter, List<FHIRResource> fHIRResourceList, SystemProperties systemProperties) {
        DiagnosticReport report = diagnosticReportBuilder.build(obs, fhirEncounter, systemProperties);
        CodeableConceptDt name = codeableConceptService.addTRCoding(obs.getConcept(), idMappingsRepository);
        if (name.getCoding() != null && name.getCoding().isEmpty()) {
            return null;
        }
        report.setCode(name);
        org.openmrs.Order obsOrder = obs.getOrder();
        report.setEffective(getOrderTime(obsOrder));

        String uuid = obsOrder.getEncounter().getUuid();
        IdMapping encounterIdMapping = idMappingsRepository.findByInternalId(uuid);
        if (encounterIdMapping == null) {
            throw new RuntimeException("Encounter id [" + uuid + "] doesn't have id mapping.");
        }

        report.addRequest().setReference(encounterIdMapping.getUri());

        for (Obs member : obs.getGroupMembers()) {
            if (member.getConcept().equals(obs.getConcept())) {
                FHIRResource observationResource = observationMapper.mapObservation(member, fhirEncounter, systemProperties);
                Observation fhirObservation = (Observation) observationResource.getResource();
                fhirObservation.setStatus(ObservationStatusEnum.FINAL);
                ResourceReferenceDt resourceReference = report.addResult();
                resourceReference.setReference(observationResource.getIdentifier().getValue());
                fHIRResourceList.add(observationResource);
            } else if (MRS_CONCEPT_NAME_LAB_NOTES.equals(member.getConcept().getName().getName())) {
                report.setConclusion(member.getValueText());
            }
        }
        return report;
    }

    private DateTimeDt getOrderTime(org.openmrs.Order obsOrder) {
        DateTimeDt diagnostic = new DateTimeDt();
        diagnostic.setValue(obsOrder.getDateActivated(), TemporalPrecisionEnum.MILLI);
        return diagnostic;
    }
}
