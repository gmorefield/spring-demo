<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:strip-space elements="*"/>

    <!-- identity transform -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="claim">
        <xsl:copy>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="policy">
        <xsl:copy>
            <xsl:apply-templates mode="flatten"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="multilevel">
        <xsl:copy>
            <xsl:apply-templates mode="single-level"/>
        </xsl:copy>
    </xsl:template>

    <!--    <xsl:template match="@*|node()" mode="flatten">-->
    <!--        <xsl:copy>-->
    <!--            <xsl:apply-templates select="@*|node()" mode="flatten"/>-->
    <!--        </xsl:copy>-->
    <!--    </xsl:template>-->
    <!--    <xsl:template match="/*/*" mode="flatten">-->
    <!--        <xsl:for-each select="*">-->
    <!--            <xsl:element name="{concat(name(..),'_',name())}">-->
    <!--                <xsl:apply-templates select="node()" mode="flatten"/>-->
    <!--            </xsl:element>-->
    <!--        </xsl:for-each>-->
    <!--    </xsl:template>-->

    <!--    <xsl:template match="/*" mode="flatten">-->
    <!--        <xsl:copy>-->
    <!--            <xsl:apply-templates select="descendant::*[not(*)]" mode="flatten"/>-->
    <!--        </xsl:copy>-->
    <!--    </xsl:template>-->

    <!--    <xsl:template match="*" mode="flatten">-->
    <!--        <xsl:variable name="test" select="string-join(ancestor-or-self::*[not(position()=last())]/name(),'-')"/>-->
    <!--        <xsl:element name="{$test}">-->
    <!--            <xsl:value-of select="."/>-->
    <!--        </xsl:element>-->
    <!--    </xsl:template>-->

    <!-- flattens one level, but not 2 -->
    <!--    <xsl:template match="@*|node()" mode="flatten">-->
    <!--        <xsl:copy>-->
    <!--            <xsl:apply-templates select="@*|node()" mode="flatten"/>-->
    <!--        </xsl:copy>-->
    <!--    </xsl:template>-->
    <!--    <xsl:template match="*/*" mode="flatten">-->
    <!--        <xsl:for-each select="*">-->
    <!--            <xsl:element name="{concat(name(..),'_',name())}">-->
    <!--                <xsl:apply-templates select="node()" mode="flatten"/>-->
    <!--            </xsl:element>-->
    <!--        </xsl:for-each>-->
    <!--    </xsl:template> -->


    <xsl:template match="*" mode="flatten">
        <xsl:param name="namePrefix" select="''"/>
        <xsl:param name="ancestorAttributes" select="/.."/>

        <xsl:variable name="fqName" select="concat($namePrefix,local-name(),'-')"/>

        <xsl:variable name="allAttributes">
            <xsl:for-each select="$ancestorAttributes">
                <xsl:copy-of select="."/>
            </xsl:for-each>
            <!-- rename local attributes -->
            <xsl:for-each select="@*">
                <xsl:element name="{concat($fqName,local-name())}">
                    <xsl:value-of select="."/>
                </xsl:element>
            </xsl:for-each>
        </xsl:variable>

        <xsl:apply-templates select="*" mode="flatten">
            <xsl:with-param name="namePrefix" select="$fqName"/>
            <xsl:with-param name="ancestorAttributes" select="$allAttributes"/>
        </xsl:apply-templates>
    </xsl:template>
    <xsl:template match="*[count(child::*)=0]" mode="flatten" priority="5">
        <xsl:param name="namePrefix"/>
        <xsl:param name="ancestorAttributes"/>

        <xsl:element name="{concat($namePrefix,local-name())}">
            <!-- dump ancestor and local attributes -->
            <xsl:for-each select="$ancestorAttributes/* | @*">
                <xsl:attribute name="{local-name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>

            <!-- use mode flatten-prop to allow for overrides -->
            <xsl:apply-templates select="." mode="flatten-prop"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="*" mode="single-level">
        <xsl:apply-templates select="*[count(child::*)=0]"/>
    </xsl:template>
<!--    <xsl:template match="@*" mode="single-level"/>-->

    <!--    &lt;!&ndash; example of overridden flattened property &ndash;&gt;-->
    <!--    <xsl:template match="propX" mode="flattened-prop">-->
    <!--        <xsl:value-of select="concat('overridden-',.)"/>-->
    <!--    </xsl:template>-->

    <!--    &lt;!&ndash; example overriding flatten element &ndash;&gt;-->
    <!--    <xsl:template match="subcov/subcov" mode="flatten">-->
    <!--        <xsl:param name="namePrefix"/>-->
    <!--        <xsl:param name="ancestorAttributes"/>-->

    <!--        <xsl:element name="{concat($namePrefix,local-name())}">-->
    <!--            &lt;!&ndash; dump ancestor and local attributes &ndash;&gt;-->
    <!--            <xsl:for-each select="$ancestorAttributes/* | @*">-->
    <!--                <xsl:attribute name="{local-name()}">-->
    <!--                    <xsl:value-of select="."/>-->
    <!--                </xsl:attribute>-->
    <!--            </xsl:for-each>-->

    <!--            <xsl:element name="test"/>-->
    <!--        </xsl:element>-->
    <!--    </xsl:template>-->

</xsl:stylesheet>