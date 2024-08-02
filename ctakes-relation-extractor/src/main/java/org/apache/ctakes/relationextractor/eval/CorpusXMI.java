/*
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
package org.apache.ctakes.relationextractor.eval;


import com.google.common.collect.Lists;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tmill on 1/31/17.
 */
public abstract class CorpusXMI {
//    public enum Corpus {SHARP, SHARP_RELEASE, DeepPhe}
    public enum Corpus {SHARP, SHARP_RELEASE}

    public enum EvaluateOn {
        TRAIN, DEV, TEST, OTHER
    }

    public static final String GOLD_VIEW_NAME = "GoldView";

    public static void validate(RelationEvaluation_ImplBase.EvaluationOptions options) throws Exception {
        // error on invalid option combinations
        if (options.getEvaluateOn().equals(EvaluateOn.TEST) && options.getGridSearch()) {
            throw new IllegalArgumentException("grid search can only be run on the train or dev sets");
        }
    }

    public static List<File> getTrainTextFiles(Corpus trainCorpus, EvaluateOn split, File corpusDirectory) {
        List<File> trainFiles = null;
        new ArrayList<>();

        // No matter what, the training files will contain the training data from the training corpus. May need to
        // add dev later.
        if (trainCorpus == Corpus.SHARP) {
            trainFiles = SHARPXMI.getTrainTextFiles(corpusDirectory);
        } else if (trainCorpus == Corpus.SHARP_RELEASE) {
            trainFiles = SHARPXMI.getTrainTextFilesFromCorpus(corpusDirectory);
//        } else if (trainCorpus == Corpus.DeepPhe) {
//            trainFiles = DeepPheXMI.getTrainTextFiles(corpusDirectory);
        } else {
            throw new RuntimeException("Unrecognized train corpus option: " + trainCorpus);
        }

        if (split == EvaluateOn.TEST) {
            // if we are testing on an actual test set then we first need to add the dev set notes to the training
            // set.
            if (trainCorpus == Corpus.SHARP) {
                trainFiles.addAll(SHARPXMI.getDevTextFiles(corpusDirectory));
            } else if (trainCorpus == Corpus.SHARP_RELEASE) {
                trainFiles.addAll(SHARPXMI.getTrainTextFilesFromCorpus(corpusDirectory));
//            } else if (trainCorpus == Corpus.DeepPhe) {
//                trainFiles.addAll(DeepPheXMI.getTrainTextFiles(corpusDirectory));
            } else {
                throw new RuntimeException("Unrecognized train corpus option: " + trainCorpus);
            }

        }
        return trainFiles;
    }

    public static List<File> getTestTextFiles(Corpus testCorpus, EvaluateOn split, File corpusDirectory) {
        List<File> testFiles = null;

        if (split == CorpusXMI.EvaluateOn.TRAIN) {
            if (testCorpus == CorpusXMI.Corpus.SHARP) {
                testFiles = SHARPXMI.getTrainTextFiles(corpusDirectory);
            } else if (testCorpus == CorpusXMI.Corpus.SHARP_RELEASE) {
                testFiles = SHARPXMI.getTrainTextFilesFromCorpus(corpusDirectory);
//            } else if (testCorpus == CorpusXMI.Corpus.DeepPhe) {
//                testFiles = DeepPheXMI.getTrainTextFiles(corpusDirectory);
            }
        } else if (split == CorpusXMI.EvaluateOn.DEV) {
            if (testCorpus == CorpusXMI.Corpus.SHARP) {
                testFiles = SHARPXMI.getDevTextFiles(corpusDirectory);
            } else if (testCorpus == Corpus.SHARP_RELEASE) {
                testFiles = SHARPXMI.getDevTextFilesFromCorpus(corpusDirectory);
//            } else if (testCorpus == CorpusXMI.Corpus.DeepPhe) {
//                testFiles = DeepPheXMI.getDevTextFiles(corpusDirectory);
            }
        } else if (split == CorpusXMI.EvaluateOn.TEST) {
            // find the test set files:
            if (testCorpus == CorpusXMI.Corpus.SHARP) {
                testFiles = SHARPXMI.getTestTextFiles(corpusDirectory);
            } else if (testCorpus == Corpus.SHARP_RELEASE) {
                testFiles = SHARPXMI.getTestTextFilesFromCorpus(corpusDirectory);
//            } else if (testCorpus == CorpusXMI.Corpus.DeepPhe) {
//                testFiles = DeepPheXMI.getTestTextFiles(corpusDirectory);
            }
        }
        return testFiles;
    }

    public static List<File> toXMIFiles( File xmiDirectory, List<File> textFiles ) {
        List<File> xmiFiles = Lists.newArrayList();
        for ( File textFile : textFiles ) {
            xmiFiles.add( toXMIFile( xmiDirectory, textFile ) );
        }
        return xmiFiles;
    }

    protected static File toXMIFile( File xmiDirectory, File textFile ) {
        return new File( xmiDirectory, textFile.getName() + ".xmi" );
    }

    public static class DocumentIDAnnotator extends JCasAnnotator_ImplBase {

        @Override
        public void process( JCas jCas ) throws AnalysisEngineProcessException {
            String documentID = new File( ViewUriUtil.getURI( jCas ) ).getPath();
            DocumentID documentIDAnnotation = new DocumentID( jCas );
            documentIDAnnotation.setDocumentID( documentID );
            documentIDAnnotation.addToIndexes();
        }
    }

    public static class CopyDocumentTextToGoldView extends JCasAnnotator_ImplBase {
        @Override
        public void process( JCas jCas ) throws AnalysisEngineProcessException {
            try {
                JCas goldView = jCas.getView( GOLD_VIEW_NAME );
                goldView.setDocumentText( jCas.getDocumentText() );
            } catch ( CASException e ) {
                throw new AnalysisEngineProcessException( e );
            }
        }
    }
}

