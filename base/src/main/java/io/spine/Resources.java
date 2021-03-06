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

package io.spine;

/**
 * Constants for resources generated by Spine Model Compiler during the build process.
 */
public final class Resources {

    /*
      IMPLEMENTATION NOTE

      This class is a temporary solution for avoiding code duplication in declaration of
      resource file names under `base` and `core-java`.

      Presumably, Validation and Enrichments would be extracted into separate libraries,
      and Known Types would be based on parsing Descriptor Set added to program resources
      _instead_ of building a map.

      Therefore, it is expected that this class will be eliminated in the near future.
     */

    /**
     * The name of the file with enrichment rules.
     *
     * <p>NOTE: the filename is referenced by {@code core-java} as well,
     * make sure to update {@code core-java} project upon changing this value.
     */
    public static final String ENRICHMENTS = "enrichments.properties";

    /**
     * A name of the file, which contains validation rules and their target field paths.
     */
    public static final String VALIDATION_RULES = "validation_rules.properties";

    /** Prevents instantiation of this utility class. */
    private Resources() {
    }
}
