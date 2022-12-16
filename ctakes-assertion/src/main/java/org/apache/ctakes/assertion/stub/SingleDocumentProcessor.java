package org.apache.ctakes.assertion.stub;

import java.util.List;
import java.util.Map;

import org.apache.ctakes.assertion.medfacts.i2b2.api.CharacterOffsetToLineTokenConverterCtakesImpl;
import org.apache.log4j.Logger;

public class SingleDocumentProcessor {
	static private final Logger LOGGER = Logger.getLogger( "SingleDocumentProcessor" );

	protected CharacterOffsetToLineTokenConverterCtakesImpl converter2;

	protected String[][] arrayOfArrayOfTokens;
	
	protected void preExecutionTest() {
		// TODO Auto-generated method stub
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		
	}
	
	public Object addConcept(ApiConcept ac) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void processSingleDocument() {
		// TODO Auto-generated method stub
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		
	}
	
	public void setConverter2(CharacterOffsetToLineTokenConverter converter) {
		// TODO Auto-generated method stub
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		
	}


	public void setAssertionDecoderConfiguration(AssertionDecoderConfiguration _assertionDecoderConfiguration) {
		// TODO Auto-generated method stub
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		
	}


	public Map<Integer, String> getAssertionTypeMap() {
		// TODO Auto-generated method stub
		return null;
	}


	public LineAndTokenPosition convertCharacterOffsetToLineToken(int characterOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<LineAndTokenPosition> calculateBeginAndEndOfConcept(ApiConcept problem) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setContents(String contents) {
		// TODO Auto-generated method stub
		
	}


	public void preprocess() {
		// TODO Auto-generated method stub
		LOGGER.warn("This class cannot be used until CTAKES-76 is implemented.");
		
	}

	public String[][] getTokenArrays() {
		// TODO Auto-generated method stub
		return null;
	}

}
