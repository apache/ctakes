package tests;

import org.apache.ctakes.core.cc.XMISerializer;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.typesystem.type.refsem.MedicationStrength;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationStrengthModifier;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;

public class xmi_test_for_FSAarray_error {
    public static void main(String[] args) throws UIMAException {
        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText("Patient takes 40 mg Aspirin per day");

        // Create our main medication Aspirin
        MedicationEventMention medEventMention = new MedicationEventMention( jCas, 20, 27 );
        medEventMention.addToIndexes();

        // Create the strength modifier, which has an actual location in the text  40 mg
        MedicationStrengthModifier medicationStrengthModifier = new MedicationStrengthModifier( jCas, 14, 19 );
        medicationStrengthModifier.addToIndexes();

        //  Create the strength attribute - a conceptual entity that doesn't exist in the text but
        //  knows of modifiers that exist  in the text.
        MedicationStrength medicationStrength = new MedicationStrength( jCas );
        medicationStrength.addToIndexes();

        // FSArray is a "primitive" type of array required by uima.
        // We can't use pojo (plain old java object.  e.g. java.util.List<>, java.util.Set<> ) collections,
        // so we have to put modifiers into this FSArray
        FSArray mentions = new FSArray( jCas, 1 );
        mentions.addToIndexes();
        mentions.set( 0, medicationStrengthModifier );

        // Add the modifier mention to the attribute
        medicationStrength.setMentions( mentions );

        // set the attribute of the medication
        medEventMention.setMedicationStrength( medicationStrength );

        // write to xmi.
        final File file = new File( args[0] + ".xmi" );
        try {
            try ( OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file.toPath())) ) {
                XmiCasSerializer casSerializer = new XmiCasSerializer(jCas.getTypeSystem() );
                XMISerializer xmiSerializer = new XMISerializer( outputStream );
                casSerializer.serialize(jCas.getCas(), xmiSerializer.getContentHandler() );
            }
        } catch (IOException | SAXException multE ) {
            //
        }
    }
}