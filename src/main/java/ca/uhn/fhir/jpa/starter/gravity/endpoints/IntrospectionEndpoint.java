package ca.uhn.fhir.jpa.starter.gravity.endpoints;

import ca.uhn.fhir.jpa.starter.gravity.ServerLogger;
import ca.uhn.fhir.jpa.starter.gravity.controllers.AuthorizationController;
import ca.uhn.fhir.jpa.starter.gravity.utils.AuthUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class IntrospectionEndpoint {
	private IntrospectionEndpoint() {
		throw new IllegalStateException("Introspection Endpoint Utility class");
	}

	private static final Logger logger = ServerLogger.getLogger();

	public static ResponseEntity<String> handleIntrospection(String token, String serverAddress) {
		final Map<String, String> responseData = new HashMap<>();

		String baseUrl = AuthUtils.getFhirBaseUrl(serverAddress);
		try {
			Algorithm ALGORITHM = Algorithm.RSA256(AuthorizationController.getPublicKey(), null);
			JWTVerifier VERIFIER = JWT.require(ALGORITHM)
					.withIssuer(baseUrl)
					.withAudience(baseUrl)
					.build();
			DecodedJWT jwt = VERIFIER.verify(token);

			responseData.put("active", String.valueOf(true));
			responseData.put("aud", jwt.getAudience().get(0));
			responseData.put("iss", jwt.getIssuer());
			responseData.put("exp", String.valueOf(jwt.getExpiresAt().getTime() / 1000)); // Display in sec not ms
			responseData.put("iat", String.valueOf(jwt.getIssuedAt().getTime() / 1000)); // Display in sec not ms
			responseData.put("providerId", jwt.getClaim("providerId").asString());
			responseData.put("practitionerId", jwt.getClaim("practitionerId").asString());

		} catch (JWTVerificationException e) {
			responseData.put("active", String.valueOf(false));
			logger.severe("Unable to verify JWT: " + e.getMessage());
		}

		JSONObject response = new JSONObject(responseData);
		return new ResponseEntity<>(response.toString(), HttpStatus.OK);
	}
}
