/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.integtests.resolve.transform

import org.gradle.integtests.fixtures.AbstractDependencyResolutionTest

public class FauxAndroidCompilationIntegrationTest extends AbstractDependencyResolutionTest {

    def setup() {
        settingsFile << """
    rootProject.name = 'fake-android-build'
    include 'java-lib'
    include 'android-lib'
    include 'android-app'
"""
        buildFile << """
import java.nio.file.Files
import java.nio.file.Paths
import org.gradle.api.artifacts.transform.*

    project(':java-lib') {
        apply plugin: 'java'
    }
    project(':android-lib') {
        apply plugin: 'base'

        configurations {
            compile
        }
        configurations['default'].extendsFrom(configurations['compile'])

        task aar(type: Zip) {
            from file('aar-image')
            destinationDir = project.buildDir
            extension = 'aar'
        }

        artifacts {
            compile aar
        }
    }
    project(':android-app') {
        apply plugin: 'java'

        repositories {
            maven { url '${mavenRepo.uri}' }
        }

        configurations.compile.resolutionStrategy {
            // Extract the manifest and classes.jar from an Aar.
            registerTransform('aar', AarExtractor)  {
                outputDirectory = project.file("transformed")
                antBuilder = project.ant
            }
            // Jar is a classpath element in it's own right
            registerTransform('jar', JarClasspathTransform) {}
        }

        def compileClasspath = configurations.compile.withType('classpath')
        def compileManifests = configurations.compile.withType('android-manifest')

        task fakeCompile {
            dependsOn compileClasspath
            doLast {
                compileClasspath.each { println it.absolutePath - rootDir }
            }
        }

        task printManifests {
            dependsOn compileManifests
            doLast {
                compileManifests.each { println it.absolutePath - rootDir }
                if (compileManifests.empty) {
                    println 'no manifest found'
                }
            }
        }
    }

    class AarExtractor extends DependencyTransform {
        def antBuilder
        private File explodedAar

        @TransformOutput(type = 'classpath')
        File getClassesJar() {
            new File(explodedAar, "classes.jar")
        }

        @TransformOutput(type = 'android-manifest')
        File getManifest() {
            new File(explodedAar, "AndroidManifest.xml")
        }

        void transform(File input) {
            assert input.name.endsWith('.aar')

            explodedAar = new File(outputDirectory, input.name)
            if (!explodedAar.exists()) {
                antBuilder.unzip(src:  input,
                                 dest: explodedAar,
                                 overwrite: "false")
            }
        }
    }

    class JarClasspathTransform extends DependencyTransform {
        private File output

        @TransformOutput(type = 'classpath')
        File getOutput() {
            output
        }

        void transform(File input) {
            output = input
        }
    }

"""

        def aarImage = file('android-lib/aar-image')
        aarImage.file('AndroidManifest.xml') << "<AndroidManifest/>"
        file('android-lib/classes/foo.txt') << "something"
        file('android-lib/classes/bar/baz.txt') << "something"
        file('android-lib/classes/bar/baz.txt') << "something"
        file('android-lib/classes').zipTo(aarImage.file('classes.jar'))

        def module = mavenRepo.module("org.gradle", "ext-android-lib").hasType('aar').publish()
        module.artifactFile.delete()
        aarImage.zipTo(module.artifactFile)

        file('android-app').mkdirs()

        mavenRepo.module("org.gradle", "ext-java-lib").publish()
    }

    def "compile classpath directly references jars from local java libraries"() {
        when:
        dependency "project(':java-lib')"

        then:
        classpath '/java-lib/build/libs/java-lib.jar'
    }

    def "local java libraries can expose classes directory directly as classpath artifact"() {
        when:
        dependency "project(':java-lib')"

        and:
        buildFile << """
    project(':java-lib') {
        artifacts {
            runtime(compileJava.destinationDir) {
                type 'classpath'
            }
        }
    }
"""

        then:
        classpath '/java-lib/build/classes/main'
    }

    def "compile classpath includes classes dir from local android libraries"() {
        when:
        dependency "project(':android-lib')"

        then:
        classpath '/transformed/android-lib.aar/classes.jar'
    }

    def "local android library can expose classes directory directly as classpath artifact"() {
        when:
        dependency "project(':android-lib')"

        and:
        buildFile << """
    project(':android-lib') {
        artifacts {
            compile(file('classes')) {
                type 'classpath'
            }
        }
    }
"""

        then:
        classpath '/android-lib/classes'
    }

    def "compile classpath includes jars from published java modules"() {
        when:
        dependency "'org.gradle:ext-java-lib:1.0'"

        then:
        classpath '/maven-repo/org/gradle/ext-java-lib/1.0/ext-java-lib-1.0.jar'
    }

    def "compile classpath includes classes jar from published android modules"() {
        when:
        dependency "'org.gradle:ext-android-lib:1.0'"

        then:
        classpath '/transformed/ext-android-lib-1.0.aar/classes.jar'
    }

    def "compile dependencies include a combination of aars and jars"() {
        when:
        dependency "project(':java-lib')"
        dependency "project(':android-lib')"
        dependency "'org.gradle:ext-java-lib:1.0'"
        dependency "'org.gradle:ext-android-lib:1.0'"

        then:
        classpath '/java-lib/build/libs/java-lib.jar',
            '/transformed/android-lib.aar/classes.jar',
            '/maven-repo/org/gradle/ext-java-lib/1.0/ext-java-lib-1.0.jar',
            '/transformed/ext-android-lib-1.0.aar/classes.jar'
    }

    def "no manifest for local java library or published java module"() {
        when:
        dependency "project(':java-lib')"
        dependency "'org.gradle:ext-java-lib:1.0'"

        then:
        manifest()
    }

    def "manifest returned for local android library"() {
        when:
        dependency "project(':android-lib')"

        then:
        manifest '/transformed/android-lib.aar/AndroidManifest.xml'
    }

    def "manifest returned for published android module"() {
        when:
        dependency "'org.gradle:ext-android-lib:1.0'"

        then:
        manifest '/transformed/ext-android-lib-1.0.aar/AndroidManifest.xml'
    }

    def "manifests returned for a combination of aars and jars"() {
        when:
        dependency "project(':java-lib')"
        dependency "project(':android-lib')"
        dependency "'org.gradle:ext-java-lib:1.0'"
        dependency "'org.gradle:ext-android-lib:1.0'"

        then:
        manifest '/transformed/android-lib.aar/AndroidManifest.xml',
            '/transformed/ext-android-lib-1.0.aar/AndroidManifest.xml'
    }

    def dependency(String notation) {
        buildFile << """
    project(':android-app') {
        dependencies {
            compile ${notation}
        }
    }
"""
    }

    void classpath(String... classpathElements) {
        assert succeeds('fakeCompile')

        for (String classpathElement : classpathElements) {
            outputContains(classpathElement)
        }
    }

    void manifest(String... manifests) {
        assert succeeds('printManifests')

        if (manifests.length == 0) {
            outputContains('no manifest found')
        }

        for (String manifest : manifests) {
            outputContains(manifest)
        }
    }

}
