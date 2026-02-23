package me.skitttyy.kami.mixin;

import me.skitttyy.kami.api.event.events.LivingEvent;
import me.skitttyy.kami.api.event.events.move.LookEvent;
import me.skitttyy.kami.api.event.events.move.PushEvent;
import me.skitttyy.kami.api.event.events.player.TeamColorEvent;
import me.skitttyy.kami.api.wrapper.IMinecraft;
import me.skitttyy.kami.impl.features.modules.movement.ElytraFly;
import me.skitttyy.kami.impl.features.modules.movement.Flight;
import me.skitttyy.kami.impl.features.modules.movement.LongJump;
import me.skitttyy.kami.impl.features.modules.player.Tweaks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements IMinecraft {

    @Shadow
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        return null;
    }

    // method_5705 = pushAwayFrom
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true, remap = false)
    private void hookPushAwayFrom(Entity entity, CallbackInfo ci) {
        PushEvent.Entities event = new PushEvent.Entities(((Entity) ((Object) this)), entity);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    // method_5784 = updateVelocity
    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true, remap = false)
    private void hookUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            LookEvent.LookVelocityEvent updateVelocityEvent = new LookEvent.LookVelocityEvent(
                movementInput, speed, mc.player.getYaw(), movementInputToVelocity(movementInput, speed, mc.player.getYaw())
            );
            updateVelocityEvent.post();
            if (updateVelocityEvent.isCancelled()) {
                ci.cancel();
                mc.player.setVelocity(mc.player.getVelocity().add(updateVelocityEvent.getVelocity()));
            }
        }
    }

    // method_18376 = getPose
    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true, remap = false)
    private void getPoseHook(CallbackInfoReturnable<EntityPose> info) {
        if ((Object) this == mc.player) {
            if (LongJump.isGrimJumping() || Flight.isGrimFlying() || ElytraFly.isPacketFlying()) {
                info.setReturnValue(EntityPose.STANDING);
            } else if (mc.player.isSneaking() && Tweaks.INSTANCE.crouch.getValue()) {
                info.setReturnValue(EntityPose.CROUCHING);
            }
        }
    }

    // method_36454 = isCrawling
    @Inject(method = "isCrawling", at = @At("HEAD"), cancellable = true, remap = false)
    private void isCrawlingHook(CallbackInfoReturnable<Boolean> info) {
        if ((Object) this == mc.player && Tweaks.INSTANCE.isEnabled() && Tweaks.INSTANCE.noCrawl.getValue()) {
            info.setReturnValue(false);
        }
    }

    // method_5723 = getTeamColorValue
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void hookGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        TeamColorEvent event = new TeamColorEvent((Entity) (Object) this);
        event.post();
        if (event.isCancelled()) {
            cir.setReturnValue(event.getColor());
        }
    }

    // method_5844 = slowMovement
    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true, remap = false)
    private void hookSlowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        if ((Object) this != mc.player) return;
        LivingEvent.BlockSlowdown event = new LivingEvent.BlockSlowdown(state);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
                                    
