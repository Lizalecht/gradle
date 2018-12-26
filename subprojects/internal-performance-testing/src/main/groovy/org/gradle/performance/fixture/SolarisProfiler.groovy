/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.performance.fixture

class SolarisProfiler extends JfrProfiler {
    SolarisProfiler(File targetDir) {
        super(targetDir)
    }

    List<String> getAdditionalJvmOpts(BuildExperimentSpec spec) {
        []
    }

    void start(BuildExperimentSpec spec) {
        if (useDaemon(spec)) {
            Process process = ["collect", "-P", pid.pid, "-d", getJfrFile(spec), "-o", "profile.er"].execute()
            process.waitForProcessOutput(System.out as Appendable, System.err as Appendable)
        }
    }

    void stop(BuildExperimentSpec spec) {
        if (useDaemon(spec)) {
            ["kill", '-SIGINT', pid.pid].execute().waitForProcessOutput(System.out as Appendable, System.err as Appendable)
        }
    }

    void gc(BuildExperimentSpec spec) {
    }
}