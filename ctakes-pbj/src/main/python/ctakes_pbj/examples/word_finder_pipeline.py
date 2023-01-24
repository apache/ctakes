# Should accept cmd line parameters such as: hostname, port, queue name for recieving cas, and queue name for
# sending cas

# These are the lines that ignore the typesystem errors
import warnings

from ctakes_pbj.component.pbj_sender import PBJSender
from ctakes_pbj.component.pbj_receiver import start_receiver
from ctakes_pbj.examples.word_finder import WordFinder
from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
import os
warnings.filterwarnings("ignore")


def main():

    print("Current working directory:", os.getcwd())
    pipeline = PBJPipeline()
    pipeline.add(WordFinder())
    pipeline.add(PBJSender())
    pipeline.initialize()
    start_receiver(pipeline)


main()

