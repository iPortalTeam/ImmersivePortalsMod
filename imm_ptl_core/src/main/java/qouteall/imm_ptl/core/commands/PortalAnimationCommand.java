package qouteall.imm_ptl.core.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.*;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Vec2d;

import java.util.Collection;
import java.util.List;

public class PortalAnimationCommand {
    static void registerPortalAnimationCommands(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("clear")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forClusterPortals(
                    portal, portal1 -> portal1.clearAnimationDrivers(true, true)
                );
                
                PortalCommand.reloadPortal(portal);
            }))
        );
        
        builder.then(Commands.literal("pause")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forClusterPortals(
                    portal, Portal::pauseAnimation
                );
                PortalCommand.reloadPortal(portal);
            }))
        );
        
        builder.then(Commands.literal("resume")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forClusterPortals(
                    portal, Portal::resumeAnimation
                );
                PortalCommand.reloadPortal(portal);
            }))
        );
        
        builder.then(Commands.literal("rotate_infinitely")
            .then(Commands.argument("rotationCenterEntity", EntityArgument.entity())
                .then(Commands.argument("rotationAxis", Vec3Argument.vec3(false))
                    .then(Commands.argument("degreesPerTick", DoubleArgumentType.doubleArg())
                        .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                            Entity rotationCenterEntity = EntityArgument.getEntity(context, "rotationCenterEntity");
                            Vec3 rotationCenter = rotationCenterEntity.position();
                            Vec3 axis = Vec3Argument.getVec3(context, "rotationAxis").normalize();
                            double angularVelocity = DoubleArgumentType.getDouble(context, "degreesPerTick");
                            
                            portal.addThisSideAnimationDriver(
                                new RotationAnimation.Builder()
                                    .setInitialPosition(portal.getOriginPos())
                                    .setInitialOrientation(portal.getOrientationRotation())
                                    .setRotationCenter(rotationCenter)
                                    .setRotationAxis(axis)
                                    .setDegreesPerTick(angularVelocity)
                                    .setStartGameTime(portal.level.getGameTime())
                                    .setEndGameTime(Long.MAX_VALUE)
                                    .build()
                            );
                            
                            PortalCommand.reloadPortal(portal);
                        }))
                    )
                )
            )
        );
        
        builder.then(Commands.literal("rotate_portals_infinitely")
            .then(Commands.argument("portals", EntityArgument.entities())
                .then(Commands.argument("rotationCenter", Vec3Argument.vec3())
                    .then(Commands.argument("axis", Vec3Argument.vec3(false))
                        .then(Commands.argument("degreesPerTick", DoubleArgumentType.doubleArg())
                            .executes(context -> {
                                Collection<? extends Entity> portals = EntityArgument.getEntities(context, "portals");
                                Vec3 rotationCenter = Vec3Argument.getVec3(context, "rotationCenter");
                                Vec3 axis = Vec3Argument.getVec3(context, "axis").normalize();
                                double angularVelocity = DoubleArgumentType.getDouble(context, "degreesPerTick");
                                
                                for (Entity entity : portals) {
                                    if (entity instanceof Portal portal) {
                                        portal.addThisSideAnimationDriver(
                                            new RotationAnimation.Builder()
                                                .setInitialPosition(portal.getOriginPos())
                                                .setInitialOrientation(portal.getOrientationRotation())
                                                .setRotationCenter(rotationCenter)
                                                .setRotationAxis(axis)
                                                .setDegreesPerTick(angularVelocity)
                                                .setStartGameTime(portal.level.getGameTime())
                                                .setEndGameTime(Long.MAX_VALUE)
                                                .build()
                                        );
                                        
                                        PortalCommand.reloadPortal(portal);
                                    }
                                    else {
                                        context.getSource().sendFailure(Component.literal("the entity is not a portal"));
                                    }
                                }
                                
                                for (Entity entity : portals) {
                                    if (entity instanceof Portal portal) {
                                        PortalCommand.reloadPortal(portal);
                                    }
                                }
                                
                                return 0;
                            })
                        )
                    )
                )
            )
        );
        
        builder.then(Commands.literal("rotate_portals")
            .then(Commands.argument("portals", EntityArgument.entities())
                .then(Commands.argument("rotationCenter", Vec3Argument.vec3())
                    .then(Commands.argument("axis", Vec3Argument.vec3(false))
                        .then(Commands.argument("degrees", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("duration", IntegerArgumentType.integer(1, 10000))
                                .executes(context -> {
                                    Collection<? extends Entity> portals = EntityArgument.getEntities(context, "portals");
                                    Vec3 rotationCenter = Vec3Argument.getVec3(context, "rotationCenter");
                                    Vec3 axis = Vec3Argument.getVec3(context, "axis").normalize();
                                    double degrees = DoubleArgumentType.getDouble(context, "degrees");
                                    int duration = IntegerArgumentType.getInteger(context, "duration");
                                    
                                    for (Entity entity : portals) {
                                        if (entity instanceof Portal portal) {
                                            PortalState endingState = portal.getAnimationEndingState();
                                            
                                            long currTime = portal.level.getGameTime();
                                            portal.addThisSideAnimationDriver(
                                                new RotationAnimation.Builder()
                                                    .setInitialPosition(endingState.fromPos)
                                                    .setInitialOrientation(endingState.orientation)
                                                    .setRotationCenter(rotationCenter)
                                                    .setRotationAxis(axis)
                                                    .setDegreesPerTick(degrees / duration)
                                                    .setStartGameTime(currTime)
                                                    .setEndGameTime(currTime + duration)
                                                    .setTimingFunction(TimingFunction.sine)
                                                    .build()
                                            );
                                            
                                            PortalCommand.reloadPortal(portal);
                                        }
                                        else {
                                            context.getSource().sendFailure(Component.literal("the entity is not a portal"));
                                        }
                                    }
                                    
                                    for (Entity entity : portals) {
                                        if (entity instanceof Portal portal) {
                                            PortalCommand.reloadPortal(portal);
                                        }
                                    }
                                    
                                    return 0;
                                })
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(Commands.literal("rotate_along_normal")
            .then(Commands.argument("degreesPerTick", DoubleArgumentType.doubleArg())
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    double angularVelocity = DoubleArgumentType.getDouble(context, "degreesPerTick");
                    
                    portal.addThisSideAnimationDriver(
                        new RotationAnimation.Builder()
                            .setInitialPosition(portal.getOriginPos())
                            .setInitialOrientation(portal.getOrientationRotation())
                            .setRotationCenter(portal.getOriginPos())
                            .setRotationAxis(portal.getNormal())
                            .setDegreesPerTick(angularVelocity)
                            .setStartGameTime(portal.level.getGameTime())
                            .setEndGameTime(Long.MAX_VALUE)
                            .build()
                    );
                    
                    PortalCommand.reloadPortal(portal);
                }))
            )
        );
        
        builder.then(Commands.literal("expand_from_center")
            .then(Commands.argument("durationTicks", IntegerArgumentType.integer(0, 1000))
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    int durationTicks = IntegerArgumentType.getInteger(context, "durationTicks");
                    
                    double animationScale = 10;
                    
                    PortalState endingState = portal.getAnimationEndingState();
                    
                    portal.addThisSideAnimationDriver(NormalAnimation.createSizeAnimation(
                        portal,
                        new Vec2d(1.0 / animationScale, 1.0 / animationScale),
                        new Vec2d(1, 1),
                        portal.level.getGameTime(),
                        durationTicks,
                        TimingFunction.sine
                    ));
                    PortalCommand.reloadPortal(portal);
                }))
            )
        );
        
        LiteralArgumentBuilder<CommandSourceStack> builderBuilder =
            Commands.literal("builder");
        
        builderBuilder.then(Commands.literal("begin")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalAnimation animation = portal.animation;
                animation.setPaused(portal, true);
                
                NormalAnimation.Phase dummyPhase = new NormalAnimation.Phase.Builder()
                    .durationTicks(0)
                    .build();
                NormalAnimation newNormalAnimation = new NormalAnimation.Builder()
                    .phases(List.of(dummyPhase))
                    .loopCount(1)
                    .startingGameTime(animation.getEffectiveTime(portal.level.getGameTime()))
                    .build();
                portal.animation.thisSideReferenceState = UnilateralPortalState.extractThisSide(portal.getPortalState());
                portal.animation.thisSideAnimations.add(newNormalAnimation);
                
                PortalCommand.reloadPortal(portal);
                context.getSource().sendSuccess(getAnimationNbtInfo(portal), false);
            }))
        );
        
        builderBuilder.then(Commands.literal("append_phase")
            .then(Commands.argument("durationTicks", IntegerArgumentType.integer(0, 1000))
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    int durationTicks = IntegerArgumentType.getInteger(context, "durationTicks");
                    
                    PortalAnimation animation = portal.animation;
                    
                    if (animation.thisSideAnimations.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("no animation is being built"));
                        return;
                    }
                    
                    int index = animation.thisSideAnimations.size() - 1;
                    PortalAnimationDriver lastAnimation = animation.thisSideAnimations.get(index);
                    if (!(lastAnimation instanceof NormalAnimation normalAnimation)) {
                        context.getSource().sendFailure(Component.literal("the last animation is not a normal animation"));
                        return;
                    }
                    
                    NormalAnimation.Phase newPhase = new NormalAnimation.Phase.Builder()
                        .durationTicks(durationTicks)
                        .timingFunction(TimingFunction.sine)
                        .delta(new DeltaUnilateralPortalState.Builder()
                            .offset(portal.getOriginPos())
                            .rotate(portal.getOrientationRotation())
                            .scaleSize(new Vec2d(portal.width, portal.height))
                            .build()
                        )
                        .build();
                    ImmutableList<NormalAnimation.Phase> newPhases = ImmutableList.<NormalAnimation.Phase>builder()
                        .addAll(normalAnimation.phases)
                        .add(newPhase)
                        .build();
                    
                    animation.thisSideAnimations.set(
                        index,
                        new NormalAnimation.Builder()
                            .phases(newPhases)
                            .loopCount(normalAnimation.loopCount)
                            .startingGameTime(normalAnimation.startingGameTime)
                            .build()
                    );
                    
                    PortalCommand.reloadPortal(portal);
                    context.getSource().sendSuccess(getAnimationNbtInfo(portal), false);
                }))
            )
        );
        
        builderBuilder.then(Commands.literal("create_loop")
            .then(Commands.argument("loopCount", IntegerArgumentType.integer(0, 100000))
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    // generated by GitHub copilot
                    int loopCount = IntegerArgumentType.getInteger(context, "loopCount");
                    
                    PortalAnimation animation = portal.animation;
                    
                    if (animation.thisSideAnimations.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("no animation is being built"));
                        return;
                    }
                    
                    int index = animation.thisSideAnimations.size() - 1;
                    PortalAnimationDriver lastAnimation = animation.thisSideAnimations.get(index);
                    if (!(lastAnimation instanceof NormalAnimation normalAnimation)) {
                        context.getSource().sendFailure(Component.literal("the last animation is not a normal animation"));
                        return;
                    }
                    
                    List<NormalAnimation.Phase> phases = normalAnimation.phases;
                    NormalAnimation.Phase firstPhase = phases.get(0);
                    NormalAnimation.Phase lastPhase = phases.get(phases.size() - 1);
                    
                    if (!isClose(firstPhase, lastPhase)) {
                        // insert a new phase to make it go to initial state
                        NormalAnimation.Phase newPhase = new NormalAnimation.Phase.Builder()
                            .durationTicks(5)
                            .offset(firstPhase.offset)
                            .rotation(firstPhase.rotation)
                            .sizeScaling(firstPhase.sizeScaling)
                            .timingFunction(TimingFunction.sine)
                            .build();
                        phases = ImmutableList.<NormalAnimation.Phase>builder()
                            .addAll(phases)
                            .add(newPhase)
                            .build();
                    }
                    
                    animation.thisSideAnimations.set(
                        index,
                        new NormalAnimation.Builder()
                            .phases(phases)
                            .loopCount(loopCount)
                            .startingGameTime(normalAnimation.startingGameTime)
                            .build()
                    );
                    
                    animation.setPaused(portal, false);
                    
                    PortalCommand.reloadPortal(portal);
                    context.getSource().sendSuccess(getAnimationNbtInfo(portal), false);
                }))
            )
        );
        
        builder.then(builderBuilder);
        
    }
    
    private static boolean isClose(NormalAnimation.Phase a, NormalAnimation.Phase b) {
        if (a.offset != null && b.offset != null) {
            if (a.offset.distanceTo(b.offset) > 0.01) {
                return false;
            }
        }
        
        if (a.rotation != null && b.rotation != null) {
            if (!DQuaternion.isClose(a.rotation, b.rotation, 0.001)) {
                return false;
            }
        }
        
        if (a.sizeScaling != null && b.sizeScaling != null) {
            if (Math.abs(a.sizeScaling.x() - b.sizeScaling.x()) > 0.01 || Math.abs(a.sizeScaling.y() - b.sizeScaling.y()) > 0.01) {
                return false;
            }
        }
        
        return true;
    }
    
    private static Component getAnimationNbtInfo(Portal portal) {
        CompoundTag tag = new CompoundTag();
        portal.animation.writeToTag(tag);
        return McHelper.compoundTagToTextSorted(
            tag,
            " ",
            0
        );
    }
}
