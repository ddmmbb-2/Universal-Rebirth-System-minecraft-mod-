package com.example.examplemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ShopManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 將會儲存在 .minecraft/config/examplemod_shop.json
    private static final File SHOP_FILE = FMLPaths.CONFIGDIR.get().resolve("examplemod_shop.json").toFile();
    
    // 存在記憶體裡的商品列表
    public static List<ShopItem> SHOP_ITEMS = new ArrayList<>();

    // 載入 JSON 的方法
    public static void loadShop() {
        if (!SHOP_FILE.exists()) {
            createDefaultShop();
        }

        try (FileReader reader = new FileReader(SHOP_FILE)) {
            Type listType = new TypeToken<ArrayList<ShopItem>>(){}.getType();
            SHOP_ITEMS = GSON.fromJson(reader, listType);
            LOGGER.info("轉生系統：成功從萬界商城載入 {} 件商品！", SHOP_ITEMS.size());
        } catch (Exception e) {
            LOGGER.error("轉生系統：載入商城失敗！", e);
        }
    }

    // 如果沒有檔案，自動生成一個預設的商城模板
    private static void createDefaultShop() {
// --- 基礎物資與食物 ---
        SHOP_ITEMS.add(new ShopItem("minecraft:cooked_beef", "§6炙烤靈牛肉", 80, "冒著熱氣、鮮嫩多汁的頂級肉質"));
        SHOP_ITEMS.add(new ShopItem("minecraft:golden_carrot", "§e黃金長生果", 200, "提供極高的飽食度與視覺恢復"));
        SHOP_ITEMS.add(new ShopItem("minecraft:golden_apple", "§e神恩蘋果", 500, "瞬間恢復生命與強大抗性"));
        SHOP_ITEMS.add(new ShopItem("minecraft:enchanted_golden_apple", "§6§l創世神遺果", 8000, "傳說中擁有不死之力的禁忌果實"));

        // --- 戰鬥神兵 ---
        SHOP_ITEMS.add(new ShopItem("minecraft:diamond_sword", "§b破空之劍", 1200, "削鐵如泥的凡界神兵"));
        SHOP_ITEMS.add(new ShopItem("minecraft:netherite_sword", "§8§l幽冥寂滅刃", 4500, "融合獄髓精華，斬斷靈魂的重刃"));
        SHOP_ITEMS.add(new ShopItem("minecraft:bow", "§a逐風長弓", 600, "箭出如龍，百步穿楊"));

        // --- 煉金藥水 (使用 NBT 標籤區分效果) ---
        // 註：若你的 ShopItem 邏輯不支援 NBT，請告知我，我再幫你改寫
        SHOP_ITEMS.add(new ShopItem("minecraft:potion{Potion:\"minecraft:strong_strength\"}", "§c戰神怒火 (力量 II)", 750, "短時間內大幅提升物理破壞力"));
        SHOP_ITEMS.add(new ShopItem("minecraft:potion{Potion:\"minecraft:long_fire_resistance\"}", "§6避火紅蓮 (抗火)", 500, "身入熔岩而不傷的避火秘藥"));
        SHOP_ITEMS.add(new ShopItem("minecraft:potion{Potion:\"minecraft:strong_healing\"}", "§d回天藥劑 (瞬間療傷 II)", 400, "將瀕死之人從冥界拉回的急救水"));
        SHOP_ITEMS.add(new ShopItem("minecraft:potion{Potion:\"minecraft:long_night_vision\"}", "§9明目靈液 (夜視)", 300, "洞穿黑暗，讓黑夜如同白晝"));

        // --- 神奇法寶 ---
        SHOP_ITEMS.add(new ShopItem("minecraft:elytra", "§d凌虛飛翼", 5000, "遨遊太虛的法寶"));
        SHOP_ITEMS.add(new ShopItem("minecraft:totem_of_undying", "§e替死泥偶", 3500, "抵擋一次致命天劫的保命符"));
        SHOP_ITEMS.add(new ShopItem("minecraft:ender_pearl", "§5縮地靈珠", 150, "瞬間移動至目光所及之處"));
        SHOP_ITEMS.add(new ShopItem("minecraft:golden_apple", "§e神恩蘋果", 500, "瞬間恢復生命與強大抗性"));
        SHOP_ITEMS.add(new ShopItem("minecraft:experience_bottle", "§a經驗瓶", 100, "洗滌靈魂的純粹精華"));
        SHOP_ITEMS.add(new ShopItem("minecraft:diamond_sword", "§b破空之劍", 1200, "削鐵如泥的凡界神兵"));
        SHOP_ITEMS.add(new ShopItem("minecraft:netherite_ingot", "§8獄髓錠", 2000, "打造絕世神兵的必要素材"));
        SHOP_ITEMS.add(new ShopItem("minecraft:enchanted_golden_apple", "§d附魔金蘋果", 3000, "傳說中的續命仙丹"));
        SHOP_ITEMS.add(new ShopItem("minecraft:totem_of_undying", "§6不死圖騰", 4000, "抵擋一次致命天劫"));
        SHOP_ITEMS.add(new ShopItem("minecraft:elytra", "§5凌虛飛翼", 5000, "遨遊太虛的無上法寶"));
        SHOP_ITEMS.add(new ShopItem("minecraft:diamond_block", "§b財富方塊", 8000, "純粹的財力展現"));

        try (FileWriter writer = new FileWriter(SHOP_FILE)) {
            GSON.toJson(SHOP_ITEMS, writer);
        } catch (IOException e) {
            LOGGER.error("轉生系統：無法建立預設商城檔案", e);
        }
    }
}