import cassis
XMI_INDICATOR = "xmlns:xmi"
CTAKES_TYPE_SYSTEM = "resources/org/apache/ctakes/pbj/types/TypeSystem.xml"


class TypeSystemLoader:

    def __init__(self, type_system_file=CTAKES_TYPE_SYSTEM):
        self.typesystem = None
        self.type_system_file = type_system_file

    def load_type_system(self):
        if self.typesystem is None:
            print("loading typesystem ...")
            with open(self.type_system_file, 'rb') as f:
                self.typesystem = cassis.load_typesystem(f)

    def get_type_system(self):
        self.load_type_system()
        return self.typesystem
