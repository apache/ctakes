package org.apache.ctakes.gui.pipeline.bit.user;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.pipeline.bit.BitInfoPanel;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/31/2017
 */
final public class UserBitInfoPanel extends BitInfoPanel {

   static private final Logger LOGGER = Logger.getLogger( "UserBitInfoPanel" );

   private BitNameListener _bitNameListener = new BitNameListener();

   public void setUserBitList( final JList<UserBit> userBitList ) {
      userBitList.getSelectionModel().addListSelectionListener( new UserBitListListener( userBitList ) );
   }

   @Override
   protected String getNameLabelPrefix() {
      return "User";
   }

   @Override
   protected JComponent createNameEditor() {
      _bitNameListener = new BitNameListener();
      final JTextField textField = new JTextField();
      textField.addActionListener( _bitNameListener );
      return textField;
   }

   @Override
   protected void setBitName( final String text, final String toolTip ) {
      ((JTextComponent)_name).setText( text );
      _name.setToolTipText( toolTip );
   }

   @Override
   protected ParameterInfoPanel createParameterInfoPanel() {
      return new UserParameterInfoPanel();
   }

   @Override
   protected void clear() {
      _bitNameListener.setUserBit( null );
      super.clear();
   }

   private void setUserBit( final UserBit userBit ) {
      if ( userBit == null ) {
         clear();
         return;
      }
      _pipeBitInfo = userBit.getPipeBitInfo();
      _pipeBitClass = userBit.getPipeBitClass();
      _bitNameListener.setUserBit( null );
      setBitName( _pipeBitInfo.name(), _pipeBitClass.getName() );
      setLabelText( _description, _pipeBitInfo.description(), _pipeBitInfo.description() );
      final String dependencies = Arrays.stream( _pipeBitInfo.dependencies() )
            .map( PipeBitInfo.TypeProduct::toString )
            .collect( Collectors.joining( ", " ) );
      setLabelText( _dependencies, dependencies, dependencies );
      final String usables = Arrays.stream( _pipeBitInfo.usables() )
            .map( PipeBitInfo.TypeProduct::toString )
            .collect( Collectors.joining( ", " ) );
      setLabelText( _usables, usables, usables );
      final String products = Arrays.stream( _pipeBitInfo.products() )
            .map( PipeBitInfo.TypeProduct::toString )
            .collect( Collectors.joining( ", " ) );
      setLabelText( _products, products, products );
      _parameterTableModel.setParameterHolder( userBit );
      _parameterInfoPanel.setParameterHolder( userBit );
      _bitNameListener.setUserBit( userBit );
   }

   private class UserBitListListener implements ListSelectionListener {
      private final JList<UserBit> __userBitList;

      private UserBitListListener( final JList<UserBit> userBitList ) {
         __userBitList = userBitList;
      }

      @Override
      public void valueChanged( final ListSelectionEvent event ) {
         if ( event.getValueIsAdjusting() ) {
            return;
         }
         final UserBit userBit = __userBitList.getSelectedValue();
         setUserBit( userBit );
      }
   }

   private class BitNameListener implements ActionListener {
      private UserBit __userBit;

      private void setUserBit( final UserBit userBit ) {
         __userBit = userBit;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( __userBit != null ) {
            __userBit.setBitName( ((JTextComponent)_name).getText() );
         }
      }
   }

}
