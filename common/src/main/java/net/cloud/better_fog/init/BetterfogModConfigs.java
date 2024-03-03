package net.cloud.better_fog.init;

import net.cloud.betterfog.BetterfogMod;
import net.cloud.betterfog.configuration.BetterFogConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = BetterfogMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BetterfogModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() -> {
			ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BetterFogConfiguration.SPEC, "better_fog.toml");
		});
	}
}
