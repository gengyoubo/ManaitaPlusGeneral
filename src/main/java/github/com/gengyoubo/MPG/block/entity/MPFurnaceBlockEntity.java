package github.com.gengyoubo.MPG.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import github.com.gengyoubo.MPG.MPGConfig;
import github.com.gengyoubo.MPG.core.MPGBlockEntityCore;
import github.com.gengyoubo.MPG.menu.MPGFurnaceMenu;

import javax.annotation.Nullable;
public class MPFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2};
    private static final int[] SLOTS_FOR_SIDES = new int[]{0};
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<Container, ? extends AbstractCookingRecipe> quickCheck;

    public MPFurnaceBlockEntity(BlockPos p_155545_, BlockState p_155546_) {
        super(MPGBlockEntityCore.FURNACE_BLOCK_ENTITY.get(), p_155545_, p_155546_, RecipeType.SMELTING);
        this.quickCheck = RecipeManager.createCheck(RecipeType.SMELTING);
    }

    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.furnace_manaita");
    }

    protected @NotNull AbstractContainerMenu createMenu(int p_59293_, @NotNull Inventory p_59294_) {
        return new MPGFurnaceMenu(p_59293_, p_59294_, this, this.dataAccess);
    }

    @Override
    public int getMaxStackSize() {
        return Integer.MAX_VALUE;
    }

    public void load(@NotNull CompoundTag p_155025_) {
        super.load(p_155025_);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(p_155025_, this.items);
        CompoundTag compoundtag = p_155025_.getCompound("RecipesUsed");

        for(String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(new ResourceLocation(s), compoundtag.getInt(s));
        }

    }

    protected void saveAdditional(@NotNull CompoundTag p_187452_) {
        super.saveAdditional(p_187452_);
        ContainerHelper.saveAllItems(p_187452_, this.items);
        CompoundTag compoundtag = new CompoundTag();
        this.recipesUsed.forEach((p_187449_, p_187450_) -> compoundtag.putInt(p_187449_.toString(), p_187450_));
        p_187452_.put("RecipesUsed", compoundtag);
    }


    public static void serverTick(Level p_155014_, BlockPos p_155015_, BlockState p_155016_, MPFurnaceBlockEntity entity) {
        boolean flag1 = false;

        if (!entity.items.get(0).isEmpty()) {
            Recipe<?> recipe = entity.quickCheck.getRecipeFor(entity, p_155014_).orElse(null);
            while (entity.canBurn(p_155014_.registryAccess(), recipe, entity.items)) {
                if (entity.burn(p_155014_.registryAccess(), recipe, entity.items)) {
                    entity.setRecipeUsed(recipe);
                }
                flag1 = true;
            }
        }


        if (flag1) {
            setChanged(p_155014_, p_155015_, p_155016_);
        }
    }

    private boolean canBurn(RegistryAccess p_266924_, @Nullable Recipe<?> p_155006_, NonNullList<ItemStack> p_155007_) {
        return MPGLogicHelper.canBurn(p_266924_, p_155006_, p_155007_, this);
    }

    private boolean burn(RegistryAccess registryAccess,
                         @Nullable Recipe<?> recipe,
                         NonNullList<ItemStack> items) {
        if (recipe == null || !this.canBurn(registryAccess, recipe, items)) return false;
        ItemStack input = items.get(0);
        ItemStack result = MPGLogicHelper.assembleResult(recipe, this, registryAccess);
        int outputCount = result.getCount() * MPGConfig.furnace_doubling_value;

        ItemStack output = items.get(2);

        if (output.isEmpty()) {
            ItemStack resultCopy = result.copy();
            resultCopy.setCount(outputCount);
            items.set(2, resultCopy);
        } else {
            output.grow(outputCount);
        }

        input.shrink(1);
        return true;
    }

    protected int getBurnDuration(@NotNull ItemStack p_58343_) {
        return 0;
    }

    public int @NotNull [] getSlotsForFace(@NotNull Direction p_58363_) {
        if (p_58363_ == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return p_58363_ == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    public boolean canPlaceItemThroughFace(int p_58336_, @NotNull ItemStack p_58337_, @Nullable Direction p_58338_) {
        return this.canPlaceItem(p_58336_, p_58337_);
    }

    public boolean canTakeItemThroughFace(int p_58377_, @NotNull ItemStack p_58378_, @NotNull Direction p_58379_) {
        return p_58377_ == 2;
    }

    public void setItem(int p_58333_, ItemStack p_58334_) {
        ItemStack itemstack = this.items.get(p_58333_);
        boolean flag = !p_58334_.isEmpty() && ItemStack.isSameItemSameTags(itemstack, p_58334_);
        this.items.set(p_58333_, p_58334_);

        if (p_58333_ == 0 && !flag) {
            this.setChanged();
        }

    }

    public boolean stillValid(@NotNull Player p_58340_) {
        return true;
    }

    public boolean canPlaceItem(int p_58389_, @NotNull ItemStack p_58390_) {
        return p_58389_ == 0;
    }

    public void setRecipeUsed(@Nullable Recipe<?> p_58345_) {
        if (p_58345_ != null) {
            ResourceLocation resourcelocation = p_58345_.getId();
            this.recipesUsed.addTo(resourcelocation, 1);
        }

    }

    public void awardUsedRecipesAndPopExperience(@NotNull ServerPlayer p_155004_) {
        MPGLogicHelper.awardUsedRecipesAndPopExperience(p_155004_, this.items, this.recipesUsed);
    }

    public @NotNull java.util.List<Recipe<?>> getRecipesToAwardAndPopExperience(@NotNull ServerLevel p_154996_, @NotNull Vec3 p_154997_) {
        return MPGLogicHelper.getRecipesToAwardAndPopExperience(p_154996_, p_154997_, this.recipesUsed);
    }

    net.minecraftforge.common.util.LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
            MPGItemHandlerHelper.createSidedHandlers(this);

    @Override
    public <T> net.minecraftforge.common.util.@NotNull LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.@NotNull Capability<T> capability, @Nullable Direction facing) {
        net.minecraftforge.common.util.LazyOptional<T> itemHandlerCap = MPGItemHandlerHelper.getSidedItemHandlerCapability(this.remove, capability, facing, handlers);
        if (itemHandlerCap.isPresent()) {
            return itemHandlerCap;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        MPGItemHandlerHelper.invalidateHandlers(handlers);
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.handlers = MPGItemHandlerHelper.createSidedHandlers(this);
    }
}
