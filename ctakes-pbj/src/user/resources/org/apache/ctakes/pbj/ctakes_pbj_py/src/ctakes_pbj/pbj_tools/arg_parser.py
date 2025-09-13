import argparse
import logging

logger = logging.getLogger(__name__)

class ArgParser:

    def __init__(self):
        self.arg_parser = None

    def get_arg_parser(self):
        if self.arg_parser is None:
            self.arg_parser = argparse.ArgumentParser(
                prog='ctakes-pbj',
                description='Does wonderful stuff...',
                epilog='Text at the bottom of help'
            )
        return self.arg_parser

    def add_arg(self, *args, **kwargs):
        self.get_arg_parser().add_argument(*args, **kwargs)

    def get_args(self):
        logger.info('Parsing Arguments ...')
        return self.get_arg_parser().parse_args()
