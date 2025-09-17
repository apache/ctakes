"""
CAS-Doc Bridge Utility for ctakes-pbj

Provides bidirectional conversion between cTAKES CAS objects and spaCy Doc objects.
Based on working ExampleNegationAnnotator implementation.
"""

import spacy
from spacy.tokens import Doc, Span, Token
from ctakes_pbj.type_system import ctakes_types
import logging

logger = logging.getLogger(__name__)

def a_sort_by(a):
    """Sort function for CAS annotations by begin position"""
    return a.begin

def overlaps(span1, span2):
    """Return True if two (start, end) spans overlap even partially"""
    s1, e1 = span1
    s2, e2 = span2
    return not (e1 <= s2 or s1 >= e2)

class CasDocBridge:
    """
    Utility class for converting between cTAKES CAS and spaCy Doc objects.
    Based on the working ExampleNegationAnnotator approach.
    """

    def __init__(self, nlp=None):
        """
        Initialize the bridge.

        Args:
            nlp: spaCy language model. If None, creates a blank English model.
        """
        self.nlp = nlp if nlp is not None else spacy.blank("en")
        logger.info(f"CasDocBridge initialized with pipeline: {self.nlp.pipe_names}")

    def has_space(self, token, next_token):
        """Check if there's a space between two tokens"""
        if next_token is None:
            return False
        return token.end + 1 == next_token.begin

    def cas_to_doc(self, cas, preserve_annotations=True):
        """
        Convert a CAS to a spaCy Doc object using the whole-document approach.

        Args:
            cas: The CAS object to convert
            preserve_annotations: If True, maps cTAKES annotations to spaCy entities

        Returns:
            spacy.tokens.Doc: The converted document with proper tokenization and entities
        """
        logger.info("Converting CAS to Doc...")

        # --- collect ALL tokens across the document ---
        tokens = list(cas.select(ctakes_types.BaseToken))
        tokens.sort(key=a_sort_by)

        if not tokens:
            logger.warning("No tokens found in CAS")
            return Doc(self.nlp.vocab, words=[], spaces=[])

        words = [t.get_covered_text() for t in tokens]
        spaces = []
        for i, t in enumerate(tokens):
            if i < len(tokens) - 1:
                spaces.append(self.has_space(t, tokens[i + 1]))
            else:
                spaces.append(False)

        logger.info(f"Total tokens: {len(tokens)}")

        # --- build ONE Doc for the whole document ---
        doc = Doc(self.nlp.vocab, words=words, spaces=spaces)
        logger.info(f"Doc created successfully: '{doc.text[:200]}...'")  # log first 200 chars

        # --- mark sentence starts from CAS sentences ---
        sentences = list(cas.select(ctakes_types.Sentence))
        sentence_starts = {s.begin for s in sentences}
        for i, t in enumerate(tokens):
            doc[i].is_sent_start = t.begin in sentence_starts

        logger.info(f"Marked {len([t for t in doc if t.is_sent_start])} sentence starts")

        # --- attach entities from CAS annotations if requested ---
        if preserve_annotations:
            self._map_cas_annotations_to_doc(cas, doc, tokens)

        return doc

    def _map_cas_annotations_to_doc(self, cas, doc, tokens):
        """Map cTAKES annotations to spaCy entities using the working approach."""
        ents = []

        # Process EventMentions
        for ev in cas.select(ctakes_types.EventMention):
            start_idx = None
            end_idx = None
            for i, t in enumerate(tokens):
                if (t.begin >= ev.begin and t.begin < ev.end) or \
                        (t.end > ev.begin and t.end <= ev.end) or \
                        (t.begin <= ev.begin and t.end >= ev.end):

                    if start_idx is None:
                        start_idx = i
                    end_idx = i

            if start_idx is not None:
                # Create span with exclusive end (add 1 to end_idx)
                span = Span(doc, start_idx, end_idx + 1, label="CONCEPT")
                ents.append(span)
                logger.debug(f"Mapped EventMention '{ev.get_covered_text()}' to span [{start_idx}:{end_idx+1}]")
            else:
                logger.warning(f"Could not align EventMention: '{ev.get_covered_text()}'")

        # Process EntityMentions if they exist
        try:
            for em in cas.select(ctakes_types.EntityMention):
                start_idx = None
                end_idx = None
                for i, t in enumerate(tokens):
                    if (t.begin >= em.begin and t.begin < em.end) or \
                            (t.end > em.begin and t.end <= em.end) or \
                            (t.begin <= em.begin and t.end >= em.end):

                        if start_idx is None:
                            start_idx = i
                        end_idx = i

                if start_idx is not None:
                    span = Span(doc, start_idx, end_idx + 1, label="ENTITY")
                    ents.append(span)
                    logger.debug(f"Mapped EntityMention '{em.get_covered_text()}' to span [{start_idx}:{end_idx+1}]")
                else:
                    logger.warning(f"Could not align EntityMention: '{em.get_covered_text()}'")
        except AttributeError:
            logger.info("EntityMention type not available in this CAS")

        # Store entities in both places for compatibility
        doc.spans["cas_annotations"] = ents
        doc.ents = spacy.util.filter_spans(ents)  # Remove overlapping spans
        logger.info(f"Attached {len(doc.ents)} entities to the Doc")

        return doc

    def update_cas_polarities(self, doc, cas):
        """
        Update event mention polarities in CAS based on spaCy negation detection.
        Uses the working overlap-based approach from ExampleNegationAnnotator.

        Args:
            doc: spaCy Doc with negation information (_.negex attributes)
            cas: CAS object to update
        """
        logger.info("Updating CAS polarities from Doc negation results...")

        # --- create entity negation mapping ---
        ent_negations = {}
        for ent in doc.ents:
            if hasattr(ent._, "negex"):
                ent_negations[(ent.start_char, ent.end_char)] = ent._.negex
                logger.debug(f"Entity '{ent.text}' ({ent.label_}): negated={ent._.negex}")
            else:
                logger.warning(f"Entity '{ent.text}' has no negex info")

        # --- update polarities using overlap detection ---
        updated_count = 0
        for ev in cas.select(ctakes_types.EventMention):
            ev_span = (ev.begin, ev.end)
            is_negated = False

            for (ent_start, ent_end), neg in ent_negations.items():
                if overlaps(ev_span, (ent_start, ent_end)):
                    is_negated = neg
                    logger.debug(
                        f"Overlap found: EventMention '{ev.get_covered_text()}' "
                        f"({ev.begin}, {ev.end}) with entity span "
                        f"({ent_start}, {ent_end}) → negated={neg}"
                    )
                    break  # stop at the first overlap

            # Update polarity
            old_polarity = getattr(ev, 'polarity', None)
            ev.polarity = -1 if is_negated else 1

            if old_polarity != ev.polarity:
                updated_count += 1
                logger.info(f"Updated polarity for '{ev.get_covered_text()}': {old_polarity} → {ev.polarity}")

        logger.info(f"Updated polarities for {updated_count} EventMentions")

    def doc_to_cas_annotations(self, doc, cas, offset=0):
        """
        Add spaCy Doc entities as new annotations in CAS.

        Args:
            doc: spaCy Doc object
            cas: CAS object to update
            offset: Character offset for positioning annotations
        """
        logger.info(f"Adding {len(doc.ents)} Doc entities to CAS...")

        added_count = 0
        for ent in doc.ents:
            # Calculate character positions
            start_char = offset + ent.start_char
            end_char = offset + ent.end_char

            try:
                # Create appropriate cTAKES annotation based on label
                if ent.label_ == "CONCEPT" or ent.label_ == "EVENT":
                    annotation = cas.add_annotation(ctakes_types.EventMention(
                        begin=start_char,
                        end=end_char
                    ))
                    # Add polarity if available (from negation detection)
                    if hasattr(ent._, 'negex'):
                        annotation.polarity = -1 if ent._.negex else 1
                    else:
                        annotation.polarity = 1  # default to positive
                    added_count += 1

                elif ent.label_ == "ENTITY":
                    annotation = cas.add_annotation(ctakes_types.EntityMention(
                        begin=start_char,
                        end=end_char
                    ))
                    added_count += 1

                logger.debug(f"Added {ent.label_} annotation: '{ent.text}' at [{start_char}:{end_char}]")

            except Exception as e:
                logger.error(f"Failed to add annotation for entity '{ent.text}': {e}")

        logger.info(f"Successfully added {added_count} annotations to CAS")

    def get_token_mapping(self, cas):
        """
        Get the token mapping used internally for debugging/analysis.

        Returns:
            tuple: (tokens, words, spaces) used in doc creation
        """
        tokens = list(cas.select(ctakes_types.BaseToken))
        tokens.sort(key=a_sort_by)

        words = [t.get_covered_text() for t in tokens]
        spaces = []
        for i, t in enumerate(tokens):
            if i < len(tokens) - 1:
                spaces.append(self.has_space(t, tokens[i + 1]))
            else:
                spaces.append(False)

        return tokens, words, spaces