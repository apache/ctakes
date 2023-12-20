import sys
import time
import stomp

from ctakes_pbj.component import cas_annotator
from ctakes_pbj.pbj_tools.pbj_defaults import *


class PBJSender(cas_annotator.CasAnnotator):

    def __init__(self):
        self.queue = None
        self.host = None
        self.port = None
        self.password = None
        self.username = None
        self.conn = None

    # Called once at the build of a pipeline.
    def declare_params(self, arg_parser):
        # arg_parser.add_arg('send_queue')
        arg_parser.add_arg('-sq', '--send_queue', default=DEFAULT_HOST)
        arg_parser.add_arg('-sh', '--send_host', default=DEFAULT_HOST)
        arg_parser.add_arg('-spt', '--send_port', default=DEFAULT_PORT)
        arg_parser.add_arg('-su', '--send_user', default=DEFAULT_USER)
        arg_parser.add_arg('-sp', '--send_pass', default=DEFAULT_PASS)

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, args):
        if args.send_queue is None:
            print('A queue to which information will be sent must be specified.  Use -sq or --send_queue')
            print('Indication that this PBJ python pipeline has failed cannot be sent to an unknown queue.')
            print('You must manually stop any processes dependent upon this pipeline.')
            sys.exit()
        self.queue = args.send_queue
        self.host = args.send_host
        self.port = args.send_port
        self.username = args.send_user
        self.password = args.send_pass

    # Called once at the beginning of a pipeline.
    def initialize(self):
        print(time.ctime((time.time())), "Starting PBJ Sender on", self.host, self.queue, "...")
        # Use a heartbeat of 10 minutes  (in milliseconds)
        self.conn = stomp.Connection12([(self.host, self.port)],
                                       keepalive=True, heartbeats=(600000, 600000))
        self.conn.connect(self.username, self.password, wait=True)

    # Called for every cas passed through the pipeline.
    def process(self, cas):
        print(time.ctime((time.time())), "Sending processed information to",
              self.host, self.queue, "...")
        xmi = cas.to_xmi()
        self.conn.send(self.queue, xmi)

    # Called once at the end of the pipeline.
    def collection_process_complete(self):
        self.send_stop()

    def send_text(self, text):
        self.conn.send(self.queue, text)

    def send_stop(self):
        print(time.ctime((time.time())), "Sending Stop code to", self.host, self.queue, "...")
        self.conn.send(self.queue, STOP_MESSAGE)
        self.conn.disconnect()
        print(time.ctime((time.time())), "Disconnected PBJ Sender on", self.host, self.queue)

    def set_host(self, host_name):
        self.host = host_name

    def set_port(self, port_name):
        self.port = port_name

    def set_password(self, password):
        self.password = password

    def get_password(self):
        return self.password

    def set_username(self, username):
        self.username = username

    def get_username(self):
        return self.username
