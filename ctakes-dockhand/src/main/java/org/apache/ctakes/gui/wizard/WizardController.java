package org.apache.ctakes.gui.wizard;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2019
 */
public class WizardController extends DefaultListSelectionModel implements ListModel<WizardStep> {


   static private final WizardStep NULL_STEP = new WizardStep() {
      public String getName() {
         return "None";
      }

      public String getDescription() {
         return "No action available.";
      }

      public JComponent getPanel() {
         return new JLabel();
      }
   };

   static private final Runnable NULL_BUILD = () -> {
   };

   private final List<WizardStep> _wizardSteps = new ArrayList<>();

   private final BooleanSupplier DEFAULT_BUILDABLE = () -> !hasNextStep()
                                                           && getBuildProcess() != null
                                                           && !NULL_BUILD.equals( getBuildProcess() )
                                                           &&
                                                           getWizardSteps().stream().allMatch( WizardStep::finished );


   private Runnable _buildProcess = NULL_BUILD;
   private BooleanSupplier _buildable = DEFAULT_BUILDABLE;


   final Runnable toPrevious() {
      return () -> {
         if ( hasPreviousStep() ) {
            setCurrentIndex( getCurrentIndex() - 1 );
         }
      };
   }

   final Runnable toNext() {
      return () -> {
         if ( hasNextStep() ) {
            setCurrentIndex( getCurrentIndex() + 1 );
         }
      };
   }

   final BooleanSupplier hasPrevious() {
      return this::hasPreviousStep;
   }

   final BooleanSupplier hasNext() {
      return this::hasNextStep;
   }


   public WizardController() {
      setSelectionMode( SINGLE_SELECTION );
   }


   final Runnable getBuildProcess() {
      return _buildProcess;
   }

   final public void setBuildProcess( final Runnable build ) {
      _buildProcess = build;
   }


   final BooleanSupplier getBuildable() {
      return _buildable;
   }

   final public void setBuildable( final BooleanSupplier buildable ) {
      _buildable = buildable;
   }


   final public void addStep( final WizardStep wizardStep ) {
      _wizardSteps.add( wizardStep );
   }

   final public List<WizardStep> getWizardSteps() {
      return Collections.unmodifiableList( _wizardSteps );
   }

   final void setCurrentIndex( final int index ) {
      setSelectionInterval( index, index );
   }

   private int getCurrentIndex() {
      return getAnchorSelectionIndex();
   }

   final public WizardStep getCurrentStep() {
      return getElementAt( getCurrentIndex() );
   }

   private boolean hasPreviousStep() {
      return getCurrentIndex() > 0;
   }

   private boolean hasNextStep() {
      return getCurrentIndex() < getSize() - 1;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   final public int getSize() {
      return _wizardSteps.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public WizardStep getElementAt( final int index ) {
      return _wizardSteps.get( index );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addListDataListener( final ListDataListener listener ) {
      listenerList.add( ListDataListener.class, listener );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void removeListDataListener( final ListDataListener listener ) {
      listenerList.remove( ListDataListener.class, listener );
   }

   /**
    * Returns an array of all the registered list data listeners.
    *
    * @return all of this model's <code>ListDataListener</code>s,
    * or an empty array if no list data listeners
    * are currently registered
    * @see #addListDataListener
    * @see #removeListDataListener
    */
   public ListDataListener[] getListDataListeners() {
      return listenerList.getListeners( ListDataListener.class );
   }


   /**
    * @param source the <code>ListModel</code> that changed, typically "this"
    * @param index0 one end of the new interval
    * @param index1 the other end of the new interval
    * @see EventListenerList
    * @see DefaultListModel
    */
   protected void fireContentsChanged( final Object source, final int index0, final int index1 ) {
      final Object[] listeners = listenerList.getListenerList();
      ListDataEvent event = null;

      for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
         if ( listeners[ i ] == ListDataListener.class ) {
            if ( event == null ) {
               event = new ListDataEvent( source, ListDataEvent.CONTENTS_CHANGED, index0, index1 );
            }
            ((ListDataListener)listeners[ i + 1 ]).contentsChanged( event );
         }
      }
   }


   // TODO : Internal ActionListener class.  Has boolean "firingAction".  Is added to every AbstractWizardStep.
   //  Each AbstractWizardStep has an actionListener.  That actionListener is added to this controller.
   //  This actionListener will call the WizardStep ".revalidate()".
   //  The NavigationPanel will also have an actionListener and a ".revalidate()".
   //  This way we can constantly update the "Next" and "Finish" buttons as panels are completed.
   //  TocPanel should probably also have one and set each list item enabled as the wizard progresses.


}
