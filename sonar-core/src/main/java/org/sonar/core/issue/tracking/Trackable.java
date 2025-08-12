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
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.algorithm.types.IssueToken;
import org.sonar.core.issue.tracking.algorithm.types.Snippet;

public interface Trackable {

  /**
   * The line index, starting with 1. Null means that
   * issue does not relate to a line (file issue for example).
   */
  @CheckForNull
  Integer getLine();

  /**
   * Trimmed message of issue
   */
  @CheckForNull
  String getMessage();

  @CheckForNull
  String getLineHash();

  RuleKey getRuleKey();

  String getStatus();

  default Snippet getSnippet() {
    throw new UnsupportedOperationException(
      String.format("Getting snippet is not supported for %s", getClass().getName()));
  }

  default void sortSnippet(Comparator<IssueToken> comparator) {
    throw new UnsupportedOperationException(
      String.format("Sorting snippet is not supported for %s", getClass().getName()));
  }

  default int getSnippetSize() {
    return getSnippet().size();
  }

  default IssueToken getToken(int index) {
    return getSnippet().get(index);
  }

  /**
   * Functional update date for the issue. See {@link DefaultIssue#updateDate()}
   */
  Date getUpdateDate();
}
