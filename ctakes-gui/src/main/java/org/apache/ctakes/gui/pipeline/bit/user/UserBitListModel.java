package org.apache.ctakes.gui.pipeline.bit.user;

import org.apache.ctakes.core.pipeline.PipeBitInfo;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/19/2017
 */
final public class UserBitListModel extends AbstractListModel<UserBit> {

   static private final Logger LOGGER = Logger.getLogger( "UserBitListModel" );

   private UserBit _readerBit = null;
   private final List<UserBit> _userBits = new ArrayList<>();


   private void setReaderBit( final UserBit reader ) {
      if ( _readerBit != null ) {
         _readerBit = reader;
         fireContentsChanged( this, 0, 0 );
         return;
      }
      _readerBit = reader;
      fireIntervalAdded( this, 0, 0 );
   }

   public void addUserBit( final UserBit userBit ) {
      insertUserBit( getSize(), userBit );
   }

   private void insertUserBit( final int viewIndex, final UserBit userBit ) {
      if ( userBit.getPipeBitInfo().role() == PipeBitInfo.Role.READER ) {
         setReaderBit( userBit );
         return;
      }
      int listIndex = viewIndex;
      if ( _readerBit != null ) {
         listIndex = viewIndex - 1;
      }
      _userBits.add( listIndex, userBit );
      fireIntervalAdded( this, viewIndex, viewIndex );
   }

   public void moveUserBitUp( final int viewIndex ) {
      if ( viewIndex >= getSize() ) {
         LOGGER.warning( "No User Pipe Bit at index " + viewIndex );
         return;
      }
      int listIndex = viewIndex;
      if ( _readerBit != null ) {
         listIndex = viewIndex - 1;
      }
      if ( listIndex <= 0 ) {
         return;
      }
      final UserBit userBit = _userBits.get( listIndex );
      _userBits.remove( listIndex );
      _userBits.add( listIndex - 1, userBit );
   }

   public void moveUserBitDown( final int viewIndex ) {
      if ( viewIndex >= getSize() ) {
         LOGGER.warning( "No User Pipe Bit at index " + viewIndex );
         return;
      }
      int listIndex = viewIndex;
      if ( _readerBit != null ) {
         listIndex = viewIndex - 1;
      }
      if ( listIndex < 0 || listIndex >= _userBits.size() - 1 ) {
         return;
      }
      final UserBit userBit = _userBits.get( listIndex );
      _userBits.remove( listIndex );
      _userBits.add( listIndex + 1, userBit );
   }

   public void removeUserBit( final int viewIndex ) {
      if ( viewIndex >= getSize() ) {
         LOGGER.warning( "No User Pipe Bit at index " + viewIndex );
         return;
      }
      int listIndex = viewIndex;
      if ( _readerBit != null ) {
         if ( viewIndex == 0 ) {
            _readerBit = null;
            return;
         }
         listIndex = viewIndex - 1;
      }
      _userBits.remove( listIndex );
   }

   public UserBit getUserBit( final int viewIndex ) {
      int listIndex = viewIndex;
      if ( _readerBit != null ) {
         if ( viewIndex == 0 ) {
            return _readerBit;
         }
         listIndex = viewIndex - 1;
      }
      return _userBits.get( listIndex );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getSize() {
      return _userBits.size() + (_readerBit == null ? 0 : 1);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public UserBit getElementAt( final int index ) {
      return getUserBit( index );
   }

}
