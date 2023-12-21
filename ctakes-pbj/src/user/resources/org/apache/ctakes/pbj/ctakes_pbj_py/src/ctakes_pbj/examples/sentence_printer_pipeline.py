# Should accept cmd line parameters such as: hostname, port, queue name for recieving cas, and queue name for
# sending cas

# These are the lines that ignore the typesystem errors
import warnings

from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
from ctakes_pbj.component.pbj_receiver import PBJReceiver
from ctakes_pbj.component.pbj_sender import PBJSender

from ctakes_pbj.examples.sentence_printer import SentencePrinter

warnings.filterwarnings("ignore")


def main():

    pipeline = PBJPipeline()
    pipeline.reader(PBJReceiver())
    pipeline.add(SentencePrinter())
    pipeline.add(PBJSender())
    pipeline.run()


if __name__ == "__main__":
    main()

