from abc import ABC, abstractmethod


class CasAnnotator(ABC):
    # Called once at the beginning of a pipeline.
    def initialize(self):
        pass

    # Called for every cas passed through the pipeline.
    @abstractmethod
    def process(self, cas):
        pass

    # Called once at the end of the pipeline.
    def collection_process_complete(self):
        pass
