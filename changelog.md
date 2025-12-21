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
