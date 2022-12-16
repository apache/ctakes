package org.apache.ctakes.temporal.keras;

import java.io.File;
import java.io.FileNotFoundException;

import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.util.featurevector.FeatureVector;

import com.google.common.annotations.Beta;

/**
 * <br>
 * Copyright (c) 2016, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Tim Miller
 * @version 2.0.1
 * 
 */
@Beta
public class KerasStringOutcomeDataWriter extends ScriptStringFeatureDataWriter<KerasStringOutcomeClassifierBuilder>{

	public KerasStringOutcomeDataWriter(File outputDirectory)
			throws FileNotFoundException {
		super(outputDirectory);
	}

	@Override
	protected KerasStringOutcomeClassifierBuilder newClassifierBuilder() {
		return new KerasStringOutcomeClassifierBuilder();
	}

	@Override
	protected void writeEncoded(FeatureVector features, Integer outcome) throws CleartkProcessingException {
		// TODO Auto-generated method stub

	}
}
