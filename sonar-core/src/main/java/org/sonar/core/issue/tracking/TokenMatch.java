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
package org.sonar.core.issue.tracking;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.OptionalInt;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.core.issue.tracking.algorithm.CloneDetection;
import org.sonar.core.issue.tracking.algorithm.GlobalTokenFrequencyMap;
import org.sonar.core.issue.tracking.algorithm.PartialIndex;

public class TokenMatch<RAW extends Trackable, BASE extends Trackable> {
  private static final double THRESHOLD = 0.5;

  private final CloneDetection cloneDetection;

  protected TokenMatch(Tracking<RAW, BASE> tracking) {
    List<Trackable> unmatched = tracking.getAllUnmatched().collect(Collectors.toList());
    List<Trackable> unmatchedBases = tracking.getUnmatchedBases().collect(Collectors.toList());

    GlobalTokenFrequencyMap globalTokenFrequencyMap = new GlobalTokenFrequencyMap(unmatched);
    PartialIndex basePartialIndex = new PartialIndex(
      globalTokenFrequencyMap,
      // The partial index only uses unmatched bases
      // as this is what raw issues will be matched against
      unmatchedBases,
      THRESHOLD);
    this.cloneDetection = new CloneDetection(globalTokenFrequencyMap, basePartialIndex, THRESHOLD);
  }

  protected void match(Tracking<RAW, BASE> tracking) {
    if (tracking.isComplete() || tracking.getUnmatchedBases().toList().isEmpty()) {
      return;
    }

    Set<BASE> alreadyMatched = new HashSet<>();

    tracking.getUnmatchedRaws()
      .map(raw -> Pair.of(raw, cloneDetection.compareBlock(raw)))
      // We want to go over the raw issues that have a candidate with the highest similarity score first
      .sorted((entry1, entry2) -> {
        OptionalInt maxScore1 = entry1.getRight().stream()
          .mapToInt(CloneDetection.Candidate::similarityScore)
          .max();
        OptionalInt maxScore2 = entry2.getRight().stream()
          .mapToInt(CloneDetection.Candidate::similarityScore)
          .max();

        return Integer.compare(
          maxScore2.orElse(0),
          maxScore1.orElse(0));
      })
      .forEach(entry -> {
        RAW raw = entry.getLeft();
        Set<CloneDetection.Candidate> candidates = entry.getRight();

        candidates.stream()
          // A BASE issue can only be matched once
          .filter(candidate -> !alreadyMatched.contains((BASE) candidate.trackable()))
          .max(Comparator
            // Compare by similarity score first
            .comparingInt(CloneDetection.Candidate::similarityScore)
            // Then by update date as tiebreaker
            .thenComparing(candidate -> candidate.trackable().getUpdateDate()))
          .ifPresent(candidate -> {
            BASE baseCandidate = (BASE) candidate.trackable();
            tracking.match(raw, baseCandidate);
            alreadyMatched.add(baseCandidate);
          });
      });
  }
}
