import create_type
from ctakes_pbj.type_system import ctakes_types


def get_annotation_at_offset(annotations, a_begins, wanted_begin):
    i = 0
    for b in a_begins:
        if b == wanted_begin:
            return annotations[i]
        i += 1
    return None


def get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type):
    i = 0
    for b in a_begins:
        if b == wanted_begin and isinstance(annotations, wanted_type):
            return annotations[i]
        i += 1
    return None


def get_or_create_event_mention(cas, annotations, a_begins, wanted_begin, wanted_type, end):
    event_mention = get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type)
    if event_mention is not None:
        return event_mention
    event_men_type = cas.typesystem.get_type(ctakes_types.EventMention)
    return create_type.add_type(cas, event_men_type, wanted_begin, end)


def get_or_create_procedure_mention(cas, annotations, a_begins, wanted_begin, wanted_type, end):
    procedure = get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type)
    if procedure is not None:
        return procedure
    event_men_type = cas.typesystem.get_type(ctakes_types.ProcedureMention)
    return create_type.add_type(cas, event_men_type, wanted_begin, end)


def get_or_create_sign_symptom_mention(cas, annotations, a_begins, wanted_begin, wanted_type, end):
    sign_symptom = get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type)
    if sign_symptom is not None:
        return sign_symptom
    event_men_type = cas.typesystem.get_type(ctakes_types.SignSymptomMention)
    return create_type.add_type(cas, event_men_type, wanted_begin, end)


def get_or_create_disease_disorder_mention(cas, annotations, a_begins, wanted_begin, wanted_type, end):
    disease_disorder = get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type)
    if disease_disorder is not None:
        return disease_disorder
    event_men_type = cas.typesystem.get_type(ctakes_types.DiseaseDisorderMention)
    return create_type.add_type(cas, event_men_type, wanted_begin, end)


def get_or_create_medication_mention(cas, annotations, a_begins, wanted_begin, wanted_type, end):
    medication = get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type)
    if medication is not None:
        return medication
    event_men_type = cas.typesystem.get_type(ctakes_types.MedicationMention)
    return create_type.add_type(cas, event_men_type, wanted_begin, end)


def get_or_create_anatomic_mention(cas, annotations, a_begins, wanted_begin, wanted_type, end):
    anatomic = get_annotation_type_at_offset(annotations, a_begins, wanted_begin, wanted_type)
    if anatomic is not None:
        return anatomic
    event_men_type = cas.typesystem.get_type(ctakes_types.AnatomicalSiteMention)
    return create_type.add_type(cas, event_men_type, wanted_begin, end)

