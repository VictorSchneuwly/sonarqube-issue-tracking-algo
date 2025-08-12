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
package org.sonar.db.issuetoken;

import java.io.Serializable;
import java.util.Objects;
import org.sonar.core.issue.tracking.algorithm.types.IssueToken;

public final class IssueTokenDto implements Serializable {
  private String uuid;
  private String token;
  private String issueUuid;
  /** 
   * Distance is used to store the distance between the token and the issue position.
   */
  private long distance;

  public IssueTokenDto() {
    // nothing to do
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getIssueUuid() {
    return issueUuid;
  }

  public void setIssueUuid(String issueUuid) {
    this.issueUuid = issueUuid;
  }

  public long getDistance() {
    return distance;
  }

  public void setDistance(long distance) {
    this.distance = distance;
  }

  public IssueToken toIssueToken() {
    return new IssueToken(token, distance);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IssueTokenDto that = (IssueTokenDto) o;
    return Objects.equals(uuid, that.uuid)
      && Objects.equals(token, that.token)
      && Objects.equals(issueUuid, that.issueUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, token, issueUuid);
  }
}
