package org.apache.ctakes.gui.dictionary.umls;

import org.apache.log4j.Logger;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
final public class LanguageTableModel implements TableModel {

   static private final Logger LOGGER = Logger.getLogger( "LanguageTableModel" );


   static private final String[] COLUMN_NAMES = { "Use", "Language" };
   static private final Class<?>[] COLUMN_CLASSES = { Boolean.class, String.class };

   private final EventListenerList _listenerList = new EventListenerList();
   private final List<String> _languages = new ArrayList<>();
   private final Collection<String> _wantedLanguage = new HashSet<>();


   public void setLangauges( final Collection<String> languages ) {
      _languages.clear();
      _wantedLanguage.clear();
      _languages.addAll( languages );
      Collections.sort( _languages );
      if ( _languages.contains( "ENG" ) ) {
         _wantedLanguage.add( "ENG" );
      }
      fireTableChanged( new TableModelEvent( this ) );
   }


   public Collection<String> getWantedLanguages() {
      return _wantedLanguage;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getRowCount() {
      return _languages.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getColumnCount() {
      return 2;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getColumnName( final int columnIndex ) {
      return COLUMN_NAMES[ columnIndex ];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> getColumnClass( final int columnIndex ) {
      return COLUMN_CLASSES[ columnIndex ];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCellEditable( final int rowIndex, final int columnIndex ) {
      return columnIndex == 0;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getValueAt( final int rowIndex, final int columnIndex ) {
      final String language = _languages.get( rowIndex );
      switch ( columnIndex ) {
         case 0:
            return _wantedLanguage.contains( language );
         case 1:
            return language;
      }
      return "ERROR";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex ) {
      if ( aValue instanceof Boolean && columnIndex == 0 ) {
         final String language = _languages.get( rowIndex );
         if ( (Boolean) aValue ) {
            _wantedLanguage.add( language );
         } else {
            _wantedLanguage.remove( language );
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addTableModelListener( final TableModelListener listener ) {
      _listenerList.add( TableModelListener.class, listener );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeTableModelListener( final TableModelListener listener ) {
      _listenerList.remove( TableModelListener.class, listener );
   }

   /**
    * Forwards the given notification event to all
    * <code>TableModelListeners</code> that registered
    * themselves as listeners for this table model.
    *
    * @param e the event to be forwarded
    * @see #addTableModelListener
    * @see TableModelEvent
    * @see EventListenerList
    */
   private void fireTableChanged( TableModelEvent e ) {
      // Guaranteed to return a non-null array
      Object[] listeners = _listenerList.getListenerList();
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
         if ( listeners[ i ] == TableModelListener.class ) {
            ((TableModelListener) listeners[ i + 1 ]).tableChanged( e );
         }
      }
   }


}
