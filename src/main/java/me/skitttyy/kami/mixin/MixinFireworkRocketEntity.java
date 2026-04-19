package me.skitttyy.kami.mixin;

import me.skitttyy.kami.api.management.RotationManager;
import me.skitttyy.kami.api.wrapper.IMinecraft;
import me.skitttyy.kami.impl.features.modules.client.AntiCheat;
import me.skitttyy.kami.impl.features.modules.movement.FastFirework;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FireworkRocketEntity.class, priority = 1100)
public abstract class MixinFireworkRocketEntity implements IMinecraft {

    @Redirect(method = "method_5773", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1297;method_18799(Lnet/minecraft/class_243;)V"))
    private void setVelocityProxy(Entity instance, Vec3d velocity) {
        if (instance == MinecraftClient.getInstance().player && FastFirework.INSTANCE.isEnabled()) {
            Vec3d rotationVector = instance.method_5720();
            
            if (AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) {
                rotationVector = RotationManager.INSTANCE.getRotationVector();
            }

            double speed = FastFirework.INSTANCE.getSpeed();
            Vec3d currentVel = instance.getVelocity();
            
            Vec3d newVel = new Vec3d(
                currentVel.x + (rotationVector.x * speed + (rotationVector.x * 1.5 - currentVel.x) * 0.5),
                currentVel.y + (rotationVector.y * speed + (rotationVector.y * 1.5 - currentVel.y) * 0.5),
                currentVel.z + (rotationVector.z * speed + (rotationVector.z * 1.5 - currentVel.z) * 0.5)
            );
            instance.method_18799(newVel);
        } else {
            instance.method_18799(velocity);
        }
    }

    @Redirect(method = "method_5773", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1297;method_5720()Lnet/minecraft/class_243;"))
    private Vec3d getRotationVectorProxy(Entity instance) {
        if (instance == MinecraftClient.getInstance().player) {
            if (AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) {
                return RotationManager.INSTANCE.getRotationVector();
            }
        }
        return instance.method_5720();
    }
}
