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

@Mixin(value = FireworkRocketEntity.class, priority = 10000)
public abstract class MixinFireworkRocketEntity implements IMinecraft {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void setVelocityProxy(Entity instance, Vec3d velocity) {
        if (instance == MinecraftClient.getInstance().player && FastFirework.INSTANCE.isEnabled()) {
            Vec3d rotation = (AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) 
                ? RotationManager.INSTANCE.getRotationVector() 
                : instance.getRotationVector();

            double s = FastFirework.INSTANCE.getSpeed();
            Vec3d v = instance.getVelocity();
            
            instance.setVelocity(new Vec3d(
                v.x + (rotation.x * s + (rotation.x * 1.5 - v.x) * 0.5),
                v.y + (rotation.y * s + (rotation.y * 1.5 - v.y) * 0.5),
                v.z + (rotation.z * s + (rotation.z * 1.5 - v.z) * 0.5)
            ));
        } else {
            instance.setVelocity(velocity);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d getRotationVectorProxy(Entity instance) {
        if (instance == MinecraftClient.getInstance().player && AntiCheat.INSTANCE.strafeFix.getValue() && RotationManager.INSTANCE.getRotation() != null) {
            return RotationManager.INSTANCE.getRotationVector();
        }
        return instance.getRotationVector();
    }
}
                                         
