package ca.uhn.fhir.jpa.starter.gravity.endpoints;

import ca.uhn.fhir.jpa.starter.gravity.ServerLogger;
import ca.uhn.fhir.jpa.starter.gravity.controllers.AuthorizationController;
import ca.uhn.fhir.jpa.starter.gravity.utils.AuthUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class TokenEndpoint {
	private static final String ERROR_KEY = "error";
	private static final String ERROR_DESCRIPTION_KEY = "error_description";

	private static final Logger logger = ServerLogger.getLogger();

	/**
	 * Enum for types of tokens
	 */
	public enum TokenType {
		REFRESH,
		ACCESS;
	}

	public static ResponseEntity<String> handleTokenRequest(
			HttpServletRequest request, String grantType, String token, String redirectURI, String serverAddress) {
		// Set the headers for the response
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CACHE_CONTROL, "no-store");
		headers.add(HttpHeaders.PRAGMA, "no-store");

		HashMap<String, String> response = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String baseUrl = AuthUtils.getFhirBaseUrl(serverAddress);

		// Validate the client is authorized: Basic authorization header
		String clientId = AuthUtils.clientIsAuthorized(request);
		if (clientId == null) {
			response.put(ERROR_KEY, "invalid_client");
			response.put(
					ERROR_DESCRIPTION_KEY,
					"Authorization header is missing, malformed, or client_id/client_secret is invalid");
			return new ResponseEntity<>(gson.toJson(response), headers, HttpStatus.UNAUTHORIZED);
		}

		// Validate the grant_type is authorization_code or refresh_token
		String providerId = null;
		if (grantType.equals("authorization_code")) {
			// Request is to trade authorization_code for access token
			providerId = AuthUtils.authCodeIsValid(token, baseUrl, redirectURI, clientId);
		} else if (grantType.equals("refresh_token")) {
			// Request is to trade refresh_token for access token
			providerId = AuthUtils.refreshTokenIsValid(token, baseUrl, clientId);
		} else {
			response.put(ERROR_KEY, "invalid_request");
			response.put(ERROR_DESCRIPTION_KEY, "grant_type must be authorization_code not " + grantType);
			return new ResponseEntity<>(gson.toJson(response), headers, HttpStatus.BAD_REQUEST);
		}

		logger.fine("TokenEndpoint::Token:Patient:" + providerId);
		if (providerId != null) {
			// One hour life time for access token
			Instant exp = LocalDateTime.now()
					.plusHours(1)
					.atZone(ZoneId.systemDefault())
					.toInstant();
			if (providerId.equals("admin"))
				exp = LocalDateTime.now()
						.plusDays(2000)
						.atZone(ZoneId.systemDefault())
						.toInstant();
			String accessToken = AuthUtils.generateToken(
					baseUrl, clientId, providerId, UUID.randomUUID().toString(), exp);
			logger.fine("TokenEndpoint::Token:Generated token " + accessToken);
			if (accessToken != null) {
				String jwtId = UUID.randomUUID().toString();
				long expiresIn = (Date.from(exp).getTime() - new Date().getTime()) / 1000;
				response.put("access_token", accessToken);
				response.put("token_type", "bearer");
				response.put("expires_in", String.valueOf(expiresIn));
				response.put("practitioner", providerId);
				response.put("scope", String.join(" ", AuthUtils.supportedScopes()));
				exp = LocalDateTime.now()
						.plusDays(30)
						.atZone(ZoneId.systemDefault())
						.toInstant();
				response.put("refresh_token", AuthUtils.generateToken(baseUrl, clientId, providerId, jwtId, exp));
				AuthorizationController.getDB().setRefreshTokenId(providerId, jwtId);
				return new ResponseEntity<>(gson.toJson(response), headers, HttpStatus.OK);
			} else {
				response.put(ERROR_KEY, "invalid_request");
				response.put(
						ERROR_DESCRIPTION_KEY,
						"Internal server error: unable to generate a singed JWT access token. Please try again");
				return new ResponseEntity<>(gson.toJson(response), headers, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			response.put(ERROR_KEY, "invalid_grant");
			response.put(
					ERROR_DESCRIPTION_KEY,
					"Failed to verify the authorization code/refresh token. Please reauthenticate.");
			return new ResponseEntity<>(gson.toJson(response), headers, HttpStatus.BAD_REQUEST);
		}
	}
}
