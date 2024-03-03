package net.cloud.better_fog.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.cloud.better_fog.BiomeColor;
import net.cloud.better_fog.Sigmoid;
import net.cloud.better_fog.Vector3f;
import net.cloud.better_fog.configuration.BetterFogConfiguration;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(FogRenderer.class)
public class BetterFogProcedure {
	private enum Stage {
		NONE, TICKS, FOG, FOGCOLOR, CLOUDS, SKY, WEATHER, PARTICLES
	}

	private enum Set {
		NONE, X, Y, Z, FANCY, FAST, SUN, MOON, RAIN, SNOW, PARTICLES_AND_SOUNDS, PARTICLES_ONLY, SOUNDS_ONLY, VANILLA_PARTICLES_AND_SOUNDS, VANILLA_PARTICLES_ONLY, VANILLA_SOUNDS_ONLY, START, END
	}

	private enum Color {
		RGBA, HSLA, RED, GREEN, BLUE, HUE, SATURATION, LIGHTNESS, ALPHA
	}

	private static Stage _stage = Stage.NONE;
	private static Minecraft _minecraft = null;
	private static ClientLevel _clientLevel = null;
	private static Field _effects = null;
	private static ViewportEvent.RenderFog _fog = null;
	private static ViewportEvent.ComputeFogColor _fogColor = null;
	private static LightTexture _lightTexture = null;
	private static PoseStack _poseStack = null;
	private static Matrix4f _projectionMatrix = null;
	private static float _partialTick = 0.0F;
	private static int _ticks = 0;
	private static Camera _camera = null;
	private static double _camX = 0.0D;
	private static double _camY = 0.0D;
	private static double _camZ = 0.0D;
	private static final Map<String, Object> _PARAMS = new HashMap<String, Object>() {
		{
			put("x", 0.0D);
			put("y", 0.0D);
			put("z", 0.0D);
			put("vx", 0.0D);
			put("vy", 0.0D);
			put("vz", 0.0D);
			put("yaw", 0.0F);
			put("pitch", 0.0F);
			put("roll", 0.0F);
			put("width", 21.0D);
			put("depth", 21.0D);
			put("height", 21.0D);
			put("seed", 0);
			put("texture", new ResourceLocation("minecraft", ""));
			put("down", new ResourceLocation("minecraft", ""));
			put("up", new ResourceLocation("minecraft", ""));
			put("north", new ResourceLocation("minecraft", ""));
			put("south", new ResourceLocation("minecraft", ""));
			put("west", new ResourceLocation("minecraft", ""));
			put("east", new ResourceLocation("minecraft", ""));
			put("u0", 0.0F);
			put("v0", 0.0F);
			put("u1", 1.0F);
			put("v1", 1.0F);
			put("size", 100.0F);
			put("weatherEffects", true);
			put("particle", ParticleTypes.RAIN);
			put("pvx", 0.0D);
			put("pvy", 0.0D);
			put("pvz", 0.0D);
			put("sound", SoundEvents.WEATHER_RAIN);
			put("soundSource", SoundSource.WEATHER);
			put("soundLevel", 0.2F);
			put("soundPitch", 1.0F);
			put("model", Color.RGBA);
			put("red", 255.0F);
			put("green", 255.0F);
			put("blue", 255.0F);
			put("hue", 0.0F);
			put("saturation", 0.0F);
			put("lightness", 100.0F);
			put("alpha", 100.0F);
		}
	};

	private static Map<String, Object> clone(Map<String, Object> map) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static float[] getColor(Map<String, Object> map) {
		Color model = (Color) map.get("model");
		float[] result = new float[4];
		if (model == Color.RGBA) {
			result[0] = (Float) map.get("red") / 255.0F;
			result[1] = (Float) map.get("green") / 255.0F;
			result[2] = (Float) map.get("blue") / 255.0F;
		} else if (model == Color.HSLA) {
			float[] color = HSLtoRGB(map);
			result[0] = color[0];
			result[1] = color[1];
			result[2] = color[2];
		}
		result[3] = (Float) map.get("alpha") / 100.0F;
		if (result[0] < 0.0F) {
			result[0] = 0.0F;
		} else if (result[0] > 1.0F) {
			result[0] = 1.0F;
		}
		if (result[1] < 0.0F) {
			result[1] = 0.0F;
		} else if (result[1] > 1.0F) {
			result[1] = 1.0F;
		}
		if (result[2] < 0.0F) {
			result[2] = 0.0F;
		} else if (result[2] > 1.0F) {
			result[2] = 1.0F;
		}
		if (result[3] < 0.0F) {
			result[3] = 0.0F;
		} else if (result[3] > 1.0F) {
			result[3] = 1.0F;
		}
		return result;
	}

	private static float[] HSLtoRGB(Map<String, Object> map) {
		float hue = (Float) map.get("hue");
		float saturation = (Float) map.get("saturation");
		float lightness = (Float) map.get("lightness");
		float[] result = new float[3];
		saturation /= 100.0F;
		lightness /= 100.0F;
		if (hue < 0.0F) {
			hue = hue % 360.0F + 360.0F;
		} else if (hue >= 360.0F) {
			hue %= 360.0F;
		}
		if (saturation < 0.0F) {
			saturation = 0.0F;
		} else if (saturation > 1.0F) {
			saturation = 1.0F;
		}
		if (lightness < 0.0F) {
			lightness = 0.0F;
		} else if (lightness > 1.0F) {
			lightness = 1.0F;
		}
		float c = (1.0F - Math.abs(lightness * 2.0F - 1.0F)) * saturation;
		float x = c * (1.0F - Math.abs((hue / 60.0F) % 2.0F - 1.0F));
		float m = lightness - c / 2.0F;
		if (hue < 60.0F) {
			result[0] = c;
			result[1] = x;
			result[2] = 0.0F;
		} else if (hue >= 60.0F && hue < 120.0F) {
			result[0] = x;
			result[1] = c;
			result[2] = 0.0F;
		} else if (hue >= 120.0F && hue < 180.0F) {
			result[0] = 0.0F;
			result[1] = c;
			result[2] = x;
		} else if (hue >= 180.0F && hue < 240.0F) {
			result[0] = 0.0F;
			result[1] = x;
			result[2] = c;
		} else if (hue >= 240.0F && hue < 300.0F) {
			result[0] = x;
			result[1] = 0.0F;
			result[2] = c;
		} else if (hue >= 300.0F) {
			result[0] = c;
			result[1] = 0.0F;
			result[2] = x;
		}
		result[0] += m;
		result[1] += m;
		result[2] += m;
		return result;
	}

	private static float[] RGBToHSL(Map<String, Object> map) {
		float red = (Float) map.get("red") / 255.0F;
		float green = (Float) map.get("green") / 255.0F;
		float blue = (Float) map.get("blue") / 255.0F;
		float max = Math.max(red, Math.max(green, blue));
		float min = Math.min(red, Math.min(green, blue));
		float delta = max - min;
		float[] result = new float[3];
		if (max == red) {
			result[0] = 60.0F * (((green - blue) / delta) % 6.0F);
		} else if (max == green) {
			result[0] = 60.0F * (((blue - red) / delta) + 2.0F);
		} else if (max == blue) {
			result[0] = 60.0F * (((red - green) / delta) + 4.0F);
		}
		result[2] = (max + min) / 2.0F;
		if (delta != 0.0F) {
			result[1] = (delta / (1.0F - Math.abs(result[2] * 2.0F - 1.0F))) * 100.0F;
		}
		result[2] *= 100.0F;
		return result;
	}

	private static Map<String, Object> RGBAMap(float[] color) {
		Map<String, Object> result = clone(_PARAMS);
		result.put("model", Color.RGBA);
		result.put("red", color[0]);
		result.put("green", color[1]);
		result.put("blue", color[2]);
		result.put("alpha", color[3]);
		return result;
	}

	private static Map<String, Object> HSLAMap(float[] color) {
		Map<String, Object> result = clone(_PARAMS);
		result.put("model", Color.HSLA);
		result.put("hue", color[0]);
		result.put("saturation", color[1]);
		result.put("lightness", color[2]);
		result.put("alpha", color[3]);
		return result;
	}

	private static int[] getTextureSize(Map<String, Object> map) {
		int[] result = new int[2];
		try {
			InputStream stream = Minecraft.getInstance().getResourceManager().open((ResourceLocation) map.get("texture"));
			NativeImage image = NativeImage.read(stream);
			result[0] = image.getWidth();
			result[1] = image.getHeight();
			image.close();
		} catch (Exception e) {
			result[0] = 0;
			result[1] = 0;
		}
		return result;
	}

	private static Map<String, Object> setTexture(Map<String, Object> map, String id, String path, Direction direction) {
		ResourceLocation texture = new ResourceLocation(id, "textures/" + path);
		if (direction == null) {
			map.put("texture", texture);
		} else {
			if (direction == Direction.DOWN) {
				map.put("down", texture);
			} else if (direction == Direction.UP) {
				map.put("up", texture);
			} else if (direction == Direction.NORTH) {
				map.put("north", texture);
			} else if (direction == Direction.SOUTH) {
				map.put("south", texture);
			} else if (direction == Direction.WEST) {
				map.put("west", texture);
			} else if (direction == Direction.EAST) {
				map.put("east", texture);
			}
		}
		return map;
	}

	private static boolean renderable(Stage stage) {
		boolean result = false;
		if (_stage == stage) {
			result = true;
		}
		return result;
	}

	private static boolean isBrightLight(DimensionType dimension) {
		DimensionSpecialEffects effects = DimensionSpecialEffects.forType(dimension);
		return effects.forceBrightLightmap();
	}

	private static float getDayLevel() {
		return _clientLevel.getTimeOfDay(_partialTick);
	}

	private static float getRainLevel() {
		return _clientLevel.getRainLevel(_partialTick);
	}

	private static float getThunderLevel() {
		return _clientLevel.getThunderLevel(_partialTick);
	}

	private static boolean rainnable(double x, double y, double z) {
		Level level = (Level) _clientLevel;
		int xi = (int) Math.floor(x);
		int yi = (int) Math.floor(y);
		int zi = (int) Math.floor(z);
		BlockPos pos = new BlockPos(xi, yi, zi);
		Biome biome = level.getBiome(pos).value();
		boolean result = false;
		result = biome.getPrecipitationAt(pos) != Biome.Precipitation.NONE;
		result = result && biome.warmEnoughToRain(pos);
		return result;
	}

	private static boolean snowable(double x, double y, double z) {
		Level level = (Level) _clientLevel;
		int xi = (int) Math.floor(x);
		int yi = (int) Math.floor(y);
		int zi = (int) Math.floor(z);
		BlockPos pos = new BlockPos(xi, yi, zi);
		Biome biome = level.getBiome(pos).value();
		boolean result = false;
		result = biome.getPrecipitationAt(pos) != Biome.Precipitation.NONE;
		result = result && biome.coldEnoughToSnow(pos);
		return result;
	}

	private static int getWorldHeight(double x, double z) {
		Level level = (Level) _clientLevel;
		int xi = (int) Math.floor(x);
		int zi = (int) Math.floor(z);
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, xi, zi);
	}

	private static double getCloudPosition(DimensionType dimension, Set component) {
		double result = 0.0D;
		if (component == Set.Y) {
			DimensionSpecialEffects effects = DimensionSpecialEffects.forType(dimension);
			result = effects.getCloudHeight();
			if (Double.isNaN(result)) {
				result = 0.0D;
			} else {
				result += 0.33D;
			}
		} else if (component == Set.Z) {
			result = -3.9625;
		}
		return result;
	}

	private static float getCloudColor(Color component) {
		Vector3f RGB = new Vector3f(_clientLevel.getCloudColor(_partialTick));
		float result = 0.0F;
		if (component == Color.RED || component == Color.GREEN || component == Color.BLUE) {
			if (component == Color.RED) {
				result = RGB.x() * 255.0F;
			} else if (component == Color.GREEN) {
				result = RGB.y() * 255.0F;
			} else if (component == Color.BLUE) {
				result = RGB.z() * 255.0F;
			}
		} else if (component == Color.HUE || component == Color.SATURATION || component == Color.LIGHTNESS) {
			float[] HSLA = RGBToHSL(RGBAMap(new float[]{RGB.x() * 255.0F, RGB.y() * 255.0F, RGB.z() * 255.0F, 100.0F}));
			if (component == Color.HUE) {
				result = HSLA[0];
			} else if (component == Color.SATURATION) {
				result = HSLA[1];
			} else if (component == Color.LIGHTNESS) {
				result = HSLA[2];
			}
		} else if (component == Color.ALPHA) {
			result = 80.0F;
		}
		return result;
	}

	private static float getSkyColor(Color component) {
		Vector3f RGB = new Vector3f(_clientLevel.getSkyColor(_camera.getPosition(), _partialTick));
		float result = 0.0F;
		if (component == Color.RED || component == Color.GREEN || component == Color.BLUE) {
			if (component == Color.RED) {
				result = RGB.x() * 255.0F;
			} else if (component == Color.GREEN) {
				result = RGB.y() * 255.0F;
			} else if (component == Color.BLUE) {
				result = RGB.z() * 255.0F;
			}
		} else if (component == Color.HUE || component == Color.SATURATION || component == Color.LIGHTNESS) {
			float[] HSLA = RGBToHSL(RGBAMap(new float[]{RGB.x() * 255.0F, RGB.y() * 255.0F, RGB.z() * 255.0F, 100.0F}));
			if (component == Color.HUE) {
				result = HSLA[0];
			} else if (component == Color.SATURATION) {
				result = HSLA[1];
			} else if (component == Color.LIGHTNESS) {
				result = HSLA[2];
			}
		} else if (component == Color.ALPHA) {
			result = 100.0F;
		}
		return result;
	}

	private static float getSunlightColor(Color component) {
		float[] RGBA = _clientLevel.effects().getSunriseColor(getDayLevel(), _partialTick);
		float result = 0.0F;
		if (RGBA != null) {
			if (component == Color.RED || component == Color.GREEN || component == Color.BLUE) {
				if (component == Color.RED) {
					result = RGBA[0] * 255.0F;
				} else if (component == Color.GREEN) {
					result = RGBA[1] * 255.0F;
				} else if (component == Color.BLUE) {
					result = RGBA[2] * 255.0F;
				}
			} else if (component == Color.HUE || component == Color.SATURATION || component == Color.LIGHTNESS) {
				float[] HSLA = RGBToHSL(RGBAMap(new float[]{RGBA[0] * 255.0F, RGBA[1] * 255.0F, RGBA[2] * 255.0F, RGBA[3] * 100.0F}));
				if (component == Color.HUE) {
					result = HSLA[0];
				} else if (component == Color.SATURATION) {
					result = HSLA[1];
				} else if (component == Color.LIGHTNESS) {
					result = HSLA[2];
				}
			} else if (component == Color.ALPHA) {
				result = RGBA[3] * 100.0F;
			}
		}
		return result;
	}

	private static float getStarColor(Color component) {
		float rainLevel = 1.0F - getRainLevel();
		float color = _clientLevel.getStarBrightness(_partialTick) * rainLevel;
		float result = 0.0F;
		if (component == Color.RED || component == Color.GREEN || component == Color.BLUE) {
			result = color * 255.0F;
		} else if (component == Color.HUE || component == Color.SATURATION || component == Color.LIGHTNESS) {
			float[] HSLA = RGBToHSL(RGBAMap(new float[]{color * 255.0F, color * 255.0F, color * 255.0F, color * 100.0F}));
			if (component == Color.HUE) {
				result = HSLA[0];
			} else if (component == Color.SATURATION) {
				result = HSLA[1];
			} else if (component == Color.LIGHTNESS) {
				result = HSLA[2];
			}
		} else if (component == Color.ALPHA) {
			result = color * 100.0F;
		}
		return result;
	}

	private static boolean isShelter(double x, double y, double z) {
		RandomSource random = RandomSource.create();
		double d0 = getWorldHeight(x, z);
		double rx = x + random.nextDouble() * 20.0D - 10.0D;
		double rz = z + random.nextDouble() * 20.0D - 10.0D;
		double d1 = getWorldHeight(rx, rz);
		boolean result = false;
		if (y < d0 && y + 2.0D < d1) {
			result = true;
		}
		return result;
	}

	private static double getWeatherWDH() {
		double result = 0.0D;
		if (Minecraft.useFancyGraphics()) {
			result = 21.0D;
		} else {
			result = 11.0D;
		}
		return result;
	}

	private static List<Vec3> getWeatherArea(Map<String, Object> map) {
		Level level = (Level) _clientLevel;
		List<Vec3> pos = new ArrayList<Vec3>();
		int width = (int) Math.floor((Double) map.get("width"));
		int min0 = (int) -Math.floor(width / 2.0D);
		int max0 = min0 + (int) width - 1;
		double height = (Double) map.get("height");
		int depth = (int) Math.floor((Double) map.get("depth"));
		int min1 = (int) -Math.floor(depth / 2.0D);
		int max1 = min1 + (int) depth - 1;
		double x = Math.floor((Double) map.get("x"));
		double y = (Double) map.get("y") - Math.floor(height / 2.0D);
		double z = Math.floor((Double) map.get("z"));
		for (int i = min0; i <= max0; ++i) {
			for (int j = min1; j <= max1; ++j) {
				if (!(i == 0 && j == 0)) {
					double sqx = x + i;
					double sqy = y;
					double sqz = z + j;
					pos.add(new Vec3(sqx + 0.5D, sqy, sqz + 0.5D));
				}
			}
		}
		return pos;
	}

	private static float getFogDistance(Set type) {
		float result = 0.0F;
		if (type == Set.START) {
			result = RenderSystem.getShaderFogStart();
		} else if (type == Set.END) {
			result = RenderSystem.getShaderFogEnd();
		}
		return result;
	}

	private static float getFogColor(Color component) {
		float result = 0.0F;
		FogRenderer.levelFogColor();
		float[] RGBA = RenderSystem.getShaderFogColor();
		if (component == Color.RED || component == Color.GREEN || component == Color.BLUE) {
			if (component == Color.RED) {
				result = RGBA[0] * 255.0F;
			} else if (component == Color.GREEN) {
				result = RGBA[1] * 255.0F;
			} else {
				result = RGBA[2] * 255.0F;
			}
		} else if (component == Color.HUE || component == Color.SATURATION || component == Color.LIGHTNESS) {
			float[] HSLA = RGBToHSL(RGBAMap(new float[]{RGBA[0] * 255.0F, RGBA[1] * 255.0F, RGBA[2] * 255.0F, 100.0F}));
			if (component == Color.HUE) {
				result = HSLA[0];
			} else if (component == Color.SATURATION) {
				result = HSLA[1];
			} else {
				result = HSLA[2];
			}
		} else if (component == Color.ALPHA) {
			result = 100.0F;
		}
		return result;
	}

	private static void renderClouds(Map<String, Object> map, Set type, double dx, double dz) {
		if (renderable(Stage.CLOUDS)) {
			CloudStatus cloudsType = _minecraft.options.getCloudsType();
			float[] color = getColor(map);
			float red = color[0];
			float green = color[1];
			float blue = color[2];
			float alpha = color[3];
			boolean b0 = true;
			if (type == Set.NONE) {
				if (cloudsType == CloudStatus.OFF) {
					b0 = false;
				}
			}
			int[] size = getTextureSize(map);
			if (size[0] == 0 || size[1] == 0) {
				b0 = false;
			}
			if (dx <= 0.0D || dz <= 0.0D) {
				b0 = false;
			}
			float width = (float) (double) (Double) map.get("width");
			float height = (float) (double) (Double) map.get("height");
			float depth = (float) (double) (Double) map.get("depth");
			if (width <= 0.0F || height < 0.0F || depth <= 0.0F) {
				b0 = false;
			}
			if (b0 && alpha > 0.0F) {
				double x = (Double) map.get("x");
				double y = (Double) map.get("y");
				double z = (Double) map.get("z");
				double vx = (Double) map.get("vx");
				double vy = (Double) map.get("vy");
				double vz = (Double) map.get("vz");
				double d0 = (_ticks + _partialTick) * 0.03D;
				float lx = (float) ((x - _camX + d0 * vx) % (width * size[0]));
				float ly = (float) ((y - _camY));
				float lz = (float) ((z - _camZ + d0 * vz) % (depth * size[1]));
				float f0 = 1.0F / width / size[0];
				float f1 = 1.0F / depth / size[1];
				float u = -lx * f0;
				float v = -lz * f1;
				int dxi = (int) Math.floor(dx);
				int dzi = (int) Math.floor(dz);
				int qx = 0;
				int qz = 0;
				boolean b1 = false;
				if (type == Set.NONE) {
					if (cloudsType == CloudStatus.FANCY) {
						b1 = true;
					}
				} else if (type == Set.FANCY) {
					b1 = true;
				}
				RenderSystem.disableCull();
				RenderSystem.depthMask(true);
				RenderSystem.enableDepthTest();
				RenderSystem.enableBlend();
				// RenderSystem.enableTexture();
				RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
				RenderSystem.setShaderTexture(0, (ResourceLocation) map.get("texture"));
				BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
				bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
				if (b1) {
					int i0 = (int) Math.floor(dxi / width) + 2;
					int i1 = (int) Math.floor(dzi / depth) + 2;
					float f2 = lx % width;
					float f3 = lz % depth;
					qx = dxi / 48;
					qz = dzi / 48;
					if (qx < 10) {
						qx = 10;
					}
					if (qz < 10) {
						qz = 10;
					}
					float red0 = red * 0.9F;
					float green0 = green * 0.9F;
					float blue0 = blue * 0.9F;
					float red1 = red * 0.8F;
					float green1 = green * 0.8F;
					float blue1 = blue * 0.8F;
					float red2 = red * 0.7F;
					float green2 = green * 0.7F;
					float blue2 = blue * 0.7F;
					for (int i = 0; i < qx; ++i) {
						for (int j = 0; j < qz; ++j) {
							float f4 = -dxi + ((float) i / qx) * dxi * 2.0F;
							float f5 = -dxi + ((float) (i + 1) / qx) * dxi * 2.0F;
							float f6 = -dzi + ((float) j / qz) * dzi * 2.0F;
							float f7 = -dzi + ((float) (j + 1) / qz) * dzi * 2.0F;
							if (ly <= 1.0F) {
								bufferBuilder.vertex(f4, ly + height, f7).uv(u + f4 * f0, v + f7 * f1).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
								bufferBuilder.vertex(f5, ly + height, f7).uv(u + f5 * f0, v + f7 * f1).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
								bufferBuilder.vertex(f5, ly + height, f6).uv(u + f5 * f0, v + f6 * f1).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
								bufferBuilder.vertex(f4, ly + height, f6).uv(u + f4 * f0, v + f6 * f1).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
							}
							if (ly >= -height - 1.0F) {
								bufferBuilder.vertex(f4, ly, f7).uv(u + f4 * f0, v + f7 * f1).color(red2, green2, blue2, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
								bufferBuilder.vertex(f5, ly, f7).uv(u + f5 * f0, v + f7 * f1).color(red2, green2, blue2, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
								bufferBuilder.vertex(f5, ly, f6).uv(u + f5 * f0, v + f6 * f1).color(red2, green2, blue2, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
								bufferBuilder.vertex(f4, ly, f6).uv(u + f4 * f0, v + f6 * f1).color(red2, green2, blue2, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
							}
						}
					}
					if (height > 0.0F) {
						for (int i = -i0; i < i0; ++i) {
							for (int j = 0; j < qz; ++j) {
								float f4 = i * width + f2;
								float f5 = i * width + width + f2;
								float f6 = i * width + width / 2.0F + f2;
								float f7 = -dzi + ((float) (j + 1) / qz) * dzi * 2.0F;
								float f8 = -dzi + ((float) j / qz) * dzi * 2.0F;
								if (f4 >= -dxi && f4 <= dxi) {
									bufferBuilder.vertex(f4, ly, f7).uv(u + f6 * f0, v + f7 * f1).color(red0, green0, blue0, alpha).normal(-1.0F, 0.0F, 0.0F).endVertex();
									bufferBuilder.vertex(f4, ly + height, f7).uv(u + f6 * f0, v + f7 * f1).color(red0, green0, blue0, alpha).normal(-1.0F, 0.0F, 0.0F).endVertex();
									bufferBuilder.vertex(f4, ly + height, f8).uv(u + f6 * f0, v + f8 * f1).color(red0, green0, blue0, alpha).normal(-1.0F, 0.0F, 0.0F).endVertex();
									bufferBuilder.vertex(f4, ly, f8).uv(u + f6 * f0, v + f8 * f1).color(red0, green0, blue0, alpha).normal(-1.0F, 0.0F, 0.0F).endVertex();
								}
								if (f5 >= -dxi && f5 <= dxi) {
									bufferBuilder.vertex(f5, ly, f7).uv(u + f6 * f0, v + f7 * f1).color(red0, green0, blue0, alpha).normal(1.0F, 0.0F, 0.0F).endVertex();
									bufferBuilder.vertex(f5, ly + height, f7).uv(u + f6 * f0, v + f7 * f1).color(red0, green0, blue0, alpha).normal(1.0F, 0.0F, 0.0F).endVertex();
									bufferBuilder.vertex(f5, ly + height, f8).uv(u + f6 * f0, v + f8 * f1).color(red0, green0, blue0, alpha).normal(1.0F, 0.0F, 0.0F).endVertex();
									bufferBuilder.vertex(f5, ly, f8).uv(u + f6 * f0, v + f8 * f1).color(red0, green0, blue0, alpha).normal(1.0F, 0.0F, 0.0F).endVertex();
								}
							}
						}
						for (int i = -i1; i < i1; ++i) {
							for (int j = 0; j < qx; ++j) {
								float f4 = i * depth + f3;
								float f5 = i * depth + depth + f3;
								float f6 = i * depth + depth / 2.0F + f3;
								float f7 = -dxi + ((float) j / qx) * dxi * 2.0F;
								float f8 = -dxi + ((float) (j + 1) / qx) * dxi * 2.0F;
								if (f4 >= -dzi && f4 <= dzi) {
									bufferBuilder.vertex(f7, ly + height, f4).uv(u + f7 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, -1.0F).endVertex();
									bufferBuilder.vertex(f8, ly + height, f4).uv(u + f8 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, -1.0F).endVertex();
									bufferBuilder.vertex(f8, ly, f4).uv(u + f8 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, -1.0F).endVertex();
									bufferBuilder.vertex(f7, ly, f4).uv(u + f7 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, -1.0F).endVertex();
								}
								if (f5 >= -dzi && f5 <= dzi) {
									bufferBuilder.vertex(f7, ly + height, f5).uv(u + f7 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, 1.0F).endVertex();
									bufferBuilder.vertex(f8, ly + height, f5).uv(u + f8 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, 1.0F).endVertex();
									bufferBuilder.vertex(f8, ly, f5).uv(u + f8 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, 1.0F).endVertex();
									bufferBuilder.vertex(f7, ly, f5).uv(u + f7 * f0, v + f6 * f1).color(red1, green1, blue1, alpha).normal(0.0F, 0.0F, 1.0F).endVertex();
								}
							}
						}
					}
				} else {
					qx = dxi / 240;
					qz = dzi / 240;
					if (qx < 2) {
						qx = 2;
					}
					if (qz < 2) {
						qz = 2;
					}
					for (int i = 0; i < qx; ++i) {
						for (int j = 0; j < qz; ++j) {
							float f2 = -dxi + ((float) i / qx) * dxi * 2.0F;
							float f3 = -dxi + ((float) (i + 1) / qx) * dxi * 2.0F;
							float f4 = -dzi + ((float) j / qz) * dzi * 2.0F;
							float f5 = -dzi + ((float) (j + 1) / qz) * dzi * 2.0F;
							bufferBuilder.vertex(f2, ly, f5).uv(u + f2 * f0, v + f5 * f1).color(red, green, blue, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
							bufferBuilder.vertex(f3, ly, f5).uv(u + f3 * f0, v + f5 * f1).color(red, green, blue, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
							bufferBuilder.vertex(f3, ly, f4).uv(u + f3 * f0, v + f4 * f1).color(red, green, blue, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
							bufferBuilder.vertex(f2, ly, f4).uv(u + f2 * f0, v + f4 * f1).color(red, green, blue, alpha).normal(0.0F, -1.0F, 0.0F).endVertex();
						}
					}
				}
				VertexBuffer cloudBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
				cloudBuffer.bind();
				cloudBuffer.upload(bufferBuilder.end());
				Matrix4f matrix4f = _poseStack.last().pose();
				ShaderInstance shaderInstance = RenderSystem.getShader();
				if (b1) {
					RenderSystem.colorMask(false, false, false, false);
					cloudBuffer.drawWithShader(matrix4f, _projectionMatrix, shaderInstance);
					RenderSystem.colorMask(true, true, true, true);
					cloudBuffer.drawWithShader(matrix4f, _projectionMatrix, shaderInstance);
				} else {
					RenderSystem.colorMask(true, true, true, true);
					cloudBuffer.drawWithShader(matrix4f, _projectionMatrix, shaderInstance);
				}
				cloudBuffer.close();
				// RenderSystem.disableTexture();
				RenderSystem.disableBlend();
				RenderSystem.disableDepthTest();
				RenderSystem.depthMask(false);
				RenderSystem.enableCull();
			}
		}
	}

	private static void renderSkyBox(Map<String, Object> map, boolean single) {
		if (renderable(Stage.SKY)) {
			float[] color = getColor(map);
			float red = color[0];
			float green = color[1];
			float blue = color[2];
			float alpha = color[3];
			if (alpha > 0.0F) {
				ResourceLocation texture = (ResourceLocation) map.get("texture");
				float u0 = (Float) map.get("u0");
				float v0 = (Float) map.get("v0");
				float u1 = (Float) map.get("u1");
				float v1 = (Float) map.get("v1");
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				// RenderSystem.enableTexture();
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderColor(red, green, blue, alpha);
				BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
				for (int i = 0; i < 6; ++i) {
					if (!single) {
						if (i == 0) {
							texture = (ResourceLocation) map.get("down");
						} else if (i == 1) {
							texture = (ResourceLocation) map.get("up");
						} else if (i == 2) {
							texture = (ResourceLocation) map.get("north");
						} else if (i == 3) {
							texture = (ResourceLocation) map.get("east");
						} else if (i == 4) {
							texture = (ResourceLocation) map.get("south");
						} else if (i == 5) {
							texture = (ResourceLocation) map.get("west");
						}
					}
					RenderSystem.setShaderTexture(0, texture);
					_poseStack.pushPose();
					_poseStack.mulPose(Vector3f.ZP.rotationDegrees((Float) map.get("yaw")));
					_poseStack.mulPose(Vector3f.XP.rotationDegrees((Float) map.get("pitch")));
					_poseStack.mulPose(Vector3f.YP.rotationDegrees((Float) map.get("roll")));
					if (i == 0) {
						_poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
					} else if (i == 1) {
						_poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
						_poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
					} else if (i == 2) {
						_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
					} else if (i == 3) {
						_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
						_poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90.0F));
					} else if (i == 4) {
						_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
						_poseStack.mulPose(Vector3f.ZP.rotationDegrees(-180.0F));
					} else if (i == 5) {
						_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
						_poseStack.mulPose(Vector3f.ZP.rotationDegrees(-270.0F));
					}
					Matrix4f matrix4f = _poseStack.last().pose();
					bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
					bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(u0, v0).endVertex();
					bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(u0, v1).endVertex();
					bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(u1, v1).endVertex();
					bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(u1, v0).endVertex();
					BufferUploader.drawWithShader(bufferBuilder.end());
					_poseStack.popPose();
				}
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				// RenderSystem.disableTexture();
				RenderSystem.disableBlend();
				RenderSystem.depthMask(true);
			}
		}
	}

	private static void renderPolygon(Map<String, Object> map, int vertex) {
		if (renderable(Stage.SKY)) {
			float[] color = getColor(map);
			float red = color[0];
			float green = color[1];
			float blue = color[2];
			float alpha = color[3];
			if (vertex >= 2 && alpha > 0.0F) {
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.setShader(GameRenderer::getPositionShader);
				RenderSystem.setShaderColor(red, green, blue, alpha);
				_poseStack.pushPose();
				_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
				_poseStack.mulPose(Vector3f.ZP.rotationDegrees((Float) map.get("yaw")));
				_poseStack.mulPose(Vector3f.XP.rotationDegrees((Float) map.get("pitch")));
				_poseStack.mulPose(Vector3f.YP.rotationDegrees((Float) map.get("roll")));
				Matrix4f matrix4f = _poseStack.last().pose();
				BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
				bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
				bufferBuilder.vertex(matrix4f, 0.0F, 16.0F, 0.0F).endVertex();
				float size = (Float) map.get("size") * 5.12F;
				float deg = 360.0F / vertex;
				for (int i = 0; i <= vertex; i++) {
					bufferBuilder.vertex(matrix4f, size * Mth.cos(deg * i * (Mth.PI / 180.0F)), 16.0F, size * Mth.sin(deg * i * (Mth.PI / 180.0F))).endVertex();
				}
				BufferUploader.drawWithShader(bufferBuilder.end());
				_poseStack.popPose();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
				RenderSystem.depthMask(true);
			}
		}
	}

	private static void renderSunlights(Map<String, Object> map) {
		if (renderable(Stage.SKY)) {
			float[] sunlightColor = _clientLevel.effects().getSunriseColor(getDayLevel(), _partialTick);
			if (sunlightColor != null) {
				float[] color = getColor(map);
				float red = color[0];
				float green = color[1];
				float blue = color[2];
				float alpha = color[3] * sunlightColor[3];
				if (alpha > 0.0F) {
					double d0 = Math.sin(_clientLevel.getSunAngle(_partialTick));
					float angle = 90.0F;
					if (d0 < 0.0D) {
						angle = 270.0F;
					}
					float f1 = (Float) map.get("size") * 0.4F;
					float f2 = f1 * 3.0F;
					RenderSystem.depthMask(false);
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					RenderSystem.setShader(GameRenderer::getPositionColorShader);
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					_poseStack.pushPose();
					_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
					_poseStack.mulPose(Vector3f.ZP.rotationDegrees((Float) map.get("yaw") + angle));
					_poseStack.mulPose(Vector3f.XP.rotationDegrees((Float) map.get("pitch")));
					_poseStack.mulPose(Vector3f.YP.rotationDegrees((Float) map.get("roll")));
					Matrix4f matrix4f = _poseStack.last().pose();
					BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
					bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
					bufferBuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(red, green, blue, alpha).endVertex();
					for (int i = 0; i <= 16; ++i) {
						float radian = (float) i * ((float) Math.PI * 2F) / 16.0F;
						float sin = Mth.sin(radian);
						float cos = Mth.cos(radian);
						bufferBuilder.vertex(matrix4f, sin * f2, cos * f2, -cos * f1 * alpha).color(red, green, blue, 0.0F).endVertex();
					}
					BufferUploader.drawWithShader(bufferBuilder.end());
					_poseStack.popPose();
					RenderSystem.disableBlend();
					RenderSystem.depthMask(true);
				}
			}
		}
	}

	private static void renderStars(Map<String, Object> map, int amount, int seed) {
		if (renderable(Stage.SKY)) {
			float[] color = getColor(map);
			float red = color[0];
			float green = color[1];
			float blue = color[2];
			float alpha = color[3];
			if ((Boolean) map.get("weatherEffects")) {
				alpha *= 1.0F - getRainLevel();
			}
			if (amount > 0 && alpha > 0.0F) {
				RandomSource random = RandomSource.create((long) seed);
				float start = RenderSystem.getShaderFogStart();
				RenderSystem.setShaderFogStart(Float.MAX_VALUE);
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.setShader(GameRenderer::getPositionShader);
				RenderSystem.setShaderColor(red, green, blue, alpha);
				_poseStack.pushPose();
				_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
				_poseStack.mulPose(Vector3f.ZP.rotationDegrees((Float) map.get("yaw")));
				_poseStack.mulPose(Vector3f.XP.rotationDegrees((Float) map.get("pitch")));
				_poseStack.mulPose(Vector3f.YP.rotationDegrees((Float) map.get("roll")));
				Matrix4f matrix4f = _poseStack.last().pose();
				BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
				bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
				for (int i = 0; i < amount; ++i) {
					float x0 = random.nextFloat() * 2.0F - 1.0F;
					float y0 = random.nextFloat() * 2.0F - 1.0F;
					float z0 = random.nextFloat() * 2.0F - 1.0F;
					float distance = x0 * x0 + y0 * y0 + z0 * z0;
					if (distance < 1.0F && distance > 0.01F) {
						distance = 1.0F / Mth.sqrt(distance);
						x0 *= distance;
						y0 *= distance;
						z0 *= distance;
						float x1 = x0 * 100.0F;
						float y1 = y0 * 100.0F;
						float z1 = z0 * 100.0F;
						float f0 = (float) Math.atan2(x0, z0);
						float sin0 = Mth.sin(f0);
						float cos0 = Mth.cos(f0);
						float f1 = (float) Math.atan2(Mth.sqrt(x0 * x0 + z0 * z0), y0);
						float sin1 = Mth.sin(f1);
						float cos1 = Mth.cos(f1);
						float f2 = random.nextFloat() * Mth.PI * 2.0F;
						float sin2 = Mth.sin(f2);
						float cos2 = Mth.cos(f2);
						float f3 = 0.15F + random.nextFloat() * 0.1F;
						for (int j = 0; j < 4; ++j) {
							float f4 = ((j & 2) - 1) * f3;
							float f5 = ((j + 1 & 2) - 1) * f3;
							float f6 = f4 * cos2 - f5 * sin2;
							float f7 = f5 * cos2 + f4 * sin2;
							float f8 = f6 * sin1;
							float f9 = f6 * -cos1;
							float f10 = f9 * sin0 - f7 * cos0;
							float f11 = f7 * sin0 + f9 * cos0;
							bufferBuilder.vertex(matrix4f, x1 + f10, y1 + f8, z1 + f11).endVertex();
						}
					}
				}
				BufferUploader.drawWithShader(bufferBuilder.end());
				_poseStack.popPose();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
				RenderSystem.depthMask(true);
				RenderSystem.setShaderFogStart(start);
			}
		}
	}

	private static void renderTexture(Map<String, Object> map, Set type, boolean specialBlends) {
		if (renderable(Stage.SKY)) {
			float[] color = getColor(map);
			float red = color[0];
			float green = color[1];
			float blue = color[2];
			float alpha = color[3];
			if ((Boolean) map.get("weatherEffects")) {
				alpha *= 1.0F - _clientLevel.getRainLevel(_partialTick);
			}
			if (alpha > 0.0F) {
				float u0 = (Float) map.get("u0");
				float v0 = (Float) map.get("v0");
				float u1 = (Float) map.get("u1");
				float v1 = (Float) map.get("v1");
				if (type == Set.SUN) {
					u0 = 0.0F;
					v0 = 0.0F;
					u1 = 1.0F;
					v1 = 1.0F;
				} else if (type == Set.MOON) {
					int phase = _clientLevel.getMoonPhase();
					int i0 = phase % 4;
					int i1 = phase / 4 % 2;
					u0 = i0 / 4.0F;
					v0 = i1 / 2.0F;
					u1 = (i0 + 1.0F) / 4.0F;
					v1 = (i1 + 1.0F) / 2.0F;
				}
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				if (specialBlends) {
					RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
				}
				// RenderSystem.enableTexture();
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderColor(red, green, blue, alpha);
				RenderSystem.setShaderTexture(0, (ResourceLocation) map.get("texture"));
				_poseStack.pushPose();
				if (type == Set.SUN || type == Set.MOON) {
					float f0 = 0.0F;
					if (type == Set.MOON) {
						f0 = 180.0F;
					}
					_poseStack.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
					_poseStack.mulPose(Vector3f.XP.rotationDegrees(_clientLevel.getTimeOfDay(_partialTick) * 360.0F + f0));
				} else {
					_poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
					_poseStack.mulPose(Vector3f.ZP.rotationDegrees((Float) map.get("yaw")));
					_poseStack.mulPose(Vector3f.XP.rotationDegrees((Float) map.get("pitch")));
					_poseStack.mulPose(Vector3f.YP.rotationDegrees((Float) map.get("roll")));
				}
				Matrix4f matrix4f = _poseStack.last().pose();
				float size = (Float) map.get("size") / 5.0F;
				BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
				bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
				bufferBuilder.vertex(matrix4f, -size, 100.0F, -size).uv(u0, v0).endVertex();
				bufferBuilder.vertex(matrix4f, size, 100.0F, -size).uv(u1, v0).endVertex();
				bufferBuilder.vertex(matrix4f, size, 100.0F, size).uv(u1, v1).endVertex();
				bufferBuilder.vertex(matrix4f, -size, 100.0F, size).uv(u0, v1).endVertex();
				BufferUploader.drawWithShader(bufferBuilder.end());
				_poseStack.popPose();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				// RenderSystem.disableTexture();
				RenderSystem.disableBlend();
				RenderSystem.depthMask(true);
			}
		}
	}

	private static void renderWeather(Map<String, Object> map, Set type, float length, float speed, boolean hitGround) {
		if (renderable(Stage.WEATHER)) {
			float camX = (float) _camX;
			float camY = (float) _camY;
			float camZ = (float) _camZ;
			float x = (float) (double) (Double) map.get("x");
			float y = (float) (double) (Double) map.get("y");
			float z = (float) (double) (Double) map.get("z");
			float lx = x - camX;
			float lz = z - camZ;
			float[] color = getColor(map);
			float red = color[0];
			float green = color[1];
			float blue = color[2];
			float alpha = color[3];
			float effects = 1.0F;
			if ((Boolean) map.get("weatherEffects")) {
				effects = _clientLevel.getRainLevel(_partialTick);
			}
			float factor = 5.0F;
			if (_minecraft.useFancyGraphics()) {
				factor = 10.0F;
			}
			factor = (float) Math.hypot(lx, lz) / factor;
			alpha *= effects * ((1.0F - factor * factor) * 0.5F + 0.5F);
			if (alpha > 0.0F) {
				Level level = (Level) _clientLevel;
				int pxi = (int) Math.floor(x);
				int pzi = (int) Math.floor(z);
				float min = y - camY;
				float max = min + length;
				int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pxi, pzi);
				boolean renderable = true;
				if (hitGround) {
					float f0 = height - camY;
					if (min < f0) {
						min = f0;
						if (min >= max) {
							renderable = false;
						}
					}
					if (max < f0) {
						renderable = false;
					}
				}
				if (renderable) {
					float rad = (float) (Math.PI / 2.0D - Math.atan2(lz, lx));
					float cos = Mth.cos(rad) * 0.5F;
					float sin = Mth.sin(rad) * 0.5F;
					int light = 15728640;
					if (hitGround) {
						int bottom = (int) Math.floor(y);
						if (bottom < height) {
							bottom = height;
						}
						int top = (int) Math.ceil(y + length);
						for (int i = bottom; i < top; ++i) {
							BlockPos pos = new BlockPos(pxi, i, pzi);
							boolean b0 = !level.getBlockState(pos).canOcclude();
							if (b0) {
								light = LevelRenderer.getLightColor((Level) _clientLevel, pos);
								break;
							}
						}
					}
					RandomSource random = RandomSource.create((long) (pxi * pxi * 3121 + pxi * 45238971 ^ pzi * pzi * 418711 + pzi * 13761));
					RenderSystem.depthMask(false);
					RenderSystem.enableDepthTest();
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					// RenderSystem.enableTexture();
					RenderSystem.setShader(GameRenderer::getParticleShader);
					RenderSystem.setShaderTexture(0, (ResourceLocation) map.get("texture"));
					_lightTexture.turnOnLightLayer();
					BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
					bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
					if (type == Set.RAIN) {
						int i0 = _ticks + pxi * pxi * 3121 + pxi * 45238971 + pzi * pzi * 418711 + pzi * 13761 & 31;
						float f0 = -(i0 + _partialTick) / 32.0F * (3.0F + random.nextFloat()) * speed;
						bufferBuilder.vertex(lx - cos, max, lz + sin).uv(0.0F, min * 0.25F + f0).color(red, green, blue, alpha).uv2(light).endVertex();
						bufferBuilder.vertex(lx + cos, max, lz - sin).uv(1.0F, min * 0.25F + f0).color(red, green, blue, alpha).uv2(light).endVertex();
						bufferBuilder.vertex(lx + cos, min, lz - sin).uv(1.0F, max * 0.25F + f0).color(red, green, blue, alpha).uv2(light).endVertex();
						bufferBuilder.vertex(lx - cos, min, lz + sin).uv(0.0F, max * 0.25F + f0).color(red, green, blue, alpha).uv2(light).endVertex();
					} else if (type == Set.SNOW) {
						float f0 = _ticks + _partialTick;
						float f1 = ((_ticks & 512) + _partialTick) / -512.0F;
						float f2 = (random.nextFloat() + f0 * (float) random.nextGaussian() * 0.01F) * speed;
						float f3 = (random.nextFloat() + f0 * (float) random.nextGaussian() * 0.001F) * speed;
						int i1 = light & '\uffff';
						int i2 = light >> 16 & '\uffff';
						int u = (i1 * 3 + 240) / 4;
						int v = (i2 * 3 + 240) / 4;
						bufferBuilder.vertex(lx - cos, max, lz + sin).uv(0.0F + f2, min * 0.25F + f1 + f3).color(red, green, blue, alpha).uv2(u, v).endVertex();
						bufferBuilder.vertex(lx + cos, max, lz - sin).uv(1.0F + f2, min * 0.25F + f1 + f3).color(red, green, blue, alpha).uv2(u, v).endVertex();
						bufferBuilder.vertex(lx + cos, min, lz - sin).uv(1.0F + f2, max * 0.25F + f1 + f3).color(red, green, blue, alpha).uv2(u, v).endVertex();
						bufferBuilder.vertex(lx - cos, min, lz + sin).uv(0.0F + f2, max * 0.25F + f1 + f3).color(red, green, blue, alpha).uv2(u, v).endVertex();
					}
					BufferUploader.drawWithShader(bufferBuilder.end());
					_lightTexture.turnOffLightLayer();
					// RenderSystem.disableTexture();
					RenderSystem.disableBlend();
					RenderSystem.disableDepthTest();
					RenderSystem.depthMask(true);
				}
			}
		}
	}

	private static void addWeatherParticlesAndSounds(Map<String, Object> map, boolean hitGround, Set set) {
		if (renderable(Stage.PARTICLES)) {
			RandomSource random = RandomSource.create();
			double x = (Double) map.get("x");
			double y = (Double) map.get("y");
			double z = (Double) map.get("z");
			int xi = (int) Math.floor(x);
			int yi = (int) Math.floor(y);
			if (hitGround) {
				yi = getWorldHeight(x, z);
			}
			int zi = (int) Math.floor(z);
			Level level = (Level) _clientLevel;
			BlockPos pos0 = new BlockPos(xi, yi, zi);
			BlockPos pos1 = new BlockPos(xi, yi - 1, zi);
			float rainLevel = getRainLevel();
			boolean weatherEffects = (Boolean) map.get("weatherEffects");
			boolean particleFlag = true;
			boolean soundFlag = true;
			boolean vanillaFlag = false;
			if (set == Set.PARTICLES_ONLY) {
				particleFlag = true;
				soundFlag = false;
			} else if (set == Set.SOUNDS_ONLY) {
				particleFlag = false;
				soundFlag = true;
			} else if (set == Set.VANILLA_PARTICLES_AND_SOUNDS) {
				vanillaFlag = true;
			} else if (set == Set.VANILLA_PARTICLES_ONLY) {
				particleFlag = true;
				soundFlag = false;
				vanillaFlag = true;
			} else if (set == Set.VANILLA_SOUNDS_ONLY) {
				particleFlag = false;
				soundFlag = true;
				vanillaFlag = true;
			}
			if (particleFlag) {
				ParticleStatus status = _minecraft.options.particles().get();
				boolean b0 = status != ParticleStatus.MINIMAL;
				int i0 = 1;
				float f0 = 1.0F;
				if (status == ParticleStatus.DECREASED) {
					i0 = 2;
				}
				if (weatherEffects) {
					f0 = rainLevel * rainLevel;
				}
				float f1 = ((100.0F * f0) / i0) / 441.0F;
				if (vanillaFlag) {
					b0 = b0 && rainnable(x, y, z);
				}
				if (b0 && random.nextFloat() < f1) {
					double rx = random.nextDouble();
					double rz = random.nextDouble();
					BlockState blockState = level.getBlockState(pos1);
					FluidState fluidState = level.getFluidState(pos1);
					VoxelShape voxelShape = blockState.getCollisionShape(level, pos1);
					double d0 = voxelShape.max(Direction.Axis.Y, rx, rz);
					double d1 = fluidState.getHeight(level, pos1);
					double d2 = Math.max(d0, d1);
					double qx = xi + rx;
					double qy = yi - 1.0D + d2;
					double qz = zi + rz;
					ParticleOptions particle = (ParticleOptions) map.get("particle");
					double vx = (Double) map.get("pvx");
					double vy = (Double) map.get("pvy");
					double vz = (Double) map.get("pvz");
					if (vanillaFlag) {
						if (!fluidState.is(FluidTags.LAVA) && !blockState.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockState)) {
							particle = ParticleTypes.RAIN;
						} else {
							particle = ParticleTypes.SMOKE;
						}
						vx = 0.0D;
						vy = 0.0D;
						vz = 0.0D;
					}
					_clientLevel.addParticle(particle, qx, qy, qz, vx, vy, vz);
				}
			}
			if (soundFlag) {
				float f2 = 1.0F / 441.0F;
				float f3 = 1.0F / 3.0F;
				boolean b1 = true;
				if (weatherEffects) {
					b1 = rainLevel > 0.0F;
				}
				if (vanillaFlag) {
					b1 = b1 && rainnable(x, y, z);
				}
				if (b1 && random.nextFloat() < f2 && random.nextFloat() < f3) {
					Vec3 cameraPos = _camera.getPosition();
					double camX = cameraPos.x();
					double camY = cameraPos.y();
					double camZ = cameraPos.z();
					double height = getWorldHeight(camX, camZ);
					--yi;
					net.minecraft.sounds.SoundEvent sound = SoundEvents.WEATHER_RAIN;
					SoundSource soundSource = SoundSource.WEATHER;
					float soundLevel = 0.2F;
					float soundPitch = 1.0F;
					if (vanillaFlag) {
						if (camY + 1.0D < yi && camY < height) {
							sound = SoundEvents.WEATHER_RAIN_ABOVE;
							soundLevel = 0.1F;
							soundPitch = 0.5F;
						}
					} else {
						sound = (net.minecraft.sounds.SoundEvent) map.get("sound");
						soundSource = (SoundSource) map.get("soundSource");
						soundLevel = (Float) map.get("soundLevel");
						soundPitch = (Float) map.get("soundPitch");
					}
					_clientLevel.playLocalSound(xi, yi, zi, sound, soundSource, soundLevel, soundPitch, false);
				}
			}
		}
	}

	private static void setFog(FogShape shape, float start, float end, Map<String, Object> map) {
		if (renderable(Stage.FOG)) {
			if (shape == null) {
				shape = _fog.getFogShape();
			}
			_fog.setFogShape(shape);
			_fog.setNearPlaneDistance(start);
			_fog.setFarPlaneDistance(end);
			_fog.setCanceled(true);
		} else {
			float[] color = getColor(map);
			if (_stage == Stage.FOGCOLOR) {
				_fogColor.setRed(color[0]);
				_fogColor.setGreen(color[1]);
				_fogColor.setBlue(color[2]);
			} else {
				RenderSystem.setShaderFogColor(color[0], color[1], color[2], color[3]);
			}
		}
	}

	private static void renderVanillaObjects(boolean clouds, boolean endSky, boolean topSky, boolean bottomSky, boolean sunlights, boolean stars, boolean sun, boolean moon, boolean weather) {
		Map<String, Object> params = clone(_PARAMS);
		if (renderable(Stage.CLOUDS)) {
			if (clouds) {
				DimensionType dimension = _clientLevel.dimensionType();
				params.put("x", getCloudPosition(dimension, Set.X));
				params.put("y", getCloudPosition(dimension, Set.Y));
				params.put("z", getCloudPosition(dimension, Set.Z));
				params.put("vx", -1.0D);
				params.put("vy", 0.0D);
				params.put("vz", 0.0D);
				params.put("width", 12.0D);
				params.put("depth", 12.0D);
				params.put("height", 4.0D);
				setTexture(params, "minecraft", "environment/clouds.png", null);
				params.put("model", Color.RGBA);
				params.put("red", getCloudColor(Color.RED));
				params.put("green", getCloudColor(Color.GREEN));
				params.put("blue", getCloudColor(Color.BLUE));
				params.put("alpha", getCloudColor(Color.ALPHA));
				renderClouds(params, Set.NONE, 480, 480);
			}
		}
		if (renderable(Stage.SKY)) {
			if (endSky) {
				boolean noFog = !_minecraft.gui.getBossOverlay().shouldCreateWorldFog();
				if (noFog) {
					setTexture(params, "minecraft", "environment/end_sky.png", null);
					params.put("u0", 0.0F);
					params.put("v0", 0.0F);
					params.put("u1", 16.0F);
					params.put("v1", 16.0F);
					params.put("model", Color.RGBA);
					params.put("red", 40.0F);
					params.put("green", 40.0F);
					params.put("blue", 40.0F);
					params.put("alpha", 100.0F);
					renderSkyBox(params, true);
				}
			}
			if (topSky) {
				params.put("yaw", 0.0F);
				params.put("pitch", -90.0F);
				params.put("roll", 0.0F);
				params.put("size", 100.0F);
				params.put("model", Color.RGBA);
				params.put("red", getSkyColor(Color.RED));
				params.put("green", getSkyColor(Color.GREEN));
				params.put("blue", getSkyColor(Color.BLUE));
				params.put("alpha", getSkyColor(Color.ALPHA));
				renderPolygon(params, 8);
			}
			if (sunlights) {
				params.put("yaw", 0.0F);
				params.put("pitch", 0.0F);
				params.put("roll", 0.0F);
				params.put("model", Color.RGBA);
				params.put("red", getSunlightColor(Color.RED));
				params.put("green", getSunlightColor(Color.GREEN));
				params.put("blue", getSunlightColor(Color.BLUE));
				params.put("alpha", getSunlightColor(Color.ALPHA));
				renderSunlights(params);
			}
			if (stars) {
				params.put("yaw", 0.0F);
				params.put("pitch", 0.0F);
				params.put("roll", getDayLevel() * 360.0F);
				params.put("model", Color.RGBA);
				params.put("red", getStarColor(Color.RED));
				params.put("green", getStarColor(Color.GREEN));
				params.put("blue", getStarColor(Color.BLUE));
				params.put("alpha", getStarColor(Color.ALPHA));
				renderStars(params, 1500, 10842);
			}
			if (bottomSky) {
				double eye = _minecraft.player.getEyePosition(_partialTick).y();
				double horizon = _clientLevel.getLevelData().getHorizonHeight(_clientLevel);
				if (eye < horizon) {
					params.put("yaw", 0.0F);
					params.put("pitch", 90.0F);
					params.put("roll", 0.0F);
					params.put("size", 100.0F);
					params.put("model", Color.RGBA);
					params.put("red", 0.0F);
					params.put("green", 0.0F);
					params.put("blue", 0.0F);
					params.put("alpha", 100.0F);
					renderPolygon(params, 8);
				}
			}
			if (sun) {
				setTexture(params, "minecraft", "environment/sun.png", null);
				params.put("u0", 0.0F);
				params.put("v0", 0.0F);
				params.put("u1", 1.0F);
				params.put("v1", 1.0F);
				params.put("size", 150.0F);
				params.put("model", Color.RGBA);
				params.put("red", 255.0F);
				params.put("green", 255.0F);
				params.put("blue", 255.0F);
				params.put("alpha", 100.0F);
				renderTexture(params, Set.SUN, true);
			}
			if (moon) {
				setTexture(params, "minecraft", "environment/moon_phases.png", null);
				params.put("u0", 0.0F);
				params.put("v0", 0.0F);
				params.put("u1", 1.0F);
				params.put("v1", 1.0F);
				params.put("size", 100.0F);
				params.put("model", Color.RGBA);
				params.put("red", 255.0F);
				params.put("green", 255.0F);
				params.put("blue", 255.0F);
				params.put("alpha", 100.0F);
				renderTexture(params, Set.MOON, true);
			}
		}
		if (renderable(Stage.WEATHER) || renderable(Stage.PARTICLES)) {
			if (weather) {
				Vec3 pos = _camera.getPosition();
				double size = getWeatherWDH();
				double x = pos.x();
				double y = pos.y();
				double z = pos.z();
				params.put("x", x);
				params.put("y", y);
				params.put("z", z);
				params.put("width", size);
				params.put("depth", size);
				params.put("height", size);
				params.put("model", Color.RGBA);
				params.put("red", 255.0F);
				params.put("green", 255.0F);
				params.put("blue", 255.0F);
				params.put("alpha", 100.0F);
				List<Vec3> list = getWeatherArea(params);
				for (Vec3 area : list) {
					double ax = area.x();
					double ay = area.y();
					double az = area.z();
					params.put("x", ax);
					params.put("y", ay);
					params.put("z", az);
					if (rainnable(ax, ay, az)) {
						setTexture(params, "minecraft", "environment/rain.png", null);
						renderWeather(params, Set.RAIN, (float) size, 1.0F, true);
					} else if (snowable(ax, ay, az)) {
						setTexture(params, "minecraft", "environment/snow.png", null);
						renderWeather(params, Set.SNOW, (float) size, 1.0F, true);
					}
					addWeatherParticlesAndSounds(params, true, Set.VANILLA_PARTICLES_AND_SOUNDS);
				}
			}
		}
	}

	private static void set(boolean brightLights) {
		if (renderable(Stage.TICKS)) {
			try {
				_effects.set(_clientLevel, new CustomDimensionEffects(DimensionSpecialEffects.forType(_clientLevel.dimensionType()), brightLights));
			} catch (Exception e) {
			}
		}
	}

	private static void clear() {
		if (renderable(Stage.TICKS)) {
			try {
				if (_effects == null) {
					_effects = _clientLevel.getClass().getDeclaredField("effects");
					_effects.setAccessible(true);
				}
				_effects.set(_clientLevel, DimensionSpecialEffects.forType(_clientLevel.dimensionType()));
			} catch (Exception e) {
			}
		}
	}

	private static class CustomDimensionEffects extends DimensionSpecialEffects {
		DimensionSpecialEffects effects = null;

		public CustomDimensionEffects(DimensionSpecialEffects effects, boolean brightLights) {
			super(effects.getCloudHeight(), effects.hasGround(), effects.skyType(), brightLights, effects.constantAmbientLight());
			this.effects = effects;
		}

		public Vec3 getBrightnessDependentFogColor(Vec3 color, float partialTick) {
			Vec3 result = effects.getBrightnessDependentFogColor(color, partialTick);
			return result;
		}

		public boolean isFoggyAt(int i0, int i1) {
			boolean result = effects.isFoggyAt(i0, i1);
			return result;
		}

		public boolean renderClouds(ClientLevel clientLevel, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
			_ticks = ticks;
			_poseStack = poseStack;
			_camX = camX;
			_camY = camY;
			_camZ = camZ;
			_stage = Stage.CLOUDS;
			_execute();
			return true;
		}

		public boolean renderSky(ClientLevel clientLevel, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
			_ticks = ticks;
			_poseStack = poseStack;
			_stage = Stage.SKY;
			_execute();
			return true;
		}

		public boolean renderSnowAndRain(ClientLevel clientLevel, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
			_ticks = ticks;
			_lightTexture = lightTexture;
			_camX = camX;
			_camY = camY;
			_camZ = camZ;
			_stage = Stage.WEATHER;
			_execute();
			return true;
		}

		public boolean tickRain(ClientLevel clientLevel, int ticks, Camera camera) {
			_ticks = ticks;
			_stage = Stage.PARTICLES;
			_execute();
			return true;
		}
	}

	@SubscribeEvent
	public static void ticks(TickEvent.RenderTickEvent event) {
		_minecraft = Minecraft.getInstance();
		_clientLevel = _minecraft.level;
		Entity entity = _minecraft.getCameraEntity();
		if (_clientLevel != null && entity != null) {
			_stage = Stage.TICKS;
			clear();
			_execute();
		}
	}

	@SubscribeEvent
	public static void fog(ViewportEvent.RenderFog event) {
		_fog = event;
		_stage = Stage.FOG;
		_execute();
	}

	@SubscribeEvent
	public static void fogColor(ViewportEvent.ComputeFogColor event) {
		_fogColor = event;
		_stage = Stage.FOGCOLOR;
		_execute();
	}

	private static void _execute() {
		_minecraft = Minecraft.getInstance();
		_clientLevel = _minecraft.level;
		Entity entity = _minecraft.getCameraEntity();
		if (_clientLevel != null && entity != null) {
			if (!_minecraft.isPaused()) {
				_partialTick = _minecraft.getFrameTime();
			}
			_projectionMatrix = RenderSystem.getProjectionMatrix();
			_camera = _minecraft.gameRenderer.getMainCamera();
			execute(null, _clientLevel, entity.getX(), entity.getY(), entity.getZ(), clone(_PARAMS));
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Map<String, Object> parameter) {
		execute(null, world, x, y, z, parameter);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Map<String, Object> parameter) {
		try {
			Color RED = Color.RED;
			Color GREEN = Color.GREEN;
			Color BLUE = Color.BLUE;
			int startfog = BetterFogConfiguration.DEFAULT_START_FOG.get();;
			double endfog = getFogDistance(Set.END) * BetterFogConfiguration.END_FOG_MULTIPLYER.get();
			int opacity = BetterFogConfiguration.DEFAULT_OPACITY.get();
			int red = (int) getFogColor(RED);
			int green = (int) getFogColor(GREEN);
			int blue = (int) getFogColor(BLUE);

			double night = BiomeColor.getTimeOfDayValue(_clientLevel);

			if (BetterFogConfiguration.NIGHT_FOG_ENABLED.get()) {
				startfog = (int) BiomeColor.fromTo(startfog, BetterFogConfiguration.NIGHT_FOG_START.get(), night);
				endfog = BiomeColor.fromTo(endfog, BetterFogConfiguration.NIGHT_FOG_END.get(), night);
				opacity = (int) BiomeColor.fromTo(opacity, BetterFogConfiguration.NIGHT_DENSITY.get() * opacity, night);
			}

			if (BetterFogConfiguration.RAIN_ENABLED.get() && !world.getLevelData().isRaining()) {
				opacity = (int) (opacity * BetterFogConfiguration.RAIN_OPACITY_FACTOR.get());
			}

			if (BetterFogConfiguration.BIOME_ENABLED.get()) {
				List<Double> features = BiomeColor.getColor((Level) world, new BlockPos((int) x, (int) y, (int) z));
				if (BetterFogConfiguration.NIGHT_FOG_ENABLED.get()) {
					red = (int) ((red * 0.5 + BiomeColor.fromTo(features.get(0), red, night)) / 1.5);
					green = (int) ((green * 0.5 + BiomeColor.fromTo(features.get(1), green, night)) / 1.5);
					blue = (int) ((blue * 0.5 + BiomeColor.fromTo(features.get(2), blue, night)) / 1.5);
				} else {
					red = (int) ((red * 0.5 + features.get(0)) / 1.5);
					green = (int) ((green * 0.5 + features.get(1)) / 1.5);
					blue = (int) ((blue * 0.5 + features.get(2)) / 1.5);
				}
				opacity = (int) (opacity * features.get(3));
				startfog = (int) (startfog * features.get(4));
				endfog = endfog * features.get(5);
			}

			opacity = Math.min(opacity, 255);

			if (BetterFogConfiguration.CAVE_ENABLED.get()) {
				int maxx = BetterFogConfiguration.Y_START.get();
				if (y < maxx) {
					int minx = BetterFogConfiguration.Y_END.get();
					int maxopacity = BetterFogConfiguration.MIN_OPACITY.get();
					int minpick = -4;
					int maxpick = 3;
					int minendfog = BetterFogConfiguration.MIN_END_FOG.get();
					int endstartfog = BetterFogConfiguration.MIN_START_FOG.get();
					int mincolorpick = -4;
					double maxcolorpick = 3.5;
					red = (int) Sigmoid.betterSigmoid(y, minx, maxx * 0.7, 120, red, mincolorpick, maxcolorpick);
					green = (int) Sigmoid.betterSigmoid(y, minx, maxx * 0.7, 120, green, mincolorpick, maxcolorpick);
					blue = (int) Sigmoid.betterSigmoid(y, minx, maxx * 0.7, 120, blue, mincolorpick, maxcolorpick);
					startfog = (int) Sigmoid.betterSigmoid(y, minx, maxx, endstartfog, startfog, mincolorpick, maxcolorpick);
					endfog = Sigmoid.betterSigmoid(y, minx, maxx, minendfog, endfog, mincolorpick, maxcolorpick);
					opacity = (int) Sigmoid.betterSigmoid(y, minx, maxx, maxopacity, opacity, minpick, maxpick);
				}
			}
			if (BetterFogConfiguration.CLOUD_ENABLED.get()) {
				int minxx2 = BetterFogConfiguration.CLOUD_Y_START.get();
				if (y > minxx2){
					int maxstartfogcloud = BetterFogConfiguration.CLOUD_START_START.get();
					int maxendfogcloud = BetterFogConfiguration.CLOUD_END_END.get();
					int maxx2 = BetterFogConfiguration.CLOUD_Y_END.get();
					double opacityFactor = BetterFogConfiguration.CLOUD_MAX_OPACITY.get();
					float half = (minxx2 + (float) (maxx2 - minxx2) / 2);
					double cloudminpick = -4.5;
					double cloudmaxpick = 0.1;
					if (y < half) {
						opacity = (int) Sigmoid.betterSigmoid(y, minxx2, half, opacity, opacity * opacityFactor, cloudminpick, cloudmaxpick);
						startfog = (int) Sigmoid.betterSigmoid(y, minxx2, half, startfog, maxstartfogcloud, cloudminpick, cloudmaxpick);
						endfog = (int) Sigmoid.betterSigmoid(y, minxx2, half, endfog, maxendfogcloud, cloudminpick, cloudmaxpick);
					} else if (y < maxx2) {
						opacity = (int) Sigmoid.betterSigmoid(y, maxx2, half, opacity, opacity * opacityFactor, -cloudminpick, -cloudmaxpick);
						startfog = (int) Sigmoid.betterSigmoid(y, maxx2, half, startfog, maxstartfogcloud, -cloudminpick, -cloudmaxpick);
						endfog = (int) Sigmoid.betterSigmoid(y, maxx2, half, endfog, maxendfogcloud, -cloudminpick, -cloudmaxpick);
					}
				}
			}

			(parameter).put("model", Color.RGBA);
			(parameter).put("red", (float) red);
			(parameter).put("green", (float) green);
			(parameter).put("blue", (float) blue);
			(parameter).put("alpha", (float) opacity);
			setFog(FogShape.CYLINDER, (float) startfog, (float) endfog, (parameter));
		} catch (Exception e) {
			return;
		}
	}
}