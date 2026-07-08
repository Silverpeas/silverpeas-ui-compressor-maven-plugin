# Silverpeas UI Compressor Maven Plugin

A Maven plugin to minify the JavaScript and CSS resources of Silverpeas.

- **JavaScript** is minified with the [Google Closure Compiler](https://github.com/google/closure-compiler)
  (at the `WHITESPACE_ONLY` level), which supports the modern ECMAScript syntaxes (ES6+: template
  literals, `const`/`let`, arrow functions, ...).
- **CSS** is minified with [ph-css](https://github.com/phax/ph-css), a real CSS parser which supports
  the latest CSS syntaxes (CSS nesting, custom properties, `calc()`/`var()`, `@layer`, `:has()`,
  `@supports`, ...).

It originates from a fork of David Bernard's
[yuicompressor-maven-plugin](https://github.com/davidB/yuicompressor-maven-plugin) but no longer uses
the YUI Compressor (Rhino based, ES5 / old CSS only), which couldn't handle the modern syntaxes.

## Goal

`compress` (bound by default to the `process-resources` phase): for every `foo.js` (resp.
`foo.css`) resource, a minified `foo-min.js` (resp. `foo-min.css`) file is generated next to it in
the build output directory. The `.js` and `.css` files of the resources directories and of the web
application source directory (`src/main/webapp`) are processed.

If a file cannot be parsed, or if its minified output wouldn't be smaller, the original content is
kept so that the build is never broken nor bloated by a single faulty (or already minified) file.

## Usage

```xml
<plugin>
  <groupId>org.silverpeas.maven</groupId>
  <artifactId>silverpeas-ui-compressor-maven-plugin</artifactId>
  <version>2.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>compress</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <excludes>
      <exclude>**/*.min.*</exclude>
      <exclude>**/*-min.*</exclude>
    </excludes>
  </configuration>
</plugin>
```

The compression can be skipped by setting the `maven.uicompressor.skip` property to `true`.

## License

This project is available under the terms described in [LICENSE.txt](LICENSE.txt).
