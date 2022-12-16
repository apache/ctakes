$(document).ready(function() {
     $('#fhirjson').click(function() {
         document.getElementById("resultText").innerHTML = "Processing ...";
         try {
            $.ajax({
				url: myContextPath + "/service/process?format=fhir",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("documentText").value,
				error: function(xhr, statusText, error) {
					 document.getElementById("resultText").innerHTML = "Error processing REST call";
				},
				success: function(response, statusText, xhr) {
				   document.getElementById("resultText").innerHTML = response;
				}
			})
        } catch (err) {
             document.getElementById("resultText").innerHTML = "Error invoking REST call";
        }
    });
     $('#pretty').click(function() {
         document.getElementById("resultText").innerHTML = "Processing ...";
         try {
            $.ajax({
				url: myContextPath + "/service/process?format=pretty",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("documentText").value,
				error: function(xhr, statusText, error) {
					 document.getElementById("resultText").innerHTML = "Error processing REST call";
				},
				success: function(response, statusText, xhr) {
				   document.getElementById("resultText").innerHTML = response;
				}
			})
        } catch (err) {
             document.getElementById("resultText").innerHTML = "Error invoking REST call";
        }
    });
     $('#props').click(function() {
         document.getElementById("resultText").innerHTML = "Processing ...";
         try {
            $.ajax({
				url: myContextPath + "/service/process?format=property",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("documentText").value,
				error: function(xhr, statusText, error) {
					 document.getElementById("resultText").innerHTML = "Error processing REST call";
				},
				success: function(response, statusText, xhr) {
				   document.getElementById("resultText").innerHTML = response;
				}
			})
        } catch (err) {
             document.getElementById("resultText").innerHTML = "Error invoking REST call";
        }
    });
     $('#umlsJson').click(function() {
         document.getElementById("resultText").innerHTML = "Processing ...";
         try {
            $.ajax({
				url: myContextPath + "/service/process?format=umls",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("documentText").value,
				error: function(xhr, statusText, error) {
					 document.getElementById("resultText").innerHTML = "Error processing REST call";
				},
				success: function(response, statusText, xhr) {
				   document.getElementById("resultText").innerHTML = response;
				}
			})
        } catch (err) {
             document.getElementById("resultText").innerHTML = "Error invoking REST call";
        }
    });
     $('#cui').click(function() {
         document.getElementById("resultText").innerHTML = "Processing ...";
         try {
            $.ajax({
				url: myContextPath + "/service/process?format=cui",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("documentText").value,
				error: function(xhr, statusText, error) {
					 document.getElementById("resultText").innerHTML = "Error processing REST call";
				},
				success: function(response, statusText, xhr) {
				   document.getElementById("resultText").innerHTML = response;
				}
			})
        } catch (err) {
             document.getElementById("resultText").innerHTML = "Error invoking REST call";
        }
    });
     $('#xmi').click(function() {
         document.getElementById("resultText").innerHTML = "Processing ...";
         try {
            $.ajax({
				url: myContextPath + "/service/process?format=xmi",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("documentText").value,
				error: function(xhr, statusText, error) {
					 document.getElementById("resultText").innerHTML = "Error processing REST call";
				},
				success: function(response, statusText, xhr) {
				   document.getElementById("resultText").innerHTML = response;
				}
			})
        } catch (err) {
             document.getElementById("resultText").innerHTML = "Error invoking REST call";
        }
    });
});
