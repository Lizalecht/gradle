/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.nativecode.language.cpp

import org.gradle.nativecode.language.cpp.fixtures.AbstractBinariesIntegrationSpec
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

// TODO:DAZ Use a convention plugin here that includes C sources
class CLanguageIntegrationTest extends AbstractBinariesIntegrationSpec {

    static final HELLO_WORLD = "Hello, World!"
    static final HELLO_WORLD_FRENCH = "Bonjour, Monde!"

    def main_c = """
            #include "hello.h"

            int main () {
                hello();
                return 0;
            }
"""
    def hello_c = """
            #include <stdio.h>
            #ifdef _WIN32
            #define DLL_FUNC __declspec(dllexport)
            #else
            #define DLL_FUNC
            #endif

            void DLL_FUNC hello() {
                #ifdef FRENCH
                printf("${HELLO_WORLD_FRENCH}");
                #else
                printf("${HELLO_WORLD}");
                #endif
            }
"""
    def hello_h = """
            void hello();

"""

    def "build fails when compilation fails"() {
        given:
        buildFile << """
            apply plugin: "cpp"
            sources {
                main {}
            }
            executables {
                main {
                    source sources.main
                }
            }
        """

        and:
        file("src", "main", "c", "helloworld.c") << """
            #include <stdio.h>

            'broken
        """

        expect:
        fails "mainExecutable"
        failure.assertHasDescription("Execution failed for task ':compileMainExecutableMainC'.");
        failure.assertHasCause("C compile failed; see the error output for details.")
    }

    def "compile and link executable"() {
        given:
        buildFile << """
            apply plugin: "cpp"
            sources {
                main {}
            }
            executables {
                main {
                    source sources.main
                }
            }
        """

        and:
        file("src", "main", "c", "main.c") << main_c
        file("src", "main", "c", "hello.c") << hello_c
        file("src", "main", "headers", "hello.h") << hello_h

        when:
        run "mainExecutable"

        then:
        def mainExecutable = executable("build/binaries/mainExecutable/main")
        mainExecutable.assertExists()
        mainExecutable.exec().out == HELLO_WORLD
    }

    def "build executable with custom compiler arg"() {
        given:
        buildFile << """
            apply plugin: "cpp"
            sources {
                main {}
            }
            executables {
                main {
                    source sources.main
                    binaries.all {
                        compilerArgs "-DFRENCH"
                    }
                }
            }
        """

        and:
        file("src", "main", "c", "main.c") << main_c
        file("src", "main", "c", "hello.c") << hello_c
        file("src", "main", "headers", "hello.h") << hello_h

        when:
        run "mainExecutable"

        then:
        def mainExecutable = executable("build/binaries/mainExecutable/main")
        mainExecutable.assertExists()
        mainExecutable.exec().out == HELLO_WORLD_FRENCH
    }

    def "build executable with macro defined"() {
        given:
        buildFile << """
            apply plugin: "cpp"
            sources {
                main {}
            }
            executables {
                main {
                    source sources.main
                    binaries.all {
                        define "FRENCH"
                    }
                }
            }
        """

        and:
        file("src", "main", "c", "main.c") << main_c
        file("src", "main", "c", "hello.c") << hello_c
        file("src", "main", "headers", "hello.h") << hello_h

        when:
        run "mainExecutable"

        then:
        def mainExecutable = executable("build/binaries/mainExecutable/main")
        mainExecutable.assertExists()
        mainExecutable.exec().out == HELLO_WORLD_FRENCH
    }

    @Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
    def "build shared library and link into executable"() {
        given:
        buildFile << """
            apply plugin: "cpp"

            sources {
                main {}
                hello {}
            }
            executables {
                main {
                    source sources.main
                }
            }
            libraries {
                hello {
                    source sources.hello
                }
            }
            sources.main.c.lib libraries.hello
        """

        and:
        file("src", "main", "c", "helloworld.c") << main_c
        file("src", "hello", "headers", "hello.h") << hello_h
        file("src", "hello", "c", "hello.c") << hello_c

        when:
        run "installMainExecutable"

        then:
        sharedLibrary("build/binaries/helloSharedLibrary/hello").assertExists()
        executable("build/binaries/mainExecutable/main").assertExists()

        def install = installation("build/install/mainExecutable")
        install.assertInstalled()
        install.assertIncludesLibraries("hello")
        install.exec().out == HELLO_WORLD
    }

    @Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
    def "build static library and link into executable"() {
        given:
        buildFile << """
            apply plugin: "cpp"

            sources {
                main {}
                hello {}
            }
            executables {
                main {
                    source sources.main
                }
            }
            libraries {
                hello {
                    source sources.hello
                    binaries.withType(StaticLibraryBinary) {
                        define "FRENCH"
                    }
                }
            }
            sources.main.c.lib libraries.hello.static
        """

        and:
        file("src", "main", "c", "helloworld.c") << main_c
        file("src", "hello", "headers", "hello.h") << hello_h
        file("src", "hello", "c", "hello.c") << hello_c

        when:
        run "installMainExecutable"

        then:
        staticLibrary("build/binaries/helloStaticLibrary/hello").assertExists()
        executable("build/binaries/mainExecutable/main").assertExists()

        and:
        def install = installation("build/install/mainExecutable")
        install.assertInstalled()
        install.exec().out == HELLO_WORLD_FRENCH
    }
}
