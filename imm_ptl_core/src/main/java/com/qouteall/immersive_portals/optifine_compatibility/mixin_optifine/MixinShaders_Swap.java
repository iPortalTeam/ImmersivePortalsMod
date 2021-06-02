package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vector4f;
import net.optifine.expr.IExpressionBool;
import net.optifine.shaders.DrawBuffers;
import net.optifine.shaders.ICustomTexture;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Program;
import net.optifine.shaders.ProgramStack;
import net.optifine.shaders.Programs;
import net.optifine.shaders.RenderStage;
import net.optifine.shaders.ShadersFramebuffer;
import net.optifine.shaders.config.PropertyDefaultFastFancyOff;
import net.optifine.shaders.config.PropertyDefaultTrueFalse;
import net.optifine.shaders.config.ScreenShaderOptions;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.config.ShaderProfile;
import net.optifine.shaders.uniform.CustomUniforms;
import net.optifine.shaders.uniform.ShaderUniform1f;
import net.optifine.shaders.uniform.ShaderUniform1i;
import net.optifine.shaders.uniform.ShaderUniform2i;
import net.optifine.shaders.uniform.ShaderUniform3f;
import net.optifine.shaders.uniform.ShaderUniform4f;
import net.optifine.shaders.uniform.ShaderUniform4i;
import net.optifine.shaders.uniform.ShaderUniformM4;
import net.optifine.shaders.uniform.ShaderUniforms;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

// ugly hack. don't look at it
@Pseudo
@Mixin(targets = "net.optifine.shaders.Shaders", remap = false)
public abstract class MixinShaders_Swap {
    
    @Shadow
    static MinecraftClient mc;
    @Shadow
    static GameRenderer entityRenderer;
    @Shadow
    public static boolean isInitializedOnce;
    @Shadow
    public static boolean isShaderPackInitialized;
    @Shadow
    public static GLCapabilities capabilities;
    @Shadow
    public static String glVersionString;
    @Shadow
    public static String glVendorString;
    @Shadow
    public static String glRendererString;
    @Shadow
    public static boolean hasGlGenMipmap;
    @Shadow
    public static int countResetDisplayLists;
    @Shadow
    private static int renderDisplayWidth;
    @Shadow
    private static int renderDisplayHeight;
    @Shadow
    public static int renderWidth;
    @Shadow
    public static int renderHeight;
    @Shadow
    public static boolean isRenderingWorld;
    @Shadow
    public static boolean isRenderingSky;
    @Shadow
    public static boolean isCompositeRendered;
    @Shadow
    public static boolean isRenderingDfb;
    @Shadow
    public static boolean isShadowPass;
    @Shadow
    public static boolean isEntitiesGlowing;
    @Shadow
    public static boolean isSleeping;
    @Shadow
    private static boolean isRenderingFirstPersonHand;
    @Shadow
    private static boolean isHandRenderedMain;
    @Shadow
    private static boolean isHandRenderedOff;
    @Shadow
    private static boolean skipRenderHandMain;
    @Shadow
    private static boolean skipRenderHandOff;
    @Shadow
    public static boolean renderItemKeepDepthMask;
    @Shadow
    public static boolean itemToRenderMainTranslucent;
    @Shadow
    public static boolean itemToRenderOffTranslucent;
    @Shadow
    static float[] sunPosition;
    @Shadow
    static float[] moonPosition;
    @Shadow
    static float[] shadowLightPosition;
    @Shadow
    static float[] upPosition;
    @Shadow
    static float[] shadowLightPositionVector;
    @Shadow
    static float[] upPosModelView;
    @Shadow
    static float[] sunPosModelView;
    @Shadow
    static float[] moonPosModelView;
    @Shadow
    private static float[] tempMat;
    @Shadow
    static Vector4f clearColor;
    @Shadow
    static float skyColorR;
    @Shadow
    static float skyColorG;
    @Shadow
    static float skyColorB;
    @Shadow
    static long worldTime;
    @Shadow
    static long lastWorldTime;
    @Shadow
    static long diffWorldTime;
    @Shadow
    static float celestialAngle;
    @Shadow
    static float sunAngle;
    @Shadow
    static float shadowAngle;
    @Shadow
    static int moonPhase;
    @Shadow
    static long systemTime;
    @Shadow
    static long lastSystemTime;
    @Shadow
    static long diffSystemTime;
    @Shadow
    static int frameCounter;
    @Shadow
    static float frameTime;
    @Shadow
    static float frameTimeCounter;
    @Shadow
    static int systemTimeInt32;
    @Shadow
    static float rainStrength;
    @Shadow
    static float wetness;
    @Shadow
    public static float wetnessHalfLife;
    @Shadow
    public static float drynessHalfLife;
    @Shadow
    public static float eyeBrightnessHalflife;
    @Shadow
    static boolean usewetness;
    @Shadow
    static int isEyeInWater;
    @Shadow
    static int eyeBrightness;
    @Shadow
    static float eyeBrightnessFadeX;
    @Shadow
    static float eyeBrightnessFadeY;
    @Shadow
    static float eyePosY;
    @Shadow
    static float centerDepth;
    @Shadow
    static float centerDepthSmooth;
    @Shadow
    static float centerDepthSmoothHalflife;
    @Shadow
    static boolean centerDepthSmoothEnabled;
    @Shadow
    static int superSamplingLevel;
    @Shadow
    static float nightVision;
    @Shadow
    static float blindness;
    @Shadow
    static boolean lightmapEnabled;
    @Shadow
    static boolean fogEnabled;
    @Shadow
    static RenderStage renderStage;
    @Shadow
    private static int baseAttribId;
    @Shadow
    public static int entityAttrib;
    @Shadow
    public static int midTexCoordAttrib;
    @Shadow
    public static int tangentAttrib;
    @Shadow
    public static boolean useEntityAttrib;
    @Shadow
    public static boolean useMidTexCoordAttrib;
    @Shadow
    public static boolean useTangentAttrib;
    @Shadow
    public static boolean progUseEntityAttrib;
    @Shadow
    public static boolean progUseMidTexCoordAttrib;
    @Shadow
    public static boolean progUseTangentAttrib;
    @Shadow
    private static boolean progArbGeometryShader4;
    @Shadow
    private static int progMaxVerticesOut;
    @Shadow
    private static boolean hasGeometryShaders;
    @Shadow
    public static int atlasSizeX;
    @Shadow
    public static int atlasSizeY;
    @Shadow
    private static ShaderUniforms shaderUniforms;
    @Shadow
    public static ShaderUniform4f uniform_entityColor;
    @Shadow
    public static ShaderUniform1i uniform_entityId;
    @Shadow
    public static ShaderUniform1i uniform_blockEntityId;
    @Shadow
    public static ShaderUniform1i uniform_texture;
    @Shadow
    public static ShaderUniform1i uniform_lightmap;
    @Shadow
    public static ShaderUniform1i uniform_normals;
    @Shadow
    public static ShaderUniform1i uniform_specular;
    @Shadow
    public static ShaderUniform1i uniform_shadow;
    @Shadow
    public static ShaderUniform1i uniform_watershadow;
    @Shadow
    public static ShaderUniform1i uniform_shadowtex0;
    @Shadow
    public static ShaderUniform1i uniform_shadowtex1;
    @Shadow
    public static ShaderUniform1i uniform_depthtex0;
    @Shadow
    public static ShaderUniform1i uniform_depthtex1;
    @Shadow
    public static ShaderUniform1i uniform_shadowcolor;
    @Shadow
    public static ShaderUniform1i uniform_shadowcolor0;
    @Shadow
    public static ShaderUniform1i uniform_shadowcolor1;
    @Shadow
    public static ShaderUniform1i uniform_noisetex;
    @Shadow
    public static ShaderUniform1i uniform_gcolor;
    @Shadow
    public static ShaderUniform1i uniform_gdepth;
    @Shadow
    public static ShaderUniform1i uniform_gnormal;
    @Shadow
    public static ShaderUniform1i uniform_composite;
    @Shadow
    public static ShaderUniform1i uniform_gaux1;
    @Shadow
    public static ShaderUniform1i uniform_gaux2;
    @Shadow
    public static ShaderUniform1i uniform_gaux3;
    @Shadow
    public static ShaderUniform1i uniform_gaux4;
    @Shadow
    public static ShaderUniform1i uniform_colortex0;
    @Shadow
    public static ShaderUniform1i uniform_colortex1;
    @Shadow
    public static ShaderUniform1i uniform_colortex2;
    @Shadow
    public static ShaderUniform1i uniform_colortex3;
    @Shadow
    public static ShaderUniform1i uniform_colortex4;
    @Shadow
    public static ShaderUniform1i uniform_colortex5;
    @Shadow
    public static ShaderUniform1i uniform_colortex6;
    @Shadow
    public static ShaderUniform1i uniform_colortex7;
    @Shadow
    public static ShaderUniform1i uniform_gdepthtex;
    @Shadow
    public static ShaderUniform1i uniform_depthtex2;
    @Shadow
    public static ShaderUniform1i uniform_colortex8;
    @Shadow
    public static ShaderUniform1i uniform_colortex9;
    @Shadow
    public static ShaderUniform1i uniform_colortex10;
    @Shadow
    public static ShaderUniform1i uniform_colortex11;
    @Shadow
    public static ShaderUniform1i uniform_colortex12;
    @Shadow
    public static ShaderUniform1i uniform_colortex13;
    @Shadow
    public static ShaderUniform1i uniform_colortex14;
    @Shadow
    public static ShaderUniform1i uniform_colortex15;
    @Shadow
    public static ShaderUniform1i uniform_colorimg0;
    @Shadow
    public static ShaderUniform1i uniform_colorimg1;
    @Shadow
    public static ShaderUniform1i uniform_colorimg2;
    @Shadow
    public static ShaderUniform1i uniform_colorimg3;
    @Shadow
    public static ShaderUniform1i uniform_colorimg4;
    @Shadow
    public static ShaderUniform1i uniform_colorimg5;
    @Shadow
    public static ShaderUniform1i uniform_shadowcolorimg0;
    @Shadow
    public static ShaderUniform1i uniform_shadowcolorimg1;
    @Shadow
    public static ShaderUniform1i uniform_tex;
    @Shadow
    public static ShaderUniform1i uniform_heldItemId;
    @Shadow
    public static ShaderUniform1i uniform_heldBlockLightValue;
    @Shadow
    public static ShaderUniform1i uniform_heldItemId2;
    @Shadow
    public static ShaderUniform1i uniform_heldBlockLightValue2;
    @Shadow
    public static ShaderUniform1i uniform_fogMode;
    @Shadow
    public static ShaderUniform1f uniform_fogDensity;
    @Shadow
    public static ShaderUniform3f uniform_fogColor;
    @Shadow
    public static ShaderUniform3f uniform_skyColor;
    @Shadow
    public static ShaderUniform1i uniform_worldTime;
    @Shadow
    public static ShaderUniform1i uniform_worldDay;
    @Shadow
    public static ShaderUniform1i uniform_moonPhase;
    @Shadow
    public static ShaderUniform1i uniform_frameCounter;
    @Shadow
    public static ShaderUniform1f uniform_frameTime;
    @Shadow
    public static ShaderUniform1f uniform_frameTimeCounter;
    @Shadow
    public static ShaderUniform1f uniform_sunAngle;
    @Shadow
    public static ShaderUniform1f uniform_shadowAngle;
    @Shadow
    public static ShaderUniform1f uniform_rainStrength;
    @Shadow
    public static ShaderUniform1f uniform_aspectRatio;
    @Shadow
    public static ShaderUniform1f uniform_viewWidth;
    @Shadow
    public static ShaderUniform1f uniform_viewHeight;
    @Shadow
    public static ShaderUniform1f uniform_near;
    @Shadow
    public static ShaderUniform1f uniform_far;
    @Shadow
    public static ShaderUniform3f uniform_sunPosition;
    @Shadow
    public static ShaderUniform3f uniform_moonPosition;
    @Shadow
    public static ShaderUniform3f uniform_shadowLightPosition;
    @Shadow
    public static ShaderUniform3f uniform_upPosition;
    @Shadow
    public static ShaderUniform3f uniform_previousCameraPosition;
    @Shadow
    public static ShaderUniform3f uniform_cameraPosition;
    @Shadow
    public static ShaderUniformM4 uniform_gbufferModelView;
    @Shadow
    public static ShaderUniformM4 uniform_gbufferModelViewInverse;
    @Shadow
    public static ShaderUniformM4 uniform_gbufferPreviousProjection;
    @Shadow
    public static ShaderUniformM4 uniform_gbufferProjection;
    @Shadow
    public static ShaderUniformM4 uniform_gbufferProjectionInverse;
    @Shadow
    public static ShaderUniformM4 uniform_gbufferPreviousModelView;
    @Shadow
    public static ShaderUniformM4 uniform_shadowProjection;
    @Shadow
    public static ShaderUniformM4 uniform_shadowProjectionInverse;
    @Shadow
    public static ShaderUniformM4 uniform_shadowModelView;
    @Shadow
    public static ShaderUniformM4 uniform_shadowModelViewInverse;
    @Shadow
    public static ShaderUniform1f uniform_wetness;
    @Shadow
    public static ShaderUniform1f uniform_eyeAltitude;
    @Shadow
    public static ShaderUniform2i uniform_eyeBrightness;
    @Shadow
    public static ShaderUniform2i uniform_eyeBrightnessSmooth;
    @Shadow
    public static ShaderUniform2i uniform_terrainTextureSize;
    @Shadow
    public static ShaderUniform1i uniform_terrainIconSize;
    @Shadow
    public static ShaderUniform1i uniform_isEyeInWater;
    @Shadow
    public static ShaderUniform1f uniform_nightVision;
    @Shadow
    public static ShaderUniform1f uniform_blindness;
    @Shadow
    public static ShaderUniform1f uniform_screenBrightness;
    @Shadow
    public static ShaderUniform1i uniform_hideGUI;
    @Shadow
    public static ShaderUniform1f uniform_centerDepthSmooth;
    @Shadow
    public static ShaderUniform2i uniform_atlasSize;
    @Shadow
    public static ShaderUniform4f uniform_spriteBounds;
    @Shadow
    public static ShaderUniform4i uniform_blendFunc;
    @Shadow
    public static ShaderUniform1i uniform_instanceId;
    @Shadow
    public static ShaderUniform1f uniform_playerMood;
    @Shadow
    public static ShaderUniform1i uniform_renderStage;
    @Shadow
    static double previousCameraPositionX;
    @Shadow
    static double previousCameraPositionY;
    @Shadow
    static double previousCameraPositionZ;
    @Shadow
    static double cameraPositionX;
    @Shadow
    static double cameraPositionY;
    @Shadow
    static double cameraPositionZ;
    @Shadow
    static int cameraOffsetX;
    @Shadow
    static int cameraOffsetZ;
    @Shadow
    static boolean hasShadowMap;
    @Shadow
    public static boolean needResizeShadow;
    @Shadow
    static int shadowMapWidth;
    @Shadow
    static int shadowMapHeight;
    @Shadow
    static int spShadowMapWidth;
    @Shadow
    static int spShadowMapHeight;
    @Shadow
    static float shadowMapFOV;
    @Shadow
    static float shadowMapHalfPlane;
    @Shadow
    static boolean shadowMapIsOrtho;
    @Shadow
    static float shadowDistanceRenderMul;
    @Shadow
    public static boolean shouldSkipDefaultShadow;
    @Shadow
    static boolean waterShadowEnabled;
    @Shadow
    public static @Final @Mutable
    int MaxDrawBuffers;
    @Shadow
    public static @Final @Mutable
    int MaxColorBuffers;
    @Shadow
    public static @Final @Mutable
    int MaxDepthBuffers;
    @Shadow
    public static @Final @Mutable
    int MaxShadowColorBuffers;
    @Shadow
    public static @Final @Mutable
    int MaxShadowDepthBuffers;
    @Shadow
    static int usedColorBuffers;
    @Shadow
    static int usedDepthBuffers;
    @Shadow
    static int usedShadowColorBuffers;
    @Shadow
    static int usedShadowDepthBuffers;
    @Shadow
    static int usedColorAttachs;
    @Shadow
    static int usedDrawBuffers;
    @Shadow
    static boolean bindImageTextures;
    @Shadow
    static ShadersFramebuffer dfb;
    @Shadow
    static ShadersFramebuffer sfb;
    @Shadow
    private static int[] gbuffersFormat;
    @Shadow
    public static boolean[] gbuffersClear;
    @Shadow
    public static Vector4f[] gbuffersClearColor;
    @Shadow
    private static @Final @Mutable
    Vector4f CLEAR_COLOR_0;
    @Shadow
    private static @Final @Mutable
    Vector4f CLEAR_COLOR_1;
    @Shadow
    private static int[] shadowBuffersFormat;
    @Shadow
    public static boolean[] shadowBuffersClear;
    @Shadow
    public static Vector4f[] shadowBuffersClearColor;
    @Shadow
    private static Programs programs;
    @Shadow
    public static @Final @Mutable
    Program ProgramNone;
    @Shadow
    public static @Final @Mutable
    Program ProgramShadow;
    @Shadow
    public static @Final @Mutable
    Program ProgramShadowSolid;
    @Shadow
    public static @Final @Mutable
    Program ProgramShadowCutout;
    @Shadow
    public static @Final @Mutable
    Program[] ProgramsShadowcomp;
    @Shadow
    public static @Final @Mutable
    Program[] ProgramsPrepare;
    @Shadow
    public static @Final @Mutable
    Program ProgramBasic;
    @Shadow
    public static @Final @Mutable
    Program ProgramTextured;
    @Shadow
    public static @Final @Mutable
    Program ProgramTexturedLit;
    @Shadow
    public static @Final @Mutable
    Program ProgramSkyBasic;
    @Shadow
    public static @Final @Mutable
    Program ProgramSkyTextured;
    @Shadow
    public static @Final @Mutable
    Program ProgramClouds;
    @Shadow
    public static @Final @Mutable
    Program ProgramTerrain;
    @Shadow
    public static @Final @Mutable
    Program ProgramTerrainSolid;
    @Shadow
    public static @Final @Mutable
    Program ProgramTerrainCutoutMip;
    @Shadow
    public static @Final @Mutable
    Program ProgramTerrainCutout;
    @Shadow
    public static @Final @Mutable
    Program ProgramDamagedBlock;
    @Shadow
    public static @Final @Mutable
    Program ProgramBlock;
    @Shadow
    public static @Final @Mutable
    Program ProgramBeaconBeam;
    @Shadow
    public static @Final @Mutable
    Program ProgramItem;
    @Shadow
    public static @Final @Mutable
    Program ProgramEntities;
    @Shadow
    public static @Final @Mutable
    Program ProgramEntitiesGlowing;
    @Shadow
    public static @Final @Mutable
    Program ProgramArmorGlint;
    @Shadow
    public static @Final @Mutable
    Program ProgramSpiderEyes;
    @Shadow
    public static @Final @Mutable
    Program ProgramHand;
    @Shadow
    public static @Final @Mutable
    Program ProgramWeather;
    @Shadow
    public static @Final @Mutable
    Program ProgramDeferredPre;
    @Shadow
    public static @Final @Mutable
    Program[] ProgramsDeferred;
    @Shadow
    public static @Final @Mutable
    Program ProgramDeferred;
    @Shadow
    public static @Final @Mutable
    Program ProgramWater;
    @Shadow
    public static @Final @Mutable
    Program ProgramHandWater;
    @Shadow
    public static @Final @Mutable
    Program ProgramCompositePre;
    @Shadow
    public static @Final @Mutable
    Program[] ProgramsComposite;
    @Shadow
    public static @Final @Mutable
    Program ProgramComposite;
    @Shadow
    public static @Final @Mutable
    Program ProgramFinal;
    @Shadow
    public static @Final @Mutable
    int ProgramCount;
    @Shadow
    public static @Final @Mutable
    Program[] ProgramsAll;
    @Shadow
    public static Program activeProgram;
    @Shadow
    public static int activeProgramID;
    @Shadow
    private static ProgramStack programStack;
    @Shadow
    private static boolean hasDeferredPrograms;
    @Shadow
    public static boolean hasShadowcompPrograms;
    @Shadow
    public static boolean hasPreparePrograms;
    @Shadow
    public static Properties loadedShaders;
    @Shadow
    public static Properties shadersConfig;
    @Shadow
    public static AbstractTexture defaultTexture;
    @Shadow
    public static boolean[] shadowHardwareFilteringEnabled;
    @Shadow
    public static boolean[] shadowMipmapEnabled;
    @Shadow
    public static boolean[] shadowFilterNearest;
    @Shadow
    public static boolean[] shadowColorMipmapEnabled;
    @Shadow
    public static boolean[] shadowColorFilterNearest;
    @Shadow
    public static boolean configTweakBlockDamage;
    @Shadow
    public static boolean configCloudShadow;
    @Shadow
    public static float configHandDepthMul;
    @Shadow
    public static float configRenderResMul;
    @Shadow
    public static float configShadowResMul;
    @Shadow
    public static int configTexMinFilB;
    @Shadow
    public static int configTexMinFilN;
    @Shadow
    public static int configTexMinFilS;
    @Shadow
    public static int configTexMagFilB;
    @Shadow
    public static int configTexMagFilN;
    @Shadow
    public static int configTexMagFilS;
    @Shadow
    public static boolean configShadowClipFrustrum;
    @Shadow
    public static boolean configNormalMap;
    @Shadow
    public static boolean configSpecularMap;
    @Shadow
    public static PropertyDefaultTrueFalse configOldLighting;
    @Shadow
    public static PropertyDefaultTrueFalse configOldHandLight;
    @Shadow
    public static int configAntialiasingLevel;
    @Shadow
    public static @Final @Mutable
    int texMinFilRange;
    @Shadow
    public static @Final @Mutable
    int texMagFilRange;
    @Shadow
    public static @Final @Mutable
    String[] texMinFilDesc;
    @Shadow
    public static @Final @Mutable
    String[] texMagFilDesc;
    @Shadow
    public static @Final @Mutable
    int[] texMinFilValue;
    @Shadow
    public static @Final @Mutable
    int[] texMagFilValue;
    @Shadow
    private static IShaderPack shaderPack;
    @Shadow
    public static boolean shaderPackLoaded;
    @Shadow
    public static String currentShaderName;
    @Shadow
    public static @Final @Mutable
    String SHADER_PACK_NAME_NONE;
    @Shadow
    public static @Final @Mutable
    String SHADER_PACK_NAME_DEFAULT;
    @Shadow
    public static @Final @Mutable
    String SHADER_PACKS_DIR_NAME;
    @Shadow
    public static @Final @Mutable
    String OPTIONS_FILE_NAME;
    @Shadow
    public static @Final @Mutable
    File shaderPacksDir;
    @Shadow
    static File configFile;
    @Shadow
    private static ShaderOption[] shaderPackOptions;
    @Shadow
    private static Set<String> shaderPackOptionSliders;
    @Shadow
    static ShaderProfile[] shaderPackProfiles;
    @Shadow
    static Map<String, ScreenShaderOptions> shaderPackGuiScreens;
    @Shadow
    static Map<String, IExpressionBool> shaderPackProgramConditions;
    @Shadow
    public static @Final @Mutable
    String PATH_SHADERS_PROPERTIES;
    @Shadow
    public static PropertyDefaultFastFancyOff shaderPackClouds;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackOldLighting;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackOldHandLight;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackDynamicHandLight;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackShadowTranslucent;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackUnderwaterOverlay;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackSun;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackMoon;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackVignette;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackBackFaceSolid;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackBackFaceCutout;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackBackFaceCutoutMipped;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackBackFaceTranslucent;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackRainDepth;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackBeaconBeamDepth;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackSeparateAo;
    @Shadow
    public static PropertyDefaultTrueFalse shaderPackFrustumCulling;
    @Shadow
    private static Map<String, String> shaderPackResources;
    @Shadow
    private static ClientWorld currentWorld;
    @Shadow
    private static List<Integer> shaderPackDimensions;
    @Shadow
    private static ICustomTexture[] customTexturesGbuffers;
    @Shadow
    private static ICustomTexture[] customTexturesComposite;
    @Shadow
    private static ICustomTexture[] customTexturesDeferred;
    @Shadow
    private static ICustomTexture[] customTexturesShadowcomp;
    @Shadow
    private static ICustomTexture[] customTexturesPrepare;
    @Shadow
    private static String noiseTexturePath;
    @Shadow
    private static Dimension[] colorBufferSizes;
    @Shadow
    private static CustomUniforms customUniforms;
    @Shadow
    public static @Final @Mutable
    boolean saveFinalShaders;
    @Shadow
    public static float blockLightLevel05;
    @Shadow
    public static float blockLightLevel06;
    @Shadow
    public static float blockLightLevel08;
    @Shadow
    public static float aoLevel;
    @Shadow
    public static float sunPathRotation;
    @Shadow
    public static float shadowAngleInterval;
    @Shadow
    public static int fogMode;
    @Shadow
    public static float fogDensity;
    @Shadow
    public static float fogColorR;
    @Shadow
    public static float fogColorG;
    @Shadow
    public static float fogColorB;
    @Shadow
    public static float shadowIntervalSize;
    @Shadow
    public static int terrainIconSize;
    @Shadow
    public static int[] terrainTextureSize;
    @Shadow
    private static ICustomTexture noiseTexture;
    @Shadow
    private static boolean noiseTextureEnabled;
    @Shadow
    private static int noiseTextureResolution;
    @Shadow
    static @Final @Mutable
    int[] colorTextureImageUnit;
    @Shadow
    static @Final @Mutable
    int[] depthTextureImageUnit;
    @Shadow
    static @Final @Mutable
    int[] shadowColorTextureImageUnit;
    @Shadow
    static @Final @Mutable
    int[] shadowDepthTextureImageUnit;
    @Shadow
    static @Final @Mutable
    int[] colorImageUnit;
    @Shadow
    static @Final @Mutable
    int[] shadowColorImageUnit;
    @Shadow
    private static @Final @Mutable
    int bigBufferSize;
    @Shadow
    private static @Final @Mutable
    ByteBuffer bigBuffer;
    @Shadow
    static @Final @Mutable
    float[] faProjection;
    @Shadow
    static @Final @Mutable
    float[] faProjectionInverse;
    @Shadow
    static @Final @Mutable
    float[] faModelView;
    @Shadow
    static @Final @Mutable
    float[] faModelViewInverse;
    @Shadow
    static @Final @Mutable
    float[] faShadowProjection;
    @Shadow
    static @Final @Mutable
    float[] faShadowProjectionInverse;
    @Shadow
    static @Final @Mutable
    float[] faShadowModelView;
    @Shadow
    static @Final @Mutable
    float[] faShadowModelViewInverse;
    @Shadow
    static @Final @Mutable
    FloatBuffer projection;
    @Shadow
    static @Final @Mutable
    FloatBuffer projectionInverse;
    @Shadow
    static @Final @Mutable
    FloatBuffer modelView;
    @Shadow
    static @Final @Mutable
    FloatBuffer modelViewInverse;
    @Shadow
    static @Final @Mutable
    FloatBuffer shadowProjection;
    @Shadow
    static @Final @Mutable
    FloatBuffer shadowProjectionInverse;
    @Shadow
    static @Final @Mutable
    FloatBuffer shadowModelView;
    @Shadow
    static @Final @Mutable
    FloatBuffer shadowModelViewInverse;
    @Shadow
    static @Final @Mutable
    FloatBuffer previousProjection;
    @Shadow
    static @Final @Mutable
    FloatBuffer previousModelView;
    @Shadow
    static @Final @Mutable
    FloatBuffer tempMatrixDirectBuffer;
    @Shadow
    static @Final @Mutable
    FloatBuffer tempDirectFloatBuffer;
    @Shadow
    static @Final @Mutable
    DrawBuffers dfbDrawBuffers;
    @Shadow
    static @Final @Mutable
    DrawBuffers sfbDrawBuffers;
    @Shadow
    static @Final @Mutable
    DrawBuffers drawBuffersNone;
    @Shadow
    static @Final @Mutable
    DrawBuffers[] drawBuffersColorAtt;
    @Shadow
    static boolean glDebugGroups;
    @Shadow
    static boolean glDebugGroupProgram;
    @Shadow
    static Map<Block, Integer> mapBlockToEntityData;
    @Shadow
    private static @Final @Mutable
    String[] formatNames;
    @Shadow
    private static @Final @Mutable
    int[] formatIds;
    @Shadow
    private static @Final @Mutable
    Pattern patternLoadEntityDataMap;
    @Shadow
    public static int[] entityData;
    @Shadow
    public static int entityDataIndex;
    
    
    @Shadow
    public static void init() {
        throw new RuntimeException();
    }
    
    
    static {
        OFGlobal.copyContextFromObject = context -> {
            mc = context.mc;
            entityRenderer = context.entityRenderer;
            isInitializedOnce = context.isInitializedOnce;
            isShaderPackInitialized = context.isShaderPackInitialized;
            capabilities = context.capabilities;
            glVersionString = context.glVersionString;
            glVendorString = context.glVendorString;
            glRendererString = context.glRendererString;
            hasGlGenMipmap = context.hasGlGenMipmap;
            countResetDisplayLists = context.countResetDisplayLists;
            renderDisplayWidth = context.renderDisplayWidth;
            renderDisplayHeight = context.renderDisplayHeight;
            renderWidth = context.renderWidth;
            renderHeight = context.renderHeight;
            isRenderingWorld = context.isRenderingWorld;
            isRenderingSky = context.isRenderingSky;
            isCompositeRendered = context.isCompositeRendered;
            isRenderingDfb = context.isRenderingDfb;
            isShadowPass = context.isShadowPass;
            isEntitiesGlowing = context.isEntitiesGlowing;
            isSleeping = context.isSleeping;
            isRenderingFirstPersonHand = context.isRenderingFirstPersonHand;
            isHandRenderedMain = context.isHandRenderedMain;
            isHandRenderedOff = context.isHandRenderedOff;
            skipRenderHandMain = context.skipRenderHandMain;
            skipRenderHandOff = context.skipRenderHandOff;
            renderItemKeepDepthMask = context.renderItemKeepDepthMask;
            itemToRenderMainTranslucent = context.itemToRenderMainTranslucent;
            itemToRenderOffTranslucent = context.itemToRenderOffTranslucent;
            sunPosition = context.sunPosition;
            moonPosition = context.moonPosition;
            shadowLightPosition = context.shadowLightPosition;
            upPosition = context.upPosition;
            shadowLightPositionVector = context.shadowLightPositionVector;
            upPosModelView = context.upPosModelView;
            sunPosModelView = context.sunPosModelView;
            moonPosModelView = context.moonPosModelView;
            tempMat = context.tempMat;
            clearColor = context.clearColor;
            skyColorR = context.skyColorR;
            skyColorG = context.skyColorG;
            skyColorB = context.skyColorB;
            worldTime = context.worldTime;
            lastWorldTime = context.lastWorldTime;
            diffWorldTime = context.diffWorldTime;
            celestialAngle = context.celestialAngle;
            sunAngle = context.sunAngle;
            shadowAngle = context.shadowAngle;
            moonPhase = context.moonPhase;
            systemTime = context.systemTime;
            lastSystemTime = context.lastSystemTime;
            diffSystemTime = context.diffSystemTime;
            frameCounter = context.frameCounter;
            frameTime = context.frameTime;
            frameTimeCounter = context.frameTimeCounter;
            systemTimeInt32 = context.systemTimeInt32;
            rainStrength = context.rainStrength;
            wetness = context.wetness;
            wetnessHalfLife = context.wetnessHalfLife;
            drynessHalfLife = context.drynessHalfLife;
            eyeBrightnessHalflife = context.eyeBrightnessHalflife;
            usewetness = context.usewetness;
            isEyeInWater = context.isEyeInWater;
            eyeBrightness = context.eyeBrightness;
            eyeBrightnessFadeX = context.eyeBrightnessFadeX;
            eyeBrightnessFadeY = context.eyeBrightnessFadeY;
            eyePosY = context.eyePosY;
            centerDepth = context.centerDepth;
            centerDepthSmooth = context.centerDepthSmooth;
            centerDepthSmoothHalflife = context.centerDepthSmoothHalflife;
            centerDepthSmoothEnabled = context.centerDepthSmoothEnabled;
            superSamplingLevel = context.superSamplingLevel;
            nightVision = context.nightVision;
            blindness = context.blindness;
            lightmapEnabled = context.lightmapEnabled;
            fogEnabled = context.fogEnabled;
            renderStage = context.renderStage;
            baseAttribId = context.baseAttribId;
            entityAttrib = context.entityAttrib;
            midTexCoordAttrib = context.midTexCoordAttrib;
            tangentAttrib = context.tangentAttrib;
            useEntityAttrib = context.useEntityAttrib;
            useMidTexCoordAttrib = context.useMidTexCoordAttrib;
            useTangentAttrib = context.useTangentAttrib;
            progUseEntityAttrib = context.progUseEntityAttrib;
            progUseMidTexCoordAttrib = context.progUseMidTexCoordAttrib;
            progUseTangentAttrib = context.progUseTangentAttrib;
            progArbGeometryShader4 = context.progArbGeometryShader4;
            progMaxVerticesOut = context.progMaxVerticesOut;
            hasGeometryShaders = context.hasGeometryShaders;
            atlasSizeX = context.atlasSizeX;
            atlasSizeY = context.atlasSizeY;
            shaderUniforms = context.shaderUniforms;
            uniform_entityColor = context.uniform_entityColor;
            uniform_entityId = context.uniform_entityId;
            uniform_blockEntityId = context.uniform_blockEntityId;
            uniform_texture = context.uniform_texture;
            uniform_lightmap = context.uniform_lightmap;
            uniform_normals = context.uniform_normals;
            uniform_specular = context.uniform_specular;
            uniform_shadow = context.uniform_shadow;
            uniform_watershadow = context.uniform_watershadow;
            uniform_shadowtex0 = context.uniform_shadowtex0;
            uniform_shadowtex1 = context.uniform_shadowtex1;
            uniform_depthtex0 = context.uniform_depthtex0;
            uniform_depthtex1 = context.uniform_depthtex1;
            uniform_shadowcolor = context.uniform_shadowcolor;
            uniform_shadowcolor0 = context.uniform_shadowcolor0;
            uniform_shadowcolor1 = context.uniform_shadowcolor1;
            uniform_noisetex = context.uniform_noisetex;
            uniform_gcolor = context.uniform_gcolor;
            uniform_gdepth = context.uniform_gdepth;
            uniform_gnormal = context.uniform_gnormal;
            uniform_composite = context.uniform_composite;
            uniform_gaux1 = context.uniform_gaux1;
            uniform_gaux2 = context.uniform_gaux2;
            uniform_gaux3 = context.uniform_gaux3;
            uniform_gaux4 = context.uniform_gaux4;
            uniform_colortex0 = context.uniform_colortex0;
            uniform_colortex1 = context.uniform_colortex1;
            uniform_colortex2 = context.uniform_colortex2;
            uniform_colortex3 = context.uniform_colortex3;
            uniform_colortex4 = context.uniform_colortex4;
            uniform_colortex5 = context.uniform_colortex5;
            uniform_colortex6 = context.uniform_colortex6;
            uniform_colortex7 = context.uniform_colortex7;
            uniform_gdepthtex = context.uniform_gdepthtex;
            uniform_depthtex2 = context.uniform_depthtex2;
            uniform_colortex8 = context.uniform_colortex8;
            uniform_colortex9 = context.uniform_colortex9;
            uniform_colortex10 = context.uniform_colortex10;
            uniform_colortex11 = context.uniform_colortex11;
            uniform_colortex12 = context.uniform_colortex12;
            uniform_colortex13 = context.uniform_colortex13;
            uniform_colortex14 = context.uniform_colortex14;
            uniform_colortex15 = context.uniform_colortex15;
            uniform_colorimg0 = context.uniform_colorimg0;
            uniform_colorimg1 = context.uniform_colorimg1;
            uniform_colorimg2 = context.uniform_colorimg2;
            uniform_colorimg3 = context.uniform_colorimg3;
            uniform_colorimg4 = context.uniform_colorimg4;
            uniform_colorimg5 = context.uniform_colorimg5;
            uniform_shadowcolorimg0 = context.uniform_shadowcolorimg0;
            uniform_shadowcolorimg1 = context.uniform_shadowcolorimg1;
            uniform_tex = context.uniform_tex;
            uniform_heldItemId = context.uniform_heldItemId;
            uniform_heldBlockLightValue = context.uniform_heldBlockLightValue;
            uniform_heldItemId2 = context.uniform_heldItemId2;
            uniform_heldBlockLightValue2 = context.uniform_heldBlockLightValue2;
            uniform_fogMode = context.uniform_fogMode;
            uniform_fogDensity = context.uniform_fogDensity;
            uniform_fogColor = context.uniform_fogColor;
            uniform_skyColor = context.uniform_skyColor;
            uniform_worldTime = context.uniform_worldTime;
            uniform_worldDay = context.uniform_worldDay;
            uniform_moonPhase = context.uniform_moonPhase;
            uniform_frameCounter = context.uniform_frameCounter;
            uniform_frameTime = context.uniform_frameTime;
            uniform_frameTimeCounter = context.uniform_frameTimeCounter;
            uniform_sunAngle = context.uniform_sunAngle;
            uniform_shadowAngle = context.uniform_shadowAngle;
            uniform_rainStrength = context.uniform_rainStrength;
            uniform_aspectRatio = context.uniform_aspectRatio;
            uniform_viewWidth = context.uniform_viewWidth;
            uniform_viewHeight = context.uniform_viewHeight;
            uniform_near = context.uniform_near;
            uniform_far = context.uniform_far;
            uniform_sunPosition = context.uniform_sunPosition;
            uniform_moonPosition = context.uniform_moonPosition;
            uniform_shadowLightPosition = context.uniform_shadowLightPosition;
            uniform_upPosition = context.uniform_upPosition;
            uniform_previousCameraPosition = context.uniform_previousCameraPosition;
            uniform_cameraPosition = context.uniform_cameraPosition;
            uniform_gbufferModelView = context.uniform_gbufferModelView;
            uniform_gbufferModelViewInverse = context.uniform_gbufferModelViewInverse;
            uniform_gbufferPreviousProjection = context.uniform_gbufferPreviousProjection;
            uniform_gbufferProjection = context.uniform_gbufferProjection;
            uniform_gbufferProjectionInverse = context.uniform_gbufferProjectionInverse;
            uniform_gbufferPreviousModelView = context.uniform_gbufferPreviousModelView;
            uniform_shadowProjection = context.uniform_shadowProjection;
            uniform_shadowProjectionInverse = context.uniform_shadowProjectionInverse;
            uniform_shadowModelView = context.uniform_shadowModelView;
            uniform_shadowModelViewInverse = context.uniform_shadowModelViewInverse;
            uniform_wetness = context.uniform_wetness;
            uniform_eyeAltitude = context.uniform_eyeAltitude;
            uniform_eyeBrightness = context.uniform_eyeBrightness;
            uniform_eyeBrightnessSmooth = context.uniform_eyeBrightnessSmooth;
            uniform_terrainTextureSize = context.uniform_terrainTextureSize;
            uniform_terrainIconSize = context.uniform_terrainIconSize;
            uniform_isEyeInWater = context.uniform_isEyeInWater;
            uniform_nightVision = context.uniform_nightVision;
            uniform_blindness = context.uniform_blindness;
            uniform_screenBrightness = context.uniform_screenBrightness;
            uniform_hideGUI = context.uniform_hideGUI;
            uniform_centerDepthSmooth = context.uniform_centerDepthSmooth;
            uniform_atlasSize = context.uniform_atlasSize;
            uniform_spriteBounds = context.uniform_spriteBounds;
            uniform_blendFunc = context.uniform_blendFunc;
            uniform_instanceId = context.uniform_instanceId;
            uniform_playerMood = context.uniform_playerMood;
            uniform_renderStage = context.uniform_renderStage;
            previousCameraPositionX = context.previousCameraPositionX;
            previousCameraPositionY = context.previousCameraPositionY;
            previousCameraPositionZ = context.previousCameraPositionZ;
            cameraPositionX = context.cameraPositionX;
            cameraPositionY = context.cameraPositionY;
            cameraPositionZ = context.cameraPositionZ;
            cameraOffsetX = context.cameraOffsetX;
            cameraOffsetZ = context.cameraOffsetZ;
            hasShadowMap = context.hasShadowMap;
            needResizeShadow = context.needResizeShadow;
            shadowMapWidth = context.shadowMapWidth;
            shadowMapHeight = context.shadowMapHeight;
            spShadowMapWidth = context.spShadowMapWidth;
            spShadowMapHeight = context.spShadowMapHeight;
            shadowMapFOV = context.shadowMapFOV;
            shadowMapHalfPlane = context.shadowMapHalfPlane;
            shadowMapIsOrtho = context.shadowMapIsOrtho;
            shadowDistanceRenderMul = context.shadowDistanceRenderMul;
            shouldSkipDefaultShadow = context.shouldSkipDefaultShadow;
            waterShadowEnabled = context.waterShadowEnabled;
            MaxDrawBuffers = context.MaxDrawBuffers;
            MaxColorBuffers = context.MaxColorBuffers;
            MaxDepthBuffers = context.MaxDepthBuffers;
            MaxShadowColorBuffers = context.MaxShadowColorBuffers;
            MaxShadowDepthBuffers = context.MaxShadowDepthBuffers;
            usedColorBuffers = context.usedColorBuffers;
            usedDepthBuffers = context.usedDepthBuffers;
            usedShadowColorBuffers = context.usedShadowColorBuffers;
            usedShadowDepthBuffers = context.usedShadowDepthBuffers;
            usedColorAttachs = context.usedColorAttachs;
            usedDrawBuffers = context.usedDrawBuffers;
            bindImageTextures = context.bindImageTextures;
            dfb = context.dfb;
            sfb = context.sfb;
            gbuffersFormat = context.gbuffersFormat;
            gbuffersClear = context.gbuffersClear;
            gbuffersClearColor = context.gbuffersClearColor;
            CLEAR_COLOR_0 = context.CLEAR_COLOR_0;
            CLEAR_COLOR_1 = context.CLEAR_COLOR_1;
            shadowBuffersFormat = context.shadowBuffersFormat;
            shadowBuffersClear = context.shadowBuffersClear;
            shadowBuffersClearColor = context.shadowBuffersClearColor;
            programs = context.programs;
            ProgramNone = context.ProgramNone;
            ProgramShadow = context.ProgramShadow;
            ProgramShadowSolid = context.ProgramShadowSolid;
            ProgramShadowCutout = context.ProgramShadowCutout;
            ProgramsShadowcomp = context.ProgramsShadowcomp;
            ProgramsPrepare = context.ProgramsPrepare;
            ProgramBasic = context.ProgramBasic;
            ProgramTextured = context.ProgramTextured;
            ProgramTexturedLit = context.ProgramTexturedLit;
            ProgramSkyBasic = context.ProgramSkyBasic;
            ProgramSkyTextured = context.ProgramSkyTextured;
            ProgramClouds = context.ProgramClouds;
            ProgramTerrain = context.ProgramTerrain;
            ProgramTerrainSolid = context.ProgramTerrainSolid;
            ProgramTerrainCutoutMip = context.ProgramTerrainCutoutMip;
            ProgramTerrainCutout = context.ProgramTerrainCutout;
            ProgramDamagedBlock = context.ProgramDamagedBlock;
            ProgramBlock = context.ProgramBlock;
            ProgramBeaconBeam = context.ProgramBeaconBeam;
            ProgramItem = context.ProgramItem;
            ProgramEntities = context.ProgramEntities;
            ProgramEntitiesGlowing = context.ProgramEntitiesGlowing;
            ProgramArmorGlint = context.ProgramArmorGlint;
            ProgramSpiderEyes = context.ProgramSpiderEyes;
            ProgramHand = context.ProgramHand;
            ProgramWeather = context.ProgramWeather;
            ProgramDeferredPre = context.ProgramDeferredPre;
            ProgramsDeferred = context.ProgramsDeferred;
            ProgramDeferred = context.ProgramDeferred;
            ProgramWater = context.ProgramWater;
            ProgramHandWater = context.ProgramHandWater;
            ProgramCompositePre = context.ProgramCompositePre;
            ProgramsComposite = context.ProgramsComposite;
            ProgramComposite = context.ProgramComposite;
            ProgramFinal=context.ProgramFinal ;
            ProgramCount = context.ProgramCount;
            ProgramsAll = context.ProgramsAll;
            activeProgram = context.activeProgram;
            activeProgramID = context.activeProgramID;
            programStack = context.programStack;
            hasDeferredPrograms = context.hasDeferredPrograms;
            hasShadowcompPrograms = context.hasShadowcompPrograms;
            hasPreparePrograms = context.hasPreparePrograms;
            loadedShaders = context.loadedShaders;
            shadersConfig = context.shadersConfig;
            defaultTexture = context.defaultTexture;
            shadowHardwareFilteringEnabled = context.shadowHardwareFilteringEnabled;
            shadowMipmapEnabled = context.shadowMipmapEnabled;
            shadowFilterNearest = context.shadowFilterNearest;
            shadowColorMipmapEnabled = context.shadowColorMipmapEnabled;
            shadowColorFilterNearest = context.shadowColorFilterNearest;
            configTweakBlockDamage = context.configTweakBlockDamage;
            configCloudShadow = context.configCloudShadow;
            configHandDepthMul = context.configHandDepthMul;
            configRenderResMul = context.configRenderResMul;
            configShadowResMul = context.configShadowResMul;
            configTexMinFilB = context.configTexMinFilB;
            configTexMinFilN = context.configTexMinFilN;
            configTexMinFilS = context.configTexMinFilS;
            configTexMagFilB = context.configTexMagFilB;
            configTexMagFilN = context.configTexMagFilN;
            configTexMagFilS = context.configTexMagFilS;
            configShadowClipFrustrum = context.configShadowClipFrustrum;
            configNormalMap = context.configNormalMap;
            configSpecularMap = context.configSpecularMap;
            configOldLighting = context.configOldLighting;
            configOldHandLight = context.configOldHandLight;
            configAntialiasingLevel = context.configAntialiasingLevel;
            texMinFilRange = context.texMinFilRange;
            texMagFilRange = context.texMagFilRange;
            texMinFilDesc = context.texMinFilDesc;
            texMagFilDesc = context.texMagFilDesc;
            texMinFilValue = context.texMinFilValue;
            texMagFilValue = context.texMagFilValue;
            shaderPack = context.shaderPack;
            shaderPackLoaded = context.shaderPackLoaded;
            currentShaderName = context.currentShaderName;
            SHADER_PACK_NAME_NONE = context.SHADER_PACK_NAME_NONE;
            SHADER_PACK_NAME_DEFAULT = context.SHADER_PACK_NAME_DEFAULT;
            SHADER_PACKS_DIR_NAME = context.SHADER_PACKS_DIR_NAME;
            OPTIONS_FILE_NAME = context.OPTIONS_FILE_NAME;
            shaderPacksDir = context.shaderPacksDir;
            configFile = context.configFile;
            shaderPackOptions = context.shaderPackOptions;
            shaderPackOptionSliders = context.shaderPackOptionSliders;
            shaderPackProfiles = context.shaderPackProfiles;
            shaderPackGuiScreens = context.shaderPackGuiScreens;
            shaderPackProgramConditions = context.shaderPackProgramConditions;
            PATH_SHADERS_PROPERTIES = context.PATH_SHADERS_PROPERTIES;
            shaderPackClouds = context.shaderPackClouds;
            shaderPackOldLighting = context.shaderPackOldLighting;
            shaderPackOldHandLight = context.shaderPackOldHandLight;
            shaderPackDynamicHandLight = context.shaderPackDynamicHandLight;
            shaderPackShadowTranslucent = context.shaderPackShadowTranslucent;
            shaderPackUnderwaterOverlay = context.shaderPackUnderwaterOverlay;
            shaderPackSun = context.shaderPackSun;
            shaderPackMoon = context.shaderPackMoon;
            shaderPackVignette = context.shaderPackVignette;
            shaderPackBackFaceSolid = context.shaderPackBackFaceSolid;
            shaderPackBackFaceCutout = context.shaderPackBackFaceCutout;
            shaderPackBackFaceCutoutMipped = context.shaderPackBackFaceCutoutMipped;
            shaderPackBackFaceTranslucent = context.shaderPackBackFaceTranslucent;
            shaderPackRainDepth = context.shaderPackRainDepth;
            shaderPackBeaconBeamDepth = context.shaderPackBeaconBeamDepth;
            shaderPackSeparateAo = context.shaderPackSeparateAo;
            shaderPackFrustumCulling = context.shaderPackFrustumCulling;
            shaderPackResources = context.shaderPackResources;
            currentWorld = context.currentWorld;
            shaderPackDimensions = context.shaderPackDimensions;
            customTexturesGbuffers = context.customTexturesGbuffers;
            customTexturesComposite = context.customTexturesComposite;
            customTexturesDeferred = context.customTexturesDeferred;
            customTexturesShadowcomp = context.customTexturesShadowcomp;
            customTexturesPrepare = context.customTexturesPrepare;
            noiseTexturePath = context.noiseTexturePath;
            colorBufferSizes = context.colorBufferSizes;
            customUniforms = context.customUniforms;
            saveFinalShaders = context.saveFinalShaders;
            blockLightLevel05 = context.blockLightLevel05;
            blockLightLevel06 = context.blockLightLevel06;
            blockLightLevel08 = context.blockLightLevel08;
            aoLevel = context.aoLevel;
            sunPathRotation = context.sunPathRotation;
            shadowAngleInterval = context.shadowAngleInterval;
            fogMode = context.fogMode;
            fogDensity = context.fogDensity;
            fogColorR = context.fogColorR;
            fogColorG = context.fogColorG;
            fogColorB = context.fogColorB;
            shadowIntervalSize = context.shadowIntervalSize;
            terrainIconSize = context.terrainIconSize;
            terrainTextureSize = context.terrainTextureSize;
            noiseTexture = context.noiseTexture;
            noiseTextureEnabled = context.noiseTextureEnabled;
            noiseTextureResolution = context.noiseTextureResolution;
            colorTextureImageUnit = context.colorTextureImageUnit;
            depthTextureImageUnit = context.depthTextureImageUnit;
            shadowColorTextureImageUnit = context.shadowColorTextureImageUnit;
            shadowDepthTextureImageUnit = context.shadowDepthTextureImageUnit;
            colorImageUnit = context.colorImageUnit;
            shadowColorImageUnit = context.shadowColorImageUnit;
            bigBufferSize = context.bigBufferSize;
            bigBuffer = context.bigBuffer;
            faProjection = context.faProjection;
            faProjectionInverse = context.faProjectionInverse;
            faModelView = context.faModelView;
            faModelViewInverse = context.faModelViewInverse;
            faShadowProjection = context.faShadowProjection;
            faShadowProjectionInverse = context.faShadowProjectionInverse;
            faShadowModelView = context.faShadowModelView;
            faShadowModelViewInverse = context.faShadowModelViewInverse;
            projection = context.projection;
            projectionInverse = context.projectionInverse;
            modelView = context.modelView;
            modelViewInverse = context.modelViewInverse;
            shadowProjection = context.shadowProjection;
            shadowProjectionInverse = context.shadowProjectionInverse;
            shadowModelView = context.shadowModelView;
            shadowModelViewInverse = context.shadowModelViewInverse;
            previousProjection = context.previousProjection;
            previousModelView = context.previousModelView;
            tempMatrixDirectBuffer = context.tempMatrixDirectBuffer;
            tempDirectFloatBuffer = context.tempDirectFloatBuffer;
            dfbDrawBuffers = context.dfbDrawBuffers;
            sfbDrawBuffers = context.sfbDrawBuffers;
            drawBuffersNone = context.drawBuffersNone;
            drawBuffersColorAtt = context.drawBuffersColorAtt;
            glDebugGroups = context.glDebugGroups;
            glDebugGroupProgram = context.glDebugGroupProgram;
            mapBlockToEntityData = context.mapBlockToEntityData;
            formatNames = context.formatNames;
            formatIds = context.formatIds;
            patternLoadEntityDataMap = context.patternLoadEntityDataMap;
            entityData = context.entityData;
            entityDataIndex = context.entityDataIndex;
            
            
        };
        
        OFGlobal.copyContextToObject = context -> {
            context.mc = mc;
            context.entityRenderer = entityRenderer;
            context.isInitializedOnce = isInitializedOnce;
            context.isShaderPackInitialized = isShaderPackInitialized;
            context.capabilities = capabilities;
            context.glVersionString = glVersionString;
            context.glVendorString = glVendorString;
            context.glRendererString = glRendererString;
            context.hasGlGenMipmap = hasGlGenMipmap;
            context.countResetDisplayLists = countResetDisplayLists;
            context.renderDisplayWidth = renderDisplayWidth;
            context.renderDisplayHeight = renderDisplayHeight;
            context.renderWidth = renderWidth;
            context.renderHeight = renderHeight;
            context.isRenderingWorld = isRenderingWorld;
            context.isRenderingSky = isRenderingSky;
            context.isCompositeRendered = isCompositeRendered;
            context.isRenderingDfb = isRenderingDfb;
            context.isShadowPass = isShadowPass;
            context.isEntitiesGlowing = isEntitiesGlowing;
            context.isSleeping = isSleeping;
            context.isRenderingFirstPersonHand = isRenderingFirstPersonHand;
            context.isHandRenderedMain = isHandRenderedMain;
            context.isHandRenderedOff = isHandRenderedOff;
            context.skipRenderHandMain = skipRenderHandMain;
            context.skipRenderHandOff = skipRenderHandOff;
            context.renderItemKeepDepthMask = renderItemKeepDepthMask;
            context.itemToRenderMainTranslucent = itemToRenderMainTranslucent;
            context.itemToRenderOffTranslucent = itemToRenderOffTranslucent;
            context.sunPosition = sunPosition;
            context.moonPosition = moonPosition;
            context.shadowLightPosition = shadowLightPosition;
            context.upPosition = upPosition;
            context.shadowLightPositionVector = shadowLightPositionVector;
            context.upPosModelView = upPosModelView;
            context.sunPosModelView = sunPosModelView;
            context.moonPosModelView = moonPosModelView;
            context.tempMat = tempMat;
            context.clearColor = clearColor;
            context.skyColorR = skyColorR;
            context.skyColorG = skyColorG;
            context.skyColorB = skyColorB;
            context.worldTime = worldTime;
            context.lastWorldTime = lastWorldTime;
            context.diffWorldTime = diffWorldTime;
            context.celestialAngle = celestialAngle;
            context.sunAngle = sunAngle;
            context.shadowAngle = shadowAngle;
            context.moonPhase = moonPhase;
            context.systemTime = systemTime;
            context.lastSystemTime = lastSystemTime;
            context.diffSystemTime = diffSystemTime;
            context.frameCounter = frameCounter;
            context.frameTime = frameTime;
            context.frameTimeCounter = frameTimeCounter;
            context.systemTimeInt32 = systemTimeInt32;
            context.rainStrength = rainStrength;
            context.wetness = wetness;
            context.wetnessHalfLife = wetnessHalfLife;
            context.drynessHalfLife = drynessHalfLife;
            context.eyeBrightnessHalflife = eyeBrightnessHalflife;
            context.usewetness = usewetness;
            context.isEyeInWater = isEyeInWater;
            context.eyeBrightness = eyeBrightness;
            context.eyeBrightnessFadeX = eyeBrightnessFadeX;
            context.eyeBrightnessFadeY = eyeBrightnessFadeY;
            context.eyePosY = eyePosY;
            context.centerDepth = centerDepth;
            context.centerDepthSmooth = centerDepthSmooth;
            context.centerDepthSmoothHalflife = centerDepthSmoothHalflife;
            context.centerDepthSmoothEnabled = centerDepthSmoothEnabled;
            context.superSamplingLevel = superSamplingLevel;
            context.nightVision = nightVision;
            context.blindness = blindness;
            context.lightmapEnabled = lightmapEnabled;
            context.fogEnabled = fogEnabled;
            context.renderStage = renderStage;
            context.baseAttribId = baseAttribId;
            context.entityAttrib = entityAttrib;
            context.midTexCoordAttrib = midTexCoordAttrib;
            context.tangentAttrib = tangentAttrib;
            context.useEntityAttrib = useEntityAttrib;
            context.useMidTexCoordAttrib = useMidTexCoordAttrib;
            context.useTangentAttrib = useTangentAttrib;
            context.progUseEntityAttrib = progUseEntityAttrib;
            context.progUseMidTexCoordAttrib = progUseMidTexCoordAttrib;
            context.progUseTangentAttrib = progUseTangentAttrib;
            context.progArbGeometryShader4 = progArbGeometryShader4;
            context.progMaxVerticesOut = progMaxVerticesOut;
            context.hasGeometryShaders = hasGeometryShaders;
            context.atlasSizeX = atlasSizeX;
            context.atlasSizeY = atlasSizeY;
            context.shaderUniforms = shaderUniforms;
            context.uniform_entityColor = uniform_entityColor;
            context.uniform_entityId = uniform_entityId;
            context.uniform_blockEntityId = uniform_blockEntityId;
            context.uniform_texture = uniform_texture;
            context.uniform_lightmap = uniform_lightmap;
            context.uniform_normals = uniform_normals;
            context.uniform_specular = uniform_specular;
            context.uniform_shadow = uniform_shadow;
            context.uniform_watershadow = uniform_watershadow;
            context.uniform_shadowtex0 = uniform_shadowtex0;
            context.uniform_shadowtex1 = uniform_shadowtex1;
            context.uniform_depthtex0 = uniform_depthtex0;
            context.uniform_depthtex1 = uniform_depthtex1;
            context.uniform_shadowcolor = uniform_shadowcolor;
            context.uniform_shadowcolor0 = uniform_shadowcolor0;
            context.uniform_shadowcolor1 = uniform_shadowcolor1;
            context.uniform_noisetex = uniform_noisetex;
            context.uniform_gcolor = uniform_gcolor;
            context.uniform_gdepth = uniform_gdepth;
            context.uniform_gnormal = uniform_gnormal;
            context.uniform_composite = uniform_composite;
            context.uniform_gaux1 = uniform_gaux1;
            context.uniform_gaux2 = uniform_gaux2;
            context.uniform_gaux3 = uniform_gaux3;
            context.uniform_gaux4 = uniform_gaux4;
            context.uniform_colortex0 = uniform_colortex0;
            context.uniform_colortex1 = uniform_colortex1;
            context.uniform_colortex2 = uniform_colortex2;
            context.uniform_colortex3 = uniform_colortex3;
            context.uniform_colortex4 = uniform_colortex4;
            context.uniform_colortex5 = uniform_colortex5;
            context.uniform_colortex6 = uniform_colortex6;
            context.uniform_colortex7 = uniform_colortex7;
            context.uniform_gdepthtex = uniform_gdepthtex;
            context.uniform_depthtex2 = uniform_depthtex2;
            context.uniform_colortex8 = uniform_colortex8;
            context.uniform_colortex9 = uniform_colortex9;
            context.uniform_colortex10 = uniform_colortex10;
            context.uniform_colortex11 = uniform_colortex11;
            context.uniform_colortex12 = uniform_colortex12;
            context.uniform_colortex13 = uniform_colortex13;
            context.uniform_colortex14 = uniform_colortex14;
            context.uniform_colortex15 = uniform_colortex15;
            context.uniform_colorimg0 = uniform_colorimg0;
            context.uniform_colorimg1 = uniform_colorimg1;
            context.uniform_colorimg2 = uniform_colorimg2;
            context.uniform_colorimg3 = uniform_colorimg3;
            context.uniform_colorimg4 = uniform_colorimg4;
            context.uniform_colorimg5 = uniform_colorimg5;
            context.uniform_shadowcolorimg0 = uniform_shadowcolorimg0;
            context.uniform_shadowcolorimg1 = uniform_shadowcolorimg1;
            context.uniform_tex = uniform_tex;
            context.uniform_heldItemId = uniform_heldItemId;
            context.uniform_heldBlockLightValue = uniform_heldBlockLightValue;
            context.uniform_heldItemId2 = uniform_heldItemId2;
            context.uniform_heldBlockLightValue2 = uniform_heldBlockLightValue2;
            context.uniform_fogMode = uniform_fogMode;
            context.uniform_fogDensity = uniform_fogDensity;
            context.uniform_fogColor = uniform_fogColor;
            context.uniform_skyColor = uniform_skyColor;
            context.uniform_worldTime = uniform_worldTime;
            context.uniform_worldDay = uniform_worldDay;
            context.uniform_moonPhase = uniform_moonPhase;
            context.uniform_frameCounter = uniform_frameCounter;
            context.uniform_frameTime = uniform_frameTime;
            context.uniform_frameTimeCounter = uniform_frameTimeCounter;
            context.uniform_sunAngle = uniform_sunAngle;
            context.uniform_shadowAngle = uniform_shadowAngle;
            context.uniform_rainStrength = uniform_rainStrength;
            context.uniform_aspectRatio = uniform_aspectRatio;
            context.uniform_viewWidth = uniform_viewWidth;
            context.uniform_viewHeight = uniform_viewHeight;
            context.uniform_near = uniform_near;
            context.uniform_far = uniform_far;
            context.uniform_sunPosition = uniform_sunPosition;
            context.uniform_moonPosition = uniform_moonPosition;
            context.uniform_shadowLightPosition = uniform_shadowLightPosition;
            context.uniform_upPosition = uniform_upPosition;
            context.uniform_previousCameraPosition = uniform_previousCameraPosition;
            context.uniform_cameraPosition = uniform_cameraPosition;
            context.uniform_gbufferModelView = uniform_gbufferModelView;
            context.uniform_gbufferModelViewInverse = uniform_gbufferModelViewInverse;
            context.uniform_gbufferPreviousProjection = uniform_gbufferPreviousProjection;
            context.uniform_gbufferProjection = uniform_gbufferProjection;
            context.uniform_gbufferProjectionInverse = uniform_gbufferProjectionInverse;
            context.uniform_gbufferPreviousModelView = uniform_gbufferPreviousModelView;
            context.uniform_shadowProjection = uniform_shadowProjection;
            context.uniform_shadowProjectionInverse = uniform_shadowProjectionInverse;
            context.uniform_shadowModelView = uniform_shadowModelView;
            context.uniform_shadowModelViewInverse = uniform_shadowModelViewInverse;
            context.uniform_wetness = uniform_wetness;
            context.uniform_eyeAltitude = uniform_eyeAltitude;
            context.uniform_eyeBrightness = uniform_eyeBrightness;
            context.uniform_eyeBrightnessSmooth = uniform_eyeBrightnessSmooth;
            context.uniform_terrainTextureSize = uniform_terrainTextureSize;
            context.uniform_terrainIconSize = uniform_terrainIconSize;
            context.uniform_isEyeInWater = uniform_isEyeInWater;
            context.uniform_nightVision = uniform_nightVision;
            context.uniform_blindness = uniform_blindness;
            context.uniform_screenBrightness = uniform_screenBrightness;
            context.uniform_hideGUI = uniform_hideGUI;
            context.uniform_centerDepthSmooth = uniform_centerDepthSmooth;
            context.uniform_atlasSize = uniform_atlasSize;
            context.uniform_spriteBounds = uniform_spriteBounds;
            context.uniform_blendFunc = uniform_blendFunc;
            context.uniform_instanceId = uniform_instanceId;
            context.uniform_playerMood = uniform_playerMood;
            context.uniform_renderStage = uniform_renderStage;
            context.previousCameraPositionX = previousCameraPositionX;
            context.previousCameraPositionY = previousCameraPositionY;
            context.previousCameraPositionZ = previousCameraPositionZ;
            context.cameraPositionX = cameraPositionX;
            context.cameraPositionY = cameraPositionY;
            context.cameraPositionZ = cameraPositionZ;
            context.cameraOffsetX = cameraOffsetX;
            context.cameraOffsetZ = cameraOffsetZ;
            context.hasShadowMap = hasShadowMap;
            context.needResizeShadow = needResizeShadow;
            context.shadowMapWidth = shadowMapWidth;
            context.shadowMapHeight = shadowMapHeight;
            context.spShadowMapWidth = spShadowMapWidth;
            context.spShadowMapHeight = spShadowMapHeight;
            context.shadowMapFOV = shadowMapFOV;
            context.shadowMapHalfPlane = shadowMapHalfPlane;
            context.shadowMapIsOrtho = shadowMapIsOrtho;
            context.shadowDistanceRenderMul = shadowDistanceRenderMul;
            context.shouldSkipDefaultShadow = shouldSkipDefaultShadow;
            context.waterShadowEnabled = waterShadowEnabled;
            context.MaxDrawBuffers = MaxDrawBuffers;
            context.MaxColorBuffers = MaxColorBuffers;
            context.MaxDepthBuffers = MaxDepthBuffers;
            context.MaxShadowColorBuffers = MaxShadowColorBuffers;
            context.MaxShadowDepthBuffers = MaxShadowDepthBuffers;
            context.usedColorBuffers = usedColorBuffers;
            context.usedDepthBuffers = usedDepthBuffers;
            context.usedShadowColorBuffers = usedShadowColorBuffers;
            context.usedShadowDepthBuffers = usedShadowDepthBuffers;
            context.usedColorAttachs = usedColorAttachs;
            context.usedDrawBuffers = usedDrawBuffers;
            context.bindImageTextures = bindImageTextures;
            context.dfb = dfb;
            context.sfb = sfb;
            context.gbuffersFormat = gbuffersFormat;
            context.gbuffersClear = gbuffersClear;
            context.gbuffersClearColor = gbuffersClearColor;
            context.CLEAR_COLOR_0 = CLEAR_COLOR_0;
            context.CLEAR_COLOR_1 = CLEAR_COLOR_1;
            context.shadowBuffersFormat = shadowBuffersFormat;
            context.shadowBuffersClear = shadowBuffersClear;
            context.shadowBuffersClearColor = shadowBuffersClearColor;
            context.programs = programs;
            context.ProgramNone = ProgramNone;
            context.ProgramShadow = ProgramShadow;
            context.ProgramShadowSolid = ProgramShadowSolid;
            context.ProgramShadowCutout = ProgramShadowCutout;
            context.ProgramsShadowcomp = ProgramsShadowcomp;
            context.ProgramsPrepare = ProgramsPrepare;
            context.ProgramBasic = ProgramBasic;
            context.ProgramTextured = ProgramTextured;
            context.ProgramTexturedLit = ProgramTexturedLit;
            context.ProgramSkyBasic = ProgramSkyBasic;
            context.ProgramSkyTextured = ProgramSkyTextured;
            context.ProgramClouds = ProgramClouds;
            context.ProgramTerrain = ProgramTerrain;
            context.ProgramTerrainSolid = ProgramTerrainSolid;
            context.ProgramTerrainCutoutMip = ProgramTerrainCutoutMip;
            context.ProgramTerrainCutout = ProgramTerrainCutout;
            context.ProgramDamagedBlock = ProgramDamagedBlock;
            context.ProgramBlock = ProgramBlock;
            context.ProgramBeaconBeam = ProgramBeaconBeam;
            context.ProgramItem = ProgramItem;
            context.ProgramEntities = ProgramEntities;
            context.ProgramEntitiesGlowing = ProgramEntitiesGlowing;
            context.ProgramArmorGlint = ProgramArmorGlint;
            context.ProgramSpiderEyes = ProgramSpiderEyes;
            context.ProgramHand = ProgramHand;
            context.ProgramWeather = ProgramWeather;
            context.ProgramDeferredPre = ProgramDeferredPre;
            context.ProgramsDeferred = ProgramsDeferred;
            context.ProgramDeferred = ProgramDeferred;
            context.ProgramWater = ProgramWater;
            context.ProgramHandWater = ProgramHandWater;
            context.ProgramCompositePre = ProgramCompositePre;
            context.ProgramsComposite = ProgramsComposite;
            context.ProgramComposite = ProgramComposite;
            context.ProgramFinal=ProgramFinal ;
            context.ProgramCount = ProgramCount;
            context.ProgramsAll = ProgramsAll;
            context.activeProgram = activeProgram;
            context.activeProgramID = activeProgramID;
            context.programStack = programStack;
            context.hasDeferredPrograms = hasDeferredPrograms;
            context.hasShadowcompPrograms = hasShadowcompPrograms;
            context.hasPreparePrograms = hasPreparePrograms;
            context.loadedShaders = loadedShaders;
            context.shadersConfig = shadersConfig;
            context.defaultTexture = defaultTexture;
            context.shadowHardwareFilteringEnabled = shadowHardwareFilteringEnabled;
            context.shadowMipmapEnabled = shadowMipmapEnabled;
            context.shadowFilterNearest = shadowFilterNearest;
            context.shadowColorMipmapEnabled = shadowColorMipmapEnabled;
            context.shadowColorFilterNearest = shadowColorFilterNearest;
            context.configTweakBlockDamage = configTweakBlockDamage;
            context.configCloudShadow = configCloudShadow;
            context.configHandDepthMul = configHandDepthMul;
            context.configRenderResMul = configRenderResMul;
            context.configShadowResMul = configShadowResMul;
            context.configTexMinFilB = configTexMinFilB;
            context.configTexMinFilN = configTexMinFilN;
            context.configTexMinFilS = configTexMinFilS;
            context.configTexMagFilB = configTexMagFilB;
            context.configTexMagFilN = configTexMagFilN;
            context.configTexMagFilS = configTexMagFilS;
            context.configShadowClipFrustrum = configShadowClipFrustrum;
            context.configNormalMap = configNormalMap;
            context.configSpecularMap = configSpecularMap;
            context.configOldLighting = configOldLighting;
            context.configOldHandLight = configOldHandLight;
            context.configAntialiasingLevel = configAntialiasingLevel;
            context.texMinFilRange = texMinFilRange;
            context.texMagFilRange = texMagFilRange;
            context.texMinFilDesc = texMinFilDesc;
            context.texMagFilDesc = texMagFilDesc;
            context.texMinFilValue = texMinFilValue;
            context.texMagFilValue = texMagFilValue;
            context.shaderPack = shaderPack;
            context.shaderPackLoaded = shaderPackLoaded;
            context.currentShaderName = currentShaderName;
            context.SHADER_PACK_NAME_NONE = SHADER_PACK_NAME_NONE;
            context.SHADER_PACK_NAME_DEFAULT = SHADER_PACK_NAME_DEFAULT;
            context.SHADER_PACKS_DIR_NAME = SHADER_PACKS_DIR_NAME;
            context.OPTIONS_FILE_NAME = OPTIONS_FILE_NAME;
            context.shaderPacksDir = shaderPacksDir;
            context.configFile = configFile;
            context.shaderPackOptions = shaderPackOptions;
            context.shaderPackOptionSliders = shaderPackOptionSliders;
            context.shaderPackProfiles = shaderPackProfiles;
            context.shaderPackGuiScreens = shaderPackGuiScreens;
            context.shaderPackProgramConditions = shaderPackProgramConditions;
            context.PATH_SHADERS_PROPERTIES = PATH_SHADERS_PROPERTIES;
            context.shaderPackClouds = shaderPackClouds;
            context.shaderPackOldLighting = shaderPackOldLighting;
            context.shaderPackOldHandLight = shaderPackOldHandLight;
            context.shaderPackDynamicHandLight = shaderPackDynamicHandLight;
            context.shaderPackShadowTranslucent = shaderPackShadowTranslucent;
            context.shaderPackUnderwaterOverlay = shaderPackUnderwaterOverlay;
            context.shaderPackSun = shaderPackSun;
            context.shaderPackMoon = shaderPackMoon;
            context.shaderPackVignette = shaderPackVignette;
            context.shaderPackBackFaceSolid = shaderPackBackFaceSolid;
            context.shaderPackBackFaceCutout = shaderPackBackFaceCutout;
            context.shaderPackBackFaceCutoutMipped = shaderPackBackFaceCutoutMipped;
            context.shaderPackBackFaceTranslucent = shaderPackBackFaceTranslucent;
            context.shaderPackRainDepth = shaderPackRainDepth;
            context.shaderPackBeaconBeamDepth = shaderPackBeaconBeamDepth;
            context.shaderPackSeparateAo = shaderPackSeparateAo;
            context.shaderPackFrustumCulling = shaderPackFrustumCulling;
            context.shaderPackResources = shaderPackResources;
            context.currentWorld = currentWorld;
            context.shaderPackDimensions = shaderPackDimensions;
            context.customTexturesGbuffers = customTexturesGbuffers;
            context.customTexturesComposite = customTexturesComposite;
            context.customTexturesDeferred = customTexturesDeferred;
            context.customTexturesShadowcomp = customTexturesShadowcomp;
            context.customTexturesPrepare = customTexturesPrepare;
            context.noiseTexturePath = noiseTexturePath;
            context.colorBufferSizes = colorBufferSizes;
            context.customUniforms = customUniforms;
            context.saveFinalShaders = saveFinalShaders;
            context.blockLightLevel05 = blockLightLevel05;
            context.blockLightLevel06 = blockLightLevel06;
            context.blockLightLevel08 = blockLightLevel08;
            context.aoLevel = aoLevel;
            context.sunPathRotation = sunPathRotation;
            context.shadowAngleInterval = shadowAngleInterval;
            context.fogMode = fogMode;
            context.fogDensity = fogDensity;
            context.fogColorR = fogColorR;
            context.fogColorG = fogColorG;
            context.fogColorB = fogColorB;
            context.shadowIntervalSize = shadowIntervalSize; ;
            context.terrainIconSize = terrainIconSize;
            context.terrainTextureSize = terrainTextureSize;
            context.noiseTexture = noiseTexture;
            context.noiseTextureEnabled = noiseTextureEnabled;
            context.noiseTextureResolution = noiseTextureResolution;
            context.colorTextureImageUnit = colorTextureImageUnit;
            context.depthTextureImageUnit = depthTextureImageUnit;
            context.shadowColorTextureImageUnit = shadowColorTextureImageUnit;
            context.shadowDepthTextureImageUnit = shadowDepthTextureImageUnit;
            context.colorImageUnit = colorImageUnit;
            context.shadowColorImageUnit = shadowColorImageUnit;
            context.bigBufferSize = bigBufferSize;
            context.bigBuffer = bigBuffer;
            context.faProjection = faProjection;
            context.faProjectionInverse = faProjectionInverse;
            context.faModelView = faModelView;
            context.faModelViewInverse = faModelViewInverse;
            context.faShadowProjection = faShadowProjection;
            context.faShadowProjectionInverse = faShadowProjectionInverse;
            context.faShadowModelView = faShadowModelView;
            context.faShadowModelViewInverse = faShadowModelViewInverse;
            context.projection = projection;
            context.projectionInverse = projectionInverse;
            context.modelView = modelView;
            context.modelViewInverse = modelViewInverse;
            context.shadowProjection = shadowProjection;
            context.shadowProjectionInverse = shadowProjectionInverse;
            context.shadowModelView = shadowModelView;
            context.shadowModelViewInverse = shadowModelViewInverse;
            context.previousProjection = previousProjection;
            context.previousModelView = previousModelView;
            context.tempMatrixDirectBuffer = tempMatrixDirectBuffer;
            context.tempDirectFloatBuffer = tempDirectFloatBuffer;
            context.dfbDrawBuffers = dfbDrawBuffers;
            context.sfbDrawBuffers = sfbDrawBuffers;
            context.drawBuffersNone = drawBuffersNone;
            context.drawBuffersColorAtt = drawBuffersColorAtt;
            context.glDebugGroups = glDebugGroups;
            context.glDebugGroupProgram = glDebugGroupProgram;
            context.mapBlockToEntityData = mapBlockToEntityData;
            context.formatNames = formatNames;
            context.formatIds = formatIds;
            context.patternLoadEntityDataMap = patternLoadEntityDataMap;
            context.entityData = entityData;
            context.entityDataIndex = entityDataIndex;
            
            
        };

        OFGlobal.getDfb = () -> dfb.getGlFramebuffer();
        
        OFGlobal.getShaderUniforms = () -> shaderUniforms;
        
        OFGlobal.getCurrentWorld = () -> currentWorld;
        
        OFGlobal.bindToShaderFrameBuffer = () -> {
            if (dfb == null) {
                GlStateManager.bindFramebuffer(36160, 0);
                return;
            }
            dfb.bindFramebuffer();
//            GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
        };
        
        OFGlobal.getCurrentShaderpack = () -> shaderPack;
    }
}
