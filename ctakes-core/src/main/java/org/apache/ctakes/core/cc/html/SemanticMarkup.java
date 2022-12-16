package org.apache.ctakes.core.cc.html;

import org.apache.ctakes.core.util.annotation.SemanticGroup;

import static org.apache.ctakes.core.util.annotation.SemanticGroup.*;


public enum SemanticMarkup {
   DRUG_MARK( DRUG, "_DRG_", "Red", "25B3" ),
   // up triangle
   DISORDER_MARK( DISORDER, "_DIS_", "Black", "25BD" ),
   // down triangle
   FINDING_MARK( FINDING, "_FND_", "Magenta", "2022" ),
   // round bullet
   PROCEDURE_MARK( PROCEDURE, "_PRC_", "Blue", "25C6" ),
   // diamond
   ANATOMY_MARK( ANATOMY, "_ANT_", "Gray", "25C9" ),
   // fish eye / target
   CLINICAL_ATTRIBUTE_MARK( CLINICAL_ATTRIBUTE, "_ATT_", "Gray", "25FE" ),
   // filled square
   DEVICE_MARK( DEVICE, "_DEV_", "Blue", "2600" ),
   // black sun
   LAB_MARK( LAB, "_LAB_", "Gray", "2714" ),
   // check mark
   PHENOMENON_MARK( PHENOMENON, "_PHN_", "Magenta", "2604" ),
   // comet
   SUBJECT_MARK( SUBJECT, "_SBJ_", "CadetBlue", "2605" ),
   // star
   TITLE_MARK( TITLE, "_TTL_", "CadetBlue", "2605" ),
   // star
   EVENT_MARK( EVENT, "_EVT_", "DarkSeaGreen", "263C" ),
   // white sun
   ENTITY_MARK( ENTITY, "_ENT_", "Black", "25FE" ),
   // filled square
   TIME_MARK( TIME, "_TMX_", "Black", "2742" ),
   // star ring
   MODIFIER_MARK( MODIFIER, "_MOD_", "Coral", "259F" ),
   // curving arrow
   LAB_MODIFIER_MARK( LAB_MODIFIER, "_LABM_", "Coral", "21B7" ),
   // curving arrow
   UNKNOWN_MARK( UNKNOWN, "_UNK_", "DarkMagenta", "2753" );
   // question mark


   private final SemanticGroup _group;
   private final String _encoding;
   private final String _color;
   private final String _asterisk;

   SemanticMarkup( final SemanticGroup group, final String encoding, final String color, final String asterisk ) {
      _group = group;
      _encoding = encoding;
      _color = color;
      _asterisk = asterisk;
   }

   public SemanticGroup getGroup() {
      return _group;
   }

   public String getEncoding() {
      return _encoding;
   }

   public String getColor() {
      return _color;
   }

   public String getAsterisk() {
      return _asterisk;
   }

   static public SemanticMarkup getMarkup( final SemanticGroup group ) {
      for ( SemanticMarkup markup : values() ) {
         if ( markup._group == group ) {
            return markup;
         }
      }
      return UNKNOWN_MARK;
   }

}
