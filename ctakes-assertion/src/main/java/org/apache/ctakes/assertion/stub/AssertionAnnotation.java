package org.apache.ctakes.assertion.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssertionAnnotation extends ConceptAnnotation {

	static private final Logger LOGGER = LoggerFactory.getLogger( "AssertionAnnotation" );
	
	public ConceptType getConceptType() {
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		return null;
	}

	public Object getAssertionValue() {
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		return null;
	}

}
