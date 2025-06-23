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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.scanner.protocol.output.ScannerReport;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokensRepositoryTest {

  private TokensRepository repository;
  private Component component;
  private ScannerReport.Token token1;
  private ScannerReport.Token token2;
  private ScannerReport.Token token3;

  @BeforeEach
  void setUp() {
    repository = new TokensRepository();
    component = mock(Component.class);

    token1 = mock(ScannerReport.Token.class);
    when(token1.getLine()).thenReturn(1);
    when(token1.getColumn()).thenReturn(0);

    token2 = mock(ScannerReport.Token.class);
    when(token2.getLine()).thenReturn(1);
    when(token2.getColumn()).thenReturn(5);

    token3 = mock(ScannerReport.Token.class);
    when(token3.getLine()).thenReturn(2);
    when(token3.getColumn()).thenReturn(0);
  }

  @Test
  void set_and_get_tokens() {
    repository.setTokens(component, List.of(token1, token2));
    List<ScannerReport.Token> tokens = repository.getTokens(component);
    assertEquals(2, tokens.size());
    assertEquals(token1, tokens.get(0));
    assertEquals(token2, tokens.get(1));
  }

  @Test
  void getTokens_returns_empty_list_if_none_set() {
    assertTrue(repository.getTokens(component).isEmpty());
  }

  @Test
  void hasTokens_returns_true_if_tokens_set() {
    repository.setTokens(component, List.of(token1));
    assertTrue(repository.hasTokens(component));
  }

  @Test
  void hasTokens_returns_false_if_no_tokens_set() {
    assertFalse(repository.hasTokens(component));
  }

  @Test
  void getTokensSnippet_returns_correct_snippet() {
    repository.setTokens(component, List.of(token1, token2, token3));
    ScannerReport.TextRange range = mock(ScannerReport.TextRange.class);
    when(range.getStartLine()).thenReturn(1);
    when(range.getStartOffset()).thenReturn(5);

    List<ScannerReport.Token> snippet = repository.getTokensSnippet(component, range, 1);
    assertEquals(List.of(token1, token2, token3), snippet);
  }

  @Test
  void getTokensSnippet_throws_if_no_token_matches_range() {
    repository.setTokens(component, List.of(token1, token2));
    ScannerReport.TextRange range = mock(ScannerReport.TextRange.class);
    when(range.getStartLine()).thenReturn(99);
    when(range.getStartOffset()).thenReturn(99);

    assertThrows(IllegalArgumentException.class, () ->
      repository.getTokensSnippet(component, range, 1)
    );
  }

  @Test
  void getTokensSnippet_throws_if_snippet_empty() {
    repository.setTokens(component, List.of(token1));
    ScannerReport.TextRange range = mock(ScannerReport.TextRange.class);
    when(range.getStartLine()).thenReturn(1);
    when(range.getStartOffset()).thenReturn(0);

    // windowSize large enough to include the only token
    assertDoesNotThrow(() -> repository.getTokensSnippet(component, range, 1));
  }

  @Test
  void getTokensSnippet_returns_empty_if_windowSize_invalid_or_component_missing() {
    // windowSize <= 0
    repository.setTokens(component, List.of(token1));
    ScannerReport.TextRange range = mock(ScannerReport.TextRange.class);
    when(range.getStartLine()).thenReturn(1);
    when(range.getStartOffset()).thenReturn(0);

    assertTrue(repository.getTokensSnippet(component, range, 0).isEmpty());
    assertTrue(repository.getTokensSnippet(component, range, -1).isEmpty());

    // component not present
    Component otherComponent = mock(Component.class);
    assertTrue(repository.getTokensSnippet(otherComponent, range, 1).isEmpty());
  }

  @Test
  void getTokensSnippet_default_window_size() {
    repository.setTokens(component, List.of(token1, token2, token3));
    ScannerReport.TextRange range = mock(ScannerReport.TextRange.class);
    when(range.getStartLine()).thenReturn(1);
    when(range.getStartOffset()).thenReturn(5);

    List<ScannerReport.Token> snippet = repository.getTokensSnippet(component, range);
    assertEquals(List.of(token1, token2, token3), snippet);
  }

  private List<ScannerReport.Token> createTokens(int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> {
        ScannerReport.Token t = mock(ScannerReport.Token.class);
        when(t.getLine()).thenReturn(1);
        when(t.getColumn()).thenReturn(i);
        return t;
      })
      .toList();
  }

  @Test
  void getTokensSnippet_window_size_first_token() {
    List<ScannerReport.Token> tokens = createTokens(10);
    repository.setTokens(component, tokens);

    ScannerReport.TextRange rangeFirst = mock(ScannerReport.TextRange.class);
    when(rangeFirst.getStartLine()).thenReturn(1);
    when(rangeFirst.getStartOffset()).thenReturn(0);
    List<ScannerReport.Token> snippetFirst = repository.getTokensSnippet(component, rangeFirst, 2);
    assertEquals(tokens.subList(0, 3), snippetFirst);
  }

  @Test
  void getTokensSnippet_window_size_last_token() {
    List<ScannerReport.Token> tokens = createTokens(10);
    repository.setTokens(component, tokens);

    ScannerReport.TextRange rangeLast = mock(ScannerReport.TextRange.class);
    when(rangeLast.getStartLine()).thenReturn(1);
    when(rangeLast.getStartOffset()).thenReturn(9);
    List<ScannerReport.Token> snippetLast = repository.getTokensSnippet(component, rangeLast, 2);
    assertEquals(tokens.subList(7, 10), snippetLast);
  }

  @Test
  void getTokensSnippet_window_size_middle_token_and_zero_window() {
    List<ScannerReport.Token> tokens = createTokens(10);
    repository.setTokens(component, tokens);

    ScannerReport.TextRange rangeMid = mock(ScannerReport.TextRange.class);
    when(rangeMid.getStartLine()).thenReturn(1);
    when(rangeMid.getStartOffset()).thenReturn(5);
    List<ScannerReport.Token> snippetMid = repository.getTokensSnippet(component, rangeMid, 2);
    assertEquals(tokens.subList(3, 8), snippetMid);

    // Test for window size 0 (should return empty)
    assertTrue(repository.getTokensSnippet(component, rangeMid, 0).isEmpty());
  }
}
