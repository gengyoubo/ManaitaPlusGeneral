package github.com.gengyoubo.MPG.util;

import github.com.gengyoubo.MPG.MPG;
import github.com.gengyoubo.MPG.item.armor.MPGArmor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.*;
import org.jetbrains.annotations.NotNull;
import github.com.gengyoubo.MPG.MPGConfig;
import github.com.gengyoubo.MPG.item.data.IMPGDestroy;
import github.com.gengyoubo.MPG.network.Networking;
import github.com.gengyoubo.MPG.network.server.DestroyBlockPacket;
import github.com.gengyoubo.MPG.util.wrapper.EntitiesWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class MPUtils {
    public static final String PROTECTED_ITEM_ENTITY_TAG = "ManaitaPlusGeneralProtectedItem";

    public static Entity getEntity(Entity entity) {
        if (entity instanceof Arrow arrow)
            return arrow.getOwner();
        if (entity instanceof DragonFireball fireball)
            return fireball.getOwner();
        return entity;
    }

    private static final ThreadLocal<EntitiesWrapper> ENTITY_CACHE =
            ThreadLocal.withInitial(EntitiesWrapper::new);

    public static void godKill(Player player,boolean remove,boolean shiftKeyDown) {
        Level level = player.level();
        if (level instanceof ServerLevel server) {
            List<Entity> tntities = new ArrayList<>();
            EntitiesWrapper wrapper = ENTITY_CACHE.get();
            wrapper.addIterable(server.getAllEntities());
            wrapper.addIterable(server.getPartEntities());
            Entity[] entities = wrapper.getEntities();
            Entity nearestTarget = null;
            double nearestDistanceSqr = Double.MAX_VALUE;
            for (int i = 0; i < wrapper.size(); i++) {
                Entity entity = entities[i];
                Entity target = getEntity(entity);
                if (target == null || target instanceof ItemEntity || target instanceof ExperienceOrb) {
                    tntities.add(target);
                    continue;
                }
                if (!isGodSwordTarget(player, target, shiftKeyDown)) continue;
                double distanceSqr = player.distanceToSqr(target);
                if (distanceSqr < nearestDistanceSqr) {
                    nearestTarget = target;
                    nearestDistanceSqr = distanceSqr;
                }
                attack(target, player,remove);
            }
            server.getPartEntities().clear();
            wrapper.reset();
            for (Entity tntity : tntities) {
                if (tntity instanceof ItemEntity item) {
                    item.setNoPickUpDelay();
                    item.playerTouch(player);
                    continue;
                }
                if (tntity instanceof ExperienceOrb xp) {
                    player.takeXpDelay = 0;
                    xp.playerTouch(player);
                }
            }
            attackNearestGodSwordTarget(player, nearestTarget, remove);
        }
    }

    private static boolean isGodSwordTarget(Player player, Entity target, boolean shiftKeyDown) {
        if (target == player || target instanceof ItemEntity || target instanceof ExperienceOrb || isProtectedFromForcedRemoval(target)) {
            return false;
        }
        return shiftKeyDown || target.getType().getCategory() == MobCategory.MONSTER;
    }

    private static void attackNearestGodSwordTarget(Player player, Entity target, boolean remove) {
        if (target == null || !target.isAlive() || isProtectedFromForcedRemoval(target)) {
            return;
        }

        if (remove) {
            removeOnServer(target);
            return;
        }

        if (target instanceof LivingEntity living) {
            living.hurt(living.damageSources().genericKill(), Float.MAX_VALUE);
            if (living.isAlive()) {
                living.die(living.damageSources().genericKill());
            }
        }
    }

    public static void attack(Entity target, Player player, boolean remove) {
        if (isProtectedFromForcedRemoval(target)) {
            return;
        }

        if (remove) {
            MPGEntityData.remove.add(target);
            if (player.isShiftKeyDown() && !target.getClass().getName().startsWith("net.minecraft")) {
                Class<?> wrapper = MPClassLoaderFactory.createWrapper(target.getClass());
                if (wrapper != null) {
                    Helper.setFieldValue(target, wrapper);
                }
            }
        }
        MPGEntityData.death.add(target);
        if (remove) {
            removeOnServer(target);
        } else {
            if (target instanceof LivingEntity living) {
                living.hurt(living.damageSources().playerAttack(player), Float.MAX_VALUE);
                if (living.isAlive()) {
                    living.hurt(living.damageSources().genericKill(), Float.MAX_VALUE);
                }

                if (living.isAlive()) {
                    living.handleEntityEvent((byte) 2);
                    living.handleEntityEvent((byte) 47);
                    living.handleEntityEvent((byte) 48);
                    living.handleEntityEvent((byte) 49);
                    living.handleEntityEvent((byte) 50);
                    living.handleEntityEvent((byte) 51);
                    living.handleEntityEvent((byte) 52);

                    living.die(living.damageSources().genericKill());
                }
            }
        }
    }



    public static void removeOnServer(Entity target) {
        if (isProtectedFromForcedRemoval(target)) {
            return;
        }

        if (target.level() instanceof ServerLevel serverLevel) {
            Int2ObjectMap<Entity> byId = serverLevel.entityManager.visibleEntityStorage.byId;
            byId.remove(target.getId());
            byId.int2ObjectEntrySet().removeIf(next -> next.getValue() == target);

            Map<UUID, Entity> byUuid = serverLevel.entityManager.visibleEntityStorage.byUuid;
            byUuid.remove(target.getUUID());
            byUuid.entrySet().removeIf(next -> next.getValue() == target);
            serverLevel.entityManager.knownUuids.remove(target.getUUID());

            LevelEntityGetter<Entity> getter = serverLevel.entityManager.entityGetter;

            if (getter instanceof LevelEntityGetterAdapter<Entity> adapter) {
                adapter.visibleEntities.byId.remove(target.getId());
                adapter.visibleEntities.byUuid.remove(target.getUUID());
            }
            serverLevel.entityTickList.remove(target);

            long sectionPos = SectionPos.asLong(target.blockPosition());
            EntitySectionStorage<Entity> sectionStorage = serverLevel.entityManager.sectionStorage;
            sectionStorage.getExistingSectionPositionsInChunk(sectionPos).forEach(sectionPos1 -> {
                EntitySection<Entity> section = sectionStorage.sections.get(sectionPos1);
                if (section == null) return;
                section.storage.remove(target);
                section.storage.allInstances.remove(target);
            });
            EntitySection<Entity> entitySection = sectionStorage.getOrCreateSection(sectionPos);
            target.setLevelCallback(new EntityInLevelCallback() {
                public void onMove() {
                }

                public void onRemove(Entity.@NotNull RemovalReason removalReason) {
                    if (!entitySection.remove(target)) {
                        serverLevel.entityManager.stopTicking(target);
                        serverLevel.entityManager.stopTracking(target);
                        serverLevel.entityManager.callbacks.onDestroyed(target);
                    }
                }
            });
            target.setRemoved(Entity.RemovalReason.DISCARDED);
            entitySection.remove(target);
            entitySection.storage.allInstances.remove(target);

            serverLevel.getChunkSource().removeEntity(target);
        }
    }


    public static boolean isManaita(Player player) {
        return MPGEntityData.manaita.accept(player);
    }

    public static boolean isProtectedFromForcedRemoval(Entity entity) {
        if (entity instanceof Player player) {
            return isManaitaArmor(player) || isManaita(player);
        }

        if (entity instanceof ItemEntity itemEntity) {
            return isProtectedItemEntity(itemEntity);
        }

        return false;
    }

    public static boolean isProtectedItemEntity(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        return !stack.isEmpty()
                && (isManaitaItem(stack) || itemEntity.getPersistentData().getBoolean(PROTECTED_ITEM_ENTITY_TAG));
    }

    public static boolean isManaitaItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && MPG.MODID.equals(id.getNamespace());
    }

    public static boolean addToEmptyInventorySlot(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        int slot = inventory.getFreeSlot();
        if (slot < 0) {
            stack.setCount(0);
            return false;
        }

        inventory.setItem(slot, stack.copyAndClear());
        inventory.getItem(slot).setPopTime(5);
        return true;
    }


    public static void popResource(Level p_49841_, BlockPos p_49842_, ItemStack p_49843_) {
        double d0 = (double) EntityType.ITEM.getHeight() / 2.0D;
        double d1 = (double)p_49842_.getX() + 0.5D + Mth.nextDouble(p_49841_.random, -0.25D, 0.25D);
        double d2 = (double)p_49842_.getY() + 0.5D + Mth.nextDouble(p_49841_.random, -0.25D, 0.25D) - d0;
        double d3 = (double)p_49842_.getZ() + 0.5D + Mth.nextDouble(p_49841_.random, -0.25D, 0.25D);
        popResource(p_49841_, () -> new ItemEntity(p_49841_, d1, d2, d3, p_49843_), p_49843_);
    }


    private static void popResource(Level p_152441_, Supplier<ItemEntity> p_152442_, ItemStack p_152443_) {
        if (!p_152441_.isClientSide && !p_152443_.isEmpty()) {
            ItemEntity itementity = p_152442_.get();
            itementity.setDefaultPickUpDelay();
            p_152441_.addFreshEntity(itementity);
        }
    }

    public static boolean isManaitaArmor(Player player) {
        for (ItemStack itemStack : player.getInventory().armor) {
            if (itemStack == null || !(itemStack.getItem() instanceof MPGArmor))
                return false;
        }
        return true;
    }

    public static boolean isManaitaArmorPart(Player player) {
        for (ItemStack itemStack : player.getInventory().armor) {
            if (itemStack != null && itemStack.getItem() instanceof MPGArmor)
                return true;
        }
        return false;
    }

    public static void destroyBlocks(ItemStack stack, Level level, BlockPos blockPos, Player player) {
        if (stack.getItem() instanceof IMPGDestroy des) {
            int range = des.getRange(stack) >> 1;
            boolean doubling = stack.getTag() != null && stack.getTag().getBoolean("Doubling");
            if (range == 0) {
                destroyBlock(stack, level, blockPos, player,doubling);
                return;
            }
            if (level instanceof ServerLevel serverLevel) {
                Networking.sendToTrackBySeen(serverLevel,player,new DestroyBlockPacket(blockPos,range,stack.getItem()));
                int xM = blockPos.getX() + range;
                int yM = blockPos.getY() + range;
                int zM = blockPos.getZ() + range;
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
                boolean isDrop = !player.getAbilities().instabuild;
                if (!isDrop && !MPGConfig.creative_range_destroy_value) return;
                for (int x = blockPos.getX() - range; x <= xM; x++) {
                    for (int y = blockPos.getY() - range; y <= yM; y++) {
                        for (int z = blockPos.getZ() - range; z <= zM; z++) {
                            BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                            if (des.accept(blockState))
                                continue;

                            Block block = blockState.getBlock();
                            BlockEntity blockEntity = serverLevel.getBlockEntity(mutableBlockPos);

                            boolean removed = setBlock(level, mutableBlockPos, level.getFluidState(mutableBlockPos).createLegacyBlock(), 2);
                            if (removed)
                                block.destroy(level, mutableBlockPos, blockState);
                            player.awardStat(Stats.BLOCK_MINED.get(block));
                            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

                            if (isDrop) {
                                player.causeFoodExhaustion(0.005F);
                                List<ItemStack> drops = Block.getDrops(blockState, serverLevel, mutableBlockPos, blockEntity, player, stack);
                                if (drops.isEmpty())
                                    popResource(serverLevel, mutableBlockPos, new ItemStack(block, doubling ? MPGConfig.destroy_doubling_value : 1));
                                else
                                    drops.forEach((p_49859_) -> {
                                        if (doubling)
                                            p_49859_.setCount(p_49859_.getCount() * MPGConfig.destroy_doubling_value);
                                        popResource(serverLevel, mutableBlockPos, p_49859_);
                                    });
                                int exp = blockState.getExpDrop(serverLevel, serverLevel.random, mutableBlockPos, stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE), stack.getEnchantmentLevel(Enchantments.SILK_TOUCH));
                                if (doubling)
                                    exp *= MPGConfig.destroy_doubling_value;
                                block.popExperience(serverLevel, mutableBlockPos, exp);
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean setBlock(Level level,BlockPos p_46605_, BlockState p_46606_,int p_46607_ ) {
        if (level.isOutsideBuildHeight(p_46605_)) {
            return false;
        } else if (!level.isClientSide && level.isDebug()) {
            return false;
        } else {
            LevelChunk levelchunk = level.getChunkAt(p_46605_);

            p_46605_ = p_46605_.immutable(); // Forge - prevent mutable BlockPos leaks
            net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;
            if (level.captureBlockSnapshots && !level.isClientSide) {
                blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(), level, p_46605_, p_46607_);
                level.capturedBlockSnapshots.add(blockSnapshot);
            }

            BlockState blockstate = levelchunk.setBlockState(p_46605_, p_46606_, false);
            if (blockstate == null) {
                if (blockSnapshot != null) level.capturedBlockSnapshots.remove(blockSnapshot);
                return false;
            } else {
                if (blockSnapshot == null) { // Don't notify clients or update physics while capturing blockstates
                    level.markAndNotifyBlock(p_46605_, levelchunk, blockstate, p_46606_, p_46607_, 512);
                }

                return true;
            }
        }
    }

    public static void destroyBlock(ItemStack stack, Level level, BlockPos pos, Player player, boolean doubling) {
        if (stack.getItem() instanceof IMPGDestroy des) {
            BlockState blockState = level.getBlockState(pos);
            if (des.accept(blockState))
                return;

            Block block = blockState.getBlock();
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                if (serverLevel.getBlockEntity(pos) == null) {
                    serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, serverLevel.getFluidState(pos).createLegacyBlock()));
                }

                boolean removed = blockState.onDestroyedByPlayer(level, pos, player, false, level.getFluidState(pos));
                if (removed)
                    block.destroy(level, pos, blockState);

                player.awardStat(Stats.BLOCK_MINED.get(block));
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

                boolean isDrop = !player.getAbilities().instabuild;
                if (isDrop) {
                    player.causeFoodExhaustion(0.005F);
                    List<ItemStack> drops = Block.getDrops(blockState, serverLevel, pos, blockEntity, player, stack);
                    if (drops.isEmpty())
                        popResource(serverLevel, pos, new ItemStack(block, doubling ? 4 : 1));
                    else
                        drops.forEach((p_49859_) -> {
                            if (doubling)
                                p_49859_.setCount(p_49859_.getCount() * 4);
                            popResource(serverLevel, pos, p_49859_);
                        });
                    int exp = blockState.getExpDrop(serverLevel, serverLevel.random, pos, stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE), stack.getEnchantmentLevel(Enchantments.SILK_TOUCH));
                    if (doubling)
                        exp *= 4;
                    block.popExperience(serverLevel, pos, exp);
                }
            }
        }
    }

    public static String getTypes(int i) {
        if (i == 1)
            return "wooden.";
        if (i == 2)
            return "stone.";
        if (i == 3)
            return "iron.";
        if (i == 4)
            return "gold.";
        if (i == 5)
            return "diamond.";
        if (i == 6)
            return "emerald.";
        if (i == 7)
            return "redstone.";
        if (i == 8)
            return "netherite.";
        return "";
    }

    public static String getTypes1(int i) {
        if (i == 2)
            return "stone";
        if (i == 3)
            return "iron";
        if (i == 4)
            return "gold";
        if (i == 5)
            return "diamond";
        if (i == 6)
            return "emerald";
        if (i == 7)
            return "redstone";
        if (i == 8)
            return "netherite";
        return "wooden";
    }

    public static void chat(Player player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, false);
        }
    }
}
