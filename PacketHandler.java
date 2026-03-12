package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    // 建立一條名為 main_channel 的網路通道
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // 註冊我們的封包
    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(SystemActionPacket.class, id++)
                .encoder(SystemActionPacket::encode)
                .decoder(SystemActionPacket::new)
                .consumerNetworkThread(SystemActionPacket::handle)
                .add();
    }
}