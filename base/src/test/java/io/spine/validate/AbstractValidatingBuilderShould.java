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

package io.spine.validate;

import com.google.protobuf.UInt32Value;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;

public class AbstractValidatingBuilderShould {

    @Test
    public void convert_to_map() {
        String key1 = "key1";
        UInt32Value value = UInt32Value.newBuilder()
                                       .setValue(123)
                                       .build();
        String mapStr = "\"key1\":\"123\",\"key2\":\"234\"";

        UInt32ValueVBuilder uInt32ValueVBuilder = UInt32ValueVBuilder.newBuilder();
        Map<String, UInt32Value> convertedValue =
                uInt32ValueVBuilder.convertToMap(mapStr,
                                                 String.class,
                                                 UInt32Value.class);

        assertTrue(convertedValue.containsKey(key1));
        assertTrue(convertedValue.containsValue(value));
    }

    @Test
    public void convert_to_list() {
        String key1 = "key1";
        String value = "123";
        String listStr = "\"key1\",\"123\",\"key2\",\"234\"";

        StringValueVBuilder stringValueVBuilder = StringValueVBuilder.newBuilder();
        List<String> convertedValue = stringValueVBuilder.convertToList(listStr,
                                                                        String.class);

        assertTrue(convertedValue.contains(key1));
        assertTrue(convertedValue.contains(value));
    }
}
