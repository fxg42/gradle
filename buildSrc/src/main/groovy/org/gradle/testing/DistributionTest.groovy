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

package org.gradle.testing

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem

/**
 * Base class for all tests that check the end-to-end behavior of a Gradle distribution.
 */
class DistributionTest extends Test {
    @Input
    String getOperatingSystem() {
        OperatingSystem.current().toString()
    }

    @Input
    Map<String, Object> getPlainSystemProperties() {
        super.getSystemProperties() - fileSystemProperties.collectEntries { key, value -> [(key): value.absolutePath]}
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File gradleHomeDir

    void setGradleHomeDir(File gradleHomeDir) {
        this.gradleHomeDir = gradleHomeDir
        fileSystemProperty('integTest.gradleHomeDir', gradleHomeDir)
    }

    @Internal
    File gradleUserHomeDir

    void setGradleUserHomeDir(File gradleUserHomeDir) {
        this.gradleUserHomeDir = gradleUserHomeDir
        fileSystemProperty('integTest.gradleUserHomeDir', gradleUserHomeDir)
    }

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File libsRepo

    void setLibsRepo(File libsRepo) {
        this.libsRepo = libsRepo
        fileSystemProperty('integTest.libsRepo', libsRepo)
    }

    @Input
    boolean requiresLibsRepo

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File toolingApiShadedJarDir

    void setToolingApiShadedJarDir(File toolingApiShadedJarDir) {
        this.toolingApiShadedJarDir = toolingApiShadedJarDir
        fileSystemProperty('integTest.toolingApiShadedJarDir', toolingApiShadedJarDir)
    }

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File distsDir

    @Input
    boolean requiresDists

    void setDistsDir(File distsDir) {
        this.distsDir = distsDir
        fileSystemProperty('integTest.distsDir', distsDir)
    }

    /** The user home dir is not wiped out by clean
     *  Move the daemon working space underneath the build dir so they don't pile up on CI
     */
    @Internal
    File daemonRegistry

    void setDaemonRegistry(File daemonRegistry) {
        this.daemonRegistry = daemonRegistry
        fileSystemProperty('org.gradle.integtest.daemon.registry', daemonRegistry)
    }

    private Map<String, File> fileSystemProperties = [:]

    void fileSystemProperty(String key, File value) {
        super.systemProperty(key, value.absolutePath)
        fileSystemProperties[key] = value
    }

    void fileSystemProperties(Map<String, File> files) {
        files.each { key, value ->
            fileSystemProperty(key, value)
        }
    }
}
