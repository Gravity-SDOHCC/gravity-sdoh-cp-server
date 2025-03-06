package ca.uhn.fhir.jpa.starter.gravity;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import jakarta.interceptor.Interceptor;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;

@Interceptor
public class SdohCapabilityStatementProvider {

	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	public CapabilityStatement getServerConformance(IBaseConformance theCapabilityStatement) {
		CapabilityStatement metadata = (CapabilityStatement) theCapabilityStatement;
		metadata.addInstantiates("http://hl7.org/fhir/us/sdoh-clinicalcare/CapabilityStatement/SDOHCC-ReferralSource");
		metadata.setName("Gravity SDOH CP FHIR Server RI");
		metadata.setDescription(
				"A Reference Implementation for Gravity SDOH Clinical Care Coordination Platform HAPI FHIR R4 Server");
		metadata.setStatus(PublicationStatus.ACTIVE);
		metadata.setPublisher("HL7 International Patient Care WG");
		metadata.addImplementationGuide(
				"http://hl7.org/fhir/us/sdoh-clinicalcare/ImplementationGuide/hl7.fhir.us.sdoh-clinicalcare");

		CapabilityStatementSoftwareComponent software = new CapabilityStatementSoftwareComponent();
		software.setName("Gravity SDOHCC CP HAPI FHIR based Server");
		software.setVersion("2.0.0");
		metadata.setSoftware(software);

		return metadata;
	}
}
