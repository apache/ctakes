import asyncio
import time

import cnlpt.api.negation_rest as negation_rest
from cnlpt.api.cnlp_rest import EntityDocument
from ctakes_pbj.component import cas_annotator
from ctakes_pbj.pbj_tools.helper_functions import *
from ctakes_pbj.type_system import ctakes_types

sem = asyncio.Semaphore(1)


async def init_caller():
    await negation_rest.startup_event()


async def negation_caller(cas, text, event_mentions, offsets):
    e_doc = EntityDocument(doc_text=text, entities=offsets)
    # async with sem:
    negation_output = await negation_rest.process(e_doc)
    i = 0
    for e in event_mentions:
        # -1 represents that it had happened, 1 represents that it is negated
        e.polarity = negation_output.statuses[i] * -1
        i += 1


def a_sort_by(a):
    return a.begin


class NegationDelegator(cas_annotator.CasAnnotator):

    # Initializes the cNLPT, which loads its Negation model.
    def initialize(self):
        print(time.ctime((time.time())), "Initializing cnlp-transformers negation ...")
        asyncio.run(init_caller())
        print(time.ctime((time.time())), "Done.")

    # Processes the document to get Negation on Events from cNLPT.
    def process(self, cas):
        print(time.ctime((time.time())), "Processing cnlp-transformers negation ...")
        # print(time.ctime((time.time())), "Processing cnlp-transformers negation on", get_document_id(cas), "...")
        sentences = cas.select(ctakes_types.Sentence)
        event_mentions = cas.select(ctakes_types.EventMention)
        event_mentions.sort(key=a_sort_by)
        sentence_events = get_covered_list(sentences, event_mentions)

        i = 0
        while i < len(sentences):
            if len(sentence_events[i]) > 0:
                print(time.ctime((time.time())), "Processing cnlp-transformers negation on sentence",
                      str(i), "of", str(len(sentences)), "...")
                offsets = get_windowed_offsets(sentence_events[i], sentences[i].begin)
                asyncio.run(negation_caller(cas, sentences[i].get_covered_text(), sentence_events[i], offsets))
            i += 1
        print(time.ctime((time.time())), "cnlp-transformers negation Done.")
