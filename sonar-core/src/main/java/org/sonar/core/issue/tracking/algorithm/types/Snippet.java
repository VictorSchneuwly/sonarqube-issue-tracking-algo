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
package org.sonar.core.issue.tracking.algorithm.types;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class Snippet implements Iterable<IssueToken> {

  private final UUID id = UUID.randomUUID();
  private final List<IssueToken> issueTokens;
  private final boolean mutable;

  public Snippet(List<IssueToken> issueTokens) {
    this(issueTokens, true);
  }

  private Snippet(List<IssueToken> issueTokens, boolean mutable) {
    this.mutable = mutable;
    this.issueTokens = mutable
      ? new ArrayList<>(issueTokens)
      : List.copyOf(issueTokens);
  }

  public int size() {
    return issueTokens.size();
  }

  public boolean isEmpty() {
    return issueTokens.isEmpty();
  }

  public List<IssueToken> getTokens() {
    return List.copyOf(issueTokens);
  }

  public IssueToken get(int index) {
    return issueTokens.get(index);
  }

  public Stream<IssueToken> stream() {
    return issueTokens.stream();
  }

  public void sort(Comparator<IssueToken> comparator) {
    if (!mutable) {
      throw new UnsupportedOperationException("Cannot sort immutable snippet");
    }

    issueTokens.sort(comparator);
  }

  public Snippet immutableCopy() {
    return new Snippet(issueTokens, false);
  }

  @NotNull
  @Override
  public Iterator<IssueToken> iterator() {
    return issueTokens.iterator();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Snippet snippet = (Snippet) o;
    return Objects.equals(id, snippet.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
