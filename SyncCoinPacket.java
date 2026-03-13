package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncCoinPacket {
    private final int coins;

    public SyncCoinPacket(int coins) {
        this.coins = coins;
    }

    public SyncCoinPacket(FriendlyByteBuf buf) {
        this.coins = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(coins);
    }

    // 當客戶端(玩家畫面)收到這台運鈔車時，要做的事：
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                // 1. 更新客戶端的 NBT 存檔
                Minecraft.getInstance().player.getPersistentData().putInt("Sys_Coins", coins);
                // 2. 更新記憶體變數，確保 UI 瞬間刷新
                ModEvents.PlayerStats.COINS = coins; 
            }
        });
        ctx.get().setPacketHandled(true);
    }
}