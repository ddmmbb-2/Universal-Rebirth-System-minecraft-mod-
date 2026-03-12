package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SystemScreen extends Screen {

    // 0 = 主頁面 (Main), 1 = 萬界商城 (Shop), 2 = 命運抽籤 (Gacha)
    private int currentTab = 0; 
    private final Random random = new Random();
    
    // 【關鍵修改 1】加上 static！讓商品清單變成「全域記憶」，關掉選單也不會遺忘
    private static List<ShopItem> currentShopItems = new ArrayList<>();

    public SystemScreen() {
        super(Component.literal("Universal System"));
        // 【關鍵修改 2】只有在「系統剛啟動、貨架全空」的時候才免費進貨一次
        if (currentShopItems.isEmpty()) {
            refreshShop();
        }
    }

    // 隨機從總商品庫中挑選 4 個商品 (改為 static 方便互相呼叫)
    private static void refreshShop() {
        if (ShopManager.SHOP_ITEMS.isEmpty()) return;
        List<ShopItem> allItems = new ArrayList<>(ShopManager.SHOP_ITEMS);
        Collections.shuffle(allItems); // 將列表順序打亂 (洗牌)
        currentShopItems = allItems.subList(0, Math.min(4, allItems.size())); // 取前 4 個
    }

    @Override
    protected void init() {
        int midX = this.width / 2;
        int midY = this.height / 2;

        // ==========================================
        // 頂部分頁按鈕 (Tab Buttons)
        // ==========================================
        int tabWidth = 100;
        this.addRenderableWidget(Button.builder(Component.literal("主頁 / Main"), (btn) -> switchTab(0))
                .bounds(midX - 160, midY - 100, tabWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("商城 / Shop"), (btn) -> switchTab(1))
                .bounds(midX - 50, midY - 100, tabWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("抽籤 / Gacha"), (btn) -> switchTab(2))
                .bounds(midX + 60, midY - 100, tabWidth, 20).build());

        if (currentTab == 0) initMainTab(midX, midY);
        else if (currentTab == 1) initShopTab(midX, midY);
        else if (currentTab == 2) initGachaTab(midX, midY);
    }

    private void switchTab(int tabIndex) {
        if (this.currentTab != tabIndex) {
            this.currentTab = tabIndex;
            this.rebuildWidgets(); 
        }
    }

    // ==========================================
    // 分頁 0: 主頁 (Main Tab) 
    // ==========================================
    private void initMainTab(int midX, int midY) {
        this.addRenderableWidget(Button.builder(Component.literal("§d洗髓 / Rebirth (1000)"), (btn) -> {
            if (ModEvents.PlayerStats.COINS >= 1000) {
                ModEvents.PlayerStats.COINS -= 1000;
                String[] physiques = { "§6[Ancient Holy Body] 荒古聖體", "§b[Void Sword Heart] 虛空劍心", "§d[Chaos Spirit] 混沌靈體", "§7[Mortal Body] 凡體" };
                ModEvents.PlayerStats.PHYSIQUE = physiques[random.nextInt(physiques.length)];
                if (Minecraft.getInstance().player != null) {
                    ModEvents.applyConstitution(Minecraft.getInstance().player);
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a[System] 洗髓成功！當前體質：" + ModEvents.PlayerStats.PHYSIQUE));
                }
            }
        }).bounds(midX - 160, midY + 65, 140, 20).build());

        int col1 = midX + 10; 
        int col2 = midX + 90; 
        int btnW = 75;

        // 物理系
        this.addRenderableWidget(Button.builder(Component.literal("§c力量 STR"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("STR", ModEvents.PlayerStats.STR);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.STR++; }
        }).bounds(col1, midY - 35, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§a體質 CON"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("CON", ModEvents.PlayerStats.CON);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.CON++; if (Minecraft.getInstance().player != null) ModEvents.applyConstitution(Minecraft.getInstance().player); }
        }).bounds(col1, midY - 10, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§e敏捷 DEX"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("DEX", ModEvents.PlayerStats.DEX);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.DEX++; if (Minecraft.getInstance().player != null) ModEvents.applyConstitution(Minecraft.getInstance().player); }
        }).bounds(col1, midY + 15, btnW, 20).build());

        // 元素系
        this.addRenderableWidget(Button.builder(Component.literal("§b風系 WIND"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("WIND", ModEvents.PlayerStats.WIND);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.WIND++; }
        }).bounds(col2, midY - 35, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§4火系 FIRE"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("FIRE", ModEvents.PlayerStats.FIRE);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.FIRE++; }
        }).bounds(col2, midY - 10, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§9水系 WATER"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("WATER", ModEvents.PlayerStats.WATER);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.WATER++; }
        }).bounds(col2, midY + 15, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§6土系 EARTH"), (btn) -> {
            int cost = ModEvents.getUpgradeCost("EARTH", ModEvents.PlayerStats.EARTH);
            if (ModEvents.PlayerStats.COINS >= cost) { ModEvents.PlayerStats.COINS -= cost; ModEvents.PlayerStats.EARTH++; }
        }).bounds(col2, midY + 40, btnW, 20).build());
    }

    // ==========================================
    // 分頁 1: 商城 (Shop Tab) - 花錢手動刷新
    // ==========================================
    private void initShopTab(int midX, int midY) {
        int startY = midY - 35;
        
        // 讀取 currentShopItems 生成按鈕
        for (int i = 0; i < currentShopItems.size(); i++) {
            ShopItem item = currentShopItems.get(i);
            int yOffset = startY + (i * 30); 

            this.addRenderableWidget(Button.builder(Component.literal("購買 / Buy " + item.cost() + " Coins"), (btn) -> {
                if (ModEvents.PlayerStats.COINS >= item.cost()) {
                    ModEvents.PlayerStats.COINS -= item.cost();
                    PacketHandler.INSTANCE.sendToServer(new SystemActionPacket("buy", item.id(), item.cost()));
                }
            }).bounds(midX + 60, yOffset - 4, 100, 20).build());
        }

        // 手動刷新商城按鈕 (維持不變，只要扣錢就會重洗牌並重繪畫面)
        this.addRenderableWidget(Button.builder(Component.literal("§d刷新商城 / Refresh (100)"), (btn) -> {
            if (ModEvents.PlayerStats.COINS >= 100) {
                ModEvents.PlayerStats.COINS -= 100; // 扣錢
                refreshShop(); // 重新洗牌
                this.rebuildWidgets(); // 重新繪製畫面按鈕
            }
        }).bounds(midX - 50, midY + 90, 100, 20).build());
    }

    // ==========================================
    // 分頁 2: 抽籤 (Gacha Tab) 
    // ==========================================
    private void initGachaTab(int midX, int midY) {
        this.addRenderableWidget(Button.builder(Component.literal("§a木質寶箱 / Wooden (100)"), (btn) -> rollGacha(100, 80))
                .bounds(midX - 75, midY - 30, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§b白銀寶箱 / Silver (500)"), (btn) -> rollGacha(500, 50))
                .bounds(midX - 75, midY + 10, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§e黃金寶箱 / Golden (1000)"), (btn) -> rollGacha(1000, 20))
                .bounds(midX - 75, midY + 50, 150, 20).build());
    }

    private void rollGacha(int cost, int winChance) {
        if (ModEvents.PlayerStats.COINS >= cost) {
            ModEvents.PlayerStats.COINS -= cost; 
            PacketHandler.INSTANCE.sendToServer(new SystemActionPacket("gacha", String.valueOf(winChance), cost));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int midX = this.width / 2;
        int midY = this.height / 2;

        guiGraphics.fill(midX - 180, midY - 110, midX + 180, midY + 110, 0xDD000000); 
        guiGraphics.fill(midX - 180, midY - 110, midX + 180, midY - 75, 0xFF222222); 
        guiGraphics.drawCenteredString(this.font, "§6§l◢ Universal System / 諸天萬界系統 ◣", midX, midY - 125, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§e餘額 / Balance: §f" + ModEvents.PlayerStats.COINS + " Coins", midX - 160, midY - 125, 0xFFFFFF, false);

        if (currentTab == 0) {
            guiGraphics.fill(midX, midY - 70, midX + 1, midY + 90, 0x55FFFFFF); 
            
            int leftX = midX - 160;
            guiGraphics.drawString(this.font, "§e【宿主狀態 / Status】", leftX, midY - 65, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "體質: " + ModEvents.PlayerStats.PHYSIQUE, leftX, midY - 45, 0xFFFFFF, false);
            
            guiGraphics.drawString(this.font, "§cSTR: §f" + ModEvents.PlayerStats.STR, leftX, midY - 20, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§aCON: §f" + ModEvents.PlayerStats.CON, leftX, midY - 5, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§eDEX: §f" + ModEvents.PlayerStats.DEX, leftX, midY + 10, 0xFFFFFF, false);
            
            guiGraphics.drawString(this.font, "§bWIND:  §f" + ModEvents.PlayerStats.WIND, leftX + 60, midY - 20, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§4FIRE:  §f" + ModEvents.PlayerStats.FIRE, leftX + 60, midY - 5, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§9WATER: §f" + ModEvents.PlayerStats.WATER, leftX + 60, midY + 10, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "§6EARTH: §f" + ModEvents.PlayerStats.EARTH, leftX + 60, midY + 25, 0xFFFFFF, false);

            guiGraphics.drawString(this.font, "§a【強化中心 / Upgrades】", midX + 10, midY - 65, 0xFFFFFF, false);
        
        } else if (currentTab == 1) {
            guiGraphics.drawCenteredString(this.font, "§b【萬界商城 / Universal Shop】", midX, midY - 65, 0xFFFFFF);
            
            int startY = midY - 35;
            for (int i = 0; i < currentShopItems.size(); i++) {
                ShopItem item = currentShopItems.get(i);
                int yOffset = startY + (i * 30);
                guiGraphics.drawString(this.font, item.name(), midX - 160, yOffset, 0xFFFFFF, false);
                guiGraphics.drawString(this.font, "§7" + item.description(), midX - 160, yOffset + 10, 0xAAAAAA, false);
            }
        } else if (currentTab == 2) {
            guiGraphics.drawCenteredString(this.font, "§d【命運抽籤 / Destiny Gacha】", midX, midY - 65, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "§7機率: 木質 80% | 白銀 50% | 黃金 20%", midX, midY - 50, 0xAAAAAA);
        }

        guiGraphics.drawCenteredString(this.font, "§8Press [ESC] to close / 按 ESC 關閉", midX, midY + 120, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}