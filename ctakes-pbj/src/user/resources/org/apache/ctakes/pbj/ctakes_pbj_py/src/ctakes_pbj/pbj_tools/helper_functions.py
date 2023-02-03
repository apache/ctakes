
# same as create offset function that was written in trello
from ctakes_pbj.pbj_tools import create_type
from ctakes_pbj.type_system import ctakes_types


def create_offset(annotations):
    offsets = []
    for a in annotations:
        offsets.append([a.begin, a.end])
    return offsets


def get_event_mention(cas, e_mentions, e_m_begins, begin, end):
    i = 0
    for b in e_m_begins:
        if b == begin:
            return e_mentions[i]
        i += 1
    i = 0
    event_men_type = cas.typesystem.get_type(ctakes_types.Procedure)
    return create_type.add_type(cas, event_men_type, begin, end)

