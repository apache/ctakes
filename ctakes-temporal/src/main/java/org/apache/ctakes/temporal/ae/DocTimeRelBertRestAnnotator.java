/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.temporal.ae;

import com.google.gson.Gson;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocTimeRelBertRestAnnotator extends JCasAnnotator_ImplBase {
    public static final String PARAM_REST_HOST = "ParamRestHost";
    @ConfigurationParameter(name=PARAM_REST_HOST, description = "Host where the REST server can be found", mandatory = false)
    private String host = "http://localhost";

    public static final String PARAM_REST_PORT = "ParamRestPort";
    @ConfigurationParameter(name=PARAM_REST_PORT, description = "Port to use to reach BERT REST server", mandatory = false)
    private int port = 8000;

    private static final String restPath = "dtr";
    private Logger logger = Logger.getLogger(DocTimeRelBertRestAnnotator.class);

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(DocTimeRelBertRestAnnotator.class);
    }

    public static AnalysisEngineDescription createAnnotatorDescription(int port) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(DocTimeRelBertRestAnnotator.class,
                DocTimeRelBertRestAnnotator.PARAM_REST_PORT,
                port);
    }

    private Gson gson = new Gson();

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        long start = System.currentTimeMillis();
        String restProcessPath = String.format("%s:%d/%s/process", host, port, restPath);
        logger.info("Processing document for DocTimeRel with call to: " + restProcessPath);

        // 2 parallel data structures -- one with UIMA types and another with simpler types to send to the classifier process
        List<List<Integer>> spans = new ArrayList<>();
        List<EventMention> events = new ArrayList<>();

        // Uses similar logic as AssertionCleartkAnalysisEngine
        for(Sentence sentence : JCasUtil.select(jCas, Sentence.class)){
            for(EventMention annot : JCasUtil.selectCovered(jCas, EventMention.class, sentence)){
                spans.add(Arrays.asList(annot.getBegin(), annot.getEnd()));
                events.add(annot);
            }
        }

        long preprocTime = System.currentTimeMillis();

        DocTimeRelRequest requestObject = new DocTimeRelRequest();
        requestObject.doc_text = jCas.getDocumentText();
        requestObject.entities = spans;

        String json = gson.toJson(requestObject);
        long procTime, postprocTime;

        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            HttpPost httppost = new HttpPost(restProcessPath);
            httppost.addHeader("content-type", "application/json");
            StringEntity stringEntity = new StringEntity(json, "UTF-8");
            httppost.setEntity(stringEntity);
            CloseableHttpResponse response = httpclient.execute(httppost);
            procTime = System.currentTimeMillis();

            // turn response into uima properties:
            HttpEntity responseEntity = response.getEntity();
            String responseStr = EntityUtils.toString(responseEntity);
            DocTimeRelResults results = gson.fromJson(responseStr.toString(), DocTimeRelResults.class);
            for(int i = 0; i < events.size(); i++){
                if(events.get(i).getEvent() == null){
                    Event event = new Event(jCas);
                    events.get(i).setEvent(event);
                }
                Event event = events.get(i).getEvent();
                if(event.getProperties() == null){
                    EventProperties props = new EventProperties(jCas);
                    event.setProperties(props);
                }
                EventProperties props = event.getProperties();
                props.setDocTimeRel(results.statuses[i]);
            }
            postprocTime =System.currentTimeMillis();
        }catch(IOException e){
            throw new AnalysisEngineProcessException(e);
        }
        logger.info("Completed in total time " + (postprocTime-start));
        logger.debug("Detailed processing time: " + (preprocTime-start)/1000.0 + " s preprocessing, " +
                (procTime-preprocTime)/1000.0 + " s processing, and " +
                (postprocTime-procTime)/1000.0 + " s post-processing.");

    }

    public class DocTimeRelRequest implements Serializable {
        String doc_text;
        List<List<Integer>> entities;
    }

    public class DocTimeRelResults implements Serializable {
        String[] statuses;
    }
}
