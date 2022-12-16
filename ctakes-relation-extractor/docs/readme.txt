# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

Basic Architechture
-------------------

The relation extraction module is capable of detecting two types of UMLS relations between identified annotation (entities and modifiers): LocationOf and DegreeOf. The relation module operates by pairing identified annotations and assigning a relation label to the pairs. To perform this task, the module uses three UIMA-compliant analysis engines (AEs):

1. AE for detecting instances of the LocationOf relation which may exist between entity mentions. E.g. LocationOf("joint", "joint pain").

2. AE for detecting instances of the DegreeOf relation which may exist between entity mentions and modifiers. E.g. DegreeOf("chronic", "headache").

3. AE for detecting modifiers (e.g. "severe", "chronic")

The module provides the descriptor files for detecting modifiers and extracting the relations. The module also provides an aggreagate AE descriptor which defines the full pipeline that is necessary for extracting relations. The latter includes the preprocessing that is required for relation extraction and the AEs that discover relations.

Please see the ae package for details.

Features
--------

The relation module uses lexical, syntactic, and semantic features to encode potential relation instances.

Please see the ae.features package for details.

Training models
---------------

To train a new model, perform the following sequence of steps:

1. Run PreprocessAndWriteXmi in the eval package, specifying the location of the text of the notes, the location of the gold standard relation annotations, and the output directory. This class will run all the preprocessing that is required for relation extraction and add gold standard relation annotations to the CAS. The resulting CASes will be saved to disk as XMI files.

2. Run RelationExtractorEvaluation, passing to it the location of the XMI files obtained in the previous steps and --grid-search option. This class will use the annotations in the XMI files to find the optimal training parameters using grid search and n-fold cross-validation. After the execution completes, record the best set of parameters found by the grid search.

3. Update the model parameters in the main() method of RelationExtractorTrain (pipelines package) to the values found by the grid search. Run RelationExtractorTrain, specifying the location of the XMI files. This class will (a) create a model that is necessary for deployment of the relation module, and (b) create the descriptor files which will ensure that the the relation AEs can be used as a part of a UIMA pipeline.

Extracting Relations
--------------------

RelationExtractorPipeline in the pipelines package is a good example of using the relation extraction module. It defines a pipeline consisting of a collection reader that reads all files in a directory, the aggregate relation extraction AE that discovers relations, and a writer that saves the resulting annotations as xmi files.

In the same package, a sample pipeline for consuming relation annotations is also provided. Please see RelationExtractorAnalysis class for details.
