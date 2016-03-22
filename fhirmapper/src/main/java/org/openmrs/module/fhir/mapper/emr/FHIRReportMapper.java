package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class FHIRReportMapper implements FHIRResourceMapper {
    private ConceptService conceptService;
    private FHIRObservationsMapper fhirObservationsMapper;

    @Autowired
    public FHIRReportMapper(ConceptService conceptService, FHIRObservationsMapper fhirObservationsMapper) {
        this.conceptService = conceptService;
        this.fhirObservationsMapper = fhirObservationsMapper;
    }

    @Override
    public boolean canHandle(IResource resource) {
        if (resource instanceof DiagnosticReport) {
            DiagnosticReport report = (DiagnosticReport) resource;
            if (hasRadiologyCategory(report))
                return true;
        }
        return false;
    }

    @Override
    public void map(IResource resource, EmrEncounter emrEncounter, ShrEncounterBundle shrEncounterBundle, SystemProperties systemProperties) {
        Concept fulfillmentConcept = conceptService.getConceptByName("Radiology Order Fulfillment Form");
        Obs fulfillmentObs = new Obs();
        fulfillmentObs.setConcept(fulfillmentConcept);

        addGroupMembers(fulfillmentObs, resource, shrEncounterBundle, emrEncounter);

        emrEncounter.addObs(fulfillmentObs);
    }

    private void addGroupMembers(Obs fulfillmentObs, IResource resource, ShrEncounterBundle shrEncounterBundle, EmrEncounter emrEncounter) {
        List<ResourceReferenceDt> result = ((DiagnosticReport) resource).getResult();
        for (ResourceReferenceDt resultReference : result) {
            Observation observationByReference = (Observation) FHIRBundleHelper.findResourceByReference(shrEncounterBundle.getBundle(), resultReference);
            Obs resultObs = fhirObservationsMapper.mapObs(shrEncounterBundle, emrEncounter, observationByReference);
            fulfillmentObs.addGroupMember(resultObs);
        }
    }

    private boolean hasRadiologyCategory(DiagnosticReport report) {
        if (report.getCategory().isEmpty()) return false;
        for (CodingDt codingDt : report.getCategory().getCoding()) {
            if (FHIRProperties.FHIR_V2_VALUESET_DIAGNOSTIC_REPORT_CATEGORY_URL.equals(codingDt.getSystem()) &&
                    FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE.equals(codingDt.getCode()))
                return true;
        }
        return false;
    }
}
