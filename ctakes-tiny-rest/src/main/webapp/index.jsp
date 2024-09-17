<!DOCTYPE html>

<html>
   <head>
      <link rel="stylesheet" type="text/css" href="css/index.css">
      <title> cTAKES Proof of Function Service </title>
      <script type="text/javascript" src="js/jquery.js"></script>
      <script type="text/javascript" src="js/app.js"></script>
      <script>var myContextPath = "${pageContext.request.contextPath}"</script>
   </head>
   <body>
      <img src="http://ctakes.apache.org/images/ctakes_logo.jpg" alt="Apache cTAKES" class="center"/>
      <h3>Apache cTAKES 7.0.0-SNAPSHOT Tiny REST Service Tester</h3>
      <div class="center">
      <textarea id="documentText" name="documentText" rows="20" cols="80" placeholder="Enter your text for analysis."></textarea>
      <br>
      <label>Process to: </label>
      <input type="button" class="button" value="FHIR" name="fhirjson" id="fhirjson"/>
      <input type="button" class="button" value="Text" name="pretty" id="pretty"/>
      <input type="button" class="button" value="Property" name="props" id="props"/>
      <input type="button" class="button" value="UMLS" name="umlsJson" id="umlsJson"/>
      <input type="button" class="button" value="CUI" name="cui" id="cui"/>
      <input type="button" class="button" value="XMI" name="xmi" id="xmi"/>
      <br>
      <textarea id="resultText" name="resultText" rows="20" cols="80" readonly placeholder="Your Result will appear here."></textarea>
      <br>
      </div>
   </body>
</html>
