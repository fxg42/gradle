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

package org.gradle.integtests.resolve

/**
 * Variant of the configuration attributes resolution integration test which makes use of the strongly typed attributes notation.
 */
class StronglyTypedConfigurationAttributesResolveIntegrationTest extends AbstractConfigurationAttributesResolveIntegrationTest {
    @Override
    String getTypeDefs() {
        '''
            @groovy.transform.Canonical
            class Flavor {
                static Flavor of(String value) { return new Flavor(value:value) }
                String value
                String toString() { value }
            }
            enum BuildType {
                debug,
                release
            }

            def flavor = Attribute.of(Flavor)
            def buildType = Attribute.of(BuildType)

        '''
    }

    @Override
    String getFreeDebug() {
        '(buildType): BuildType.debug, (flavor): Flavor.of("free")'
    }

    @Override
    String getFreeRelease() {
        '(buildType): BuildType.release, (flavor): Flavor.of("free")'
    }

    @Override
    String getDebug() {
        '(buildType): BuildType.debug'
    }

    @Override
    String getFree() {
        '(flavor): Flavor.of("free")'
    }

    // This documents the current behavior, not necessarily the one we would want. Maybe
    // we will prefer failing with an error indicating incompatible types
    def "fails if two configurations use the same attribute name with different types"() {
        given:
        file('settings.gradle') << "include 'a', 'b'"
        buildFile << """
            $typeDefs

            project(':a') {
                configurations {
                    _compileFreeDebug.attributes($freeDebug)
                    _compileFreeRelease.attributes($freeRelease)
                }
                dependencies {
                    _compileFreeDebug project(':b')
                    _compileFreeRelease project(':b')
                }
                task checkDebug(dependsOn: configurations._compileFreeDebug) {
                    doLast {
                       assert configurations._compileFreeDebug.collect { it.name } == ['b-default.jar']
                    }
                }
                task checkRelease(dependsOn: configurations._compileFreeRelease) {
                    doLast {
                       assert configurations._compileFreeRelease.collect { it.name } == ['b-default.jar']
                    }
                }
            }
            project(':b') {
                configurations {
                    create('default')
                    foo {
                        attributes(flavor: 'free', buildType: 'debug') // use String type instead of Flavor/BuildType
                    }
                    bar {
                        attributes(flavor: 'free', buildType: 'release') // use String type instead of Flavor/BuildType
                    }
                }
                task fooJar(type: Jar) {
                   baseName = 'b-foo'
                }
                task barJar(type: Jar) {
                   baseName = 'b-bar'
                }
                artifacts {
                    'default' file('b-default.jar')
                    foo fooJar
                    bar barJar
                }
            }

        """

        when:
        run ':a:checkDebug'

        then:
        notExecuted ':b:fooJar', ':b:barJar'

        when:
        run ':a:checkRelease'

        then:
        notExecuted ':b:fooJar', ':b:barJar'
    }
}
