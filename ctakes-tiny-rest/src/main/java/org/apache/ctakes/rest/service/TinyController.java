package org.apache.ctakes.rest.service;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.ctakes.rest.service.response.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/5/2019
 */
@RestController
public class TinyController {

   static private final Logger LOGGER = Logger.getLogger( "TinyController" );

   static private volatile boolean _initialized = false;

   @PostConstruct
   public void init() throws ResourceInitializationException {
      synchronized ( LOGGER ) {
         if ( _initialized ) {
            return;
         }
         LOGGER.info( "Initializing analysis engine ..." );
         try {
            // The first access of a singleton enum instantiates it.
            RestPipelineRunner.getInstance();
         } catch ( ExceptionInInitializerError initE ) {
            throw new ResourceInitializationException( initE );
         }
         LOGGER.info( "Analysis Engine Initialized." );
         _initialized = true;
      }
   }

   @RequestMapping( value = "/process", method = RequestMethod.POST )
   @ResponseBody
   public String processText( @RequestBody final String text,
                              @RequestParam( "format" ) final Optional<String> responseFormat )
         throws AnalysisEngineProcessException {
//      LOGGER.info( "Processing " + text );
      final String format = responseFormat.orElse( "default" ).toLowerCase();
      switch ( format ) {
         case "fhir":
            return RestPipelineRunner.getInstance().process( new FhirJsonFormatter(), text );
         case "pretty":
            return RestPipelineRunner.getInstance().process( new PrettyPrintFormatter(), text );
         case "property":
            return RestPipelineRunner.getInstance().process( new PropertyListFormatter(), text );
         case "umls":
            return RestPipelineRunner.getInstance().process( new UmlsJsonFormatter(), text );
         case "cui":
            return RestPipelineRunner.getInstance().process( new CuiListFormatter(), text );
         case "xmi":
            return RestPipelineRunner.getInstance().process( new XmiFormatter(), text );
      }
      return RestPipelineRunner.getInstance().process( new FhirJsonFormatter(), text );
   }


}
