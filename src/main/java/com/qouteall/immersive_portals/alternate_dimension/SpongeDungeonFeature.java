package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.datafixers.Dynamic;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.MobSpawnerEntry;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpongeDungeonFeature extends Feature<DefaultFeatureConfig> {
    //no need to register it
    public static final Feature<DefaultFeatureConfig> instance =
        new SpongeDungeonFeature(DefaultFeatureConfig::deserialize);
    
    private static RandomSelector<BlockState> spawnerShieldSelector =
        new RandomSelector.Builder<BlockState>()
            .add(20, Blocks.OBSIDIAN.getDefaultState())
            .add(10, Blocks.SPONGE.getDefaultState())
            .add(5, Blocks.CLAY.getDefaultState())
            .build();
    
    private static RandomSelector<Function<Random, Integer>> heightSelector =
        new RandomSelector.Builder<Function<Random, Integer>>()
            .add(20, random -> (int) (random.nextDouble() * 50 + 3))
            .add(10, random -> random.nextInt(150) + 3)
            .build();
    
    private static RandomSelector<BiFunction<World, Random, Entity>> entitySelector =
        new RandomSelector.Builder<BiFunction<World, Random, Entity>>()
            .add(10, SpongeDungeonFeature::randomMonster)
            .add(60, SpongeDungeonFeature::randomRidingMonster)
            .build();
    
    private static RandomSelector<EntityType<?>> monsterTypeSelector =
        new RandomSelector.Builder<EntityType<?>>()
            .add(10, EntityType.ZOMBIFIED_PIGLIN)
            .add(10, EntityType.HUSK)
            .add(30, EntityType.SKELETON)
            .add(10, EntityType.WITHER_SKELETON)
            .add(20, EntityType.SHULKER)
            .add(10, EntityType.SILVERFISH)
            .add(30, EntityType.SLIME)
            .add(20, EntityType.GHAST)
            .add(10, EntityType.SPIDER)
            .add(10, EntityType.CAVE_SPIDER)
            .add(10, EntityType.GIANT)
            .add(10, EntityType.MAGMA_CUBE)
//            .add(10, EntityType.GUARDIAN)
            .add(10, EntityType.ENDERMAN)
            .add(10, EntityType.SNOW_GOLEM)
            .add(10, EntityType.ARMOR_STAND)
            .add(10, EntityType.PILLAGER)
            .add(10, EntityType.PUFFERFISH)
            .add(10, EntityType.RAVAGER)
            .add(1, EntityType.WITHER)
            .build();
    
    private static RandomSelector<EntityType<?>> vehicleTypeSelector =
        new RandomSelector.Builder<EntityType<?>>()
            .add(20, EntityType.BAT)
            .add(20, EntityType.PHANTOM)
            .add(20, EntityType.GHAST)
            .add(10, EntityType.SLIME)
            .add(20, EntityType.SPIDER)
            .add(20, EntityType.PARROT)
            .add(10, EntityType.SHULKER)
            .build();
    
    private static RandomSelector<RandomSelector<EntityType<?>>> typeSelectorSelector =
        new RandomSelector.Builder<RandomSelector<EntityType<?>>>()
            .add(40, monsterTypeSelector)
            .add(10, vehicleTypeSelector)
            .build();
    
    public SpongeDungeonFeature(Function<Dynamic<?>, ? extends DefaultFeatureConfig> configDeserializer) {
        super(configDeserializer);
    }
    
    @Override
    public boolean generate(
        ServerWorldAccess serverWorldAccess,
        StructureAccessor accessor,
        ChunkGenerator generator,
        Random random,
        BlockPos pos,
        DefaultFeatureConfig config
    ) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        random.setSeed(chunkPos.toLong() + random.nextInt(2333));
        
        if (random.nextDouble() < 0.03) {
            generateOnce(serverWorldAccess, random, chunkPos);
        }
        
        return true;
        
    }
    
    private static final BlockPos[] shieldPoses = new BlockPos[]{
        new BlockPos(0, -2, 0),
        new BlockPos(1, -1, 0),
        new BlockPos(-1, -1, 0),
        new BlockPos(0, -1, 1),
        new BlockPos(0, -1, -1),
        new BlockPos(1, 0, 0),
        new BlockPos(-1, 0, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(0, 0, -1),
        new BlockPos(0, 1, 0),
    };
    
    private static List<Block> shulkerBoxes = Registry.BLOCK.stream()
        .filter(block -> block.getDefaultState().getMaterial() == Material.SHULKER_BOX)
        .collect(Collectors.toList());
    
    public void generateOnce(WorldAccess world, Random random, ChunkPos chunkPos) {
        int height = heightSelector.select(random).apply(random);
        BlockPos spawnerPos = chunkPos.toBlockPos(
            random.nextInt(16),
            height,
            random.nextInt(16)
        );
        
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                world.setBlockState(
                    spawnerPos.add(dx, -2, dz),
                    Blocks.SPONGE.getDefaultState(),
                    2
                );
            }
        }
        
        BlockState spawnerShieldBlock = spawnerShieldSelector.select(random);
        for (BlockPos shieldPos : shieldPoses) {
            world.setBlockState(
                spawnerPos.add(shieldPos),
                spawnerShieldBlock,
                2
            );
        }
        
        world.setBlockState(spawnerPos, Blocks.SPAWNER.getDefaultState(), 2);
        initSpawnerBlockEntity(world, random, spawnerPos);
        
        BlockPos shulkerBoxPos = spawnerPos.down();
        initShulkerBoxTreasure(world, random, shulkerBoxPos);
    }
    
    public void initShulkerBoxTreasure(
        WorldAccess world,
        Random random,
        BlockPos shulkerBoxPos
    ) {
        Block randomShulkerBox = shulkerBoxes.get(random.nextInt(shulkerBoxes.size()));
        world.setBlockState(shulkerBoxPos, randomShulkerBox.getDefaultState(), 2);
        
        BlockEntity blockEntity = world.getBlockEntity(shulkerBoxPos);
        if (!(blockEntity instanceof ShulkerBoxBlockEntity)) {
            //Helper.err("No Spawner Block Entity???");
            return;
        }
        
        treasureSelector.select(random).accept(random, ((ShulkerBoxBlockEntity) blockEntity));
    }
    
    public void initSpawnerBlockEntity(WorldAccess world, Random random, BlockPos spawnerPos) {
        BlockEntity blockEntity = world.getBlockEntity(spawnerPos);
        if (!(blockEntity instanceof MobSpawnerBlockEntity)) {
            //Helper.err("No Spawner Block Entity???");
            return;
        }
        
        MobSpawnerBlockEntity mobSpawner = (MobSpawnerBlockEntity) blockEntity;
        Entity spawnedEntity = entitySelector.select(random).apply(world.getWorld(), random);
        Validate.isTrue(!spawnedEntity.hasVehicle());
        CompoundTag tag = new CompoundTag();
        spawnedEntity.saveToTag(tag);
        
        removeUnnecessaryTag(tag);
        
        mobSpawner.getLogic().setSpawnEntry(
            new MobSpawnerEntry(100, tag)
        );
        mobSpawner.getLogic().setEntityId(spawnedEntity.getType());
        
        CompoundTag logicTag = mobSpawner.getLogic().toTag(new CompoundTag());
        logicTag.putShort("RequiredPlayerRange", (short) 32);
        //logicTag.putShort("MinSpawnDelay",(short) 10);
        //logicTag.putShort("MaxSpawnDelay",(short) 100);
        //logicTag.putShort("MaxNearbyEntities",(short) 200);
        mobSpawner.getLogic().fromTag(logicTag);
    }
    
    private static void removeUnnecessaryTag(Tag tag) {
        if (tag instanceof CompoundTag) {
            ((CompoundTag) tag).remove("Pos");
            ((CompoundTag) tag).remove("UUID");
            ((CompoundTag) tag).getKeys().forEach(key -> {
                Tag currTag = ((CompoundTag) tag).get(key);
                removeUnnecessaryTag((currTag));
                
            });
        }
        if (tag instanceof ListTag) {
            ((ListTag) tag).stream().forEach(SpongeDungeonFeature::removeUnnecessaryTag);
        }
    }
    
    private static Entity randomMonster(World world, Random random) {
        EntityType<?> entityType = monsterTypeSelector.select(random);
        return initRandomEntity(world, random, entityType);
    }
    
    private static Entity initRandomEntity(World world, Random random, EntityType<?> entityType) {
        Entity entity = entityType.create(world);
        if (entity instanceof SlimeEntity) {
            CompoundTag tag = entity.toTag(new CompoundTag());
            tag.putInt("Size", random.nextInt(20));
            entity.fromTag(tag);
        }
        if (entity instanceof LivingEntity) {
            entityPostprocessorSelector.select(random).accept(random, ((LivingEntity) entity));
        }
        if (entity instanceof SkeletonEntity) {
            ItemStack stack = new ItemStack(() -> Items.BOW);
            entity.equipStack(EquipmentSlot.MAINHAND, stack);
            entity.equipStack(EquipmentSlot.OFFHAND, stack.copy());
            ItemStack pumpkin = new ItemStack(() -> Items.PUMPKIN);
            entity.equipStack(EquipmentSlot.HEAD, pumpkin);
        }
        return entity;
    }
    
    private static Entity randomRidingMonster(World world, Random random) {
        
        int layer = random.nextInt(9) + 1;
        
        Entity vehicle = vehicleTypeSelector.select(random).create(world);
        
        Helper.reduce(
            vehicle,
            IntStream.range(0, layer)
                .mapToObj(i -> initRandomEntity(
                    world, random,
                    typeSelectorSelector.select(random).select(random)
                    )
                ),
            (down, up) -> {
                up.startRiding(down, true);
                return up;
            }
        );
        
        return vehicle;
    }
    
    private static RandomSelector<BiConsumer<Random, LivingEntity>> entityPostprocessorSelector =
        new RandomSelector.Builder<BiConsumer<Random, LivingEntity>>()
            .add(10, (random, entity) -> {
                //nothing
            })
            .add(15, (random, entity) ->
                entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, 120, 2
                ))
            )
            .add(20, (random, entity) ->
                entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.JUMP_BOOST, 120, 3
                ))
            )
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_CHESTPLATE);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.equipStack(EquipmentSlot.CHEST, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_HELMET);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.equipStack(EquipmentSlot.HEAD, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_LEGGINGS);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.equipStack(EquipmentSlot.LEGS, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_BOOTS);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.equipStack(EquipmentSlot.FEET, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_PICKAXE);
                stack.addEnchantment(Enchantments.EFFICIENCY, 6);
                entity.equipStack(EquipmentSlot.MAINHAND, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_AXE);
                stack.addEnchantment(Enchantments.EFFICIENCY, 6);
                entity.equipStack(EquipmentSlot.MAINHAND, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_SWORD);
                stack.addEnchantment(Enchantments.KNOCKBACK, 6);
                entity.equipStack(EquipmentSlot.MAINHAND, stack);
            })
            .build();
    
    private static RandomSelector<ItemStack> filledTreasureSelector =
        new RandomSelector.Builder<ItemStack>()
            .add(60, new ItemStack(() -> Items.DIRT, 64))
            .add(60, new ItemStack(() -> Items.SAND, 64))
            .add(60, new ItemStack(() -> Items.TERRACOTTA, 64))
            .add(60, new ItemStack(() -> Items.GRAVEL, 64))
            .add(40, new ItemStack(() -> Items.PACKED_ICE, 64))
            .add(60, new ItemStack(() -> Items.GLASS, 64))
            .add(60, new ItemStack(() -> Items.COBBLESTONE, 64))
            .add(10, new ItemStack(() -> Items.FEATHER, 64))
            .add(10, new ItemStack(() -> Items.INK_SAC, 64))
            .add(10, new ItemStack(() -> Items.LADDER, 64))
            .add(10, new ItemStack(() -> Items.POISONOUS_POTATO, 64))
            .add(10, new ItemStack(() -> Items.BIRCH_BUTTON, 64))
            .add(10, new ItemStack(() -> Items.LOOM, 64))
            .add(10, new ItemStack(() -> Items.DAYLIGHT_DETECTOR, 64))
            .add(10, new ItemStack(() -> Items.ACTIVATOR_RAIL, 64))
            .add(10, new ItemStack(() -> Items.SEA_PICKLE, 64))
            .add(10, new ItemStack(() -> Items.GRASS_PATH, 64))
            .add(10, new ItemStack(() -> Items.FLOWER_POT, 64))
            .add(10, new ItemStack(() -> Items.FLETCHING_TABLE, 64))
            .add(10, new ItemStack(() -> Items.BRICK_STAIRS, 64))
            .add(10, new ItemStack(() -> Items.COBWEB, 64))
            .add(10, new ItemStack(() -> Items.GRASS, 64))
            .add(10, new ItemStack(() -> Items.BELL, 64))
            .add(10, new ItemStack(() -> Items.JUNGLE_FENCE_GATE, 64))
            .add(10, new ItemStack(() -> Items.LILY_PAD, 64))
            .add(10, new ItemStack(() -> Items.JUKEBOX, 64))
            .add(10, new ItemStack(() -> Items.LEAD, 64))
            .add(10, new ItemStack(() -> Items.DRIED_KELP, 64))
            .add(10, new ItemStack(() -> Items.SPIDER_EYE, 64))
            .add(10, new ItemStack(() -> Items.LECTERN, 64))
            .add(10, new ItemStack(() -> Items.FARMLAND, 64))
            .add(10, new ItemStack(() -> Items.END_STONE_BRICK_STAIRS, 64))
            .add(10, new ItemStack(() -> Items.STRIPPED_OAK_WOOD, 64))
            .add(10, new ItemStack(() -> Items.ENCHANTING_TABLE, 64))
            .add(10, new ItemStack(() -> Items.ENDER_CHEST, 64))
            .add(10, new ItemStack(() -> Items.PACKED_ICE, 64))
            .add(10, new ItemStack(() -> Items.FERN, 64))
            .add(10, new ItemStack(() -> Items.DEAD_BUSH, 64))
            .add(10, new ItemStack(() -> Items.SEAGRASS, 64))
            .add(10, new ItemStack(() -> Items.CACTUS, 64))
            .add(10, new ItemStack(() -> Items.VINE, 64))
            .add(10, new ItemStack(() -> Items.OBSERVER, 64))
            .add(10, new ItemStack(() -> Items.COMPARATOR, 64))
            .add(10, new ItemStack(() -> Items.REPEATER, 64))
            .add(10, new ItemStack(() -> Items.DIAMOND_HOE, 1))
            .add(10, new ItemStack(() -> Items.FLINT_AND_STEEL, 1))
            .add(10, new ItemStack(() -> Items.COMPASS, 1))
            .add(10, new ItemStack(() -> Items.ACACIA_BOAT, 1))
            .add(10, new ItemStack(() -> Items.CARROT_ON_A_STICK, 1))
            .add(10, new ItemStack(() -> Items.PUFFERFISH_BUCKET, 1))
            .add(10, new ItemStack(() -> Items.MILK_BUCKET, 1))
            .add(10, new ItemStack(() -> Items.LEATHER_HORSE_ARMOR, 1))
            .add(10, new ItemStack(() -> Items.LEATHER_BOOTS, 1))
            .add(10, new ItemStack(() -> Items.SLIME_BLOCK, 1))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.ENCHANTED_BOOK, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.BINDING_CURSE, 1)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.WOODEN_PICKAXE, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.UNBREAKING, 5)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.GOLDEN_AXE, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.EFFICIENCY, 10)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.GOLDEN_PICKAXE, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.EFFICIENCY, 10)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.POTION, 1),
                itemStack -> PotionUtil.setPotion(itemStack, Potions.MUNDANE)
            ))
            .add(80, Helper.makeIntoExpression(
                new ItemStack(() -> Items.POTION, 1),
                itemStack -> PotionUtil.setPotion(itemStack, HandReachTweak.longerReachPotion)
            ))
            .build();
    
    private static RandomSelector<Function<Random, ItemStack>> singleTreasureSelector =
        new RandomSelector.Builder<Function<Random, ItemStack>>()
            .add(2, random -> {
                ItemStack stack = new ItemStack(() -> Items.STICK, 1);
                stack.addEnchantment(Enchantments.KNOCKBACK, 7);
                return stack;
            })
            .add(10, random -> {
                ItemStack stack = new ItemStack(() -> Items.LINGERING_POTION, 1);
                ArrayList<StatusEffectInstance> effects = new ArrayList<>();
                effects.add(new StatusEffectInstance(
                    ((SimpleRegistry<StatusEffect>) Registry.STATUS_EFFECT).getRandom(random),
                    1200, 4
                ));
                PotionUtil.setCustomPotionEffects(stack, effects);
                return stack;
            })
            .add(10, random -> {
                ItemStack stack = new ItemStack(() -> Items.ENCHANTED_BOOK, 1);
                stack.addEnchantment(((SimpleRegistry<Enchantment>) Registry.ENCHANTMENT).getRandom(
                    random), 5);
                return stack;
            })
            .build();
    
    private static RandomSelector<BiConsumer<Random, Inventory>> treasureSelector =
        new RandomSelector.Builder<BiConsumer<Random, Inventory>>()
            .add(20, (random, shulker) -> {
                ItemStack toFill = filledTreasureSelector.select(random);
                IntStream.range(
                    0, shulker.size()
                ).forEach(i -> shulker.setStack(i, toFill.copy()));
            })
            .add(5, (random, shulker) -> {
                shulker.setStack(
                    random.nextInt(shulker.size()),
                    singleTreasureSelector.select(random).apply(random)
                );
            })
            .build();
    
}
