from ctakes_pbj.type_system import ctakes_types


def create_relation(cas, relation_type, category, source, target):
    relation = relation_type()
    relation.category = category
    relation_arg_type = cas.typesystem.get_type(ctakes_types.RelationArgument)
    relation.arg1 = relation_arg_type()
    relation.arg2 = relation_arg_type()
    relation.arg1.argument = source
    relation.arg2.argument = target
    cas.add(relation)
    return relation
