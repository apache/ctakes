import sys
import os
from ctakes_pbj.component.collection_reader import CollectionReader
from ctakes_pbj.type_system.type_system_loader import *

import logging


logger = logging.getLogger(__name__)


class PBJDirReader(CollectionReader):

    def __init__(self):
        self.input_dir = None
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
