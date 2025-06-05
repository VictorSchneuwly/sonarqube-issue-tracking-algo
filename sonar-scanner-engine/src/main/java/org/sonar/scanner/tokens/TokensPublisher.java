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
package org.sonar.scanner.tokens;

import com.google.protobuf.StringValue;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.util.ProgressReport;

public class TokensPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(TokensPublisher.class);

  private final TokenPipe pipe;
  private final ReportPublisher publisher;
  private final ProgressReport progressReport;
  private final ExecutorService executorService;

  @Inject
  public TokensPublisher(TokenPipe pipe, ReportPublisher publisher) {
    this(pipe, publisher, Executors.newSingleThreadExecutor());
  }

  public TokensPublisher(TokenPipe pipe, ReportPublisher publisher, ExecutorService executorService) {
    this.pipe = pipe;
    this.publisher = publisher;
    this.progressReport = new ProgressReport("Tokenization", TimeUnit.SECONDS.toMillis(10));
    this.executorService = executorService;
  }

  public void execute() {
    progressReport.start("Starting tokenization");

    for (Map.Entry<Integer, List<String>> entry : pipe.getMap().entrySet()) {
      int scannerId = entry.getKey();
      List<String> tokens = entry.getValue();

      Iterable<StringValue> stringTokens = () -> tokens.stream()
        .filter(token -> token != null && !token.isEmpty())
        .map(token -> StringValue.newBuilder().setValue(token).build())
        .iterator();

      publisher.getWriter().writeTokens(scannerId, stringTokens);
    }

    progressReport.stopAndLogTotalTime("Tokenization completed");
    executorService.shutdown();
  }
}
