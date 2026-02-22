package me.skitttyy.kami.mixin;

import me.skitttyy.kami.api.management.RotationManager;
import me.skitttyy.kami.api.wrapper.IMinecraft;
import me.skitttyy.kami.impl.features.modules.client.AntiCheat;
import me.skitttyy.kami.impl.features.modules.movement.FastFirework;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static me.skitttyy.kami.api.wrapper.IMinecraft.mc;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity implements IMinecraft {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void setVelocityProxy(Entity instance, Vec3d velocity) {
        if (instance == MinecraftClient.getInstance().player && FastFirework.INSTANCE.isEnabled()) {
            Vec3d rotationVector = mc.player.getRotationVector();
            if (AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) {
                rotationVector = RotationManager.INSTANCE.getRotationVector();
            }

            double speed = FastFirework.INSTANCE.getSpeed();
            Vec3d currentVel = instance.getVelocity();
            
            instance.setVelocity(currentVel.add(
                rotationVector.x * speed + (rotationVector.x * 1.5 - currentVel.x) * 0.5,
                rotationVector.y * speed + (rotationVector.y * 1.5 - currentVel.y) * 0.5,
                rotationVector.z * speed + (rotationVector.z * 1.5 - currentVel.z) * 0.5
            ));
        } else {
            instance.addVelocity(velocity);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d getRotationVectorProxy(LivingEntity instance) {
        if (instance == MinecraftClient.getInstance().player) {
            if (AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) {
                return RotationManager.INSTANCE.getRotationVector();
            }
        }
        return instance.getRotationVector();
    }
}
