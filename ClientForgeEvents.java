package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 這個標籤會告訴 Forge：這是一個只在「客戶端 (玩家畫面)」執行的事件，伺服器不需要管它
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onRenderHUD(RenderGuiOverlayEvent.Post event) {
        // 確保只在遊戲畫「快捷欄 (HOTBAR)」的時候才順便畫我們的錢
        // 這樣可以避免每一幀重複畫好幾次，導致畫面閃爍或拖慢效能
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 如果玩家還沒死過(還沒覺醒)，就不顯示錢錢介面
        if (!ModEvents.HAS_AWAKENED) return;

        // 讀取玩家當前的錢錢 (結合 NBT 存檔與記憶體中的數值)
        int coins = mc.player.getPersistentData().getInt("Sys_Coins");
        int displayCoins = Math.max(coins, ModEvents.PlayerStats.COINS);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        
        // 取得當前遊戲視窗的寬度與高度
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // 設定要顯示的文字內容
        String coinText = "§e💰 代幣: " + displayCoins;

        // 計算這段文字的寬度，方便我們做「靠右對齊」
        int textWidth = mc.font.width(coinText);

        // 繪製文字到畫面上！
        // X 座標：螢幕最右邊 - 文字寬度 - 10格安全邊界
        // Y 座標：螢幕最底下 - 20格 (剛好在快捷欄的右上方一點點，不會擋到物品)
        // 最後的 true 代表開啟文字陰影，讓字體更立體清晰
        guiGraphics.drawString(mc.font, coinText, screenWidth - textWidth - 10, screenHeight - 20, 0xFFFFFF, true);
    }
}