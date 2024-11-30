package net.cloud.betterfog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URISyntaxException;

import static net.cloud.betterfog.BetterFogConfiguration.*;


public class Test {

    private static String biomeInfo = "{\n" +
            "  \"day\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"night\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"cave\": {\n" +
            "    \"color\": \"0111111\",\n" +
            "    \"start\": 32,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"rain\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"thunder\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"void\": {\n" +
            "    \"color\": \"0\",\n" +
            "    \"start\": 32,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"cloud\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"water\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  },\n" +
            "  \"lava\": {\n" +
            "    \"color\": 0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128,\n" +
            "    \"shape\": 0\n" +
            "  }\n" +
            "}";
    public static String defaultInfoS = "{\n" +
            "  \"template\": {\n" +
            "    \"red\": 0,\n" +
            "    \"green\": 0,\n" +
            "    \"blue\": 0,\n" +
            "    \"opacity\": 1.0,\n" +
            "    \"start\": 64,\n" +
            "    \"end\": 128\n" +
            "  },\n" +
            "  \"day\": {\n" +
            "    \"start\": 32,\n" +
            "    \"end\": \"*0.9\"\n" +
            "  },\n" +
            "  \"night\": {\n" +
            "    \"red\": \"*0.1\",\n" +
            "    \"green\": \"*0.1\",\n" +
            "    \"blue\": \"*0.1\",\n" +
            "    \"opacity\": \"*1.0\",\n" +
            "    \"start\": 0,\n" +
            "    \"end\": \"*0.6\"\n" +
            "  },\n" +
            "  \"cave\": {\n" +
            "    \"red\": \"*0.7\",\n" +
            "    \"green\": \"*0.7\",\n" +
            "    \"blue\": \"*0.7\",\n" +
            "    \"opacity\": 0.75,\n" +
            "    \"start\": 0,\n" +
            "    \"end\": 64\n" +
            "  },\n" +
            "  \"rain\": {\n" +
            "    \"red\": \"*0.8\",\n" +
            "    \"green\": \"*0.8\",\n" +
            "    \"blue\": \"*0.8\",\n" +
            "    \"opacity\": 0.4,\n" +
            "    \"start\": 0,\n" +
            "    \"end\": \"*0.6\"\n" +
            "  },\n" +
            "  \"thunder\": {\n" +
            "    \"red\": \"*0.65\",\n" +
            "    \"green\": \"*0.65\",\n" +
            "    \"blue\": \"*0.65\",\n" +
            "    \"opacity\": \"*1.0\",\n" +
            "    \"start\": 0,\n" +
            "    \"end\": \"*0.5\"\n" +
            "  },\n" +
            "  \"void\": {\n" +
            "    \"red\": 0,\n" +
            "    \"green\": 0,\n" +
            "    \"blue\": 0,\n" +
            "    \"opacity\": 0.8,\n" +
            "    \"start\": 0,\n" +
            "    \"end\": 48\n" +
            "  },\n" +
            "  \"cloud\": {\n" +
            "    \"red\": 1,\n" +
            "    \"green\": 1,\n" +
            "    \"blue\": 1,\n" +
            "    \"opacity\": 0.75,\n" +
            "    \"start\": 0,\n" +
            "    \"end\": 32\n" +
            "  },\n" +
            "  \"water\": {\n" +
            "    \"red\": \"-0.6\",\n" +
            "    \"green\": \"-0.6\",\n" +
            "    \"blue\": \"+0.75\",\n" +
            "    \"opacity\": 0.75,\n" +
            "    \"start\": 0,\n" +
            "    \"end\": 96\n" +
            "  },\n" +
            "  \"lava\": {\n" +
            "    \"red\": \"+0.9\",\n" +
            "    \"green\": \"-0.9\",\n" +
            "    \"blue\": \"-0.9\",\n" +
            "    \"opacity\": 0.8,\n" +
            "    \"start\": 0,\n" +
            "    \"end\": 32\n" +
            "  }\n" +
            "}";
    public static JsonObject defaultInfo = JsonParser.parseString(defaultInfoS).getAsJsonObject();

    public static void main(String[] args) {

        JsonObject jsonBiomeInfo = JsonParser.parseString(biomeInfo).getAsJsonObject();

        BiomeInfo biomeInfo = new BiomeInfo(jsonBiomeInfo);

        BiomeInfo.CustomFogData defaultData = biomeInfo.getDefault();
        BiomeInfo.CustomFogData caveData = biomeInfo.getCave();

        float half = (cloudYStart + (float) (cloudYEnd - cloudYStart) / 2);

        Sigmoid cloudSig1 = Sigmoid.sigmoid01(cloudYStart, half, cloudP, 0.5f);
        Sigmoid cloudSig2 = Sigmoid.sigmoid01(cloudYEnd, half, cloudP, 0.5f);

        for (int y = cloudYStart; y < cloudYEnd; y++){
            if (y < half) {
                System.out.println(cloudSig1.bs(y));
            } else {
                System.out.println(cloudSig2.bs(y));
            }
        }
    }
}
