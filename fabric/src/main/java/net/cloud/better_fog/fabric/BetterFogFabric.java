package net.cloud.better_fog.fabric;

import net.cloud.better_fog.BetterFog;
import net.fabricmc.api.ModInitializer;

public class BetterFogFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BetterFog.init();
    }
}