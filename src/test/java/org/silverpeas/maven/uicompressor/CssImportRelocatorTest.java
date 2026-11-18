package org.silverpeas.maven.uicompressor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests on the relocation of the CSS imports towards the minified stylesheets.
 */
class CssImportRelocatorTest {

  private final CssImportRelocator relocator = new CssImportRelocator(
      List.of("yui/menu.css", "yui/yui-menu.css", "yui/fonts/fonts.css", "common.css",
          "script.js"), "-min");

  @Test
  void importOfASiblingMinifiedStylesheetIsRelocated() {
    assertEquals("yui-menu-min.css", relocator.relocate("yui/menu.css", "yui-menu.css"));
    assertEquals("./yui-menu-min.css", relocator.relocate("yui/menu.css", "./yui-menu.css"));
  }

  @Test
  void importOfAStylesheetInAnotherDirectoryIsRelocated() {
    assertEquals("fonts/fonts-min.css", relocator.relocate("yui/menu.css", "fonts/fonts.css"));
    assertEquals("../common-min.css", relocator.relocate("yui/menu.css", "../common.css"));
    assertEquals("yui/menu-min.css", relocator.relocate("common.css", "yui/menu.css"));
  }

  @Test
  void queryAndFragmentOfTheImportArePreserved() {
    assertEquals("yui-menu-min.css?v=2", relocator.relocate("yui/menu.css", "yui-menu.css?v=2"));
    assertEquals("yui-menu-min.css#top", relocator.relocate("yui/menu.css", "yui-menu.css#top"));
  }

  @Test
  void importOfAStylesheetNotMinifiedByThePluginIsKept() {
    assertEquals("unknown.css", relocator.relocate("yui/menu.css", "unknown.css"));
    assertEquals("../vendor/lib.min.css",
        relocator.relocate("yui/menu.css", "../vendor/lib.min.css"));
    assertEquals("../../common.css", relocator.relocate("yui/menu.css", "../../common.css"));
    assertEquals("../script.js", relocator.relocate("yui/menu.css", "../script.js"));
  }

  @Test
  void absoluteImportIsKept() {
    assertEquals("/yui/yui-menu.css", relocator.relocate("yui/menu.css", "/yui/yui-menu.css"));
    assertEquals("https://cdn.org/yui/yui-menu.css",
        relocator.relocate("yui/menu.css", "https://cdn.org/yui/yui-menu.css"));
    assertEquals("//cdn.org/common.css", relocator.relocate("yui/menu.css", "//cdn.org/common.css"));
  }

  @Test
  void importIsKeptWhenThereIsNoSuffix() {
    final CssImportRelocator noSuffix = new CssImportRelocator(List.of("a.css", "b.css"), "");
    assertEquals("b.css", noSuffix.relocate("a.css", "b.css"));
  }

  @Test
  void windowsSeparatorsInTheNamesAreSupported() {
    final CssImportRelocator relocatorOnWindows =
        new CssImportRelocator(List.of("yui\\menu.css", "yui\\fonts\\fonts.css"), "-min");
    assertEquals("fonts/fonts-min.css",
        relocatorOnWindows.relocate("yui\\menu.css", "fonts/fonts.css"));
  }
}
