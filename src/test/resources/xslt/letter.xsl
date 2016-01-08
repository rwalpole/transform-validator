<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:strip-space elements="*" />
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/letter">
        <html>
            <head>
                <title><xsl:value-of select="concat('Letter from ',from, ' to ', to)"/></title>
            </head>
            <body>
                <div>
                    <h1><xsl:value-of select="concat('Letter from ',from, ' to ', to)"/></h1>
                </div>
                <xsl:apply-templates/>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="body">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="opening">
        <div><xsl:value-of select="."/></div>
    </xsl:template>
    <xsl:template match="para">
        <div><xsl:value-of select="."/></div>
    </xsl:template>
    <xsl:template match="closing">
        <div><xsl:value-of select="."/></div>
    </xsl:template>
    <xsl:template match="from"/>
    <xsl:template match="to"/>
    <xsl:template match="date"/>
    <xsl:template match="place"/>
</xsl:stylesheet>