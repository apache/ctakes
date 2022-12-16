package org.apache.ctakes.gui.dictionary.umls;

import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.log4j.Logger;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import static org.apache.ctakes.core.util.annotation.SemanticTui.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
final public class TuiTableModel implements TableModel {

   static private final Logger LOGGER = Logger.getLogger( "TuiTableModel" );

//   static public final Tui[] CTAKES_ANAT = { T021, T022, T023, T024, T025, T026, T029, T030 };
//   static public final Tui[] CTAKES_DRUG = { T109, T110, T114, T115, T116, T118, T119, T121, T122, T123, T124,
//                                             T125, T126, T127, T129, T130, T131, T195, T196, T197, T200, T203 };

   // Semantic Types that are in the normal ctakes semantic groups but are still not wanted.  e.g. "Cell"
   static private final Collection<SemanticTui> UNWANTED_TUIS
         = EnumSet.of( T116, T087, T123, T118, T026, T043, T025, T103, T120, T104, T077, T049, T088, T065, T196,
         T050, T018, T126, T168, T045, T028, T125, T078, T129, T055, T197, T170, T130, T119, T063,
         T066, T041, T073, T044, T085, T114, T124, T086, T115, T109, T040, T042, T046, T039,
         T192, T062, T075, T054, UNKNOWN );

   static private final String[] COLUMN_NAMES = { "Use", "TUI", "Semantic Type", "Semantic Group" };
   static private final Class<?>[] COLUMN_CLASSES = { Boolean.class, String.class, String.class, String.class };

   private final EventListenerList _listenerList = new EventListenerList();
   private final Collection<SemanticTui> _wantedTuis = EnumSet.noneOf( SemanticTui.class );

   public TuiTableModel() {
      final EnumSet<SemanticGroup> wantedGroups
            = EnumSet.of( SemanticGroup.ANATOMY,
            SemanticGroup.DISORDER,
            SemanticGroup.FINDING,
            SemanticGroup.PROCEDURE,
            SemanticGroup.DRUG );
      Arrays.stream( SemanticTui.values() )
            .filter( t -> !UNWANTED_TUIS.contains( t ) )
            .filter( t -> wantedGroups.contains( t.getGroup() ) )
            .forEach( _wantedTuis::add );
   }

   public Collection<SemanticTui> getWantedTuis() {
      return _wantedTuis;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getRowCount() {
      return SemanticTui.values().length;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getColumnCount() {
      return 4;
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
      final SemanticTui tui = SemanticTui.values()[ rowIndex ];
      switch ( columnIndex ) {
         case 0:
            return isTuiEnabled( tui );
         case 1:
            return tui.name();
         case 2:
            return tui.getSemanticType();
         case 3:
            return tui.getGroupName();
      }
      return "ERROR";
   }

   private boolean isTuiEnabled( final SemanticTui tui ) {
      return _wantedTuis.contains( tui );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex ) {
      if ( aValue instanceof Boolean && columnIndex == 0 ) {
         final SemanticTui tui = SemanticTui.values()[ rowIndex ];
         if ( (Boolean)aValue ) {
            _wantedTuis.add( tui );
         } else {
            _wantedTuis.remove( tui );
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
