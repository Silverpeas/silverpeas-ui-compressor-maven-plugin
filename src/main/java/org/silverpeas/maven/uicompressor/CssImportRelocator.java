package org.silverpeas.maven.uicompressor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Relocates the {@code @import} of a stylesheet towards the minified version of the imported
 * stylesheet, so that a minified stylesheet imports minified stylesheets only.
 * <p>
 * Only the imports targeting, by a relative location, a stylesheet minified by the plugin within the
 * same source directory are relocated: absolute locations ({@code /foo.css},
 * {@code https://cdn/foo.css}, ...) and stylesheets not minified by the plugin (excluded, already
 * minified, outside the source directory, ...) are kept as they are because no minified version of
 * them is generated.
 */
public class CssImportRelocator {

  private static final String CSS_EXTENSION = ".css";
  private static final Pattern ABSOLUTE_LOCATION = Pattern.compile("^(/|[a-zA-Z][a-zA-Z0-9+.-]*:)");

  private final Set<Path> minifiedStylesheets;
  private final String suffix;

  /**
   * @param minifiedNames the paths, relative to the source directory, of the resources minified
   * by the plugin in that directory. Only the stylesheets among them are taken into account.
   * @param suffix the suffix appended to the name of the minified files (e.g. {@code -min}).
   */
  public CssImportRelocator(final Collection<String> minifiedNames, final String suffix) {
    this.minifiedStylesheets = minifiedNames.stream()
        .filter(name -> name.toLowerCase().endsWith(CSS_EXTENSION))
        .map(CssImportRelocator::toPath)
        .collect(Collectors.toSet());
    this.suffix = suffix;
  }

  /**
   * Relocates the specified import location towards the minified version of the imported
   * stylesheet when the plugin generates one.
   * @param importingName the path, relative to the source directory, of the stylesheet declaring
   * the import.
   * @param location the location of the imported stylesheet as written in the {@code @import} rule.
   * @return the location of the minified imported stylesheet, or the location itself if the
   * imported stylesheet isn't minified by the plugin.
   */
  public String relocate(final String importingName, final String location) {
    if (suffix.isEmpty() || ABSOLUTE_LOCATION.matcher(location).find()) {
      return location;
    }
    final int end = indexOfQueryOrFragment(location);
    final String path = location.substring(0, end);
    if (!path.toLowerCase().endsWith(CSS_EXTENSION)) {
      return location;
    }
    final Path imported = toPath(importingName).resolveSibling(path).normalize();
    if (!minifiedStylesheets.contains(imported)) {
      return location;
    }
    final int extension = path.length() - CSS_EXTENSION.length();
    return path.substring(0, extension) + suffix + location.substring(extension);
  }

  private static int indexOfQueryOrFragment(final String location) {
    for (int i = 0; i < location.length(); i++) {
      final char c = location.charAt(i);
      if (c == '?' || c == '#') {
        return i;
      }
    }
    return location.length();
  }

  private static Path toPath(final String name) {
    return Path.of(name.replace('\\', '/')).normalize();
  }
}
