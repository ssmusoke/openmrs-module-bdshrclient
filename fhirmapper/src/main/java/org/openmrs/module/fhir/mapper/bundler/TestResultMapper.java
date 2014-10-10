package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Obs;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hl7.fhir.instance.model.DiagnosticReport.DiagnosticReportStatus;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_LAB_NOTES;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_ENC_TYPE_LAB_RESULT;

@Component("testResultMapper")
public class TestResultMapper implements EmrObsResourceHandler {

    @Autowired
    private ObservationMapper observationMapper;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Override
    public boolean handles(Obs observation) {
        return MRS_ENC_TYPE_LAB_RESULT.equals(observation.getEncounter().getEncounterType().getName());
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> emrResourceList = new ArrayList<EmrResource>();
        if (obs != null) {
            for (Obs observation : obs.getGroupMembers()) {
                DiagnosticReport diagnosticReport = build(observation, fhirEncounter, emrResourceList);
                EmrResource emrResource = new EmrResource("Diagnostic Report", Arrays.asList(diagnosticReport.getIdentifier()), diagnosticReport);
                emrResourceList.add(emrResource);
            }
        }
        return emrResourceList;
    }

    private DiagnosticReport build(Obs obs, Encounter fhirEncounter, List<EmrResource> emrResourceList) {
        DiagnosticReport report = new DiagnosticReport();
        report.setName(FHIRFeedHelper.addReferenceCodes(obs.getConcept(), idMappingsRepository));
        report.setStatus(new Enumeration<DiagnosticReportStatus>(DiagnosticReportStatus.final_));
        report.setIssuedSimple(new DateAndTime(obs.getObsDatetime()));
        report.setSubject(fhirEncounter.getSubject());

        List<Encounter.EncounterParticipantComponent> participants = fhirEncounter.getParticipant();
        if (CollectionUtils.isNotEmpty(participants)) {
            report.setPerformer(participants.get(0).getIndividual());
        }

        DateTime diagnostic = new DateTime();
        diagnostic.setValue(new DateAndTime(obs.getOrder().getDateActivated()));
        report.setDiagnostic(diagnostic);

        /*ResourceReference requestDetail = report.addRequestDetail();
        requestDetail.setReferenceSimple()*/

        for (Obs member : obs.getGroupMembers()) {
            if(member.getConcept().equals(obs.getConcept())) {
                List<EmrResource> observationResources = observationMapper.map(member, fhirEncounter);
                ResourceReference resourceReference = report.addResult();
                resourceReference.setReferenceSimple(observationResources.get(0).getIdentifier().getValueSimple());
                emrResourceList.addAll(observationResources);
            }else if(MRS_CONCEPT_NAME_LAB_NOTES.equals(member.getConcept().getName().getName())) {
                report.setConclusionSimple(member.getValueText());
            }
        }
        return report;
    }
}
