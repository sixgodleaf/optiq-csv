/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.csv;

import net.hydromatic.optiq.*;
import net.hydromatic.optiq.impl.AbstractSchema;

import com.google.common.collect.ImmutableMap;

import java.io.*;
import java.util.*;

/**
 * Schema mapped onto a directory of CSV files. Each table in the schema
 * is a CSV file in that directory.
 */
public class CsvSchema extends AbstractSchema {
  final File directoryFile;
  private final boolean smart;

  /**
   * Creates a CSV schema.
   *
   * @param directoryFile Directory that holds {@code .csv} files
   * @param smart      Whether to instantiate smart tables that undergo
   *                   query optimization
   */
  public CsvSchema(File directoryFile, boolean smart) {
    super();
    this.directoryFile = directoryFile;
    this.smart = smart;
  }

  /** Looks for a suffix on a string and returns
   * either the string with the suffix removed
   * or the original string. */
  private static String trim(String s, String suffix) {
    String trimmed = trimOrNull(s, suffix);
    return trimmed != null ? trimmed : s;
  }

  /** Looks for a suffix on a string and returns
   * either the string with the suffix removed
   * or null. */
  private static String trimOrNull(String s, String suffix) {
    return s.endsWith(suffix)
        ? s.substring(0, s.length() - suffix.length())
        : null;
  }

  @Override
  protected Map<String, Table> getTableMap() {
    final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();
    File[] files = directoryFile.listFiles(
        new FilenameFilter() {
          public boolean accept(File dir, String name) {
            final String nameSansGz = trim(name, ".gz");
            return nameSansGz.endsWith(".csv")
                || nameSansGz.endsWith(".json");
          }
        });
    if (files == null) {
      System.out.println("directory " + directoryFile + " not found");
      files = new File[0];
    }
    for (File file : files) {
      String tableName = trim(file.getName(), ".gz");
      final String tableNameSansJson = trimOrNull(tableName, ".json");
      if (tableNameSansJson != null) {
        JsonTable table = new JsonTable(file);
        builder.put(tableNameSansJson, table);
        continue;
      }
      tableName = trim(tableName, ".csv");
      final CsvTable table;
      if (smart) {
        table = new CsvSmartTable(file, null);
      } else {
        table = new CsvTable(file, null);
      }
      builder.put(tableName, table);
    }
    return builder.build();
  }
}

// End CsvSchema.java
