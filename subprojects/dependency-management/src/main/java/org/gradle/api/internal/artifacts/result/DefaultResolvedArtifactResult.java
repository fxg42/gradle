/*
 * Copyright 2014 the original author or authors.
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
package org.gradle.api.internal.artifacts.result;

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.component.Artifact;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;

import java.io.File;

public class DefaultResolvedArtifactResult implements ResolvedArtifactResult {
    private final ComponentArtifactIdentifier identifier;
    private final Class<? extends Artifact> type;
    private final File file;
    private final String format;

    public DefaultResolvedArtifactResult(ComponentArtifactIdentifier identifier,  Class<? extends Artifact> type, String format, File file) {
        this.identifier = identifier;
        this.type = type;
        this.format = format;
        this.file = file;
    }

    @Override
    public ComponentArtifactIdentifier getId() {
        return identifier;
    }

    public Class<? extends Artifact> getType() {
        return type;
    }

    @Override
    public String getFormat() {
        return format;
    }

    public File getFile() {
        return file;
    }
}
