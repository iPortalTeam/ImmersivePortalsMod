package qouteall.imm_ptl.core.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.*;
import qouteall.q_misc_util.my_util.Access;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Vec2d;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class PortalAnimationCommand {
    static void registerPortalAnimationCommands(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("clear")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forEachClusterPortal(
                    portal,
                    thisPortal -> thisPortal.clearAnimationDrivers(true, false),
                    flippedPortal -> flippedPortal.clearAnimationDrivers(true, false),
                    reversePortal -> reversePortal.clearAnimationDrivers(false, true),
                    parallelPortal -> parallelPortal.clearAnimationDrivers(false, true)
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
        
        builder.then(Commands.literal("all_pause")
            .executes(context -> {
                ServerLevel level = context.getSource().getLevel();
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Portal portal) {
                        PortalExtension.forClusterPortals(
                            portal, Portal::pauseAnimation
                        );
                    }
                }
                return 0;
            })
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
                            
                            giveRotationAnimation(portal, rotationCenter, axis, angularVelocity);
                            
                            PortalCommand.reloadPortal(portal);
                        }))
                    )
                )
            )
        );
        
        builder.then(Commands.literal("rotate_infinitely_random")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                Vec3 rotationCenter = context.getSource().getPosition();
                Vec3 axis = new Vec3(
                    Math.random() - 0.5,
                    Math.random() - 0.5,
                    Math.random() - 0.5
                ).normalize();
                double angularVelocity = Math.random() * 3;
                
                giveRotationAnimation(portal, rotationCenter, axis, angularVelocity);
                
                PortalCommand.reloadPortal(portal);
            }))
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
                                        giveRotationAnimation(portal, rotationCenter, axis, angularVelocity);
                                        
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
                    
                    giveRotationAnimation(portal, portal.getOriginPos(), portal.getNormal(), angularVelocity);
                    
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
                        portal.animation.getEffectiveTime(portal.level.getGameTime()),
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
                
                PortalState portalState = portal.getPortalState();
                assert portalState != null;
                if (portal.animation.thisSideReferenceState == null) {
                    portal.animation.thisSideReferenceState =
                        UnilateralPortalState.extractThisSide(portalState);
                }
                else {
                    // set this side state back to reference state
                    UnilateralPortalState thisSideState = portal.animation.thisSideReferenceState;
                    UnilateralPortalState otherSideState = UnilateralPortalState.extractOtherSide(portalState);
                    PortalState newState = UnilateralPortalState.combine(thisSideState, otherSideState);
                    
                    portal.setPortalState(newState);
                }
                
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
                    
                    Access<NormalAnimation> animationToBuild = getAnimationToBuild(animation);
                    
                    if (animationToBuild == null) {
                        context.getSource().sendFailure(Component.literal("No animation to build"));
                        return;
                    }
                    
                    PortalState currentPortalState = portal.getPortalState();
                    assert currentPortalState != null;
                    
                    UnilateralPortalState referenceState = animation.thisSideReferenceState;
                    if (referenceState == null) {
                        context.getSource().sendFailure(Component.literal("No reference state"));
                        return;
                    }
                    
                    DeltaUnilateralPortalState delta =
                        UnilateralPortalState.extractThisSide(currentPortalState)
                            .subtract(referenceState);
                    
                    NormalAnimation.Phase newPhase = new NormalAnimation.Phase.Builder()
                        .durationTicks(durationTicks)
                        .timingFunction(TimingFunction.sine)
                        .delta(delta)
                        .build();
                    ImmutableList<NormalAnimation.Phase> newPhases = ImmutableList.<NormalAnimation.Phase>builder()
                        .addAll(animationToBuild.get().phases)
                        .add(newPhase)
                        .build();
                    
                    animationToBuild.set(new NormalAnimation.Builder()
                        .phases(newPhases)
                        .loopCount(animationToBuild.get().loopCount)
                        .startingGameTime(animationToBuild.get().startingGameTime)
                        .build()
                    );
                    
                    PortalCommand.reloadPortal(portal);
                    context.getSource().sendSuccess(getAnimationNbtInfo(portal), false);
                }))
            )
        );
        
        builderBuilder.then(Commands.literal("create_loop")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                finishBuildingNormalAnimation(context, portal, 100000);
            }))
            .then(Commands.argument("loopCount", IntegerArgumentType.integer(0, 100000))
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    int loopCount = IntegerArgumentType.getInteger(context, "loopCount");
                    
                    finishBuildingNormalAnimation(context, portal, loopCount);
                }))
            )
        );
        
        builder.then(builderBuilder);
        
    }
    
    private static void finishBuildingNormalAnimation(CommandContext<CommandSourceStack> context, Portal portal, int loopCount) {
        PortalAnimation animation = portal.animation;
        
        Access<NormalAnimation> animationToBuild = getAnimationToBuild(animation);
        
        if (animationToBuild == null) {
            context.getSource().sendFailure(Component.literal("No animation to build"));
            return;
        }
        
        List<NormalAnimation.Phase> phases = animationToBuild.get().phases;
        
        if (phases.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No phases to loop"));
            return;
        }
        
        NormalAnimation.Phase lastPhase = phases.get(phases.size() - 1);
        DeltaUnilateralPortalState lastPhaseDelta = lastPhase.delta().purgeFPError();
        
        if (!lastPhaseDelta.isIdentity()) {
            // the animation does not return to the beginning state
            // insert another stage to make it return to the beginning state
            // otherwise it will abruptly jump
            NormalAnimation.Phase newPhase = new NormalAnimation.Phase.Builder()
                .durationTicks(5)
                .timingFunction(TimingFunction.sine)
                .delta(DeltaUnilateralPortalState.identity)
                .build();
            phases = ImmutableList.<NormalAnimation.Phase>builder()
                .addAll(phases)
                .add(newPhase)
                .build();
        }
        
        animationToBuild.set(
            new NormalAnimation.Builder()
                .phases(phases)
                .loopCount(loopCount)
                .startingGameTime(animationToBuild.get().startingGameTime)
                .build()
        );
        
        animation.setPaused(portal, false);
        
        PortalCommand.reloadPortal(portal);
        context.getSource().sendSuccess(getAnimationNbtInfo(portal), false);
    }
    
    private static void giveRotationAnimation(Portal portal, Vec3 rotationCenter, Vec3 axis, double angularVelocity) {
        portal.addThisSideAnimationDriver(
            new RotationAnimation.Builder()
                .setInitialPosition(portal.getOriginPos())
                .setInitialOrientation(portal.getOrientationRotation())
                .setRotationCenter(rotationCenter)
                .setRotationAxis(axis)
                .setDegreesPerTick(angularVelocity)
                .setStartGameTime(portal.animation.getEffectiveTime(portal.level.getGameTime()))
                .setEndGameTime(Long.MAX_VALUE)
                .build()
        );
    }
    
    @Nullable
    private static Access<NormalAnimation> getAnimationToBuild(PortalAnimation animation) {
        if (animation.thisSideAnimations.isEmpty()) {
            return null;
        }
        
        int index = animation.thisSideAnimations.size() - 1;
        PortalAnimationDriver lastAnimation = animation.thisSideAnimations.get(index);
        if (!(lastAnimation instanceof NormalAnimation normalAnimation)) {
            return null;
        }
        
        return new Access<NormalAnimation>() {
            @Override
            public NormalAnimation get() {
                return normalAnimation;
            }
            
            @Override
            public void set(NormalAnimation normalAnimation) {
                animation.thisSideAnimations.set(index, normalAnimation);
            }
        };
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
