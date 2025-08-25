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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.MutablePair;
import org.sonar.core.issue.tracking.Trackable;
import org.sonar.core.issue.tracking.algorithm.types.IssueToken;

public class CloneDetection {

  public record Candidate(Trackable trackable, int similarityScore) {
  }

  private final GlobalTokenFrequencyMap globalTokenFrequencyMap;
  private final PartialIndex index;
  private final double threshold;
  private final Comparator<IssueToken> tokenComparator;

  public CloneDetection(GlobalTokenFrequencyMap globalTokenFrequencyMap, PartialIndex partialIndex, double threshold) {
    this.globalTokenFrequencyMap = globalTokenFrequencyMap;
    this.tokenComparator = Utils.createComparator(globalTokenFrequencyMap);
    this.index = partialIndex;
    this.threshold = threshold;
  }

  public Set<Candidate> compareBlock(Trackable trackable) {
    // var selectedClones = new ArrayList<Trackable>();
    var selectedClones = new HashSet<Candidate>();
    var cloneCandidates = new HashMap<Trackable, MutablePair<Integer, Integer>>();
    trackable.sortSnippet(tokenComparator);

    var nbTokens = trackable.getSnippetSize();
    var querySubBlock = nbTokens - Math.ceil(nbTokens * threshold) + 1;

    // TODO: potential for parallelization
    for (int blockTokenIndex = 0; blockTokenIndex < querySubBlock; blockTokenIndex++) {
      var token = trackable.getToken(blockTokenIndex);
      for (var entry : index.get(token.value())) {
        Trackable candidate = entry.getLeft();
        if (!shouldConsider(candidate, trackable, threshold)) {
          continue;
        }

        int candidateTokenCount = candidate.getSnippetSize();
        int candidateTokenIndex = entry.getRight();
        double minimumNumberOfTokens = computeMinimumNumberOfTokens(trackable, candidate, threshold);
        int uBound = 1 + Math.min(nbTokens - blockTokenIndex, candidateTokenCount - candidateTokenIndex);
        int candidateIncrement = cloneCandidates
          .getOrDefault(candidate, MutablePair.of(0, 0))
          .getLeft();

        if (candidateIncrement + uBound >= minimumNumberOfTokens) {
          cloneCandidates.merge(candidate, MutablePair.of(1, candidateTokenIndex), (oldValue, newValue) -> {
            // Increment count
            oldValue.left += newValue.getLeft();
            // Keep track of last index
            oldValue.right = newValue.getRight();
            return oldValue;
          });
        } else {
          // equivalent to (0,0) in paper but remove need for filtering
          cloneCandidates.remove(candidate);
        }
      }

      selectedClones.addAll(verifyCandidates(trackable, blockTokenIndex, cloneCandidates, threshold));

      // Reset the candidates for the next block
      cloneCandidates.clear();
    }

    return selectedClones;
  }

  private double computeMinimumNumberOfTokens(Trackable block, Trackable candidate, double threshold) {
    return Math.ceil(Math.max(block.getSnippetSize(), candidate.getSnippetSize()) * threshold);
  }

  private boolean shouldConsider(Trackable candidate, Trackable checked, double threshold) {
    var candidateTokenCount = candidate.getSnippetSize();
    var checkedTokenCount = checked.getSnippetSize();
    return candidateTokenCount > Math.ceil(threshold * checkedTokenCount);
  }

  private List<Candidate> verifyCandidates(Trackable block, int lastTokenFromBlock, Map<Trackable, MutablePair<Integer, Integer>> cloneCandidates,
    double threshold) {
    List<Candidate> verifiedClones = new ArrayList<>();

    for (var entry : cloneCandidates.entrySet()) {
      Trackable candidate = entry.getKey();
      var minimumNumberOfTokens = computeMinimumNumberOfTokens(block, candidate, threshold);
      var lastTokenIndexInCandidate = entry.getValue().getRight();
      while (lastTokenFromBlock < block.getSnippetSize() && lastTokenIndexInCandidate < candidate.getSnippetSize()) {
        if (Math.min(block.getSnippetSize() - lastTokenFromBlock, candidate.getSnippetSize() - lastTokenIndexInCandidate) < minimumNumberOfTokens) {
          break;
        }

        IssueToken blockToken = block.getToken(lastTokenFromBlock);
        IssueToken candidateToken = candidate.getToken(lastTokenIndexInCandidate);

        if (blockToken.equals(candidateToken)) {
          // Increase similarity score
          entry.getValue().left += 1;
          lastTokenFromBlock += 1;
          lastTokenIndexInCandidate += 1;
        } else if (globalTokenFrequencyMap.getFrequency(blockToken) < globalTokenFrequencyMap.getFrequency(candidateToken)) {
          lastTokenFromBlock += 1;
        } else {
          lastTokenIndexInCandidate += 1;
        }
      }

      // If the similarity is above minimum, add to verified clones
      int similarityScore = entry.getValue().getLeft();
      if (similarityScore > minimumNumberOfTokens) {
        verifiedClones.add(new Candidate(candidate, similarityScore));
      }
    }

    return verifiedClones;
  }
}
