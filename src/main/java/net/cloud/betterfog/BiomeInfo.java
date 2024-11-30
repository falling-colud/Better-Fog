package net.cloud.betterfog;

import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import static net.cloud.betterfog.BetterFogConfiguration.*;

public class BiomeInfo {
    //private final HashMap<String, Pair<List<String>, List<Float>>> biomeData = new HashMap<>();
    private JsonObject biomeData;

    public BiomeInfo(JsonObject object) {
        this.biomeData = object;
    }

    public CustomFogData get(String id) {

        JsonObject def = BetterFogConfiguration.getDefault();

        if ((configJson == null || configJson.get(id + "_enabled").getAsBoolean()) && (biomeData.has(id) || !disableForUnconfigured)) {

            if (!biomeData.has(id))
                return new CustomFogData(def.get(id).getAsJsonObject());
            else
                return new CustomFogData(biomeData.get(id).getAsJsonObject());
        }

        return new CustomFogData(null);
    }

    public CustomFogData getDefault() {
        return get("day");
    }
    public CustomFogData getNight() {
        return get("night");
    }
    public CustomFogData getRain() {
        return get("rain");
    }
    public CustomFogData getThunder() {
        return get("thunder");
    }
    public CustomFogData getVoid() {
        return get("void");
    }
    public CustomFogData getCave() {
        return get("cave");
    }
    public CustomFogData getCloud() {
        return get("cloud");
    }
    public CustomFogData getWater() {
        return get("water");
    }
    public CustomFogData getLava() {
        return get("lava");
    }

    public static class CustomFogData {

        private final JsonObject data;
        private final boolean isEnabled;

        public CustomFogData(JsonObject data) {
            this.isEnabled = data != null;
            this.data = data;
        }

        public Pair<String, Float> get(String key) {
            if (data.has(key)) return getPairSV(data.get(key).getAsString());
            else return  getPairSV(null);
        }
        public Pair<String, Float> getRed() {
            return get("red");
        }
        public Pair<String, Float> getGreen() {
            return get("green");
        }
        public Pair<String, Float> getBlue() {
            return get("blue");
        }

        public Pair<String, Float> getStart() {
            return get("start");
        }
        public Pair<String, Float> getEnd() {
            return get("end");
        }
        public Pair<String, Float> getOpacity() {
            return get("opacity");
        }



        public Vec3 colorAsVec3() {
            return new Vec3(getRed().getRight(), getGreen().getRight(), getBlue().getRight());
        }

        public boolean isEnabled() {
            return isEnabled;
        }
    }
}
