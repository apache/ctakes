package org.apache.ctakes.gui.dictionary.cased.table;


import org.apache.ctakes.gui.dictionary.cased.umls.file.Tty;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/27/2020
 */
public class TextTypeModel implements TableModel {


   // TODO make a separate panel for examples.
   //  Make it listen to the table for row selection.
   //  On row selection change the contents of the example panel based upon get( row, 3 );

   static private final String[] COLUMN_NAMES = { "Use Type", "TTY", "Text Type", "Examples" };
   static private final Class<?>[] COLUMN_CLASSES = { Boolean.class, String.class, String.class, List.class };

   private final EventListenerList _listenerList = new EventListenerList();
   private final Collection<Tty> _wantedTypes = EnumSet.noneOf( Tty.class );
   private final Map<Tty, List<String>> _examples = new HashMap<>();

   public TextTypeModel() {
      Arrays.stream( Tty.values() )
            .filter( Tty::collect )
            .forEach( _wantedTypes::add );
   }

   public void setTypeExamples( final Map<Tty, List<String>> examples ) {
      _examples.putAll( examples );
   }


   public Collection<Tty> getWantedTypes() {
      return _wantedTypes;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getRowCount() {
      return Tty.values().length;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getColumnCount() {
      return 3;
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
      final Tty type = Tty.values()[ rowIndex ];
      switch ( columnIndex ) {
         case 0:
            return _wantedTypes.contains( type );
         case 1:
            return type.name();
         case 2:
            return type.getDescription();
         case 3:
            return _examples.getOrDefault( type, Collections.emptyList() );
      }
      return "ERROR";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex ) {
      if ( aValue instanceof Boolean && columnIndex == 0 ) {
         final Tty type = Tty.values()[ rowIndex ];
         if ( (Boolean)aValue ) {
            _wantedTypes.add( type );
         } else {
            _wantedTypes.remove( type );
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

}
