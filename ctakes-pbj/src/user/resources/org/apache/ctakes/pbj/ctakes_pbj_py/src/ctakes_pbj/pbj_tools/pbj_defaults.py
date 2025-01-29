import sys
import os.path

DEFAULT_HOST = 'localhost'
DEFAULT_PORT = 61616
DEFAULT_USER = 'guest'
DEFAULT_PASS = 'guest'
DEFAULT_OUT_DIR = 'pbj_output/'
STOP_MESSAGE = "Apache cTAKES PBJ Stop Message."


# The default receive queue is the name of the script with the prefix "to_"
def get_default_rcv_q():
    return "to_" + os.path.splitext(os.path.basename(sys.argv[0]))[0]


# The default send queue is the name of the script with the prefix "from_"
def get_default_send_q():
    return "from_" + os.path.splitext(os.path.basename(sys.argv[0]))[0]
