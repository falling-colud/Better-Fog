package net.cloud.better_fog.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(BiomeSpecialEffects.class)
public abstract class BiomeEffectsMixin {


    @Shadow @Final private int waterFogColor;

    @Shadow @Final private int skyColor;

    @Shadow @Final private Optional<Integer> foliageColorOverride;

    @Shadow @Final private Optional<Integer> grassColorOverride;

    @Shadow @Final private BiomeSpecialEffects.GrassColorModifier grassColorModifier;

    @Shadow @Final private Optional<AmbientParticleSettings> ambientParticleSettings;

    @Shadow @Final private int fogColor;

    @Shadow @Final private int waterColor;

    @Shadow @Final private Optional<Holder<SoundEvent>> ambientLoopSoundEvent;

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public int getFogColor() {
        return this.fogColor;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public int getWaterColor() {
        return this.waterColor;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public int getWaterFogColor() {
        return this.waterFogColor;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public int getSkyColor() {
        return this.skyColor;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public Optional<Integer> getFoliageColorOverride() {
        return this.foliageColorOverride;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public Optional<Integer> getGrassColorOverride() {
        return this.grassColorOverride;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public BiomeSpecialEffects.GrassColorModifier getGrassColorModifier() {
        return this.grassColorModifier;
    }

    /**
     * @author Cloud
     * @reason gotta change the color
     */
    @Overwrite
    public Optional<AmbientParticleSettings> getAmbientParticleSettings() {
        return this.ambientParticleSettings;

    }

}