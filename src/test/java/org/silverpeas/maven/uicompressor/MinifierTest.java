package org.silverpeas.maven.uicompressor;

import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests validating the minification of both JavaScript (Google Closure Compiler) and CSS
 * (ph-css), including the support of modern ECMAScript and CSS syntaxes.
 */
class MinifierTest {

  private final JavaScriptMinifier js = new JavaScriptMinifier();
  private final CssMinifier css = new CssMinifier();

  @Test
  void javaScriptIsMinified() {
    final String source =
        "function add(first, second) {\n    const total = first + second;\n    return total;\n}\n";
    final String minified = js.minify(source, "add.js");
    assertTrue(minified.length() < source.length(),
        "the minified script should be smaller than the source");
    assertFalse(minified.contains("\n\n"), "the minified script should not hold blank lines");
    assertTrue(minified.contains("function add"),
        "the behaviour of the script must be preserved");
  }

  @Test
  void modernJavaScriptIsSupported() {
    final String source = "const greet = (name) => `Hello, ${name}!`;";
    final String minified = js.minify(source, "greet.js");
    assertTrue(minified.contains("`Hello, ${name}!`"), "ES6 template literals must be preserved");
  }

  @Test
  void invalidJavaScriptIsKeptAsIs() {
    final String source = "function ( { this is not valid javascript";
    assertEquals(source, js.minify(source, "ko.js"), "an unparseable script must be kept as-is");
  }

  @Test
  void cssIsMinified() {
    final String source = ".box {\n    color: red;\n    margin: 0;\n}\n";
    assertEquals(".box{color:red;margin:0}", css.minify(source));
  }

  @Test
  void modernCssNestingIsSupported() {
    final String source = ".menu {\n  color: red;\n  & .item { color: blue; }\n}\n";
    final String minified = css.minify(source);
    assertTrue(minified.contains("& .item"), "CSS nesting must be preserved");
    assertTrue(minified.contains("color:blue"), "the nested rule must be preserved");
  }

  @Test
  void modernCssCustomPropertiesAndCalcAreSupported() {
    final String source = ":root { --gap: 10px; }\n.grid { width: calc(100% - var(--gap)); }";
    final String minified = css.minify(source);
    assertTrue(minified.contains("--gap:10px"), "CSS custom properties must be preserved");
    assertTrue(minified.contains("calc(100% - var(--gap))"), "calc() with var() must be preserved");
  }

  @Test
  void cssImportsAreRelocatedToTheMinifiedStylesheets() {
    final String source = "@import url(yui-menu.css);\n@import \"print.css\" print;\n" +
        "@import url(http://cdn.org/lib.css);\n.box {\n    color: red;\n}\n";
    final UnaryOperator<String> locator =
        location -> location.startsWith("http") ? location : location.replace(".css", "-min.css");
    final String minified = css.minify(source, locator);
    assertTrue(minified.contains("@import url(yui-menu-min.css);"),
        "the import must target the minified stylesheet: " + minified);
    assertTrue(minified.contains("@import url(print-min.css) print;"),
        "the import with a media query must target the minified stylesheet: " + minified);
    assertTrue(minified.contains("@import url(http://cdn.org/lib.css);"),
        "the import left unchanged by the locator must be kept: " + minified);
    assertTrue(minified.endsWith(".box{color:red}"), "the rules must be minified: " + minified);
  }

  @Test
  void cssImportsAreRelocatedEvenInAnAlreadyCompactStylesheet() {
    final String source = "@import url(a.css);";
    assertEquals("@import url(a-min.css);",
        css.minify(source, location -> location.replace(".css", "-min.css")));
  }

  @Test
  void compactStylesheetWithoutRelocatedImportIsKeptAsIs() {
    final String source = "@import url(a.css);.b{color:red}";
    assertEquals(source, css.minify(source, UnaryOperator.identity()));
  }
}
