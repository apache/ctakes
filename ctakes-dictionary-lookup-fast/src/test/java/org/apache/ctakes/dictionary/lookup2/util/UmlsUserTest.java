package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.core.ae.UmlsEnvironmentConfiguration;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;


/**
 * @author SPF , chip-nlp, pabramowitsch
 * @version %I%
 * @since 8/24/2015
 *
 * To run this test suite, please substitute CHANGE_ME for your API Key
 * then add the following to your environment
 *  umlsPass = <your api key>, 
 *       or ctakes.umlspw = <your api key>
 *  ummlsUser = "umls_api_user", 
 *       or ctakes.umlsuser="umls_api_user"
 *  Then Remove the @Ignore attribute below
 *  Then run.
 *  
 */

@Ignore  // needed for successful maven build/test when no API key is supplied
final public class UmlsUserTest {

   static private final Logger LOGGER = Logger.getLogger( "UmlsUserTester" );
   static UimaContext _uimaContext = null;
   
   private final String apiKeyForTesting = "CHANGE_ME";
 
   // reductio ad absurdem to get a UimaContext
   static  {
	   AnalysisEngine contextAE;
	try {
		DummyAnnotator da = new DummyAnnotator();
		contextAE = UIMAFramework.produceAnalysisEngine(da.createAnnotatorDescription());
		 _uimaContext = contextAE.getUimaContext();
	} catch (ResourceInitializationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
   
  @Test
  public void testViaProps() {
	   Properties props = new Properties();
	   props.setProperty(UmlsUserApprover.USER_PARAM, "umls_api_key");
	   props.setProperty(UmlsUserApprover.PASS_PARAM, apiKeyForTesting);
	   UmlsUserApprover approver = UmlsUserApprover.getInstance();
	   Assert.assertEquals(true, approver.isValidUMLSUser(_uimaContext, props));
	   approver.resetUserCache();
   }
  
  @Test
  public void testViaProps2() {
	   Properties props = new Properties();
	   props.setProperty(UmlsEnvironmentConfiguration.USER.toString(), "umls_api_key");
	   props.setProperty(UmlsEnvironmentConfiguration.PASSWORD.toString(), apiKeyForTesting);
	   UmlsUserApprover approver = UmlsUserApprover.getInstance();
	   Assert.assertEquals(true, approver.isValidUMLSUser(_uimaContext, props));
	   approver.resetUserCache();
   }
   
   @Test
   public void testViaEnvironment() {
	   /**
	    * to pass this test please either pair of environment variables
	    * umlsUser, umlsPass
	    * ctakes.umlsuser
	    * ctakes.umlspass
	    */
 	   UmlsUserApprover approver = UmlsUserApprover.getInstance();
 	   Assert.assertEquals(true, approver.isValidUMLSUser(_uimaContext, System.getProperties()));
 	   approver.resetUserCache();
    }
   
   @Test
   public void testViaApiKeyasUser() {
	   UmlsUserApprover approver = UmlsUserApprover.getInstance();
	   Assert.assertEquals(true,approver.isValidUMLSUser(
			   "","","umls_api_key", apiKeyForTesting));
	   approver.resetUserCache();
   }
   
   @Test
   public void testOldApiParams() {
	   UmlsUserApprover approver = UmlsUserApprover.getInstance();
	   Assert.assertNotEquals(true,approver.isValidUMLSUser(
			   "","","mocuser","mockpwd"));
	   approver.resetUserCache();
   }
   
   @Test
   public void testViaSingleProp() {
      Properties props = new Properties();
      props.setProperty( UmlsUserApprover.OLDY_KEY_PROP, apiKeyForTesting );
      UmlsUserApprover approver = UmlsUserApprover.getInstance();
      Assert.assertEquals( true, approver.isValidUMLSUser( _uimaContext, props ) );
      approver.resetUserCache();
   }
   
   /**
    * for this test you will need to make sure any user/pass env vars have been removed and instead
    * export ctakes.umls_apikey="<Your Key>"   or  export ctakes_umls_apikey="<Your Key>"
    */
   @Ignore
   @Test
   public void testViaSingleEnvar() {
 	   Properties props = new Properties();
 	   UmlsUserApprover approver = UmlsUserApprover.getInstance();
 	   Assert.assertEquals(true, approver.isValidUMLSUser(_uimaContext, props));
 	   approver.resetUserCache();
    }
   
}
