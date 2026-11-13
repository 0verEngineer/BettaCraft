package org.overengineer.bettacraft.tank.menu;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringUtil;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.network.TankActionPayload;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/** Server side of the tank menu actions. Every action is checked: open menu, distance (stillValid), lock, limits. */
public final class TankActions {
    public static final int SET_NAME = 0;
    public static final int TOGGLE_SIGN = 1;
    public static final int SET_SIGN_POS = 2;
    public static final int SET_FRONT = 3;
    public static final int TOGGLE_LOCK = 4;
    /** value = target temperature * 2 (0.5 degree steps). */
    public static final int SET_TARGET = 5;
    public static final int RENAME_FISH = 6;
    /** value 0 = bucket, 1 = fish bag. */
    public static final int TAKE_OUT = 7;
    public static final int ENTER_INTERIOR = 8;
    /** M12: thin the floating plants out by half (the plants go to the player). */
    public static final int THIN_PLANTS = 9;
    /** M14: take one small animal of a kind (text = species:variety; value 1 = the worst grade) into a fish bag. */
    public static final int TAKE_CRITTER = 10;
    /** M14: take the apple snail clutches off the glass. */
    public static final int REMOVE_CLUTCHES = 11;

    public static final int MAX_FISH_NAME = 32;

    private TankActions() {}

    public static void handle(ServerPlayer player, TankActionPayload p) {
        if (!(player.containerMenu instanceof TankMenu menu) || menu.containerId != p.containerId()) return;
        if (!menu.stillValid(player)) return;
        TankBlockEntity controller = menu.controller();
        if (controller == null) return;
        TankData tank = controller.getTank();
        boolean modify = TankInhabitants.canModify(player, tank);
        boolean owner = player.getUUID().equals(tank.owner()) || player.hasPermissions(2);
        Component error = null;
        switch (p.action()) {
            case SET_NAME -> {
                if (!modify) error = locked();
                else tank.setName(clean(p.text(), TankData.MAX_NAME_LENGTH));
            }
            case TOGGLE_SIGN -> {
                if (!modify) error = locked();
                else tank.setSignVisible(!tank.signVisible());
            }
            case SET_SIGN_POS -> {
                if (!modify) error = locked();
                else if (p.value() >= 0 && p.value() < SignPosition.values().length) tank.setSignPos(SignPosition.values()[p.value()]);
            }
            case SET_FRONT -> {
                if (!modify) error = locked();
                else tank.setFront(Direction.from2DDataValue(Math.floorMod(p.value(), 4)));
            }
            case TOGGLE_LOCK -> {
                if (!owner) error = Component.translatable("message.bettacraft.tank.only_owner");
                else {
                    if (tank.owner() == null) tank.setOwner(player.getUUID()); // ownerless (e.g. placed by a command)
                    tank.setLocked(!tank.locked());
                }
            }
            case SET_TARGET -> {
                if (!modify) error = locked();
                else tank.setTargetTemp(p.value() / 2.0F);
            }
            case RENAME_FISH -> {
                if (!modify) error = locked();
                else if (p.target().isPresent()) error = rename(player, tank, p.target().get(), clean(p.text(), MAX_FISH_NAME));
            }
            case TAKE_OUT -> {
                if (p.target().isPresent()) error = TankInhabitants.takeOut(player, controller, p.target().get(), p.value() == 1);
            }
            case THIN_PLANTS -> {
                if (!modify) error = locked();
                else {
                    var items = org.overengineer.bettacraft.tank.plants.TankPlants.thin(player.serverLevel(), controller);
                    if (items.isEmpty()) error = Component.translatable("message.bettacraft.plants.none");
                    else org.overengineer.bettacraft.tank.plants.TankPlants.taken(player.serverLevel(), controller, player, items, true);
                }
            }
            case TAKE_CRITTER -> error = org.overengineer.bettacraft.critters.TankCritters.takeOut(player.serverLevel(), controller, player,
                    p.text(), p.value() == 1, p.value() == 2 ? org.overengineer.bettacraft.critters.TankCritters.MAX_PER_BAG : 1);
            case REMOVE_CLUTCHES -> {
                if (!modify) error = locked();
                else {
                    int n = org.overengineer.bettacraft.critters.TankCritters.removeClutches(player.serverLevel(), controller);
                    if (n == 0) error = Component.translatable("message.bettacraft.critters.no_clutches");
                    else {
                        TankCare.log(player.serverLevel(), controller, "log.bettacraft.clutch_removed", TankCare.nameArg(player));
                        player.displayClientMessage(Component.translatable("message.bettacraft.critters.clutch_removed").withStyle(ChatFormatting.GREEN), true);
                    }
                }
            }
            case ENTER_INTERIOR -> {
                player.closeContainer();
                InteriorManager.enter(player, controller.getBlockPos());
                return;
            }
            default -> {
                return; // M17: unknown action ids do nothing at all
            }
        }
        if (error != null) {
            player.displayClientMessage(error.copy().withStyle(ChatFormatting.RED), true);
        } else {
            // M17: only a change goes to the clients near the tank (a refused action sent the whole tank view to all)
            controller.markTankChanged();
        }
        menu.requestSync();
    }

    private static Component locked() {
        return Component.translatable("message.bettacraft.tank.locked");
    }

    private static String clean(String s, int max) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (StringUtil.isAllowedChatCharacter(c)) b.append(c);
        String r = b.toString().strip();
        return r.length() > max ? r.substring(0, max) : r;
    }

    private static Component rename(ServerPlayer player, TankData tank, UUID id, String name) {
        Entity e = player.serverLevel().getEntity(id);
        if (e == null || !TankInhabitants.hasTankRef(e) || !tank.id().equals(e.getData(ModAttachments.TANK_REF.get()).tankId())) {
            return Component.translatable("message.bettacraft.tank.not_inside");
        }
        e.setCustomName(name.isEmpty() ? null : Component.literal(name));
        return null;
    }

    /** Used by the screen and the server log. */
    public static Component logLine(TankView.LogLine l) {
        Object[] args = l.args().stream().map(TankCare::argComponent).toArray();
        return Component.translatable(l.key(), args);
    }
}
