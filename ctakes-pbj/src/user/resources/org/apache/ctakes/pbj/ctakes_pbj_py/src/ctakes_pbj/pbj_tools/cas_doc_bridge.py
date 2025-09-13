"""
CAS-Doc Bridge Utility for ctakes-pbj

Provides bidirectional conversion between cTAKES CAS objects and spaCy Doc objects.
"""

import spacy
from spacy.tokens import Doc, Span, Token
from ctakes_pbj.type_system import ctakes_types
import logging

logger = logging.getLogger(__name__)


class CasDocBridge:
    """
    Utility class for converting between cTAKES CAS and spaCy Doc objects.
    """

    def __init__(self, nlp=None):
        """
        Initialize the bridge.

        Args:
            nlp: spaCy language model. If None, creates a blank English model.
        """
        self.nlp = nlp if nlp is not None else spacy.blank("en")

        # Ensure sentencizer is available for sentence boundary detection
        if "sentencizer" not in self.nlp.pipe_names:
            self.nlp.add_pipe("sentencizer")

    def cas_to_doc(self, cas, sentence=None, preserve_annotations=True):
        """
        Convert a CAS (or sentence within CAS) to a spaCy Doc object.

        Args:
            cas: The CAS object to convert
            sentence: Optional specific sentence to convert. If None, converts entire CAS.
            preserve_annotations: If True, maps cTAKES annotations to spaCy entities

        Returns:
            spacy.tokens.Doc: The converted document
        """
        if sentence is not None:
            return self._sentence_to_doc(cas, sentence, preserve_annotations)
        else:
            return self._full_cas_to_doc(cas, preserve_annotations)

    def _sentence_to_doc(self, cas, sentence, preserve_annotations=True):
        """Convert a single sentence from CAS to Doc."""
        # Get tokens for this sentence
        tokens = cas.select_covered(ctakes_types.BaseToken, sentence)
        tokens = sorted(tokens, key=lambda t: t.begin)

        # Extract words
        words = [token.get_covered_text() for token in tokens]

        # Create Doc
        doc = Doc(self.nlp.vocab, words=words)

        # Set sentence boundaries (entire doc is one sentence)
        if len(doc) > 0:
            doc[0].is_sent_start = True
            for i in range(1, len(doc)):
                doc[i].is_sent_start = False

        if preserve_annotations:
            self._map_annotations_to_doc(cas, doc, sentence, tokens)

        return doc

    def _full_cas_to_doc(self, cas, preserve_annotations=True):
        """Convert entire CAS to Doc."""
        # Get all sentences
        sentences = list(cas.select(ctakes_types.Sentence))
        sentences.sort(key=lambda s: s.begin)

        all_words = []
        sentence_starts = []
        all_tokens = []

        for sentence in sentences:
            tokens = cas.select_covered(ctakes_types.BaseToken, sentence)
            tokens = sorted(tokens, key=lambda t: t.begin)

            # Mark sentence start
            sentence_starts.append(len(all_words))

            # Add words
            words = [token.get_covered_text() for token in tokens]
            all_words.extend(words)
            all_tokens.extend(tokens)

        # Create Doc
        doc = Doc(self.nlp.vocab, words=all_words)

        # Set sentence boundaries
        for i, token in enumerate(doc):
            token.is_sent_start = i in sentence_starts

        if preserve_annotations:
            self._map_annotations_to_doc(cas, doc, None, all_tokens)

        return doc

    def _map_annotations_to_doc(self, cas, doc, sentence_filter, tokens):
        """Map cTAKES annotations to spaCy entities."""
        entities = []

        # Get annotations (filter by sentence if provided)
        if sentence_filter:
            event_mentions = cas.select_covered(ctakes_types.EventMention, sentence_filter)
            entity_mentions = cas.select_covered(ctakes_types.EntityMention, sentence_filter) if hasattr(ctakes_types, 'EntityMention') else []
        else:
            event_mentions = list(cas.select(ctakes_types.EventMention))
            entity_mentions = list(cas.select(ctakes_types.EntityMention)) if hasattr(ctakes_types, 'EntityMention') else []

        # Create token offset mapping
        token_to_doc_idx = {}
        doc_offset = 0 if sentence_filter is None else sentence_filter.begin

        for i, token in enumerate(tokens):
            token_to_doc_idx[token.begin] = i

        # Map event mentions
        for event in event_mentions:
            span_info = self._find_token_span(event, tokens, token_to_doc_idx)
            if span_info:
                start_idx, end_idx = span_info
                if start_idx < len(doc) and end_idx <= len(doc):
                    entities.append((start_idx, end_idx, "EVENT"))

        # Map entity mentions
        for entity in entity_mentions:
            span_info = self._find_token_span(entity, tokens, token_to_doc_idx)
            if span_info:
                start_idx, end_idx = span_info
                if start_idx < len(doc) and end_idx <= len(doc):
                    entities.append((start_idx, end_idx, "ENTITY"))

        # Set entities on doc
        doc.ents = [Span(doc, start, end, label=label) for start, end, label in entities]

    def _find_token_span(self, annotation, tokens, token_to_doc_idx):
        """Find the token span indices for an annotation."""
        start_idx = None
        end_idx = None

        for i, token in enumerate(tokens):
            # Check if token overlaps with annotation
            if (token.begin >= annotation.begin and token.begin < annotation.end) or \
                    (token.end > annotation.begin and token.end <= annotation.end) or \
                    (token.begin <= annotation.begin and token.end >= annotation.end):

                if start_idx is None:
                    start_idx = i
                end_idx = i + 1  # spaCy uses exclusive end

        return (start_idx, end_idx) if start_idx is not None else None

    def doc_to_cas(self, doc, cas, sentence_begin_offset=0, update_annotations=True):
        """
        Update a CAS object with information from a spaCy Doc.

        Args:
            doc: The spaCy Doc object
            cas: The CAS object to update
            sentence_begin_offset: Character offset where this doc starts in the CAS
            update_annotations: If True, adds spaCy entities as cTAKES annotations
        """
        if update_annotations:
            self._map_doc_to_annotations(doc, cas, sentence_begin_offset)

    def _map_doc_to_annotations(self, doc, cas, offset):
        """Map spaCy entities back to cTAKES annotations."""
        for ent in doc.ents:
            # Calculate character positions
            start_char = offset + ent.start_char
            end_char = offset + ent.end_char

            # Create appropriate cTAKES annotation based on label
            if ent.label_ == "EVENT":
                annotation = cas.add_annotation(ctakes_types.EventMention(
                    begin=start_char,
                    end=end_char
                ))
                # Add polarity if available (from negation detection)
                if hasattr(ent._, 'negex'):
                    annotation.polarity = 1 if ent._.negex else -1

            elif ent.label_ == "ENTITY" and hasattr(ctakes_types, 'EntityMention'):
                annotation = cas.add_annotation(ctakes_types.EntityMention(
                    begin=start_char,
                    end=end_char
                ))

    def update_cas_polarities(self, doc, cas, sentence=None):
        """
        Update event mention polarities in CAS based on spaCy negation detection.

        Args:
            doc: spaCy Doc with negation information (_.negex attributes)
            cas: CAS object to update
            sentence: Optional sentence filter
        """
        # Get event mentions
        if sentence:
            event_mentions = cas.select_covered(ctakes_types.EventMention, sentence)
            tokens = cas.select_covered(ctakes_types.BaseToken, sentence)
        else:
            event_mentions = list(cas.select(ctakes_types.EventMention))
            tokens = list(cas.select(ctakes_types.BaseToken))

        tokens = sorted(tokens, key=lambda t: t.begin)

        # Create negation mapping from doc
        token_negations = {}
        for i, token in enumerate(doc):
            if hasattr(token._, 'negex'):
                token_negations[i] = token._.negex
            else:
                # Check entities
                for ent in doc.ents:
                    if ent.start <= i < ent.end and hasattr(ent._, 'negex'):
                        token_negations[i] = ent._.negex
                        break
                else:
                    token_negations[i] = False

        # Update event mentions
        for event in event_mentions:
            # Find corresponding tokens
            start_idx = None
            end_idx = None

            for i, token in enumerate(tokens):
                if (token.begin >= event.begin and token.begin < event.end) or \
                        (token.end > event.begin and token.end <= event.end) or \
                        (token.begin <= event.begin and token.end >= event.end):

                    if start_idx is None:
                        start_idx = i
                    end_idx = i

            if start_idx is not None:
                # Check if any token in range is negated
                is_negated = any(token_negations.get(i, False)
                                 for i in range(start_idx, end_idx + 1))

                # Update polarity
                event.polarity = -1 if is_negated else 1
                logger.info(f"Updated polarity for '{event.get_covered_text()}': {event.polarity}")


# Convenience functions
def cas_sentence_to_doc(cas, sentence, nlp=None, preserve_annotations=True):
    """
    Convert a single CAS sentence to spaCy Doc.

    Args:
        cas: CAS object
        sentence: Sentence annotation
        nlp: Optional spaCy model
        preserve_annotations: Whether to preserve cTAKES annotations

    Returns:
        spacy.tokens.Doc
    """
    bridge = CasDocBridge(nlp)
    return bridge.cas_to_doc(cas, sentence, preserve_annotations)

def cas_to_doc(cas, nlp=None, preserve_annotations=True):
    """
    Convert entire CAS to spaCy Doc.

    Args:
        cas: CAS object
        nlp: Optional spaCy model
        preserve_annotations: Whether to preserve cTAKES annotations

    Returns:
        spacy.tokens.Doc
    """
    bridge = CasDocBridge(nlp)
    return bridge.cas_to_doc(cas, preserve_annotations=preserve_annotations)

def update_cas_from_doc(doc, cas, sentence_begin_offset=0):
    """
    Update CAS with spaCy Doc annotations.

    Args:
        doc: spaCy Doc object
        cas: CAS object to update
        sentence_begin_offset: Character offset
    """
    bridge = CasDocBridge()
    bridge.doc_to_cas(doc, cas, sentence_begin_offset)