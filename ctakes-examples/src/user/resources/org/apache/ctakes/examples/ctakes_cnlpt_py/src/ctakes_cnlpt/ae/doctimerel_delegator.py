import asyncio
import time

import cnlpt.api.dtr_rest as dtr_rest
from cnlpt.api.cnlp_rest import EntityDocument
from ctakes_pbj.component import cas_annotator
from ctakes_pbj.pbj_tools.event_creator import EventCreator
from ctakes_pbj.pbj_tools.helper_functions import *
from ctakes_pbj.type_system import ctakes_types

sem = asyncio.Semaphore(1)


class DocTimeRelDelegator(cas_annotator.CasAnnotator):

    def __init__(self, cas):
        self.event_creator = EventCreator(cas)
        self.event_mention_type = cas.typesystem.get_type(ctakes_types.EventMention)

    # Initializes cNLPT, which loads its DocTimeRel model.
    def initialize(self):
        print(time.ctime((time.time())), "Initializing cnlp-transformers doctimerel ...")
        asyncio.run(self.init_caller())
        print(time.ctime((time.time())), "Done.")

    # Processes the document to get DocTimeRel on Events from cNLPT.
    def process(self, cas):
        print(time.ctime((time.time())), "Processing cnlp-transformers doctimerel ...")
        event_mentions = cas.select(ctakes_types.EventMention)
        offsets = get_offsets(event_mentions)
        asyncio.run(self.dtr_caller(cas, event_mentions, offsets))
        print(time.ctime((time.time())), "cnlp-transformers doctimerel Done.")

    async def init_caller(self):
        await dtr_rest.startup_event()

    async def dtr_caller(self, cas, event_mentions, offsets):
        text = cas.sofa_string
        e_doc = EntityDocument(doc_text=text, annotations=offsets)

        #async with sem:
        dtr_output = await dtr_rest.process(e_doc)
        i = 0
        for e in event_mentions:
            event = self.event_creator.create_event(cas, dtr_output.statuses[i])
            e.event = event
            i += 1
