package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class SystemActionPacket {
    private final String action;   
    private final String payload;  
    private final int cost;        

    public SystemActionPacket(String action, String payload, int cost) {
        this.action = action;
        this.payload = payload;
        this.cost = cost;
    }

    public SystemActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.payload = buf.readUtf();
        this.cost = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeUtf(payload);
        buf.writeInt(cost);
    }

    // --- 【新增核心功能】解析物品與 NBT 標籤 (解決藥水與附魔物品無法給予的問題) ---
    private void giveItemToPlayer(ServerPlayer player, String fullId, String itemName) {
        String baseId = fullId;
        String nbtStr = "";
        
        // 判斷是否帶有 NBT 標籤 (例如 minecraft:potion{...})
        if (fullId.contains("{")) {
            int braceIndex = fullId.indexOf("{");
            nbtStr = fullId.substring(braceIndex);
            baseId = fullId.substring(0, braceIndex);
        }

        Item mcItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(baseId));
        if (mcItem != null && mcItem != Items.AIR) {
            ItemStack stack = new ItemStack(mcItem, 1);
            
            // 如果有 NBT 標籤，嘗試寫入物品中 (例如特定藥水效果)
            if (!nbtStr.isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(nbtStr);
                    stack.setTag(tag);
                } catch (Exception e) {
                    System.out.println("NBT 解析失敗: " + nbtStr);
                }
            }
            player.getInventory().add(stack);
            player.sendSystemMessage(Component.literal("§e[System] 已發放物品： " + itemName));
        } else {
            player.sendSystemMessage(Component.literal("§c[System] 系統錯誤：找不到物品 " + baseId));
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                int serverCoins = player.getPersistentData().getInt("Sys_Coins");
                
                if (serverCoins >= cost) {
                    int newCoins = serverCoins - cost;
                    
                    // 扣款與同步
                    player.getPersistentData().putInt("Sys_Coins", newCoins);
                    PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncCoinPacket(newCoins));

                    // 1. 商城直購
                    if (action.equals("buy")) {
                        String itemName = payload;
                        for (ShopItem item : ShopManager.SHOP_ITEMS) {
                            if (item.id().equals(payload)) itemName = item.name();
                        }
                        giveItemToPlayer(player, payload, itemName);
                    } 
                    
                    // 2. 【全新邏輯】動態抽獎系統
                    else if (action.equals("gacha")) {
                        if (ShopManager.SHOP_ITEMS.isEmpty()) {
                            player.sendSystemMessage(Component.literal("§c[Gacha] 商城目前為空，無法抽籤！"));
                            return;
                        }

                        // 利用客戶端傳來的 cost 判斷玩家買了哪種寶箱
                        int boxTier = 1; 
                        String tierName = "§a【木質寶箱】";
                        if (cost >= 1000) {
                            boxTier = 3;
                            tierName = "§e【黃金寶箱】";
                        } else if (cost >= 500) {
                            boxTier = 2;
                            tierName = "§b【白銀寶箱】";
                        }

                        double totalWeight = 0.0;
                        List<Double> weights = new ArrayList<>();

                        // 計算每件物品的「中獎權重」
                        for (ShopItem item : ShopManager.SHOP_ITEMS) {
                            double itemCost = Math.max(1, item.cost());
                            double weight = 0.0;
                            
                            // 演算法：根據寶箱等級，調整高價物品的權重懲罰
                            if (boxTier == 1) { 
                                // 木質寶箱：平方懲罰 (極難抽到高價物品)
                                weight = 1000000.0 / (itemCost * itemCost); 
                            } else if (boxTier == 2) { 
                                // 白銀寶箱：線性機率 (普通機率)
                                weight = 10000.0 / itemCost; 
                            } else { 
                                // 黃金寶箱：開根號 (大幅縮小便宜與高價物品的機率差距)
                                weight = 100.0 / Math.sqrt(itemCost); 
                            }
                            
                            weights.add(weight);
                            totalWeight += weight;
                        }

                        // 擲骰子
                        double randomValue = new Random().nextDouble() * totalWeight;
                        ShopItem wonItem = null;
                        
                        for (int i = 0; i < ShopManager.SHOP_ITEMS.size(); i++) {
                            randomValue -= weights.get(i);
                            if (randomValue <= 0) {
                                wonItem = ShopManager.SHOP_ITEMS.get(i);
                                break;
                            }
                        }

                        // 發放抽到的物品
                        if (wonItem != null) {
                            player.sendSystemMessage(Component.literal("§6[Gacha] ✨ 歐氣爆發！打開 " + tierName + " 獲得了！"));
                            giveItemToPlayer(player, wonItem.id(), wonItem.name());
                        }
                    }
                    
                    // 3. 升級屬性
                    else if (action.equals("upgrade")) {
                        String statName = payload;
                        int currentLevel = player.getPersistentData().getInt("Sys_" + statName);
                        int newLevel = Math.max(1, currentLevel) + 1;
                        player.getPersistentData().putInt("Sys_" + statName, newLevel);
                        
                        if (statName.equals("CON") || statName.equals("DEX")) {
                            ModEvents.applyConstitution(player);
                        }
                    }
                    
                    // 4. 洗髓
                    else if (action.equals("rebirth")) {
                        player.getPersistentData().putString("Sys_Physique", payload);
                        ModEvents.applyConstitution(player);
                    }
                    
                    // 5. 刷新商城
                    else if (action.equals("refresh")) {
                        player.sendSystemMessage(Component.literal("§d[System] 商城已刷新！"));
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}