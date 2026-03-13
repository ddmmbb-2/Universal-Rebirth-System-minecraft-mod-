package com.example.examplemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 設定檔會存在 .minecraft/config/examplemod_settings.json
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("examplemod_settings.json").toFile();

    // 存放所有設定值的變數，預設值寫在這裡
    public static Settings DATA = new Settings();

    public static class Settings {
        public double coinsPerDamage = 10.0;       // 每受到 1 點傷害獲得的代幣
        public int baseUpgradeCost = 100;          // 升級的基礎代幣消耗
        public double normalUpgradeCurve = 1.15;   // 一般屬性升級曲線 (1.15 = 每次變貴 15%)
        public double conUpgradeCurve = 1.25;      // 體質(CON)升級曲線 (1.25 = 每次變貴 25%)
    }

    // 載入 JSON
    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                DATA = GSON.fromJson(reader, Settings.class);
                System.out.println("轉生系統：成功載入設定檔！");
            } catch (Exception e) {
                System.out.println("轉生系統：設定檔載入失敗，使用預設值！");
                e.printStackTrace();
            }
        } else {
            save(); // 如果檔案不存在，就建立一個預設的
        }
    }

    // 儲存 JSON
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(DATA, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}