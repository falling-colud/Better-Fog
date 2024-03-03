package net.cloud.better_fog.forge;

import dev.architectury.platform.forge.EventBuses;
import net.cloud.better_fog.BetterFog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BetterFog.MOD_ID)
public class BetterFogForge {
    public BetterFogForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(BetterFog.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        BetterFog.init();
    }
}