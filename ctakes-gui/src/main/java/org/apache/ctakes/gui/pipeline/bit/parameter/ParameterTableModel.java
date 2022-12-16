package org.apache.ctakes.gui.pipeline.bit.parameter;


import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/5/2017
 */
final public class ParameterTableModel implements TableModel {

   static private final Logger LOGGER = Logger.getLogger( "ParameterTableModel" );

   static private final String[] COLUMN_NAMES = { "Parameter Name", "Value", "" };
   static private final Class<?>[] COLUMN_CLASSES = { ConfigurationParameter.class, String.class, File.class };

   private final EventListenerList _listenerList = new EventListenerList();

   private ParameterHolder _parameterHolder;
   private List<String[]> _values = new ArrayList<>();

   public ParameterHolder getParameterHolder() {
      return _parameterHolder;
   }

   /**
    * Populate the list
    *
    * @param holder - holder with all parameter information
    */
   public void setParameterHolder( final ParameterHolder holder ) {
      final int oldSize = _parameterHolder == null ? 0 : _parameterHolder.getParameterCount();
      _parameterHolder = holder;
      _values.clear();
      if ( holder == null ) {
         if ( oldSize > 0 ) {
            fireTableChanged(
                  new TableModelEvent( this, 0, oldSize - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
         }
         return;
      }
      for ( int i = 0; i < holder.getParameterCount(); i++ ) {
         _values.add( holder.getParameterValue( i ) );
      }
      if ( holder.getParameterCount() > 0 ) {
         fireTableChanged( new TableModelEvent( this ) );
      } else if ( oldSize > 0 ) {
         fireTableChanged(
               new TableModelEvent( this, 0, oldSize - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
      }
   }

   public List<String[]> getValues() {
      return Collections.unmodifiableList( _values );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getRowCount() {
      if ( _parameterHolder == null ) {
         return 0;
      }
      return _parameterHolder.getParameterCount();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getColumnCount() {
      return COLUMN_NAMES.length;
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
      return columnIndex != 0;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getValueAt( final int rowIndex, final int columnIndex ) {
      switch ( columnIndex ) {
         case 0:
            return _parameterHolder.getParameter( rowIndex );
         case 1:
            return Arrays.stream( _values.get( rowIndex ) )
                  .filter( v -> !ConfigurationParameter.NO_DEFAULT_VALUE.equals( v ) )
                  .collect( Collectors.joining( " , " ) );
         case 2:
            final String path = Arrays.stream( _values.get( rowIndex ) )
                  .filter( v -> !ConfigurationParameter.NO_DEFAULT_VALUE.equals( v ) )
                  .collect( Collectors.joining( "/" ) );
            return new File( path );
      }
      return "ERROR";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex ) {
      if ( columnIndex == 1 ) {
         _values.set( rowIndex, aValue.toString().split( "," ) );
         fireTableChanged( new TableModelEvent( this, rowIndex, rowIndex, columnIndex ) );
      } else if ( columnIndex == 2 && File.class.isInstance( aValue ) ) {
         final String[] path = { ((File)aValue).getPath() };
         _values.set( rowIndex, path );
         fireTableChanged( new TableModelEvent( this, rowIndex, rowIndex, 1 ) );
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
    * @param event the event to be forwarded
    * @see #addTableModelListener
    * @see TableModelEvent
    * @see EventListenerList
    */
   private void fireTableChanged( final TableModelEvent event ) {
      // Guaranteed to return a non-null array
      Object[] listeners = _listenerList.getListenerList();
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
         if ( listeners[ i ] == TableModelListener.class ) {
            ((TableModelListener)listeners[ i + 1 ]).tableChanged( event );
         }
      }
   }

}
