/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue.tracking.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.core.issue.tracking.Trackable;

public class PartialIndex {
  private final Map<String, List<Pair<Trackable, Integer>>> index;

  public PartialIndex(
    GlobalTokenFrequencyMap globalTokenFrequencyMap,
    List<Trackable> blocks,
    double threshold) {
    this.index = createPartialIndex(globalTokenFrequencyMap, blocks, threshold);
  }

  public List<Pair<Trackable, Integer>> get(String token) {
    return index.getOrDefault(token, List.of());
  }

  public Set<Map.Entry<String, List<Pair<Trackable, Integer>>>> entrySet() {
    return index.entrySet();
  }

  private static Map<String, List<Pair<Trackable, Integer>>> createPartialIndex(
    GlobalTokenFrequencyMap globalTokenFrequencyMap,
    List<Trackable> blocks,
    double threshold) {
    var index = new HashMap<String, List<Pair<Trackable, Integer>>>();
    // TODO: potential for parallel processing => spliterator with parallel stream?
    for (Trackable trackable : blocks) {
      trackable.sortSnippet(globalTokenFrequencyMap.getComparator());
      var nbTokens = trackable.getSnippetSize();
      var tokensToBeIndexed = nbTokens - Math.ceil(nbTokens * threshold) + 1;
      for (int i = 0; i < tokensToBeIndexed; i++) {
        var token = trackable.getToken(i);
        index.computeIfAbsent(token, k -> new ArrayList<>())
          .add(Pair.of(trackable, i));
      }
    }

    return index;
  }
}
