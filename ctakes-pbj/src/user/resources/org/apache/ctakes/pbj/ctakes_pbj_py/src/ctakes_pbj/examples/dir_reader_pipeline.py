# Should accept cmd line parameters such as: hostname, port, queue name for recieving cas, and queue name for
# sending cas

# These are the lines that ignore the typesystem errors
import warnings

from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
from ctakes_pbj.component.pbj_dir_reader import PBJDirReader
from ctakes_pbj.component.pbj_sender import PBJSender


warnings.filterwarnings("ignore")


def main():

    pipeline = PBJPipeline()
    pipeline.reader(PBJDirReader())
    pipeline.add(PBJSender())
    pipeline.run()


main()

