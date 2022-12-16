package org.apache.ctakes.core.pipeline;

import com.lexicalscope.jewel.cli.Option;

/**
 * Defines command line interface single-character options that are not ctakes defaults.
 * This allows -a SomeValue -b SomeValue -c SomeValue ... -1 SomeValue -2 SomeValue ... -A SomeValue -B SomeValue ...
 * Reserved option characters are -p (PiperFile), -i (InputDirectory), -o (OutputDirectory).
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2017
 */
public interface CliOptionals extends StandardCliOptions {

   @Option(
         shortName = "a",
         defaultValue = "" )
   String getOption_a();

   @Option(
         shortName = "b",
         defaultValue = "" )
   String getOption_b();

   @Option(
         shortName = "c",
         defaultValue = "" )
   String getOption_c();

   @Option(
         shortName = "d",
         defaultValue = "" )
   String getOption_d();

   @Option(
         shortName = "e",
         defaultValue = "" )
   String getOption_e();

   @Option(
         shortName = "f",
         defaultValue = "" )
   String getOption_f();

   @Option(
         shortName = "g",
         defaultValue = "" )
   String getOption_g();

   @Option(
         shortName = "h",
         defaultValue = "" )
   String getOption_h();

   // -i is reserved for common parameter "InputDirectory"

   @Option(
         shortName = "j",
         defaultValue = "" )
   String getOption_j();

   @Option(
         shortName = "k",
         defaultValue = "" )
   String getOption_k();

   // -l is reserved for common parameter "LookupXml"

   @Option(
         shortName = "m",
         defaultValue = "" )
   String getOption_m();

   @Option(
         shortName = "n",
         defaultValue = "" )
   String getOption_n();

   // -o is reserved for common parameter "OutputDirectory"

   // -p is reserved for common parameter "PiperFile"

   @Option(
         shortName = "q",
         defaultValue = "" )
   String getOption_q();

   @Option(
         shortName = "r",
         defaultValue = "" )
   String getOption_r();

   // -s is reserved for common parameter "SubDirectory"

   @Option(
         shortName = "t",
         defaultValue = "" )
   String getOption_t();

   @Option(
         shortName = "u",
         defaultValue = "" )
   String getOption_u();

   @Option(
         shortName = "v",
         defaultValue = "" )
   String getOption_v();

   @Option(
         shortName = "w",
         defaultValue = "" )
   String getOption_w();

   @Option(
         shortName = "x",
         defaultValue = "" )
   String getOption_x();

   @Option(
         shortName = "y",
         defaultValue = "" )
   String getOption_y();

   @Option(
         shortName = "z",
         defaultValue = "" )
   String getOption_z();

   @Option(
         shortName = "0",
         defaultValue = "" )
   String getOption_0();

   @Option(
         shortName = "1",
         defaultValue = "" )
   String getOption_1();

   @Option(
         shortName = "2",
         defaultValue = "" )
   String getOption_2();

   @Option(
         shortName = "3",
         defaultValue = "" )
   String getOption_3();

   @Option(
         shortName = "4",
         defaultValue = "" )
   String getOption_4();

   @Option(
         shortName = "5",
         defaultValue = "" )
   String getOption_5();

   @Option(
         shortName = "6",
         defaultValue = "" )
   String getOption_6();

   @Option(
         shortName = "7",
         defaultValue = "" )
   String getOption_7();

   @Option(
         shortName = "8",
         defaultValue = "" )
   String getOption_8();

   @Option(
         shortName = "9",
         defaultValue = "" )
   String getOption_9();

   @Option(
         shortName = "A",
         defaultValue = "" )
   String getOption_A();

   @Option(
         shortName = "B",
         defaultValue = "" )
   String getOption_B();

   @Option(
         shortName = "C",
         defaultValue = "" )
   String getOption_C();

   @Option(
         shortName = "D",
         defaultValue = "" )
   String getOption_D();

   @Option(
         shortName = "E",
         defaultValue = "" )
   String getOption_E();

   @Option(
         shortName = "F",
         defaultValue = "" )
   String getOption_F();

   @Option(
         shortName = "G",
         defaultValue = "" )
   String getOption_G();

   @Option(
         shortName = "H",
         defaultValue = "" )
   String getOption_H();

   // -i is reserved for common parameter "InputDirectory"

   @Option(
         shortName = "J",
         defaultValue = "" )
   String getOption_J();

   @Option(
         shortName = "K",
         defaultValue = "" )
   String getOption_K();

   // -l is reserved for common parameter "LookupXml"

   @Option(
         shortName = "M",
         defaultValue = "" )
   String getOption_M();

   @Option(
         shortName = "N",
         defaultValue = "" )
   String getOption_N();

   // -o is reserved for common parameter "OutputDirectory"

   // -p is reserved for common parameter "PiperFile"

   @Option(
         shortName = "Q",
         defaultValue = "" )
   String getOption_Q();

   @Option(
         shortName = "R",
         defaultValue = "" )
   String getOption_R();

   // -s is reserved for common parameter "SubDirectory"

   @Option(
         shortName = "T",
         defaultValue = "" )
   String getOption_T();

   @Option(
         shortName = "U",
         defaultValue = "" )
   String getOption_U();

   @Option(
         shortName = "V",
         defaultValue = "" )
   String getOption_V();

   @Option(
         shortName = "W",
         defaultValue = "" )
   String getOption_W();

   @Option(
         shortName = "X",
         defaultValue = "" )
   String getOption_X();

   @Option(
         shortName = "Y",
         defaultValue = "" )
   String getOption_Y();

   @Option(
         shortName = "Z",
         defaultValue = "" )
   String getOption_Z();

}