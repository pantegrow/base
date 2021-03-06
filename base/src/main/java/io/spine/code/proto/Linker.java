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

package io.spine.code.proto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.FileDescriptor.buildFrom;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.stream.Collectors.toList;

/**
 * Builds a set of {@link FileDescriptor}s from a list of {@link FileDescriptorProto}.
 *
 * @author Alexander Yevsyukov
 */
class Linker {

    private static final FileDescriptor[] NO_DEPENDENCIES = {};

    private final List<FileDescriptorProto> input;

    private final List<FileDescriptorProto> remaining;

    private final FileSet resolved;
    private final FileSet partiallyResolved;
    private final FileSet unresolved;

    Linker(Iterable<FileDescriptorProto> input) {
        this.input = ImmutableList.copyOf(input);
        this.remaining = Lists.newArrayList(input);
        this.resolved = FileSet.newInstance();
        this.partiallyResolved = FileSet.newInstance();
        this.unresolved = FileSet.newInstance();
    }

    static FileSet link(Collection<FileDescriptorProto> files) {
        Linker linker = new Linker(files);
        try {
            linker.resolve();
        } catch (DescriptorValidationException e) {
            throw newIllegalStateException(e, "Unable to link descriptor set files");
        }
        FileSet result = linker.getResolved()
                               .union(linker.getPartiallyResolved())
                               .union(linker.getUnresolved());
        return result;
    }

    void resolve() throws DescriptorValidationException {
        // Make sure this method is called only after the constructor once.
        checkState(input.size() == remaining.size());
        findNoDependencies();
        findResolved();
        findPartiallyResolved();
        addUnresolved();
    }

    private void findNoDependencies() throws DescriptorValidationException {
        Iterator<FileDescriptorProto> iterator = remaining.iterator();
        while (iterator.hasNext()) {
            FileDescriptorProto next = iterator.next();
            if (next.getDependencyCount() == 0) {
                FileDescriptor fd = buildFrom(next, NO_DEPENDENCIES, true);
                resolved.add(fd);
                iterator.remove();
            }
        }
    }

    /**
     * Iterates over the remaining files while the list is not empty,
     * or no more resolved files found.
     */
    private void findResolved() throws DescriptorValidationException {
        boolean resolvedFound = true;
        while (!remaining.isEmpty() && resolvedFound) {
            resolvedFound = doFindResolved();
        }
    }

    private boolean doFindResolved() throws DescriptorValidationException {
        boolean result = false;
        Iterator<FileDescriptorProto> iterator = remaining.iterator();
        while (iterator.hasNext()) {
            FileDescriptorProto next = iterator.next();
            Collection<FileName> dependencyList = dependencies(next);
            if (resolved.containsAll(dependencyList)) {
                FileSet dependencies = resolved.find(dependencyList);
                FileDescriptor newResolved = buildFrom(next, dependencies.toArray(), true);
                resolved.add(newResolved);
                result = true;
                iterator.remove();
            }
        }
        return result;
    }

    private void findPartiallyResolved() throws DescriptorValidationException {
        boolean partiallyResolvedFound = true;
        while (!remaining.isEmpty() && partiallyResolvedFound) {
            partiallyResolvedFound = doFindPartiallyResolved();
        }
    }

    private boolean doFindPartiallyResolved() throws DescriptorValidationException {
        boolean result = false;
        FileSet partialAndResolved = resolved.union(partiallyResolved);
        Iterator<FileDescriptorProto> iterator = remaining.iterator();
        while (iterator.hasNext()) {
            FileDescriptorProto next = iterator.next();
            Collection<FileName> dependencyList = dependencies(next);
            FileSet dependencies = partialAndResolved.find(dependencyList);
            if (dependencies.isEmpty()) {
                FileDescriptor newPartial = buildFrom(next, dependencies.toArray(), true);
                partiallyResolved.add(newPartial);
                partialAndResolved.add(newPartial);
                result = true;
            }
            iterator.remove();
        }
        return result;
    }

    /**
     * Adds unresolved descriptors.
     *
     * <p>Even though unresolved by now descriptors can be resolvable to each other isolation,
     * we would not be able to use that information for code generation. That's why this method
     * simply adds the remaining files as unresolvable without attempting to resolve them within
     * the group.
     */
    private void addUnresolved() throws DescriptorValidationException {
        while (!remaining.isEmpty()) {
            FileDescriptorProto first = remaining.get(0);
            FileDescriptor fd = buildFrom(first, NO_DEPENDENCIES, true);
            unresolved.add(fd);
            remaining.remove(first);
        }
    }

    private static Collection<FileName> dependencies(FileDescriptorProto file) {
        return file.getDependencyList()
                   .stream()
                   .map(FileName::of)
                   .collect(toList());
    }

    @VisibleForTesting
    List<FileDescriptorProto> getRemaining() {
        return ImmutableList.copyOf(remaining);
    }

    FileSet getResolved() {
        return resolved;
    }

    FileSet getPartiallyResolved() {
        return partiallyResolved;
    }

    FileSet getUnresolved() {
        return unresolved;
    }
}
