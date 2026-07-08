package org.silverpeas.maven.uicompressor;

import java.io.File;

/**
 * Resolves the source file to compress and the destination file (the minified one) of a resource,
 * relative to a source root and a destination root directory.
 */
public class SourceFile {

  private final File srcRoot;
  private final File destRoot;
  private final boolean destAsSource;
  private final String rpath;
  private final String extension;

  public SourceFile(final File srcRoot, final File destRoot, final String name,
      final boolean destAsSource) {
    this.srcRoot = srcRoot;
    this.destRoot = destRoot;
    this.destAsSource = destAsSource;
    final int sep = name.lastIndexOf('.');
    if (sep > 0) {
      this.extension = name.substring(sep);
      this.rpath = name.substring(0, sep);
    } else {
      this.extension = "";
      this.rpath = name;
    }
  }

  /**
   * @return the source file to compress. When the destination is used as source and an already
   * processed copy exists in the destination root, that copy is returned; otherwise the file from
   * the source root is returned.
   */
  public File toFile() {
    final String path = rpath + extension;
    if (destAsSource) {
      final File processed = new File(destRoot, path);
      if (processed.exists() && processed.canRead()) {
        return processed;
      }
    }
    return new File(srcRoot, path);
  }

  /**
   * @param suffix the suffix to append to the name of the minified file (e.g. {@code -min}).
   * @return the destination (minified) file.
   */
  public File toDestFile(final String suffix) {
    return new File(destRoot, rpath + suffix + extension);
  }

  /**
   * @return the extension of the file, including the leading dot (e.g. {@code .css}), or an empty
   * string if it has none.
   */
  public String getExtension() {
    return extension;
  }
}
