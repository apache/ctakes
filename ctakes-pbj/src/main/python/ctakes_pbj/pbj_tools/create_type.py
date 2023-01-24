# Create and add a Type
def create_type(cas, type_name, begin, end):
    type_type = cas.typesystem.get_type(type_name)
    return add_type(cas, type_type, begin, end)


#  Add a type.  If a known type is repeatedly added then it is faster to get the type from the type system once
#  and call this function repeatedly with the given type.
def add_type(cas, type_type, begin, end):
    ts_type = type_type(begin=begin, end=end)
    cas.add(ts_type)
    return ts_type


def create_annotations(cas, type_name, offsets_list):
    a_type = cas.typesystem.get_type(type_name)
    annotations = []
    for offsets in offsets_list:
        a = add_type(cas, a_type, offsets[0], offsets[1])
        annotations.append(a)
    return annotations
