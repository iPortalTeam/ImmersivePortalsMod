## v6.0.121 - 2025-12-30
- **Fixed**
  - ?? (FIX) Rilasciati i ticket chunk usando il full chunk future per evitare slot bloccati e chunk invisibili - Ref: IMPT000117 - Scope: src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTickets.java

## v6.0.120 - 2025-12-29
- **Fixed**
  - ?? (FIX) Inviati chunk pronti al client tramite ChunkMap.getChunkToSend per evitare chunk invisibili - Ref: IMPT000116 - Scope: src/main/java/qouteall/imm_ptl/core/chunk_loading/PlayerChunkLoading.java

## v6.0.119 - 2025-12-29
- **Fixed**
  - ?? (FIX) Forzato refreshEmptySections dopo replace nello storage client per evitare chunk invisibili - Ref: IMPT000115 - Scope: src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlClientChunkMap.java

## v6.0.118 - 2025-12-29
- **Fixed**
  - ?? (FIX) Sincronizzata storage ClientChunkCache con view center/radius e chunk map per evitare chunk invisibili - Ref: IMPT000114 - Scope: src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlClientChunkMap.java, src/main/java/qouteall/imm_ptl/core/mixin/client/accessor/IEClientChunkCacheStorage.java, src/main/resources/imm_ptl.mixins.json

## v6.0.117 - 2025-12-29
- **Fixed**
  - ?? (FIX) Registrato il TicketType ImmPtl nel registry per evitare crash in salvataggio - Ref: IMPT000113 - Scope: src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTickets.java

## v6.0.116 - 2025-12-29
- **Fixed**
  - ?? (FIX) Sincronizzati gli indici RenderSection per evitare ArrayIndexOutOfBoundsException nell'occlusion graph - Ref: IMPT000112 - Scope: src/main/java/qouteall/imm_ptl/core/render/ImmPtlViewArea.java

## v6.0.115 - 2025-12-29
- **Fixed**
  - ?? (FIX) Spostata la write del uniform clipping fuori dal render pass per evitare IllegalStateException - Ref: IMPT000111 - Scope: src/main/java/qouteall/imm_ptl/core/render/FrontClipping.java

## v6.0.114 - 2025-12-29
- **Fixed**
  - ?? (FIX) Letto ResourceKey dimensione nel decode del ClientboundPlayerPositionPacket per evitare packet extra - Ref: IMPT000110 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinPlayerPositionLookS2CPacket.java

## v6.0.113 - 2025-12-29
- **Fixed**
  - ?? (FIX) Retarget hook frustum a isVisible(AABB) per firma 1.21.11 - Ref: IMPT000109 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/optimization/MixinFrustum.java

## v6.0.112 - 2025-12-29
- **Fixed**
  - ?? (FIX) Retarget hook packet entity tracking ai metodi sendToTrackingPlayers* di TrackedEntity - Ref: IMPT000107 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/entity_sync/MixinTrackedEntity.java, src/main/java/qouteall/imm_ptl/core/mixin/common/entity_sync/MixinServerEntity.java
  - ?? (FIX) Rimossa la mixin BiomeAmbientSoundsHandler basata su biomeManager assente - Ref: IMPT000108 - Scope: src/main/resources/imm_ptl.mixins.json, src/main/java/qouteall/imm_ptl/core/mixin/client/multiworld_awareness/MixinBiomeAmbientSoundPlayer.java

## v6.0.111 - 2025-12-29
- **Fixed**
  - ?? (FIX) Retarget inject accept-teleport a ServerPlayer.absSnapTo in 1.21.11 - Ref: IMPT000106 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinServerGamePacketListenerImpl.java

## v6.0.110 - 2025-12-29
- **Fixed**
  - ?? (FIX) Accessor TagValueInput/Output per leggere/scrivere CompoundTag senza reflection - Ref: IMPT000104 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/storage/IETagValueInput.java, src/main/java/qouteall/imm_ptl/core/mixin/common/storage/IETagValueOutput.java, src/main/java/qouteall/imm_ptl/core/portal/Portal.java, src/main/resources/imm_ptl.mixins.json
  - ?? (FIX) Retarget inject collisione a isEntityCollidingWithAnythingNew in ServerGamePacketListenerImpl - Ref: IMPT000105 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinServerGamePacketListenerImpl.java

## v6.0.109 - 2025-12-29
- **Fixed**
  - ?? (FIX) Usata replaceEntries per evitare liste UI non modificabili nel dimension stack - Ref: IMPT000103 - Scope: src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimStackGuiController.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/SelectDimensionScreen.java

## v6.0.108 - 2025-12-29
- **Fixed**
  - ?? (FIX) Allineata signature overwrite onChunkReadyToSend a ChunkHolder+LevelChunk - Ref: IMPT000102 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/MixinChunkMap_C.java

## v6.0.107 - 2025-12-28
- **Fixed**
  - ?? (FIX) Retarget WrapOperation distanza a isWithinBlockInteractionRange in ServerPlayerGameMode - Ref: IMPT000101 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/interaction/MixinServerPlayerGameMode.java

## v6.0.106 - 2025-12-28
- **Fixed**
  - ?? (FIX) Aggiornato redirect ServerPlayer.level a ritorno ServerLevel in ServerPlayerGameMode - Ref: IMPT000100 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/interaction/MixinServerPlayerGameMode.java

## v6.0.105 - 2025-12-28
- **Fixed**
  - ?? (FIX) Allineata signature mixin CreateWorldScreen init per 1.21.11 - Ref: IMPT000099 - Scope: src/main/java/qouteall/imm_ptl/peripheral/mixin/client/dim_stack/MixinCreateWorldScreen_CVB.java

## v6.0.104 - 2025-12-28
- **Fixed**
  - ?? (FIX) Salvataggio config via GsonConfigSerializer per evitare fallimenti build e crash AutoConfig - Ref: IMPT000098 - Scope: src/main/java/qouteall/imm_ptl/core/platform_specific/IPConfig.java

## v6.0.103 - 2025-12-28
- **Fixed**
  - ?? (FIX) Salvato config via serializer diretto per evitare crash AutoConfig su save - Ref: IMPT000097 - Scope: src/main/java/qouteall/imm_ptl/core/platform_specific/IPConfig.java

## v6.0.102 - 2025-12-28
- **Fixed**
  - ?? (FIX) Resa mutabile la lista splashes in SplashManager per evitare UnsupportedOperationException - Ref: IMPT000096 - Scope: src/main/java/qouteall/imm_ptl/peripheral/mixin/client/MixinSplashManager_CVB.java

## v6.0.101 - 2025-12-28
- **Fixed**
  - ?? (FIX) Agganciato debug text a renderLines nel DebugScreenOverlay 1.21.11 - Ref: IMPT000095 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinDebugScreenOverlay.java

## v6.0.100 - 2025-12-28
- **Fixed**
  - ?? (FIX) Migrazione hook wand debug da DebugRenderer.render a emitGizmos con buffer interno - Ref: IMPT000094 - Scope: src/main/java/qouteall/imm_ptl/peripheral/mixin/client/portal_wand/MixinDebugRenderer.java

## v6.0.99 - 2025-12-28
- **Fixed**
  - ?? (FIX) Rimosso redirect obsoleto su getTranslucentTarget non più invocato in renderLevel - Ref: IMPT000093 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.98 - 2025-12-28
- **Fixed**
  - ?? (FIX) Spostato hook glowing su EntityRenderState.appearsGlowing in extractVisibleEntities - Ref: IMPT000092 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.97 - 2025-12-28
- **Fixed**
  - ?? (FIX) Spostato redirect submit entity su submitEntities per agganciare la chiamata reale - Ref: IMPT000091 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.96 - 2025-12-28
- **Fixed**
  - ?? (FIX) Spostato redirect extractEntity su extractVisibleEntities per agganciare la chiamata reale - Ref: IMPT000090 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.95 - 2025-12-28
- **Fixed**
  - ?? (FIX) Aggiornato redirect clearing a LevelTargetBundle.clear in renderLevel - Ref: IMPT000089 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.94 - 2025-12-28
- **Fixed**
  - ?? (FIX) Disambiguato ModifyVariable in compileSections per ForceMainThreadRebuild (index=14) - Ref: IMPT000088 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer_ForceMainThreadRebuild.java

## v6.0.93 - 2025-12-28
- **Fixed**
  - ?? (FIX) Corretto index ModifyVariable per cullTerrain in 1.21.11 - Ref: IMPT000087 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.92 - 2025-12-28
- **Fixed**
  - ?? (FIX) Adeguati mixin non mappati per consentire il refmap con Loom 1.14 - Ref: IMPT000086 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderPipelineBuilder.java, src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/IEChunkMap_Accessor.java, src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java

## v6.0.91 - 2025-12-28
- **Changed**
  - ?? (FIX) Abilitato legacy mixin AP per generare il refmap con Loom 1.14 - Ref: IMPT000085 - Scope: build.gradle

## v6.0.90 - 2025-12-28
- **Changed**
  - ?? (FIX) Aggiunta refmap ai mixin json per caricare correttamente i target in runtime - Ref: IMPT000084 - Scope: src/main/resources/imm_ptl.mixins.json, src/main/resources/imm_ptl_compat.mixins.json, src/main/resources/imm_ptl_fabric.mixins.json, src/main/resources/imm_ptl_peripheral.mixins.json, src/main/resources/q_misc_util.mixins.json

## v6.0.89 - 2025-12-28
- **Changed**
  - ?? (FIX) Impostato refmap default Loom per risoluzione mixin in runtime - Ref: IMPT000083 - Scope: build.gradle

## v6.0.88 - 2025-12-28
- **Fixed**
  - ?? (FIX) Agganciato hook meteo a addWeatherPass (method_62203) per evitare crash mixin su 1.21.11 - Ref: IMPT000082 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.87 - 2025-12-28
- **Fixed**
  - ?? (FIX) Allineata signature Inject method_62214 (rimosso boolean extra) - Ref: IMPT000081 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.86 - 2025-12-28
- **Fixed**
  - ?? (FIX) Spostati hook layer rendering su method_62214 e wrap renderGroup per clipping corretto - Ref: IMPT000080 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.85 - 2025-12-27
- **Fixed**
  - ?? (FIX) Agganciati hook layer rendering a renderLevel intermediary per evitare mismatch senza refmap - Ref: IMPT000079 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.84 - 2025-12-27
- **Fixed**
  - ?? (FIX) Allineati target mixin renderGroup a nomi intermediary per aggancio senza refmap - Ref: IMPT000078 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.83 - 2025-12-27
- **Fixed**
  - ?? (FIX) Retarget hook render layer a ChunkSectionsToRender.renderGroup in LevelRenderer - Ref: IMPT000077 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.82 - 2025-12-27
- **Fixed**
  - ?? (FIX) Spostato hook fine rendering entità su submitBlockEntities - Ref: IMPT000076 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.81 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reso opzionale l'hook translucentCullBlockSheet in LevelRenderer - Ref: IMPT000075 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.80 - 2025-12-27
- **Fixed**
  - ?? (FIX) Declassato il log di fetch mod info su 404 per evitare errori non bloccanti - Ref: IMPT000074 - Scope: src/main/java/qouteall/imm_ptl/core/compat/IPModInfoChecking.java

## v6.0.79 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reso opzionale l'hook constantAmbientLight in LevelRenderer - Ref: IMPT000073 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.78 - 2025-12-27
- **Fixed**
  - ?? (FIX) Allineate le Inject di renderLevel alla signature 1.21.11 in LevelRenderer - Ref: IMPT000072 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.77 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornata signature renderLevel in MixinLevelRenderer_BeforeIris per 1.21.11 - Ref: IMPT000071 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer_BeforeIris.java

## v6.0.76 - 2025-12-27
- **Fixed**
  - ?? (FIX) Retarget redirect setCameraPosition a cullTerrain senza owner prefix - Ref: IMPT000070 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer_Optional.java

## v6.0.75 - 2025-12-27
- **Fixed**
  - ?? (FIX) Retarget hook chunk compiled a isSectionCompiledAndVisible - Ref: IMPT000069 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.74 - 2025-12-27
- **Fixed**
  - ?? (FIX) Retarget hook sky rendering a addSkyPass e rimosso redirect renderSky - Ref: IMPT000068 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.73 - 2025-12-27
- **Fixed**
  - ?? (FIX) Retarget inject setup terreno a cullTerrain e aggiornati parametri - Ref: IMPT000067 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.72 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato accesso frustum usando getCapturedFrustum/applyFrustum - Ref: IMPT000066 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.71 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato redirect translucentTarget a getTranslucentTarget in LevelRenderer - Ref: IMPT000065 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.70 - 2025-12-27
- **Fixed**
  - ?? (FIX) Gestita transparencyChain via override e invoker getTransparencyChain - Ref: IMPT000064 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.69 - 2025-12-27
- **Fixed**
  - ?? (FIX) Usato LevelRenderer.close al posto di deinitTransparency rimosso - Ref: IMPT000063 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.68 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reso abstract lo shadow extractEntity in LevelRenderer per compilazione - Ref: IMPT000062 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.67 - 2025-12-27
- **Fixed**
  - ?? (FIX) Spostato redirect FogRenderer su computeFogColor per Camera.position - Ref: IMPT000061 - Scope: src/main/java/qouteall/imm_ptl/peripheral/mixin/client/alternate_dimension/MixinFogRenderer_A_CVB.java

## v6.0.66 - 2025-12-27
- **Fixed**
  - ?? (FIX) Riallineato hook entita in LevelRenderer alla pipeline submit/extract 1.21.11 - Ref: IMPT000060 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java

## v6.0.65 - 2025-12-27
- **Fixed**
  - ?? (FIX) Allineata signature ScreenEffectRenderer.renderTex e rimossi owner nei selector Inject - Ref: IMPT000059 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinScreenEffectRenderer.java

## v6.0.64 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reso opzionale il redirect FogRenderer su Camera.position per evitare crash senza refmap - Ref: IMPT000058 - Scope: src/main/java/qouteall/imm_ptl/peripheral/mixin/client/alternate_dimension/MixinFogRenderer_A_CVB.java

## v6.0.63 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornata signature Camera.setup a Level e rimosso shadow getEntity - Ref: IMPT000057 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinCamera.java

## v6.0.62 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reso opzionale il redirect fog su Camera.position in FogRenderer - Ref: IMPT000056 - Scope: src/main/java/qouteall/imm_ptl/peripheral/mixin/client/alternate_dimension/MixinFogRenderer_A_CVB.java

## v6.0.61 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato WrapOperation renderLevel alla signature 1.21.11 - Ref: IMPT000055 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java

## v6.0.60 - 2025-12-27
- **Fixed**
  - ?? (FIX) Gestito toggle render hand direttamente in renderItemInHand con cancellazione - Ref: IMPT000054 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java

## v6.0.59 - 2025-12-27
- **Fixed**
  - ?? (FIX) Allineata signature inject renderItemInHand a float/boolean/Matrix4f - Ref: IMPT000053 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java

## v6.0.58 - 2025-12-27
- **Fixed**
  - ?? (FIX) Gestito panorama via override isPanoramicMode su GameRenderer - Ref: IMPT000052 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java

## v6.0.57 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook posizione client a handleMovePlayer per ClientPacketListener - Ref: IMPT000051 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinClientPacketListener.java

## v6.0.56 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reintrodotto toggle render hand via flag interno e ModifyArg - Ref: IMPT000050 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java

## v6.0.55 - 2025-12-27
- **Fixed**
  - ?? (FIX) Allineato reset proiezione a PerspectiveProjectionMatrixBuffer in GameRenderer - Ref: IMPT000049 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java

## v6.0.54 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimosso Inject su tickParticle in ParticleEngine non piu presente - Ref: IMPT000048 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/particle/MixinParticleEngine.java

## v6.0.53 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook ParticleEngine a extract per pipeline particelle 1.21.11 - Ref: IMPT000047 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/particle/MixinParticleEngine.java

## v6.0.52 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook handleMovePlayer a method_11157 con Inject su RETURN - Ref: IMPT000046 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinClientPacketListener.java

## v6.0.51 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato redirect buffer size per ChunkSectionLayer in SectionBufferBuilderPack - Ref: IMPT000045 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/optimization/MixinSectionBufferBuilderPack.java

## v6.0.50 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimossi owner nei selector Inject di MixinGlDebug - Ref: IMPT000044 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinGlDebug.java

## v6.0.49 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimossi owner nei selector Inject di MixinGlStateManager - Ref: IMPT000043 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGlStateManager.java

## v6.0.48 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornati i target PacketUtils a PacketProcessor in ClientPacketListener - Ref: IMPT000042 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinClientPacketListener.java

## v6.0.47 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimossi @Shadow applyLightData/enableChunkLight non presenti in ClientPacketListener - Ref: IMPT000041 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinClientPacketListener.java

## v6.0.46 - 2025-12-27
- **Fixed**
  - ?? (FIX) Impostati ResourceKey sugli item custom per evitare "Item id not set" - Ref: IMPT000040 - Scope: src/main/java/qouteall/imm_ptl/peripheral/PeripheralModMain.java, src/main/java/qouteall/imm_ptl/peripheral/CommandStickItem.java, src/main/java/qouteall/imm_ptl/peripheral/wand/PortalWandItem.java

## v6.0.45 - 2025-12-27
- **Fixed**
  - ?? (FIX) Impostato ResourceKey su blocchi custom per evitare "Block id not set" - Ref: IMPT000039 - Scope: src/main/java/qouteall/imm_ptl/core/portal/PortalPlaceholderBlock.java, src/main/java/qouteall/imm_ptl/peripheral/PeripheralModMain.java

## v6.0.44 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimossa Inject su costruttore ClientboundPlayerPositionPacket non presente - Ref: IMPT000038 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinClientboundPlayerPositionPacket.java

## v6.0.43 - 2025-12-27
- **Fixed**
  - ?? (FIX) Wrappato STREAM_CODEC per appendere dimensione a ClientboundPlayerPositionPacket - Ref: IMPT000037 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinPlayerPositionLookS2CPacket.java

## v6.0.42 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimossa class owner dal selector Inject su ClientboundPlayerPositionPacket.write - Ref: IMPT000036 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinPlayerPositionLookS2CPacket.java

## v6.0.41 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornata signature ServerboundMovePlayerPacket (aggiunto boolean extra) - Ref: IMPT000035 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinServerBoundMovePlayerPacket.java

## v6.0.37 - 2025-12-27
- **Fixed**
  - ?? (FIX) Corretto owner di discard in ThrownEnderPearl (package throwableitemprojectile) - Ref: IMPT000031 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinThrownEnderPearl.java

## v6.0.38 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook container openers a getEntitiesWithContainerOpen - Ref: IMPT000032 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/container_gui/MixinContainerOpenersCounter.java

## v6.0.39 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornata signature costruttore ClientLevel (rimosso Supplier, aggiunto int) - Ref: IMPT000033 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinClientLevel.java

## v6.0.40 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook send packet a ChannelFutureListener in ServerCommonPacketListenerImpl - Ref: IMPT000034 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/entity_sync/MixinServerGamePacketListenerImpl_Redirect.java

## v6.0.36 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato target discard in ThrownEnderpearl onHit - Ref: IMPT000030 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinThrownEnderPearl.java

## v6.0.35 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook minecart interpolation a lerpPositionAndRotationStep - Ref: IMPT000029 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinAbstractMinecartEntity.java

## v6.0.34 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornata firma teleportTo e rimossi owner prefix nelle @Inject - Ref: IMPT000028 - Scope: src/main/java/qouteall/imm_ptl/core/platform_specific/mixin/common/MixinServerPlayerEntity_MA.java

## v6.0.33 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook dimension change a ServerPlayer.teleport(TeleportTransition) - Ref: IMPT000027 - Scope: src/main/java/qouteall/imm_ptl/core/platform_specific/mixin/common/MixinServerPlayerEntity_MA.java

## v6.0.32 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato redirect owner projectile a EntityReference.getEntity - Ref: IMPT000026 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinProjectile.java

## v6.0.31 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reindirizzati i WrapOperation alla nuova isWithinBlockInteractionRange - Ref: IMPT000025 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/container_gui/MixinContainer.java, src/main/java/qouteall/imm_ptl/core/mixin/common/container_gui/MixinAbstractContainerMenu.java

## v6.0.30 - 2025-12-27
- **Fixed**
  - ?? (FIX) Resi opzionali i WrapOperation su canInteractWithBlock in container GUI - Ref: IMPT000024 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/container_gui/MixinContainer.java, src/main/java/qouteall/imm_ptl/core/mixin/common/container_gui/MixinAbstractContainerMenu.java

## v6.0.29 - 2025-12-27
- **Fixed**
  - ?? (FIX) Reso opzionale il redirect tick ServerLevel su List.isEmpty - Ref: IMPT000023 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/MixinServerLevel.java

## v6.0.28 - 2025-12-27
- **Fixed**
  - ?? (FIX) Resi opzionali gli hook fog RenderSystem mancanti in 1.21.11 - Ref: IMPT000022 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderSystem_Fog.java

## v6.0.27 - 2025-12-27
- **Fixed**
  - ?? (FIX) Usato invoker su Entity.setLevel per aggiornare il livello client - Ref: IMPT000021 - Scope: src/main/java/qouteall/imm_ptl/core/ducks/IEEntityLevelSetter.java, src/main/java/qouteall/imm_ptl/core/mixin/common/MixinEntityLevelSetter.java, src/main/java/qouteall/imm_ptl/core/mixin/client/MixinAbstractClientPlayer.java, src/main/resources/imm_ptl.mixins.json

## v6.0.26 - 2025-12-27
- **Fixed**
  - ?? (FIX) Resa astratta MixinAbstractClientPlayer per supportare @Shadow setLevel - Ref: IMPT000020 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinAbstractClientPlayer.java

## v6.0.25 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornati owner access ItemEntity e setter livello per AbstractClientPlayer - Ref: IMPT000019 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/portal_generation/MixinItemEntity_P.java, src/main/java/qouteall/imm_ptl/core/mixin/client/MixinAbstractClientPlayer.java

## v6.0.24 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato hook collisioni inside-block su Entity.makeBoundingBox - Ref: IMPT000018 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinEntity.java

## v6.0.23 - 2025-12-27
- **Fixed**
  - ?? (FIX) Aggiornato interpolation hook client per Entity usando InterpolationHandler - Ref: IMPT000017 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinLivingEntity_C.java

## v6.0.22 - 2025-12-27
- **Fixed**
  - ?? (FIX) Resi opzionali gli hook legacy in MixinMainTarget per 1.21.11 - Ref: IMPT000016 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/render/framebuffer/MixinMainTarget.java

## v6.0.21 - 2025-12-27
- **Fixed**
  - ?? (FIX) Allineato redirect collisioni Entity e resi opzionali gli hook framebuffer legacy - Ref: IMPT000015 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinEntity.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/framebuffer/MixinRenderTarget.java

## v6.0.20 - 2025-12-27
- **Fixed**
  - ?? (FIX) Corretta firma mixin addInitialScreens e import CallbackInfo per compatibilita 1.21.11 - Ref: IMPT000014 - Scope: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinMinecraft.java

## v6.0.19 - 2025-12-27
- **Fixed**
  - ?? (FIX) Rimosso mixin orfano per ChunkTaskPriorityQueueSorter dopo migrazione a TicketStorage - Ref: IMPT000013 - Scope: src/main/resources/imm_ptl.mixins.json

## v6.0.18 - 2025-12-26
- **Fixed**
  - ?? (FIX) Aggiunto junit-platform-launcher per eseguire test JUnit 5 - Ref: IMPT000012 - Scope: build.gradle

## v6.0.17 - 2025-12-26
- **Fixed**
  - ?? (FIX) Riattivato useJUnitPlatform per la discovery dei test JUnit 5 - Ref: IMPT000011 - Scope: build.gradle

## v6.0.16 - 2025-12-26
- **Fixed**
  - ?? (FIX) Allineati overlay GUI, layer rendering e helper direzioni alle API 1.21.11 - Ref: IMPT000010 - Scope: src/main/java/qouteall/imm_ptl/peripheral/CommandStickItem.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimListWidget.java, src/main/java/qouteall/imm_ptl/peripheral/platform_specific/PeripheralModEntryClient.java, src/main/java/qouteall/q_misc_util/CustomTextOverlay.java, src/main/java/qouteall/q_misc_util/mixin/dimension/MixinPlayerList_Misc.java, src/main/java/qouteall/q_misc_util/my_util/AARotation.java

## v6.0.15 - 2025-12-26
- **Fixed**
  - ?? (FIX) Isolata la compatibilita Iris escludendo il package dalla compilazione - Ref: IMPT000009 - Scope: build.gradle, build.gradle.bak

## v6.0.14 — 2025-12-21
- **Fixed**
  - 🟢 (FIX) Aggiornata lettura NBT opzionale per stati portale e wand in 1.21.11 — Ref: IMPT000008 — Scope: src/main/java/qouteall/imm_ptl/core/portal/PortalState.java, src/main/java/qouteall/imm_ptl/core/portal/PortalExtension.java, src/main/java/qouteall/imm_ptl/core/portal/animation/PortalAnimation.java, src/main/java/qouteall/imm_ptl/core/portal/animation/DefaultPortalAnimation.java, src/main/java/qouteall/imm_ptl/core/portal/animation/PortalAnimationDriver.java, src/main/java/qouteall/imm_ptl/core/portal/animation/UnilateralPortalState.java, src/main/java/qouteall/imm_ptl/core/portal/animation/DeltaUnilateralPortalState.java, src/main/java/qouteall/imm_ptl/peripheral/wand/PortalWandItem.java

## v6.0.13 — 2025-12-21
- **Changed**
  - 🟡 (FTR) Allineati accesso NBT, ResourceKey e cleanup mixin cloud/clipping per API 1.21.11 — Ref: IMPT000007 — Scope: src/main/java/qouteall/q_misc_util/my_util/Mesh2D.java, src/main/java/qouteall/q_misc_util/my_util/IntBox.java, src/main/java/qouteall/q_misc_util/dimension/DimIntIdMap.java, src/main/java/qouteall/q_misc_util/Helper.java, src/main/java/qouteall/q_misc_util/my_util/DQuaternion.java, src/main/java/qouteall/imm_ptl/core/ClientWorldLoader.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderSystem_Clipping.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/optimization/MixinLevelRenderer_Clouds.java, src/main/java/qouteall/imm_ptl/core/render/context_management/CloudContext.java, src/main/java/qouteall/imm_ptl/core/IPMcHelper.java, src/main/java/qouteall/imm_ptl/core/mixin/client/MixinGlDebug.java, src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinReceivingLevelScreen.java, src/main/resources/imm_ptl.accesswidener

## v6.0.12 — 2025-12-21
- **Fixed**
  - 🟢 (FIX) Adeguata gestione ticket chunk ImmPtl a TicketStorage/TicketType 1.21.11 — Ref: IMPT000006 — Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/MixinDistanceManager.java, src/main/java/qouteall/imm_ptl/core/ducks/IEDistanceManager.java, src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/IEDistanceManager.java, src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTickets.java, src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java, src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/IEChunkTaskPriorityQueueSorter.java

## v6.0.11 — 2025-12-21
- **Changed**
  - 🟡 (FTR) Aggiornati tipi di teletrasporto/proiettili, input GUI e gestione tag opzionali per 1.21.11 — Ref: IMPT000005 — Scope: src/main/java/qouteall/imm_ptl/core/portal/Portal.java, src/main/java/qouteall/q_misc_util/Helper.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimEntryWidget.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimListWidget.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimStackEntry.java, src/main/java/qouteall/imm_ptl/core/portal/EndPortalEntity.java, src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinAbstractArrow.java, src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinThrownEnderPearl.java, src/main/java/qouteall/imm_ptl/core/platform_specific/mixin/common/MixinServerPlayerEntity_MA.java, src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinServerGamePacketListenerImpl.java, src/main/java/qouteall/q_misc_util/MiscHelper.java, src/main/java/qouteall/q_misc_util/MiscNetworking.java

## v6.0.10 — 2025-12-21
- **Fixed**
  - 🟢 (FIX) Allineati i riferimenti a listener livelli, preset grafici e Sodium occlusion culling per API 1.21.11 — Ref: IMPT000004 — Scope: src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java, src/main/java/qouteall/imm_ptl/core/compat/mixin/sodium/MixinSodiumOcclusionCuller.java, src/main/java/qouteall/q_misc_util/mixin/MixinMinecraftServer_Misc.java, src/main/java/qouteall/imm_ptl/peripheral/mixin/common/dim_stack/MixinMinecraftServer_DimStack_CVB.java, src/main/java/qouteall/imm_ptl/peripheral/alternate_dimension/NormalSkylandGenerator.java

## v6.0.9 — 2025-12-20
- **Changed**
  - 🟡 (FTR) Porting a Minecraft 1.21.11 con aggiornamento metadata Fabric e vincoli compatibilità Iris/Sodium — Ref: IMPT000001 — Scope: gradle.properties, src/main/resources/fabric.mod.json
  - 🟡 (FTR) Allineate le versioni Iris/Sodium verificate su Modrinth per 1.21.11 — Ref: IMPT000002 — Scope: gradle.properties, src/main/resources/fabric.mod.json
  - 🟢 (FIX) Aggiornati Fabric Loom e Gradle wrapper per risolvere l’errore sui Javadoc con namespace intermediary — Ref: IMPT000003 — Scope: build.gradle, gradle.properties, gradle/wrapper/gradle-wrapper.properties

