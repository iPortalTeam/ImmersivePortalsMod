package com.qouteall.immersive_portals.portal;

public class ClassicalPortalConverter {
//
//    public static interface PortalConverter {
//        void onPlayerTeleported(
//            ServerWorld oldWorld,
//            ServerPlayerEntity player,
//            BlockPos portalBlockPos
//        );
//    }
//
//    private static Map<Block, PortalConverter> converterMap = new HashMap<>();
//
//    public static void onPlayerChangeDimension(
//        ServerPlayerEntity player,
//        ServerWorld oldWorld,
//        Vec3d oldPos
//    ) {
//        BlockPos playerPos = new BlockPos(oldPos);
//        Iterator<BlockPos> iterator = BlockPos.stream(
//            playerPos.add(-2, -2, -2),
//            playerPos.add(2, 2, 2)
//        ).iterator();
//        while (iterator.hasNext()) {
//            BlockPos blockPos = iterator.next();
//            Block block = oldWorld.getBlockState(blockPos).getBlock();
//            PortalConverter portalConverter = converterMap.get(block);
//            if (portalConverter != null) {
//                portalConverter.onPlayerTeleported(
//                    oldWorld, player, blockPos
//                );
//                return;
//            }
//        }
//    }
//
//    public static void init() {
//
//    }
//
//    public static void registerClassicalPortalConverter(
//        Block portalBlock,
//        Function<ServerWorld, Predicate<BlockPos>> portalBody,
//        Function<ServerWorld, Predicate<BlockPos>> portalFrame,
//        EntityType<NetherPortalEntity> entityType
//    ) {
//        converterMap.put(
//            portalBlock,
//            (oldWorld, oldPlayer, portalBlockPos) -> {
//                Predicate<BlockPos> portalBodyPredicate = portalBody.apply(oldWorld);
//                Predicate<BlockPos> portalFramePredicate = portalFrame.apply(oldWorld);
//                UUID playerUuid = oldPlayer.getUuid();
//                Arrays.stream(Direction.Axis.values()).map(
//                    axis -> {
//                        return BlockPortalShape.findArea(
//                            portalBlockPos,
//                            axis,
//                            portalBodyPredicate,
//                            portalFramePredicate
//                        );
//                    }
//                ).filter(Objects::nonNull).findFirst().ifPresent(blockPortalShape -> {
//                    ModMain.serverTaskList.addTask(() -> {
//                        ServerPlayerEntity player =
//                            McHelper.getServer().getPlayerManager().getPlayer(playerUuid);
//                        if (player == null) {
//                            return true;
//                        }
//                        if (player.isInTeleportationState()) {
//                            return false;
//                        }
//                        if (player.notInAnyWorld) {
//                            return false;
//                        }
//
//                        onPlayerLandedOnAnotherDimension(
//                            player,
//                            oldWorld,
//                            blockPortalShape,
//                            portalBody,
//                            portalFrame,
//                            entityType
//                        );
//                        return true;
//                    });
//                });
//            }
//        );
//    }
//
//    private static void onPlayerLandedOnAnotherDimension(
//        ServerPlayerEntity player,
//        ServerWorld oldWorld,
//        BlockPortalShape oldPortalShape,
//        Function<ServerWorld, Predicate<BlockPos>> portalBody,
//        Function<ServerWorld, Predicate<BlockPos>> portalFrame,
//        EntityType<NetherPortalEntity> entityType
//    ) {
//        BlockPos playerPos = new BlockPos(player.getPos());
//        BlockPos.Mutable temp = new BlockPos.Mutable();
//        Predicate<BlockPos> portalBodyPredicate = portalBody.apply(((ServerWorld) player.world));
//        Predicate<BlockPos> portalFramePredicate = portalFrame.apply(((ServerWorld) player.world));
//        BlockPortalShape thisSideShape = BlockPos.stream(
//            playerPos.add(-10, -10, -10),
//            playerPos.add(10, 10, 10)
//        ).map(
//            blockPos -> oldPortalShape.matchShape(
//                portalBodyPredicate,
//                portalFramePredicate,
//                blockPos,
//                temp
//            )
//        ).filter(Objects::nonNull).findFirst().orElse(null);
//
//        if (thisSideShape == null) {
//            player.sendMessage(new TranslatableText("imm_ptl.auto_portal_generation_failed"), false);
//        }
//        else {
//            NetherPortalGeneration.generateBreakablePortalEntities(
//                new NetherPortalGeneration.Info(
//                    oldWorld.getRegistryKey(),
//                    player.world.getRegistryKey(),
//                    oldPortalShape,
//                    thisSideShape
//                ),
//                entityType
//            );
//            player.sendMessage(new TranslatableText("imm_ptl.auto_portal_generation_succeeded"), false);
//        }
//    }
//
    
}
