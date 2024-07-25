package org.apache.ctakes.assertion.stub;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class AssertionAnnotation extends ConceptAnnotation {

	static private final Logger LOGGER = LogManager.getLogger( "AssertionAnnotation" );
	
	public ConceptType getConceptType() {
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		return null;
	}

	public Object getAssertionValue() {
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		return null;
	}

}
