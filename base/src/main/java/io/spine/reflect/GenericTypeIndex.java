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

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.reflect.Types.getArgument;

/**
 * Base interface for enumerations on generic parameters of types.
 *
 * <p>Example of implementing an enumeration for generic parameters:
 * <pre>
 * {@code
 * public abstract class Tuple<K, V> {
 *      ...
 *     public enum GenericParameter extends GenericTypeIndex<Tuple> {
 *
 *         // <K> param has index 0
 *         KEY(0),
 *
 *         // <V> param has index 1
 *         VALUE(1);
 *
 *         private final int index;
 *
 *         GenericParameter(int index) { this.index = index; }
 *
 *         {@literal @}Override
 *         public int getIndex() { return index; }
 *
 *         {@literal @}Override
 *         public Class<?> getArgumentIn(Class<? extends Tuple> derivedClass) {
 *             return Default.getArgument(this, derivedClass);
 *         }
 *     }
 * }
 * }
 * </pre>
 * @param <C> the type for which class the generic index is declared
 * @author Alexander Yevsyukov
 */
public interface GenericTypeIndex<C> {

    /**
     * Obtains a zero-based index of a generic parameter of a type.
     */
    int getIndex();

    /**
     * Obtains the class of the generic type argument.
     *
     * @param cls the class to inspect
     * @return the argument class
     */
    default Class<?> getArgumentIn(Class<? extends C> cls) {
        checkNotNull(cls);
        Class<? extends GenericTypeIndex> indexClass = getClass();
        // Obtain the super class of the passed one by inspecting the class which implements
        // `GenericTypeIndex`.
        // The type cast is ensured by the declaration of the `GenericTypeIndex` interface.
        @SuppressWarnings("unchecked") Class<C> superclassOfPassed =
                (Class<C>) getArgument(indexClass, GenericTypeIndex.class, 0);
        Class<?> result = getArgument(cls, superclassOfPassed, getIndex());
        return result;
    }
}
