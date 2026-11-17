package org.overengineer.bettacraft.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.overengineer.bettacraft.crucible.CrucibleRecipe;
import org.overengineer.bettacraft.equipment.CabinetStones;
import org.overengineer.bettacraft.food.FoodMakerRecipe;
import org.overengineer.bettacraft.network.RecipeSyncPayload;
import org.overengineer.bettacraft.platform.event.OnDatapackSyncEvent;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M52b: our recipes for the clients ({@link RecipeSyncPayload}). The client keeps them ({@link #client}) until it leaves
 * the server; a single player world gets them the same way.
 */
public final class RecipeSync {
    private static volatile Client client = Client.EMPTY;

    /** What the client got: the food maker and crucible recipes (sorted by id) and the stone cabinet pairs. */
    public record Client(List<RecipeHolder<FoodMakerRecipe>> foodMaker, List<RecipeHolder<CrucibleRecipe>> crucible,
                         List<CabinetStones.Pair> stones) {
        static final Client EMPTY = new Client(List.of(), List.of(), List.of());
    }

    private RecipeSync() {}

    public static RecipeSyncPayload payload(MinecraftServer server) {
        RecipeManager rm = server.getRecipeManager();
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        recipes.addAll(RecipeCompat.byType(rm, ModRecipes.FOOD_MAKER_TYPE.get()));
        recipes.addAll(RecipeCompat.byType(rm, ModRecipes.CRUCIBLE_TYPE.get()));
        recipes.sort(Comparator.comparing(h -> h.id().identifier().toString()));
        List<Item> stones = new ArrayList<>(), slabs = new ArrayList<>();
        for (CabinetStones.Pair p : CabinetStones.all(rm, server.registryAccess())) {
            stones.add(p.stone());
            slabs.add(p.slab());
        }
        return new RecipeSyncPayload(recipes, stones, slabs);
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> players = event.getRelevantPlayers().toList();
        if (players.isEmpty()) return;
        RecipeSyncPayload p = payload(players.getFirst().level().getServer());
        for (ServerPlayer sp : players) {
            // fake and GameTest mock players have no channel
            if (sp.connection != null && org.overengineer.bettacraft.platform.Platform.get().hasChannel(sp, RecipeSyncPayload.TYPE)) {
                PacketDistributor.sendToPlayer(sp, p);
            }
        }
    }

    /** Client: the recipes of the server (main thread). */
    @SuppressWarnings("unchecked")
    public static void accept(RecipeSyncPayload p) {
        List<RecipeHolder<FoodMakerRecipe>> food = new ArrayList<>();
        List<RecipeHolder<CrucibleRecipe>> crucible = new ArrayList<>();
        for (RecipeHolder<?> h : p.recipes()) {
            if (h.value() instanceof FoodMakerRecipe) food.add((RecipeHolder<FoodMakerRecipe>) h);
            else if (h.value() instanceof CrucibleRecipe) crucible.add((RecipeHolder<CrucibleRecipe>) h);
        }
        List<CabinetStones.Pair> stones = new ArrayList<>();
        for (int i = 0; i < Math.min(p.stones().size(), p.slabs().size()); i++) {
            stones.add(new CabinetStones.Pair(p.stones().get(i), p.slabs().get(i)));
        }
        client = new Client(List.copyOf(food), List.copyOf(crucible), List.copyOf(stones));
    }

    /** Client: forget them (left the server). */
    public static void clear() {
        client = Client.EMPTY;
    }

    public static Client client() {
        return client;
    }
}
