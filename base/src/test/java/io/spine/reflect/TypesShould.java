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

package io.spine.reflect;

import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static io.spine.reflect.Types.listTypeOf;
import static io.spine.reflect.Types.mapTypeOf;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
        "SerializableInnerClassWithNonSerializableOuterClass"}) // It is OK for test methods.
public class TypesShould {

    @Test
    public void pass_the_null_tolerance_check() {
        NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(Types.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Types.class);
    }

    @Test
    public void create_map_type() {
        Type type = mapTypeOf(String.class, Integer.class);
        Type expectedType = new TypeToken<Map<String, Integer>>(){}.getType();
        assertEquals(expectedType, type);
    }

    @Test
    public void create_list_type() {
        Type type = listTypeOf(String.class);
        Type expectedType = new TypeToken<List<String>>(){}.getType();
        assertEquals(expectedType, type);
    }
}
