package ca.uhn.fhir.jpa.starter;

import java.net.URI;
import java.net.URISyntaxException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class, JpaStarterWebsocketDispatcherConfig.class}, properties = {
		"spring.datasource.url=jdbc:h2:mem:dbr4",
		// "hapi.fhir.enable_repository_validating_interceptor=true",
		"hapi.fhir.fhir_version=r4"
})

public class ResVersionConstraintTest {
	
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ExampleServerR4IT.class);

	@LocalServerPort
	private int port;

	@Autowired
	private IFhirResourceDao<Patient> patientResourceDao;

	private IGenericClient client;
	private FhirContext ctx;
	private String ourServerBase;

	@BeforeEach
	void setUp() {
		ctx = FhirContext.forR4();
		ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ctx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		ourServerBase = "http://localhost:" + port + "/fhir/";
		client = ctx.newRestfulGenericClient(ourServerBase);

		// Properties props = new Properties();
		// props.put("spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration");
	}

	@Test
	void testBadBundleConcurrencyIssue() throws URISyntaxException {

		// we registered two custom interceptors via the property 'hapi.fhir.custom-interceptor-classes'
		// one is discovered as a Spring Bean, one instantiated via reflection
		// both should be registered with the server and will add a custom extension to any Patient resource created
		// so we can verify they were registered
		
		String bundleText = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"PractitionerRole\",\"id\":\"P000000661\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/davinci-pdex-plan-net/StructureDefinition/plannet-PractitionerRole\"]},\"extension\":[{\"valueReference\":{\"reference\":\"Organization/ATRIO Medicare\"}}],\"identifier\":[{\"value\":\"P000000661\"}],\"practitioner\":{\"reference\":\"Practitioner/P000000661\"},\"organization\":{\"reference\":\"Organization/S000008531\"},\"code\":[{\"coding\":[{\"code\":\"P000000661-S000008531L006\",\"display\":\"P000000661-S000008531L006\"}]}],\"specialty\":[{\"coding\":[{\"code\":\"1041C0700X\",\"display\":\"Social Worker, Clinical\"}]}],\"location\":[{\"reference\":\"Location/S000008531L006\"}],\"healthcareService\":[{\"reference\":\"HealthcareService/0\"}]},\"request\":{\"method\":\"PUT\",\"url\":\"PractitionerRole/P000000661\"}}]}";

		exchangeResouce(ourServerBase, bundleText, HttpMethod.POST);
		
		
		
		Bundle bundle = (Bundle) client.search().forResource(PractitionerRole.class).execute();
		PractitionerRole pr = (PractitionerRole) bundle.getEntryFirstRep().getResource();
		
		 
		
		
		Assertions.assertNotNull(pr);
		
		//PractitionerRole savedRes = client.search().forResource(PractitionerRole.class).execute();
				
				//sead().resource(PractitionerRole.class).withId(1).execute();
		
		/*(Patient pat = new Patient();
		String patId = client.create().resource(pat).execute().getId().getIdPart();

		Patient readPat = client.read().resource(Patient.class).withId(patId).execute();

		Assertions.assertNotNull(readPat.getExtensionByUrl("http://some.custom.pkg1/CustomInterceptorBean"));
		Assertions.assertNotNull(readPat.getExtensionByUrl("http://some.custom.pkg1/CustomInterceptorPojo"));*/

	}
	
	private void exchangeResouce(String url, String jsonStr, HttpMethod method) throws URISyntaxException {

		RestTemplate restTemplate = new RestTemplate(); 

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add(HttpHeaders.CONTENT_TYPE, ca.uhn.fhir.rest.api.Constants.CT_FHIR_JSON_NEW);
		httpHeaders.add(HttpHeaders.ACCEPT, ca.uhn.fhir.rest.api.Constants.CT_FHIR_JSON_NEW);

		HttpEntity<String> httpEntity = new HttpEntity<>(jsonStr, httpHeaders);

		String responseBody = null;
		HttpStatus status = null;
		try {
			URI uri = new URI(url);
			ResponseEntity<String> resp = restTemplate.exchange(uri, method, httpEntity, String.class);
			//ResponseEntity<String> resp = restTemplate.postForEntity(url, httpEntity, String.class);
			//log.debug("postedResource returned id->" + retrieveResourceId(resp));
			status = resp.getStatusCode();
			responseBody = resp.getBody();
		} catch (HttpStatusCodeException ex) {
			status = ex.getStatusCode();
			responseBody = ex.getResponseBodyAsString();
		}

		ourLog.debug("status->{}, responseBody->{}", status, responseBody);

		//IBaseResource fhirResource = ctx.newJsonParser().setPrettyPrint(true).parseResource(responseBody);
		
		
	}
	
	
}
