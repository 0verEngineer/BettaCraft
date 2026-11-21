package org.overengineer.bettacraft.tank.menu;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
    /** M19: takes the breeding box out of the tank (the young in it swim free). */
    public static final int REMOVE_BREEDING_BOX = 12;

    /** M21.5: break protection on/off (owner or OP). */
    public static final int TOGGLE_PROTECT = 13;
    /** M22.3: takes exactly one small animal (value = its id; text "bucket" or "bag"). */
    public static final int TAKE_CRITTER_ONE = 14;
    /** M22.3: analyses one animal with the magnifier (target = a fish; or text "critter" and value = the id of a small animal). */
    public static final int ANALYSE = 15;
    /** M23: lights: dimmer (value 0..100), timer on/off, on time and off time (value = minutes of the day). */
    public static final int SET_DIMMER = 16;
    public static final int TOGGLE_TIMER = 17;
    public static final int SET_LIGHT_ON = 18;
    public static final int SET_LIGHT_OFF = 19;
    /** M51: who may let his hired aquarist work here (owner or OP; value = the {@code AquaristAccess} ordinal). */
    public static final int SET_AQUARIST = 20;
    /** M53: adds a user (text = player name, value = the role ordinal: caretaker or viewer); owner, caretakers or OP. */
    public static final int USERS_ADD = 21;
    /** M53: changes the role of a listed user (target, value = role ordinal). */
    public static final int USERS_ROLE = 22;
    /** M53: removes a listed user (target; a user may also remove himself). */
    public static final int USERS_REMOVE = 23;
    /** M53: the sender's own alarm waypoint switch for this tank (value 1 on, 0 off); any user, also a viewer. */
    public static final int WAYPOINT_OWN = 24;
    /** M53: the alarm waypoints of this tank for all of its users (value 1 on, 0 off); owner, caretakers or OP. */
    public static final int WAYPOINT_ALL = 25;

    /** M22.3: bits of the value of {@link #TAKE_CRITTER}: 0-1 how many (one best, one worst, a full container), 2-3 the sex, 4 a bucket. */
    public static int critterValue(int mode, int sex, boolean bucket) {
        return (mode & 3) | (sex & 3) << 2 | (bucket ? 16 : 0);
    }

    public static final int MAX_FISH_NAME = 32;

    private TankActions() {}

    public static void handle(ServerPlayer player, TankActionPayload p) {
        if (!(player.containerMenu instanceof TankMenu menu) || menu.containerId != p.containerId()) return;
        if (!menu.stillValid(player)) return;
        TankBlockEntity controller = menu.controller();
        if (controller == null) return;
        TankData tank = controller.getTank();
        boolean modify = TankInhabitants.canModify(player, tank);
        // M53: owner, caretakers and operators manage the tank (lock, aquarist, users, the waypoints of all)
        boolean owner = org.overengineer.bettacraft.tank.TankAccess.canManage(player, tank);
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
            case SET_DIMMER, TOGGLE_TIMER, SET_LIGHT_ON, SET_LIGHT_OFF -> {
                if (!modify) error = locked();
                else if (Features.enabled(Feature.LIGHT_TIMER)) {
                    switch (p.action()) {
                        case SET_DIMMER -> tank.setLightDimmer(p.value());
                        case TOGGLE_TIMER -> {
                            tank.setLightTimer(!tank.lightTimer());
                            if (tank.lightTimer()) org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.LIGHT_TIMER); // M45
                        }
                        case SET_LIGHT_ON -> tank.setLightOn(p.value());
                        default -> tank.setLightOff(p.value());
                    }
                    controller.setChanged();
                    org.overengineer.bettacraft.tank.TankLighting.step(player.serverLevel(), controller);
                    org.overengineer.bettacraft.tank.TankPower.updateLights(player.serverLevel(), tank);
                }
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
            case TOGGLE_PROTECT -> error = org.overengineer.bettacraft.tank.TankProtection.toggle(player, controller);
            case SET_AQUARIST -> {
                var values = org.overengineer.bettacraft.aquarist.AquaristAccess.values();
                if (!owner && tank.owner() != null) error = Component.translatable("message.bettacraft.tank.only_owner");
                else if (!Features.enabled(Feature.AQUARIST_NPC)) error = Component.translatable("message.bettacraft.aquarist.feature_off");
                else if (p.value() >= 0 && p.value() < values.length) {
                    if (tank.owner() == null) tank.setOwner(player.getUUID());
                    tank.setAquaristAccess(values[p.value()]);
                    TankCare.log(player.serverLevel(), controller, "log.bettacraft.aquarist.access", TankCare.nameArg(player),
                            "@" + values[p.value()].translationKey());
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
            case TAKE_CRITTER -> {
                int mode = p.value() & 3, sex = (p.value() >> 2) & 3;
                boolean bucket = (p.value() & 16) != 0;
                int full = 1;
                if (mode == 2) {
                    var s = org.overengineer.bettacraft.critters.CritterSpecies.byIdOrNull(p.text().contains(":") ? p.text().substring(0, p.text().indexOf(':')) : p.text());
                    full = s == null ? 1 : Math.max(1, bucket ? org.overengineer.bettacraft.containers.AnimalContainers.bucket(s)
                            : org.overengineer.bettacraft.containers.AnimalContainers.bag(s));
                }
                error = org.overengineer.bettacraft.critters.TankCritters.takeOut(player.serverLevel(), controller, player, p.text(), mode == 1, full, sex, bucket);
            }
            case TAKE_CRITTER_ONE -> error = org.overengineer.bettacraft.critters.TankCritters.takeOutById(player.serverLevel(), controller, player, p.value(),
                    "bucket".equals(p.text()));
            case ANALYSE -> error = analyse(player, controller, p);
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
            case REMOVE_BREEDING_BOX -> {
                if (!modify) error = locked();
                else error = org.overengineer.bettacraft.tankmates.TankmateItems.removeBox(player.serverLevel(), controller, player);
            }
            case USERS_ADD, USERS_ROLE, USERS_REMOVE, WAYPOINT_OWN, WAYPOINT_ALL -> error = TankUserActions.handle(player, controller, p, owner);
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

    /** M22.3: the magnifier on one animal of the tank (one use). */
    private static Component analyse(ServerPlayer player, TankBlockEntity controller, TankActionPayload p) {
        if (org.overengineer.bettacraft.genetics.GeneVisibility.free()) return Component.translatable("message.bettacraft.magnifier.free");
        var magnifier = org.overengineer.bettacraft.genetics.MagnifierItem.find(player);
        if (magnifier.isEmpty()) return Component.translatable("message.bettacraft.magnifier.need");
        TankData tank = controller.getTank();
        boolean done = false;
        if ("critter".equals(p.text())) {
            for (var k : tank.critters()) {
                if (k.id != p.value() || !k.alive()) continue;
                if (k.analysed) return Component.translatable("message.bettacraft.magnifier.already");
                k.analysed = true;
                done = true;
            }
        } else if (p.target().isPresent()) {
            Entity e = player.serverLevel().getEntity(p.target().get());
            if (e == null || !TankInhabitants.hasTankRef(e) || !tank.id().equals(ModAttachments.TANK_REF.get(e).tankId())) {
                return Component.translatable("message.bettacraft.tank.not_inside");
            }
            if (!org.overengineer.bettacraft.genetics.MagnifierItem.analyse(e)) return Component.translatable("message.bettacraft.magnifier.already");
            done = true;
        }
        if (!done) return Component.translatable("message.bettacraft.critters.none_of_kind");
        org.overengineer.bettacraft.genetics.MagnifierItem.wear(player, magnifier, null);
        player.serverLevel().playSound(null, controller.getBlockPos(), net.minecraft.sounds.SoundEvents.SPYGLASS_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.bettacraft.magnifier.analysed", 1).withStyle(ChatFormatting.GREEN), true);
        TankCare.log(player.serverLevel(), controller, "log.bettacraft.analysed", TankCare.nameArg(player), "1");
        return null;
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
        if (e == null || !TankInhabitants.hasTankRef(e) || !tank.id().equals(ModAttachments.TANK_REF.get(e).tankId())) {
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
