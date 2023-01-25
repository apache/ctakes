package org.apache.ctakes.temporal.ae;

import com.google.gson.Gson;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
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
import java.util.List;
import java.util.stream.Collectors;

public class TemporalBertRestAnnotator extends JCasAnnotator_ImplBase {
    public static final String PARAM_REST_HOST = "ParamRestHost";
    @ConfigurationParameter(name=PARAM_REST_HOST, description = "Host where the REST server can be found", mandatory = false)
    private String host = "http://localhost";

    public static final String PARAM_REST_PORT = "ParamRestPort";
    @ConfigurationParameter(name=PARAM_REST_PORT, description = "Port to use to reach BERT REST server", mandatory = false)
    private int port = 8000;

    private static final String restPath = "temporal";
    private Logger logger = Logger.getLogger(TemporalBertRestAnnotator.class);

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(TemporalBertRestAnnotator.class);
    }

    private Gson gson = new Gson();

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        String restInitPath = String.format("%s:%d/%s/initialize", host, port, restPath);
        logger.info("Initializing BERT REST temporal engine at: " + restInitPath + " -- remote server will load BERT model onto GPU");

        super.initialize(context);

        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            HttpPost httppost = new HttpPost(restInitPath);
            httpclient.execute(httppost);
        }catch(IOException e){
            throw new ResourceInitializationException(e);
        }
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        long start = System.currentTimeMillis();
        String restProcessPath = String.format("%s:%d/%s/process", host, port, restPath);
        logger.info("Processing document for temporality with call to: " + restProcessPath);

        // 2 parallel data structures -- one with UIMA types and another with simpler types to send to the classifier process
        List<List<BaseToken>> sentTokens = new ArrayList<>();
        List<List<String>> sentTokenStrings = new ArrayList<>();
        List<IdentifiedAnnotation> entities = new ArrayList<>();

        // Uses similar logic as AssertionCleartkAnalysisEngine
        List<Sentence> sents = new ArrayList(JCasUtil.select(jCas, Sentence.class));
        for(Sentence sentence : sents){
            List<BaseToken> tokens = new ArrayList(JCasUtil.selectCovered(jCas, BaseToken.class, sentence));
            sentTokens.add(tokens);
            sentTokenStrings.add(tokens.stream().map(p -> p.getCoveredText()).collect(Collectors.toList()));
        }

        long preprocTime = System.currentTimeMillis();

        TemporalRequest requestObject = new TemporalRequest();
        requestObject.sent_tokens = sentTokenStrings;

        String json = gson.toJson(requestObject);
        long procTime, postprocTime;

        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            HttpPost httppost = new HttpPost(restProcessPath);
            StringEntity stringEntity = new StringEntity(json, "UTF-8");
            httppost.setEntity(stringEntity);
            CloseableHttpResponse response = httpclient.execute(httppost);
            procTime = System.currentTimeMillis();

            // turn response into uima properties:
            HttpEntity responseEntity = response.getEntity();
            String responseStr = EntityUtils.toString(responseEntity);
            TemporalResults results = gson.fromJson(responseStr, TemporalResults.class);

            for(int i = 0; i < sents.size(); i++){
                List<BaseToken> tokens = sentTokens.get(i);
                List<Timex> timexes = results.timexes.get(i);
                for(Timex timex : timexes){
                    TimeMention timeMention = new TimeMention(jCas,
                            tokens.get(timex.begin).getBegin(),
                            tokens.get(timex.end).getEnd());
                    timeMention.setTimeClass(timex.timeClass);
                    timeMention.addToIndexes();
                }
                List<SimpleEvent> events = results.events.get(i);
                for(SimpleEvent event : events){
                    EventMention eventMention = new EventMention(jCas,
                            tokens.get(event.begin).getBegin(),
                            tokens.get(event.end).getEnd());
                    Event e = new Event(jCas);
                    EventProperties props = new EventProperties(jCas);
                    props.setDocTimeRel(event.dtr);
                    e.setProperties(props);
                    eventMention.setEvent(e);
                    eventMention.addToIndexes();
                }

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

    public class TemporalRequest implements Serializable {
        List<List<String>> sent_tokens;
    }

    public class Timex{
        int begin, end;
        String timeClass;
    }
    public class SimpleEvent{
        int begin,end;
        String dtr;
    }
    public class TemporalResults implements Serializable {
        List<List<Timex>> timexes;
        List<List<SimpleEvent>> events;
    }

}
