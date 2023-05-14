import stomp
import time
from ctakes_pbj.component import cas_annotator
from ctakes_pbj.pbj_tools.pbj_defaults import *


class PBJSender(cas_annotator.CasAnnotator):

    def __init__(self):
        self.target_queue = None
        self.target_host = None
        self.target_port = None
        self.password = None
        self.username = None
        self.conn = None

    # Called once at the build of a pipeline.
    def declare_params(self, arg_parser):
        arg_parser.add_arg('send_queue')
        arg_parser.add_arg('-sh', '--send_host', default=DEFAULT_HOST)
        arg_parser.add_arg('-spt', '--send_port', default=DEFAULT_PORT)
        arg_parser.add_arg('-su', '--send_user', default=DEFAULT_USER)
        arg_parser.add_arg('-sp', '--send_pass', default=DEFAULT_PASS)

    # Called once at the beginning of a pipeline, before initialize.
    def init_params(self, args):
        self.target_queue = args.send_queue
        self.target_host = args.send_host
        self.target_port = args.send_port
        self.username = args.send_user
        self.password = args.send_pass

    # Called once at the beginning of a pipeline.
    def initialize(self):
        print(time.ctime((time.time())), "Starting PBJ Sender on", self.target_host, self.target_queue, "...")
        # Use a heartbeat of 10 minutes  (in milliseconds)
        self.conn = stomp.Connection12([(self.target_host, self.target_port)],
                                       keepalive=True, heartbeats=(600000, 600000))
        self.conn.connect(self.username, self.password, wait=True)

    # Called for every cas passed through the pipeline.
    def process(self, cas):
        print(time.ctime((time.time())), "Sending processed information to",
              self.target_host, self.target_queue, "...")
        xmi = cas.to_xmi()
        self.conn.send(self.target_queue, xmi)

    # Called once at the end of the pipeline.
    def collection_process_complete(self):
        self.send_stop()

    def send_text(self, text):
        self.conn.send(self.target_queue, text)

    def send_stop(self):
        print(time.ctime((time.time())), "Sending Stop code to", self.target_host, self.target_queue, "...")
        self.conn.send(self.target_queue, STOP_MESSAGE)
        self.conn.disconnect()
        print(time.ctime((time.time())), "Disconnected PBJ Sender on", self.target_host, self.target_queue)

    def set_queue(self, queue_name):
        self.target_queue = queue_name

    def set_host(self, host_name):
        self.target_host = host_name

    def set_port(self, port_name):
        self.target_port = port_name

    def set_password(self, password):
        self.password = password

    def get_password(self):
        return self.password

    def set_username(self, username):
        self.username = username

    def get_username(self):
        return self.username
