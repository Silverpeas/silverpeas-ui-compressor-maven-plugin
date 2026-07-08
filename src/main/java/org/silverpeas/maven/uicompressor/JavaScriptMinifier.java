package org.silverpeas.maven.uicompressor;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.WarningLevel;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Level;

/**
 * Minifies JavaScript source code by using the Google Closure Compiler. Unlike the legacy YUI
 * Compressor (Rhino based, ES5 only), it supports the modern ECMAScript syntaxes (ES6+: template
 * literals, const/let, arrow functions, ...).
 * <p>
 * The compilation is performed at the {@code WHITESPACE_ONLY} level: only the whitespaces and the
 * comments are stripped (no symbol renaming nor dead-code removal) so that the behaviour of the
 * script is strictly preserved.
 */
public class JavaScriptMinifier {

  /**
   * Minifies the specified JavaScript source. If the source cannot be parsed/compiled, or if the
   * minified output wouldn't be smaller, the source is returned unchanged so that a single faulty
   * (or already minified) file never breaks nor bloats the build.
   * @param source the JavaScript source code.
   * @param name the name of the source (only used by the compiler to report diagnostics).
   * @return the minified JavaScript, or the source itself as a fallback.
   */
  public String minify(final String source, final String name) {
    Compiler.setLoggingLevel(Level.OFF);
    final Compiler compiler =
        new Compiler(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
    final CompilerOptions options = new CompilerOptions();
    CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options);
    WarningLevel.QUIET.setOptionsForWarningLevel(options);
    options.setLanguageIn(LanguageMode.ECMASCRIPT_NEXT);
    options.setLanguageOut(LanguageMode.NO_TRANSPILE);
    options.setEmitUseStrict(false);
    options.setPrettyPrint(false);
    final com.google.javascript.jscomp.SourceFile input =
        com.google.javascript.jscomp.SourceFile.fromCode(name, source);
    final com.google.javascript.jscomp.SourceFile externs =
        com.google.javascript.jscomp.SourceFile.fromCode("externs.js", "");
    final Result result = compiler.compile(Collections.singletonList(externs),
        Collections.singletonList(input), options);
    if (result.success) {
      final String minified = compiler.toSource();
      return minified.length() < source.length() ? minified : source;
    }
    return source;
  }
}
