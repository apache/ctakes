package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.hl7.fhir.dstu3.model.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.apache.ctakes.fhir.element.FhirElementFactory.DIVIDER_CHAR;
import static org.hl7.fhir.dstu3.model.Composition.CompositionAttesterComponent;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/21/2018
 */
public interface FhirPractitioner {


   String getName();

   String getFamilyName();

   String getVersion();

   String getContactEmail();


   /**
    * @return Practitioner representation as the creator/extractor of data
    */
   Practitioner getPractitioner();

   /**
    * @return Reference to the Practitioner representation
    */
   Reference getPractitionerReference();

   /**
    * @return an organization for the practitioner
    */
   Organization getOrganization();

   /**
    * @return a reference to the organization for the practitioner
    */
   Reference getOrganizationReference();

   /**
    * @return Practitioner representation as the creator/extractor of data
    */
   default Practitioner createPractitioner() {
      final HumanName name = new HumanName();
      name.setUse( HumanName.NameUse.OFFICIAL );
      name.setFamily( getFamilyName() );
      name.addGiven( getName() );
      final ContactPoint contact = new ContactPoint();
      contact.setSystem( ContactPoint.ContactPointSystem.EMAIL );
      contact.setValue( getContactEmail() );
      contact.setUse( ContactPoint.ContactPointUse.WORK );
      final Practitioner practitioner = new Practitioner();
      String hostname;
      try {
         hostname = InetAddress.getLocalHost()
               .getHostName();
      } catch ( UnknownHostException uhE ) {
         hostname = "UnknownHost";
      }
      final String userName = System.getProperty( "user.name" );
      practitioner.setId(
            getFamilyName() + DIVIDER_CHAR
            + getName() + DIVIDER_CHAR
            + getVersion() + DIVIDER_CHAR
            + hostname + DIVIDER_CHAR
            + userName );
      practitioner.setActive( true );
      practitioner.addName( name );
      practitioner.addTelecom( contact );
      return practitioner;
   }

   /**
    * @return an organization for the practitioner
    */
   default Organization createOrganization() {
      final Organization organization = new Organization();
      final Practitioner practitioner = getPractitioner();
      final String id = practitioner.getId()
                                    .replace( DIVIDER_CHAR + getName(), "" );
      organization.setId( id );
      organization.setActive( practitioner.getActive() );
      organization.setName( getFamilyName() );
      organization.setTelecom( practitioner.getTelecom() );
      return organization;
   }

   /**
    * @param noteSpecs information about a note
    * @return an Attester for the given note with the Practitioner as the attesting party
    */
   default CompositionAttesterComponent createAttester( final FhirNoteSpecs noteSpecs ) {
      final CompositionAttesterComponent attester = new CompositionAttesterComponent();
      attester.addMode( Composition.CompositionAttestationMode.OFFICIAL );
      attester.setTime( noteSpecs.getNoteDate() );
      attester.setParty( getPractitionerReference() );
      return attester;
   }


}
