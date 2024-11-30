package net.cloud.betterfog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

public class CustomFogRenderer {
    private static final int WATER_FOG_DISTANCE = 96;
    private static final List<MobEffectFogFunction> MOB_EFFECT_FOG = Lists.newArrayList(new MobEffectFogFunction[]{new BlindnessFogFunction(), new DarknessFogFunction()});
    public static final float BIOME_FOG_TRANSITION_TIME = 5000.0F;
    private static float fogRed;
    private static float fogGreen;
    private static float fogBlue;
    private static int targetBiomeFog = -1;
    private static int previousBiomeFog = -1;
    private static long biomeChangedTime = -1L;

    public static Vec3 setupColor(Camera arg, float g, ClientLevel arg2, int m, float h) {
        FogType fogtype = arg.getFluidInCamera();
        Entity entity = arg.getEntity();
        float f13;
        float f15;
        float f16;
        float f5;
        float f7;
        float f9;
        {
            f5 = 0.25F + 0.75F * (float)m / 32.0F;
            f5 = 1.0F - (float)Math.pow((double)f5, 0.25);
            Vec3 vec3 = arg2.getSkyColor(arg.getPosition(), g);
            f7 = (float)vec3.x;
            f9 = (float)vec3.y;
            float f10 = (float)vec3.z;
            float f11 = Mth.clamp(Mth.cos(arg2.getTimeOfDay(g) * 6.2831855F) * 2.0F + 0.5F, 0.0F, 1.0F);
            BiomeManager biomemanager = arg2.getBiomeManager();
            Vec3 vec31 = arg.getPosition().subtract(2.0, 2.0, 2.0).scale(0.25);
            Vec3 vec32 = CubicSampler.gaussianSampleVec3(vec31, (ix, jx, kx) -> {
                return arg2.effects().getBrightnessDependentFogColor(Vec3.fromRGB24(((Biome)biomemanager.getNoiseBiomeAtQuart(ix, jx, kx).value()).getFogColor()), f11);
            });
            fogRed = (float)vec32.x();
            fogGreen = (float)vec32.y();
            fogBlue = (float)vec32.z();
            if (m >= 4) {
                f13 = Mth.sin(arg2.getSunAngle(g)) > 0.0F ? -1.0F : 1.0F;
                Vector3f vector3f = new Vector3f(f13, 0.0F, 0.0F);
                f16 = arg.getLookVector().dot(vector3f);
                if (f16 < 0.0F) {
                    f16 = 0.0F;
                }

                if (f16 > 0.0F) {
                    float[] afloat = arg2.effects().getSunriseColor(arg2.getTimeOfDay(g), g);
                    if (afloat != null) {
                        f16 *= afloat[3];
                        fogRed = fogRed * (1.0F - f16) + afloat[0] * f16;
                        fogGreen = fogGreen * (1.0F - f16) + afloat[1] * f16;
                        fogBlue = fogBlue * (1.0F - f16) + afloat[2] * f16;
                    }
                }
            }

            fogRed += (f7 - fogRed) * f5;
            fogGreen += (f9 - fogGreen) * f5;
            fogBlue += (f10 - fogBlue) * f5;

            biomeChangedTime = -1L;
        }

        {
            label86: {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingentity1 = (LivingEntity)entity;
                    if (livingentity1.hasEffect(MobEffects.NIGHT_VISION) && !livingentity1.hasEffect(MobEffects.DARKNESS)) {
                        f7 = GameRenderer.getNightVisionScale(livingentity1, g);
                        break label86;
                    }
                }

                f7 = 0.0F;
            }
        }

        if (fogRed != 0.0F && fogGreen != 0.0F && fogBlue != 0.0F) {
            f9 = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
            fogRed = fogRed * (1.0F - f7) + fogRed * f9 * f7;
            fogGreen = fogGreen * (1.0F - f7) + fogGreen * f9 * f7;
            fogBlue = fogBlue * (1.0F - f7) + fogBlue * f9 * f7;
        }

        return new Vec3(fogRed, fogGreen, fogBlue);
    }
    public static void setupNoFog() {
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
    }

    @Nullable
    private static MobEffectFogFunction getPriorityFogFunction(Entity arg, float f) {
        if (arg instanceof LivingEntity livingentity) {
            return (MobEffectFogFunction)MOB_EFFECT_FOG.stream().filter((arg2) -> {
                return arg2.isEnabled(livingentity, f);
            }).findFirst().orElse((MobEffectFogFunction)null);
        } else {
            return null;
        }
    }

    public static Pair<Float, Float> setupFog(Camera arg, FogMode arg2, float g, boolean bl, float h) {
        FogType fogtype = arg.getFluidInCamera();
        Entity entity = arg.getEntity();
        FogData CustomFogRenderer$fogdata = new FogData(arg2);
        MobEffectFogFunction CustomFogRenderer$mobeffectfogfunction = getPriorityFogFunction(entity, h);
        if (fogtype == FogType.LAVA) {
            if (entity.isSpectator()) {
                CustomFogRenderer$fogdata.start = -8.0F;
                CustomFogRenderer$fogdata.end = g * 0.5F;
            } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasEffect(MobEffects.FIRE_RESISTANCE)) {
                CustomFogRenderer$fogdata.start = 0.0F;
                CustomFogRenderer$fogdata.end = 3.0F;
            } else {
                CustomFogRenderer$fogdata.start = 0.25F;
                CustomFogRenderer$fogdata.end = 1.0F;
            }
        } else if (fogtype == FogType.POWDER_SNOW) {
            if (entity.isSpectator()) {
                CustomFogRenderer$fogdata.start = -8.0F;
                CustomFogRenderer$fogdata.end = g * 0.5F;
            } else {
                CustomFogRenderer$fogdata.start = 0.0F;
                CustomFogRenderer$fogdata.end = 2.0F;
            }
        } else if (CustomFogRenderer$mobeffectfogfunction != null) {
            LivingEntity livingentity = (LivingEntity)entity;
            MobEffectInstance mobeffectinstance = livingentity.getEffect(CustomFogRenderer$mobeffectfogfunction.getMobEffect());
            if (mobeffectinstance != null) {
                CustomFogRenderer$mobeffectfogfunction.setupFog(CustomFogRenderer$fogdata, livingentity, mobeffectinstance, g, h);
            }
        } else if (fogtype == FogType.WATER) {
            CustomFogRenderer$fogdata.start = -8.0F;
            CustomFogRenderer$fogdata.end = 96.0F;
            if (entity instanceof LocalPlayer) {
                LocalPlayer localplayer = (LocalPlayer)entity;
                CustomFogRenderer$fogdata.end *= Math.max(0.25F, localplayer.getWaterVision());
                Holder<Biome> holder = localplayer.level().getBiome(localplayer.blockPosition());
                if (holder.is(BiomeTags.HAS_CLOSER_WATER_FOG)) {
                    CustomFogRenderer$fogdata.end *= 0.85F;
                }
            }

            if (CustomFogRenderer$fogdata.end > g) {
                CustomFogRenderer$fogdata.end = g;
                CustomFogRenderer$fogdata.shape = FogShape.CYLINDER;
            }
        } else if (bl) {
            CustomFogRenderer$fogdata.start = g * 0.05F;
            CustomFogRenderer$fogdata.end = Math.min(g, 192.0F) * 0.5F;
        } else if (arg2 == FogMode.FOG_SKY) {
            CustomFogRenderer$fogdata.start = 0.0F;
            CustomFogRenderer$fogdata.end = g;
            CustomFogRenderer$fogdata.shape = FogShape.CYLINDER;
        } else {
            float f = Mth.clamp(g / 10.0F, 4.0F, 64.0F);
            CustomFogRenderer$fogdata.start = g - f;
            CustomFogRenderer$fogdata.end = g;
            CustomFogRenderer$fogdata.shape = FogShape.CYLINDER;
        }

        return Pair.of(CustomFogRenderer$fogdata.start, CustomFogRenderer$fogdata.end);
    }

    public static void levelFogColor() {
        RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue);
    }

    @OnlyIn(Dist.CLIENT)
    interface MobEffectFogFunction {
        MobEffect getMobEffect();

        void setupFog(FogData arg, LivingEntity arg2, MobEffectInstance arg3, float f, float g);

        default boolean isEnabled(LivingEntity arg, float f) {
            return arg.hasEffect(this.getMobEffect());
        }

        default float getModifiedVoidDarkness(LivingEntity arg, MobEffectInstance arg2, float f, float g) {
            MobEffectInstance mobeffectinstance = arg.getEffect(this.getMobEffect());
            if (mobeffectinstance != null) {
                if (mobeffectinstance.endsWithin(19)) {
                    f = 1.0F - (float)mobeffectinstance.getDuration() / 20.0F;
                } else {
                    f = 0.0F;
                }
            }

            return f;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class FogData {
        public final FogMode mode;
        public float start;
        public float end;
        public FogShape shape;

        public FogData(FogMode arg) {
            this.shape = FogShape.SPHERE;
            this.mode = arg;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum FogMode {
        FOG_SKY,
        FOG_TERRAIN;

        private FogMode() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class BlindnessFogFunction implements MobEffectFogFunction {
        BlindnessFogFunction() {
        }

        public MobEffect getMobEffect() {
            return MobEffects.BLINDNESS;
        }

        public void setupFog(FogData arg, LivingEntity arg2, MobEffectInstance arg3, float g, float h) {
            float f = arg3.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, (float)arg3.getDuration() / 20.0F), g, 5.0F);
            if (arg.mode == FogMode.FOG_SKY) {
                arg.start = 0.0F;
                arg.end = f * 0.8F;
            } else {
                arg.start = f * 0.25F;
                arg.end = f;
            }

        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DarknessFogFunction implements MobEffectFogFunction {
        DarknessFogFunction() {
        }

        public MobEffect getMobEffect() {
            return MobEffects.DARKNESS;
        }

        public void setupFog(FogData arg, LivingEntity arg2, MobEffectInstance arg3, float g, float h) {
            if (!arg3.getFactorData().isEmpty()) {
                float f = Mth.lerp(((MobEffectInstance.FactorData)arg3.getFactorData().get()).getFactor(arg2, h), g, 15.0F);
                arg.start = arg.mode == FogMode.FOG_SKY ? 0.0F : f * 0.75F;
                arg.end = f;
            }

        }

        public float getModifiedVoidDarkness(LivingEntity arg, MobEffectInstance arg2, float f, float g) {
            return arg2.getFactorData().isEmpty() ? 0.0F : 1.0F - ((MobEffectInstance.FactorData)arg2.getFactorData().get()).getFactor(arg, g);
        }
    }
}
