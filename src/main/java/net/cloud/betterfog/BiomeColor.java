package net.cloud.betterfog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class BiomeColor {
    static double v;
    static int r;
    static int checks;
    static int skip;


    /*
    public static List<Double> getColor(Level level, BlockPos pos) {
        v = net.cloud.betterfog.forge.BetterFogConfiguration.BIOME_CHECK_VERTICAL_FACTOR;
        r = net.cloud.betterfog.forge.BetterFogConfiguration.BIOME_CHECK_RADIUS;
        checks = net.cloud.betterfog.forge.BetterFogConfiguration.BIOME_CHECKS_FOR_SIDE;
        skip = (int) Math.round((r * 2) / (double) (checks));
        return getColorAverage(level, pos);

    }
    */

    public static List<Double> getColorAverage(Level level, BlockPos pos) {
        int x = pos.getX(); int y = pos.getY(); int z = pos.getZ();

        int ix; int iz; int iy;

        int red = 0; int green = 0; int blue = 0; double alphaM = 0; double startM = 0; double endM = 0;

        for (ix = -r; ix < r; ix += skip) {
            for (iy = (int) Math.round((-r * v)); iy < (int) Math.round((r * v)); iy += skip) {
                for (iz = -r; iz < r; iz += skip) {
                    List<Double> features = getBiomeFeatures(level, new BlockPos(x + ix, y + iy, z + iz));
                    red += features.get(0);
                    green += features.get(1);
                    blue += features.get(2);
                    alphaM += features.get(3);
                    startM += features.get(4);
                    endM += features.get(5);
                }
            }
        }
        double tot = checks * checks * Math.round(checks * v);
        return List.of((red / tot),(green / tot), (blue / tot), (alphaM / tot), (startM / tot), (endM / tot));
    }
    public static List<Double> getBiomeFeatures(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        List<Double> output = new ArrayList<>();

        //Biomes list
        for (String category_color : List.of("")) {

            if (category_color.startsWith(biome.unwrapKey().get().location().toString()) || category_color.startsWith(level.dimension().location().toString())) {
                String[] input = category_color.split(";");
                for (int i = 1; i < input.length; i++) {
                    if (i == 1) {
                        String hexColor = input[i];
                        double red = Integer.parseInt(hexColor.substring(0, 2), 16);
                        double green = Integer.parseInt(hexColor.substring(2, 4), 16);
                        double blue = Integer.parseInt(hexColor.substring(4, 6), 16);
                        output.add(red);
                        output.add(green);
                        output.add(blue);
                    } else {
                        output.add(Double.parseDouble(input[i]));
                    }
                }
                return output;
            }
        }
        return List.of(255.0, 255.0, 255.0, 1.0, 1.0, 1.0);
    }

    public static float getTimeOfDayValue(Level world) {
        long time = world.getDayTime() % 24000; // Get the current time of day

        // Define the time ranges for different values
        long startDay = 2000;
        long endDay = 11000;
        long startTransition = 11000;
        long endTransition = 14500;
        long endNight = 21500;

        // Determine the current value based on the time range
        float value;

        if (time < startDay) {
            return 25f/45f - time / 4500f;
        } else if (time < endDay) {
            value = 0f;
        } else if (time < endTransition) {
            value = (float) (time - startTransition) / (float) (endTransition - startTransition);
        } else if (time < endNight) {
            value = 1f;
        } else {
            value = 20f/45f + (24000f - time) / 4500f;
        }

        return value;
    }

    public static double fromTo (double a, double b, double f) {
        if (f >= 1d) {
            return b;
        } else if (f <= 0d) {
            return a;
        } else {
            return a + (b - a) * f;
        }
    }
}