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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.List;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.tokens.TokensRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

public class LoadTokensStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final ScannerReportReader reportReader;
  private final TokensRepository tokensRepository;

  public LoadTokensStep(TreeRootHolder treeRootHolder, ScannerReportReader reportReader, TokensRepository tokensRepository) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.tokensRepository = tokensRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new DepthTraversalTypeAwareCrawler(new TokenVisitor()).visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Load code tokens";
  }

  private class TokenVisitor extends TypeAwareVisitorAdapter {

    private TokenVisitor() {
      super(CrawlerDepthLimit.FILE, Order.PRE_ORDER);
    }

    @Override
    public void visitFile(Component file) {
      var fileRef = file.getReportAttributes().getRef();
      if (fileRef == null) {
        throw new IllegalStateException("File reference is missing for file: " + file.getKey());
      }

      try (CloseableIterator<ScannerReport.Token> tokensIterator = reportReader.readTokens(fileRef)) {
        if (tokensIterator != null) {
          tokensRepository.setTokens(file, processTokens(tokensIterator));
        }
      }
    }

    private List<ScannerReport.Token> processTokens(CloseableIterator<ScannerReport.Token> tokensIterator) {
      List<ScannerReport.Token> tokens = new ArrayList<>();
      while (tokensIterator.hasNext()) {
        tokens.add(tokensIterator.next());
      }
      return tokens;
    }
  }
}
