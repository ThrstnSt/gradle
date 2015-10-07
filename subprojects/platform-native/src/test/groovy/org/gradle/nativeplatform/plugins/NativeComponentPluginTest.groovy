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

package org.gradle.nativeplatform.plugins

import org.gradle.api.tasks.TaskDependencyMatchers
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.type.ModelType
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.tasks.CreateStaticLibrary
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.tasks.LinkExecutable
import org.gradle.nativeplatform.tasks.LinkSharedLibrary
import org.gradle.platform.base.BinaryContainer
import org.gradle.util.TestUtil
import spock.lang.Specification

class NativeComponentPluginTest extends Specification {
    final def project = TestUtil.createRootProject()

    def setup() {
        project.pluginManager.apply(NativeComponentPlugin)
    }

    BinaryContainer realizeBinaries() {
        project.modelRegistry.find(ModelPath.path("binaries"), ModelType.of(BinaryContainer))
    }

    def "creates link and install task for executable"() {
        when:
        project.model {
            components {
                test(NativeExecutableSpec)
            }
        }

        project.tasks.realize()
        def binaries = realizeBinaries()
        project.bindAllModelRules()

        then:
        def testExecutable = binaries.testExecutable
        with(project.tasks.linkTestExecutable) {
            it instanceof LinkExecutable
            it == testExecutable.tasks.link
            it.toolChain == testExecutable.toolChain
            it.targetPlatform == testExecutable.targetPlatform
            it.linkerArgs == testExecutable.linker.args
        }

        and:
        def lifecycleTask = project.tasks.testExecutable
        lifecycleTask TaskDependencyMatchers.dependsOn("linkTestExecutable")

        and:
        project.tasks.installTestExecutable instanceof InstallExecutable
    }

    def "creates link task and static archive task for library"() {
        when:
        project.model {
            components {
                test(NativeLibrarySpec)
            }
        }

        project.tasks.realize()
        def binaries = realizeBinaries()
        project.bindAllModelRules()

        then:
        def sharedLibraryBinary = binaries.testSharedLibrary
        with(project.tasks.linkTestSharedLibrary) {
            it instanceof LinkSharedLibrary
            it == sharedLibraryBinary.tasks.link
            it.toolChain == sharedLibraryBinary.toolChain
            it.targetPlatform == sharedLibraryBinary.targetPlatform
            it.linkerArgs == sharedLibraryBinary.linker.args
        }

        and:
        def sharedLibTask = project.tasks.testSharedLibrary
        sharedLibTask TaskDependencyMatchers.dependsOn("linkTestSharedLibrary")

        and:
        def staticLibraryBinary = binaries.testStaticLibrary
        with(project.tasks.createTestStaticLibrary) {
            it instanceof CreateStaticLibrary
            it == staticLibraryBinary.tasks.createStaticLib
            it.toolChain == staticLibraryBinary.toolChain
            it.targetPlatform == staticLibraryBinary.targetPlatform
            it.staticLibArgs == staticLibraryBinary.staticLibArchiver.args
        }

        and:
        def staticLibTask = project.tasks.testStaticLibrary
        staticLibTask TaskDependencyMatchers.dependsOn("createTestStaticLibrary")
    }
}
