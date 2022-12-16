package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.core.util.collection.ImmutableCollectionMap;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
@Immutable
final public class DefaultConcept implements Concept {

   final private String _cui;
   final private String _preferredText;
   final private CollectionMap<String, String, ? extends Collection<String>> _codes;
   final private Collection<Integer> _ctakesSemantics;

   final private int _hashcode;

   /**
    * @param cui -
    */
   public DefaultConcept( final String cui ) {
      this( cui, "" );
   }

   /**
    * @param cui           -
    * @param preferredText -
    */
   public DefaultConcept( final String cui, final String preferredText ) {
      this( cui, preferredText, new HashSetMap<>() );
   }

   /**
    * @param cui           -
    * @param preferredText -
    * @param codes         collection of coding scheme names and this concept's codes for those schemes
    */
   public DefaultConcept( final String cui, final String preferredText,
                          final CollectionMap<String, String, ? extends Collection<String>> codes ) {
      _cui = cui;
      _preferredText = preferredText;
      _codes = new ImmutableCollectionMap<>( codes );
      // Attempt to obtain one or more valid type ids from the tuis of the term
      Collection<Integer> ctakesSemantics
            = getCodes( TUI ).stream()
                             .map( SemanticTui::getTuiFromCode )
                             .map( SemanticTui::getGroupCode )
                             .collect( Collectors.toSet() );
      if ( ctakesSemantics.isEmpty() ) {
         ctakesSemantics = Collections.singletonList( SemanticGroup.UNKNOWN.getCode() );
      }
      _ctakesSemantics = Collections.unmodifiableCollection( ctakesSemantics );
      _hashcode = cui.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCui() {
      return _cui;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPreferredText() {
      if ( _preferredText != null ) {
         return _preferredText;
      }
      return PREFERRED_TERM_UNKNOWN;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodeNames() {
      return _codes.keySet();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodes( final String codeType ) {
      return _codes.getCollection( codeType );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Integer> getCtakesSemantics() {
      return _ctakesSemantics;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return (_preferredText == null || _preferredText.isEmpty()) && _codes.isEmpty();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return value instanceof Concept && _cui.equals( ((DefaultConcept)value)._cui );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashcode;
   }

}
