package com.android.andex.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WebResearchToolTest {
    @Test fun parsesBingAndDeduplicatesLinks() {
        val html = """<ol><li class="b_algo"><h2><a href="https://github.com/termux">Termux</a></h2><p>Android terminal</p></li>
            <li class="b_algo"><h2><a href="https://github.com/termux">Duplicate</a></h2></li></ol>"""
        val result = WebResearchTool.parseSearchResults("bing", html, "https://www.bing.com/search", 8)
        assertEquals(1, result.length())
        assertEquals("https://github.com/termux", result.getJSONObject(0).getString("url"))
    }
    @Test fun parsesBaiduResults() {
        val result = WebResearchTool.parseSearchResults("baidu",
            """<div class="result"><h3><a href="https://example.com/doc">Document</a></h3><p>Text</p></div>""",
            "https://www.baidu.com/s", 8)
        assertEquals(1, result.length())
    }
    @Test fun unwrapsGoogleDestination() {
        val result = WebResearchTool.parseSearchResults("google",
            """<div class="g"><a href="/url?q=https%3A%2F%2Fexample.com%2Fdoc"><h3>Document</h3></a></div>""",
            "https://www.google.com/search", 8)
        assertEquals("https://example.com/doc", result.getJSONObject(0).getString("url"))
    }
    @Test fun verificationPageHasNoInventedResults() {
        val result = WebResearchTool.parseSearchResults("bing", "<h1>Verify you are human</h1>", "https://www.bing.com", 8)
        assertEquals(0, result.length())
    }
}
