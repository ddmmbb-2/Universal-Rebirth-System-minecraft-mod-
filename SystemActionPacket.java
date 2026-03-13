package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;

import java.util.Random;
import java.util.function.Supplier;

public class SystemActionPacket {
    private final String action;   // 動作類型 ("buy", "gacha", "upgrade", "rebirth", "refresh")
    private final String payload;  // 負載資料 (物品 ID, 機率, 屬性名稱)
    private final int cost;        // 花費代幣

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                int serverCoins = player.getPersistentData().getInt("Sys_Coins");
                
                // 確認伺服器上的錢足夠
                if (serverCoins >= cost) {
                    int newCoins = serverCoins - cost;
                    
                    // 1. 伺服器扣款存檔
                    player.getPersistentData().putInt("Sys_Coins", newCoins);
                    
                    // 2. 伺服器扣款後，立刻發封包叫客戶端同步最新餘額！(這行解決了金錢沒變化的問題)
                    PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncCoinPacket(newCoins));

                    // 3. 商城購買
                    if (action.equals("buy")) {
                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(payload));
                        if (item != null && item != Items.AIR) {
                            player.getInventory().add(new ItemStack(item, 1));
                            player.sendSystemMessage(Component.literal("§e[System] 交易完成，已發放物品！"));
                        }
                    } 
                    // 4. 抽籤邏輯
                    else if (action.equals("gacha")) {
                        int winChance = Integer.parseInt(payload);
                        int roll = new Random().nextInt(100) + 1;
                        if (roll <= winChance) {
                            player.sendSystemMessage(Component.literal("§6[Gacha] ✨ 歐氣爆發！恭喜獲得大獎【鑽石磚】！"));
                            player.getInventory().add(new ItemStack(Items.DIAMOND_BLOCK, 1));
                        } else {
                            player.sendSystemMessage(Component.literal("§c[Gacha] ☁ 銘謝惠顧，下次會更好..."));
                        }
                    }
                    // 5. 【新增】屬性升級 (伺服器端真正存檔)
                    else if (action.equals("upgrade")) {
                        String statName = payload;
                        int currentLevel = player.getPersistentData().getInt("Sys_" + statName);
                        if (currentLevel < 1) currentLevel = 1;
                        
                        int newLevel = currentLevel + 1;
                        player.getPersistentData().putInt("Sys_" + statName, newLevel);
                        
                        // 如果升級的是體質或敏捷，伺服器端要立刻重算血量與跑速
                        if (statName.equals("CON") || statName.equals("DEX")) {
                            ModEvents.applyConstitution(player);
                        }
                    }
                    // 6. 【新增】洗髓 (伺服器端真正存檔)
                    else if (action.equals("rebirth")) {
                        player.getPersistentData().putString("Sys_Physique", payload);
                        ModEvents.applyConstitution(player);
                    }
                    // 7. 【新增】刷新商城 (只需扣款，無需特別操作)
                    else if (action.equals("refresh")) {
                        player.sendSystemMessage(Component.literal("§d[System] 商城已刷新！"));
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}