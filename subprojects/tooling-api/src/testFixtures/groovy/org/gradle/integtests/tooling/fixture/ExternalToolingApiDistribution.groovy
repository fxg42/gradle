/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.integtests.tooling.fixture

import org.gradle.api.internal.classpath.DefaultModuleRegistry
import org.gradle.api.internal.classpath.Module
import org.gradle.integtests.fixtures.executer.GradleDistribution
import org.gradle.internal.classloader.VisitableURLClassLoader
import org.gradle.internal.classpath.ClassPath
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.internal.installation.GradleInstallation
import org.gradle.util.GradleVersion

class ExternalToolingApiDistribution implements ToolingApiDistribution {
    private final GradleVersion version
    private final GradleDistribution distribution

    ExternalToolingApiDistribution(String version, GradleDistribution distribution) {
        this.version = GradleVersion.version(version)
        this.distribution = distribution
    }

    GradleVersion getVersion() {
        version
    }

    Collection<File> getClasspath() {
        getDistributionClassPath().getAsFiles()
    }

    private ClassPath getDistributionClassPath() {
        ClassPath classpath = new DefaultClassPath();
        DefaultModuleRegistry registry = new DefaultModuleRegistry(new GradleInstallation(distribution.gradleHomeDir));
        for (Module module : registry.getModule("gradle-tooling-api").getAllRequiredModules()) {
            classpath = classpath.plus(module.getClasspath());
        }
        return classpath;
    }

    ClassLoader createClassLoader(ClassLoader parent) {
        return new VisitableURLClassLoader(parent, getDistributionClassPath())
    }

    String toString() {
        "Tooling API $version.version"
    }
}
