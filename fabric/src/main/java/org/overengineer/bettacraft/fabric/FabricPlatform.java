package org.overengineer.bettacraft.fabric;

import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.fabric.transfer.FabricTransfer;
import org.overengineer.bettacraft.platform.Platform;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.FluidSpec;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem;
import org.overengineer.bettacraft.platform.item.IItemHandler;

/** M52: the platform layer on Fabric (found through META-INF/services). */
public final class FabricPlatform implements Platform {
    /** Set by the client entry point (ClientPlayNetworking is a client class). */
    public static Consumer<CustomPacketPayload> clientSender = p -> {
        throw new IllegalStateException("no client to send " + p.type().id() + " from");
    };
    static MinecraftServer server;

    @Override
    public String loader() {
        return "fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public @Nullable MinecraftServer currentServer() {
        return server;
    }

    @Override
    public CompoundTag persistentData(Entity entity) {
        return ((PersistentDataHolder) entity).bettacraft$persistentData();
    }

    @Override
    public boolean isFakePlayer(@Nullable Player player) {
        return player instanceof FakePlayer;
    }

    @Override
    public String fluidDescriptionId(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return "block.minecraft.air";
        if (fluid.isSame(Fluids.WATER)) return "block.minecraft.water";
        if (fluid.isSame(Fluids.LAVA)) return "block.minecraft.lava";
        var key = fluid.builtInRegistryHolder().key().location();
        if (key.getNamespace().equals("bettacraft")) return "fluid_type.bettacraft." + key.getPath().replaceFirst("^flowing_", "");
        Component name = FluidVariantAttributes.getName(FluidVariant.of(fluid));
        return name.getContents() instanceof TranslatableContents t ? t.getKey() : name.getString();
    }

    // ---- networking

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        clientSender.accept(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        for (ServerPlayer p : PlayerLookup.tracking(entity)) sendToPlayer(p, payload);
    }

    @Override
    public void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        sendToPlayersTrackingEntity(entity, payload);
        if (entity instanceof ServerPlayer self) sendToPlayer(self, payload);
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        for (ServerPlayer p : PlayerLookup.tracking(level, chunk)) sendToPlayer(p, payload);
    }

    @Override
    public void sendToAllPlayers(CustomPacketPayload payload) {
        if (server != null) for (ServerPlayer p : PlayerLookup.all(server)) sendToPlayer(p, payload);
    }

    @Override
    public boolean hasChannel(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && ServerPlayNetworking.canSend(player, type);
    }

    // ---- menus: the extra data travels as bytes (Fabric's extended screen handler with a byte array)

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        extra.accept(buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        player.openMenu(new ExtendedScreenHandlerFactory<byte[]>() {
            @Override
            public byte[] getScreenOpeningData(ServerPlayer p) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return provider.createMenu(id, inv, p);
            }
        });
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
        return new ExtendedScreenHandlerType<>((id, inv, data) -> {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inv.player.registryAccess());
            return factory.create(id, inv, buf);
        }, ByteBufCodecs.BYTE_ARRAY.cast());
    }

    // ---- other mods' blocks and items (transfer API)

    @Override
    public @Nullable IItemHandler itemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return FabricTransfer.items(level, pos, side);
    }

    @Override
    public @Nullable IFluidHandler fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return FabricTransfer.fluids(level, pos, side);
    }

    @Override
    public @Nullable IEnergyStorage energyStorage(Level level, BlockPos pos, @Nullable Direction side) {
        return FabricTransfer.energy(level, pos, null, null, side);
    }

    @Override
    public @Nullable IEnergyStorage energyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side) {
        return FabricTransfer.energy(level, pos, state, be, side);
    }

    @Override
    public Optional<IFluidHandlerItem> fluidHandler(ItemStack stack) {
        return Optional.ofNullable(FabricTransfer.fluidItem(stack));
    }

    @Override
    public boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
        return FabricTransfer.interact(player, hand, level, pos, side);
    }

    @Override
    public boolean interactWithFluidHandler(Player player, InteractionHand hand, IFluidHandler handler) {
        return FabricTransfer.interact(player, hand, handler);
    }

    @Override
    public FluidActionResult tryEmptyContainer(ItemStack container, IFluidHandler dest, int maxAmount, @Nullable Player player, boolean doDrain) {
        return FabricTransfer.emptyContainer(container, dest, maxAmount, doDrain);
    }

    @Override
    public FluidActionResult tryFillContainer(ItemStack container, IFluidHandler source, int maxAmount, @Nullable Player player, boolean doFill) {
        return FabricTransfer.fillContainer(container, source, maxAmount, doFill);
    }

    // ---- vanilla equivalents of NeoForge's extensions

    @Override
    public void requestModelDataUpdate(BlockEntity be) {
        Level level = be.getLevel();
        if (level != null && level.isClientSide()) {
            BlockState s = be.getBlockState();
            level.sendBlockUpdated(be.getBlockPos(), s, s, 8);
        }
    }

    @Override
    public AABB infiniteRenderBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    public boolean isAddedToLevel(Entity entity) {
        // NeoForge's isAddedToLevel: the entity got a level callback from the entity manager (also in a chunk that is
        // not ticking, where the level's getEntity does not see it)
        return !entity.isRemoved() && entity.levelCallback != net.minecraft.world.level.entity.EntityInLevelCallback.NULL;
    }

    @Override
    public ItemStack craftingRemainder(ItemStack stack) {
        return stack.getRecipeRemainder();
    }

    @Override
    public @Nullable FoodProperties foodProperties(ItemStack stack, @Nullable LivingEntity eater) {
        return stack.get(DataComponents.FOOD);
    }

    @Override
    public void invalidateCapabilities(Level level, BlockPos pos) {
        // the transfer API looks up again on every query (BlockApiCache invalidates itself)
    }

    @Override
    public int auxLight(BlockGetter level, BlockPos pos) {
        return 0; // no auxiliary light on Fabric: covers of filter pipes do not shine
    }

    @Override
    public void setAuxLight(Level level, BlockPos pos, int light) {}

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return defaultHarvestCheck(state, level, pos, player);
    }

    @Override
    public boolean defaultHarvestCheck(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state);
    }

    @Override
    public float explosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getBlock().getExplosionResistance();
    }

    @Override
    public SoundType soundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return state.getSoundType();
    }

    @Override
    public int lightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getLightEmission();
    }

    @Override
    public boolean checkSpawnPosition(Mob mob, ServerLevelAccessor level, EntitySpawnReason type) {
        return mob.checkSpawnRules(level, type) && mob.checkSpawnObstruction(level);
    }

    @Override
    public @Nullable SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason type,
            @Nullable SpawnGroupData data) {
        return mob.finalizeSpawn(level, difficulty, type, data);
    }

    @Override
    public boolean emptyContents(DispensibleContainerItem item, @Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit,
            @Nullable ItemStack container) {
        return item.emptyContents(player, level, pos, hit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SpawnEggItem spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Item.Properties props) {
        // the entity types are registered before the items (vanilla registry order), so the type is there
        return new SpawnEggItem((EntityType<? extends Mob>) type.get(), background, highlight, props);
    }

    @Override
    public FlowingFluid fluid(FluidSpec spec, boolean source) {
        return source ? new BettaFluid.Source(spec) : new BettaFluid.Flowing(spec);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public float biomeDownfall(Biome biome) {
        return biome.climateSettings.downfall();
    }


    /** {@code {"item": ...}} or {@code {"tag": ...}}: the flat ingredient of a sized ingredient (vanilla values). */
    static final class FlatIngredient {
        static final com.mojang.serialization.MapCodec<Ingredient> MAP_CODEC = com.mojang.serialization.Codec.mapEither(
                ItemStack.SIMPLE_ITEM_CODEC.fieldOf("item"), TagKey.codec(Registries.ITEM).fieldOf("tag")
        ).xmap(e -> e.map(Ingredient::of, Ingredient::of), ing -> {
            ItemStack[] items = ing.getItems();
            return com.mojang.datafixers.util.Either.left(items.length == 0 ? ItemStack.EMPTY : items[0]);
        });

        private FlatIngredient() {}
    }

    @Override
    public String modVersion(String modId) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }
}
