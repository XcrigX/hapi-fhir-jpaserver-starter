/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v.
 * 2.0 with a Healthcare Disclaimer.
 * A copy of the Mozilla Public License, v. 2.0 with the Healthcare Disclaimer can
 * be found under the top level directory, named LICENSE.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * If a copy of the Healthcare Disclaimer was not distributed with this file, You
 * can obtain one at the project website https://github.com/igia.
 * <p>
 * Copyright (C) 2018-2019 Persistent Systems, Inc.
 */
package ca.uhn.fhir.jpa.starter.smart.model;

import ca.uhn.fhir.jpa.starter.smart.exception.InvalidClinicalScopeException;

public class SmartClinicalScope {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(SmartClinicalScope.class);

	private final String compartment;
	private final String resource;
	//private final SmartOperationEnum operation;
	
	private boolean create; 
	private boolean read;
	private boolean update;
	private boolean delete;
	private boolean search;
	

	public SmartClinicalScope(String compartment, String resource) {
		this.compartment = compartment;
		this.resource = resource;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		if( create ) {
			sb.append("c");
		}
		if( read ) {
			sb.append("r");
		}
		if( update ) {
			sb.append("u");
		}
		if( delete ) {
			sb.append("d");
		}
		if( search ) {
			sb.append("s");
		}
		
		return "SmartClinicalScope [compartment=" + compartment + ", resource=" + resource + ", operation=" + sb.toString() + "]";
	}

	public SmartClinicalScope(String scope) {
		if(scope.matches("([A-z]*/([A-z]*|[*])[.]([*]|[A-z]*))")){
			String[] parts = scope.split("/");
			compartment = parts[0];
			String[] resourceAndOperation = parts[1].split("[.]");
			resource = resourceAndOperation[0];
			
			
			String scopeStr = resourceAndOperation[1];
			if( "read".equals(scopeStr)) {
				read = true;
				search = true;
			}
			else if( "write".equals(scopeStr)) {
				create = true;
				update = true;
				delete = true;
			}
			else if( "*".equals(scopeStr)) {
				read = true;
				search = true;
				create = true;
				update = true;
				delete = true;
			}
			else {
				//SMART v2 scope
				if(!scopeStr.matches("c?r?u?d?s?")){
					throw new InvalidClinicalScopeException(scope+" is not a valid clinical scope");
				}
				
				for( char c : scopeStr.toCharArray()) {
					if( 'c' == c ) {
						create = true;
					} else if( 'r' == c ) {
						read = true;
					} else if( 'u' == c ) {
						update = true;
					}else if( 'd' == c ) {
						delete = true;
					} else if( 's' == c ) {
						search = true;
					}
				}
			}
			
		} else{
			throw new InvalidClinicalScopeException(scope+" is not a valid clinical scope");
		}
	}

	/**
	 * Utility for creating SMART scopes - this method will return null if the scope format is not recognized
	 * @param scope the scope string, ex ) patient/Patient.read, patient/*.read, etc.
	 * @return a SmartClinicalScope if the formatting is valid
	 */
	public static SmartClinicalScope createIfValidSmartClinicalScope(String scope) {

		//this method doesnt do anything special yet, but made it to allow for making smarter determinations
		//if necessary at some point (checking the compartment/resource values, etc.)
		try {
			return new SmartClinicalScope(scope);
		}
		catch(Exception e) {
			ourLog.debug("Ignoring unknown scope: {}", scope);
			return null;
		}
	}

	public String getCompartment(){
		return compartment;
	}

	public String getResource() {
		return resource;
	}

	public boolean canCreate() {
		return create;
	}

	public boolean canRead() {
		return read;
	}

	public boolean canUpdate() {
		return update;
	}

	public boolean canDelete() {
		return delete;
	}

	public boolean canSearch() {
		return search;
	}
	
	public boolean canCruds() {
		return create && read && update && delete && search;
	}

	

}
