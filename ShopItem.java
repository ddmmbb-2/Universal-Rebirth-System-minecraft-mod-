package com.example.examplemod;

// 這是一個輕量級的資料載體，用來對應 JSON 裡的每一個商品
public record ShopItem(
    String id,          // 物品 ID，例如 "minecraft:golden_apple"
    String name,        // 顯示名稱，例如 "§e神恩蘋果"
    int cost,           // 需要消耗的代幣數量
    String description  // 物品描述
) {}