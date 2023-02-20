
# same as create offset function that was written in trello
from ctakes_pbj.pbj_tools import create_type
from ctakes_pbj.type_system import ctakes_types


def get_offsets(annotations):
    offsets = []
    for a in annotations:
        offsets.append([a.begin, a.end])
    return offsets


def get_windowed_offsets(annotations, window_offset):
    offsets = []
    for a in annotations:
        offsets.append([a.begin-window_offset, a.end-window_offset])
    return offsets


def get_event_mention(cas, e_mentions, e_m_begins, begin, end):
    i = 0
    for b in e_m_begins:
        if b == begin:
            return e_mentions[i]
        i += 1
    event_men_type = cas.typesystem.get_type(ctakes_types.EventMention)
    return create_type.add_type(cas, event_men_type, begin, end)


def get_covered_list(to_cover_with, to_cover):
    cover_max = len(to_cover)
    covered_list = []
    i = 0
    for covering in to_cover_with:
        covered = []
        while i < cover_max:
            if to_cover[i].begin >= covering.begin and to_cover[i].end <= covering.end:
                covered.append(to_cover[i])
                i += 1
            else:
                break
        covered_list.append(covered)
    return covered_list


def get_document_id(cas):
    doc_ids = cas.select(ctakes_types.DocumentID)
    for doc_id in doc_ids:
        return doc_id.documentID
    return "Unknown Document"
