import time
import threading
from threading import Event
import logging

import stomp

from ctakes_pbj.pbj_tools.pbj_defaults import STOP_MESSAGE
from ctakes_pbj.type_system.type_system_loader import *


# exit_event = Event()


# def start_receiver(pipeline, queue_name, host_name, port_name,
#                    password, username, r_id='1'):
#     StompReceiver(pipeline, queue_name, host_name, port_name, password, username, r_id)
#     while not exit_event.is_set():
#         exit_event.wait()

# def start_receiver():
#     while not exit_event.is_set():
#         exit_event.wait()

# def start_receiver(stomp_receiver):
#     stomp_receiver.start_receiver()
# while not exit_event.is_set():
#     exit_event.wait()
# wait_thread = threading.Thread(target=run_receiver(stomp_receiver))
# wait_thread.start()


# def run_receiver(stomp_receiver):
#     stomp_receiver.start_receiver()
#     while not exit_event.is_set():
#         exit_event.wait()


# def stop_receiver():
#     exit_event.set()

logger = logging.getLogger(__name__)


class StompReceiver(stomp.ConnectionListener):

    def __init__(self, pipeline, queue_name, host_name, port_name, password, username, r_id):
        self.stop = False
        self.conn = None
        self.source_queue = queue_name
        self.source_host = host_name
        self.source_port = port_name
        self.pipeline = pipeline
        self.password = password
        self.username = username
        self.r_id = r_id
        self.typesystem = None
        self.completed = False
        # print(time.ctime((time.time())), "Starting Stomp Receiver on", self.source_host, self.source_queue, "...")
        # # Use a heartbeat of 10 minutes  (in milliseconds)
        # self.conn = stomp.Connection12([(self.source_host, self.source_port)],
        #                                keepalive=True, heartbeats=(600000, 600000))
        # self.conn.set_listener('Stomp_Receiver', self)
        # self.stop = False
        # self.__connect_and_subscribe()

    def start_receiver(self):
        logger.info(f"{time.ctime()} Starting Stomp Receiver on f{self.source_host} f{self.source_queue} ...")
        # Use a heartbeat of 10 minutes  (in milliseconds)
        self.conn = stomp.Connection12([(self.source_host, self.source_port)],
                                       keepalive=True, heartbeats=(600000, 600000))
        self.conn.set_listener('Stomp_Receiver', self)
        self.stop = False
        self.__connect_and_subscribe()

    def __connect_and_subscribe(self):
        if not self.stop:
            self.conn.connect(self.username, self.password, wait=True)
            self.conn.subscribe(destination=self.source_queue, id=self.r_id, ack='auto')
        # self.conn.subscribe(destination=self.source_queue, id=self.id, ack='client')

    def set_typesystem(self, typesystem):
        self.typesystem = typesystem

    def get_typesystem(self):
        if self.typesystem is None:
            # Load the typesystem
            type_system_accessor = TypeSystemLoader()
            type_system_accessor.load_type_system()
            self.set_typesystem(type_system_accessor.get_type_system())
        return self.typesystem

    def set_host(self, host_name):
        self.source_host = host_name

    def set_stop(self, stop):
        self.stop = stop

    def stop_receiver(self):
        self.stop = True
        logger.info(f"{time.ctime()} Disconnecting Stomp Receiver on f{self.source_host} f{self.source_queue} ...")
        self.conn.disconnect()
        logger.info(f"{time.ctime()} Stomp Receiver disconnected.")
        self.conn.unsubscribe(destination=self.source_queue, id=self.r_id)
        logger.info(f"{time.ctime()} Stomp Receiver unsubscribed.")
        # self.conn.disconnect()
        # print(time.ctime((time.time())), "Stomp Receiver disconnected.")
        if self.completed:
            self.pipeline.collection_process_complete()

    def on_message(self, frame):
        if frame.body == STOP_MESSAGE:
            logger.info(f"{time.ctime()} Received Stop code.")
            # self.stop = True
            # time.sleep(3)
            self.completed = True
            self.stop_receiver()
        else:
            if XMI_INDICATOR in frame.body:
                cas = cassis.load_cas_from_xmi(frame.body, self.get_typesystem())
                self.pipeline.process(cas)
            else:
                logger.error(f"{time.ctime()} Malformed Message: f{frame.body}")

    def on_disconnected(self):
        if not self.stop:
            self.__connect_and_subscribe()

    def on_error(self, frame):
        logger.error(f"{time.ctime()} Receiver Error: f{frame.body}")

    # Called when an exception is thrown.
    def handle_exception(self):
        self.completed = False
        self.stop_receiver()
