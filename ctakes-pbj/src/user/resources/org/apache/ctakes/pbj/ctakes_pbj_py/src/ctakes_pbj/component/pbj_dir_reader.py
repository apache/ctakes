import os
from ctakes_pbj.component.collection_reader import CollectionReader
from ctakes_pbj.type_system.type_system_loader import *
from cassis import Cas

import logging


logger = logging.getLogger(__name__)


class PBJDirReader(CollectionReader):

    def __init__(self):
        self.input_dir = None
        self.typesystem = None
        self.pipeline = None

    # Set the pipeline.  The collection reader controls the pipeline flow.
    def set_pipeline(self, pipeline):
        self.pipeline = pipeline

    # Called once at the build of a pipeline.
    def declare_params(self, arg_parser):
        arg_parser.add_arg('-i', '--input_dir')

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, args):
        if args.input_dir is None:
            logger.error(
                "A directory from which files will be read must be specified. "
                "Use -i or --input_dir"
            )
            logger.critical("PBJ python pipeline has failed and cannot continue.")
            exit(1)
        self.input_dir = args.input_dir

    # Called start reading cas objects and pass them to the pipeline.
    def start(self):
        logger.info(f"Starting Directory Reader on {self.input_dir}...")
        for filename in os.listdir(self.input_dir):
            filepath = os.path.join(self.input_dir, filename)
            if os.path.isfile(filepath):
                with open(filepath, 'r') as f:
                    content = f.read()
                    cas = Cas(
                        sofa_string=content,
                        document_language="en",
                        typesystem=self.get_typesystem())
                    self.pipeline.process(cas)
        logger.info(f"Finished reading {self.input_dir}.")
        self.stop()

    # Called to stop reading.
    def stop(self):
        # logger.info("%s PBJ Dir Reader: Stopping Pipeline ...", time.ctime())
        self.pipeline.collection_process_complete()

    def set_typesystem(self, typesystem):
        self.typesystem = typesystem

    def get_typesystem(self):
        if self.typesystem is None:
            # Load the typesystem
            type_system_accessor = TypeSystemLoader()
            type_system_accessor.load_type_system()
            self.set_typesystem(type_system_accessor.get_type_system())
        return self.typesystem
