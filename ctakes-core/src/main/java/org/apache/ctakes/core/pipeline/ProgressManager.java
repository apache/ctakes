package org.apache.ctakes.core.pipeline;

import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/4/2019
 */
public enum ProgressManager {
   INSTANCE;

   public static ProgressManager getInstance() {
      return INSTANCE;
   }

   static public final String PROGRESS_COMPLETE = "PROGRESS_COMPLETE";

   private String _name;
   private String _patientId;
   private String _docId;
   private final BoundedRangeModel _model;

   ProgressManager() {
      _model = new DefaultBoundedRangeModel();
   }

   public void initializeProgress( final String name, final int max ) {
      _name = name;
      _model.setRangeProperties( 0, 0, 0, max, false );
   }

   public String getName() {
      if ( _name == null || _name.trim()
                                 .isEmpty() ) {
         return "Progress";
      }
      return _name;
   }

   public void updatePatientId( final String patientId ) {
      _patientId = patientId;
   }

   public String getPatientId() {
      if ( _patientId == null || _patientId.trim()
                                           .isEmpty()
           || _patientId.equals( PROGRESS_COMPLETE ) || _patientId.equals( SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
         return "";
      }
      return _patientId;
   }

   public void updateDocId( final String docId ) {
      _docId = docId;
   }

   public String getDocId() {
      if ( _docId == null || _docId.trim()
                                   .isEmpty()
           || _docId.equals( PROGRESS_COMPLETE ) || _docId.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
         return "";
      }
      return _docId;
   }

   public void updateProgress( final int value ) {
      if ( value <= _model.getValue() ) {
         return;
      }
      if ( _model.getMaximum() < value ) {
         _model.setMaximum( value );
      }
      _model.setValue( value );
   }

   public void updateProgress( final int value, final int max ) {
      _model.setMaximum( max );
      updateProgress( value );
   }

   public BoundedRangeModel getModel() {
      return _model;
   }

   public void addListener( final ChangeListener listener ) {
      _model.addChangeListener( listener );
   }

   public void removeListener( final ChangeListener listener ) {
      _model.removeChangeListener( listener );
   }


}
