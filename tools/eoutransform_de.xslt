<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0"/>
<xsl:template match ="/">
    <html>
    <body title="EFA Versionshistorie">
    <h1><b>EFA Versionshistorie</b></h1>
    <xsl:for-each select="efaOnlineUpdate/Version">
	<h2><b><xsl:value-of select="VersionID"/>  Release Date: <xsl:value-of select="ReleaseDate"/></b></h2>
	<xsl:for-each select="Changes[@lang='de']">
	    <ul>
	    <xsl:for-each select="ChangeItem">
		<li>
		    <xsl:value-of select="."/>
		</li>
	    </xsl:for-each>
	    </ul>
	</xsl:for-each>
    </xsl:for-each>
    </body>
    </html>
</xsl:template>
</xsl:stylesheet>