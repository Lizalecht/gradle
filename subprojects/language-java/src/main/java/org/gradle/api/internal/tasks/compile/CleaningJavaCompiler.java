/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.tasks.compile;

import org.gradle.api.internal.TaskOutputsInternal;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.file.Deleter;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.language.base.internal.tasks.SimpleStaleClassCleaner;
import org.gradle.language.base.internal.tasks.StaleClassCleaner;

import javax.annotation.Nullable;
import java.io.File;

/**
 * Deletes stale classes before invoking the actual compiler.
 */
public class CleaningJavaCompiler<T extends JavaCompileSpec> implements Compiler<T> {
    private final Compiler<T> compiler;
    private final TaskOutputsInternal taskOutputs;
    private final Deleter deleter;

    public CleaningJavaCompiler(Compiler<T> compiler, TaskOutputsInternal taskOutputs, Deleter deleter) {
        this.compiler = compiler;
        this.taskOutputs = taskOutputs;
        this.deleter = deleter;
    }

    @Override
    public WorkResult execute(T spec) {
        StaleClassCleaner cleaner = new SimpleStaleClassCleaner(deleter, taskOutputs);

        addDirectory(cleaner, spec.getDestinationDir());
        MinimalJavaCompileOptions compileOptions = spec.getCompileOptions();
        addDirectory(cleaner, compileOptions.getAnnotationProcessorGeneratedSourcesDirectory());
        addDirectory(cleaner, compileOptions.getHeaderOutputDirectory());
        cleaner.execute();

        Compiler<? super T> compiler = getCompiler();
        return compiler.execute(spec);
    }

    private void addDirectory(@Nullable StaleClassCleaner cleaner, File dir) {
        if (dir != null) {
            cleaner.addDirToClean(dir);
        }
    }

    public Compiler<T> getCompiler() {
        return compiler;
    }
}
