package net.cloud.better_fog.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @Shadow private static long biomeChangedTime;

    @Shadow private static int targetBiomeFog;

    @Shadow private static int previousBiomeFog;

    @Shadow private static float fogRed;

    @Shadow private static float fogGreen;

    @Shadow private static float fogBlue;

    @Shadow @Nullable
    protected abstract FogRenderer.MobEffectFogFunction getPriorityFogFunction(Entity arg, float f);

    /**
     * @author Cloud
     * @reason gotta change the fog color
     */
    @Overwrite
    public static void setupColor(Camera camera, float f, ClientLevel clientLevel, int i, float g) {
        FogType fogType = camera.getFluidInCamera();
        Entity entity = camera.getEntity();
        float h;
        float r;
        float s;
        float u;
        float v;
        float w;
        if (fogType == FogType.WATER) {
            long l = Util.getMillis();
            int j = ((Biome)clientLevel.getBiome(BlockPos.containing(camera.getPosition())).value()).getWaterFogColor();
            if (biomeChangedTime < 0L) {
                targetBiomeFog = j;
                previousBiomeFog = j;
                biomeChangedTime = l;
            }

            int k = targetBiomeFog >> 16 & 255;
            int m = targetBiomeFog >> 8 & 255;
            int n = targetBiomeFog & 255;
            int o = previousBiomeFog >> 16 & 255;
            int p = previousBiomeFog >> 8 & 255;
            int q = previousBiomeFog & 255;
            h = Mth.clamp((float)(l - biomeChangedTime) / 5000.0F, 0.0F, 1.0F);
            r = Mth.lerp(h, (float)o, (float)k);
            s = Mth.lerp(h, (float)p, (float)m);
            float t = Mth.lerp(h, (float)q, (float)n);
            fogRed = r / 255.0F;
            fogGreen = s / 255.0F;
            fogBlue = t / 255.0F;
            if (targetBiomeFog != j) {
                targetBiomeFog = j;
                previousBiomeFog = Mth.floor(r) << 16 | Mth.floor(s) << 8 | Mth.floor(t);
                biomeChangedTime = l;
            }
        } else if (fogType == FogType.LAVA) {
            fogRed = 0.6F;
            fogGreen = 0.1F;
            fogBlue = 0.0F;
            biomeChangedTime = -1L;
        } else if (fogType == FogType.POWDER_SNOW) {
            fogRed = 0.623F;
            fogGreen = 0.734F;
            fogBlue = 0.785F;
            biomeChangedTime = -1L;
            RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
        } else {
            u = 0.25F + 0.75F * (float)i / 32.0F;
            u = 1.0F - (float)Math.pow((double)u, 0.25);
            Vec3 vec3 = clientLevel.getSkyColor(camera.getPosition(), f);
            v = (float)vec3.x;
            w = (float)vec3.y;
            float x = (float)vec3.z;
            float y = Mth.clamp(Mth.cos(clientLevel.getTimeOfDay(f) * 6.2831855F) * 2.0F + 0.5F, 0.0F, 1.0F);
            BiomeManager biomeManager = clientLevel.getBiomeManager();
            Vec3 vec32 = camera.getPosition().subtract(2.0, 2.0, 2.0).scale(0.25);
            Vec3 vec33 = CubicSampler.gaussianSampleVec3(vec32, (ix, jx, kx) -> {
                return clientLevel.effects().getBrightnessDependentFogColor(Vec3.fromRGB24(((Biome)biomeManager.getNoiseBiomeAtQuart(ix, jx, kx).value()).getFogColor()), y);
            });
            fogRed = (float)vec33.x();
            fogGreen = (float)vec33.y();
            fogBlue = (float)vec33.z();
            if (i >= 4) {
                h = Mth.sin(clientLevel.getSunAngle(f)) > 0.0F ? -1.0F : 1.0F;
                Vector3f vector3f = new Vector3f(h, 0.0F, 0.0F);
                s = camera.getLookVector().dot(vector3f);
                if (s < 0.0F) {
                    s = 0.0F;
                }

                if (s > 0.0F) {
                    float[] fs = clientLevel.effects().getSunriseColor(clientLevel.getTimeOfDay(f), f);
                    if (fs != null) {
                        s *= fs[3];
                        fogRed = fogRed * (1.0F - s) + fs[0] * s;
                        fogGreen = fogGreen * (1.0F - s) + fs[1] * s;
                        fogBlue = fogBlue * (1.0F - s) + fs[2] * s;
                    }
                }
            }

            fogRed += (v - fogRed) * u;
            fogGreen += (w - fogGreen) * u;
            fogBlue += (x - fogBlue) * u;
            h = clientLevel.getRainLevel(f);
            if (h > 0.0F) {
                r = 1.0F - h * 0.5F;
                s = 1.0F - h * 0.4F;
                fogRed *= r;
                fogGreen *= r;
                fogBlue *= s;
            }

            r = clientLevel.getThunderLevel(f);
            if (r > 0.0F) {
                s = 1.0F - r * 0.5F;
                fogRed *= s;
                fogGreen *= s;
                fogBlue *= s;
            }

            biomeChangedTime = -1L;
        }

        u = ((float)camera.getPosition().y - (float)clientLevel.getMinBuildHeight()) * clientLevel.getLevelData().getClearColorScale();
        FogRenderer.MobEffectFogFunction mobEffectFogFunction = getPriorityFogFunction(entity, f);
        if (mobEffectFogFunction != null) {
            LivingEntity livingEntity = (LivingEntity)entity;
            u = mobEffectFogFunction.getModifiedVoidDarkness(livingEntity, livingEntity.getEffect(mobEffectFogFunction.getMobEffect()), u, f);
        }

        if (u < 1.0F && fogType != FogType.LAVA && fogType != FogType.POWDER_SNOW) {
            if (u < 0.0F) {
                u = 0.0F;
            }

            u *= u;
            fogRed *= u;
            fogGreen *= u;
            fogBlue *= u;
        }

        if (g > 0.0F) {
            fogRed = fogRed * (1.0F - g) + fogRed * 0.7F * g;
            fogGreen = fogGreen * (1.0F - g) + fogGreen * 0.6F * g;
            fogBlue = fogBlue * (1.0F - g) + fogBlue * 0.6F * g;
        }

        if (fogType == FogType.WATER) {
            if (entity instanceof LocalPlayer) {
                v = ((LocalPlayer)entity).getWaterVision();
            } else {
                v = 1.0F;
            }
        } else {
            label86: {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity2 = (LivingEntity)entity;
                    if (livingEntity2.hasEffect(MobEffects.NIGHT_VISION) && !livingEntity2.hasEffect(MobEffects.DARKNESS)) {
                        v = GameRenderer.getNightVisionScale(livingEntity2, f);
                        break label86;
                    }
                }

                v = 0.0F;
            }
        }

        if (fogRed != 0.0F && fogGreen != 0.0F && fogBlue != 0.0F) {
            w = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
            fogRed = fogRed * (1.0F - v) + fogRed * w * v;
            fogGreen = fogGreen * (1.0F - v) + fogGreen * w * v;
            fogBlue = fogBlue * (1.0F - v) + fogBlue * w * v;
        }

        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }



}