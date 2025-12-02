# Should accept cmd line parameters such as: hostname, port, queue name for recieving cas, and queue name for
# sending cas

# These are the lines that ignore the typesystem errors.
import warnings

from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
from ctakes_pbj.component.pbj_receiver import PBJReceiver
from ctakes_pbj.component.pbj_sender import PBJSender

from ctakes_pbj.examples.word_finder import WordFinder

warnings.filterwarnings("ignore")


def main():

    # Create a PBJ Pipeline.
    pipeline = PBJPipeline()
    # Add the PBJReceiver component.  This connects to the Artemis broker and retrieves information.
    pipeline.reader(PBJReceiver())
    # Add the WordFinder component.
    pipeline.add(WordFinder())
    # Add the PBJSender component.  This sends information to the Artemis broker.
    pipeline.add(PBJSender())
    # Start running the pipeline.  This will automatically initialize all components.
    pipeline.run()


main()

