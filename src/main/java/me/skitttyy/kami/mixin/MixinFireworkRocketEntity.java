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

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    public void setVelocityProxy(Entity instance, Vec3d vec3d) {
        if (instance == MinecraftClient.getInstance().player) {
            if (FastFirework.INSTANCE.isEnabled()) {
                Vec3d rotationVector = mc.player.getRotationVector();
                if (AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) {
                    rotationVector = RotationManager.INSTANCE.getRotationVector();
                }

                double d = 1.5;
                double e = FastFirework.INSTANCE.getSpeed();
                Vec3d currentVelocity = instance.getVelocity();
                instance.setVelocity(currentVelocity.add(
                        rotationVector.x * e + (rotationVector.x * d - currentVelocity.x) * 0.5,
                        rotationVector.y * e + (rotationVector.y * d - currentVelocity.y) * 0.5,
                        rotationVector.z * e + (rotationVector.z * d - currentVelocity.z) * 0.5
                ));
                return;
            }
        }
        instance.setVelocity(vec3d);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d getRotationVectorProxy(LivingEntity instance) {
        if (instance == MinecraftClient.getInstance().player) {
            if (AntiCheat.INSTANCE.strafeFix.getValue() && MinecraftClient.getInstance().player != null && RotationManager.INSTANCE.getRotation() != null) {
                return RotationManager.INSTANCE.getRotationVector();
            }
        }
        return instance.getRotationVector();
    }
}
                    
