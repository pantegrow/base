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
package io.spine.tools.compiler.enrichment;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import io.spine.logging.Logging;
import io.spine.type.TypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Finds event enrichment definitions for messages defined in a single proto file.
 *
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
class EnrichmentFinder implements Logging {

    private final FileDescriptorProto file;

    /**
     * Creates a new instance.
     *
     * @param file a file to search enrichments in
     */
    EnrichmentFinder(FileDescriptorProto file) {
        this.file = file;
    }

    /**
     * Finds event enrichment definitions in the file.
     *
     * @return a map from enrichment type name to event to enrich type name
     */
    Map<String, String> findAll() {
        Logger log = log();
        log.debug("Looking up for the enrichments in {}", file.getName());

        List<DescriptorProto> messages = file.getMessageTypeList();
        String packagePrefix = file.getPackage() + TypeName.PACKAGE_SEPARATOR;
        EnrichmentMap map = new EnrichmentMap(packagePrefix);
        Map<String, String> result = map.allOf(messages);

        log.debug("Found {} enrichments", result.size());
        return result;
    }
}
