/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.spine.tools.codestyle.javadoc;

import com.google.common.base.Optional;
import io.spine.tools.codestyle.AbstractJavaStyleCheck;
import io.spine.tools.codestyle.CodeStyleViolation;
import io.spine.tools.codestyle.StepConfiguration;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

/**
 * Checks that Javadoc links correctly reference fully-qualified class names.
 *
 * <p>In case if any violation is found it will be logged as warning in build's
 * stacktrace info or an error will be thrown. That depends on threshold and report type parameters
 * stated in build file.
 *
 * @author Alexander Aleksandrov
 */
public class JavadocLinkCheck extends AbstractJavaStyleCheck {

    private final StepConfiguration configuration;

    JavadocLinkCheck(StepConfiguration configuration) {
        super();
        this.configuration = configuration;
    }

    @Override
    protected void processResult() {
        if (numberOfViolations() > configuration.getThreshold()
                                                .getValue()) {
            onAboveThreshold();
        }
    }

    /**
     * Describes the behavior in case if threshold is exceeded.
     */
    private void onAboveThreshold() {
        reportViolations();
        configuration.getReportType()
                     .logOrFail(new InvalidJavadocLinkException());
    }

    @Override
    public List<CodeStyleViolation> findViolations(List<String> fileContent) {
        int lineNumber = 0;
        final List<CodeStyleViolation> invalidLinks = newArrayList();
        for (String line : fileContent) {
            final Optional<CodeStyleViolation> result = checkSingleComment(line);
            lineNumber++;
            if (result.isPresent()) {
                final CodeStyleViolation codeStyleViolation = result.get()
                                                                    .withLineNumber(lineNumber);
                invalidLinks.add(codeStyleViolation);
            }
        }
        return invalidLinks;
    }

    private static Optional<CodeStyleViolation> checkSingleComment(String comment) {
        final Matcher matcher = JavadocPattern.LINK.getPattern()
                                                   .matcher(comment);
        final boolean found = matcher.find();
        if (found) {
            final String improperUsage = matcher.group(0);
            final CodeStyleViolation result = new CodeStyleViolation(improperUsage);
            return Optional.of(result);
        }
        return Optional.absent();
    }

    @Override
    public void onViolation(Path file, CodeStyleViolation v) {
        final String msg = format(
                " Wrong link format found: %s on %s line in %s",
                v.getCodeLine(), v.getLineNumber(), file
        );
        log().error(msg);
    }
}
