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
package org.sonar.ce.task.projectanalysis.tokens;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.tracking.algorithm.types.IssueToken;
import org.sonar.core.issue.tracking.algorithm.types.Snippet;
import org.sonar.scanner.protocol.output.ScannerReport;

public class TokensRepository {
  private static final int DEFAULT_WINDOW_SIZE = 4;

  private final Map<Component, List<ScannerReport.Token>> tokensByComponent = new HashMap<>();

  public void setTokens(Component component, List<ScannerReport.Token> tokens) {
    tokensByComponent.put(component, tokens);
  }

  public List<ScannerReport.Token> getTokens(Component component) {
    return tokensByComponent.getOrDefault(component, List.of());
  }

  public boolean hasTokens(Component component) {
    return tokensByComponent.containsKey(component);
  }

  public Snippet getTokensSnippet(Component component, ScannerReport.TextRange textRange) {
    return getTokensSnippet(component, textRange, DEFAULT_WINDOW_SIZE);
  }

  public Snippet getTokensSnippet(Component component, ScannerReport.TextRange textRange, int windowSize) {
    if (windowSize <= 0 || !tokensByComponent.containsKey(component)) {
      return new Snippet(List.of());
    }

    List<ScannerReport.Token> tokens = tokensByComponent.get(component);

    int targetIndex = getTargetIndex(textRange, tokens);
    int startIndex = Math.max(0, targetIndex - windowSize);
    int endIndex = Math.min(tokens.size(), targetIndex + windowSize + 1);

    List<ScannerReport.Token> snippet = tokens.subList(startIndex, endIndex);
    if (snippet.isEmpty()) {
      throw new IllegalArgumentException("No tokens found in the specified range for component: " + component);
    }

    return getSnippetWithDistance(snippet, targetIndex - startIndex);
  }

  private static int getTargetIndex(ScannerReport.TextRange textRange, List<ScannerReport.Token> tokens) {
    // Find the token that matches the start of the text range
    OptionalInt targetIndexOpt = IntStream.range(0, tokens.size())
      .filter(i -> tokens.get(i).getLine() == textRange.getStartLine() &&
        tokens.get(i).getColumn() == textRange.getStartOffset())
      .findFirst();

    if (targetIndexOpt.isEmpty()) {
      throw new IllegalArgumentException("No token found for the specified text range: " + textRange);
    }

    return targetIndexOpt.getAsInt();
  }

  private static Snippet getSnippetWithDistance(List<ScannerReport.Token> tokens, int targetIndex) {
    return IntStream.range(0, tokens.size())
      .mapToObj(i -> {
        String tokenValue = tokens.get(i).getText();
        int distance = Math.abs(i - targetIndex);
        return new IssueToken(tokenValue, distance);
      })
      .collect(Collectors.collectingAndThen(Collectors.toList(), Snippet::new));
  }
}
