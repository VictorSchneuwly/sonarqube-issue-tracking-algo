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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.Trackable;

import static org.junit.jupiter.api.Assertions.*;

class GlobalTokenFrequencyMapTest {

  @Test
  void comparator_should_put_rarer_tokens_first() {
    // Given: Create trackables with different token frequencies
    // "rare" appears 1 time, "common" appears 3 times, "medium" appears 2 times
    List<Trackable> trackables = Arrays.asList(
      createTrackable(Arrays.asList("rare", "common")),
      createTrackable(Arrays.asList("common", "medium")),
      createTrackable(Arrays.asList("common", "medium")));

    GlobalTokenFrequencyMap frequencyMap = new GlobalTokenFrequencyMap(trackables);

    // Verify frequencies are as expected
    assertEquals(1L, frequencyMap.getFrequency("rare"));
    assertEquals(2L, frequencyMap.getFrequency("medium"));
    assertEquals(3L, frequencyMap.getFrequency("common"));

    // When: Get the comparator and sort tokens
    Comparator<String> comparator = frequencyMap.getComparator();
    List<String> tokens = Arrays.asList("common", "rare", "medium");
    tokens.sort(comparator);

    // Then: Rarer tokens should come first
    assertEquals(Arrays.asList("rare", "medium", "common"), tokens);
  }

  @Test
  void comparator_should_handle_equal_frequencies() {
    // Given: Trackables where tokens have equal frequencies
    List<Trackable> trackables = Arrays.asList(
      createTrackable(Arrays.asList("token1", "token2")),
      createTrackable(Arrays.asList("token3", "token4")));

    GlobalTokenFrequencyMap frequencyMap = new GlobalTokenFrequencyMap(trackables);

    // All tokens should have frequency 1
    assertEquals(1L, frequencyMap.getFrequency("token1"));
    assertEquals(1L, frequencyMap.getFrequency("token2"));
    assertEquals(1L, frequencyMap.getFrequency("token3"));
    assertEquals(1L, frequencyMap.getFrequency("token4"));

    // When: Get the comparator
    Comparator<String> comparator = frequencyMap.getComparator();

    // Then: Should return 0 for equal frequencies (stable sort)
    assertEquals(0, comparator.compare("token1", "token2"));
    assertEquals(0, comparator.compare("token3", "token4"));
  }

  private Trackable createTrackable(List<String> snippet) {
    return new TestTrackable(snippet);
  }

  private static class TestTrackable implements Trackable {
    private final List<String> snippet;

    TestTrackable(List<String> snippet) {
      this.snippet = snippet;
    }

    @Override
    public List<String> getSnippet() {
      return snippet;
    }

    @Override
    @CheckForNull
    public Integer getLine() {
      return null;
    }

    @Override
    @CheckForNull
    public String getMessage() {
      return null;
    }

    @Override
    @CheckForNull
    public String getLineHash() {
      return null;
    }

    @Override
    public RuleKey getRuleKey() {
      return RuleKey.of("repo", "rule");
    }

    @Override
    public String getStatus() {
      return "OPEN";
    }

    @Override
    public Date getUpdateDate() {
      return new Date();
    }
  }
}
