package org.apache.ctakes.gui.pipeline.bit.info;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipeBitInfoUtil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.Role.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/23/2016
 */
public enum PipeBitInfoComparator implements Comparator<PipeBitInfo> {
   INSTANCE;

   static public PipeBitInfoComparator getInstance() {
      return INSTANCE;
   }

   static private final Map<PipeBitInfo.Role, Integer> ROLE_ORDER = new EnumMap<>( PipeBitInfo.Role.class );

   static {
      ROLE_ORDER.put( READER, 1 );
      ROLE_ORDER.put( ANNOTATOR, 2 );
      ROLE_ORDER.put( WRITER, 3 );
      ROLE_ORDER.put( SPECIAL, 4 );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int compare( final PipeBitInfo info1, final PipeBitInfo info2 ) {
      if ( info1.role() != info2.role() ) {
         return compareByRole( info1, info2 );
      }

      if ( PipeBitInfoUtil.isUnknown( info1 ) != PipeBitInfoUtil.isUnknown( info2 ) ) {
         return PipeBitInfoUtil.isUnknown( info1 ) ? 1 : -1;
      }
      final int depends = compareByDependency( info1, info2 );
      if ( depends != 0 ) {
         return depends;
      }
      final int usable = compareByMax( info1.usables(), info2.usables() );
      if ( usable != 0 ) {
         return usable;
      }
      return info1.name().compareTo( info2.name() );
   }

   static private int compareByRole( final PipeBitInfo info1, final PipeBitInfo info2 ) {
      return ROLE_ORDER.get( info1.role() ) - ROLE_ORDER.get( info2.role() );
   }

   static private int compareByMax( final PipeBitInfo.TypeProduct[] types1, final PipeBitInfo.TypeProduct[] types2 ) {
      final int max1 = Arrays.stream( types1 ).mapToInt( Enum::ordinal ).max().orElse( 0 );
      final int max2 = Arrays.stream( types2 ).mapToInt( Enum::ordinal ).max().orElse( 0 );
      return max1 - max2;
   }

   static private int compareBySum( final PipeBitInfo.TypeProduct[] types1, final PipeBitInfo.TypeProduct[] types2 ) {
      final int sum1 = Arrays.stream( types1 ).mapToInt( Enum::ordinal ).sum();
      final int sum2 = Arrays.stream( types2 ).mapToInt( Enum::ordinal ).sum();
      return sum1 - sum2;
   }

   static private int compareByDependency( final PipeBitInfo info1, final PipeBitInfo info2 ) {
      // The info with the lower dependency should go first
      final int dependMax = compareByMax( info1.dependencies(), info2.dependencies() );
      if ( dependMax != 0 ) {
         return dependMax;
      }
      // The info with the lower production should go first
      final int produceMax = compareByMax( info1.products(), info2.products() );
      if ( produceMax != 0 ) {
         return produceMax;
      }
      // The info with the lowest sum dependencies should go first
      final int dependSum = compareBySum( info1.dependencies(), info2.dependencies() );
      if ( dependSum != 0 ) {
         return dependSum;
      }
      // The info with the lowest sum products should go first
      final int produceSum = compareBySum( info1.products(), info2.products() );
      if ( produceSum != 0 ) {
         return produceSum;
      }
      // The info with the lower number of dependencies should go first
      if ( info1.dependencies().length != info2.dependencies().length ) {
         return info1.dependencies().length - info2.dependencies().length;
      }
      // The info with the lower number of products should go first
      return info1.products().length - info2.products().length;
   }


}
