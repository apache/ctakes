import sys
import time
import traceback
from traceback import TracebackException
from threading import Event

from ctakes_pbj.pbj_tools import pbj_defaults
from ctakes_pbj.pbj_tools.arg_parser import ArgParser

exit_event = Event()


class PBJPipeline:

    def __init__(self):
        self.annotators = []
        self.initialized = False
        self.c_reader = None
        self.arg_parser = ArgParser()

    # Set the Collection Reader for the Corpus.
    # This is absolutely necessary.  If you don't tell the pipeline how to get the notes ...
    def reader(self, collection_reader):
        collection_reader.declare_params(self.arg_parser)
        collection_reader.set_pipeline(self)
        self.c_reader = collection_reader

    # Add an annotator to the pipeline.
    def add(self, cas_annotator):
        cas_annotator.declare_params(self.arg_parser)
        self.annotators.append(cas_annotator)

    # Fill command line parameters, then call each annotator to initialize.
    def initialize(self):
        if self.c_reader is None:
            print('No Reader Specified, quitting')
            exit(1)
        self.arg_parser.add_arg('-o', '--output_dir', default=pbj_defaults.DEFAULT_OUT_DIR)
        # Get/Init all of the declared parameter arguments.
        # Do the actual argument parsing.
        # If get_args has already been called then added parameters will crash the tool.
        args = self.arg_parser.get_args()
        # Set the necessary parameters in the collection reader.
        try:
            self.c_reader.init_params(args)
        except Exception as exceptable:
            self.handle_exception(self.c_reader, exceptable, True)
        # For each annotator set the necessary parameters.
        for annotator in self.annotators:
            annotator.init_params(args)
        # For each annotator initialize resources, etc.
        for annotator in self.annotators:
            if exit_event.is_set():
                break
            try:
                print(time.ctime(), "Initializing", type(annotator).__name__, "...", flush=True)
                annotator.initialize()
            except Exception as exceptable:
                self.handle_exception(annotator, exceptable, True)
        self.initialized = True

    # Starts / Runs the pipeline.  This calls start on the collection reader.
    def run(self):
        if not self.initialized:
            self.initialize()
        try:
            print(time.ctime(), "Starting", type(self.c_reader).__name__, "...", flush=True)
            self.c_reader.start()
        except Exception as exceptable:
            self.handle_exception(self.c_reader, exceptable)
        # Start a second thread that does nothing.
        # It will allow the collection reader to wait for information.
        while not exit_event.is_set():
            exit_event.wait()

    # For a new cas, call each annotator to process that cas.
    def process(self, cas):
        for annotator in self.annotators:
            if exit_event.is_set():
                break
            try:
                print(time.ctime(), "Running", type(annotator).__name__, "...", flush=True)
                annotator.process(cas)
            except Exception as exceptable:
                self.handle_exception(annotator, exceptable)

    # At the end of the corpus, call each annotator for cleanup, etc.
    def collection_process_complete(self):
        print(time.ctime(), "Collection processing complete.")
        for annotator in self.annotators:
            if exit_event.is_set():
                break
            try:
                print(time.ctime(), "Notifying", type(annotator).__name__, "of completion ...", flush=True)
                annotator.collection_process_complete()
            except Exception as exceptable:
                self.handle_exception(annotator, exceptable)
        print(time.ctime(), "Done.", flush=True)
        exit_event.set()

    def handle_exception(self, thrower, exceptable, initializing=False):
        print(time.ctime(), "Exception thrown in",
              type(thrower).__name__, ":",
              type(exceptable).__name__, exceptable, flush=True)
        # print(TracebackException.from_exception(exceptable, limit=-3, capture_locals=True))
        traceback.print_exc(limit=3, chain=False)
        # Print nice output regarding the exception.
        try:
            print(time.ctime(), "Notifying", type(self.c_reader).__name__, "of exception ...", flush=True)
            self.c_reader.handle_exception(thrower, exceptable)
        except Exception as exceptable_2:
            traceback.print_exc(limit=3, chain=False)
        # Distribute handling of exceptions.
        for annotator in self.annotators:
            try:
                print(time.ctime(), "Notifying", type(annotator).__name__, "of exception ...", flush=True)
                annotator.handle_exception(thrower, exceptable, initializing)
            except Exception as exceptable_2:
                traceback.print_exc(limit=3, chain=False)
        print(time.ctime(), "Done.", flush=True)
        # sys.exit(1)
        exit_event.set()
