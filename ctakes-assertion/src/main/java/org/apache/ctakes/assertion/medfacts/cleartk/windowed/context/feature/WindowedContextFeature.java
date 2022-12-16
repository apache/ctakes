package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature;

import org.cleartk.ml.Feature;

import java.util.Locale;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedContextFeature extends Feature {
   private static final long serialVersionUID = 1L;
   public Feature feature;

   public WindowedContextFeature( String baseName, Feature feature ) {
      this.feature = feature;
      this.setName( Feature.createName( baseName, feature.getName() ) );
      this.setValue( this.feature.getValue() );
   }

   public WindowedContextFeature( String baseName, int position, Feature feature ) {
      this.feature = feature;
      this.setName( Feature.createName( baseName, String.valueOf( position ), feature.getName() ) );
      this.setValue( feature.getValue() );
   }

   public WindowedContextFeature( String baseName, int position, int oobPosition, String featureName ) {
      this.feature = new Feature( featureName, String.format( Locale.ROOT, "OOB%d", oobPosition ) );
      this.setName( Feature.createName( baseName, String.valueOf( position ), featureName ) );
      this.setValue( this.feature.getValue() );
   }

}
