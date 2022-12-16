
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from fastapi import FastAPI
from pydantic import BaseModel

from typing import List, Tuple, Dict

from transformers import (
    AutoConfig,
    AutoModelForSequenceClassification,
    AutoTokenizer,
    HfArgumentParser,
    Trainer,
    TrainingArguments,
)
from transformers.data.processors.utils import InputFeatures, InputExample
from torch.utils.data.dataset import Dataset
from transformers.data.processors.glue import glue_convert_examples_to_features
import numpy as np

import logging
from time import time

app = FastAPI()
model_name = "tmills/roberta_sfda_sharpseed"
logger = logging.getLogger('Negation_REST_Processor')
logger.setLevel(logging.INFO)

labels = ["-1", "1"]
max_length = 128

class EntityDocument(BaseModel):
    ''' doc_text: The raw text of the document
    offset:  A list of entities, where each is a mapping from some identifier to the character offsets into doc_text for that entity'''
    doc_text: str
    entities: List[List[int]]

class NegationResults(BaseModel):
    ''' statuses: dictionary from entity id to classification decision about negation; true -> negated, false -> not negated'''
    statuses: List[int]

class NegationDocumentDataset(Dataset):
    def __init__(self, features):
        self.features = features
        self.label_list = ["-1", "1"]

    def __len__(self):
        return len(self.features)

    def __getitem__(self, i) -> InputFeatures:
        return self.features[i]

    def get_labels(self):
        return self.label_list

    @classmethod
    def from_instance_list(cls, inst_list, tokenizer):
        examples = []
        for (ind,inst) in enumerate(inst_list):
            guid = 'instance-%d' % (ind)
            examples.append(InputExample(guid=guid, text_a=inst, text_b='', label=None))

        features = glue_convert_examples_to_features(
            examples,
            tokenizer,
            max_length=max_length,
            label_list = labels,
            output_mode='classification'
        )
        return cls(features)

def create_instance_string(doc_text, offsets):
    start = max(0, offsets[0]-100)
    end = min(len(doc_text), offsets[1]+100)
    raw_str = doc_text[start:offsets[0]] + ' <e> ' + doc_text[offsets[0]:offsets[1]] + ' </e> ' + doc_text[offsets[1]:end]
    return raw_str.replace('\n', ' ')

@app.on_event("startup")
async def startup_event():
    args = ['--output_dir', 'save_run/', '--per_device_eval_batch_size', '128', '--do_predict']
    # training_args = parserTrainingArguments('save_run/')
    parser = HfArgumentParser((TrainingArguments,))
    training_args, = parser.parse_args_into_dataclasses(args=args)

    app.training_args = training_args
    
    # training_args.per_device_eval_size = 32
    logger.warn("Eval batch size is: " + str(training_args.eval_batch_size))

@app.post("/negation/initialize")
async def initialize():
    ''' Load the model from disk and move to the device'''
    config = AutoConfig.from_pretrained(model_name)
    app.tokenizer = AutoTokenizer.from_pretrained(model_name,
                                              config=config)
    model = AutoModelForSequenceClassification.from_pretrained(model_name,
                                                               config=config)
    model.to('cuda')

    app.trainer = Trainer(
            model=model,
            args=app.training_args,
            compute_metrics=None,
        )    

@app.post("/negation/process")
async def process(doc: EntityDocument):
    doc_text = doc.doc_text
    logger.warn('Received document of len %d to process with %d entities' % (len(doc_text), len(doc.entities)))
    instances = []
    start_time = time()

    for ent_ind, offsets in enumerate(doc.entities):
        # logger.debug('Entity ind: %d has offsets (%d, %d)' % (ent_ind, offsets[0], offsets[1]))
        inst_str = create_instance_string(doc_text, offsets)
        logger.debug('Instance string is %s' % (inst_str))
        instances.append(inst_str)

    dataset = NegationDocumentDataset.from_instance_list(instances, app.tokenizer)
    preproc_end = time()

    output = app.trainer.predict(test_dataset=dataset)
    predictions = output.predictions
    predictions = np.argmax(predictions, axis=1)

    pred_end = time()

    results = []
    for ent_ind in range(len(dataset)):
        results.append(dataset.get_labels()[predictions[ent_ind]])

    output = NegationResults(statuses=results)

    postproc_end = time()

    preproc_time = preproc_end - start_time
    pred_time = pred_end - preproc_end
    postproc_time = postproc_end - pred_end

    logging.warn("Pre-processing time: %f, processing time: %f, post-processing time %f" % (preproc_time, pred_time, postproc_time))
    
    return output


@app.post("/negation/collection_process_complete")
async def collection_process_complete():
    app.trainer = None

@app.get("/negation/{test_str}")
async def test(test_str: str):
    return {'argument': test_str}
