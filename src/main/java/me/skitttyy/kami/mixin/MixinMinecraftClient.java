package me.skitttyy.kami.mixin;

import me.skitttyy.kami.api.event.events.TickEvent;
import me.skitttyy.kami.api.event.events.render.EntityOutlineEvent;
import me.skitttyy.kami.api.event.events.render.FrameEvent;
import me.skitttyy.kami.api.event.events.render.ScreenEvent;
import me.skitttyy.kami.api.gui.font.Fonts;
import me.skitttyy.kami.api.utils.render.WindowResizeCallback;
import me.skitttyy.kami.impl.features.modules.client.FontModule;
import me.skitttyy.kami.impl.features.modules.client.Optimizer;
import me.skitttyy.kami.impl.features.modules.ghost.FastMechs;
import me.skitttyy.kami.impl.features.modules.misc.MultiTask;
import me.skitttyy.kami.mixin.accessor.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.skitttyy.kami.api.wrapper.IMinecraft.mc;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow
    public abstract boolean method_1569(); // isWindowFocused

    @Shadow
    @Final
    private Window field_1704; // window

    @Shadow
    @Final
    public GameOptions field_1690; // options

    @Inject(method = "method_1523", at = @At("HEAD")) // render
    public void render(boolean tick, CallbackInfo ci) {
        new FrameEvent.FrameFlipEvent().post();
    }

    @Inject(method = "method_1508", at = @At("HEAD")) // tick
    public void tickPre(CallbackInfo ci) {
        new TickEvent.ClientTickEvent().post();
    }

    @Inject(method = "method_1508", at = @At(value = "TAIL")) // tick
    public void tickPost(CallbackInfo ci) {
        new TickEvent.AfterClientTickEvent().post();
    }

    @Inject(method = "method_1507", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_746;method_6115()Z"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/class_310;method_1536()Z"))) // handleInputEvents, isUsingItem, doAttack
    public void hookEventAttack(CallbackInfo ci) {
        new TickEvent.VanillaTick().post();
    }

    @Inject(method = "method_1508", at = @At(value = "FIELD", target = "Lnet/minecraft/client/class_310;field_1765:Lnet/minecraft/class_408;")) // tick, overlay
    public void hookInputTick(CallbackInfo ci) {
        new TickEvent.InputTick().post();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    void postWindowInit(RunArgs args, CallbackInfo ci) {
        try {
            Fonts.CUSTOM = Fonts.create(FontModule.INSTANCE.fontSize.getValue().intValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "method_1537", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_746;method_6115()Z"), cancellable = true) // handleBlockBreaking, isUsingItem
    private void hookIsUsingItem(boolean breaking, CallbackInfo ci) {
        if (MultiTask.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "method_1531", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_636;method_2923()Z"), cancellable = true) // doItemUse, isBreakingBlock
    private void hookIsBreakingBlock(CallbackInfo ci) {
        if (MultiTask.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "method_1483", at = @At("HEAD"), cancellable = true) // getFramerateLimit
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> info) {
        if (Optimizer.INSTANCE.isEnabled() && Optimizer.INSTANCE.unfocusedFPS.getValue() && !method_1569())
            info.setReturnValue((int) Math.min(Optimizer.INSTANCE.fps.getValue().intValue(), this.field_1690.getMaxFps().getValue()));
    }

    @Inject(method = "method_1490", at = @At(value = "HEAD"), cancellable = true) // hasOutline
    private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EntityOutlineEvent entityOutlineEvent = new EntityOutlineEvent(entity);
        entityOutlineEvent.post();
        if (entityOutlineEvent.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_1508", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_757;method_3192()V")) // tick, GameRenderer.tick
    public void hookGameRenderTick(CallbackInfo ci) {
        new TickEvent.GameRenderTick().post();
    }

    @Inject(method = "method_1514", at = @At("TAIL")) // onResolutionChanged
    private void captureResize(CallbackInfo ci) {
        WindowResizeCallback.EVENT.invoker().onResized((MinecraftClient) (Object) this, this.field_1704);
    }

    @Inject(method = "method_1507", at = @At(value = "FIELD", target = "Lnet/minecraft/client/class_310;field_1752:I")) // handleInputEvents, itemUseCooldown
    public void hookItemUseCooldown(CallbackInfo ci) {
        if (mc.player != null && FastMechs.INSTANCE.isEnabled()) {
            int wantedDelay = FastMechs.INSTANCE.getWantedDelay(mc.player.getMainHandStack());
            if (wantedDelay != -1) {
                if (((IMinecraftClient) mc).getItemUseCooldown() > wantedDelay)
                    ((IMinecraftClient) mc).setItemUseCooldown(wantedDelay);
            }
        }
    }

    @ModifyVariable(method = "method_1504", at = @At(value = "HEAD"), argsOnly = true) // setScreen
    private Screen modifyScreen(Screen value) {
        ScreenEvent.SetScreen event = new ScreenEvent.SetScreen(value);
        event.post();
        return event.getGuiScreen();
    }
}
                       
