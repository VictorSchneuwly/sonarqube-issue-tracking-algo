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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Block {
  private final UUID id = UUID.randomUUID();
  private List<Token> tokens;

  public Block(List<Token> tokens) {
    // Copy list to avoid external modification
    this.tokens = new ArrayList<>(tokens);
  }

  public int getTokenCount() {
    return tokens.size();
  }

  public List<Token> getTokens() {
    return List.copyOf(tokens);
  }

  public Token get(int index) {
    return tokens.get(index);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass())
      return false;
    Block block = (Block) o;
    return Objects.equals(id, block.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
