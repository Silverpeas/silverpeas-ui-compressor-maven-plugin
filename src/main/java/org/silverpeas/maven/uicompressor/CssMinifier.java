package org.silverpeas.maven.uicompressor;

import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;

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
    final CSSWriterSettings settings = new CSSWriterSettings(true).setRemoveUnnecessaryCode(true);
    final String minified = new CSSWriter(settings).setWriteHeaderText(false).getCSSAsString(css);
    return minified.length() < source.length() ? minified : source;
  }
}
