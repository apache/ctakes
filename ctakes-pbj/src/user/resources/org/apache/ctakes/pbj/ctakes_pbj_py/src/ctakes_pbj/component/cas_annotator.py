from abc import ABC, abstractmethod


class CasAnnotator(ABC):

    # Called once at the build of a pipeline.
    def declare_params(self, arg_parser):
        pass

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, arg_parser):
        pass

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

    # Called when an exception is thrown.
    def handle_exception(self, thrower, exceptable, initializing=False):
        pass
