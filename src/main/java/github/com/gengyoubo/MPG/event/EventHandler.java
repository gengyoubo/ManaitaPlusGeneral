package github.com.gengyoubo.MPG.event;

import github.com.gengyoubo.MPG.MPG;
import github.com.gengyoubo.MPG.ae2.MPGStorageCellEnhancement;
import github.com.gengyoubo.MPG.item.data.IMPGKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import github.com.gengyoubo.MPG.MPGConfig;
import github.com.gengyoubo.MPG.core.MPGBlockCore;
import github.com.gengyoubo.MPG.core.MPGItemCore;
import github.com.gengyoubo.MPG.item.data.IMPGDoubling;
import github.com.gengyoubo.MPG.trades.MPGBowVillagerTrade;
import github.com.gengyoubo.MPG.trades.MPGSwordGodVillagerTrade;
import github.com.gengyoubo.MPG.util.MPGEntityData;
import github.com.gengyoubo.MPG.util.MPUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = MPG.MODID)
public class EventHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (ModList.get().isLoaded("ae2")) {
            MPGStorageCellEnhancement.addTooltip(stack, event.getToolTip());
        }

        if (!(stack.getItem() instanceof IMPGKey)) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        Iterator<Component> iterator = tooltip.iterator();

        while (iterator.hasNext()) {
            Component component = iterator.next();

            if (!isAttributeModifierLine(component)) {
                continue;
            }

            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }

            return;
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModList.get().isLoaded("ae2")) {
            return;
        }
        if (event.level instanceof ServerLevel serverLevel) {
            MPGStorageCellEnhancement.tick(serverLevel);
        }
    }

    private static boolean isAttributeModifierLine(Component component) {
        if (!(component instanceof MutableComponent mutableComponent)) {
            return false;
        }

        if (!(mutableComponent.getContents() instanceof TranslatableContents contents)) {
            return false;
        }

        return contents.getKey().startsWith("item.modifiers.");
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (MPUtils.isProtectedFromForcedRemoval(event.getPlayer())) {
            event.getEntity().getPersistentData().putBoolean(MPUtils.PROTECTED_ITEM_ENTITY_TAG, true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        Player player = event.getEntity();
        MPUtils.destroyBlocks(player.getMainHandItem(), event.getLevel(), event.getPos(), player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        MPGEntityData.death.remove(event.getEntity());
        MPGEntityData.remove.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        Player player = getDropKiller(event);
        if (player == null) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();
        if (!(mainHandStack.getItem() instanceof IMPGDoubling doublingItem)) {
            return;
        }

        int multiplier = doublingItem.isDoubling(mainHandStack)
                ? MPGConfig.item_drops_doubling_value
                : 1;

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem().copy();
            stack.setCount(stack.getCount() * multiplier);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }

        event.getDrops().clear();
        event.setCanceled(true);
    }

    @Nullable
    private static Player getDropKiller(LivingDropsEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof Player player) {
            return player;
        }

        LivingEntity killCredit = event.getEntity().getKillCredit();
        return killCredit instanceof Player player ? player : null;
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        Player attackingPlayer = event.getAttackingPlayer();
        if (attackingPlayer == null) return;

        ItemStack mainHandItem = attackingPlayer.getMainHandItem();
        if (mainHandItem.getItem() instanceof IMPGDoubling doublingItem) {
            if (doublingItem.isDoubling(mainHandItem)) {
                attackingPlayer.giveExperiencePoints(event.getDroppedExperience() * MPGConfig.experience_drops_doubling_value);
            } else {
                attackingPlayer.giveExperiencePoints(event.getDroppedExperience());
            }
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || !hasManaitaProtectionForFall(player)) return;
        event.setCanceled(true);
        resetPlayerDamageState(player);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || hasManaitaProtection(player)) return;
        event.setCanceled(true);
        resetPlayerDamageState(player);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || hasManaitaProtection(player)) return;
        event.setCanceled(true);
        resetPlayerDamageState(player);
    }

    private static boolean hasManaitaProtection(Player player) {
        return !MPUtils.isManaitaArmor(player) && !MPUtils.isManaita(player);
    }

    private static boolean hasManaitaProtectionForFall(Player player) {
        return MPUtils.isManaitaArmorPart(player) || MPUtils.isManaita(player);
    }

    private static void resetPlayerDamageState(Player player) {
        player.setHealth(player.getMaxHealth());
        player.fallDistance = 0;
        player.hurtTime = 0;
        player.deathTime = 0;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || hasManaitaProtection(player)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.WEAPONSMITH) {
            List<VillagerTrades.ItemListing> tradesTier = event.getTrades().get(5);

            tradesTier.add(new MPGBowVillagerTrade(
                    new ItemStack(MPGBlockCore.CraftingBlockItem.get(), 64),
                    new ItemStack(MPGItemCore.ManaitaBow.get(), 1),
                    1, 0, 1));
            tradesTier.add(new MPGBowVillagerTrade(
                    new ItemStack(MPGBlockCore.FurnaceBlockItem.get(), 64),
                    new ItemStack(MPGItemCore.ManaitaBow.get(), 1),
                    1, 0, 1));
            tradesTier.add(new MPGBowVillagerTrade(
                    new ItemStack(MPGBlockCore.BrewingBlock.get(), 64),
                    new ItemStack(MPGItemCore.ManaitaBow.get(), 1),
                    1, 0, 1));

            tradesTier.add(new MPGSwordGodVillagerTrade(
                    new ItemStack(MPGItemCore.ManaitaBow.get(), 1),
                    new ItemStack(MPGItemCore.ManaitaSwordGod.get(), 1),
                    1, 0, 1));
        }
    }
}
