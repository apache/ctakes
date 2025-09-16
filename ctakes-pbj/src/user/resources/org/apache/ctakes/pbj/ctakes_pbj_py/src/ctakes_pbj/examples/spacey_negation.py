import spacy
from negspacy.negation import Negex
from ctakes_pbj.component import cas_annotator
from ctakes_pbj.type_system import ctakes_types
from spacy.tokens import Doc
from spacy.tokens import Span
from spacy.lang.en import English
from negspacy.termsets import termset
import logging
import os
from pathlib import Path

logger = logging.getLogger(__name__)
def a_sort_by(a):
    return a.begin


class ExampleNegationAnnotator(cas_annotator.CasAnnotator):
    def __init__(self, ts=None):
        # Use current directory or user home instead of hardcoded path
        log_dir = Path.cwd() / "spacey_output"  # User's current working directory
        log_path = log_dir / "spacey.log"

        # Create directory
        log_path.parent.mkdir(parents=True, exist_ok=True)

        handler = logging.FileHandler(log_path, mode="w", encoding="utf-8")
        formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s")
        handler.setFormatter(formatter)
        handler.setLevel(logging.INFO)

        logger.setLevel(logging.INFO)
        logger.addHandler(handler)
        logger.propagate = False
        super().__init__()

        logger.info("Initializing ExampleNegationAnnotator")
        self.ts = ts

        # Create SpaCy pipeline
        self.nlp = spacy.blank("en")

        # ADD SENTENCIZER BEFORE NEGEX
        if "sentencizer" not in self.nlp.pipe_names:
            self.nlp.add_pipe("sentencizer")

        if "negex" not in self.nlp.pipe_names:
            self.nlp.add_pipe("negex")

        logger.info(f"Pipeline components: {self.nlp.pipe_names}")

    def process(self, cas):
        logger.info("Entered process()")
        logger.info(f"Spacy version: {spacy.__version__}")

        for sentence in cas.select(ctakes_types.Sentence):
            tokens = cas.select_covered(ctakes_types.BaseToken, sentence)
            event_mentions = cas.select_covered(ctakes_types.EventMention, sentence)
            tokens.sort(key=a_sort_by)

            words = [token.get_covered_text() for token in tokens]
            logger.info(f"words: {words}")

            # For spacy 3.x - use words= parameter
            doc = Doc(self.nlp.vocab, words=words)
            logger.info(f"Doc created successfully: '{doc.text}'")

            try:
                logger.info("About to create entities...")
                # need to create spans because doc does not take raw token data!
                doc.ents = [Span(doc, i, i+1, label="CONCEPT") for i in range(len(doc))]
                logger.info(f"Created {len(doc.ents)} entities successfully")

                logger.info("About to process through negspacy pipeline...")
                doc = self.nlp(doc)
                logger.info("Doc processed through negspacy pipeline successfully")

                # Create token negation mapping
                token_negations = {}
                for i, token in enumerate(doc):
                    try:
                        token_negations[i] = token._.negex
                        logger.info(f"Token {i} '{token.text}': negated={token._.negex}")
                    except AttributeError:
                        # Fallback - check entities instead
                        if i < len(doc.ents):
                            token_negations[i] = doc.ents[i]._.negex
                            logger.info(f"Entity {i} '{doc.ents[i].text}': negated={doc.ents[i]._.negex}")
                        else:
                            token_negations[i] = False
                            logger.warning(f"Could not get negation for token {i}")

                # Update polarity for event mentions
                for ev in event_mentions:
                    logger.info(f"Processing event mention: '{ev.get_covered_text()}'")

                    # Find token alignment
                    event_start_idx = None
                    event_end_idx = None

                    for i, token in enumerate(tokens):
                        if (token.begin >= ev.begin and token.begin < ev.end) or \
                                (token.end > ev.begin and token.end <= ev.end) or \
                                (token.begin <= ev.begin and token.end >= ev.end):

                            if event_start_idx is None:
                                event_start_idx = i
                            event_end_idx = i

                    if event_start_idx is not None:
                        is_negated = any(token_negations.get(i, False)
                                         for i in range(event_start_idx, event_end_idx + 1))

                        logger.info(f"Event '{ev.get_covered_text()}' spans tokens {event_start_idx}-{event_end_idx}, negated: {is_negated}")

                        # Update polarity: -1 for negated, 1 for not negated
                        ev.polarity = -1 if is_negated else 1
                        logger.info(f"Updated polarity for '{ev.get_covered_text()}': {ev.polarity}")
                    else:
                        logger.warning(f"Could not find token alignment for event mention: '{ev.get_covered_text()}'")


            except Exception as e:
                logger.error(f"Error during processing: {str(e)}")
                logger.error(f"Error type: {type(e)}")
                import traceback
                logger.error(f"Traceback: {traceback.format_exc()}")