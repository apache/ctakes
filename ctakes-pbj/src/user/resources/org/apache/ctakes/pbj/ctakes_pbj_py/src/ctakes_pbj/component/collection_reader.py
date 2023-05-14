from abc import ABC, abstractmethod


class CollectionReader(ABC):

    # Set the pipeline.  The collection reader controls the pipeline flow.
    @abstractmethod
    def set_pipeline(self, pipeline):
        pass

    # Called once at the build of a pipeline.
    def declare_params(self, arg_parser):
        pass

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, arg_parser):
        pass

    # Called once at the beginning of a pipeline.
    def initialize(self):
        pass

    # Called start reading cas objects and pass them to the pipeline.
    @abstractmethod
    def start(self):
        pass
