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
 * FilterUtils.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.simplemavenfilefiltering;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For filtering resources, replacing variables with actual values obtained from the model.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class FilterUtils {

  public static final String PROJECT = "project.";

  /**
   * Extracts variables from a line: ${blah} -> blah.
   *
   * @param log  	for logging
   * @param line 	the line to process
   * @param varPrefix 	the prefix of the variable (eg "${")
   * @param varSuffix 	the suffix of the variable (eg "}")
   * @return 		the variables found
   */
  protected static List<String> extractVariables(Log log, String line, String varPrefix, String varSuffix) {
    List<String> result;
    int indexStart;
    int indexEnd;
    String original;

    original = line;
    result = new ArrayList<>();
    while (line.length() > 0) {
      indexStart = line.indexOf(varPrefix);
      if (indexStart == -1) {
	line = "";
      }
      else {
	if ((indexEnd = line.indexOf(varSuffix, indexStart)) == -1) {
	  line = "";
	}
	else {
	  result.add(line.substring(indexStart + 2, indexEnd));
	  line = line.substring(indexEnd + 1);
	}
      }
    }

    if (result.size() > 0)
      log.debug("Variables extracted from line '" + original + "': " + result);

    return result;
  }

  /**
   * Obtains the variable value from the model ("project.*").
   *
   * @param log   	for logging
   * @param var   	the variable
   * @param model 	the model to obtain the value from
   * @return 		null if not found or not a string object, otherwise the value
   */
  protected static String variableValue(Log log, String var, Model model) {
    Object value;

    if (!var.startsWith(PROJECT)) {
      log.warn("Variable does not start with " + PROJECT + ": " + var);
      return null;
    }

    value = BeanUtils.extractValue(log, model, var.substring(PROJECT.length()));
    if (value == null) {
      log.warn("Failed to extract value for variable: " + var);
      return null;
    }
    else {
      log.debug("Value for variable " + var + ": " + value);
      return "" + value;
    }
  }

  /**
   * Filters a file, replacing any project or additional variables in it.
   * Uses "${" and "}" as prefix/suffix for variables.
   *
   * @param log        	for logging
   * @param input      	the input file
   * @param output     	the output file
   * @param model      	the model to use for resolving "project.*" variables
   * @throws IOException if reading/writing of file fails
   */
  public static void filterFile(Log log, Path input, Path output, Model model) throws IOException {
    filterFile(log, input, output, model, new HashMap<>());
  }

  /**
   * Filters a file, replacing any project or additional variables in it.
   * Uses "${" and "}" as prefix/suffix for variables.
   *
   * @param log        	for logging
   * @param input      	the input file
   * @param output     	the output file
   * @param model      	the model to use for resolving "project.*" variables
   * @param additional 	additional variables with associated values
   * @throws IOException if reading/writing of file fails
   */
  public static void filterFile(Log log, Path input, Path output, Model model, Map<String, String> additional) throws IOException {
    filterFile(log, input, output, model, additional, "${", "}");
  }

  /**
   * Filters a file, replacing any project or additional variables in it.
   *
   * @param log        	for logging
   * @param input      	the input file
   * @param output     	the output file
   * @param model      	the model to use for resolving "project.*" variables
   * @param varPrefix 	the prefix of the variable (eg "${")
   * @param varSuffix 	the suffix of the variable (eg "}")
   * @throws IOException if reading/writing of file fails
   */
  public static void filterFile(Log log, Path input, Path output, Model model, String varPrefix, String varSuffix) throws IOException {
    filterFile(log, input, output, model, new HashMap<>(), varPrefix, varSuffix);
  }

  /**
   * Filters a file, replacing any project or additional variables in it.
   *
   * @param log        	for logging
   * @param input      	the input file
   * @param output     	the output file
   * @param model      	the model to use for resolving "project.*" variables
   * @param additional 	additional variables with associated values
   * @param varPrefix 	the prefix of the variable (eg "${")
   * @param varSuffix 	the suffix of the variable (eg "}")
   * @throws IOException if reading/writing of file fails
   */
  public static void filterFile(Log log, Path input, Path output, Model model, Map<String, String> additional, String varPrefix, String varSuffix) throws IOException {
    List<String> 		lines;
    int 			i;
    String 			line;
    List<String> 		vars;
    String 			value;
    boolean 			updated;
    boolean 			modified;
    Map<String, String> 	cache;

    lines    = Files.readAllLines(input);
    cache    = new HashMap<>();
    modified = false;
    for (i = 0; i < lines.size(); i++) {
      line    = lines.get(i);
      updated = false;
      vars    = extractVariables(log, line, varPrefix, varSuffix);
      for (String var : vars) {
	if (cache.containsKey(var))
	  value = cache.get(var);
	else if (additional.containsKey(var))
	  value = additional.get(var);
	else
	  value = variableValue(log, var, model);
	cache.put(var, value);

	if (value != null) {
	  log.debug("Replacing variable '" + varPrefix + var + varSuffix + "' with value '" + value + "' in: " + line);
	  line = line.replace(varPrefix + var + varSuffix, value);
	  updated = true;
	}
      }
      if (updated) {
	lines.set(i, line);
	modified = true;
      }
    }

    if (modified) {
      log.debug("Writing filtered file " + input + " to " + output);
      Files.createDirectories(output.getParent());
      Files.write(output, lines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }
    else {
      log.debug("Copying unmodified file " + input + " to " + output);
      Files.createDirectories(output.getParent());
      Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
