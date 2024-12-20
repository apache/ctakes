package org.apache.ctakes.core.pipeline;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2017
 */
final public class CliOptionalsHandler {

   static private final Logger LOGGER = LoggerFactory.getLogger( "CliOptionalsHandler" );

   private CliOptionalsHandler() {
   }

   /**
    * Brute force method to get option values
    *
    * @param optionals  -
    * @param name character option
    * @return the value specified on the command line for the given character
    */
   static public String getCliOptionalValue( final CliOptionals optionals, final String name ) {
      switch ( name ) {
         case "i":
            return optionals.getInputDirectory();
         case "o":
            return optionals.getOutputDirectory();
         case StandardCliOptions.USER_NAME:
            return optionals.getUserName();
         case StandardCliOptions.PASSWORD:
            return optionals.getPassword();
         case StandardCliOptions.UMLS_KEY:
            return optionals.getUmlsApiKey();
         case StandardCliOptions.XMI_OUT_DIR:
            return optionals.getXmiOutDirectory();
         case StandardCliOptions.HTML_OUT_DIR:
            return optionals.getHtmlOutDirectory();
         case StandardCliOptions.SUB_DIR:
            return optionals.getSubDirectory();
         case StandardCliOptions.PIP_PBJ:
            return optionals.getPipPbj();
         case "a":
            return optionals.getOption_a();
         case "b":
            return optionals.getOption_b();
         case "c":
            return optionals.getOption_c();
         case "d":
            return optionals.getOption_d();
         case "e":
            return optionals.getOption_e();
         case "f":
            return optionals.getOption_f();
         case "g":
            return optionals.getOption_g();
         case "h":
            return optionals.getOption_h();
         case "j":
            return optionals.getOption_j();
         case "k":
            return optionals.getOption_k();
         case "l":
            return optionals.getOption_l();
         case "m":
            return optionals.getOption_m();
         case "n":
            return optionals.getOption_n();
         case "q":
            return optionals.getOption_q();
         case "r":
            return optionals.getOption_r();
         case "s":
            return optionals.getOption_s();
         case "t":
            return optionals.getOption_t();
         case "u":
            return optionals.getOption_u();
         case "v":
            return optionals.getOption_v();
         case "w":
            return optionals.getOption_w();
         case "x":
            return optionals.getOption_x();
         case "y":
            return optionals.getOption_y();
         case "z":
            return optionals.getOption_z();
         case "0":
            return optionals.getOption_0();
         case "1":
            return optionals.getOption_1();
         case "2":
            return optionals.getOption_2();
         case "3":
            return optionals.getOption_3();
         case "4":
            return optionals.getOption_4();
         case "5":
            return optionals.getOption_5();
         case "6":
            return optionals.getOption_6();
         case "7":
            return optionals.getOption_7();
         case "8":
            return optionals.getOption_8();
         case "9":
            return optionals.getOption_9();
         case "A":
            return optionals.getOption_A();
         case "B":
            return optionals.getOption_B();
         case "C":
            return optionals.getOption_C();
         case "D":
            return optionals.getOption_D();
         case "E":
            return optionals.getOption_E();
         case "F":
            return optionals.getOption_F();
         case "G":
            return optionals.getOption_G();
         case "H":
            return optionals.getOption_H();
         case "J":
            return optionals.getOption_J();
         case "K":
            return optionals.getOption_K();
         case "L":
            return optionals.getOption_L();
         case "M":
            return optionals.getOption_M();
         case "N":
            return optionals.getOption_N();
         case "Q":
            return optionals.getOption_Q();
         case "R":
            return optionals.getOption_R();
         case "S":
            return optionals.getOption_S();
         case "T":
            return optionals.getOption_T();
         case "U":
            return optionals.getOption_U();
         case "V":
            return optionals.getOption_V();
         case "W":
            return optionals.getOption_W();
         case "X":
            return optionals.getOption_X();
         case "Y":
            return optionals.getOption_Y();
         case "Z":
            return optionals.getOption_Z();
      }
      final List<String> userDefined = optionals.getUserDefined();
      if ( userDefined != null ) {
         for ( int i = 0; i < userDefined.size() - 1; i++ ) {
            if ( userDefined.get( i ).equals( "++" + name ) ) {
               LOGGER.debug( "Using value \"{}\" for dynamic command line parameter \"++{}\"", userDefined.get( i + 1 ),
                     name );
               return userDefined.get( i + 1 );
            }
         }
      }
      LOGGER.warn( "No value specified on command line for parameter \"{}\"", name );
      return "";
   }

}
