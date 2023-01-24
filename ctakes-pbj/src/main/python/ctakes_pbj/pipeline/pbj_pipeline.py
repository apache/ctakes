STOP_MESSAGE = "Apache cTAKES PBJ Stop Message."


class PBJPipeline:

    def __init__(self):
        self.annotators = []
        self.initialized = False

    def add(self, cas_processor):
        self.annotators.append(cas_processor)

    def initialize(self):
        for processor in self.annotators:
            self.initialized = True
            processor.initialize()

    def process(self, cas):
        if self.initialized:
            for processor in self.annotators:
                processor.process(cas)
        else:
            self.initialize()
            self.process(cas)

    def collection_process_complete(self):
        for processor in self.annotators:
            processor.collection_process_complete()
