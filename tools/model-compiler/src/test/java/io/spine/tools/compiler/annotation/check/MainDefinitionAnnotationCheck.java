/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.tools.compiler.annotation.check;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.forge.roaster.model.impl.AbstractJavaSource;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.compiler.annotation.check.Annotations.findSpiAnnotation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MainDefinitionAnnotationCheck implements SourceCheck {

    private final boolean shouldBeAnnotated;

    public MainDefinitionAnnotationCheck(boolean shouldBeAnnotated) {
        this.shouldBeAnnotated = shouldBeAnnotated;
    }

    @Override
    public @Nullable Void apply(@Nullable AbstractJavaSource<JavaClassSource> input) {
        checkNotNull(input);
        AnnotationSource annotationSource = findSpiAnnotation(input);
        if (shouldBeAnnotated) {
            assertNotNull(annotationSource);
        } else {
            assertNull(annotationSource);
        }
        return null;
    }
}
