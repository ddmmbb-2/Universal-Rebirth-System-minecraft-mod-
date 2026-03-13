package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// 這裡是你的 Mod ID，必須跟 mods.toml 一致
@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 基礎註冊系統 (保留 Forge 範例的架構，以後加物品會用到)
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 註冊基礎內容
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // --- 【核心：註冊你的系統事件】 ---
        // 這行會告訴 Forge 去監聽 ModEvents.java 裡的死亡、受傷、按鍵邏輯
        MinecraftForge.EVENT_BUS.register(ModEvents.class);

        // 註冊主程式本身
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("轉生系統：通用設置載入中...");

        // --- 啟動時讀取通用設定檔 ---
        ModConfig.load();

        
        // 啟動時讀取萬界商城 JSON
        ShopManager.loadShop(); 



        PacketHandler.register();
    }




    // --- 【核心：客戶端專屬事件（按鍵註冊）】 ---
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        // 1. 定義 J 鍵 (KeyMapping)
        public static final net.minecraft.client.KeyMapping SYSTEM_KEY = new net.minecraft.client.KeyMapping(
                "key.examplemod.system_menu",
                com.mojang.blaze3d.platform.InputConstants.KEY_J,
                "key.categories.misc"
        );

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("轉生系統：客戶端已啟動，宿主名稱: {}", Minecraft.getInstance().getUser().getName());
        }

        // 2. 真正把 J 鍵登記到遊戲的按鍵選單中
        @SubscribeEvent
        public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(SYSTEM_KEY);
        }
    }
}