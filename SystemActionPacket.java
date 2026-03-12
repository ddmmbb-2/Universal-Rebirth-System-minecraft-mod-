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
    private final String action;   // 動作類型 ("buy" 或 "gacha")
    private final String payload;  // 負載資料 (物品 ID 或 中獎機率)
    private final int cost;        // 花費代幣

    public SystemActionPacket(String action, String payload, int cost) {
        this.action = action;
        this.payload = payload;
        this.cost = cost;
    }

    // 接收端 (解碼)
    public SystemActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.payload = buf.readUtf();
        this.cost = buf.readInt();
    }

    // 發送端 (編碼)
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeUtf(payload);
        buf.writeInt(cost);
    }

    // --- 【伺服器端收到封包後的處理邏輯】 ---
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 1. 讀取伺服器端的玩家真實存款 (防作弊)
                int serverCoins = player.getPersistentData().getInt("Sys_Coins");
                
                if (serverCoins >= cost) {
                    // 2. 扣款並存檔
                    player.getPersistentData().putInt("Sys_Coins", serverCoins - cost);

                    // 3. 發放商城物品
                    if (action.equals("buy")) {
                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(payload));
                        if (item != null && item != Items.AIR) {
                            player.getInventory().add(new ItemStack(item, 1)); // 給予物品
                            player.sendSystemMessage(Component.literal("§e[System] 交易完成，已發放物品！"));
                        }
                    } 
                    // 4. 抽籤邏輯 (由伺服器決定機率，杜絕客戶端竄改)
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
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}