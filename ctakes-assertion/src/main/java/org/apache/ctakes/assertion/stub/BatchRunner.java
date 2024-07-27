package org.apache.ctakes.assertion.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

public class BatchRunner {

	static private final Logger LOGGER = LoggerFactory.getLogger( "BatchRunner" );

	public static Set<String> loadEnabledFeaturesFromFile(File enabledFeaturesFile) {
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		return null;
	}

}
