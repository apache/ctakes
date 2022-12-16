package org.apache.ctakes.assertion.stub;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Logger;

public class BatchRunner {

	static private final Logger LOGGER = Logger.getLogger( "BatchRunner" );

	public static Set<String> loadEnabledFeaturesFromFile(File enabledFeaturesFile) {
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		return null;
	}

}
