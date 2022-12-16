<?xml version="1.0" encoding="ISO-8859-1"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:t="http://uima.apache.org/resourceSpecifier">

<xsl:template match="/t:typeSystemDescription">
  <html>
  <body>
  <h2>Apache cTAKES Data Dictionary </h2>
  <table border="1">
    <tr bgcolor="#9acd32">
      <th>Type</th>
      <th>Features</th>
    </tr>
    <xsl:for-each select="t:types/t:typeDescription">
    <xsl:sort select="t:name"/>
    <tr>
      <td>
      <b><xsl:value-of select="t:name" />
      </b>
      <!-- extends <xsl:value-of select="t:supertypeName" />  -->
      <p>
      <xsl:value-of select="t:description" />
      </p>
      </td>
      <td>
		      <table border="1">
		          <xsl:for-each select="t:features/t:featureDescription">
				    <tr>
				      <td><xsl:value-of select="t:name" /></td>
				      <td><xsl:value-of select="t:description" /></td>
				    </tr>
				    </xsl:for-each>  
		      </table>
      </td>
    </tr>  
    </xsl:for-each>
  </table>
  </body>
  </html>
</xsl:template>

</xsl:stylesheet>