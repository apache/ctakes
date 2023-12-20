import sys
from ctakes_pbj.component.collection_reader import CollectionReader
from ctakes_pbj.pbj_tools import pbj_defaults
from ctakes_pbj.pbj_tools.stomp_receiver import start_receiver


class PBJReceiver(CollectionReader):

    def __init__(self):
        self.queue = None
        self.host = None
        self.port = None
        self.password = None
        self.username = None
        self.pipeline = None
        self.receiving = False
        self.stomp_receiver = None

    # Set the pipeline.  The collection reader controls the pipeline flow.
    def set_pipeline(self, pipeline):
        self.pipeline = pipeline

    # Called once at the build of a pipeline.
    def declare_params(self, arg_parser):
        arg_parser.add_arg('-rq', '--receive_queue')
        arg_parser.add_arg('-rh', '--receive_host', default=pbj_defaults.DEFAULT_HOST)
        arg_parser.add_arg('-rpt', '--receive_port', default=pbj_defaults.DEFAULT_PORT)
        arg_parser.add_arg('-ru', '--receive_user', default=pbj_defaults.DEFAULT_USER)
        arg_parser.add_arg('-rp', '--receive_pass', default=pbj_defaults.DEFAULT_PASS)

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, args):
        if args.receive_queue is None:
            print('A queue from which information will be received must be specified.  Use -rq or --receive_queue')
            print('Indication that this PBJ python pipeline has failed cannot be sent.')
            print('You must manually stop any processes dependent upon this pipeline.')
            sys.exit()
        self.queue = args.receive_queue
        self.host = args.receive_host
        self.port = args.receive_port
        self.username = args.receive_user
        self.password = args.receive_pass

    # Called start reading cas objects and pass them to the pipeline.
    def start(self):
        if not self.receiving:
            self.receiving = True
            start_receiver(self.pipeline, self.queue, self.host, self.port,
                           self.password, self.username)
