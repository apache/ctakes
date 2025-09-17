import spacy
from negspacy.negation import Negex
from ctakes_pbj.component import cas_annotator
from ctakes_pbj.type_system import ctakes_types
import logging
from pathlib import Path
from ..pbj_tools.cas_doc_spacy_bridge import CasDocBridge

logger = logging.getLogger(__name__)

class BridgedNegationAnnotator(cas_annotator.CasAnnotator):
    """
    This annotator:
    1. Converts CAS to spaCy Doc using CasDocBridge
    2. Processes through negspacy pipeline
    3. Updates CAS polarities using the bridge

    Usage:
    - Drop this file into your cTAKES-PBJ project
    - Update your pipeline configuration to use BridgedNegationAnnotator
    - No other changes needed!
    """

    def __init__(self, ts=None):
        # Set up logging
        log_dir = Path.cwd() / "spacey_output"
        log_path = log_dir / "bridged_negation.log"
        log_path.parent.mkdir(parents=True, exist_ok=True)

        handler = logging.FileHandler(log_path, mode="w", encoding="utf-8")
        formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s")
        handler.setFormatter(formatter)
        handler.setLevel(logging.INFO)

        logger.setLevel(logging.INFO)
        logger.addHandler(handler)
        logger.propagate = False

        super().__init__()
        logger.info("Initializing BridgedNegationAnnotator")

        self.ts = ts

        # Create SpaCy pipeline with negspacy
        self.nlp = spacy.blank("en")
        self.nlp.add_pipe("negex", config={"ent_types": ["CONCEPT"]})
        logger.info(f"SpaCy pipeline components: {self.nlp.pipe_names}")

        # Initialize the CAS-Doc bridge
        self.bridge = CasDocBridge(self.nlp)
        logger.info("CasDocBridge initialized successfully")

    def process(self, cas):
        """
        Main processing method - converts CAS to Doc, processes negation, updates CAS.
        """
        logger.info("=" * 50)
        logger.info("Starting BridgedNegationAnnotator processing")
        logger.info(f"Spacy version: {spacy.__version__}")

        try:
            # Step 1: Convert CAS to spaCy Doc
            logger.info("Step 1: Converting CAS to spaCy Doc...")
            doc = self.bridge.cas_to_doc(cas, preserve_annotations=True)

            if not doc or len(doc) == 0:
                logger.warning("No tokens found in document, skipping processing")
                return

            logger.info(f"Converted to Doc with {len(doc)} tokens and {len(doc.ents)} entities")
            logger.info(f"Document preview: '{doc.text[:200]}...'")

            # Step 2: Process through negspacy pipeline
            logger.info("Step 2: Processing through negspacy pipeline...")
            doc = self.nlp(doc)
            logger.info("Negspacy processing completed successfully")

            # Log negation results
            negated_count = 0
            for ent in doc.ents:
                if hasattr(ent._, "negex") and ent._.negex:
                    negated_count += 1
                    logger.info(f"NEGATED: '{ent.text}' ({ent.label_})")
                elif hasattr(ent._, "negex"):
                    logger.debug(f"NOT NEGATED: '{ent.text}' ({ent.label_})")
                else:
                    logger.warning(f"No negation info for: '{ent.text}'")

            logger.info(f"Found {negated_count} negated entities out of {len(doc.ents)} total")

            # Step 3: Update CAS polarities
            logger.info("Step 3: Updating CAS polarities...")
            self.bridge.update_cas_polarities(doc, cas)
            logger.info("CAS polarity updates completed")


        except Exception as e:
            logger.error(f"Error during processing: {str(e)}")
            logger.error(f"Error type: {type(e).__name__}")
            import traceback
            logger.error(f"Traceback: {traceback.format_exc()}")
            raise  # Re-raise to maintain cTAKES error handling

        logger.info("BridgedNegationAnnotator processing completed successfully")

    # def get_bridge(self):
    #     """
    #     Provide access to the bridge for advanced usage.
    #
    #     Returns:
    #         CasDocBridge: The bridge instance used by this annotator
    #     """
    #     return self.bridge
    #
    # def get_nlp(self):
    #     """
    #     Provide access to the spaCy pipeline for inspection.
    #
    #     Returns:
    #         spacy.lang: The spaCy pipeline used by this annotator
    #     """
    #     return self.nlp
