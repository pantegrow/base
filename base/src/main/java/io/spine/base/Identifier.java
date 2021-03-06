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

package io.spine.base;

import com.google.common.reflect.TypeToken;
import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.StringValue;
import io.spine.annotation.Internal;
import io.spine.protobuf.AnyPacker;
import io.spine.protobuf.Messages;
import io.spine.protobuf.TypeConverter;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Wrapper of an identifier value.
 */
@Internal
public final class Identifier<I> {

    /** The suffix of ID fields. */
    public static final String ID_PROPERTY_SUFFIX = "id";

    /** A {@code null} ID string representation. */
    public static final String NULL_ID = "NULL";

    /** An empty ID string representation. */
    public static final String EMPTY_ID = "EMPTY";

    private static final Pattern PATTERN_COLON_SPACE = Pattern.compile(": ");
    private static final String EQUAL_SIGN = "=";

    private final Type type;
    private final I value;

    private Identifier(Type type, I value) {
        this.value = value;
        this.type = type;
    }

    static <I> Identifier<I> from(I value) {
        checkNotNull(value);
        Type type = Type.getType(value);
        Identifier<I> result = create(type, value);
        return result;
    }

    private static Identifier<Message> fromMessage(Message value) {
        checkNotNull(value);
        Identifier<Message> result = create(Type.MESSAGE, value);
        return result;
    }

    private static <I> Identifier<I> create(Type type, I value) {
        return new Identifier<>(type, value);
    }

    /**
     * Obtains a default value for an identifier of the passed class.
     */
    public static <I> I getDefaultValue(Class<I> idClass) {
        checkNotNull(idClass);
        Type type = getType(idClass);
        I result = type.getDefaultValue(idClass);
        return result;
    }

    /**
     * Obtains the type of identifiers of the passed class.
     */
    public static <I> Type getType(Class<I> idClass) {
        for (Type type : Type.values()) {
            if (type.matchClass(idClass)) {
                return type;
            }
        }
        throw unsupportedClass(idClass);
    }

    private static <I> IllegalArgumentException unsupported(I id) {
        return newIllegalArgumentException("ID of unsupported type encountered: %s", id);
    }

    private static <I> IllegalArgumentException unsupportedClass(Class<I> idClass) {
        return newIllegalArgumentException("Unsupported ID class encountered: %s",
                                           idClass.getName());
    }

    /**
     * Ensures that the passed class of identifiers is supported.
     *
     * <p>The following types of IDs are supported:
     * <ul>
     *   <li>{@code String}
     *   <li>{@code Long}
     *   <li>{@code Integer}
     *   <li>A class implementing {@link Message}
     * </ul>
     *
     * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code,
     * and/or if you need to have IDs with some structure inside.
     *
     * <p>Examples of such structural IDs are:
     * <ul>
     *   <li>EAN value used in bar codes
     *   <li>ISBN
     *   <li>Phone number
     *   <li>Email address as a couple of local-part and domain
     * </ul>
     *
     * @param <I>     the type of the ID
     * @param idClass the class of IDs
     * @throws IllegalArgumentException if the class of IDs is not of supported type
     */
    public static <I> void checkSupported(Class<I> idClass) {
        checkNotNull(idClass);
        // Even through `getType()` can never return null, we use its return value here
        // instead of allowing ignoring just because of this one usage.
        checkNotNull(getType(idClass));
    }

    /**
     * Wraps the passed ID value into an instance of {@link Any}.
     *
     * <p>The passed value must be of one of the supported types listed below.
     * The type of the value wrapped into the returned instance is defined by the type
     * of the passed value:
     * <ul>
     *   <li>For classes implementing {@link Message} — the value of the message itself
     *   <li>For {@code String} — {@link StringValue}
     *   <li>For {@code Long} — {@link Int64Value}
     *   <li>For {@code Integer} — {@link Int32Value}
     * </ul>
     *
     * @param id  the value to wrap
     * @param <I> the type of the value
     * @return instance of {@link Any} with the passed value
     * @throws IllegalArgumentException if the passed value is not of the supported type
     */
    public static <I> Any pack(I id) {
        checkNotNull(id);
        Identifier<I> identifier = from(id);
        Any anyId = identifier.pack();
        return anyId;
    }

    /**
     * Extracts ID object from the passed {@code Any} instance.
     *
     * <p>Returned type depends on the type of the message wrapped into {@code Any}:
     * <ul>
     *   <li>{@code String} for unwrapped {@link StringValue}
     *   <li>{@code Integer} for unwrapped {@link Int32Value}
     *   <li>{@code Long} for unwrapped {@link Int64Value}
     *   <li>unwrapped {@code Message} instance if its type is none of the above
     * </ul>
     *
     * @apiNote This method assumes that the calling code knows the type of value packed into
     *          {@code Any}. The generic parameter and the cast performed by the method allows
     *          to avoid casting and suppressing {@code "unchecked"} warning at the place of the
     *          call.
     * @param any the ID value wrapped into {@code Any}
     * @return unwrapped ID
     */
    @SuppressWarnings("TypeParameterUnusedInFormals" /* See api note. */)
    public static <I> I unpack(Any any) {
        checkNotNull(any);
        Message unpacked = AnyPacker.unpack(any);

        for (Type type : Type.values()) {
            if (type.matchMessage(unpacked)) {
                // Expect the client to know the desired type.
                // If the client fails to predict it in compile time, fail fast.
                @SuppressWarnings("unchecked") I result = (I) type.fromMessage(unpacked);
                return result;
            }
        }

        throw unsupported(unpacked);
    }

    /**
     * Generates a new random UUID.
     *
     * @return the generated value
     * @see UUID#randomUUID()
     */
    public static String newUuid() {
        String id = UUID.randomUUID()
                        .toString();
        return id;
    }

    /**
     * Converts the passed ID value into the string representation.
     *
     * @param id  the value to convert
     * @param <I> the type of the ID
     * @return <ul>
     * <li>for classes implementing {@link Message} &mdash; a Json form;
     * <li>for {@code String}, {@code Long}, {@code Integer} &mdash;
     * the result of {@link Object#toString()};
     * <li>for {@code null} ID &mdash; the {@link #NULL_ID};
     * <li>if the result is empty or blank string &mdash; the {@link #EMPTY_ID}.
     * </ul>
     * @throws IllegalArgumentException if the passed type isn't one of the above or
     *                                  the passed {@link Message} instance has no fields
     * @see StringifierRegistry
     */
    public static <I> String toString(@Nullable I id) {
        if (id == null) {
            return NULL_ID;
        }

        Identifier<?> identifier;
        if (id instanceof Any) {
            Message unpacked = AnyPacker.unpack((Any) id);
            identifier = fromMessage(unpacked);
        } else {
            identifier = from(id);
        }

        String result = identifier.toString();
        return result;
    }

    @SuppressWarnings("unchecked") // OK to cast to String as output type of Stringifier.
    private static String idMessageToString(Message message) {
        checkNotNull(message);
        String result;
        StringifierRegistry registry = StringifierRegistry.getInstance();
        Class<? extends Message> msgClass = message.getClass();
        TypeToken<? extends Message> msgToken = TypeToken.of(msgClass);
        java.lang.reflect.Type msgType = msgToken.getType();
        Optional<Stringifier<Object>> optional = registry.get(msgType);
        if (optional.isPresent()) {
            Stringifier converter = optional.get();
            result = (String) converter.convert(message);
        } else {
            result = convert(message);
        }
        return result;
    }

    private static String convert(Message message) {
        Collection<Object> values = message.getAllFields()
                                           .values();
        String result;
        if (values.isEmpty()) {
            result = EMPTY_ID;
        } else if (values.size() == 1) {
            Object object = values.iterator()
                                  .next();
            result = object instanceof Message
                     ? idMessageToString((Message) object)
                     : object.toString();
        } else {
            result = messageWithMultipleFieldsToString(message);
        }
        return result;
    }

    private static String messageWithMultipleFieldsToString(MessageOrBuilder message) {
        String result = shortDebugString(message);
        result = PATTERN_COLON_SPACE.matcher(result)
                                    .replaceAll(EQUAL_SIGN);
        return result;
    }

    boolean isString() {
        return type == Type.STRING;
    }

    boolean isInteger() {
        return type == Type.INTEGER;
    }

    boolean isLong() {
        return type == Type.LONG;
    }

    boolean isMessage() {
        return type == Type.MESSAGE;
    }

    private Any pack() {
        Any result = type.pack(value);
        return result;
    }

    @Override
    public String toString() {
        String result;

        switch (type) {
            case INTEGER:
            case LONG:
                result = value.toString();
                break;

            case STRING:
                result = value.toString();
                break;

            case MESSAGE:
                result = idMessageToString((Message) value);
                break;
            default:
                throw newIllegalStateException("toString() is not supported for type: %s", type);
        }

        if (result.isEmpty()) {
            result = EMPTY_ID;
        }

        return result;
    }

    /**
     * Supported types of identifiers.
     */
    @SuppressWarnings(
            {"OverlyStrongTypeCast" /* For clarity. We cannot get OrBuilder instances here. */,
                    "unchecked" /* We ensure type by matching it first. */})
    public enum Type {
        STRING {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof String;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof StringValue;
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return String.class.equals(idClass);
            }

            @Override
            String fromMessage(Message message) {
                return ((StringValue) message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                return (I) "";
            }
        },

        INTEGER {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof Integer;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof Int32Value;
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return Integer.class.equals(idClass);
            }

            @Override
            Integer fromMessage(Message message) {
                return ((Int32Value) message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                return (I) Integer.valueOf(0);
            }
        },

        LONG {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof Long;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof Int64Value;
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return Long.class.equals(idClass);
            }

            @Override
            Long fromMessage(Message message) {
                return ((Int64Value) message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                return (I) Long.valueOf(0);
            }
        },

        MESSAGE {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof Message;
            }

            /**
             * Verifies if the passed message is not an instance of a wrapper for
             * simple types that are used for packing simple Java types into {@code Any}.
             *
             * @return {@code true} if the message is neither {@code StringValue}, nor
             *         {@code Int32Value}, nor {@code Int64Value}
             */
            @Override
            boolean matchMessage(Message message) {
                return !(message instanceof StringValue
                        || message instanceof Int32Value
                        || message instanceof Int64Value);
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return Message.class.isAssignableFrom(idClass);
            }

            @Override
            <I> Message toMessage(I id) {
                return (Message) id;
            }

            @Override
            Message fromMessage(Message message) {
                return message;
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                Class<? extends Message> msgClass = (Class<? extends Message>) idClass;
                Message result = Messages.newInstance(msgClass);
                return (I) result;
            }
        };

        private static <I> Type getType(I id) {
            for (Type type : values()) {
                if (type.matchValue(id)) {
                    return type;
                }
            }
            throw unsupported(id);
        }

        abstract <I> boolean matchValue(I id);

        abstract boolean matchMessage(Message message);

        abstract <I> boolean matchClass(Class<I> idClass);

        <I> Message toMessage(I id) {
            Message message = TypeConverter.toMessage(id);
            return message;
        }

        abstract Object fromMessage(Message message);

        abstract <I> I getDefaultValue(Class<I> idClass);

        <I> Any pack(I id) {
            Message msg = toMessage(id);
            Any result = AnyPacker.pack(msg);
            return result;
        }
    }
}
