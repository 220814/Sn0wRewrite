package me.skitttyy.kami.mixin;

import me.skitttyy.kami.api.event.events.TickEvent;
import me.skitttyy.kami.api.event.events.render.*;
import me.skitttyy.kami.api.gui.font.Fonts;
import me.skitttyy.kami.api.utils.render.WindowResizeCallback;
import me.skitttyy.kami.api.wrapper.IMinecraft;
import me.skitttyy.kami.impl.features.modules.client.FontModule;
import me.skitttyy.kami.impl.features.modules.client.Optimizer;
import me.skitttyy.kami.impl.features.modules.misc.MultiTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.skitttyy.kami.api.wrapper.IMinecraft.mc;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MixinMinecraftClient implements IMinecraft {

    @Shadow public abstract boolean isWindowFocused();
    @Shadow @Final private Window window;
    @Shadow @Final public GameOptions options;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void renderHook(boolean tick, CallbackInfo ci) {
        new FrameEvent.FrameFlipEvent().post();
        if (mc.world != null && mc.player != null) {
            new Render3DEvent().post();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER), remap = false)
    private void render2DHook(boolean tick, CallbackInfo ci) {
        RenderTickCounter tickCounter = mc.getRenderTickCounter();
        DrawContext drawContext = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
        new Render2DEvent(drawContext, tickCounter.getTickDelta(true)).post();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void onTickPre(CallbackInfo ci) {
        new TickEvent.ClientTickEvent().post();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void onTickPost(CallbackInfo ci) {
        new TickEvent.AfterClientTickEvent().post();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doAttack()Z")), remap = false)
    public void hookEventAttack(CallbackInfo ci) {
        new TickEvent.VanillaTick().post();
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    void postWindowInit(RunArgs args, CallbackInfo ci) {
        try {
            if (FontModule.INSTANCE != null) {
                Fonts.CUSTOM = Fonts.create(FontModule.INSTANCE.fontSize.getValue().intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), cancellable = true, remap = false)
    private void hookIsUsingItem(boolean breaking, CallbackInfo ci) {
        if (MultiTask.INSTANCE != null && MultiTask.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"), cancellable = true, remap = false)
    private void hookIsBreakingBlock(CallbackInfo ci) {
        if (MultiTask.INSTANCE != null && MultiTask.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> info) {
        if (Optimizer.INSTANCE != null && Optimizer.INSTANCE.isEnabled() && Optimizer.INSTANCE.unfocusedFPS.getValue() && !isWindowFocused())
            info.setReturnValue((int) Math.min(Optimizer.INSTANCE.fps.getValue().intValue(), this.options.getMaxFps().getValue()));
    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true, remap = false)
    private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EntityOutlineEvent event = new EntityOutlineEvent(entity);
        event.post();
        if (event.isCancelled()) cir.setReturnValue(true);
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"), remap = false)
    private void captureResize(CallbackInfo ci) {
        WindowResizeCallback.EVENT.invoker().onResized((MinecraftClient) (Object) this, this.window);
    }

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, remap = false)
    private Screen modifyScreen(Screen value) {
        ScreenEvent.SetScreen event = new ScreenEvent.SetScreen(value);
        event.post();
        return event.getGuiScreen();
    }
}
                                        
