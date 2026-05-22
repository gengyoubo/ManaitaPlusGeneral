package github.com.gengyoubo.MPG;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import github.com.gengyoubo.MPG.core.*;
import github.com.gengyoubo.MPG.network.Networking;
import github.com.gengyoubo.MPG.util.MPGNBTData;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Mod(MPG.MODID)
public class MPG {
    public static final String MODID = "manaita_plus_general";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTE_TYPE = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER_DEFERRED_REGISTER =  DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB_TYPES = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> MANAITA_PLUS_TAB = CREATIVE_MODE_TAB_TYPES.register("manaita_plus_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MPGBlockCore.CraftingBlockItem.get().getDefaultInstance())
            .title(Component.translatable("itemGroup.ManaitaPlusTab"))
            .displayItems((parameters, output) -> {
                acceptMPGType(MPGBlockCore.CraftingBlockItem.get(), output, 8);
                acceptMPGType(MPGBlockCore.FurnaceBlockItem.get(), output, 8);
                acceptMPGType(MPGBlockCore.BrewingBlockItem.get(), output, 8);
                acceptMPGType(MPGBlockCore.HookBlockItem.get(), output, 7);

                acceptMPGType(MPGItemCore.ManaitaCraftingPortable.get(), output, 8);
                acceptMPGType(MPGItemCore.ManaitaFurnacePortable.get(), output, 8);
                acceptMPGType(MPGItemCore.ManaitaBrewingPortable.get(), output, 8);
                if (ModList.get().isLoaded("curios")) {
                    acceptMPGType(MPGItemCore.ManaitaCraftingRing.get(), output, 8);
                    acceptMPGType(MPGItemCore.ManaitaFurnaceRing.get(), output, 8);
                    acceptMPGType(MPGItemCore.ManaitaBrewingRing.get(), output, 8);
                }
                output.accept(MPGItemCore.ManaitaSwordGod.get());
                output.accept(MPGItemCore.ManaitaSword.get());
                output.accept(MPGItemCore.ManaitaBow.get());
                output.accept(MPGItemCore.ManaitaShovel.get());
                output.accept(MPGItemCore.ManaitaPickaxe.get());
                output.accept(MPGItemCore.ManaitaAxe.get());
                output.accept(MPGItemCore.ManaitaPaxel.get());
                output.accept(MPGItemCore.ManaitaHoe.get());
                output.accept(MPGItemCore.ManaitaShears.get());
                output.accept(MPGItemCore.ManaitaHelmet.get());
                output.accept(MPGItemCore.ManaitaChestplate.get());
                output.accept(MPGItemCore.ManaitaLeggings.get());
                output.accept(MPGItemCore.ManaitaBoots.get());
                output.accept(MPGItemCore.ManaitaHook.get());
                output.accept(MPGItemCore.ManaitaSource.get());
                if (ModList.get().isLoaded("ae2") && MPGItemCore.EnhancedCraftingPattern != null) {
                    output.accept(MPGItemCore.EnhancedCraftingPattern.get());
                }
            }).build());

    public MPG() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onFMLCommonSetup);

        // Force-load all registry holder classes before RegisterEvent starts firing.
        MPGBlockCore.init();
        MPGItemCore.init();
        MPGEntityCore.init();
        MPGBlockEntityCore.init();
        MPGMenuCore.init();
        MPGRecipeSerializerCore.init();
        MPGAttributeCore.init();

        MPGSynchedDataCore.init();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ATTRIBUTE_TYPE.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TAB_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        RECIPE_SERIALIZER_DEFERRED_REGISTER.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MPGConfig.SPEC);
    }



    private void onFMLCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(Networking::registerMessage);
    }

    public static String getModVersion() {
        return ModList.get()
                .getModContainerById(MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("UNKNOWN");
    }

    private static void acceptMPGType(Item item, CreativeModeTab.Output output, int maxType) {
        for (int type = 0; type <= maxType; type++) {
            ItemStack stack = new ItemStack(item);
            stack.getOrCreateTag().putInt(MPGNBTData.ItemType, type);
            output.accept(stack);
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    private static class MPGUpdateChecker {
        private static final String VERSION_URL =
                "https://raw.githubusercontent.com/gengyoubo/ManaitaPlusGeneral/refs/heads/forge1.20.1/mcmodsrepo/github/com/gengyoubo/ManaitaPlusGeneral/maven-metadata.xml";

        private static CompletableFuture<String> latestVersionFuture;

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            getLatestVersionFuture()
                    .thenAccept(latestVersion -> server.execute(() -> {
                        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(player.getUUID());
                        if (onlinePlayer != null) {
                            reportUpdate(onlinePlayer, latestVersion);
                        }
                    }))
                    .exceptionally(exception -> {
                        LOGGER.error("Failed to check mod updates", exception);
                        return null;
                    });
        }

        private static synchronized CompletableFuture<String> getLatestVersionFuture() {
            if (latestVersionFuture == null || latestVersionFuture.isCompletedExceptionally()) {
                latestVersionFuture = CompletableFuture.supplyAsync(MPGUpdateChecker::loadLatestVersion);
            }
            return latestVersionFuture;
        }

        private static String loadLatestVersion() {
            try {
                URL url = new URL(VERSION_URL);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (InputStream inputStream = connection.getInputStream()) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    Document document = builder.parse(inputStream);

                    NodeList latestList = document.getElementsByTagName("latest");

                    if (latestList.getLength() <= 0) {
                        return null;
                    }

                    return latestList.item(0).getTextContent().trim();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load latest mod version", e);
            }
        }

        private static void reportUpdate(ServerPlayer player, String latestVersion) {
            if (latestVersion == null || latestVersion.isBlank()) {
                return;
            }

            String currentVersion = MPG.getModVersion();
            if (MPGLatestVersion.isRemoteNewer(latestVersion, currentVersion)) {
                Component message = Component.translatable(
                        "message.manaita_plus_general.update.available",
                        currentVersion,
                        latestVersion
                );
                LOGGER.warn("ManaitaPlusGeneral is out of date. Current: {}, Latest: {}", currentVersion, latestVersion);
                player.sendSystemMessage(message);
            } else {
                Component message = Component.translatable(
                        "message.manaita_plus_general.update.current",
                        currentVersion,
                        latestVersion
                );
                LOGGER.info("ManaitaPlusGeneral is up to date. Current: {}, Latest: {}", currentVersion, latestVersion);
                player.sendSystemMessage(message);
            }
        }
        private static class MPGLatestVersion {
            private static boolean isRemoteNewer(String latestVersion, String currentVersion) {
                return compareVersions(latestVersion, currentVersion) > 0;
            }

            private static int compareVersions(String left, String right) {
                String[] leftParts = left.split("\\.");
                String[] rightParts = right.split("\\.");

                int length = Math.max(leftParts.length, rightParts.length);
                for (int i = 0; i < length; i++) {
                    int leftValue = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
                    int rightValue = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;

                    if (leftValue != rightValue) {
                        return Integer.compare(leftValue, rightValue);
                    }
                }

                return 0;
            }

            private static int parseVersionPart(String part) {
                try {
                    return Integer.parseInt(part);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }

        }
    }
}
