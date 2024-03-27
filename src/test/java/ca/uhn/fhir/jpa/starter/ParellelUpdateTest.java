package ca.uhn.fhir.jpa.starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class}, properties = {
	"spring.datasource.url=jdbc:h2:mem:dbr4",
	// "hapi.fhir.enable_repository_validating_interceptor=true",
	"hapi.fhir.fhir_version=r4"
})

public class ParellelUpdateTest {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ParellelUpdateTest.class);

	@LocalServerPort
	private int port;

	@Autowired
	private IFhirResourceDao<Patient> patientResourceDao;

	private IGenericClient client;
	private FhirContext ctx;

	@BeforeEach
	void setUp() {
		ctx = FhirContext.forR4();
		ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ctx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		client = ctx.newRestfulGenericClient(ourServerBase);
	}

	@Test
	void testParallelResourceUpdateTransactionBundles() {
		Patient pat = new Patient();
		String patId = client.create().resource(pat).execute().getId().getIdPart();

		//try it both using transaction bundles and instance PUTs
		launchUpdateThreads(patId, true, 2);
	}

	@Test
	void testParallelResourceUpdateSeperatePuts() {
		Patient pat = new Patient();
		String patId = client.create().resource(pat).execute().getId().getIdPart();

		//try it both using transaction bundles and instance PUTs
		launchUpdateThreads(patId, false, 2);
	}

	@Test
	void testParallelResourcePosts() {

		int threadCnt = 4;
		ExecutorService execSvc = Executors.newFixedThreadPool(threadCnt);

		//launch a bunch of threads at the same time that update the same patient
		List<Callable<Integer>> callables = new ArrayList<>();

		for (int i = 0; i < threadCnt; i++) {

			final int cnt = i;

			Callable<Integer> callable = new Callable<>() {

				@Override
				public Integer call() throws Exception {
					Patient pat = new Patient();

					//make sure to change something so the server doesnt short circuit on a no-op
					pat.addName().setFamily("fam-" + cnt);
					//String patId = client.create().resource(pat).execute().getId().getIdPart();

					MethodOutcome outcome = client.create().resource(pat).execute();
					ourLog.info("created patient response: " + outcome.getResponseStatusCode());

					if( outcome.getResponseStatusCode() == 201) {
						ourLog.info("created patient: " + outcome.getId().getIdPart());
					}
					return outcome.getResponseStatusCode();
				}
			};

			callables.add(callable);
		}

		List<Future<Integer>> futures = new ArrayList<>();

		//launch them all at once
		for (Callable<Integer> callable : callables) {
			futures.add(execSvc.submit(callable));
		}

		//wait for calls to complete
		for (Future<Integer> future : futures) {
			try {
				Integer httpResponseCode = future.get();
				Assertions.assertEquals(201, httpResponseCode);

			} catch (InterruptedException | ExecutionException e) {
				Assertions.fail("something failed", e);
			}
		}
	}

	private void launchUpdateThreads(String patientId, boolean useBundles, int threadCnt) {

		ExecutorService execSvc = Executors.newFixedThreadPool(threadCnt);

		//launch a bunch of threads at the same time that update the same patient
		List<Callable<Integer>> callables = new ArrayList<>();

		for (int i = 0; i < threadCnt; i++) {

			final int cnt = i;

			Callable<Integer> callable = new Callable<>() {

				@Override
				public Integer call() throws Exception {
					Patient pat = new Patient();
					//make sure to change something so the server doesnt short circuit on a no-op
					pat.addName().setFamily("fam-" + cnt);
					pat.setId(patientId);

					if( useBundles) {
						Bundle b = new Bundle();
						b.setType(BundleType.TRANSACTION);
						BundleEntryComponent bec = b.addEntry();
						bec.setResource(pat);
						//bec.setFullUrl("Patient/" + patId);
						Bundle.BundleEntryRequestComponent req = bec.getRequest();
						req.setUrl("Patient/" + patientId);
						req.setMethod(HTTPVerb.PUT);
						bec.setRequest(req);

						Bundle returnBundle = client.transaction().withBundle(b).execute();

						String statusString = returnBundle.getEntryFirstRep().getResponse().getStatus();
						ourLog.info("statusString->{}", statusString);

						try {

							return Integer.parseInt(statusString.substring(0,3));
						}catch(NumberFormatException nfe) {
							return 500;
						}

					}
					else {
						MethodOutcome outcome = client.update().resource(pat).withId(patientId).execute();
						ourLog.info("updated patient: " + outcome.getResponseStatusCode());
						//TODO: not sure why it returns 0
						return outcome.getResponseStatusCode() == 0 ? 200 : outcome.getResponseStatusCode();
					}
				}
			};

			callables.add(callable);
		}

		List<Future<Integer>> futures = new ArrayList<>();

		//launch them all at once
		for (Callable<Integer> callable : callables) {
			futures.add(execSvc.submit(callable));
		}

		//wait for calls to complete
		for (Future<Integer> future : futures) {
			try {
				Integer httpResponseCode = future.get();
				Assertions.assertEquals(200, httpResponseCode);

			} catch (InterruptedException | ExecutionException e) {
				Assertions.fail("something failed", e);
			}
		}
	}
}
