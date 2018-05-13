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
package io.spine.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Multimap;
import org.junit.Assert;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * An extension of the {@link Assert} class, which adds useful additional "assert" methods.
 * You can import this class instead of Assert, and use it thus, e.g.:
 * <pre>
 *     Verify.assertEquals("fred", name);  // from original Assert class
 *     Verify.assertContains("fred", nameList);  // from new extensions
 *     Verify.assertBefore("fred", "jim", orderedNamesList);  // from new extensions
 * </pre>
 *
 * <p>Is based on
 * <a href="https://github.com/eclipse/eclipse-collections/blob/master/eclipse-collections-testutils/src/main/java/org/eclipse/collections/impl/test/Verify.java">
 * org.eclipse.collections.impl.test.Verify</a> class.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"ExtendsUtilityClass" /* we do this to depend on this class from tests */,
        "ClassWithTooManyMethods", "OverlyComplexClass",
        "ErrorNotRethrown", "unused"})
public final class Verify extends Assert {

    private static final String SHOULD_NOT_BE_EQUAL = "ns should not be equal:<";

    private static final String EXPECTED_ITEMS_IN_ASSERTION_MESSAGE = "Expected items in assertion";
    private static final String ITERABLE = "iterable";
    private static final String SHOULD_NOT_BE_EMPTY_MESSAGE = " should be non-empty, but was empty";

    @SuppressWarnings("AccessOfSystemProperties")
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String MSG_SHOULD_BE_EMPTY_ACTUAL_SIZE =
            " should be empty; actual size:<";
    private static final String MSG_IN_STRING_TO_SEARCH =
            "> in stringToSearch:<";
    private static final String MSG_STRING_TO_SEARCH_SHOULD_NOT_BE_NULL =
            "stringToSearch should not be null";
    private static final String MSG_HAS_INCORRECT_ELEMENT_AT_INDEX =
            " has incorrect element at index:<";
    private static final String MSG_SHOULD_NOT_CONTAIN_UNEXPECTED_ITEM =
            " should not contain unexpectedItem:<";
    private static final String MSG_SHOULD_NOT_BE_NULL = " should not be null";
    private Verify() {
        super();
    }

    /**
     * Mangles the stack trace of {@link AssertionError} so that it looks like its been thrown from
     * the line that called to a custom assertion.
     *
     * <p>This method behaves identically to {@link #mangledException(AssertionError, int)} and is
     * provided for convenience for assert methods that only want to pop two stack frames. The only
     * time that you would want to call the other {@link #mangledException(AssertionError, int)}
     * method is if you have a custom assert that calls another custom assert i.e. the source line
     * calling the custom asserts is more than two stack frames away.
     *
     * @param e the exception to mangle
     * @see #mangledException(AssertionError, int)
     */
    public static AssertionError mangledException(AssertionError e) {
        /*
         * Note that we actually remove 3 frames from the stack trace because
         * we wrap the real method doing the work: e.fillInStackTrace() will
         * include us in the exceptions stack frame.
         */
        throw mangledException(e, 3);
    }

    /**
     * Mangles the stack trace of {@link AssertionError} so that it looks like
     * its been thrown from the line that called to a custom assertion.
     *
     * <p>This is useful for when you are in a debugging session and you want to go to the source
     * of the problem in the test case quickly. The regular use case for this would be something
     * along the lines of:
     * <pre>
     * public class TestFoo extends junit.framework.TestCase
     * {
     *   public void testFoo() throws Exception
     *   {
     *     Foo foo = new Foo();
     *     ...
     *     assertFoo(foo);
     *   }
     *
     *   // Custom assert
     *   private static void assertFoo(Foo foo)
     *   {
     *     try
     *     {
     *       assertEquals(...);
     *       ...
     *       assertSame(...);
     *     }
     *     catch (AssertionFailedException e)
     *     {
     *       AssertUtils.mangledException(e, 2);
     *     }
     *   }
     * }
     * </pre>
     *
     * <p>Without the {@code try ... catch} block around lines 11-13 the stack trace following a
     * test rejection would look a little like:
     *
     * <p><pre>
     * java.lang.AssertionError: ...
     *  at TestFoo.assertFoo(TestFoo.java:11)
     *  at TestFoo.testFoo(TestFoo.java:5)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     *  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     *  at java.lang.reflect.Method.invoke(Method.java:324)
     *  ...
     * </pre>
     *
     * <p>Note that the source of the error isn't readily apparent as the first line in the stack
     * trace is the code within the custom assert. If we were debugging the rejection we would be
     * more interested in the second line of the stack trace which shows us where in our tests the
     * assert failed.
     *
     * <p>With the {@code try ... catch} block around lines 11-13 the stack trace would look like
     * the following:
     *
     * <p><pre>
     * java.lang.AssertionError: ...
     *  at TestFoo.testFoo(TestFoo.java:5)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     *  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     *  at java.lang.reflect.Method.invoke(Method.java:324)
     *  ...
     * </pre>
     * <p>
     * Here the source of the error is more visible as we can instantly see that the testFoo test is
     * failing at line 5.
     *
     * @param e           The exception to mangle.
     * @param framesToPop The number of frames to remove from the stack trace.
     * @throws AssertionError that was given as an argument with its stack trace mangled.
     */
    public static AssertionError mangledException(AssertionError e, int framesToPop) {
        e.fillInStackTrace();
        final StackTraceElement[] stackTrace = e.getStackTrace();
        final StackTraceElement[] newStackTrace =
                new StackTraceElement[stackTrace.length - framesToPop];
        System.arraycopy(stackTrace, framesToPop, newStackTrace, 0, newStackTrace.length);
        e.setStackTrace(newStackTrace);
        throw e;
    }

    public static void fail(String message, Throwable cause) {
        final AssertionError failedException = new AssertionError(message, cause);
        throw mangledException(failedException);
    }

    /**
     * Asserts that two floats are not equal concerning a delta. If the expected value is
     * {@code infinity} or {@code NaN} then the delta value is ignored.
     */
    public static void assertNotEquals(String itemName, float notExpected,
                                       float actual, float delta) {
        try {
            // handle infinity specially since subtracting to infinite values gives NaN and the
            // the following test fails
            if (areNaNs(notExpected, actual)
                    || areSameKindOfInfinity(notExpected, actual)
                    || Math.abs(notExpected - actual) <= delta) {
                fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    private static boolean areNaNs(float first, float second) {
        return Float.isNaN(first) && Float.isNaN(second);
    }

    @SuppressWarnings("FloatingPointEquality") // OK because second compared with infinity constant.
    private static boolean areSameKindOfInfinity(float first, float second) {
        return Float.isInfinite(first) && first == second;
    }

    /**
     * Asserts that two floats are not equal concerning a delta. If the expected value is
     * {@code infinity} or {@code NaN} then the delta value is ignored.
     */
    public static void assertNotEquals(float expected, float actual, float delta) {
        try {
            assertNotEquals(Param.FLOAT, expected, actual, delta);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two booleans are not equal. */
    public static void assertNotEquals(String itemName, boolean notExpected, boolean actual) {
        try {
            if (notExpected == actual) {
                fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two booleans are not equal. */
    public static void assertNotEquals(boolean notExpected, boolean actual) {
        try {
            assertNotEquals(Param.BOOLEAN, notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two bytes are not equal. */
    public static void assertNotEquals(String itemName, byte notExpected, byte actual) {
        try {
            if (notExpected == actual) {
                fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two bytes are not equal. */
    public static void assertNotEquals(byte notExpected, byte actual) {
        try {
            assertNotEquals("byte", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two chars are not equal. */
    public static void assertNotEquals(String itemName, char notExpected, char actual) {
        try {
            if (notExpected == actual) {
                fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two chars are not equal. */
    public static void assertNotEquals(char notExpected, char actual) {
        try {
            assertNotEquals("char", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two shorts are not equal. */
    public static void assertNotEquals(String itemName, short notExpected, short actual) {
        try {
            if (notExpected == actual) {
                fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that two shorts are not equal. */
    public static void assertNotEquals(short notExpected, short actual) {
        try {
            assertNotEquals("short", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Iterable} is empty. */
    public static void assertIterableEmpty(Iterable<?> iterable) {
        try {
            assertIterableEmpty(ITERABLE, iterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Iterable} is empty. */
    public static void assertIterableEmpty(String iterableName, Iterable<?> iterable) {
        try {
            assertObjectNotNull(iterableName, iterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(iterable);
            if (!fluentIterable.isEmpty()) {
                fail(iterableName + " must be empty; actual size:<" +
                             fluentIterable.size() + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given object is an instanceof expectedClassType. */
    public static void assertInstanceOf(Class<?> expectedClassType, Object actualObject) {
        try {
            assertInstanceOf(actualObject.getClass()
                                         .getName(), expectedClassType, actualObject);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given object is an instanceof expectedClassType. */
    public static void assertInstanceOf(String objectName,
                                        Class<?> expectedClassType,
                                        Object actualObject) {
        try {
            if (!expectedClassType.isInstance(actualObject)) {
                fail(objectName + " is not an instance of " + expectedClassType.getName());
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given object is not an instanceof expectedClassType. */
    public static void assertNotInstanceOf(Class<?> expectedClassType, Object actualObject) {
        try {
            assertNotInstanceOf(actualObject.getClass()
                                            .getName(), expectedClassType, actualObject);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given object is not an instanceof expectedClassType. */
    public static void assertNotInstanceOf(String objectName,
                                           Class<?> expectedClassType,
                                           Object actualObject) {
        try {
            if (expectedClassType.isInstance(actualObject)) {
                fail(objectName + " is an instance of " + expectedClassType.getName());
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} is empty. */
    public static void assertEmpty(Map<?, ?> actualMap) {
        try {
            assertEmpty(Param.MAP, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Multimap} is empty. */
    public static void assertEmpty(Multimap<?, ?> actualMultimap) {
        try {
            assertEmpty(Param.MULTIMAP, actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Multimap} is empty. */
    public static void assertEmpty(String multimapName, Multimap<?, ?> actualMultimap) {
        try {
            assertObjectNotNull(multimapName, actualMultimap);

            if (!actualMultimap.isEmpty()) {
                fail(multimapName + MSG_SHOULD_BE_EMPTY_ACTUAL_SIZE +
                             actualMultimap.size() + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} is empty. */
    @SuppressWarnings({
            "MethodWithMoreThanThreeNegations",
            "ConstantConditions" /* IDEA reports the {@code actualMap.size() != 0} as always false.
            It can be true for some flawed custom Map impl. which this method tests. */
    })
    public static void assertEmpty(String mapName, Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);

            final String errorMessage = MSG_SHOULD_BE_EMPTY_ACTUAL_SIZE;
            if (!actualMap.isEmpty()) {
                fail(mapName + errorMessage + actualMap.size() + '>');
            }

            if (actualMap.size() != 0) {
                fail(mapName + errorMessage + actualMap.size() + '>');
            }

            if (actualMap.keySet()
                         .size() != 0) {
                fail(mapName + errorMessage + actualMap.keySet()
                                                       .size() + '>');
            }

            if (actualMap.values()
                         .size() != 0) {
                fail(mapName + errorMessage + actualMap.values()
                                                       .size() + '>');
            }

            if (actualMap.entrySet()
                         .size() != 0) {
                fail(mapName + errorMessage + actualMap.entrySet()
                                                       .size() + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertEmpty(Iterator<?> iterator) {
        if (iterator.hasNext()) {
            throw new AssertionError("Iterator is not empty.");
        }
    }

    /** Assert that the given {@link Iterable} is <em>not</em> empty. */
    public static void assertNotEmpty(Iterable<?> actualIterable) {
        try {
            assertNotEmpty(ITERABLE, actualIterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Iterable} is <em>not</em> empty. */
    public static void assertNotEmpty(String iterableName, Iterable<?> actualIterable) {
        try {
            assertObjectNotNull(iterableName, actualIterable);
            final FluentIterable<?> fluentIterable = FluentIterable.from(actualIterable);
            assertFalse(iterableName + SHOULD_NOT_BE_EMPTY_MESSAGE,
                        fluentIterable.isEmpty());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Iterable} is <em>not</em> empty. */
    public static void assertIterableNotEmpty(Iterable<?> iterable) {
        try {
            assertNotEmpty(ITERABLE, iterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} is <em>not</em> empty. */
    public static void assertNotEmpty(Map<?, ?> actualMap) {
        try {
            assertNotEmpty(Param.MAP, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} is <em>not</em> empty. */
    public static void assertNotEmpty(String mapName, Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);
            assertFalse(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, actualMap.isEmpty());
            assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0,
                            actualMap.size());
            assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0,
                            actualMap.keySet()
                                     .size());
            assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0,
                            actualMap.values()
                                     .size());
            assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0,
                            actualMap.entrySet()
                                     .size());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Multimap} is <em>not</em> empty. */
    public static void assertNotEmpty(Multimap<?, ?> actualMultimap) {
        try {
            assertNotEmpty(Param.MULTIMAP, actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Multimap} is <em>not</em> empty. */
    public static void assertNotEmpty(String multimapName, Multimap<?, ?> actualMultimap) {
        try {
            assertObjectNotNull(multimapName, actualMultimap);
            assertFalse(multimapName + SHOULD_NOT_BE_EMPTY_MESSAGE,
                        actualMultimap.isEmpty());
            assertNotEquals(multimapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0,
                            actualMultimap.size());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <T> void assertNotEmpty(String itemsName, T[] items) {
        try {
            assertObjectNotNull(itemsName, items);
            assertNotEquals(itemsName, 0, items.length);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <T> void assertNotEmpty(T[] items) {
        try {
            assertNotEmpty("items", items);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertNotEmpty(Iterator<?> iterator) {
        if (!iterator.hasNext()) {
            throw new AssertionError("Iterator is empty.");
        }
    }

    /** Assert the size of the given array. */
    public static void assertSize(int expectedSize, Object[] actualArray) {
        try {
            assertSize(Param.ARRAY, expectedSize, actualArray);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given array. */
    public static void assertSize(String arrayName, int expectedSize, Object[] actualArray) {
        try {
            assertNotNull(arrayName + MSG_SHOULD_NOT_BE_NULL, actualArray);

            final int actualSize = actualArray.length;
            failOnSizeMismatch(arrayName, expectedSize, actualSize);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link Iterable}. */
    public static void assertSize(int expectedSize, Iterable<?> actualIterable) {
        try {
            assertSize(ITERABLE, expectedSize, actualIterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link Iterable}. */
    public static void assertSize(
            String iterableName,
            int expectedSize,
            Iterable<?> actualIterable) {
        try {
            assertObjectNotNull(iterableName, actualIterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(actualIterable);
            final int actualSize = fluentIterable.size();
            failOnSizeMismatch(iterableName, expectedSize, actualSize);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link Map}. */
    public static void assertSize(String mapName, int expectedSize, Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);
            assertSize(mapName, expectedSize, actualMap.keySet());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link Map}. */
    public static void assertSize(int expectedSize, Map<?, ?> actualMap) {
        try {
            assertSize(Param.MAP, expectedSize, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link Multimap}. */
    public static void assertSize(int expectedSize, Multimap<?, ?> actualMultimap) {
        try {
            assertSize(Param.MULTIMAP, expectedSize, actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link Multimap}. */
    public static void assertSize(String multimapName,
                                  int expectedSize,
                                  Multimap<?, ?> actualMultimap) {
        try {
            assertObjectNotNull(multimapName, actualMultimap);

            final int actualSize = actualMultimap.size();
            failOnSizeMismatch(multimapName, expectedSize, actualSize);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertSize(int expectedSize,
                                  Iterator<?> iterator) {
        try {
            assertObjectNotNull("Iterator", iterator);

            final int actualSize = newArrayList(iterator).size();
            failOnSizeMismatch("the Iterator", expectedSize, actualSize);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    private static void failOnSizeMismatch(String collectionName,
                                           int expectedSize,
                                           int actualSize) {
        if (actualSize != expectedSize) {
            fail("Incorrect size for "
                         + collectionName
                         + "; expected:<"
                         + expectedSize
                         + "> but was:<"
                         + actualSize
                         + '>');
        }
    }

    /** Assert the size of the given {@link com.google.common.collect.ImmutableSet ImmutableSet}. */
    public static void assertSize(int expectedSize, Collection<?> actualImmutableSet) {
        try {
            assertSize("immutable set", expectedSize, actualImmutableSet);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert the size of the given {@link com.google.common.collect.ImmutableSet ImmutableSet}. */
    public static void assertSize(String immutableSetName,
                                  int expectedSize,
                                  Collection<?> actualImmutableSet) {
        try {
            final int actualSize = actualImmutableSet.size();
            failOnSizeMismatch(immutableSetName, expectedSize, actualSize);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code stringToFind} is contained within
     * the {@code stringToSearch}.
     */
    public static void assertContains(CharSequence stringToFind, String stringToSearch) {
        try {
            assertContains(Param.STRING, stringToFind, stringToSearch);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code unexpectedString} is <em>not</em> contained within
     * the {@code stringToSearch}.
     */
    public static void assertNotContains(CharSequence unexpectedString, String stringToSearch) {
        try {
            assertNotContains(Param.STRING, unexpectedString, stringToSearch);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code stringToFind} is contained
     * within the {@code stringToSearch}.
     */
    public static void assertContains(String stringName,
                                      CharSequence stringToFind,
                                      String stringToSearch) {
        try {
            assertNotNull("stringToFind should not be null", stringToFind);
            assertNotNull(MSG_STRING_TO_SEARCH_SHOULD_NOT_BE_NULL, stringToSearch);

            if (!stringToSearch.contains(stringToFind)) {
                fail(stringName
                             + " did not contain stringToFind:<"
                             + stringToFind
                             + MSG_IN_STRING_TO_SEARCH
                             + stringToSearch
                             + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code unexpectedString} is <em>not</em> contained within
     * the {@code stringToSearch}.
     */
    public static void assertNotContains(String stringName,
                                         CharSequence unexpectedString,
                                         String stringToSearch) {
        try {
            assertNotNull("unexpectedString should not be null", unexpectedString);
            assertNotNull(MSG_STRING_TO_SEARCH_SHOULD_NOT_BE_NULL, stringToSearch);

            if (stringToSearch.contains(unexpectedString)) {
                fail(stringName
                             + " contains unexpectedString:<"
                             + unexpectedString
                             + MSG_IN_STRING_TO_SEARCH
                             + stringToSearch
                             + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Collection} contains the given item. */
    public static void assertContains(Object expectedItem, Collection<?> actualCollection) {
        try {
            assertContains(Param.COLLECTION, expectedItem, actualCollection);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Collection} contains the given item. */
    public static void assertContains(
            String collectionName,
            Object expectedItem,
            Collection<?> actualCollection) {
        assertCollectionContains(collectionName, expectedItem, actualCollection);
    }

    /** Assert that the given {@link ImmutableCollection} contains the given item. */
    public static void assertContains(Object expectedItem,
                                      ImmutableCollection<?> actualCollection) {
        try {
            assertContains("ImmutableCollection", expectedItem, actualCollection);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link ImmutableCollection} contains the given item. */
    public static void assertContains(
            String collectionName,
            Object expectedItem,
            ImmutableCollection<?> actualCollection) {
        assertCollectionContains(collectionName, expectedItem, actualCollection);
    }

    private static void assertCollectionContains(String collectionName,
                                                 Object expectedItem,
                                                 Collection<?> actualCollection) {
        try {
            assertObjectNotNull(collectionName, actualCollection);

            if (!actualCollection.contains(expectedItem)) {
                fail(collectionName + " did not contain expectedItem:<" +
                             expectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    public static <T> void assertContainsAll(
            Iterable<T> iterable,
            T... items) {
        try {
            assertObjectNotNull("Collection", iterable);

            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);
            final FluentIterable<?> fluentIterable = FluentIterable.from(iterable);

            for (Object item : items) {
                assertTrue(fluentIterable.contains(item));
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <K, V> void assertMapsEqual(Map<K, V> expectedMap,
                                              Map<K, V> actualMap,
                                              String actualMapName) {
        try {
            //noinspection ConstantConditions
            if (expectedMap == null) {
                assertNull(actualMapName + " should be null", actualMap);
                return;
            }

            assertNotNull(actualMapName + MSG_SHOULD_NOT_BE_NULL, actualMap);

            final Set<? extends Map.Entry<K, V>> expectedEntries = expectedMap.entrySet();
            for (Map.Entry<K, V> expectedEntry : expectedEntries) {
                final K expectedKey = expectedEntry.getKey();
                final V expectedValue = expectedEntry.getValue();
                final V actualValue = actualMap.get(expectedKey);
                if (!Objects.equals(actualValue, expectedValue)) {
                    fail("Values differ at key " + expectedKey + " expected " +
                                 expectedValue + " but was " + actualValue);
                }
            }
            assertSetsEqual(expectedMap.keySet(), actualMap.keySet());
            assertSetsEqual(expectedMap.entrySet(), actualMap.entrySet());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <T> void assertSetsEqual(@Nullable Collection<T> expectedSet,
                                           @Nullable Collection<T> actualSet) {
        try {
            if (expectedSet == null) {
                assertNull("Actual set should be null", actualSet);
                return;
            }

            //noinspection ConstantConditions
            assertObjectNotNull("actual set", actualSet);
            assertSize(expectedSet.size(), actualSet);

            if (!actualSet.equals(expectedSet)) {
                final Set<T> inExpectedOnlySet = newHashSet(expectedSet);
                inExpectedOnlySet.removeAll(actualSet);

                final int numberDifferences = inExpectedOnlySet.size();

                final int maxDifferences = 5;
                if (numberDifferences > maxDifferences) {
                    fail("Actual set: " + numberDifferences + " elements different.");
                }

                fail("Sets are not equal.");
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Multimap} contains an entry with the given key and value. */
    public static <K, V> void assertContainsEntry(
            K expectedKey,
            V expectedValue,
            Multimap<K, V> actualMultimap) {
        try {
            assertContainsEntry(Param.MULTIMAP, expectedKey, expectedValue, actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Multimap} contains an entry with the given key and value. */
    public static <K, V> void assertContainsEntry(
            String multimapName,
            K expectedKey,
            V expectedValue,
            Multimap<K, V> actualMultimap) {
        try {
            assertNotNull(multimapName, actualMultimap);

            if (!actualMultimap.containsEntry(expectedKey, expectedValue)) {
                fail(multimapName + " did not contain entry: <" + expectedKey + ", "
                             + expectedValue + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} contains an entry with the given key. */
    public static void assertContainsKey(Object expectedKey, Map<?, ?> actualMap) {
        try {
            assertContainsKey(Param.MAP, expectedKey, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} contains an entry with the given key. */
    public static void assertContainsKey(String mapName, Object expectedKey, Map<?, ?> actualMap) {
        try {
            assertNotNull(mapName, actualMap);

            if (!actualMap.containsKey(expectedKey)) {
                fail(mapName + " did not contain expectedKey:<" + expectedKey + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Deny that the given {@link Map} contains an entry with the given key. */
    public static void denyContainsKey(Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            denyContainsKey(Param.MAP, unexpectedKey, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Deny that the given {@link Map} contains an entry with the given key. */
    public static void denyContainsKey(String mapName, Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            assertNotNull(mapName, actualMap);

            if (actualMap.containsKey(unexpectedKey)) {
                fail(mapName + " contained unexpectedKey:<" + unexpectedKey + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} contains an entry with the given key and value. */
    public static void assertContainsKeyValue(
            Object expectedKey,
            Object expectedValue,
            Map<?, ?> actualMap) {
        try {
            assertContainsKeyValue(Param.MAP, expectedKey, expectedValue, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Map} contains an entry with the given key and value. */
    public static void assertContainsKeyValue(
            String mapName,
            Object expectedKey,
            Object expectedValue,
            Map<?, ?> actualMap) {
        try {
            assertContainsKey(mapName, expectedKey, actualMap);

            final Object actualValue = actualMap.get(expectedKey);
            if (!Objects.equals(actualValue, expectedValue)) {
                fail(
                        mapName
                                + " entry with expectedKey:<"
                                + expectedKey
                                + '>'
                                + " did not contain expectedValue:<"
                                + expectedValue
                                + ">, "
                                + "but had actualValue:<"
                                + actualValue
                                + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Collection} does <em>not</em> contain the given item. */
    public static void assertNotContains(Object unexpectedItem, Collection<?> actualCollection) {
        try {
            assertNotContains(Param.COLLECTION, unexpectedItem, actualCollection);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Collection} does <em>not</em> contain the given item. */
    public static void assertNotContains(
            String collectionName,
            Object unexpectedItem,
            Collection<?> actualCollection) {
        try {
            assertObjectNotNull(collectionName, actualCollection);

            if (actualCollection.contains(unexpectedItem)) {
                fail(collectionName + MSG_SHOULD_NOT_CONTAIN_UNEXPECTED_ITEM +
                             unexpectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Iterable} does <em>not</em> contain the given item. */
    public static void assertNotContains(Object unexpectedItem, Iterable<?> iterable) {
        try {
            assertNotContains(ITERABLE, unexpectedItem, iterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Iterable} does <em>not</em> contain the given item. */
    public static void assertNotContains(
            String collectionName,
            Object unexpectedItem,
            Iterable<?> iterable) {
        try {
            assertObjectNotNull(collectionName, iterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(iterable);
            if (fluentIterable.contains(unexpectedItem)) {
                fail(collectionName + MSG_SHOULD_NOT_CONTAIN_UNEXPECTED_ITEM +
                             unexpectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Collection} does <em>not</em> contain the given item. */
    public static void assertNotContainsKey(Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            assertNotContainsKey(Param.MAP, unexpectedKey, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@link Collection} does <em>not</em> contain the given item. */
    public static void assertNotContainsKey(String mapName,
                                            Object unexpectedKey,
                                            Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);

            if (actualMap.containsKey(unexpectedKey)) {
                fail(mapName + MSG_SHOULD_NOT_CONTAIN_UNEXPECTED_ITEM + unexpectedKey + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the formerItem appears before the latterItem in the given {@link Collection}.
     * Both the formerItem and the latterItem must appear in the collection, or this assert will
     * fail.
     */
    public static <T> void assertBefore(T formerItem, T latterItem, List<T> actualList) {
        try {
            assertBefore(Param.LIST, formerItem, latterItem, actualList);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the formerItem appears before the latterItem in the given {@link Collection}.
     * {@link #assertContains(String, Object, Collection)} will be called for both the formerItem
     * and the latterItem, prior to the "before" assertion.
     */
    public static <T> void assertBefore(
            String listName,
            T formerItem,
            T latterItem,
            List<T> actualList) {
        try {
            assertObjectNotNull(listName, actualList);
            assertNotEquals(
                    "Bad test, formerItem and latterItem are equal, listName:<" +
                            listName + '>',
                    formerItem,
                    latterItem);
            assertContainsAll(actualList, formerItem, latterItem);
            final int formerPosition = actualList.indexOf(formerItem);
            final int latterPosition = actualList.indexOf(latterItem);
            if (latterPosition < formerPosition) {
                fail("Items in " + listName + " are in incorrect order; " +
                             "expected formerItem:<" + formerItem + '>' +
                             " to appear before latterItem:<" + latterItem + ">, but didn't");
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertObjectNotNull(String objectName, Object actualObject) {
        try {
            assertNotNull(objectName + MSG_SHOULD_NOT_BE_NULL, actualObject);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@code item} is at the {@code index} in the given {@link List}. */
    public static void assertItemAtIndex(Object expectedItem, int index, List<?> list) {
        try {
            assertItemAtIndex(Param.LIST, expectedItem, index, list);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@code item} is at the {@code index} in the given {@code array}. */
    public static void assertItemAtIndex(Object expectedItem, int index, Object[] array) {
        try {
            assertItemAtIndex(Param.ARRAY, expectedItem, index, array);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertStartsWith(T[] array, T... items) {
        try {
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex(Param.ARRAY, item, i, array);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @SafeVarargs
    public static <T> void assertStartsWith(List<T> list, T... items) {
        try {
            assertStartsWith(Param.LIST, list, items);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertStartsWith(String listName, List<T> list, T... items) {
        try {
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex(listName, item, i, list);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertEndsWith(List<T> list, T... items) {
        try {
            assertObjectNotNull(Param.LIST, list);
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex(Param.LIST, item, list.size() - items.length + i, list);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertEndsWith(T[] array, T... items) {
        try {
            assertObjectNotNull(Param.ARRAY, array);
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex(Param.ARRAY, item, array.length - items.length + i, array);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@code item} is at the {@code index} in the given {@link List}. */
    public static void assertItemAtIndex(
            String listName,
            Object expectedItem,
            int index,
            List<?> list) {
        try {
            assertObjectNotNull(listName, list);

            final Object actualItem = list.get(index);
            if (!Objects.equals(expectedItem, actualItem)) {
                assertEquals(
                        listName + MSG_HAS_INCORRECT_ELEMENT_AT_INDEX + index + '>',
                        expectedItem,
                        actualItem);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Assert that the given {@code item} is at the {@code index} in the given {@link List}. */
    public static void assertItemAtIndex(
            String arrayName,
            Object expectedItem,
            int index,
            Object[] array) {
        try {
            assertNotNull(array);
            final Object actualItem = array[index];
            if (!Objects.equals(expectedItem, actualItem)) {
                assertEquals(
                        arrayName + MSG_HAS_INCORRECT_ELEMENT_AT_INDEX + index + '>',
                        expectedItem,
                        actualItem);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that {@code objectA} and {@code objectB} are equal
     * (via the {@link Object#equals(Object)} method, and that they both return
     * the same {@link Object#hashCode()}.
     */
    public static void assertEqualsAndHashCode(Object objectA, Object objectB) {
        try {
            assertEqualsAndHashCode("objects", objectA, objectB);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that a value is negative. */
    public static void assertNegative(int value) {
        try {
            assertTrue(value + " is not negative", value < 0);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that a value is positive. */
    public static void assertPositive(int value) {
        try {
            assertTrue(value + " is not positive", value > 0);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /** Asserts that a value is positive. */
    public static void assertZero(int value) {
        try {
            assertEquals(0, value);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that {@code objectA} and {@code objectB} are equal
     * (via the {@link Object#equals(Object)} method, and that they both return
     * the same {@link Object#hashCode()}.
     */
    public static void assertEqualsAndHashCode(String itemNames, Object objectA, Object objectB) {
        try {
            //noinspection ConstantConditions
            if (objectA == null || objectB == null) {
                fail("Neither item should be null: <" + objectA + "> <" + objectB + '>');
            }

            final String errNotEqualNewObject = "Neither item should equal new Object()";
            assertNotEquals(errNotEqualNewObject, objectA.equals(new Object()));
            assertNotEquals(errNotEqualNewObject, objectB.equals(new Object()));

            final String expectedEqual = String.format("Expected %s to be equal.", itemNames);

            assertEquals(expectedEqual, objectA, objectA);
            assertEquals(expectedEqual, objectB, objectB);
            assertEquals(expectedEqual, objectA, objectB);
            assertEquals(expectedEqual, objectB, objectA);

            final String expectedSameHashCode =
                    String.format("Expected %s to have the same hashCode().", itemNames);
            assertEquals(expectedSameHashCode, objectA.hashCode(), objectB.hashCode());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertShallowClone(Cloneable object) {
        try {
            assertShallowClone("object", object);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertShallowClone(String itemName, Cloneable object) {
        try {
            //noinspection ConfusingArgumentToVarargsMethod
            final Method method = Object.class.getDeclaredMethod("clone", (Class<?>[]) null);
            method.setAccessible(true);
            final Object clone = method.invoke(object);
            final String prefix = itemName + " and its clone";
            assertNotSame(prefix, object, clone);
            assertEqualsAndHashCode(prefix, object, clone);
        } catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException |
                IllegalAccessException | AssertionError | SecurityException e) {
            throw new AssertionError(e.getLocalizedMessage(), e);
        }
    }

    public static <T> void assertClassNonInstantiable(Class<T> aClass) {
        try {
            try {
                //noinspection ClassNewInstance
                aClass.newInstance();
                fail("Expected class '" + aClass + "' to be non-instantiable");
            } catch (InstantiationException e) {
                // pass
            } catch (IllegalAccessException ignored) {
                if (canInstantiateThroughReflection(aClass)) {
                    fail("Expected constructor of non-instantiable class '" + aClass +
                                 "' to throw an exception, but didn't");
                }
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    private static <T> boolean canInstantiateThroughReflection(Class<T> aClass) {
        try {
            final Constructor<T> declaredConstructor = aClass.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            declaredConstructor.newInstance();
            return true;
        } catch (NoSuchMethodException |
                InvocationTargetException |
                InstantiationException |
                IllegalAccessException |
                AssertionError ignored) {
            return false;
        }
    }

    public static void assertError(Class<? extends Error> expectedErrorClass, Runnable code) {
        try {
            code.run();
        } catch (Error ex) {
            try {
                assertSame(
                        "Caught error of type <"
                                + ex.getClass()
                                    .getName()
                                + ">, expected one error of type <"
                                + expectedErrorClass.getName()
                                + '>',
                        expectedErrorClass,
                        ex.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            fail("Block did not throw an error of type " + expectedErrorClass.getName());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Runs the {@link Callable} {@code code} and asserts that it throws an {@code Exception} of
     * the type {@code exceptionClass}.
     * <p>
     * {@code Callable} is most appropriate when a checked exception will be thrown.
     * If a subclass of {@link RuntimeException} will be thrown, the form
     * {@link #assertThrows(Class, Runnable)} may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.<b>assertThrows</b>(StringIndexOutOfBoundsException.class,
     *                      new Callable&lt;String&gt;()
     * {
     *    public String call() throws Exception
     *    {
     *        return "Craig".substring(42, 3);
     *    }
     * });
     * </pre>
     *
     * @see #assertThrows(Class, Runnable)
     */
    public static void assertThrows(
            Class<? extends Exception> exceptionClass,
            Callable<?> code) {
        try {
            code.call();
        } catch (Exception ex) {
            try {

                assertSame(msgCaughtButExpectedWithMessage(ex, exceptionClass),
                           exceptionClass,
                           ex.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            failDidNotThrow(exceptionClass);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    private static String msgCaughtButExpectedWithMessage(Exception actual,
                                                          Class<? extends Exception> expected) {
        return msgCaughtButExpected(actual, expected)
                + LINE_SEPARATOR
                + "Exception Message: " + actual.getMessage()
                + LINE_SEPARATOR;
    }

    /**
     * Runs the {@link Runnable} {@code code} and asserts that it throws an {@code Exception} of
     * the type {@code expectedClass}.
     * <p>
     * {@code Runnable} is most appropriate when a subclass of {@link RuntimeException}
     * will be thrown.
     * If a checked exception will be thrown, the form {@link #assertThrows(Class, Callable)}
     * may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.<b>assertThrows</b>(NullPointerException.class, new Runnable()
     * {
     *    public void run()
     *    {
     *        final Integer integer = null;
     *        LOGGER.info(integer.toString());
     *    }
     * });
     * </pre>
     *
     * @see #assertThrows(Class, Callable)
     */
    public static void assertThrows(
            Class<? extends Exception> expectedClass,
            Runnable code) {
        try {
            code.run();
        } catch (RuntimeException ex) {
            try {
                assertSame(msgCaughtButExpectedWithMessage(ex, expectedClass),
                           expectedClass,
                           ex.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            failDidNotThrow(expectedClass);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Runs the {@link Callable} {@code code} and asserts that it throws an {@code Exception} of
     * the type {@code exceptionClass}, which contains a cause of type expectedCauseClass.
     * <p>
     * {@code Callable} is most appropriate when a checked exception will be thrown.
     * If a subclass of {@link RuntimeException} will be thrown, the form
     * {@link #assertThrowsWithCause(Class, Class, Runnable)} may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.assertThrowsWithCause(RuntimeException.class, IOException.class,
     *                              new Callable&lt;Void&gt;() {
     *      public Void call() throws Exception {
     *           try {
     *               new File("").createNewFile();
     *           }
     *           catch (final IOException e) {
     *               throw new RuntimeException("Uh oh!", e);
     *           }
     *           return null;
     *      }
     *  });
     * </pre>
     *
     * @see #assertThrowsWithCause(Class, Class, Runnable)
     */
    public static void assertThrowsWithCause(
            Class<? extends Exception> exceptionClass,
            Class<? extends Throwable> expectedCauseClass,
            Callable<?> code) {
        try {
            code.call();
        } catch (Exception ex) {
            try {
                assertExceptionWithCause(ex, exceptionClass, expectedCauseClass);
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            failDidNotThrow(exceptionClass);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    private static void failDidNotThrow(Class<? extends Exception> exceptionClass) {
        fail(msgBlockDidNotThrow(exceptionClass));
    }

    /**
     * Runs the {@link Runnable} {@code code} and asserts that it throws an {@code Exception} of
     * the type {@code exceptionClass}, which contains a cause of type expectedCauseClass.
     * <p>
     * {@code Runnable} is most appropriate when a subclass of {@link RuntimeException}
     * will be thrown.
     * If a checked exception will be thrown, the form
     * {@link #assertThrowsWithCause(Class, Class, Callable)} may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.assertThrowsWithCause(RuntimeException.class,
     * StringIndexOutOfBoundsException.class, new Runnable()
     * {
     *    public void run()
     *    {
     *        try
     *        {
     *            LOGGER.info("Craig".substring(42, 3));
     *        }
     *        catch (final StringIndexOutOfBoundsException e)
     *        {
     *            throw new RuntimeException("Uh oh!", e);
     *        }
     *    }
     * });
     * </pre>
     *
     * @see #assertThrowsWithCause(Class, Class, Callable)
     */
    public static void assertThrowsWithCause(
            Class<? extends Exception> exceptionClass,
            Class<? extends Throwable> expectedCauseClass,
            Runnable code) {
        try {
            code.run();
        } catch (RuntimeException ex) {
            try {
                assertExceptionWithCause(ex, exceptionClass, expectedCauseClass);
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            failDidNotThrow(exceptionClass);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <E> void assertIteratorsEqual(Iterator<? extends E> first,
                                                Iterator<? extends E> second) {
        final Collection<? extends E> firstCollection = newArrayList(first);
        final Collection<? extends E> secondCollection = newArrayList(second);
        assertEquals("Sizes are not equal: ", firstCollection.size(), secondCollection.size());
        for (E el : firstCollection) {
            assertContains(el, secondCollection);
        }
    }

    private static String msgBlockDidNotThrow(Class<? extends Exception> exceptionClass) {
        return "Block did not throw an exception of type " + exceptionClass.getName();
    }

    private static void assertExceptionWithCause(Exception actual,
                                                 Class<? extends Exception> expectedClass,
                                                 Class<? extends Throwable> expectedCauseClass) {
        assertSame(
                msgCaughtButExpected(actual, expectedClass),
                expectedClass,
                actual.getClass());
        final Throwable actualCauseClass = actual.getCause();
        assertNotNull(
                "Caught exception with null cause, expected cause of type <"
                        + expectedCauseClass.getName()
                        + '>',
                actualCauseClass);
        assertSame(
                "Caught exception with cause of type<"
                        + actualCauseClass.getClass()
                                          .getName()
                        + ">, expected cause of type <"
                        + expectedCauseClass.getName()
                        + '>',
                expectedCauseClass,
                actualCauseClass.getClass());
    }

    private static String msgCaughtButExpected(Exception actual,
                                               Class<? extends Exception> expectedClass) {
        return "Caught exception of type <" + actual.getClass()
                                                    .getName() +
                ">, expected one of type <" + expectedClass.getName() + '>';
    }

    /** Parameter name constants. */
    @VisibleForTesting
    static class Param {

        static final String MAP = simpleLowerCaseOf(Map.class);
        private static final String MULTIMAP = simpleLowerCaseOf(Multimap.class);
        private static final String COLLECTION = simpleLowerCaseOf(Collection.class);
        private static final String ARRAY = simpleLowerCaseOf(Array.class);
        private static final String STRING = simpleLowerCaseOf(String.class);
        private static final String LIST = simpleLowerCaseOf(List.class);
        private static final String FLOAT = simpleLowerCaseOf(float.class);
        private static final String BOOLEAN = simpleLowerCaseOf(boolean.class);

        /** Prevents instantiation of this utility class. */
        private Param() {
        }

        private static String simpleLowerCaseOf(Class<?> cls) {
            return cls.getSimpleName()
                      .toLowerCase();
        }
    }
}