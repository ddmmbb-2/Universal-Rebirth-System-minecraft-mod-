package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "examplemod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    public static boolean HAS_AWAKENED = false;
    private static int midAirJumps = 0;
    private static final Random RANDOM = new Random();

    public static class PlayerStats {
        public static int COINS = 0;
        public static int STR = 1, DEX = 1, CON = 1, PER = 1;
        public static int FIRE = 1, WATER = 1, WIND = 1, EARTH = 1;
        public static String PHYSIQUE = "§7[Mortal Body] 凡體";
    }

    // ==========================================
    // 肉身重塑：更新真實血量與敏捷速度
    // ==========================================
    private static final UUID DEX_MODIFIER_UUID = UUID.fromString("6373f1d4-8b63-49a0-b5bc-07eb9e7a2c26");

    public static void applyConstitution(Player player) {
        if (!HAS_AWAKENED) return;

        // 1. 體質 (CON)：血量
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double bonusHealth = (PlayerStats.CON - 1) * 2.0;
            if (PlayerStats.PHYSIQUE.contains("荒古聖體")) bonusHealth *= 3.0;
            maxHealth.setBaseValue(20.0 + bonusHealth);
        }

        // 2. 敏捷 (DEX)：自動曲線提升移速 (最高 +100% 跑速)
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(DEX_MODIFIER_UUID);
            if (PlayerStats.DEX > 1) {
                double bonusSpeed = (PlayerStats.DEX / 100.0) * 0.1; 
                speed.addTransientModifier(new AttributeModifier(DEX_MODIFIER_UUID, "System_DEX", bonusSpeed, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    // ==========================================
    // 存檔與讀檔系統
    // ==========================================
    public static void saveToPlayer(Player player) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean("Sys_Awakened", HAS_AWAKENED);
        data.putInt("Sys_Coins", PlayerStats.COINS);
        data.putInt("Sys_STR", PlayerStats.STR);
        data.putInt("Sys_DEX", PlayerStats.DEX);
        data.putInt("Sys_CON", PlayerStats.CON);
        data.putInt("Sys_PER", PlayerStats.PER);
        data.putInt("Sys_FIRE", PlayerStats.FIRE);
        data.putInt("Sys_WATER", PlayerStats.WATER);
        data.putInt("Sys_WIND", PlayerStats.WIND);
        data.putInt("Sys_EARTH", PlayerStats.EARTH);
        data.putString("Sys_Physique", PlayerStats.PHYSIQUE);
    }

    public static void loadFromPlayer(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains("Sys_Awakened")) {
            HAS_AWAKENED = data.getBoolean("Sys_Awakened");
            PlayerStats.COINS = data.getInt("Sys_Coins");
            PlayerStats.STR = Math.max(1, data.getInt("Sys_STR"));
            PlayerStats.DEX = Math.max(1, data.getInt("Sys_DEX"));
            PlayerStats.CON = Math.max(1, data.getInt("Sys_CON"));
            PlayerStats.PER = Math.max(1, data.getInt("Sys_PER"));
            PlayerStats.FIRE = Math.max(1, Math.min(100, data.getInt("Sys_FIRE"))); 
            PlayerStats.WATER = Math.max(1, Math.min(100, data.getInt("Sys_WATER")));
            PlayerStats.WIND = Math.max(1, Math.min(100, data.getInt("Sys_WIND")));
            PlayerStats.EARTH = Math.max(1, Math.min(100, data.getInt("Sys_EARTH")));
            PlayerStats.PHYSIQUE = data.getString("Sys_Physique");
            applyConstitution(player); 
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) { loadFromPlayer(event.getEntity()); }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) { saveToPlayer(event.getEntity()); }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        for (String key : oldData.getAllKeys()) {
            if (key.startsWith("Sys_")) newData.put(key, oldData.get(key));
        }
        loadFromPlayer(event.getEntity());
    }

    public static int getUpgradeCost(String statName, int currentLevel) {
        if (currentLevel >= 100) return 9999999; 
        double baseMult = 1.15;
        double physMod = 1.0;
        if (statName.equals("CON")) baseMult = 1.25;
        if (PlayerStats.PHYSIQUE.contains("荒古聖體") && statName.equals("CON")) physMod = 1.5;
        if (PlayerStats.PHYSIQUE.contains("虛空劍心") && (statName.equals("STR") || statName.equals("WIND"))) physMod = 0.6;
        if (PlayerStats.PHYSIQUE.contains("混沌靈體") && (statName.equals("FIRE") || statName.equals("WATER") || statName.equals("EARTH"))) physMod = 0.7;
        return (int) (100 * Math.pow(baseMult, currentLevel) * physMod);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !HAS_AWAKENED) {
            HAS_AWAKENED = true;
            String[] physiques = { "§6[Ancient Holy Body] 荒古聖體", "§b[Void Sword Heart] 虛空劍心", "§d[Chaos Spirit] 混沌靈體", "§7[Mortal Body] 凡體" };
            PlayerStats.PHYSIQUE = physiques[RANDOM.nextInt(physiques.length)];
            player.sendSystemMessage(Component.literal("§a[System] Awakening / 覺醒先天體質： " + PlayerStats.PHYSIQUE));
            saveToPlayer(player);
            applyConstitution(player); 
        }
    }

    // ==========================================
    // 受傷邏輯 (火系免疫、土系減傷、感知迴避)
    // ==========================================
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && HAS_AWAKENED) {
            
            // 【感知 PER】迴避機制
            float dodgeChance = (PlayerStats.PER / 100.0f) * 0.3f;
            if (RANDOM.nextFloat() < dodgeChance) {
                event.setCanceled(true);
                player.displayClientMessage(Component.literal("§b✨ 迴避成功！ (Dodge)"), true);
                return; 
            }

            // --- 【關鍵修復】挨打賺錢，強制只在伺服器端計算，然後同步給玩家 ---
            if (!player.level().isClientSide() && event.getSource().getEntity() instanceof LivingEntity) {
                int coinsEarned = (int) (event.getAmount() * 10);
                int currentCoins = player.getPersistentData().getInt("Sys_Coins");
                int newCoins = currentCoins + coinsEarned;
                
                // 1. 存入伺服器端真實 NBT
                player.getPersistentData().putInt("Sys_Coins", newCoins);
                
                // 2. 發送運鈔車封包，通知客戶端更新畫面
                PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player), new SyncCoinPacket(newCoins));

                // 3. 顯示入帳文字
                player.displayClientMessage(Component.literal("§e§lCoins + " + coinsEarned), true);
            }

            // 【火系 FIRE】平滑減免火焰傷害
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                float fireReduction = PlayerStats.FIRE / 100.0f;
                event.setAmount(event.getAmount() * (1.0f - fireReduction));
                if (PlayerStats.FIRE >= 100) player.clearFire(); 
            }

            // 【土系 EARTH】平滑物理減傷
            if (!event.isCanceled()) {
                float earthReduction = (PlayerStats.EARTH / 100.0f) * 0.5f; 
                event.setAmount(event.getAmount() * (1.0f - earthReduction));
            }
        }
    }

    // ==========================================
    // 攻擊邏輯 (STR 真實傷害, 火系燃燒與爆炸)
    // ==========================================
    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player && HAS_AWAKENED) {
            LivingEntity target = event.getEntity();
            
            float bonusDamage = (PlayerStats.STR - 1) * 0.5f;
            event.setAmount(event.getAmount() + bonusDamage);

            if (PlayerStats.FIRE >= 80) {
                target.setSecondsOnFire(5); 
            }
            if (PlayerStats.FIRE >= 100) {
                target.level().explode(player, target.getX(), target.getY(), target.getZ(), 2.0F, Level.ExplosionInteraction.NONE);
            }
        }
    }

    // ==========================================
    // 【水系 WATER】延長換氣 & 【土系 EARTH】連鎖挖礦
    // ==========================================
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        
        // 【關鍵修改】在這裡加上 player.isCrouching()，確保玩家處於潛行狀態才會執行
        if (player != null && !player.level().isClientSide() && HAS_AWAKENED && player.isCrouching()) {
            // 【土系 EARTH】連鎖挖礦 (指數曲線)
            int maxChain = (int) (Math.pow(PlayerStats.EARTH / 100.0f, 2) * 255);
            
            if (maxChain > 1) {
                BlockState state = event.getState();
                if (state.is(BlockTags.LOGS) || state.is(Tags.Blocks.ORES)) {
                    // 這裡呼叫下方定義好的 processVeinMine 方法
                    processVeinMine((ServerLevel) player.level(), player, event.getPos(), state, maxChain);
                }
            }
        }
    }

    // 【被遺漏的核心方法】連鎖挖礦的遞迴尋路 (BFS 演算法)
    private static void processVeinMine(ServerLevel level, Player player, BlockPos startPos, BlockState targetState, int maxBlocks) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);
        int blocksBroken = 0;

        while (!queue.isEmpty() && blocksBroken < maxBlocks) {
            BlockPos current = queue.poll();
            
            if (!current.equals(startPos)) {
                level.destroyBlock(current, true, player);
                blocksBroken++;
                player.getMainHandItem().hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    if (level.getBlockState(neighbor).getBlock() == targetState.getBlock()) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    // ==========================================
    // 【土系 EARTH】一鍵收割與豐收加成
    // ==========================================
    @SubscribeEvent
    public static void onRightClickCrop(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!HAS_AWAKENED || player.level().isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);
        
        if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
            if (crop.isMaxAge(state)) {
                ServerLevel level = (ServerLevel) player.level();
                net.minecraft.world.level.block.Block.dropResources(state, level, pos);
                level.setBlock(pos, crop.getStateForAge(0), 3);
                event.setCanceled(true); 
                
                if (RANDOM.nextFloat() < (PlayerStats.EARTH / 100.0f)) {
                     net.minecraft.world.level.block.Block.dropResources(state, level, pos);
                     player.displayClientMessage(Component.literal("§6✨ 豐收！土系之力使作物加倍！"), true);
                }
            }
        }
    }

    // ==========================================
    // 按鍵與跳躍邏輯
    // ==========================================
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ExampleMod.ClientModEvents.SYSTEM_KEY.consumeClick()) {
            if (Minecraft.getInstance().player != null && HAS_AWAKENED) {
                Minecraft.getInstance().setScreen(new SystemScreen());
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        if (event.getKey() == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE && event.getAction() == 1) {
            int maxJumps = (int) ((PlayerStats.WIND / 100.0f) * 5); 
            if (!mc.player.onGround() && midAirJumps < maxJumps) {
                mc.player.jumpFromGround();
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, 0.4, 0));
                midAirJumps++;
            }
        }
    }

    // ==========================================
    // 每秒偵測：【感知 PER】透視 & 【水系 WATER】水下呼吸
    // ==========================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player.onGround()) midAirJumps = 0;
        
        if (HAS_AWAKENED) {
            if (player.isUnderWater() && PlayerStats.WATER >= 10) {
                float saveChance = (PlayerStats.WATER - 10) / 90.0f; 
                if (RANDOM.nextFloat() < saveChance) {
                    player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 1));
                }
            }

            if (!player.level().isClientSide() && player.tickCount % 20 == 0 && PlayerStats.PER > 1) {
                double radius = 5 + ((PlayerStats.PER / 100.0f) * 45); 
                List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius));
                for (Mob mob : mobs) mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
            }
        }
    }
}