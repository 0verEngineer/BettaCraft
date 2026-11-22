package org.overengineer.bettacraft.neoforge;

import java.nio.file.Path;
import java.util.Optional;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.Platform;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem;
import org.overengineer.bettacraft.platform.item.IItemHandler;

/** M52: the platform layer on NeoForge (found through META-INF/services). */
public final class NeoForgePlatform implements Platform {
    @Override
    public String loader() {
        return "neoforge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.getDist().isClient();
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public @Nullable MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public CompoundTag persistentData(Entity entity) {
        return entity.getPersistentData();
    }

    @Override
    public boolean isFakePlayer(@Nullable Player player) {
        return player instanceof FakePlayer;
    }

    @Override
    public String fluidDescriptionId(Fluid fluid) {
        return fluid.getFluidType().getDescriptionId();
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    @Override
    public void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk, payload);
    }

    @Override
    public void sendToAllPlayers(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
        player.openMenu(provider, extra::accept);
    }

    @Override
    public @Nullable IItemHandler itemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return org.overengineer.bettacraft.neoforge.transfer.FromResources.items(level.getCapability(Capabilities.Item.BLOCK, pos, side));
    }

    @Override
    public @Nullable IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return org.overengineer.bettacraft.neoforge.transfer.FromResources.fluids(level.getCapability(Capabilities.Fluid.BLOCK, pos, side));
    }

    @Override
    public @Nullable IEnergyStorage energyStorage(Level level, BlockPos pos, @Nullable Direction side) {
        return org.overengineer.bettacraft.neoforge.transfer.FromResources.energy(level.getCapability(Capabilities.Energy.BLOCK, pos, side));
    }

    @Override
    public @Nullable IEnergyStorage energyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side) {
        return org.overengineer.bettacraft.neoforge.transfer.FromResources.energy(Capabilities.Energy.BLOCK.getCapability(level, pos, state, be, side));
    }

    @Override
    public Optional<IFluidHandlerItem> fluidHandler(ItemStack stack) {
        return org.overengineer.bettacraft.neoforge.transfer.FromResources.fluidItem(stack); // M52e: the 26.3 transfer API
    }

    @Override
    public boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
        return net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(player, hand, level, pos, side, null);
    }

    @Override
    public boolean interactWithFluidHandler(Player player, InteractionHand hand, IFluidHandler handler) {
        var h = org.overengineer.bettacraft.neoforge.transfer.NeoResources.fluids(handler);
        return h != null && net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(player, hand, null, h, null);
    }

    @Override
    public FluidActionResult tryEmptyContainer(ItemStack container, IFluidHandler dest, int maxAmount, @Nullable Player player, boolean doDrain) {
        return containerMove(container, dest, maxAmount, player, doDrain, true);
    }

    @Override
    public void requestModelDataUpdate(BlockEntity be) {
        be.requestModelDataUpdate();
    }

    @Override
    public net.minecraft.world.phys.AABB infiniteRenderBox() {
        return net.minecraft.world.phys.AABB.INFINITE;
    }

    @Override
    public boolean isAddedToLevel(Entity entity) {
        return entity.isAddedToLevel();
    }

    @Override
    public boolean hasChannel(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }

    @Override
    public ItemStack craftingRemainder(ItemStack stack) {
        // M52c (26.1): the remainder is a stack template, null without one
        var r = stack.getCraftingRemainder();
        return r == null ? ItemStack.EMPTY : r.create();
    }

    @Override
    public @Nullable net.minecraft.world.food.FoodProperties foodProperties(ItemStack stack, @Nullable net.minecraft.world.entity.LivingEntity eater) {
        return stack.get(net.minecraft.core.component.DataComponents.FOOD);
    }

    @Override
    public void invalidateCapabilities(Level level, BlockPos pos) {
        level.invalidateCapabilities(pos);
    }

    @Override
    public int auxLight(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) return 0; // M61: a foreign caller asking the light emission without a position
        var aux = level.getAuxLightManager(pos);
        return aux == null ? 0 : aux.getLightAt(pos);
    }

    @Override
    public void setAuxLight(Level level, BlockPos pos, int light) {
        var aux = level.getAuxLightManager(pos);
        if (aux != null && aux.getLightAt(pos) != light) aux.setLightAt(pos, light);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player) {
        return state.canHarvestBlock(level, pos, player);
    }

    @Override
    public boolean defaultHarvestCheck(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player) {
        return net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos);
    }

    @Override
    public float explosionResistance(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        return state.getExplosionResistance(level, pos, explosion);
    }

    @Override
    public net.minecraft.world.level.block.SoundType soundType(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return state.getSoundType(level, pos, entity);
    }

    @Override
    public boolean checkSpawnPosition(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason type) {
        return net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(mob, level, type);
    }

    @Override
    public @Nullable net.minecraft.world.entity.SpawnGroupData finalizeMobSpawn(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason type, @Nullable net.minecraft.world.entity.SpawnGroupData data) {
        return net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, difficulty, type, data);
    }

    @Override
    public boolean emptyContents(net.minecraft.world.item.DispensibleContainerItem item, @Nullable Player player, Level level, BlockPos pos,
            @Nullable net.minecraft.world.phys.BlockHitResult hit, @Nullable ItemStack container) {
        return item.emptyContents(player, level, pos, hit, container);
    }

    @Override
    public net.minecraft.world.item.SpawnEggItem spawnEgg(java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int background, int highlight, net.minecraft.world.item.Item.Properties props) {
        return new net.minecraft.world.item.SpawnEggItem(props.spawnEgg(type.get())); // M52b: no colours since 1.21.5 (textures)
    }

    @Override
    public <T extends net.minecraft.world.inventory.AbstractContainerMenu> net.minecraft.world.inventory.MenuType<T> menuType(MenuFactory<T> factory) {
        return net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(factory::create);
    }

    @Override
    public net.minecraft.world.level.material.FlowingFluid fluid(org.overengineer.bettacraft.platform.fluid.FluidSpec spec, boolean source) {
        return org.overengineer.bettacraft.registry.NeoForgeFluids.fluid(spec, source);
    }

    @Override
    public net.minecraft.world.item.CreativeModeTab.Builder creativeTabBuilder() {
        return net.minecraft.world.item.CreativeModeTab.builder();
    }

    @Override
    public boolean chunkHasAttachments(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        return chunk.hasAttachments();
    }

    @Override
    public float biomeDownfall(net.minecraft.world.level.biome.Biome biome) {
        return biome.getModifiedClimateSettings().downfall();
    }

    @Override
    public int lightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getLightEmission(level, pos);
    }


    @Override
    public FluidActionResult tryFillContainer(ItemStack container, IFluidHandler source, int maxAmount, @Nullable Player player, boolean doFill) {
        return containerMove(container, source, maxAmount, player, doFill, false);
    }

    /** M52e: NeoForge 26.3 has no legacy {@code tryEmptyContainer} / {@code tryFillContainer}; the same with the transfer API. */
    private static FluidActionResult containerMove(ItemStack container, IFluidHandler other, int maxAmount, @Nullable Player player,
                                                   boolean execute, boolean fromItem) {
        var none = org.overengineer.bettacraft.platform.fluid.FluidStack.EMPTY;
        var before = fromItem ? org.overengineer.bettacraft.neoforge.transfer.FromResources.fluidItem(container)
                .map(f -> f.getFluidInTank(0)).orElse(none) : none;
        ItemStack result = org.overengineer.bettacraft.neoforge.transfer.FromResources.moveWithContainer(container, other, maxAmount, execute, fromItem);
        if (result == null) return FluidActionResult.FAILURE;
        if (execute && player != null) {
            var fluid = fromItem ? before : org.overengineer.bettacraft.neoforge.transfer.FromResources.fluidItem(result)
                    .map(f -> f.getFluidInTank(0)).orElse(none);
            if (!fluid.isEmpty()) net.neoforged.neoforge.transfer.fluid.FluidUtil.triggerSoundAndGameEvent(
                    net.neoforged.neoforge.transfer.fluid.FluidResource.of(org.overengineer.bettacraft.neoforge.transfer.NeoTransfer.toNeo(fluid)),
                    player.level(), player.position().add(0, 0.5, 0), player, !fromItem);
        }
        return new FluidActionResult(true, result);
    }

    @Override
    public String worldConfigFolder() {
        return "syncedconfig"; // M52e: FML 12 loads a world's own synced (server) config from there
    }

    @Override
    public String modVersion(String modId) {
        return net.neoforged.fml.ModList.get().getModContainerById(modId).map(m -> m.getModInfo().getVersion().toString()).orElse("?");
    }
}
