import warnings
from ctakes_pbj.component.pbj_receiver import PBJReceiver
from ctakes_pbj.pipeline.pbj_pipeline import PBJPipeline
from ctakes_pbj.component.pbj_sender import PBJSender
from ctakes_pbj.examples.spacey_negation import ExampleNegationAnnotator
from ctakes_pbj.pbj_tools import arg_parser

warnings.filterwarnings("ignore")
# Create an instance of ArgParser
ap = arg_parser.ArgParser()
#
ap.add_arg('--receive_queue', default='JavaToPy')
ap.add_arg('--send_queue', default='PyToJava')

# ap.add_arg('--receive_queue', default='JavaToPy')
# ap.add_arg('--send_queue', default='PyToJava')
ap.add_arg('--output_dir', default=r'C:\Users\ch229935\Desktop\spacey_output')
# ap.add_arg('--host_name', default='localhost')
# ap.add_arg('--port_name', default='61616')
# ap.add_arg('--username', default='admin')
# ap.add_arg('--password', default='admin')

# Now get the parsed args
args = ap.get_args()

def main():
    # Initialize PBJ pipeline
    pipeline = PBJPipeline()

    # Set the receiver as the reader (this declares receive_queue etc.)
    pipeline.reader(PBJReceiver())

    # Add spaCy-based negation annotator
    pipeline.add(ExampleNegationAnnotator(ts=None))

    # Add PBJ sender
    pipeline.add(PBJSender())

    # Start receiver
    pipeline.run() # start method on instance, not class

if __name__ == "__main__":
    main()
