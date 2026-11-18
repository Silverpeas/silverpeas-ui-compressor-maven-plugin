package org.silverpeas.maven.uicompressor;

import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;

import java.util.function.UnaryOperator;

/**
 * Minifies CSS source code by using ph-css, a real CSS parser supporting the latest CSS syntaxes
 * (CSS nesting, custom properties, calc()/var(), &#64;layer, :has(), &#64;supports, ...) that the
 * legacy regex-based YUI CssCompressor couldn't handle.
 * <p>
 * The parser is run in browser-compliant mode so that legacy hacks and edge cases are recovered
 * like a browser does instead of failing.
 */
public class CssMinifier {

  /**
   * Minifies the specified CSS source. If the source cannot be parsed at all, or if the minified
   * output wouldn't be smaller, the source is returned unchanged so that a single faulty (or already
   * minified) file never breaks nor bloats the build.
   * @param source the CSS source code.
   * @return the minified CSS, or the source itself as a fallback.
   */
  public String minify(final String source) {
    return minify(source, UnaryOperator.identity());
  }

  /**
   * Minifies the specified CSS source and relocates its {@code @import} rules with the given
   * locator, so that they can target the minified version of the imported stylesheets. If the
   * source cannot be parsed at all, it is returned unchanged. If the minified output wouldn't be
   * smaller and no import has been relocated, the source is also returned unchanged so that a
   * single faulty (or already minified) file never breaks nor bloats the build.
   * @param source the CSS source code.
   * @param importLocator a function computing the new location of an imported stylesheet from its
   * location in the source.
   * @return the minified CSS, or the source itself as a fallback.
   */
  public String minify(final String source, final UnaryOperator<String> importLocator) {
    CascadingStyleSheet css;
    try {
      css = CSSReader.readFromStringReader(source,
          new CSSReaderSettings().setBrowserCompliantMode(true));
    } catch (final RuntimeException e) {
      css = null;
    }
    if (css == null) {
      return source;
    }
    final boolean relocated = relocateImports(css, importLocator);
    final CSSWriterSettings settings = new CSSWriterSettings(true).setRemoveUnnecessaryCode(true);
    final String minified = new CSSWriter(settings).setWriteHeaderText(false).getCSSAsString(css);
    return relocated || minified.length() < source.length() ? minified : source;
  }

  private static boolean relocateImports(final CascadingStyleSheet css,
      final UnaryOperator<String> importLocator) {
    boolean relocated = false;
    for (final CSSImportRule rule : css.getAllImportRules()) {
      final String location = rule.getLocationString();
      final String newLocation = importLocator.apply(location);
      if (!location.equals(newLocation)) {
        rule.setLocationString(newLocation);
        relocated = true;
      }
    }
    return relocated;
  }
}
