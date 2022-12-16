package org.apache.ctakes.gui.pipeline;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.component.PositionedSplitPane;
import org.apache.ctakes.gui.component.SmoothTipList;
import org.apache.ctakes.gui.pipeline.bit.BitCellRenderer;
import org.apache.ctakes.gui.pipeline.bit.PipeBitFinder;
import org.apache.ctakes.gui.pipeline.bit.available.AvailablesListModel;
import org.apache.ctakes.gui.pipeline.bit.available.AvailablesRenderer;
import org.apache.ctakes.gui.pipeline.bit.info.PipeBitInfoPanel;
import org.apache.ctakes.gui.pipeline.bit.user.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO this interface is completely graphical and needs a lot of attention to be done well
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/20/2016
 */
final class MainPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "MainPanel" );


   private final AvailablesListModel _availablesListModel = new AvailablesListModel();
   private JList<PipeBitInfo> _availablesList;
   private JList<UserBit> _userBitsList;

   MainPanel() {
      super( new BorderLayout( 10, 10 ) );

      final JSplitPane logSplit = new PositionedSplitPane( JSplitPane.VERTICAL_SPLIT );
      logSplit.setTopComponent( createMainPanel() );
      logSplit.setBottomComponent( LoggerPanel.createLoggerPanel() );
      logSplit.setDividerLocation( 0.6d );

      add( logSplit, BorderLayout.CENTER );
   }


   private JComponent createWestPanel() {
      final JLabel header = new JLabel( "Available Pipe Bits" );
      header.setPreferredSize( new Dimension( 100, 30 ) );
      header.setHorizontalAlignment( SwingConstants.CENTER );
      _availablesList = createPipeBitList( _availablesListModel );

      final ListCellRenderer<Object> availableRenderer = new AvailablesRenderer();
      _availablesList.setCellRenderer( availableRenderer );
      final JScrollPane scroll = new JScrollPane( _availablesList );
      scroll.setColumnHeaderView( header );
      scroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

      final JSplitPane split = new PositionedSplitPane();
      split.setLeftComponent( scroll );
      split.setRightComponent( createBitInfoPanel( _availablesList ) );
      split.setDividerLocation( 0.3d );
      return split;
   }


   private JComponent createEastPanel() {
      final JLabel header = new JLabel( "User Pipeline" );
      header.setPreferredSize( new Dimension( 100, 30 ) );
      header.setHorizontalAlignment( SwingConstants.CENTER );
      final UserBitListModel userBitListModel = new UserBitListModel();
      _userBitsList = createUserBitList( userBitListModel );
      final ListCellRenderer<Object> usersRenderer = new UserBitRenderer();
      _userBitsList.setCellRenderer( usersRenderer );
      final JScrollPane scroll = new JScrollPane( _userBitsList );
      scroll.setColumnHeaderView( header );

      // Listener for mouse clicks and float-over in availables list
      final AvailablesMouseListener availablesMouse = new AvailablesMouseListener( _availablesList, userBitListModel );
      _availablesList.addMouseListener( availablesMouse );
      _availablesList.addMouseMotionListener( availablesMouse );

      // Listener for mouse clicks and float-over in users list
      final UsersMouseListener usersMouse = new UsersMouseListener( _userBitsList, userBitListModel );
      _userBitsList.addMouseListener( usersMouse );
      _userBitsList.addMouseMotionListener( usersMouse );

      final JSplitPane split = new PositionedSplitPane();
      split.setLeftComponent( scroll );
      split.setRightComponent( createUserBitPanel( _userBitsList ) );
      split.setDividerLocation( 0.3d );
      return split;
   }


   private JComponent createMainPanel() {
      final JComponent westPanel = createWestPanel();
      final JComponent eastPanel = createEastPanel();
      return new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, westPanel, eastPanel );
   }


//   private JComponent createCenterPanel() {
//      final JPanel panel = new JPanel( new BorderLayout() );
//      final JList<PipeBitInfo> pipeBitList = createPipeBitList( _availablesListModel );
//      final JSplitPane centerSplit = new PositionedSplitPane();
//      centerSplit.setLeftComponent( new JScrollPane( pipeBitList ) );
//      final PipeBitInfoPanel pipeBitInfoPanel = createPipeBitPanel();
//      centerSplit.setRightComponent( pipeBitInfoPanel );
//      centerSplit.setDividerLocation( 0.25d );
//      panel.add( centerSplit, BorderLayout.CENTER );
//      panel.add( createGoPanel(), BorderLayout.SOUTH );
//      pipeBitInfoPanel.addPipeBitListListener( pipeBitList );
//      return panel;
//   }

   static private JList<PipeBitInfo> createPipeBitList( final ListModel<PipeBitInfo> model ) {
      final JList<PipeBitInfo> bitList = new SmoothTipList<>( model );
      bitList.setCellRenderer( new BitCellRenderer() );
      bitList.setFixedCellHeight( 20 );
      return bitList;
   }

   static private JList<UserBit> createUserBitList( final ListModel<UserBit> model ) {
      final JList<UserBit> bitList = new SmoothTipList<>( model );
      bitList.setFixedCellHeight( 20 );
      return bitList;
   }

   static private PipeBitInfoPanel createBitInfoPanel( final JList<PipeBitInfo> list ) {
      final PipeBitInfoPanel pipeBitInfoPanel = new PipeBitInfoPanel();
      pipeBitInfoPanel.setPipeBitInfoList( list );
      return pipeBitInfoPanel;
   }

   static private UserBitInfoPanel createUserBitPanel( final JList<UserBit> list ) {
      final UserBitInfoPanel userBitPanelPanel = new UserBitInfoPanel();
      userBitPanelPanel.setUserBitList( list );
      return userBitPanelPanel;
   }


   private JComponent createGoPanel() {
      return new JButton( new FindPipeBitsAction() );
   }


   public void findPipeBits() {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( new PiperBitParser() );
   }

   private class PiperBitParser implements Runnable {
      @Override
      public void run() {
         final JFrame frame = (JFrame)SwingUtilities.getRoot( MainPanel.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         PipeBitFinder.getInstance().scan();
         _availablesListModel.setPipeBits( PipeBitFinder.getInstance().getPipeBits() );
         DisablerPane.getInstance().setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }
   }


   /**
    * Builds the dictionary
    */
   private class FindPipeBitsAction extends AbstractAction {
      private FindPipeBitsAction() {
         super( "Find Readers, Annotators and Writers" );
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         final ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.execute( new PiperBitParser() );
      }
   }

   static private final class AvailablesMouseListener extends MouseAdapter {
      private final JList<PipeBitInfo> _list;
      private final UserBitListModel __userBitListModel;
      private int _currentFocusIndex = -1;

      private AvailablesMouseListener( final JList<PipeBitInfo> list, final UserBitListModel userBitListModel ) {
         _list = list;
         __userBitListModel = userBitListModel;
      }

      @Override
      public void mouseReleased( final MouseEvent event ) {
         final Point p = _list.getMousePosition();
         if ( p.getX() < _list.getWidth() - 37 ) {
            return;
         }
         final int index = _list.locationToIndex( p );
         final AvailablesListModel availablesModel = (AvailablesListModel)_list.getModel();
         final PipeBitInfo pipeBitInfo = availablesModel.getElementAt( index );
         final UserBit userBit = new DefaultUserBit( pipeBitInfo, availablesModel.getPipeBit( pipeBitInfo ) );
         __userBitListModel.addUserBit( userBit );
      }

      @Override
      public void mouseEntered( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      @Override
      public void mouseExited( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      @Override
      public void mouseDragged( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      @Override
      public void mouseMoved( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      private void setFocus( final Point p ) {
         if ( p == null ) {
            if ( _currentFocusIndex >= 0 ) {
               _currentFocusIndex = -1;
               _list.repaint();
            }
            return;
         }
         final int index = _list.locationToIndex( p );
         if ( index == _currentFocusIndex ) {
            return;
         }
         _currentFocusIndex = index;
         _list.repaint();
      }
   }

   static private final class UsersMouseListener extends MouseAdapter {
      private final JList<UserBit> _list;
      private final UserBitListModel __userBitListModel;
      private int _currentFocusIndex = -1;

      private UsersMouseListener( final JList<UserBit> list, final UserBitListModel userBitListModel ) {
         _list = list;
         __userBitListModel = userBitListModel;
      }

      @Override
      public void mouseReleased( final MouseEvent event ) {
         final Point p = _list.getMousePosition();
         final int widthMinusX = _list.getWidth() - p.x;
         if ( widthMinusX > 65 ) {
            return;
         }
         _list.getSelectionModel().clearSelection();
         UserBitRenderer.SUSPEND_BUTTONS = true;
         final int index = _list.locationToIndex( p );
         if ( widthMinusX > 45 ) {
            __userBitListModel.moveUserBitUp( index );
         } else if ( widthMinusX > 25 ) {
            __userBitListModel.moveUserBitDown( index );
         } else {
            __userBitListModel.removeUserBit( index );
         }
         UserBitRenderer.SUSPEND_BUTTONS = false;
         _list.repaint();
      }

      @Override
      public void mouseEntered( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      @Override
      public void mouseExited( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      @Override
      public void mouseDragged( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      @Override
      public void mouseMoved( final MouseEvent event ) {
         setFocus( _list.getMousePosition() );
      }

      private void setFocus( final Point p ) {
         if ( p == null ) {
            if ( _currentFocusIndex >= 0 ) {
               _currentFocusIndex = -1;
               _list.repaint();
            }
            return;
         }
         final int index = _list.locationToIndex( p );
         if ( index == _currentFocusIndex ) {
            return;
         }
         _currentFocusIndex = index;
         _list.repaint();
      }
   }


}
