import warnings
import logging

from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
from ctakes_pbj.component.pbj_receiver import PBJReceiver
from ctakes_pbj.component.pbj_sender import PBJSender

from ctakes_cnlpt.ae.negation_delegator import NegationDelegator

warnings.filterwarnings("ignore")

logger = logging.getLogger(__name__)


def main():

    logger.info('If you are running this negation pipeline without a GPU then it may take several minutes to finish.')

    # Create a new PBJ Pipeline, add a class that interacts with cNLPT to add Negation to Events.
    pipeline = PBJPipeline()
    pipeline.reader(PBJReceiver())
    pipeline.add(NegationDelegator())
    # Add a PBJ Sender to the end of the pipeline to send the processed cas back to cTAKES and initialize the pipeline.
    pipeline.add(PBJSender())
    pipeline.run()


main()

