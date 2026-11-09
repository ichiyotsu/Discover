package dev.ichiyotsu.tokidiscover.browser

object WebScripts {
    const val EXTRACT = """
        (function(){
          if (!document.body) return "[]";
          var skip = {SCRIPT:1, STYLE:1, NOSCRIPT:1, TEXTAREA:1, CODE:1, SVG:1};
          var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
          var out = [];
          var n;
          while ((n = walker.nextNode()) && out.length < 36) {
            var p = n.parentElement;
            if (!p || skip[p.tagName]) continue;
            var t = (n.nodeValue || "").replace(/\s+/g, " ").trim();
            if (t.length < 2 || t.length > 360) continue;
            out.push(t);
          }
          return JSON.stringify(out);
        })();
    """

    fun apply(jsonArray: String): String {
        val payload = jsonArray.replace("</", "<\\/")
        return """
            (function(){
              var parsed = $payload;
              if (!document.body || !parsed || !parsed.length) return "0";
              var skip = {SCRIPT:1, STYLE:1, NOSCRIPT:1, TEXTAREA:1, CODE:1, SVG:1};
              var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
              var i = 0, n, applied = 0;
              while ((n = walker.nextNode()) && i < parsed.length) {
                var p = n.parentElement;
                if (!p || skip[p.tagName]) continue;
                var raw = n.nodeValue || "";
                var t = raw.replace(/\s+/g, " ").trim();
                if (t.length < 2 || t.length > 360) continue;
                if (parsed[i] && parsed[i] !== t) {
                  n.nodeValue = raw.replace(t, parsed[i]);
                  applied++;
                }
                i++;
              }
              return String(applied);
            })();
        """.trimIndent()
    }

    const val PAGE_LANG = """
        (function(){
          var l = (document.documentElement && document.documentElement.lang) || "";
          return (l || "").trim();
        })();
    """
}
