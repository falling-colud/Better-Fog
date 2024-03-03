package net.cloud.better_fog.configuration;


import java.util.ArrayList;

public class BetterFogConfiguration {
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
	public static final ForgeConfigSpec.ConfigValue<Boolean> CLOUD_ENABLED;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_Y_START;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_START_START;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_Y_END;
	public static final ForgeConfigSpec.ConfigValue<Integer> CLOUD_END_END;
	public static final ForgeConfigSpec.ConfigValue<Double> CLOUD_MAX_OPACITY;
	public static final ForgeConfigSpec.ConfigValue<Double> RAIN_OPACITY_FACTOR;
	public static final ForgeConfigSpec.ConfigValue<Boolean> RAIN_ENABLED;
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
		RAIN_ENABLED = BUILDER.define("enabled", true);
		RAIN_OPACITY_FACTOR = BUILDER.comment("the rain fog's opacity multiplier").define("opacity_factor", 2.0);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}
