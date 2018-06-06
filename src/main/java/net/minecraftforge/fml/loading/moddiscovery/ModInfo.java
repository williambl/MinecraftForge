/*
 * Minecraft Forge
 * Copyright (c) 2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.loading.moddiscovery;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.fml.FMLEnvironment;
import net.minecraftforge.fml.StringSubstitutor;
import net.minecraftforge.fml.StringUtils;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.loading.IModLanguageProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModInfo {
    private static final DefaultArtifactVersion DEFAULT_VERSION = new DefaultArtifactVersion("1");
    public static final VersionRange UNBOUNDED = VersionRange.createFromVersionSpec("");
    private final ModFile.ModFileInfo owningFile;
    private final String modId;
    private final ArtifactVersion version;
    private final String displayName;
    private final String description;
    private final List<ModInfo.ModVersion> dependencies;
    private final Map<String,Object> properties;
    private final UnmodifiableConfig modConfig;

    public ModInfo(final ModFile.ModFileInfo owningFile, final UnmodifiableConfig modConfig)
    {
        this.owningFile = owningFile;
        this.modConfig = modConfig;
        this.modId = modConfig.<String>getOptional("modId").orElseThrow(() -> new InvalidModFileException("Missing modId entry", owningFile));
        this.version = modConfig.<String>getOptional("version").
                map(s->StringSubstitutor.replace(s, owningFile != null ? owningFile.getFile() : null )).
                map(DefaultArtifactVersion::new).orElse(DEFAULT_VERSION);
        this.displayName = modConfig.<String>getOptional("displayName").orElse(null);
        this.description = modConfig.get("description");
        if (owningFile != null) {
            this.dependencies = owningFile.getConfig().<List<UnmodifiableConfig>>getOptional(Arrays.asList("dependencies", this.modId)).
                    orElse(Collections.emptyList()).stream().map(dep -> new ModVersion(this, dep)).collect(Collectors.toList());
            this.properties = owningFile.getConfig().<UnmodifiableConfig>getOptional(Arrays.asList("modproperties", this.modId)).
                    map(UnmodifiableConfig::valueMap).orElse(Collections.emptyMap());
        } else {
            this.dependencies = Collections.emptyList();
            this.properties = Collections.emptyMap();
        }

    }

    public ModFile getOwningFile() {
        return owningFile.getFile();
    }

    public String getModId() {
        return modId;
    }

    public ArtifactVersion getVersion() {
        return version;
    }

    public List<ModInfo.ModVersion> getDependencies() {
        return this.dependencies;
    }

    public enum Ordering {
        BEFORE, AFTER, NONE
    }

    public enum DependencySide {
        CLIENT, SERVER, BOTH;

        public boolean isCorrectSide()
        {
            return this == BOTH || FMLEnvironment.side.name().equals(this.name());
        }
    }

    public static class ModVersion {
        private ModInfo owner;
        private final String modId;
        private final VersionRange versionRange;
        private final boolean mandatory;
        private final Ordering ordering;
        private final DependencySide side;

        ModVersion(final ModInfo owner, final UnmodifiableConfig config) {
            this.owner = owner;
            this.modId = config.get("modId");
            this.versionRange = config.getOptional("versionRange").map(String.class::cast).
                    map(VersionRange::createFromVersionSpec).orElse(UNBOUNDED);
            this.mandatory = config.get("mandatory");
            this.ordering = config.getOptional("ordering").map(String.class::cast).
                    map(Ordering::valueOf).orElse(Ordering.NONE);
            this.side = config.getOptional("side").map(String.class::cast).
                    map(DependencySide::valueOf).orElse(DependencySide.BOTH);
        }


        public String getModId()
        {
            return modId;
        }

        public VersionRange getVersionRange()
        {
            return versionRange;
        }

        public boolean isMandatory()
        {
            return mandatory;
        }

        public Ordering getOrdering()
        {
            return ordering;
        }

        public DependencySide getSide()
        {
            return side;
        }

        public void setOwner(final ModInfo owner)
        {
            this.owner = owner;
        }
    }
}