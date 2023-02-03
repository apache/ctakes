from ctakes_pbj.component.cas_annotator import *
from ctakes_pbj.pbj_tools.create_type import *
from ctakes_pbj.type_system.ctakes_types import *


class WordFinder(CasAnnotator):

    def process(self, cas):

        print('Checking Doc', cas.select(DocumentID))

        #  While we could use ct.create_type to create and add types, for each type lookup the cas array is searched.
        #  So it is faster to get the types first and then create instances with ct.add_type
        anatomy_type = cas.typesystem.get_type(AnatomicalSiteMention)
        symptom_type = cas.typesystem.get_type(SignSymptomMention)
        procedure_type = cas.typesystem.get_type(ProcedureMention)

        sites = ['breast']
        findings = ['hernia', 'pain', 'migraines', 'allergies']
        procedures = ['thyroidectomy', 'exam']

        for segment in cas.select(Segment):
            text = segment.get_covered_text()
            for word in sites:
                begin = text.find(word)
                if begin > -1:
                    print("found Anatomic Site")
                    end = begin + len(word)
                    add_type(cas, anatomy_type, begin, end)
            for word in findings:
                begin = text.find(word)
                if begin > -1:
                    print("found Sign or Symptom")
                    end = begin + len(word)
                    add_type(cas, symptom_type, begin, end)
            for word in procedures:
                begin = text.find(word)
                if begin > -1:
                    print("found Procedure")
                    end = begin + len(word)
                    add_type(cas, procedure_type, begin, end)
