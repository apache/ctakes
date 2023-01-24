import argparse
DEFAULT_HOST = 'localhost'
DEFAULT_PORT = 61616
DEFAULT_USER = 'guest'
DEFAULT_PASS = 'guest'


def get_args():
    parser = argparse.ArgumentParser(
        prog='pbj_sender.py',
        description='Sends...',
        epilog='Text at the bottom of help'
    )
    parser.add_argument('receive_queue')
    parser.add_argument('send_queue')
    parser.add_argument('-hn', '--host_name', default=DEFAULT_HOST)
    parser.add_argument('-pn', '--port_name', default=DEFAULT_PORT)
    parser.add_argument('-u', '--username', default=DEFAULT_USER)
    parser.add_argument('-p', '--password', default=DEFAULT_PASS)

    parser.parse_args()
    args = parser.parse_args()

    return args


