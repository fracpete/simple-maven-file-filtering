/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * FilterUtilsTest.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.simplemavenfilefiltering;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.maven.model.Model;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For testing the FilterUtils class.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class FilterUtilsTest
  extends TestCase {

  /** the prefix for reference files. */
  public final static String REF_PREFIX = "src/test/resources/com/github/fracpete/simplemavenfilefiltering";

  /** the dummy Model instance. */
  protected Model m_Model;

  /** the log instance to use. */
  protected Log m_Log;

  /** the dummy script. */
  protected Path m_PathDummyScript;

  /** the dummy text. */
  protected Path m_PathDummyText;

  /**
   * Writes the content to disk. Overwrites any existing file.
   *
   * @param content	the content to write
   * @param output	the file to write to
   * @return 		true if successfully written
   */
  protected boolean write(List<String> content, Path output) {
    try {
      Files.write(output, content, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      return true;
    }
    catch (Exception e) {
      System.err.println("Failed to write content to " + output);
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Sets up the fixture, for example, open a network connection.
   * This method is called before a test is executed.
   */
  @Override
  protected void setUp() throws Exception {
    List<String> 	content;

    super.setUp();

    m_Model = new Model();
    m_Model.setGroupId("com.github.fracpete");
    m_Model.setArtifactId("MyDummyProject");
    m_Model.setName("My Dummy Project");
    m_Model.setVersion("1.0.0");
    m_Model.setDescription("This is just a dummy Apache model.");

    m_Log = new DefaultLog(new ConsoleLogger());

    content = new ArrayList<>();
    content.add("#!/bin/bash");
    content.add("");
    content.add("# ${comment}");
    content.add("");
    content.add("echo \"${project.name} ${project.version}\"");
    content.add("");
    m_PathDummyScript = new File(System.getProperty("java.io.tmpdir") + "/dummyscript.sh").toPath();
    write(content, m_PathDummyScript);

    content = new ArrayList<>();
    content.add("@<project.name>@");
    content.add("@<var1>@: The quick brown fox jumps over the lazy dog");
    content.add("@<var2>@: Jived fox nymph grabs quick waltz");
    content.add("@<var3>@: Glib jocks quiz nymph to vex dwarf");
    m_PathDummyText = new File(System.getProperty("java.io.tmpdir") + "/dummytext.txt").toPath();
    write(content, m_PathDummyText);
  }

  /**
   * Tears down the fixture, for example, close a network connection.
   * This method is called after a test is executed.
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    m_PathDummyScript.toFile().delete();
    m_PathDummyText.toFile().delete();
  }

  /**
   * Compares the reference file with the current file.
   * Automatically uses the current file if not reference file available
   * (eg when running for first time).
   *
   * @param refPath	the reference file to compare against
   * @param curPath 	the current file to check
   * @return		the difference (or an error message), null if no difference
   */
  protected String compare(Path refPath, Path curPath) throws Exception {
    List<String>	reference;
    List<String> 	current;
    File		refFile;
    StringBuilder	result;

    current = Files.readAllLines(curPath);

    // reference available?
    refFile = refPath.toFile();
    if (!refFile.exists()) {
      if (!refFile.getParentFile().exists()) {
	if (!refFile.getParentFile().mkdirs())
	  return "Failed to create reference file: " + refFile;
      }

      if (write(current, refPath)) {
	System.err.println("Reference file created: " + refFile);
	return null;
      }
      else {
	return "Failed to create reference file: " + refFile;
      }
    }

    // compare
    reference = Files.readAllLines(refPath);
    if (!reference.equals(current)) {
      result = new StringBuilder();
      result.append(">>> Reference\n");
      for (String line: reference)
        result.append(line).append("\n");
      result.append("<<< Reference\n");
      result.append(">>> Current\n");
      for (String line: current)
        result.append(line).append("\n");
      result.append("<<< Current\n");
      return result.toString();
    }

    return null;
  }

  /**
   * Tests the file filtering with default var prefix/suffix and no additional vars.
   */
  public void testFilterFile1() {
    Path ref = new File(REF_PREFIX + "/filefiltering1.ref").toPath();
    Path out = new File(System.getProperty("java.io.tmpdir") + "/dummyscript-exp.sh").toPath();
    try {
      FilterUtils.filterFile(m_Log, m_PathDummyScript, out, m_Model);
      String comparison = compare(ref, out);
      if (comparison != null)
        fail(comparison);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  /**
   * Tests the file filtering with default var prefix/suffix and additional vars.
   */
  public void testFilterFile2() {
    Path ref = new File(REF_PREFIX + "/filefiltering2.ref").toPath();
    Path out = new File(System.getProperty("java.io.tmpdir") + "/dummyscript-exp.sh").toPath();
    Map<String,String> vars = new HashMap<>();
    vars.put("comment", "This comment shows how to use custom variables");
    try {
      FilterUtils.filterFile(m_Log, m_PathDummyScript, out, m_Model, vars);
      String comparison = compare(ref, out);
      if (comparison != null)
        fail(comparison);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  /**
   * Tests the file filtering with custom var prefix/suffix and no additional vars.
   */
  public void testFilterFile3() {
    Path ref = new File(REF_PREFIX + "/filefiltering3.ref").toPath();
    Path out = new File(System.getProperty("java.io.tmpdir") + "/dummytext-exp.txt").toPath();
    try {
      FilterUtils.filterFile(m_Log, m_PathDummyText, out, m_Model, "@<", ">@");
      String comparison = compare(ref, out);
      if (comparison != null)
        fail(comparison);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  /**
   * Tests the file filtering with custom var prefix/suffix and additional vars.
   */
  public void testFilterFile4() {
    Path ref = new File(REF_PREFIX + "/filefiltering4.ref").toPath();
    Path out = new File(System.getProperty("java.io.tmpdir") + "/dummytext-exp.txt").toPath();
    Map<String,String> vars = new HashMap<>();
    vars.put("var1", "1.1");
    vars.put("var2", "1.2");
    vars.put("var3", "1.3");
    try {
      FilterUtils.filterFile(m_Log, m_PathDummyText, out, m_Model, vars, "@<", ">@");
      String comparison = compare(ref, out);
      if (comparison != null)
        fail(comparison);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  /**
   * Returns the test suite.
   *
   * @return		the suite
   */
  public static Test suite() {
    return new TestSuite(FilterUtilsTest.class);
  }

  /**
   * Runs the test from commandline.
   *
   * @param args	ignored
   */
  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
