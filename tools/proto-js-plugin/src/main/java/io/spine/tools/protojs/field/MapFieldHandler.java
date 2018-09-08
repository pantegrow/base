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

package io.spine.tools.protojs.field;

import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.tools.protojs.code.JsGenerator;
import io.spine.tools.protojs.field.checker.FieldValueChecker;
import io.spine.tools.protojs.field.parser.FieldValueParser;

import static io.spine.tools.protojs.field.Fields.capitalizedName;
import static io.spine.tools.protojs.message.MessageHandler.MESSAGE;

public class MapFieldHandler extends AbstractFieldHandler {

    private static final String ATTRIBUTE = "attribute";
    private static final String MAP_KEY = "mapKey";

    private final FieldValueParser keyParser;

    // todo create builder for ctors with arg count > 3.
    MapFieldHandler(FieldDescriptor field,
                    FieldValueChecker valueChecker,
                    FieldValueParser keyParser,
                    FieldValueParser valueParser,
                    JsGenerator jsGenerator) {
        super(field, valueChecker, valueParser, jsGenerator);
        this.keyParser = keyParser;
    }

    // todo try string format instead of concatenation everywhere
    // todo check js object for null
    @Override
    public void generateJs() {
        String jsObject = acquireJsObject();
        String value = iterateOwnAttributes(jsObject);
        parseMapKey();
        setValue(value);
        exitOwnAttributeIteration();
    }

    @Override
    String setterFormat() {
        String fieldName = capitalizedName(field());
        String getMapCall = "get" + fieldName + "Map()";
        String setMapValueCall = "set(" + MAP_KEY + ", %s)";
        String addToMapFormat = MESSAGE + '.' + getMapCall + '.' + setMapValueCall + ';';
        return addToMapFormat;
    }

    private void parseMapKey() {
        keyParser.parseIntoVariable(ATTRIBUTE, MAP_KEY);
    }

    private String iterateOwnAttributes(String jsObject) {
        jsWriter().enterIfBlock(jsObject + " !== undefined && " + jsObject + " !== null");
        jsWriter().enterBlock("for (let " + ATTRIBUTE + " in " + jsObject + ')');
        jsWriter().enterIfBlock(jsObject + ".hasOwnProperty(" + ATTRIBUTE + ')');
        String value = jsObject + '[' + ATTRIBUTE + ']';
        return value;
    }

    private void exitOwnAttributeIteration() {
        jsWriter().exitBlock();
        jsWriter().exitBlock();
        jsWriter().exitBlock();
    }
}
