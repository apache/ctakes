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

import com.lexicalscope.jewel.cli.Option;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.cleartk.eval.AnnotationStatistics;

import java.io.File;
import java.util.List;

/**
 * Created by tmill on 1/31/17.
 */
public abstract class RelationEvaluation_ImplBase extends org.cleartk.eval.Evaluation_ImplBase<File, AnnotationStatistics<String>> {
    public RelationEvaluation_ImplBase( File baseDirectory ) {
        super( baseDirectory );
    }

    @Override
    public CollectionReader getCollectionReader(List<File> items ) throws Exception {
        return CollectionReaderFactory.createReader(
                XMIReader.class,
                TypeSystemDescriptionFactory.createTypeSystemDescription(),
                XMIReader.PARAM_FILES,
                items );
    }

    public static interface EvaluationOptions {
        @Option(
                longName = "evaluate-on",
                defaultValue = "DEV",
                description = "perform evaluation using the training (TRAIN), development (DEV) or test "
                        + "(TEST) data.")
        public CorpusXMI.EvaluateOn getEvaluateOn();

        @Option(
                longName = "grid-search",
                description = "run a grid search to select the best parameters")
        public boolean getGridSearch();

        @Option(
                defaultToNull=true,
                longName = "train-xmi-dir",
                description = "use these XMI files for training; they must contain the necessary preprocessing "
                        + "in system view and gold annotation in gold view")
        public File getTrainXmiDir();

        @Option(
                longName = "test-xmi-dir",
                defaultValue = "",
                description = "evaluate on these XMI files; they must contain the necessary preprocessing "
                        + "in system view and gold annotation in gold view")
        public File getTestXmiDir();

        @Option(
                longName = "batches-dir",
                description = "directory containing ssN_batchNN directories, each of which should contain "
                        + "a Knowtator directory and a Knowtator_XML directory",
                defaultToNull = true)
        public File getSharpBatchesDirectory();

        @Option(
                longName = "corpus-dir",
                description = "Path to the SHARP corpus release (version 2 would end in /v2/SHARP)",
                defaultToNull = true)
        public File getSharpCorpusDirectory();

        @Option(
                longName = "deepphe-anafora-dir",
                description = "Path to the anafora directory containing DeepPhe data",
                shortName = "d",
                defaultToNull = true)
        public File getDeepPheAnaforaDirectory();

        @Option(
                longName = "xmi-dir",
                defaultValue = "target/xmi",
                description = "directory to store and load XMI serialization of annotations")
        public File getXMIDirectory();

        @Option(
                longName = "generate-xmi",
                description = "read in the gold annotations and serialize them as XMI")
        public boolean getGenerateXMI();
    }
}
