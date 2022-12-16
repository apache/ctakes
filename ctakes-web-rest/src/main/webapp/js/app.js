$(document).ready(function() {
     $('#fire').click(function() {
        try {
            $.ajax({
				url: myContextPath + "/service/analyze?pipeline=Default",
				type: "POST",
				crossDomain: true,
				cache: false,
				async: true,
				data: document.getElementById("analysisText").value,
				error: function(xhr, statusText, error) {
					 alert("Error processing REST call");
				},
				success: function(response, statusText, xhr) {					
					alert("Text analyzed successfully !!! Response JSON - " + JSON.stringify(response));
				}
			})
        } catch (err) {
            alert("Error invoking REST call - " + err.message);
        }

    });
});
