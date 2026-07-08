package org.silverpeas.maven.uicompressor;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Compresses (minifies) the JavaScript and CSS resources of the project.
 * <p>
 * The JavaScript is minified with the Google Closure Compiler and the CSS with ph-css; both support
 * the latest ECMAScript and CSS syntaxes. For each {@code foo.js} (resp. {@code foo.css}) resource a
 * minified {@code foo-min.js} (resp. {@code foo-min.css}) file is generated next to it, in the build
 * output directory.
 *
 * @author Silverpeas
 * @goal compress
 * @phase process-resources
 * @threadSafe
 */
public class UICompressorMojo extends AbstractMojo {

  private static final String[] DEFAULT_INCLUDES = {"**/*.css", "**/*.js"};

  /**
   * Encoding of the source files to read and of the minified files to generate.
   *
   * @parameter property="file.encoding" default-value="UTF-8"
   */
  private String encoding;

  /**
   * Suffix appended to the name of each generated minified file.
   *
   * @parameter property="maven.uicompressor.suffix" default-value="-min"
   */
  private String suffix;

  /**
   * When true, no suffix is appended to the name of the minified files.
   *
   * @parameter property="maven.uicompressor.nosuffix" default-value="false"
   */
  private boolean nosuffix;

  /**
   * Force the compression even when the minified file is more recent than its source.
   *
   * @parameter property="maven.uicompressor.force" default-value="false"
   */
  private boolean force;

  /**
   * Log the compression ratio of each processed file.
   *
   * @parameter property="maven.uicompressor.statistics" default-value="true"
   */
  private boolean statistics;

  /**
   * Skip the execution of the plugin.
   *
   * @parameter property="maven.uicompressor.skip" default-value="false"
   */
  private boolean skip;

  /**
   * Ant-style patterns of the files to compress. Defaults to all the {@code .css} and {@code .js}
   * files.
   *
   * @parameter
   */
  private List<String> includes;

  /**
   * Additional Ant-style patterns of the files to exclude from the compression.
   *
   * @parameter
   */
  private List<String> excludes;

  /**
   * Exclude the resources directories from the compression.
   *
   * @parameter default-value="false"
   */
  private boolean excludeResources;

  /**
   * Exclude the web application source directory from the compression.
   *
   * @parameter default-value="false"
   */
  private boolean excludeWarSourceDirectory;

  /**
   * Use the already processed resources (from the output directory) as source when available.
   *
   * @parameter default-value="false"
   */
  private boolean useProcessedResources;

  /**
   * An additional JavaScript source directory (allows a {@code src/main/js} structure).
   *
   * @parameter expression="${project.build.sourceDirectory}/../js"
   */
  private File sourceDirectory;

  /**
   * The web application source directory.
   *
   * @parameter expression="${basedir}/src/main/webapp"
   */
  private File warSourceDirectory;

  /**
   * The directory into which the web application is built.
   *
   * @parameter expression="${project.build.directory}/${project.build.finalName}"
   */
  private File webappDirectory;

  /**
   * The output directory into which the resources are copied.
   *
   * @parameter property="project.build.outputDirectory"
   */
  private File outputDirectory;

  /**
   * The resources of the project.
   *
   * @parameter property="project.resources"
   * @readonly
   */
  private List<Resource> resources;

  /**
   * @parameter property="project"
   * @readonly
   * @required
   */
  private MavenProject project;

  private final JavaScriptMinifier jsMinifier = new JavaScriptMinifier();
  private final CssMinifier cssMinifier = new CssMinifier();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("UI compression is skipped");
      return;
    }
    if (nosuffix) {
      suffix = "";
    }
    try {
      processDir(sourceDirectory, outputDirectory, null);
      if (!excludeResources && resources != null) {
        for (final Resource resource : resources) {
          final File destRoot = resource.getTargetPath() != null ?
              new File(outputDirectory, resource.getTargetPath()) :
              outputDirectory;
          processDir(new File(resource.getDirectory()), destRoot, resource.getExcludes());
        }
      }
      if (!excludeWarSourceDirectory) {
        processDir(warSourceDirectory, webappDirectory, null);
      }
    } catch (final MojoExecutionException | MojoFailureException e) {
      throw e;
    } catch (final Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void processDir(final File srcRoot, final File destRoot, final List<String> srcExcludes)
      throws Exception {
    if (srcRoot == null || !srcRoot.exists()) {
      return;
    }
    if (destRoot == null) {
      throw new MojoFailureException("The destination directory for " + srcRoot + " is null");
    }
    final DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(srcRoot);
    scanner.setIncludes(includes == null ? DEFAULT_INCLUDES : includes.toArray(new String[0]));
    final List<String> allExcludes = new ArrayList<>();
    if (srcExcludes != null) {
      allExcludes.addAll(srcExcludes);
    }
    if (excludes != null) {
      allExcludes.addAll(excludes);
    }
    if (!allExcludes.isEmpty()) {
      scanner.setExcludes(allExcludes.toArray(new String[0]));
    }
    scanner.addDefaultExcludes();
    scanner.scan();
    for (final String name : scanner.getIncludedFiles()) {
      processFile(new SourceFile(srcRoot, destRoot, name, useProcessedResources));
    }
  }

  private void processFile(final SourceFile src) throws Exception {
    final File inFile = src.toFile();
    final File outFile = src.toDestFile(suffix);
    if (isAlreadyMinified(inFile)) {
      return;
    }
    if (!force && outFile.exists() && outFile.lastModified() > inFile.lastModified()) {
      getLog().debug("Nothing to do, " + outFile + " is more recent than its source");
      return;
    }
    final Charset charset = Charset.forName(encoding);
    final String source = Files.readString(inFile.toPath(), charset);
    final String result;
    if (".js".equalsIgnoreCase(src.getExtension())) {
      result = jsMinifier.minify(source, inFile.getName());
    } else if (".css".equalsIgnoreCase(src.getExtension())) {
      result = cssMinifier.minify(source);
    } else {
      return;
    }
    final File parent = outFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new MojoExecutionException("Cannot create the output directory: " + parent);
    }
    Files.writeString(outFile.toPath(), result, charset);
    if (statistics) {
      getLog().info(String.format("%s (%db) -> %s (%db)[%d%%]", inFile.getName(), inFile.length(),
          outFile.getName(), outFile.length(), ratioOfSize(inFile, outFile)));
    }
  }

  private boolean isAlreadyMinified(final File file) {
    final String name = file.getName().toLowerCase();
    return !suffix.isEmpty() && (name.endsWith(suffix + ".js") || name.endsWith(suffix + ".css"));
  }

  private static long ratioOfSize(final File source, final File dest) {
    return (dest.length() * 100) / Math.max(source.length(), 1);
  }
}
