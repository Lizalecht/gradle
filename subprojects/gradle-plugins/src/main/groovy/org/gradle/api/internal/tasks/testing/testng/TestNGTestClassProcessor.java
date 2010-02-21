/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.testing.testng;

import org.gradle.api.GradleException;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.api.testing.TestClassProcessor;
import org.gradle.api.testing.fabric.TestClassRunInfo;
import org.gradle.util.GFileUtils;
import org.gradle.util.GUtil;
import org.gradle.util.IdGenerator;
import org.testng.TestNG;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestNGTestClassProcessor implements TestClassProcessor {
    private final List<Class<?>> testClasses = new ArrayList<Class<?>>();
    private final File testReportDir;
    private final TestNGOptions options;
    private final List<File> suiteFiles;
    private final IdGenerator<?> idGenerator;
    private TestNGListenerAdapter listener;
    private ClassLoader applicationClassLoader;

    public TestNGTestClassProcessor(File testReportDir, TestNGOptions options, List<File> suiteFiles, IdGenerator<?> idGenerator) {
        this.testReportDir = testReportDir;
        this.options = options;
        this.suiteFiles = suiteFiles;
        this.idGenerator = idGenerator;
    }

    public void startProcessing(TestResultProcessor resultProcessor) {
        listener = new TestNGListenerAdapter(resultProcessor, idGenerator);
        applicationClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public void processTestClass(TestClassRunInfo testClass) {
        try {
            testClasses.add(applicationClassLoader.loadClass(testClass.getTestClassName()));
        } catch (ClassNotFoundException e) {
            throw new GradleException(String.format("Could not load test class '%s'.", testClass.getTestClassName()));
        }
    }

    public void endProcessing() {
        TestNG testNg = new TestNG();
        testNg.setOutputDirectory(testReportDir.getAbsolutePath());
        testNg.setDefaultSuiteName(options.getSuiteName());
        testNg.setDefaultTestName(options.getTestName());
        testNg.setAnnotations(options.getAnnotations());
        if (options.getJavadocAnnotations()) {
            testNg.setSourcePath(GUtil.join(options.getTestResources(), File.pathSeparator));
        }
        testNg.setUseDefaultListeners(options.getUseDefaultListeners());
        testNg.addListener(listener);
        testNg.setVerbose(0);

        if (!suiteFiles.isEmpty()) {
            testNg.setTestSuites(GFileUtils.toPaths(suiteFiles));
        } else {
            Class[] classes = testClasses.toArray(new Class[testClasses.size()]);
            testNg.setTestClasses(classes);
        }

        testNg.run();
    }
}
