import asyncio
import time

import cnlpt.api.temporal_rest as temporal_rest
from ctakes_pbj.component import cas_annotator
from ctakes_pbj.pbj_tools import create_type
from ctakes_pbj.pbj_tools.create_relation import create_relation
from ctakes_pbj.pbj_tools.event_creator import create_event
from ctakes_pbj.pbj_tools.helper_functions import *
from ctakes_pbj.pbj_tools.token_tools import *
from ctakes_pbj.type_system import ctakes_types

sem = asyncio.Semaphore(1)


class TemporalDelegator(cas_annotator.CasAnnotator):

    def __init__(self, cas):
        self.event_mention_type = cas.typesystem.get_type(ctakes_types.EventMention)
        self.timex_type = cas.typesystem.get_type(ctakes_types.TimeMention)
        self.tlink_type = cas.typesystem.get_type(ctakes_types.TemporalTextRelation)
        self.argument_type = cas.typesystem.get_type(ctakes_types.RelationArgument)

    # Initializes cNLPT, which loads its Temporal model.
    def initialize(self):
        print(time.ctime((time.time())), "Initializing cnlp-transformers temporal ...")
        asyncio.run(self.init_caller())
        print(time.ctime((time.time())), "Done.")

    # Process Sentences, adding Times, Events and TLinks found by cNLPT.
    def process(self, cas):
        print(time.ctime((time.time())), "Processing cnlp-transformers temporal ...")
        sentences = cas.select(ctakes_types.Sentence)
        event_mentions = cas.select(ctakes_types.EventMention)
        sentence_events = get_covered_list(sentences, event_mentions)

        # e_m_begins = []
        # for e in e_mentions:
        #     e_m_begins.append(e.begin)

        tokens = cas.select(ctakes_types.BaseToken)
        sentence_tokens = get_covered_list(sentences, tokens)
        # token_begins = []
        # for t in tokens:
        #     token_begins.append(t.begin)

        i = 0
        while i < len(sentences):
            if len(sentence_events[i]) > 0:
                print(time.ctime((time.time())), "Processing cnlp-transformers temporal on sentence",
                      str(i), "of", str(len(sentences)), "...")
                event_offsets = get_windowed_offsets(sentence_events[i], sentences[i].begin)
                token_offsets = get_windowed_offsets(sentence_tokens[i], sentences[i].begin)
                asyncio.run(self.temporal_caller(cas, sentences[i], sentence_events[i], event_offsets, token_offsets))
            i += 1
        print(time.ctime((time.time())), "cnlp-transformers temporal Done.")

    async def init_caller(self):
        await temporal_rest.startup_event()

    async def temporal_caller(self, cas, sentence, event_mentions, event_offsets, token_offsets):

        sentence_doc = temporal_rest.SentenceDocument(sentence.get_covered_text())
        temporal_result = await temporal_rest.process_sentence(sentence_doc)

        events_times = {}
        i = 0
        for t in temporal_result.timexes:
            for tt in t:
                first_token_offset = token_offsets[tt.begin]
                last_token_offset = token_offsets[tt.end]
                timex = create_type.add_type(cas, self.timex_type,
                                             sentence.begin + first_token_offset[0],
                                             sentence.begin + last_token_offset[1])
                events_times['TIMEX-' + str(i)] = timex
                i += 1

        i = 0
        for e in temporal_result.events:
            for ee in e:
                first_token_offset = token_offsets[ee.begin]
                last_token_offset = token_offsets[ee.end]
                event_mention = get_or_create_event_mention(cas, event_mentions,
                                                            sentence.begin + first_token_offset[0],
                                                            sentence.begin + last_token_offset[1])
                event = create_event(cas, ee.dtr)
                event_mention.event = event
                events_times['EVENT-' + str(i)] = event_mention
                i += 1

        for r in temporal_result.relations:
            for rr in r:
                arg1 = self.argument_type()
                arg1.argument = events_times[rr.arg1]
                print("Arg1 =", events_times[rr.arg1])
                arg2 = self.argument_type()
                arg2.argument = events_times[rr.arg2]
                print("Arg2 =", events_times[rr.arg2])
                tlink = create_relation(self.tlink_type, rr.category, arg1, arg2)
                cas.add(tlink)

    def get_index_by_offsets(tokens, begin, end):
        i = 0
        for token in tokens:
            if token.begin == begin and token.end == end:
                return i
            i += 1
        return -1

    def get_or_create_event_mention(cas, event_mentions, begin, end):
        i = get_index_by_offsets(event_mentions, begin, end)
        if i == -1:
            return create_type.add_type(cas, self.event_mention_type, begin, end)
        return event_mentions[i]
