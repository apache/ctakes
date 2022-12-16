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
package org.apache.ctakes.necontexts.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.necontexts.ContextAnnotator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

@PipeBitInfo(
        name = "Thread safe Context Annotator",
        description = "Collects context for focus annotations for use by context consuming annotators.",
        dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN }
)
final public class ThreadSafeContextAnnotator extends ContextAnnotator {
    static private final Logger LOGGER = Logger.getLogger( "ThreadSafeContextAnnotator" );

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( final UimaContext context ) throws ResourceInitializationException {
        ContextSingleton.getInstance().initialize( context );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process( final JCas jCas ) throws AnalysisEngineProcessException {
        ContextSingleton.getInstance().process( jCas );
    }

    /**
     * @return a part of speech tagger using a default model
     * @throws ResourceInitializationException -
     */
    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                ThreadSafeContextAnnotator.class,
                TypeSystemDescriptionFactory.createTypeSystemDescription(),
                TypePrioritiesFactory.createTypePriorities( Sentence.class, BaseToken.class ) );
    }

    private enum ContextSingleton implements ThreadSafeWrapper<ContextAnnotator> {
        INSTANCE;

        static public ContextSingleton getInstance() {
            return INSTANCE;
        }

        private final ContextAnnotator _delegate;
        private boolean _initialized;

        ContextSingleton() {
            _delegate = new ContextAnnotator();
        }

        final private Object LOCK = new Object();

        @Override
        public Object getLock() {
            return LOCK;
        }

        @Override
        public ContextAnnotator getDelegate() {
            return _delegate;
        }

        @Override
        public boolean isInitialized() {
            return _initialized;
        }

        @Override
        public void setInitialized( final boolean initialized ) {
            _initialized = initialized;
        }
    }

}
