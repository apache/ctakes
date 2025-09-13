import sys
import time
import threading
from ctakes_pbj.component.collection_reader import CollectionReader
from ctakes_pbj.pbj_tools import pbj_defaults
# from ctakes_pbj.pbj_tools.stomp_receiver import start_receiver
# from ctakes_pbj.pbj_tools.stomp_receiver import stop_receiver
from ctakes_pbj.pbj_tools.stomp_receiver import StompReceiver
import logging


logger = logging.getLogger(__name__)


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
        arg_parser.add_arg('-rq', '--receive_queue', default=pbj_defaults.get_default_rcv_q())
        arg_parser.add_arg('-rh', '--receive_host', default=pbj_defaults.DEFAULT_HOST)
        arg_parser.add_arg('-rpt', '--receive_port', default=pbj_defaults.DEFAULT_PORT)
        arg_parser.add_arg('-ru', '--receive_user', default=pbj_defaults.DEFAULT_USER)
        arg_parser.add_arg('-rp', '--receive_pass', default=pbj_defaults.DEFAULT_PASS)

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, args):
        if args.receive_queue is None:
            logger.error(
                "A queue from which information will be received must be specified. "
                "Use -rq or --receive_queue"
            )
            logger.critical("PBJ python pipeline has failed and cannot continue.")
            sys.exit()
        self.queue = args.receive_queue
        self.host = args.receive_host
        self.port = args.receive_port
        self.username = args.receive_user
        self.password = args.receive_pass

    # Called start reading cas objects and pass them to the pipeline.
    def start(self):
        if not self.receiving:
            # start_receiver(self.pipeline, self.queue, self.host, self.port,
            #                self.password, self.username)
            self.receiving = True
            self.stomp_receiver = StompReceiver(self.pipeline, self.queue, self.host, self.port,
                                                self.password, self.username, r_id='1')
            # stomp_thread = threading.Thread(target=start_receiver(self.stomp_receiver))
            # stomp_thread.start()
            # start_receiver(self.stomp_receiver)
            self.stomp_receiver.start_receiver()

    # Called to stop reading.
    def stop(self):
        logger.info("%s PBJ Receiver: Stopping Stomp receiver ...", time.ctime())
        self.stomp_receiver.stop_receiver()

    # Called when an exception is thrown.
    def handle_exception(self, thrower, exceptable, initializing=False):
        if self.receiving:
            # self.stop()
            # print(time.ctime(), "PBJ Receiver: Stopping Stomp receiver ...")
            # stop_receiver()
            # self.stomp_receiver.set_stop(True)
            self.receiving = False
            self.stomp_receiver.handle_exception()
            # Stop stomp in a background thread, just in case it is slow to react.
            # stop_thread = threading.Thread(target=self.stomp_receiver.handle_exception)
            # stop_thread.start()
