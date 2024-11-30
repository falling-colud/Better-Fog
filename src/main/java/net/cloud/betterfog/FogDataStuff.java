package net.cloud.betterfog;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FogDataStuff {
    @OnlyIn(Dist.CLIENT)
    public interface MobEffectFogFunction {
        MobEffect getMobEffect();

        void setupFog(FogData fogData, LivingEntity livingEntity, MobEffectInstance mobEffectInstance, float f, float g);

        default boolean isEnabled(LivingEntity livingEntity, float f) {
            return livingEntity.hasEffect(this.getMobEffect());
        }

        default float getModifiedVoidDarkness(LivingEntity livingEntity, MobEffectInstance mobEffectInstance, float f, float g) {
            MobEffectInstance mobEffectInstance2 = livingEntity.getEffect(this.getMobEffect());
            if (mobEffectInstance2 != null) {
                if (mobEffectInstance2.endsWithin(19)) {
                    f = 1.0F - (float)mobEffectInstance2.getDuration() / 20.0F;
                } else {
                    f = 0.0F;
                }
            }

            return f;
        }
    }

    @OnlyIn(Dist.CLIENT)

    public static class FogData {
        public final FogRenderer.FogMode mode;
        public float start;
        public float end;
        public FogShape shape;

        public FogData(FogRenderer.FogMode fogMode) {
            this.shape = FogShape.SPHERE;
            this.mode = fogMode;
        }
    }

    @OnlyIn(Dist.CLIENT)

    public static class BlindnessFogFunction implements MobEffectFogFunction {
        public BlindnessFogFunction() {
        }

        public MobEffect getMobEffect() {
            return MobEffects.BLINDNESS;
        }

        public void setupFog(FogData fogData, LivingEntity livingEntity, MobEffectInstance mobEffectInstance, float f, float g) {
            float h = mobEffectInstance.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, (float)mobEffectInstance.getDuration() / 20.0F), f, 5.0F);
            if (fogData.mode == FogRenderer.FogMode.FOG_SKY) {
                fogData.start = 0.0F;
                fogData.end = h * 0.8F;
            } else {
                fogData.start = h * 0.25F;
                fogData.end = h;
            }

        }
    }

    @OnlyIn(Dist.CLIENT)

    public static class DarknessFogFunction implements MobEffectFogFunction {
        public DarknessFogFunction() {
        }

        public MobEffect getMobEffect() {
            return MobEffects.DARKNESS;
        }

        public void setupFog(FogData fogData, LivingEntity livingEntity, MobEffectInstance mobEffectInstance, float f, float g) {
            if (!mobEffectInstance.getFactorData().isEmpty()) {
                float h = Mth.lerp(((MobEffectInstance.FactorData)mobEffectInstance.getFactorData().get()).getFactor(livingEntity, g), f, 15.0F);
                fogData.start = fogData.mode == FogRenderer.FogMode.FOG_SKY ? 0.0F : h * 0.75F;
                fogData.end = h;
            }
        }

        public float getModifiedVoidDarkness(LivingEntity livingEntity, MobEffectInstance mobEffectInstance, float f, float g) {
            return mobEffectInstance.getFactorData().isEmpty() ? 0.0F : 1.0F - ((MobEffectInstance.FactorData)mobEffectInstance.getFactorData().get()).getFactor(livingEntity, g);
        }
    }
}
