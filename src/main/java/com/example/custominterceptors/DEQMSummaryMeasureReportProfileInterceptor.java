package com.example.custominterceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingRuleBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Interceptor
@Component
public class DEQMSummaryMeasureReportProfileInterceptor extends RepositoryValidatingInterceptor {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(DEQMSummaryMeasureReportProfileInterceptor.class);

	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ApplicationContext myApplicationContext;
	/**
	 * This method will be called at startup time
	 */
	@PostConstruct
	public void start() {

		ourLog.info("DEQMSummaryMeasureReportProfileInterceptor - PostConstruct called. FhirContext is: {}", myFhirContext);
		setFhirContext(myFhirContext);

		// Ask the application context for a new Rule Builder
		RepositoryValidatingRuleBuilder ruleBuilder =
			myApplicationContext.getBean(RepositoryValidatingRuleBuilder.class);

		// Here we will mandate only that any MeasureReport resources stored in the repository
		// must declare conformance to the DEQM Summary MeasureReport Profile, and must correctly validate.
		// You may add as many rules for as many resource types as you like here.


		ruleBuilder
			.forResourcesOfType("MeasureReport")
			.requireAtLeastProfile("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/summary-measurereport-deqm")
			.and()
			.requireValidationToDeclaredProfiles();

		// Create the ruleset and pass it to the interceptor
		setRules(ruleBuilder.build());
	}

}