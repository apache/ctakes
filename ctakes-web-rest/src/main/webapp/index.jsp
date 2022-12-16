<!DOCTYPE html>

<html>
    <head>
        <link rel="stylesheet" type="text/css" href="css/index.css">
        <title> Ctakes Rest Service Test </title>
		<script type="text/javascript" src="js/jquery.js"></script>			
		<script type="text/javascript" src="js/app.js"></script>
        <script>var myContextPath = "${pageContext.request.contextPath}"</script>
    </head>
    <body>
        <img src="http://ctakes.apache.org/images/ctakes_logo.jpg"/>
        <br>Version: 5.0.0</br>
		</br>
        <h3>cTAKES REST SERVICE TESTING</h3>		
        <textarea id="analysisText" name="analysisText" rows="6" cols="75" onFocus="this.value=''">Enter your text for analysis</textarea><br>
        <input type="button" value="Analyze" name="fire" id="fire"/>
    </body>
</html>
