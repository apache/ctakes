from ctakes_pbj.component import cas_annotator
from ctakes_pbj.type_system import ctakes_types
import asyncio
from cnlpt.api.cnlp_rest import EntityDocument
import cnlpt.api.negation_rest as negation_rest
import time
from ctakes_pbj.pbj_tools.helper_functions import *

sem = asyncio.Semaphore(1)


class NegationDelegator(cas_annotator.CasAnnotator):

    # Initializes the cNLPT, which loads its Negation model.
    def initialize(self):
        print("Initializing cnlp-transformers negation " + str(time.time()) + " ...")
        asyncio.run(self.init_caller())
        print("Done " + str(time.time()))

    # Processes the document to get Negation on Events from cNLPT.
    def process(self, cas):
        event_mentions = cas.select(ctakes_types.EventMention)
        offsets = create_offset(event_mentions)

        print("Calling cnlp-transformers negation " + str(time.time()) + " ...")
        asyncio.run(self.negation_caller(cas, event_mentions, offsets))
        print("Done " + str(time.time()))

    # def process2(self, cas):
    #     sentences = cas.select(ctakes_types.Sentence)
    #     event_mentions = cas.select(ctakes_types.EventMention)
    #     print("Calling cnlp-transformers negation " + str(time.time()) + " ...")
    #     asyncio.run(self.negation_caller2(cas, sentences, event_mentions))
    #     print("Done " + str(time.time()))

    async def init_caller(self):
        await negation_rest.startup_event()

    async def negation_caller(self, cas, event_mentions, offsets):
        text = cas.sofa_string
        eDoc = EntityDocument(doc_text=text, entities=offsets)

        #async with sem:
        negation_output = await negation_rest.process(eDoc)
        i = 0
        for e in event_mentions:
            # -1 represents that it had happened, 1 represents that it is negated
            e.polarity = negation_output.statuses[i] * -1
            i += 1


    # async def negation_caller2(self, cas, sentences, event_mentions):
    #     for sentence in sentences:
    #         text = sentence.get_covered_text()
    #
    #     eDoc = EntityDocument(doc_text=text, entities=offsets)
    #
    #     #async with sem:
    #     negation_output = await negation_rest.process(eDoc)
    #     i = 0
    #     for e in event_mentions:
    #         # -1 represents that it had happened, 1 represents that it is negated
    #         e.polarity = negation_output.statuses[i] * -1
    #         i += 1
