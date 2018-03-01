/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.tools.protoc;

import com.google.protobuf.Message;
import io.spine.tools.protoc.test.PIUserEvent;
import io.spine.tools.protoc.test.UserInfo;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class PluginShould {

    private static final String EVENT_INTERFACE_FQN =
            "io.spine.tools.protoc.PICustomerEvent";
    private static final String COMMAND_INTERFACE_FQN =
            "io.spine.tools.protoc.PICustomerCommand";
    private static final String USER_COMMAND_FQN =
            "io.spine.tools.protoc.PIUserCommand";

    @Test
    public void generate_marker_interfaces() throws ClassNotFoundException {
        checkMarkerInterface(EVENT_INTERFACE_FQN);
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"}) // Required by the test logic.
    @Test
    public void implement_marker_interfaces_in_generated_messages() {
        assertTrue((Object) PICustomerNotified.getDefaultInstance() instanceof PICustomerEvent);
        assertTrue((Object) PICustomerEmailRecieved.getDefaultInstance() instanceof PICustomerEvent);
    }

    @Test
    public void generate_marker_interfaces_for_separate_messages() throws ClassNotFoundException {
        checkMarkerInterface(COMMAND_INTERFACE_FQN);
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"}) // Required by the test logic.
    @Test
    public void implement_interface_in_generated_messages_with_IS_option() {
        assertTrue((Object) PICustomerCreated.getDefaultInstance() instanceof PICustomerEvent);
        assertTrue((Object) PICreateCustomer.getDefaultInstance() instanceof PICustomerCommand);
    }

    @SuppressWarnings({"ConstantConditions", "RedundantCast"}) // Required by the test logic.
    @Test
    public void use_IS_in_priority_to_EVERY_IS() {
        assertTrue((Object) PIUserCreated.getDefaultInstance() instanceof PIUserEvent);
        assertTrue((Object) PIUserNameUpdated.getDefaultInstance() instanceof PIUserEvent);

        assertFalse((Object) UserName.getDefaultInstance() instanceof PIUserEvent);
        assertTrue((Object) UserName.getDefaultInstance() instanceof UserInfo);
    }

    @Test
    public void resolve_packages_from_src_proto_if_not_specified() throws ClassNotFoundException {
        final Class<?> cls = checkMarkerInterface(USER_COMMAND_FQN);
        assertTrue(cls.isAssignableFrom(PICreateUser.class));
    }

    @Test
    public void skip_non_specified_message_types() {
        final Class<?> cls = CustomerName.class;
        final Class[] interfaces = cls.getInterfaces();
        assertEquals(1, interfaces.length);
        assertSame(CustomerNameOrBuilder.class, interfaces[0]);
    }

    private static Class<?> checkMarkerInterface(String fqn) throws ClassNotFoundException {
        final Class<?> cls = Class.forName(fqn);
        assertTrue(cls.isInterface());
        assertTrue(Message.class.isAssignableFrom(cls));

        final Method[] declaredMethods = cls.getDeclaredMethods();
        assertEquals(0, declaredMethods.length);
        return cls;
    }
}