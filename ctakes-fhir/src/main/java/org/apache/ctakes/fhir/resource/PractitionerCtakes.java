package org.apache.ctakes.fhir.resource;


import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;

import static org.apache.ctakes.fhir.element.FhirElementFactory.DIVIDER_CHAR;

/**
 * https://www.hl7.org/fhir/practitioner.html
 * Even though ctakes is not human, registering it as a Practitioner provides reference to information source and devlist contact
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
public enum PractitionerCtakes implements FhirPractitioner {
   INSTANCE;

   static public PractitionerCtakes getInstance() {
      return INSTANCE;
   }

   @Override
   public String getName() {
      return "cTAKES";
   }

   @Override
   public String getFamilyName() {
      return "Apache";
   }

   @Override
   public String getVersion() {
      return "4" + DIVIDER_CHAR + "0" + DIVIDER_CHAR + "1";
   }

   @Override
   public String getContactEmail() {
      return "dev@ctakes.apache.org";
   }

   final private Practitioner _ctakes;
   final private Reference _ctakesReference;

   final private Organization _apache;
   final private Reference _apacheReference;

   PractitionerCtakes() {
      _ctakes = createPractitioner();
      _ctakesReference = new Reference( _ctakes );
      _apache = createOrganization();
      _apacheReference = new Reference( _apache );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Practitioner getPractitioner() {
      return _ctakes;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Reference getPractitionerReference() {
      return _ctakesReference;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Organization getOrganization() {
      return _apache;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Reference getOrganizationReference() {
      return _apacheReference;
   }

}
