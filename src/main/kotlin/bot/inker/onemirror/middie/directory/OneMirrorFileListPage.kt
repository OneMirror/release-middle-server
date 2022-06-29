package bot.inker.onemirror.middie.directory

import io.undertow.util.FlexBase64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

// TODO: remove it, provide it in web-ui
class OneMirrorFileListPage(
    private val title:String = "OneMirror",
    private val colspans:Int = 5,
    private val copyright:String = "OneMirror (onemirror.net, im@inker.bot)",
    private val coloms:List<String> = listOf("Name", "Last modified", "Size", "ETag", "status")
) {
    private val COLOMS by lazy { StringBuilder().apply {
        append("""<th class="label offset">""")
        append(coloms[0])
        append("""</th>""")
        coloms.subList(1, coloms.size).forEach {
            append("""<th class="label">""")
            append(it)
            append("""</th>""")
        }
    }.toString() }
    private val HEAD_1 by lazy { """
<html>
    <head>
        <script src="?js"></script>
        <link rel="stylesheet" type="text/css" href="?css"/>
    </head>
    <body onresize="growit()" onload="growit()">
        <table id="thetable">
            <thead>
                <tr>
                    <th class="loc" colspan="$colspans">$title - """}
    private val HEAD_2 by lazy { """</th>
                </tr>
                <tr>
                    $COLOMS
                </tr>
            </thead>
            <tfoot>
                <tr>
                    <th class="loc footer" colspan="$colspans">
                        <a class="api" href="?json">api</a>
                        <span class="copyright">$copyright</span>
                    </th>
                </tr>
            </tfoot>
            <tbody>
""" }

    private val FOOTER by lazy{ """
            </tbody>
        </table>
    </body>
</html>
""" }

    private val JS by lazy { """
function growit() {
    var table = document.getElementById("thetable");

    var i = table.rows.length - 1;
    while (i-- > 0) {
        if (table.rows[i].id == "eraseme") {
            table.deleteRow(i);
        } else {
            break;
        }
    }
    table.style.height = "";
    var i = 0;
    while (table.offsetHeight < window.innerHeight - 24) {
        i++;
        var tbody = table.tBodies[0];
        var row = tbody.insertRow(tbody.rows.length);
        row.id = "eraseme";
        var cell = row.insertCell(0);
        if (table.rows.length % 2 != 0) {
            row.className = "even eveninvis";
        } else {
            row.className = "odd oddinvis";
        }

        cell.colSpan = $colspans;
        cell.appendChild(document.createTextNode("i"));
    }
    table.style.height = "100%";
    if (i > 0) {
        document.documentElement.style.overflowY = "hidden";
    } else {
        document.documentElement.style.overflowY = "auto";
    }
}
""" }

    private val CSS by lazy{"""
body {
    font-family: "Lucida Grande", "Lucida Sans Unicode", "Trebuchet MS", Helvetica, Arial, Verdana, sans-serif;
    margin: 5px;
}

th.loc {
    background-image: linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);
    background-image: -o-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);
    background-image: -moz-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);
    background-image: -webkit-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);
    background-image: -ms-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);
    color: black;
    padding: 2px;
    font-weight: normal;
    border: solid 1px;
    font-size: 150%;
    text-align: left;
}

th.label {
    border: solid 1px;
    text-align: left;
    padding: 4px;
    padding-left: 8px;
    font-weight: normal;
    font-size: small;
    background-color: #e8e8e8;
}

th.offset {
    padding-left: 32px;
}

th.footer {
    font-size: 75%;
}

a.icon {
    padding-left: 24px;
    text-decoration: none;
    color: black;
}

a.icon:hover {
    text-decoration: underline;
}

.api {
    padding-left: 12px;
    text-decoration: none;
    color: black;
    float: left;
}

.copyright {
    padding-right: 12px;
    text-decoration: none;
    color: black;
    float: right;
}

table {
    border: 1px solid;
    border-spacing: 0px;
    width: 100%;
    border-collapse: collapse;
}

tr.odd {
    background-color: #f3f6fa;
}

tr.odd td {
    padding: 2px;
    padding-left: 8px;
    font-size: smaller;
}

tr.even {
    background-color: #ffffff;
}

tr.even td {
    padding: 2px;
    padding-left: 8px;
    font-size: smaller;
}

tr.eveninvis td {
    color: #ffffff;
}

tr.oddinvis td {
    color: #f3f6fa
}

a.up {
    background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABI0lEQVQ4y2P4//8/Ay7sM4nhPwjjUwMm0ua//Y+M0+e//QrSGDAfgvEZAjdgydHXcAzTXLjWDoxhhqBbhGLA1N0vwBhdM7ohMHVwA8yrzn4zLj/936j8FE7N6IaA1IL0gPQy2DVc+rnp3FeCmtENAekB6WXw7Lz1tWD5x/+wEIdhdI3o8iA9IL0MYZMfvq9a9+V/w+avcIzLAGQ1ID0gvQxJc56/aNn29X/vnm9wjMsAZDWtQD0gvQwFy94+6N37/f/Moz/gGJcByGpAekB6GarXf7427ciP/0vP/YRjdP/CMLIakB6QXobKDd9PN+769b91P2kYpAekl2HJhb8r11/583/9ZRIxUM+8U783MQCBGBDXAHEbibgGrBdfTiMGU2wAAPz+nxp+TnhDAAAAAElFTkSuQmCC') left center no-repeat;
    background-size: 16px 16px;
}

a.dir {
    background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXZwQWcAAAAQAAAAEABcxq3DAAAA+UlEQVQ4jWP4//8/AyUYTKTNf/sfGafPf/s1be47G5IMWHL0NRxP2f3mbcaCtz/RDUbHKAZM3f2CJAw3wLzq7Dfj8tP/jcpPkYRBekB6GewaLv3cdO7r/y0XSMMgPSC9DJ6dt74WLP/4v3TVZ5IwSA9IL0PY5Ifvq9Z9+d+w+StJGKQHpJchac7zFy3bvv7v3fONJNwK1APSy5C/7O2D3r3f/888+oMkDNID0stQvf7ztWlHfvxfeu4nSRikB6SXoXLD99ONu379b91PGgbpAellWHLh38r1V/78X3+ZRAzUM/fUr00MQCAGxDVA3EYirgHrpUpupAQDAPs+7c1tGDnPAAAAAElFTkSuQmCC') left center no-repeat;
    background-size: 16px 16px;
}

a.file {
    background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXZwQWcAAAAQAAAAEABcxq3DAAABM0lEQVQ4y5WSTW6DMBCF3xvzc4wuOEIO0kVAuUB7vJ4g3KBdoHSRROomEpusUaoAcaYLfmKoqVRLIxnJ7/M3YwJVBcknACv8b+1U9SvoP1bXa/3WNDVIAQmQBLsNOEsGQYAwDNcARgDqusbl+wIRA2NkBEyqP0s+kCOAQhhjICJdkaDIJDwEvQAhH+G+SHagWTsi4jHoAWYIOxYDZDjnb8Fn4Akvz6AHcAbx3Tp5ETwI3RwckyVtv4Fr4VEe9qq6bDB5tlnYWou2bWGtRRRF6jdwAm5Za1FVFc7nM0QERVG8A9hPDRaGpapomgZlWSJJEuR5ftpsNq8ADr9amC+SuN/vuN1uIIntdnvKsuwZwKf2wxgBxpjpX+dA4jjW4/H4kabpixt2AbvAmDX+XnsAB509ww+A8mAar+XXgQAAAABJRU5ErkJggg==') left center no-repeat;
}
"""}

    val JS_BUFFER = ByteBuffer.wrap(JS.toByteArray(StandardCharsets.UTF_8)).apply(ByteBuffer::rewind)

    val CSS_BUFFER = ByteBuffer.wrap(CSS.toByteArray(StandardCharsets.UTF_8)).apply(ByteBuffer::rewind)

    val FILE_JS_ETAG = md5(JS_BUFFER)
    val FILE_JS_ETAG_QUOTED = "\"$FILE_JS_ETAG\""

    val FILE_CSS_ETAG = md5(CSS_BUFFER)
    val FILE_CSS_ETAG_QUOTED = "\"$FILE_CSS_ETAG\""

    fun build(path:String,entries:List<List<String>>):String{
        val builder = StringBuilder()
        builder.append(HEAD_1)
        builder.append(path)
        builder.append(HEAD_2)
        var i = 0
        entries.forEach { entry->
            builder.append("""<tr class="""")
            builder.append(if((i++).mod(2) == 0){
                "odd"
            }else{
                "even"
            })
            builder.append("""">""")
            entry.forEach { builder.append("""<td>""").append(it).append("""</td>""") }
            builder.append("""</tr>""")
        }
        builder.append(FOOTER)
        return builder.toString()
    }

    private fun md5(buffer: ByteBuffer): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(buffer.duplicate())
            val digest = md.digest()
            String(FlexBase64.encodeBytes(digest, 0, digest.size, false), StandardCharsets.US_ASCII)
        } catch (e: NoSuchAlgorithmException) {
            // Should never happen
            throw InternalError("MD5 not supported on this platform")
        }
    }
}