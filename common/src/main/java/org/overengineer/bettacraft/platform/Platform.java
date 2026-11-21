package org.overengineer.bettacraft.platform;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem;
import org.overengineer.bettacraft.platform.item.IItemHandler;

/**
 * M52: everything the common code needs from the mod loader, one implementation per loader
 * ({@code org.overengineer.bettacraft.neoforge.NeoForgePlatform}, {@code org.overengineer.bettacraft.fabric.FabricPlatform}),
 * found through {@link ServiceLoader} ({@code META-INF/services/org.overengineer.bettacraft.platform.Platform} of the loader
 * module). Call sites use {@link #get()}.
 */
public interface Platform {
    static Platform get() {
        if (Instance.PLATFORM == null) throw new IllegalStateException("Bettacraft: no platform implementation (the loader module is missing)");
        return Instance.PLATFORM;
    }

    /** Whether a loader is there (false in the plain JUnit tests, which run without Minecraft). */
    static boolean available() {
        return Instance.PLATFORM != null;
    }

    /** "neoforge" or "fabric". */
    String loader();

    boolean isModLoaded(String modId);

    /** The physical client (also for the integrated server's thread). */
    boolean isClient();

    /** A development run (gradlew run*), not a release game. */
    boolean isDevelopment();

    Path configDir();

    Path gameDir();

    @Nullable
    MinecraftServer currentServer();

    /** The entity's data other mods and we keep across saves (NeoForge's persistent data, saved as "NeoForgeData"). */
    CompoundTag persistentData(Entity entity);

    /** Fake players of other mods (machines that use items). */
    boolean isFakePlayer(@Nullable Player player);

    /** The version of a loaded mod, "?" if it is not there. */
    String modVersion(String modId);

    /** The translation key of a fluid (NeoForge: its fluid type's). */
    String fluidDescriptionId(Fluid fluid);

    // ---- networking (payload types are registered by the loader from network.ModNetwork)

    void sendToServer(CustomPacketPayload payload);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload);

    void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload);

    void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload);

    void sendToAllPlayers(CustomPacketPayload payload);

    /** Opens a menu whose client side reads {@code extra} (NeoForge's openMenu with a buffer writer). */
    void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra);

    /** Opens a menu whose client side reads a block position (NeoForge's openMenu with a position). */
    /**
     * M52e: the folder of a world that holds its own copy of the server config ({@code serverconfig}; NeoForge 26.3 /
     * FML 12: {@code syncedconfig}, the global {@code config/bettacraft-server.toml} is the default there).
     */
    default String worldConfigFolder() {
        return "serverconfig";
    }

    default void openMenu(ServerPlayer player, MenuProvider provider, net.minecraft.core.BlockPos pos) {
        openMenu(player, provider, buf -> buf.writeBlockPos(pos));
    }

    // ---- other mods' blocks and items (capabilities / transfer API)

    @Nullable
    IItemHandler itemHandler(Level level, BlockPos pos, @Nullable Direction side);

    @Nullable
    IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side);

    @Nullable
    IEnergyStorage energyStorage(Level level, BlockPos pos, @Nullable Direction side);

    /** The energy storage a block would offer with this state and block entity (e.g. a block on a contraption). */
    @Nullable
    IEnergyStorage energyStorage(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
            @Nullable net.minecraft.world.level.block.entity.BlockEntity be, @Nullable Direction side);

    /** The fluid of an item (a bucket or another mod's tank item); works on a copy of one item. */
    Optional<IFluidHandlerItem> fluidHandler(ItemStack stack);

    /** A player uses the fluid item in the hand on the tank at {@code pos} (fill or empty it, like a bucket). */
    boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side);

    /** A player uses the fluid item in the hand on {@code handler}. */
    boolean interactWithFluidHandler(Player player, InteractionHand hand, IFluidHandler handler);

    /** Empties a fluid container item into {@code dest}; the result holds the emptied container (empty: nothing moved). */
    FluidActionResult tryEmptyContainer(ItemStack container, IFluidHandler dest, int maxAmount, @Nullable Player player, boolean doDrain);

    /** Fills a fluid container item from {@code source}; the result holds the filled container (empty: nothing moved). */
    FluidActionResult tryFillContainer(ItemStack container, IFluidHandler source, int maxAmount, @Nullable Player player, boolean doFill);

    /** The result of a container interaction: success and the container after it. */
    record FluidActionResult(boolean success, ItemStack result) {
        public static final FluidActionResult FAILURE = new FluidActionResult(false, ItemStack.EMPTY);

        public boolean isSuccess() {
            return success;
        }

        public ItemStack getResult() {
            return result;
        }
    }

    // ---- hooks of the loader's extensions of vanilla classes (NeoForge: its extension methods; Fabric: the vanilla equivalent)

    /** The block entity's model data changed (NeoForge: requestModelDataUpdate; elsewhere a re-render of its block). */
    void requestModelDataUpdate(net.minecraft.world.level.block.entity.BlockEntity be);

    /** A render box that is never culled (NeoForge: AABB.INFINITE). */
    net.minecraft.world.phys.AABB infiniteRenderBox();

    /** The entity is in its level's entity lists (NeoForge: isAddedToLevel). */
    boolean isAddedToLevel(Entity entity);

    /** The player's client accepts this payload type (has Bettacraft). */
    boolean hasChannel(ServerPlayer player, CustomPacketPayload.Type<?> type);

    /** What stays in the crafting grid when this stack is used (a bucket, a bottle). */
    ItemStack craftingRemainder(ItemStack stack);

    /** The food of a stack for this eater, or null. */
    @Nullable
    net.minecraft.world.food.FoodProperties foodProperties(ItemStack stack, @Nullable net.minecraft.world.entity.LivingEntity eater);

    /** Other mods should look up the capabilities at this position again. */
    void invalidateCapabilities(Level level, BlockPos pos);

    /** The auxiliary block light at a position (light of a block entity's cover), 0 without. */
    int auxLight(net.minecraft.world.level.BlockGetter level, BlockPos pos);

    /** Sets the auxiliary block light at a position (where the loader has one). */
    void setAuxLight(Level level, BlockPos pos, int light);

    /** Whether the player can harvest the block state (NeoForge: the state's canHarvestBlock with its events). */
    boolean canHarvestBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player);

    /** The default harvest check of a block (NeoForge: the harvest check event; vanilla: the right tool). */
    boolean defaultHarvestCheck(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player);

    float explosionResistance(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.level.Explosion explosion);

    net.minecraft.world.level.block.SoundType soundType(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level,
            BlockPos pos, @Nullable Entity entity);

    /** The spawn position check of a mob spawned by us (NeoForge: with its spawn events). */
    boolean checkSpawnPosition(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason type);

    @Nullable
    net.minecraft.world.entity.SpawnGroupData finalizeMobSpawn(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason type, @Nullable net.minecraft.world.entity.SpawnGroupData data);

    /** Empties a bucket-like item at a position (NeoForge: with the container stack). */
    boolean emptyContents(net.minecraft.world.item.DispensibleContainerItem item, @Nullable Player player, Level level, BlockPos pos,
            @Nullable net.minecraft.world.phys.BlockHitResult hit, @Nullable ItemStack container);

    /** A spawn egg for one of our entity types (resolved lazily where the loader allows it). */
    net.minecraft.world.item.SpawnEggItem spawnEgg(java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int background, int highlight, net.minecraft.world.item.Item.Properties props);

    /** One of our fluids, the source or the flowing one (NeoForge: a BaseFlowingFluid with its fluid type). */
    net.minecraft.world.level.material.FlowingFluid fluid(org.overengineer.bettacraft.platform.fluid.FluidSpec spec, boolean source);

    /** A builder of a creative tab (NeoForge places the tabs itself; vanilla needs a row and column). */
    net.minecraft.world.item.CreativeModeTab.Builder creativeTabBuilder();

    /** The downfall of a biome after the loader's biome changes (NeoForge: the modified climate settings). */
    float biomeDownfall(net.minecraft.world.level.biome.Biome biome);

    /**
     * M53c: whether a chunk carries data of the loader's attachment system (NeoForge data attachments, Fabric data
     * attachments): such a chunk is always saved, even when no block is in it.
     */
    boolean chunkHasAttachments(net.minecraft.world.level.chunk.ChunkAccess chunk);

    /** The light a block state gives at a position (NeoForge: the position-aware light emission). */
    int lightEmission(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos);


    /** A menu factory that reads the extra data of {@link #openMenu}. */
    @FunctionalInterface
    interface MenuFactory<T extends net.minecraft.world.inventory.AbstractContainerMenu> {
        T create(int windowId, net.minecraft.world.entity.player.Inventory inv, RegistryFriendlyByteBuf data);
    }

    <T extends net.minecraft.world.inventory.AbstractContainerMenu> net.minecraft.world.inventory.MenuType<T> menuType(MenuFactory<T> factory);

    /** The fluid of a stack for display (NeoForge: the fluid type's name). */
    default net.minecraft.network.chat.Component fluidName(FluidStack stack) {
        return net.minecraft.network.chat.Component.translatable(fluidDescriptionId(stack.getFluid()));
    }

    final class Instance {
        private static final Platform PLATFORM = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst().orElse(null);

        private Instance() {}
    }
}
