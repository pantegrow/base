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

package io.spine.validate.rules;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.spine.option.ValidationTargetParser;
import io.spine.util.PropertyFiles;

import java.util.Collection;
import java.util.Properties;

/**
 * Utilities for obtaining {@linkplain ValidationRule validation rules} known to the application.
 *
 * <p>During initialization of this class, definitions of validation rules are
 * {@linkplain Builder#put(Properties) validated}. If an invalid validation rule was found,
 * a runtime exception will be thrown.
 *
 * @author Dmytro Grankin
 */
public class ValidationRules {

    /**
     * A name of the file, which contains validation rules and their target field paths.
     */
    private static final String VAL_RULES_PROPS_FILE_NAME = "validation_rules.properties";

    private static final ImmutableCollection<ValidationRule> rules = new Builder().build();

    private ValidationRules() {
        // Prevent instantiation of this class.
    }

    /**
     * Obtains validation rules known to the application.
     *
     * @return the immutable collection of validation rules
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // It's OK to return
                                                        // an immutable collection.
    static ImmutableCollection<ValidationRule> getRules() {
        return rules;
    }

    public static String getValRulesPropsFileName() {
        return VAL_RULES_PROPS_FILE_NAME;
    }

    /**
     * {@code Builder} assembles the validation rules from the
     * {@linkplain #VAL_RULES_PROPS_FILE_NAME properties} files.
     *
     * <p>Obtains the files, using {@link PropertyFiles#loadAllProperties(String) PropertyFiles}.
     */
    private static class Builder {

        /**
         * Properties to process.
         *
         * <p>Properties must contain the list of validation rules and the targets for the rules.
         */
        private final Iterable<Properties> properties;

        /**
         * The validation rules collection to be assembled.
         */
        private final ImmutableCollection.Builder<ValidationRule> rules;

        private Builder() {
            this.properties = PropertyFiles.loadAllProperties(VAL_RULES_PROPS_FILE_NAME);
            this.rules = ImmutableSet.builder();
        }

        private ImmutableCollection<ValidationRule> build() {
            for (Properties props : this.properties) {
                put(props);
            }
            return rules.build();
        }

        /**
         * Puts the validation rules obtained from the specified properties to the collection.
         *
         * @param properties the properties to process
         * @throws IllegalStateException if an entry from the properties contains invalid data
         */
        private void put(Properties properties) {
            final ValidationTargetParser targetParser = ValidationTargetParser.getInstance();
            for (String validationRuleType : properties.stringPropertyNames()) {
                final String ruleTargetPaths = properties.getProperty(validationRuleType);
                final Collection<String> parsedPaths = targetParser.parse(ruleTargetPaths);
                final ValidationRule rule = new ValidationRule(validationRuleType, parsedPaths);
                rules.add(rule);
            }
        }
    }
}
