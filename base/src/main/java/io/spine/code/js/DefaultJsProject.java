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

package io.spine.code.js;

import io.spine.code.DefaultProject;

import java.io.File;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default directory structure for a Spine-based JavaScript project.
 *
 * <p>The project structure reflects the conventions currently used in Spine, and contains at least
 * the following directories:
 *
 * <ul>
 * <li>{@code build}
 * <ul>
 *     <li>{@code descriptors}
 *     <ul>
 *         <li>{@code main}
 *         <ul>
 *             <li>{@code known_types.desc} — descriptors for "main" source set.
 *         </ul>
 *         <li>{@code test}
 *         <ul>
 *             <li>{@code known_types.desc} — descriptors for "test" source set.
 *         </ul>
 *     </ul>
 * </ul>
 *
 * <li>{@code proto} — the code generated from Protobuf messages.
 * <ul>
 *     <li>{@code main}
 *     <ul>
 *         <li>{@code js} — Protobuf messages from "main" source set.
 *     </ul>
 *     <li>{@code test}
 *     <ul>
 *         <li>{@code js} — Protobuf messages from "test" source set.
 *     </ul>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>Other directories (like source code directory) may also be present in the project, but their
 * location is currently not standardized and thus is not described by this class.
 *
 * @author Dmytro Kuzmin
 */
public final class DefaultJsProject extends DefaultProject {

    private DefaultJsProject(Path path) {
        super(path);
    }

    public static DefaultJsProject at(Path root) {
        checkNotNull(root);
        DefaultJsProject result = new DefaultJsProject(root);
        return result;
    }

    public static DefaultJsProject at(File projectDir) {
        checkNotNull(projectDir);
        return at(projectDir.toPath());
    }

    /**
     * The root folder for generated Protobuf messages.
     */
    public GeneratedProtoRoot proto() {
        return new GeneratedProtoRoot(this);
    }

    public static final class GeneratedProtoRoot extends SourceRoot {

        @SuppressWarnings("DuplicateStringLiteralInspection") // Same name in different context.
        private static final String DIR_NAME = "proto";

        private GeneratedProtoRoot(DefaultProject parent) {
            super(parent, DIR_NAME);
        }

        public Directory mainJs() {
            return Directory.rootIn(getMain());
        }

        public Directory testJs() {
            return Directory.rootIn(getTest());
        }
    }
}
