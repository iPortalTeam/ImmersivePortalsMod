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
import qouteall.q_misc_util.my_util.Vec2d;

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
                context.getSource().sendSuccess(getAnimationInfo(portal), false);
            }))
        );
        
        builder.then(Commands.literal("pause")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forClusterPortals(
                    portal, Portal::pauseAnimation
                );
                PortalCommand.reloadPortal(portal);
                context.getSource().sendSuccess(Component.literal("Paused"), false);
            }))
        );
        
        builder.then(Commands.literal("resume")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forClusterPortals(
                    portal, Portal::resumeAnimation
                );
                PortalCommand.reloadPortal(portal);
                context.getSource().sendSuccess(Component.literal("Resumed"), false);
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
                        context.getSource().sendSuccess(
                            Component.literal("Paused " + portal.toString()),
                            false
                        );
                    }
                }
                return 0;
            })
        );
        
        builder.then(Commands.literal("remove_at")
            .then(Commands.argument("index", IntegerArgumentType.integer())
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    int index = IntegerArgumentType.getInteger(context, "index");
                    AnimationView animationView = portal.getAnimationView();
                    List<PortalAnimationDriver> thisSideAnimations = animationView.getThisSideAnimations();
                    if (index >= 0 && index < thisSideAnimations.size()) {
                        thisSideAnimations.remove(index);
                        PortalCommand.reloadPortal(portal);
                        context.getSource().sendSuccess(getAnimationInfo(portal), false);
                    }
                    else {
                        context.getSource().sendFailure(
                            Component.literal("Invalid index " + index)
                        );
                    }
                }))
            )
        );
        
        builder.then(Commands.literal("remove_last")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                AnimationView animationView = portal.getAnimationView();
                List<PortalAnimationDriver> thisSideAnimations = animationView.getThisSideAnimations();
                if (!thisSideAnimations.isEmpty()) {
                    thisSideAnimations.remove(thisSideAnimations.size() - 1);
                    PortalCommand.reloadPortal(portal);
                    context.getSource().sendSuccess(getAnimationInfo(portal), false);
                }
                else {
                    context.getSource().sendFailure(
                        Component.literal("No animation")
                    );
                }
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
                            
                            giveRotationAnimation(portal, rotationCenter, axis, angularVelocity);
                            
                            PortalCommand.reloadPortal(portal);
                            context.getSource().sendSuccess(getAnimationInfo(portal), false);
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
                context.getSource().sendSuccess(getAnimationInfo(portal), false);
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
                                                    .setInitialOffset(endingState.fromPos.subtract(rotationCenter))
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
                    context.getSource().sendSuccess(getAnimationInfo(portal), false);
                }))
            )
        );
        
        builder.then(Commands.literal("expand_from_center")
            .then(Commands.argument("durationTicks", IntegerArgumentType.integer(0, 1000))
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    int durationTicks = IntegerArgumentType.getInteger(context, "durationTicks");
                    
                    double animationScale = 10;
                    
                    portal.addThisSideAnimationDriver(NormalAnimation.createSizeAnimation(
                        portal,
                        new Vec2d(1.0 / animationScale, 1.0 / animationScale),
                        new Vec2d(1, 1),
                        portal.getAnimationEffectiveTime(),
                        durationTicks,
                        TimingFunction.sine
                    ));
                    PortalCommand.reloadPortal(portal);
                    context.getSource().sendSuccess(getAnimationInfo(portal), false);
                }))
            )
        );
        
        LiteralArgumentBuilder<CommandSourceStack> buildBuilder =
            Commands.literal("build");
        
        buildBuilder.then(Commands.literal("begin")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                Portal animationHolder = portal.getAnimationHolder();
                if (animationHolder != null && animationHolder != portal) {
                    sendAnimationHolderFailure(context, animationHolder);
                    return;
                }
                
                PortalAnimation animation = portal.animation;
                animation.setPaused(portal, true);
                
                PortalState portalState = portal.getPortalState();
                assert portalState != null;
                if (animation.thisSideReferenceState != null && animation.otherSideReferenceState != null) {
                    // set this side state back to reference state
                    PortalState newState = UnilateralPortalState.combine(
                        animation.thisSideReferenceState, animation.otherSideReferenceState
                    );
                    portal.setPortalState(newState);
                }
                else {
                    // initialize reference state
                    animation.thisSideReferenceState =
                        UnilateralPortalState.extractThisSide(portalState);
                    animation.otherSideReferenceState =
                        UnilateralPortalState.extractOtherSide(portalState);
                }
                
                NormalAnimation.Phase dummyPhase = new NormalAnimation.Phase.Builder()
                    .durationTicks(0)
                    .build();
                NormalAnimation newNormalAnimation = new NormalAnimation.Builder()
                    .phases(List.of(dummyPhase))
                    .loopCount(1)
                    .startingGameTime(portal.getAnimationEffectiveTime())
                    .build();
                animation.thisSideAnimations.add(newNormalAnimation);
                animation.otherSideAnimations.add(newNormalAnimation); // reusing immutable object
                
                PortalCommand.reloadPortal(portal);
                context.getSource().sendSuccess(getAnimationInfo(portal), false);
            }))
        );
        
        buildBuilder.then(Commands.literal("append_phase")
            .then(Commands.argument("durationTicks", IntegerArgumentType.integer(0, 1000))
                .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                    Portal animationHolder = portal.getAnimationHolder();
                    if (animationHolder != null && animationHolder != portal) {
                        sendAnimationHolderFailure(context, animationHolder);
                        return;
                    }
                    
                    int durationTicks = IntegerArgumentType.getInteger(context, "durationTicks");
                    
                    PortalAnimation animation = portal.animation;
                    
                    AnimationBuilderContext animationBuilderContext = getAnimationBuilderContext(portal);
                    
                    if (animationBuilderContext == null) {
                        context.getSource().sendFailure(Component.literal("No animation to build"));
                        return;
                    }
                    
                    PortalState currentPortalState = portal.getPortalState();
                    assert currentPortalState != null;
                    
                    UnilateralPortalState thisSideReferenceState = animation.thisSideReferenceState;
                    if (thisSideReferenceState == null) {
                        context.getSource().sendFailure(Component.literal("No reference state"));
                        return;
                    }
                    DeltaUnilateralPortalState thisSideDelta =
                        UnilateralPortalState.extractThisSide(currentPortalState)
                            .subtract(thisSideReferenceState);
                    
                    UnilateralPortalState otherSideReferenceState = animation.otherSideReferenceState;
                    if (otherSideReferenceState == null) {
                        context.getSource().sendFailure(Component.literal("No reference state"));
                        return;
                    }
                    DeltaUnilateralPortalState otherSideDelta =
                        UnilateralPortalState.extractOtherSide(currentPortalState)
                            .subtract(otherSideReferenceState);
                    
                    NormalAnimation thisSideAnimation = animationBuilderContext.thisSideAnimation().get();
                    animationBuilderContext.thisSideAnimation().set(new NormalAnimation.Builder()
                        .phases(ImmutableList.<NormalAnimation.Phase>builder()
                            .addAll(thisSideAnimation.phases)
                            .add(new NormalAnimation.Phase.Builder()
                                .durationTicks(durationTicks)
                                .timingFunction(TimingFunction.sine)
                                .delta(thisSideDelta)
                                .build())
                            .build())
                        .loopCount(thisSideAnimation.loopCount)
                        .startingGameTime(thisSideAnimation.startingGameTime)
                        .build()
                    );
                    NormalAnimation otherSideAnimation = animationBuilderContext.otherSideAnimation().get();
                    animationBuilderContext.otherSideAnimation().set(new NormalAnimation.Builder()
                        .phases(ImmutableList.<NormalAnimation.Phase>builder()
                            .addAll(otherSideAnimation.phases)
                            .add(new NormalAnimation.Phase.Builder()
                                .durationTicks(durationTicks)
                                .timingFunction(TimingFunction.sine)
                                .delta(otherSideDelta)
                                .build())
                            .build())
                        .loopCount(otherSideAnimation.loopCount)
                        .startingGameTime(otherSideAnimation.startingGameTime)
                        .build()
                    );
                    
                    PortalCommand.reloadPortal(portal);
                    context.getSource().sendSuccess(getAnimationInfo(portal), false);
                }))
            )
        );
        
        buildBuilder.then(Commands.literal("finish")
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
        
        builder.then(buildBuilder);
    }
    
    private static AnimationBuilderContext getAnimationBuilderContext(
        Portal portal
    ) {
        PortalAnimation animation = portal.animation;
        if (animation.thisSideAnimations.isEmpty() || animation.otherSideAnimations.isEmpty()) {
            return null;
        }
        
        int thisSideIndex = animation.thisSideAnimations.size() - 1;
        PortalAnimationDriver lastThisSideAnimation = animation.thisSideAnimations.get(thisSideIndex);
        if (!(lastThisSideAnimation instanceof NormalAnimation thisSideNormalAnimation)) {
            return null;
        }
        
        int otherSideIndex = animation.otherSideAnimations.size() - 1;
        PortalAnimationDriver lastOtherSideAnimation = animation.otherSideAnimations.get(otherSideIndex);
        if (!(lastOtherSideAnimation instanceof NormalAnimation otherSideNormalAnimation)) {
            return null;
        }
        
        return new AnimationBuilderContext(
            portal,
            new Access<NormalAnimation>() {
                @Override
                public NormalAnimation get() {
                    return thisSideNormalAnimation;
                }
                
                @Override
                public void set(NormalAnimation normalAnimation) {
                    animation.thisSideAnimations.set(thisSideIndex, normalAnimation);
                }
            },
            new Access<NormalAnimation>() {
                @Override
                public NormalAnimation get() {
                    return otherSideNormalAnimation;
                }
                
                @Override
                public void set(NormalAnimation normalAnimation) {
                    animation.otherSideAnimations.set(otherSideIndex, normalAnimation);
                }
            }
        );
    }
    
    private static void sendAnimationHolderFailure(CommandContext<CommandSourceStack> context, Portal animationHolder) {
        context.getSource().sendFailure(Component.literal(
            "This portal entity is not animation holder. Use this command to " + animationHolder
        ));
    }
    
    private static void finishBuildingNormalAnimation(CommandContext<CommandSourceStack> context, Portal portal, int loopCount) {
        Portal animationHolder = portal.getAnimationHolder();
        if (animationHolder != null && animationHolder != portal) {
            sendAnimationHolderFailure(context, animationHolder);
            return;
        }
        
        PortalAnimation animation = portal.animation;
        
        AnimationBuilderContext animationBuilderContext = getAnimationBuilderContext(portal);
        
        if (animationBuilderContext == null) {
            context.getSource().sendFailure(Component.literal("No animation to build"));
            return;
        }
        
        List<NormalAnimation.Phase> thisSidePhases = animationBuilderContext.thisSideAnimation().get().phases;
        List<NormalAnimation.Phase> otherSidePhases = animationBuilderContext.otherSideAnimation().get().phases;
        
        if (thisSidePhases.isEmpty() || otherSidePhases.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No phase"));
            return;
        }
        
        NormalAnimation.Phase lastThisSidePhase = thisSidePhases.get(thisSidePhases.size() - 1);
        DeltaUnilateralPortalState lastThisSidePhaseDelta = lastThisSidePhase.delta().purgeFPError();
        NormalAnimation.Phase lastOtherSidePhase = otherSidePhases.get(otherSidePhases.size() - 1);
        DeltaUnilateralPortalState lastOtherSidePhaseDelta = lastOtherSidePhase.delta().purgeFPError();
        
        if (loopCount > 1 && !lastThisSidePhaseDelta.isIdentity() || !lastOtherSidePhaseDelta.isIdentity()) {
            // the animation does not return to the beginning state
            // insert another stage to make it return to the beginning state
            // otherwise it will abruptly jump
            thisSidePhases = ImmutableList.<NormalAnimation.Phase>builder()
                .addAll(thisSidePhases)
                .add(new NormalAnimation.Phase.Builder()
                    .durationTicks(5)
                    .timingFunction(TimingFunction.sine)
                    .delta(DeltaUnilateralPortalState.identity)
                    .build())
                .build();
            otherSidePhases = ImmutableList.<NormalAnimation.Phase>builder()
                .addAll(otherSidePhases)
                .add(new NormalAnimation.Phase.Builder()
                    .durationTicks(5)
                    .timingFunction(TimingFunction.sine)
                    .delta(DeltaUnilateralPortalState.identity)
                    .build())
                .build();
        }
        
        animationBuilderContext.thisSideAnimation().set(
            new NormalAnimation.Builder()
                .phases(thisSidePhases)
                .loopCount(loopCount)
                .startingGameTime(animationBuilderContext.thisSideAnimation().get().startingGameTime)
                .build()
        );
        animationBuilderContext.otherSideAnimation().set(
            new NormalAnimation.Builder()
                .phases(otherSidePhases)
                .loopCount(loopCount)
                .startingGameTime(animationBuilderContext.otherSideAnimation().get().startingGameTime)
                .build()
        );
        
        animation.setBackToPausingState(portal);
        
        animation.setPaused(portal, false);
        
        PortalExtension.forClusterPortals(portal, Portal::reloadAndSyncToClientNextTick);
        
        context.getSource().sendSuccess(getAnimationInfo(portal), false);
    }
    
    private static void giveRotationAnimation(Portal portal, Vec3 rotationCenter, Vec3 axis, double angularVelocity) {
        portal.addThisSideAnimationDriver(
            new RotationAnimation.Builder()
                .setInitialOffset(portal.getOriginPos().subtract(rotationCenter))
                .setRotationAxis(axis)
                .setDegreesPerTick(angularVelocity)
                .setStartGameTime(portal.getAnimationEffectiveTime())
                .setEndGameTime(Long.MAX_VALUE)
                .build()
        );
    }
    
    private static Component getAnimationInfo(Portal portal) {
        return portal.getAnimationView().getInfo();
    }
    
    private static record AnimationBuilderContext(
        Portal portal,
        Access<NormalAnimation> thisSideAnimation,
        Access<NormalAnimation> otherSideAnimation
    ) {
    
    }
}
