<!--
 # Copyright (c) 2014, The Linux Foundation. All rights reserved.
 #
 # Redistribution and use in source and binary forms, with or without
 # modification, are permitted provided that the following conditions are
 # met:
 #     * Redistributions of source code must retain the above copyright
 #      notice, this list of conditions and the following disclaimer.
 #    * Redistributions in binary form must reproduce the above
 #      copyright notice, this list of conditions and the following
 #      disclaimer in the documentation and/or other materials provided
 #      with the distribution.
 #    * Neither the name of The Linux Foundation nor the names of its
 #      contributors may be used to endorse or promote products derived
 #      from this software without specific prior written permission.
 #
 # THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 # WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 # MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 # ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 # BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 # CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 # SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 # BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 # WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 # OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 # IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="html" omit-xml-declaration="no" indent="yes"/>

<xsl:variable name="wml_card">wml_card</xsl:variable>
<xsl:variable name="wml_select">wml_select</xsl:variable>
<xsl:variable name="wml_input">wml_input</xsl:variable>
<xsl:variable name="wml_timer">wml_timer</xsl:variable>
<xsl:variable name="wml_setvar">wml_setvar</xsl:variable>
<xsl:variable name="wml_postfield">wml_postfield</xsl:variable>
<xsl:variable name="wml_anchor_task">wml_anchor_task</xsl:variable>
<xsl:variable name="wml_anchor_go">wml_anchor_go</xsl:variable>
<xsl:variable name="wml_anchor_prev">wml_anchor_prev</xsl:variable>
<xsl:variable name="select_onchange_handler">handleSelectOnchangeEvent(this)</xsl:variable>
<xsl:variable name="wml_empty_str"></xsl:variable>
<xsl:variable name="wml_noop_href">javascript:void();</xsl:variable>
<xsl:variable name="wml_template">wml_template</xsl:variable>
<xsl:variable name="wml_onevent">wml_onevent</xsl:variable>
<xsl:variable name="wml_task_go">wml_task_go</xsl:variable>
<xsl:variable name="wml_task_refresh">wml_task_refresh</xsl:variable>
<xsl:variable name="wml_task_prev">wml_task_prev</xsl:variable>
<xsl:variable name="wml_task_noop">wml_task_noop</xsl:variable>


    <xsl:template match="/">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="wml">
        <html>
            <head>
                <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"></meta>
                <link rel="stylesheet" href="file:///android_asset/wml/swe_wml.css" type="text/css"></link>
                <script src="file:///android_asset/wml/swe_wml.js" type="text/javascript"></script>
            </head>
            <body class="wml_body">
                <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
                <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
                <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
                  <xsl:apply-templates />
            </body>
        </html>
    </xsl:template>

    <xsl:template match="card">
        <div>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_card, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_card"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@title"><xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@onenterforward"><xsl:attribute name="data-wml_onenterforward"><xsl:value-of select="@onenterforward"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@onenterbackward"><xsl:attribute name="data-wml_onenterbackward"><xsl:value-of select="@onenterbackward"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@ontimer"><xsl:attribute name="data-wml_ontimer"><xsl:value-of select="@ontimer"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@newcontext"><xsl:attribute name="data-wml_newcontext"><xsl:value-of select="@newcontext"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@ordered"><xsl:attribute name="data-wml_ordered"><xsl:value-of select="@ordered"/></xsl:attribute></xsl:when></xsl:choose>
        <div class="wml_card_title"><center><xsl:value-of select="@title" /></center></div>
        <div class="wml_card_content">
            <xsl:apply-templates />
            <xsl:for-each select="/wml/template">
                    <span>
                    <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
                    <xsl:choose>
                        <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_template, ' ', @class)"/></xsl:attribute></xsl:when>
                        <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_template"/></xsl:attribute></xsl:otherwise>
                    </xsl:choose>
                    <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
                    <xsl:choose><xsl:when test="@onenterforward"><xsl:attribute name="data-wml_onenterforward"><xsl:value-of select="@onenterforward"/></xsl:attribute></xsl:when></xsl:choose>
                    <xsl:choose><xsl:when test="@onenterbackward"><xsl:attribute name="data-wml_onenterbackward"><xsl:value-of select="@onenterbackward"/></xsl:attribute></xsl:when></xsl:choose>
                    <xsl:choose><xsl:when test="@ontimer"><xsl:attribute name="data-wml_ontimer"><xsl:value-of select="@ontimer"/></xsl:attribute></xsl:when></xsl:choose>
                        <xsl:apply-templates />
                    </span>
            </xsl:for-each>
        </div>
        </div>
    </xsl:template>

    <xsl:template match="head|template|style|link|script" />

    <xsl:template match="p">
        <p>
            <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:choose><xsl:when test="@align"><xsl:attribute name="align"><xsl:value-of select="@align"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:choose><xsl:when test="@mode"><xsl:attribute name="data-wml_mode"><xsl:value-of select="@mode"/></xsl:attribute></xsl:when></xsl:choose>
                <xsl:apply-templates />
        </p>
    </xsl:template>

    <xsl:template match="br">
        <br>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </br>
    </xsl:template>

    <xsl:template match="pre">
        <pre>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </pre>
    </xsl:template>

    <xsl:template match="b">
        <b>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </b>
    </xsl:template>

    <xsl:template match="big">
        <big>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </big>
    </xsl:template>

    <xsl:template match="em">
        <em>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </em>
    </xsl:template>

    <xsl:template match="i">
        <i>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </i>
    </xsl:template>

    <xsl:template match="small">
        <small>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </small>
    </xsl:template>

    <xsl:template match="strong">
        <strong>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </strong>
    </xsl:template>

    <xsl:template match="u">
        <u>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </u>
    </xsl:template>

    <xsl:template match="a">
        <a onclick="handleAOnClick(event, this)">
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@title"><xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@href"><xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </a>
    </xsl:template>

    <xsl:template match="img">
        <img>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@alt"><xsl:attribute name="alt"><xsl:value-of select="@alt"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@src"><xsl:attribute name="src"><xsl:value-of select="@src"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@align"><xsl:attribute name="align"><xsl:value-of select="@align"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@height"><xsl:attribute name="height"><xsl:value-of select="@height"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@width"><xsl:attribute name="width"><xsl:value-of select="@width"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@localsrc"><xsl:attribute name="data-wml_localsrc"><xsl:value-of select="@localsrc"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@vspace"><xsl:attribute name="data-wml_vspace"><xsl:value-of select="@vspace"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@hspace"><xsl:attribute name="data-wml_hspace"><xsl:value-of select="@hspace"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </img>
    </xsl:template>

    <xsl:template match="table">
        <table>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@title"><xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@align"><xsl:attribute name="data-wml_align"><xsl:value-of select="@align"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@columns"><xsl:attribute name="data-wml_columns"><xsl:value-of select="@columns"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </table>
    </xsl:template>

    <xsl:template match="tr">
        <tr>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@align"><xsl:attribute name="align"><xsl:value-of select="@align"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    <xsl:template match="td">
        <td>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </td>
    </xsl:template>

    <xsl:template match="select">
        <select>
        <xsl:choose><xsl:when test="@multiple = 'true'"><xsl:attribute name="multiple"></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="onchange"><xsl:value-of select="$select_onchange_handler"/></xsl:attribute>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_select, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_select"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@title"><xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@name"><xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@iname"><xsl:attribute name="data-wml_iname"><xsl:value-of select="@iname"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@ivalue"><xsl:attribute name="data-wml_ivalue"><xsl:value-of select="@ivalue"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </select>
    </xsl:template>

    <xsl:template match="option">
        <option>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@title"><xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@value"><xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@onpick"><xsl:attribute name="data-wml_onpick"><xsl:value-of select="@onpick"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </option>
    </xsl:template>

    <xsl:template match="optgroup">
        <optgroup>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@title"><xsl:attribute name="label"><xsl:value-of select="@title"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </optgroup>
    </xsl:template>

    <xsl:template match="postfield">
        <input type="hidden">
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_postfield, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_postfield"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@name"><xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@value"><xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute></xsl:when></xsl:choose>
        </input>
    </xsl:template>

    <xsl:template match="input">
        <input>
        <xsl:choose>
            <xsl:when test="@type"><xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="type"><xsl:value-of select="'text'"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_input, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_input"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@name"><xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@value"><xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@maxlength"><xsl:attribute name="maxLength"><xsl:value-of select="@maxlength"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@format"><xsl:attribute name="data-wml_format"><xsl:value-of select="@format"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@emptyok = 'false'"><xsl:attribute name="required"></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-old_value"></xsl:attribute>
        </input>
    </xsl:template>

    <xsl:template match="timer">
        <span>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_timer, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_timer"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@name"><xsl:attribute name="data-wml_name"><xsl:value-of select="@name"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@value"><xsl:attribute name="data-wml_value"><xsl:value-of select="@value"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:apply-templates />
        </span>
    </xsl:template>

    <xsl:template match="setvar">
        <span>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_setvar, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_setvar"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@name"><xsl:attribute name="data-wml_name"><xsl:value-of select="@name"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="@value"><xsl:attribute name="data-wml_value"><xsl:value-of select="@value"/></xsl:attribute></xsl:when></xsl:choose>
        </span>
    </xsl:template>

    <xsl:template match="do">
    <div>
        <xsl:apply-templates />
    </div>
    </xsl:template>

    <xsl:template match="do/go">
    <form>
        <xsl:attribute name="action"><xsl:value-of select="@href"/></xsl:attribute>
        <xsl:choose><xsl:when test="@href"><xsl:attribute name="data-wml_href"><xsl:value-of select="@href"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
                <xsl:when test="@method"><xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute></xsl:when>
                <xsl:otherwise><xsl:attribute name="method"><xsl:value-of select="'get'"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
        <input type="button" onclick="executeGoTask(event, this.parentNode)">
            <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:choose>
                <xsl:when test="../@label"><xsl:attribute name="value"><xsl:value-of select="../@label"/></xsl:attribute></xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="../@name"><xsl:attribute name="value"><xsl:value-of select="../@name" /></xsl:attribute></xsl:when>
                        <xsl:otherwise><xsl:attribute name="value"><xsl:value-of select="'OK'"/></xsl:attribute></xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </input>
    </form>
    </xsl:template>

    <xsl:template match="do/refresh">
    <input type="button" onclick="executeRefreshTask(event, this)">
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="../@label"><xsl:attribute name="value"><xsl:value-of select="../@label"/></xsl:attribute></xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="../@name"><xsl:attribute name="value"><xsl:value-of select="../@name" /></xsl:attribute></xsl:when>
                    <xsl:otherwise><xsl:attribute name="value"><xsl:value-of select="'Refresh'"/></xsl:attribute></xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </input>
    <span>
        <xsl:apply-templates />
    </span>
    </xsl:template>

    <xsl:template match="do/prev">
    <input type="button" onclick="executePrevTask(event, this)">
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="../@label"><xsl:attribute name="value"><xsl:value-of select="../@label"/></xsl:attribute></xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="../@name"><xsl:attribute name="value"><xsl:value-of select="../@name" /></xsl:attribute></xsl:when>
                    <xsl:otherwise><xsl:attribute name="value"><xsl:value-of select="'Back'"/></xsl:attribute></xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </input>
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="do/noop">
    <span></span>
    </xsl:template>

    <xsl:template match="anchor">
    <div>
        <xsl:apply-templates />
    </div>
    </xsl:template>

    <xsl:template match="anchor/go">
    <form>
        <xsl:attribute name="action"><xsl:value-of select="@href"/></xsl:attribute>
        <xsl:choose><xsl:when test="@href"><xsl:attribute name="data-wml_href"><xsl:value-of select="@href"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
                <xsl:when test="@method"><xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute></xsl:when>
                <xsl:otherwise><xsl:attribute name="method"><xsl:value-of select="'get'"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <a onclick="executeGoTask(event, this.parentNode)">
            <xsl:choose>
                <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_anchor_task, ' ', @class)"/></xsl:attribute></xsl:when>
                <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_anchor_task"/></xsl:attribute></xsl:otherwise>
            </xsl:choose>
            <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:choose><xsl:when test="@href"><xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute></xsl:when></xsl:choose>
            <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_go"/></xsl:attribute>
                <xsl:apply-templates />
        </a>
    </form>
    </xsl:template>

    <xsl:template match="anchor/prev">
        <xsl:apply-templates />
    <a href="javascript:void(0);" onclick="executePrevTask(event, this)">
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_anchor_task, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_anchor_task"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_prev"/></xsl:attribute>
    </a>
    </xsl:template>

    <xsl:template match="anchor/refresh">
    <a href="javascript:void(0);" onclick="executeRefreshTask(event, this)">
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_anchor_task, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="$wml_anchor_task"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_refresh"/></xsl:attribute>
            <xsl:apply-templates />
    </a>
    </xsl:template>

    <xsl:template match="onevent">
    <div>
        <xsl:attribute name="class"><xsl:value-of select="$wml_onevent"/></xsl:attribute>
        <xsl:apply-templates />
    </div>
    </xsl:template>

    <xsl:template match="onevent/go">
    <form>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type)"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="../@type"><xsl:attribute name="data-wml_onevent_type"><xsl:value-of select="../@type"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_go"/></xsl:attribute>
        <xsl:attribute name="action"><xsl:value-of select="@href"/></xsl:attribute>
        <xsl:choose><xsl:when test="@href"><xsl:attribute name="data-wml_href"><xsl:value-of select="@href"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
                <xsl:when test="@method"><xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute></xsl:when>
                <xsl:otherwise><xsl:attribute name="method"><xsl:value-of select="'get'"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
    </form>
    </xsl:template>

    <xsl:template match="onevent/refresh">
    <span>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type)"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="../@type"><xsl:attribute name="data-wml_onevent_type"><xsl:value-of select="../@type"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_refresh"/></xsl:attribute>
            <xsl:apply-templates />
    </span>
    </xsl:template>

    <xsl:template match="onevent/prev">
    <span>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type)"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="../@type"><xsl:attribute name="data-wml_onevent_type"><xsl:value-of select="../@type"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_prev"/></xsl:attribute>
            <xsl:apply-templates />
    </span>
    </xsl:template>

    <xsl:template match="onevent/noop">
    <span>
        <xsl:choose><xsl:when test="@id"><xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose>
            <xsl:when test="@class"><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type, ' ', @class)"/></xsl:attribute></xsl:when>
            <xsl:otherwise><xsl:attribute name="class"><xsl:value-of select="concat($wml_onevent, '_', ../@type)"/></xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:choose><xsl:when test="@lang"><xsl:attribute name="lang"><xsl:value-of select="@lang"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="../@type"><xsl:attribute name="data-wml_onevent_type"><xsl:value-of select="../@type"/></xsl:attribute></xsl:when></xsl:choose>
        <xsl:attribute name="data-wml_task_type"><xsl:value-of select="$wml_task_noop"/></xsl:attribute>
            <xsl:apply-templates />
    </span>
    </xsl:template>

</xsl:stylesheet>
