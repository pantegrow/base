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

package io.spine.tools.protojs.field.parser.primitive;

import io.spine.generate.JsOutput;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.protobuf.Descriptors.FieldDescriptor.Type.BYTES;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.FLOAT;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.INT32;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.INT64;
import static io.spine.testing.Verify.assertInstanceOf;

/**
 * @author Dmytro Kuzmin
 */
@DisplayName("PrimitiveParsers utility should")
class PrimitiveParsersTest extends UtilityClassTest<PrimitiveParsers> {

    private JsOutput jsOutput;

    PrimitiveParsersTest() {
        super(PrimitiveParsers.class);
    }

    @BeforeEach
    void setUp() {
        jsOutput = new JsOutput();
    }

    @Test
    @DisplayName("create identity parser")
    void createIdentityParser() {
        PrimitiveParser parser = PrimitiveParsers.createFor(INT32, jsOutput);
        assertInstanceOf(IdentityParser.class, parser);
    }

    @Test
    @DisplayName("create parser for long value")
    void createLongParser() {
        PrimitiveParser parser = PrimitiveParsers.createFor(INT64, jsOutput);
        assertInstanceOf(LongParser.class, parser);
    }

    @Test
    @DisplayName("create parser for float value")
    void createFloatParser() {
        PrimitiveParser parser = PrimitiveParsers.createFor(FLOAT, jsOutput);
        assertInstanceOf(FloatParser.class, parser);
    }

    @Test
    @DisplayName("create parser for bytes value")
    void createBytesParser() {
        PrimitiveParser parser = PrimitiveParsers.createFor(BYTES, jsOutput);
        assertInstanceOf(BytesParser.class, parser);
    }
}
