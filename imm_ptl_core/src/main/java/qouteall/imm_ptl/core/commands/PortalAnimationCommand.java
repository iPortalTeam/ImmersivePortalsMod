package qouteall.imm_ptl.core.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.NormalAnimation;
import qouteall.imm_ptl.core.portal.animation.RotationAnimation;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.q_misc_util.my_util.Vec2d;

import java.util.Collection;

public class PortalAnimationCommand {
    static void registerPortalAnimationCommands(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("clear")
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                PortalExtension.forClusterPortals(
                    portal, Portal::clearAnimationDrivers
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
                        new Vec2d(endingState.width / animationScale, endingState.height / animationScale),
                        new Vec2d(endingState.width, endingState.height),
                        portal.level.getGameTime(),
                        durationTicks,
                        TimingFunction.sine
                    ));
                    PortalCommand.reloadPortal(portal);
                }))
            )
        );
        
    }
}
