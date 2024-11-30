package net.cloud.betterfog;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static net.cloud.betterfog.Test.defaultInfoS;


public class BetterFogConfiguration {

    //by removing config option you disable for biome
    private static final List<String> defaultBiomes = List.of("default", "plains");
    public static boolean usesDefaultBiomes = true;
    private static final List<String> signs = List.of("", "+", "-", "=", "*", "/");
    public static boolean disableForUnconfigured;
    public static int caveYStart = 64;
    public static int caveYEnd = -32;
    public static int caveP = 6;
    public static int cloudYStart = 192;
    public static int cloudYEnd = 256;
    public static int cloudP = 6;
    public static int voidYStart = -64;
    public static int voidYEnd = -32;
    public static int voidP = 6;
    public static int changeTime = 200;
    public static float stepColor = 0.1f;
    public static float stepDistance = 20f;
    public static ArrayList<String> dimensionBlacklist;

    static {
        dimensionBlacklist = new ArrayList<>();
        dimensionBlacklist.add("minecraft:the_end");
    }

    private static boolean write = false;

    public static JsonObject configJson;

    public static JsonObject getDefault() {
        if (configJson != null) {
            JsonObject settings = configJson.get("biomes").getAsJsonObject();
            return settings.get("default").getAsJsonObject();
        } else {
            return Test.defaultInfo;
        }
    }

    public static boolean genPrimitive(String setting, boolean def) {
        if (!configJson.has(setting) || !configJson.get(setting).isJsonPrimitive()) {
            configJson.addProperty(setting, def);
            write = true;

            return def;
        } else {
            return configJson.get(setting).getAsBoolean();
        }
    }
    public static int genPrimitive(String setting, int def) {
        if (!configJson.has(setting) || !configJson.get(setting).isJsonPrimitive()) {
            configJson.addProperty(setting, def);
            write = true;

            return def;
        } else {
            return configJson.get(setting).getAsInt();
        }
    }
    public static float genPrimitive(String setting, float def) {
        if (!configJson.has(setting) || !configJson.get(setting).isJsonPrimitive()) {
            configJson.addProperty(setting, def);
            write = true;

            return def;
        } else {
            return configJson.get(setting).getAsFloat();
        }
    }

    public static ArrayList<String> genArrayString(String setting, ArrayList<String> defList) {
        if (!configJson.has(setting) || !configJson.get(setting).isJsonArray()) {
            JsonArray def = new JsonArray();
            defList.forEach(def::add);
            configJson.add(setting, def);
            write = true;

            return defList;
        } else {
            ArrayList<String> list = new ArrayList<>();
            for (JsonElement e : configJson.get(setting).getAsJsonArray()) {
                list.add(e.getAsString());
            }
            return list;
        }
    }

    public static ArrayList<Integer> genArrayInt(String setting, ArrayList<Integer> defList) {
        if (!configJson.has(setting) || !configJson.get(setting).isJsonArray()) {
            JsonArray def = new JsonArray();
            defList.forEach(def::add);
            configJson.add(setting, def);
            write = true;

            return defList;
        } else {
            ArrayList<Integer> list = new ArrayList<>();
            for (JsonElement e : configJson.get(setting).getAsJsonArray()) {
                list.add(e.getAsInt());
            }
            return list;
        }
    }

    public static ArrayList<Float> genArrayFloat(String setting, ArrayList<Float> defList) {
        if (!configJson.has(setting) || !configJson.get(setting).isJsonArray()) {
            JsonArray def = new JsonArray();
            defList.forEach(def::add);
            configJson.add(setting, def);
            write = true;

            return defList;
        } else {
            ArrayList<Float> list = new ArrayList<>();
            for (JsonElement e : configJson.get(setting).getAsJsonArray()) {
                list.add(e.getAsFloat());
            }
            return list;
        }
    }

    public static JsonObject getBiomeInfoJson(String biome) {
        JsonObject settings = configJson.get("biomes").getAsJsonObject();
        if (settings.has(biome)) {
            return settings.get(biome).getAsJsonObject();
        }
        /*
        def.remove("water");
        def.remove("lava");
        def.remove("day");
        def.remove("night");
         */
        //return new JsonObject();
        return getDefault();
    }

    public static BiomeInfo getBiomeInfo(String biome) {
        return new BiomeInfo(getBiomeInfoJson(biome));
    }

    public static void genConfig(String configName) {

        File config = new File((Minecraft.getInstance().gameDirectory + "/config/betterfog/"), File.separator + configName + ".json");

        File biomesDir = new File((Minecraft.getInstance().gameDirectory + "/config/betterfog/biomes/"));
        biomesDir.mkdirs();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(config));
            StringBuilder jsonstringbuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonstringbuilder.append(line);
            }
            bufferedReader.close();
            configJson = new Gson().fromJson(jsonstringbuilder.toString(), JsonObject.class);
        } catch (IOException e) {
            configJson = new JsonObject();

            write = true;

            if (!config.exists()) {
                try {
                    config.getParentFile().mkdirs();
                    config.createNewFile();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        if (configJson == null) configJson = new JsonObject();

        genPrimitive("enabled", true);
        genPrimitive("day_enabled", true);
        genPrimitive("night_enabled", false); //todo fix night
        genPrimitive("rain_enabled", true);
        genPrimitive("thunder_enabled", true);
        genPrimitive("cave_enabled", true);
        genPrimitive("cloud_enabled", true);
        genPrimitive("void_enabled", true);
        genPrimitive("water_enabled", true);
        genPrimitive("lava_enabled", true);
        genPrimitive("use_vanilla_for_unconfigured", true);

        dimensionBlacklist = genArrayString("dimension_blacklist", dimensionBlacklist);

        disableForUnconfigured = genPrimitive("use_vanilla_for_unconfigured", false);
        usesDefaultBiomes = genPrimitive("use_defaults", false);
        caveYStart = genPrimitive("cave_y_start", caveYStart);
        caveYEnd = genPrimitive("cave_y_end", caveYEnd);
        caveP = genPrimitive("cave_pick", caveP);
        cloudYStart = genPrimitive("cloud_y_start", cloudYStart);
        cloudYEnd = genPrimitive("cloud_y_end", cloudYEnd);
        cloudP = genPrimitive("cloud_pick", cloudP);
        voidYStart = genPrimitive("void_y_start", voidYStart);
        voidYEnd = genPrimitive("void_y_end", voidYEnd);
        voidP = genPrimitive("void_pick", voidP);

        changeTime = genPrimitive("change_time", 200);
        stepColor = genPrimitive("step_color", stepColor);
        stepDistance = genPrimitive("step_distance", stepDistance);

        if (write) {
            Gson mainGSONBuilderVariable = new GsonBuilder().setPrettyPrinting().create();
            try {
                FileWriter fileWriter = new FileWriter(config);
                fileWriter.write(mainGSONBuilderVariable.toJson(configJson));
                fileWriter.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        for (String key : getDefaultBiomes()) {
            File defBiome = new File((Minecraft.getInstance().gameDirectory + "/config/betterfog/biomes/"), File.separator + key + ".json");
            if (!defBiome.exists()) {
                try {
                    defBiome.getParentFile().mkdirs();
                    defBiome.createNewFile();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        JsonObject biomesJson = new JsonObject();
        for (File biomeFile : biomesDir.listFiles()) {
            String biome = biomeFile.getName().replace(".json", "");
            JsonObject biomeJson = fixSettingError(biome, biomeFile);
            biomesJson.add(biome, biomeJson);
        }

        configJson.add("biomes", biomesJson);
    }

    public static List<String> getDefaultBiomes() {
        return usesDefaultBiomes ? defaultBiomes : List.of("default");
    }

    private static JsonObject fixSettingError(File file) {
        String biome = file.getName().replace(".json", "");
        return fixSettingError(biome, file);
    }
    private static JsonObject fixSettingError(String biome, File file) {

        JsonObject ds = defaultSetting(biome);

        JsonObject settings;
        JsonObject returnSettings;
        boolean write = false;

        t: try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder jsonstringbuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonstringbuilder.append(line);
            }
            bufferedReader.close();
            JsonElement damagedSettings = new Gson().fromJson(jsonstringbuilder.toString(), JsonElement.class);

            if (damagedSettings == null || !damagedSettings.isJsonObject() || damagedSettings.getAsJsonObject().keySet().isEmpty()) {
                settings = ds;
                settings.remove("template");
                write = true;
                break t;
            }

            settings = damagedSettings.getAsJsonObject();

            for (String key : settings.keySet()) {
                JsonElement values = settings.get(key);
                JsonObject dvs = ds.get(key).getAsJsonObject();
                if (!values.isJsonObject() && !key.contains("template")) {
                    settings.add(key, dvs);
                    if (!write) write = true;
                } else {
                    boolean shouldAddVs = false;

                    //checks if only the properties that should exist are present
                    JsonObject valuesA = values.getAsJsonObject();
                    Set<String> valuesKeys = valuesA.deepCopy().keySet();
                    for (String i: valuesKeys) {
                        if (!ds.get("template").getAsJsonObject().has(i)) {
                            valuesA.remove(i);
                            shouldAddVs = true;
                        }
                    }
                    /*
                    for (String i: dvs.keySet()) {
                        if (!valuesA.has(i)) {
                            valuesA.add(i, dvs.get(i));
                            shouldAddVs = true;
                        }
                    }
                     */

                    for (String i: valuesA.keySet()) {
                        JsonElement valueE = valuesA.get(i);

                        String dvStr;
                        if (dvs.has(i)) dvStr = dvs.get(i).getAsString();
                        else dvStr = null;

                        if (!valueE.isJsonPrimitive())
                            if (dvStr != null)
                                valuesA.addProperty(i, dvStr);
                            else
                                valuesA.remove(i);
                        else {
                            boolean shouldAddV = false;

                            String fullValue = valueE.getAsString();

                            String sign = fullValue.substring(0, 1);
                            String value = fullValue;

                            float fValue;

                            if (signs.contains(sign))
                                value = fullValue.substring(1);
                            else sign = "";

                            try { // check if value is valid
                                fValue = Float.parseFloat(value);
                            } catch (NumberFormatException ignored) {
                                try { // if not check if sign is invalid by checking if substring value is valid
                                    String newValue = fullValue.substring(1);
                                    fValue = Float.parseFloat(newValue);
                                } catch (NumberFormatException ignored2) { //if not just use default
                                    Pair<String, Float> sv = getPairSV(dvStr);
                                    sign = sv.getKey() == "=" ? "" : sv.getKey();
                                    fValue = sv.getValue();
                                }

                                shouldAddV = true;
                            }

                            if ((sign == "=" || sign == "") && (i.contains("red") || i.contains("green") || i.contains("blue") || i.contains("opacity"))) {
                                if (fValue > 1) {
                                    shouldAddV = true;
                                    fValue = 1;
                                }
                                if (fValue < 0) {
                                    shouldAddV = true;
                                    fValue = 0;
                                }
                            }

                            fullValue = sign + fValue;

                            if (shouldAddV) {
                                try { // check if number to write is just an int, if so write an int
                                    valuesA.addProperty(i, Integer.parseInt(fullValue));
                                } catch (NumberFormatException ignored2) {
                                    try { // check if number to write is just an int, if so write an int
                                        valuesA.addProperty(i, Float.parseFloat(fullValue));
                                    } catch (NumberFormatException ignored) {
                                        valuesA.addProperty(i, (fullValue));

                                    }
                                }
                                shouldAddVs = true;
                            }
                        }
                    }

                    if (shouldAddVs) {
                        settings.add(key, valuesA);
                        write = true;
                    }

                }
            }
        } catch (IOException e) {
            settings = ds;
            write = true;
        }

        if (write) {
            writeJson(file, settings);
        }

        return settings;

    }
    
    public static void writeJson(File file, JsonObject json) {
        Gson mainGSONBuilderVariable = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(mainGSONBuilderVariable.toJson(json));
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static Pair<String, Float> getPairSV(String value) {

        if (value == null) return  Pair.of("*", 1f);

        String sign = value.substring(0, 1);

        if (signs.contains(sign))
            value = value.substring(1);
        else  sign = "=";

        return Pair.of(sign, Float.parseFloat(value));
    }

    private static JsonObject defaultSetting(String biome) {
        try {
            //TODO resources file

            InputStream in;

            ResourceLocation loc = new ResourceLocation(BetterFog.MOD_ID, "default_config/" + biome + ".json");
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isPresent()) {
                in = res.get().open();
            } else {
                loc = new ResourceLocation(BetterFog.MOD_ID, "default_config/default.json");
                in = Minecraft.getInstance().getResourceManager().getResource(loc).get().open();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            JsonElement je = new Gson().fromJson(reader, JsonElement.class);

            return je.getAsJsonObject();

            /*

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonstringbuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonstringbuilder.append(line);
            }
            reader.close();
            return new Gson().fromJson(jsonstringbuilder.toString(), JsonObject.class);

             */
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	/*
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;
	public static final ForgeConfigSpec.ConfigValue<Integer> Y_START;
	public static final ForgeConfigSpec.ConfigValue<Integer> Y_END;
	public static final ForgeConfigSpec.ConfigValue<Integer> MIN_OPACITY;
	public static final ForgeConfigSpec.ConfigValue<Integer> DEFAULT_OPACITY;
	public static final ForgeConfigSpec.ConfigValue<Integer> MIN_START_FOG;
	public static final ForgeConfigSpec.ConfigValue<Integer> DEFAULT_START_FOG;
	public static final ForgeConfigSpec.ConfigValue<Double> END_FOG_MULTIPLYER;
	public static final ForgeConfigSpec.ConfigValue<Integer> MIN_END_FOG;
    public static final ForgeConfigSpec.ConfigValue<Double> BIOME_CHECK_VERTICAL_FACTOR;
	public static final ForgeConfigSpec.ConfigValue<Integer> BIOME_CHECK_RADIUS;
	public static final ForgeConfigSpec.ConfigValue<Integer> BIOME_CHECKS_FOR_SIDE;
	public static final ForgeConfigSpec.ConfigValue<Boolean> BIOME_ENABLED;
	public static final ForgeConfigSpec.ConfigValue<Boolean> CAVE_ENABLED;
	public static final ForgeConfigSpec.ConfigValue<Boolean> void_enabled;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_Y_START;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_START_START;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_Y_END;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_END_END;
	public static final ForgeConfigSpec.ConfigValue<Double> CLOUD_MAX_OPACITY;
	public static final ForgeConfigSpec.ConfigValue<Double> RAIN_OPACITY_FACTOR;
	public static final ForgeConfigSpec.ConfigValue<Boolean> thunder_enabled;
	public static final ForgeConfigSpec.ConfigValue<ArrayList<String>> BIOME_FEATURES_LIST;
	public static final ForgeConfigSpec.ConfigValue<Boolean> NIGHT_FOG_ENABLED;
	public static final ForgeConfigSpec.ConfigValue<Integer> NIGHT_FOG_START;
	public static final ForgeConfigSpec.ConfigValue<Integer> NIGHT_FOG_END;
	public static final ForgeConfigSpec.ConfigValue<Double> NIGHT_DENSITY;





	static {
		BUILDER.push("general_fog");
		DEFAULT_OPACITY = BUILDER.comment("the default fog's opacity from 0 to 255").define("default_opacity", (Integer) 80);
		DEFAULT_START_FOG = BUILDER.comment("the default fog's start distance from the player").define("default_start_fog", (Integer) 25);
		END_FOG_MULTIPLYER = BUILDER.comment("the default fog's end distance from the player multiplier (multiplies the vanilla fog end by this amount)").define("END_FOG_MULTIPLYER", 1.15);
		BUILDER.pop();

		BUILDER.push("night_fog");
		NIGHT_FOG_ENABLED = BUILDER.define("enabled", true);
		NIGHT_DENSITY = BUILDER.comment("the night fog's opacity multiplier").define("night_opacity",  1.5);
		NIGHT_FOG_START = BUILDER.comment("the night fog's start distance from the player").define("night_start_fog",  0);
		NIGHT_FOG_END = BUILDER.comment("the night fog's end distance from the player").define("night_end_fog", 60);

		BUILDER.pop();

		BUILDER.push("cave_fog");
		CAVE_ENABLED = BUILDER.define("enabled", true);
		Y_START = BUILDER.comment("y value where cave fog starts").define("y_start", (Integer) 100);
		Y_END = BUILDER.comment("y value where the cave fog ends").define("y_end", -36);
		MIN_OPACITY = BUILDER.comment("the cave fog's opacity and the y_end y value from 0 to 255").define("min_opacity", (Integer) 110);
		MIN_START_FOG = BUILDER.comment("the cave fog's start distance from the player the y_end y value").define("min_start_fog", (Integer) 0);
		MIN_END_FOG = BUILDER.comment("the cave fog's end distance from the player the y_end y value").define("min_end_fog", (Integer) 100);
		BUILDER.pop();

		BUILDER.push("biome_fog");
		BIOME_ENABLED = BUILDER.define("enabled", true);
		BIOME_CHECK_RADIUS = BUILDER.comment("the radius in which the mod will check for different biomes and calculate the correct fog color").define("check_radius", 20);
		BIOME_CHECK_VERTICAL_FACTOR = BUILDER.comment("the vertical multiplier for the radius check").define("vertical_factor", 0.25);
		BIOME_CHECKS_FOR_SIDE = BUILDER.comment("how many checks for the diameter of the circle that check for biomes will be made (higher value = better accuracy)").define("checks_for_side", 8);

		ArrayList<String> biomeColors = new ArrayList<>();
		biomeColors.add("minecraft:desert;C2B280;1.2;0.75;1");
		biomeColors.add("minecraft:beach;C2B280;1.2;0.75;1");
		biomeColors.add("minecraft:badlands;FF9100;1.2;0.75;1");
		biomeColors.add("minecraft:mesa;FF9100;1.2;0.75;1");
		biomeColors.add("minecraft:savana;FFDD2C;1;0.85;1");
		biomeColors.add("minecraft:forest;E2FFFF;1.1;0.95;0.9");
		biomeColors.add("minecraft:mushroom_island;EAA2FF;0.6;0.5;1");
		biomeColors.add("minecraft:mountain;94CAFF;1.5;0.5;1.5");
		biomeColors.add("minecraft:ice_spikes;39AAFF;1.5;0.7;0.85");
		biomeColors.add("minecraft:frozen_ocean;39AAFF;1.5;0.7;0.85");
		biomeColors.add("minecraft:deep_frozen_ocean;39AAFF;1.5;0.7;0.85");
		biomeColors.add("minecraft:frozen_peaks;DBF0FF;39AAFF;1.5;0.7;0.85");
		biomeColors.add("minecraft:swamp;005F0A;1.2;0.75;1");
		biomeColors.add("minecraft:the_nether;B50000;1.3;0.75;1");
		biomeColors.add("minecraft:the_end;660066;1;0.75;1");

		BIOME_FEATURES_LIST = BUILDER.comment("list of properties for each biome, the values mean: <biome_id>;<color in hex>;<opacity multiplier>;<fog start multiplier>;<fog end multiplier>").define("colors_list", biomeColors);
		BUILDER.pop();

		BUILDER.push("cloud_fog");
		CLOUD_ENABLED = BUILDER.define("enabled", true);
		CLOUD_Y_START = BUILDER.comment("y value at which cloud fog starts").define("y_start", 100);
		CLOUD_Y_END = BUILDER.comment("y value at which cloud fog ends").define("y_end", 275);
		CLOUD_START_START = BUILDER.comment("the cloud fog's start distance from the player when in the middle of the cloud fog (in between the start and end value)").define("max_fog_start", 0);
		CLOUD_END_END = BUILDER.comment("the cloud fog's end distance from the player when in the middle of the cloud fog (in between the start and end value)").define("max_fog_end", 80);
		CLOUD_MAX_OPACITY = BUILDER.comment("the cloud fog's opacity multiplier when in the middle of the cloud fog (in between the start and end value)").define("max_opacity_factor", 3.0);
		BUILDER.pop();

		BUILDER.push("rain_fog");
		thunder_enabled = BUILDER.define("enabled", true);
		RAIN_OPACITY_FACTOR = BUILDER.comment("the rain fog's opacity multiplier").define("opacity_factor", 2.0);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	 */

}
