package github.com.gengyoubo.MPG.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import github.com.gengyoubo.MPG.MPG;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;

public class MPGEnhancedCraftingPattern implements IMolecularAssemblerSupportedPattern {
    private final AEItemKey definition;
    private final ResourceLocation recipeId;
    private final ItemStack[] sparseInputs;
    private final GenericStack[] sparseInputStacks;
    private final GenericStack[] outputs;
    private final IInput[] inputs;
    private final int[] sparseToCompressed = new int[MPGEnhancedCraftingPatternData.CRAFTING_GRID_SIZE];
    private final boolean[] templateOnlyInputs = new boolean[MPGEnhancedCraftingPatternData.CRAFTING_GRID_SIZE];
    private final AEKey[] remainingKeys;
    private final CraftingRecipe recipe;

    public MPGEnhancedCraftingPattern(AEItemKey definition, Level level) {
        this.definition = definition;
        CompoundTag tag = definition.getTag();
        if (tag == null || !MPGEnhancedCraftingPatternData.isEncoded(tag)) {
            throw new IllegalArgumentException("Missing enhanced crafting pattern data");
        }

        this.recipeId = MPGEnhancedCraftingPatternData.readRecipeId(tag);
        this.recipe = findRecipe(level, recipeId)
                .orElseThrow(() -> new IllegalStateException("Missing recipe for enhanced pattern: " + recipeId));
        this.sparseInputs = MPGEnhancedCraftingPatternData.readInputs(tag);
        this.sparseInputStacks = new GenericStack[sparseInputs.length];
        this.remainingKeys = new AEKey[sparseInputs.length];

        ItemStack output = MPGEnhancedCraftingPatternData.readOutput(tag);
        if (output.isEmpty()) {
            throw new IllegalStateException("Enhanced pattern has no output: " + recipeId);
        }
        this.outputs = new GenericStack[]{GenericStack.fromItemStack(output)};

        Arrays.fill(sparseToCompressed, -1);
        CraftingContainer testContainer = createCraftingContainer();
        for (int slot = 0; slot < sparseInputs.length; slot++) {
            ItemStack input = sparseInputs[slot];
            if (!input.isEmpty()) {
                ItemStack single = input.copy();
                single.setCount(1);
                sparseInputs[slot] = single;
                sparseInputStacks[slot] = GenericStack.fromItemStack(single);
                testContainer.setItem(slot, single.copy());
            }
        }

        NonNullList<ItemStack> remainders = recipe.getRemainingItems(testContainer);
        int inputCount = 0;
        for (int slot = 0; slot < sparseInputStacks.length; slot++) {
            GenericStack input = sparseInputStacks[slot];
            if (input == null) {
                continue;
            }
            ItemStack remainder = slot < remainders.size() ? remainders.get(slot) : ItemStack.EMPTY;
            if (!remainder.isEmpty()) {
                AEItemKey remainingKey = AEItemKey.of(remainder);
                if (input.what().equals(remainingKey) && outputsContain(remainingKey)) {
                    templateOnlyInputs[slot] = true;
                    continue;
                }
                remainingKeys[slot] = remainingKey;
            }
            sparseToCompressed[slot] = inputCount++;
        }

        this.inputs = new IInput[inputCount];
        for (int slot = 0; slot < sparseInputStacks.length; slot++) {
            int compressed = sparseToCompressed[slot];
            if (compressed >= 0) {
                this.inputs[compressed] = new PatternInput(sparseInputStacks[slot], remainingKeys[slot]);
            }
        }

        if (!level.isClientSide) {
            MPG.LOGGER.info("[MPG AE2 DEBUG] enhanced pattern created recipe={} inputs={} aeInputs={} output={}",
                    recipeId, describeGrid(sparseInputs), inputs.length, describeStack(output));
        }
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    @Override
    public ItemStack assemble(Container container, Level level) {
        CraftingContainer craftingContainer = createCraftingContainer();
        for (int slot = 0; slot < Math.min(container.getContainerSize(), craftingContainer.getContainerSize()); slot++) {
            craftingContainer.setItem(slot, unwrapCraftingStack(container.getItem(slot)));
        }
        boolean matches = recipe.matches(craftingContainer, level);
        ItemStack result = recipe.assemble(craftingContainer, level.registryAccess());
        MPG.LOGGER.info("[MPG AE2 DEBUG] assemble recipe={} matches={} grid={} result={}",
                recipeId, matches, describeContainer(craftingContainer), describeStack(result));
        return result;
    }

    @Override
    public boolean isItemValid(int slot, AEItemKey key, Level level) {
        if (!isSlotEnabled(slot)) {
            return false;
        }
        GenericStack input = sparseInputStacks[slot];
        return input != null && input.what().equals(key);
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return slot >= 0 && slot < sparseInputStacks.length && sparseInputStacks[slot] != null;
    }

    @Override
    public void fillCraftingGrid(KeyCounter[] availableInputs, CraftingGridAccessor accessor) {
        MPG.LOGGER.info("[MPG AE2 DEBUG] fillCraftingGrid recipe={} availableSlots={} sparseInputs={}",
                recipeId, availableInputs.length, describeGrid(sparseInputs));
        for (int slot = 0; slot < sparseInputStacks.length; slot++) {
            GenericStack input = sparseInputStacks[slot];
            int compressed = sparseToCompressed[slot];
            if (input == null) {
                continue;
            }
            long amount = input.amount();
            accessor.set(slot, toCraftingGridStack(input));
            if (templateOnlyInputs[slot]) {
                MPG.LOGGER.info("[MPG AE2 DEBUG] slot {} set template-only input={} amount={}",
                        slot, input.what(), amount);
                continue;
            }
            if (compressed < 0 || compressed >= availableInputs.length) {
                MPG.LOGGER.info("[MPG AE2 DEBUG] slot {} set input={} amount={} but compressedIndex={} is out of range",
                        slot, input.what(), amount, compressed);
                continue;
            }
            long before = availableInputs[compressed].get(input.what());
            availableInputs[compressed].remove(input.what(), amount);
            long after = availableInputs[compressed].get(input.what());
            MPG.LOGGER.info("[MPG AE2 DEBUG] slot {} set input={} amount={} compressedIndex={} availableBefore={} availableAfter={}",
                    slot, input.what(), amount, compressed, before, after);
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        CraftingContainer craftingContainer = createCraftingContainer();
        for (int slot = 0; slot < Math.min(container.getContainerSize(), craftingContainer.getContainerSize()); slot++) {
            craftingContainer.setItem(slot, unwrapCraftingStack(container.getItem(slot)));
        }

        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(craftingContainer);
        for (int slot = 0; slot < Math.min(templateOnlyInputs.length, remainingItems.size()); slot++) {
            if (templateOnlyInputs[slot]) {
                remainingItems.set(slot, ItemStack.EMPTY);
            }
        }
        MPG.LOGGER.info("[MPG AE2 DEBUG] getRemainingItems recipe={} grid={} remaining={}",
                recipeId, describeContainer(craftingContainer), describeGrid(remainingItems.toArray(new ItemStack[0])));
        return remainingItems;
    }

    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof MPGEnhancedCraftingPattern pattern && definition.equals(pattern.definition);
    }

    public ItemStack getSparseInput(int slot) {
        return slot >= 0 && slot < sparseInputs.length ? sparseInputs[slot] : ItemStack.EMPTY;
    }

    public static CraftingContainer createCraftingContainer() {
        return new TransientCraftingContainer(new DummyMenu(), 3, 3);
    }

    public static Optional<CraftingRecipe> findRecipe(Level level, ResourceLocation recipeId) {
        Optional<? extends Recipe<?>> recipe = level.getRecipeManager().byKey(recipeId);
        if (recipe.isPresent() && recipe.get() instanceof CraftingRecipe craftingRecipe) {
            return Optional.of(craftingRecipe);
        }
        return Optional.empty();
    }

    private boolean outputsContain(AEKey key) {
        for (GenericStack output : outputs) {
            if (output != null && output.what().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String describeContainer(Container container) {
        ItemStack[] stacks = new ItemStack[container.getContainerSize()];
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            stacks[slot] = container.getItem(slot);
        }
        return describeGrid(stacks);
    }

    private static String describeGrid(ItemStack[] stacks) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (int slot = 0; slot < stacks.length; slot++) {
            ItemStack stack = stacks[slot];
            if (stack != null && !stack.isEmpty()) {
                joiner.add(slot + "=" + describeStack(stack));
            }
        }
        return joiner.toString();
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getCount() + "x" + itemId + (stack.hasTag() ? " tag=" + stack.getTag() : "");
    }

    private static ItemStack toCraftingGridStack(GenericStack stack) {
        if (stack.what() instanceof AEItemKey itemKey) {
            ItemStack itemStack = itemKey.toStack();
            itemStack.setCount((int) Math.min(stack.amount(), itemStack.getMaxStackSize()));
            return itemStack;
        }
        return GenericStack.wrapInItemStack(stack.what(), stack.amount());
    }

    private static ItemStack unwrapCraftingStack(ItemStack stack) {
        GenericStack genericStack = GenericStack.unwrapItemStack(stack);
        if (genericStack != null && genericStack.what() instanceof AEItemKey itemKey) {
            ItemStack itemStack = itemKey.toStack();
            itemStack.setCount((int) Math.min(genericStack.amount(), itemStack.getMaxStackSize()));
            return itemStack;
        }
        return stack.copy();
    }

    private static class PatternInput implements IPatternDetails.IInput {
        private final GenericStack[] possibleInputs;
        @Nullable
        private final AEKey remainingKey;

        private PatternInput(GenericStack input, @Nullable AEKey remainingKey) {
            this.possibleInputs = new GenericStack[]{input};
            this.remainingKey = remainingKey;
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possibleInputs;
        }

        @Override
        public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey key, Level level) {
            return possibleInputs[0].what().equals(key);
        }

        @Override
        public AEKey getRemainingKey(AEKey key) {
            return possibleInputs[0].what().equals(key) ? remainingKey : null;
        }
    }

    private static class DummyMenu extends AbstractContainerMenu {
        private DummyMenu() {
            super(null, -1);
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return false;
        }
    }
}
