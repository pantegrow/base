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

package io.spine.tools.compiler.validation;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import io.spine.tools.compiler.MessageTypeCache;
import io.spine.tools.compiler.fieldtype.FieldType;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An abstract base for the method constructor builders.
 *
 * @author Illia Shepilov
 */
abstract class AbstractMethodConstructorBuilder<T extends MethodConstructor> {

    private int fieldIndex;
    private String javaClass;
    private String javaPackage;
    private ClassName genericClassName;
    private MessageTypeCache typeCache;
    private FieldDescriptorProto field;
    private FieldType fieldType;

    /**
     * Builds a method constructor for the specified field.
     *
     * <p>Implementations must {@linkplain #checkFields() validate the state} of the builder
     * before producing the result.
     *
     * @return built method constructor
     */
    abstract T build();

    AbstractMethodConstructorBuilder setFieldIndex(int fieldIndex) {
        checkArgument(fieldIndex >= 0);
        this.fieldIndex = fieldIndex;
        return this;
    }

    AbstractMethodConstructorBuilder setJavaPackage(String javaPackage) {
        checkNotNull(javaPackage);
        this.javaPackage = javaPackage;
        return this;
    }

    AbstractMethodConstructorBuilder setJavaClass(String javaClass) {
        checkNotNull(javaClass);
        this.javaClass = javaClass;
        return this;
    }

    AbstractMethodConstructorBuilder setTypeCache(MessageTypeCache typeCache) {
        checkNotNull(typeCache);
        this.typeCache = typeCache;
        return this;
    }

    AbstractMethodConstructorBuilder setField(FieldDescriptorProto field) {
        checkNotNull(field);
        this.field = field;
        return this;
    }

    AbstractMethodConstructorBuilder setBuilderGenericClassName(ClassName genericClassName) {
        checkNotNull(genericClassName);
        this.genericClassName = genericClassName;
        return this;
    }

    AbstractMethodConstructorBuilder setFieldType(FieldType fieldType) {
        checkNotNull(fieldType);
        this.fieldType = fieldType;
        return this;
    }

    int getFieldIndex() {
        return fieldIndex;
    }

    @Nullable
    String getJavaClass() {
        return javaClass;
    }

    @Nullable
    String getJavaPackage() {
        return javaPackage;
    }

    @Nullable
    ClassName getGenericClassName() {
        return genericClassName;
    }

    @Nullable
    MessageTypeCache getTypeCache() {
        return typeCache;
    }

    @Nullable
    FieldDescriptorProto getField() {
        return field;
    }

    @Nullable
    FieldType getFieldType() {
        return fieldType;
    }

    /**
     * Checks the builder fields.
     */
    final void checkFields() {
        checkNotNull(javaClass);
        checkNotNull(javaPackage);
        checkNotNull(typeCache);
        checkNotNull(field);
        checkNotNull(genericClassName);
        checkNotNull(fieldType);
        checkState(fieldIndex >= 0);
    }
}
