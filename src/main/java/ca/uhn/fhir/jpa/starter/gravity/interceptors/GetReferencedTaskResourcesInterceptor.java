package ca.uhn.fhir.jpa.starter.gravity.interceptors;

import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.jpa.starter.gravity.ServerLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GetReferencedTaskResourcesInterceptor extends InterceptorAdapter {
	private static final Logger logger = ServerLogger.getLogger();

	private static final FhirContext ctx = FhirContext.forR4();;

	@Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
	public void handleTaskCreation(IBaseResource theResource, RequestDetails theRequestDetails,
			ResponseDetails theResponseDetails) {
		if (!(theResource instanceof Task)) {
			return;
		}
		Task createdTask = (Task) theResource;
		if (!createdTask.getPartOf().isEmpty()) {
			return;
		}

		String thisServerBaseUrl = theRequestDetails.getFhirServerBase();
		String patientUrl = createdTask.getFor().getReference();
		String serviceRequestUrl = createdTask.getFocus().getReference();
		String requesterUrl = createdTask.getRequester().getReference();
		String receiverBaseUrl = serviceRequestUrl.substring(0, serviceRequestUrl.indexOf("/ServiceRequest"));
		String serviceRequestId = serviceRequestUrl.substring(serviceRequestUrl.lastIndexOf('/') + 1);
		serviceRequestUrl = receiverBaseUrl + "/ServiceRequest" + "?_id=" + serviceRequestId
				+ "&_include=ServiceRequest:subject";
		String ownerUrl = receiverBaseUrl + "/" + createdTask.getOwner().getReference();

		IGenericClient receiverClient = setupClient(receiverBaseUrl);
		IGenericClient myClient = setupClient(thisServerBaseUrl);

		getreferencedResourcesAndPersist(receiverClient, myClient, patientUrl, requesterUrl, serviceRequestUrl, ownerUrl);

	}

	private void getreferencedResourcesAndPersist(IGenericClient receiverClient, IGenericClient myClient,
			String patientUrl,
			String requesterUrl, String serviceRequestUrl, String ownerUrl) {

		try {
			Patient patient = receiverClient.read().resource(Patient.class).withUrl(patientUrl).execute();
			myClient.update().resource(patient).execute();
			logger.info("Successfully retrieved and saved the associated patient");
		} catch (Exception e) {
			logger.severe("Unable to retrieve/update referenced resoource for received task: " + e.getMessage());
		}

		String consentId = "";
		ServiceRequest request = null;
		try {
			Bundle bundle = receiverClient.search().byUrl(serviceRequestUrl).returnBundle(Bundle.class).execute();
			logger.info("Retrieved the associated service request from the requester");

			for (BundleEntryComponent entry : bundle.getEntry()) {

				if (entry.getResource().getResourceType() == ResourceType.ServiceRequest) {
					request = (ServiceRequest) entry.getResource();
					if (request.getSupportingInfoFirstRep() != null
							&& request.getSupportingInfoFirstRep().getReference() != null) {
						String consentref = request.getSupportingInfoFirstRep().getReference();
						consentId = consentref.substring(consentref.lastIndexOf('/') + 1);
					} else if (request.getReasonReferenceFirstRep() != null) {
						String conditionref = request.getReasonReferenceFirstRep().getReference();
						conditionref = receiverClient.getServerBase() + "/" + conditionref;
						List<Reference> newRef = new ArrayList<Reference>();
						newRef.add(new Reference(conditionref));
						request.setReasonReference(newRef);
					}
				}
			}

		} catch (Exception e) {
			logger.severe("Unable to retrieve service request for received task: " + e.getMessage());
		}
		try {
			if (!consentId.isEmpty()) {
				Consent consent = receiverClient.read().resource(Consent.class).withId(consentId).execute();
				String orgRef = consent.getOrganizationFirstRep().getReference();
				orgRef = receiverClient.getServerBase() + "/" + orgRef;
				List<Reference> newList = new ArrayList<Reference>();
				newList.add(new Reference(orgRef));
				consent.setOrganization(newList);

				myClient.update().resource(consent).execute();
			}
		} catch (Exception e) {
			logger.severe("Unable to retrieve/update consent for received task: " + e.getMessage());

		}

		try {
			if (request != null) {
				myClient.update().resource(request).execute();
				logger.info("Successfully saved the associated patient");
			}

		} catch (Exception e) {
			logger.severe("Unable to update service request for received task: " + e.getMessage());
		}

		try {
			if (requesterUrl.contains("Organization")) {
				Organization organization = receiverClient.read().resource(Organization.class).withUrl(requesterUrl)
						.execute();
				myClient.update().resource(organization).execute();

			} else if (requesterUrl.contains("PractitionerRole")) {

				PractitionerRole practitionerRole = receiverClient.read().resource(PractitionerRole.class)
						.withUrl(requesterUrl).execute();
				String orgRef = practitionerRole.getOrganization().getReference();
				orgRef = receiverClient.getServerBase() + "/" + orgRef;
				practitionerRole.getOrganization().setReference(orgRef);
				String practitionerRef = practitionerRole.getPractitioner().getReference();
				practitionerRef = receiverClient.getServerBase() + "/" + practitionerRef;
				practitionerRole.getPractitioner().setReference(practitionerRef);
				myClient.update().resource(practitionerRole).execute();
			} else if (requesterUrl.contains("Practitioner")) {
				Practitioner practitioner = receiverClient.read().resource(Practitioner.class).withUrl(requesterUrl)
						.execute();
				myClient.update().resource(practitioner).execute();
			}

		} catch (Exception e) {
			logger.severe("Unable to retrieved/update requester for received task: " + e.getMessage());
		}

		try {
			Organization organization = receiverClient.read().resource(Organization.class).withUrl(ownerUrl)
					.execute();
			myClient.update().resource(organization).execute();
		} catch (Exception e) {
			logger.severe("Unable to retrieved/update owner for received task: " + e.getMessage());
		}
	}

	private IGenericClient setupClient(String serverBaseUrl) {
		ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ctx.getRestfulClientFactory().setConnectTimeout(20 * 1000);
		return ctx.newRestfulGenericClient(serverBaseUrl);
	}
}
