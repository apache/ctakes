import warnings
from ctakes_pbj.component.pbj_receiver import PBJReceiver
from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
from ctakes_pbj.component.pbj_sender import PBJSender
from ctakes_pbj.examples.negspacy import BridgedNegationAnnotator
from ctakes_pbj.pbj_tools import arg_parser

warnings.filterwarnings("ignore")
ap = arg_parser.ArgParser()
ap.add_arg('--receive_queue', default='JavaToPy')
ap.add_arg('--send_queue', default='PyToJava')
ap.add_arg('--output_dir')

# Get the parsed args
args = ap.get_args()

def main():
    # Initialize PBJ pipeline
    pipeline = PBJPipeline()

    # Set the receiver as the reader (this declares receive_queue etc.)
    pipeline.reader(PBJReceiver())

    # Add spaCy-based negation annotator
    pipeline.add(BridgedNegationAnnotator())

    # Add PBJ sender
    pipeline.add(PBJSender())

    # Start receiver
    pipeline.run()

if __name__ == "__main__":
    main()
