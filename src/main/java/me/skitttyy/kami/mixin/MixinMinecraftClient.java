package me.skitttyy.kami.mixin;

import me.skitttyy.kami.api.event.events.TickEvent;
import me.skitttyy.kami.api.event.events.render.EntityOutlineEvent;
import me.skitttyy.kami.api.event.events.render.FrameEvent;
import me.skitttyy.kami.api.event.events.render.ScreenEvent;
import me.skitttyy.kami.api.gui.font.Fonts;
import me.skitttyy.kami.api.utils.render.WindowResizeCallback;
import me.skitttyy.kami.impl.features.modules.client.FontModule;
import me.skitttyy.kami.impl.features.modules.client.Optimizer;
import me.skitttyy.kami.impl.features.modules.misc.MultiTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow public abstract boolean isWindowFocused();
    @Shadow @Final private Window window;
    @Shadow @Final public GameOptions options;
    @Shadow public ClientPlayerInteractionManager interactionManager;
    @Shadow public ClientPlayerEntity player;

    @Inject(method = "render", at = @At("HEAD"))
    public void renderHook(boolean tick, CallbackInfo ci) {
        new FrameEvent.FrameFlipEvent().post();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickPre(CallbackInfo ci) {
        new TickEvent.ClientTickEvent().post();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tickPost(CallbackInfo ci) {
        new TickEvent.AfterClientTickEvent().post();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doAttack()Z")))
    public void hookEventAttack(CallbackInfo ci) {
        new TickEvent.VanillaTick().post();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;", ordinal = 0))
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

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"))
    private void onHandleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (MultiTask.INSTANCE.isEnabled() && this.player != null && this.player.isUsingItem()) {
            return;
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo ci) {
        if (MultiTask.INSTANCE.isEnabled() && this.interactionManager != null && this.interactionManager.isBreakingBlock()) {
            return;
        }
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> info) {
        if (Optimizer.INSTANCE.isEnabled() && Optimizer.INSTANCE.unfocusedFPS.getValue() && !isWindowFocused())
            info.setReturnValue((int) Math.min(Optimizer.INSTANCE.fps.getValue().intValue(), this.options.getMaxFps().getValue()));
    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EntityOutlineEvent entityOutlineEvent = new EntityOutlineEvent(entity);
        entityOutlineEvent.post();
        if (entityOutlineEvent.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void captureResize(CallbackInfo ci) {
        WindowResizeCallback.EVENT.invoker().onResized((MinecraftClient) (Object) this, this.window);
    }

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen modifyScreen(Screen value) {
        ScreenEvent.SetScreen event = new ScreenEvent.SetScreen(value);
        event.post();
        return event.getGuiScreen();
    }
}
    
