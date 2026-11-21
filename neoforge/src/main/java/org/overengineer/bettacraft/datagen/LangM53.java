package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M53 English texts: the users of a tank (users tab: owner, caretakers, viewers), the alarm waypoint switches per tank and
 * per user, the client config of the map mods and the gear button of the overview. The German ones are in de_de.json.
 */
final class LangM53 {
    private LangM53() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("gui.bettacraft.tab.users", "Users");
        a.accept("gui.bettacraft.tank.client_config", "Bettacraft client settings: the alarm waypoints of all tanks, which map mods show them, rendering and sounds (also in the config file and the mod list)");
        a.accept("gui.bettacraft.client_config.title", "Bettacraft: client settings");
        a.accept("gui.bettacraft.users.header", "Users of this tank (you: %s)");
        a.accept("gui.bettacraft.users.role.owner", "Owner");
        a.accept("gui.bettacraft.users.role.caretaker", "Caretaker");
        a.accept("gui.bettacraft.users.role.viewer", "Viewer");
        a.accept("gui.bettacraft.users.role.other", "Not listed");
        a.accept("gui.bettacraft.users.roles_title", "Who may do what");
        a.accept("gui.bettacraft.users.role.owner.desc", "Owner (built the tank): everything, also the users and the lock");
        a.accept("gui.bettacraft.users.role.caretaker.desc", "Caretaker: everything the owner may (care, settings, going in, users, lock)");
        a.accept("gui.bettacraft.users.role.viewer.desc", "Viewer: sees the menu, changes nothing, cannot go into the tank");
        a.accept("gui.bettacraft.users.role.other.desc", "Everyone else: may change the tank while it is open, only look while it is locked");
        a.accept("gui.bettacraft.users.role_button", "Role: %s");
        a.accept("gui.bettacraft.users.role_button_none", "Role: -");
        a.accept("gui.bettacraft.users.role_tooltip", "Select a user in the list, then switch them between caretaker and viewer");
        a.accept("gui.bettacraft.users.remove", "Remove");
        a.accept("gui.bettacraft.users.remove_tooltip", "Takes the selected user off the list (owner and caretakers)");
        a.accept("gui.bettacraft.users.leave_tooltip", "Takes you off the list of this tank");
        a.accept("gui.bettacraft.users.my_waypoint_on", "My waypoint: on");
        a.accept("gui.bettacraft.users.my_waypoint_off", "My waypoint: off");
        a.accept("gui.bettacraft.users.my_waypoint_tooltip", "Your own alarm waypoint of this tank: an alarm of a monitor or a warning of the hired aquarist puts a waypoint at it on your map (JourneyMap, Xaero's Minimap, FTB Chunks). On by default for the owner, off for the other users. The gear in the overview sets it for all tanks and per map mod");
        a.accept("gui.bettacraft.users.name", "Player name");
        a.accept("gui.bettacraft.users.name_hint", "Player name");
        a.accept("gui.bettacraft.users.new_role", "as %s");
        a.accept("gui.bettacraft.users.new_role_tooltip", "The role of the next player you add (caretaker or viewer)");
        a.accept("gui.bettacraft.users.add", "Add");
        a.accept("gui.bettacraft.users.add_tooltip", "Adds the player with this name (he has to be online) as caretaker or viewer");
        a.accept("gui.bettacraft.users.all_waypoints_on", "Waypoints for all: on");
        a.accept("gui.bettacraft.users.all_waypoints_off", "Waypoints for all: off");
        a.accept("gui.bettacraft.users.all_waypoints_tooltip", "The alarm waypoints of this tank for all of its users (owner and caretakers switch this; off: nobody gets one, whatever their own switch says)");
        a.accept("gui.bettacraft.users.others_change", "Others: may change");
        a.accept("gui.bettacraft.users.others_view", "Others: view only");
        a.accept("gui.bettacraft.users.others_tooltip", "Players who are not on the list: while the tank is open they may change it and go in, locked they only look (the same as the lock in the overview)");
        a.accept("gui.bettacraft.users.waypoint_on", "waypoint");
        a.accept("gui.bettacraft.users.waypoint_off", "no waypoint");
        a.accept("gui.bettacraft.users.empty", "Nobody else listed yet: add a player below");
        a.accept("gui.bettacraft.users.feature_off", "The user list is switched off on this server (features.equipment.tankUsers)");
        a.accept("message.bettacraft.users.viewer", "You are a viewer of this tank: you can look, but not change it or go in");
        a.accept("message.bettacraft.users.waypoints_off", "Alarm waypoints are switched off on this server (features.compat.journeymap)");
        a.accept("message.bettacraft.users.not_user", "Only the owner and the listed users of this tank have an alarm waypoint");
        a.accept("message.bettacraft.users.feature_off", "The user list is switched off on this server (features.equipment.tankUsers)");
        a.accept("message.bettacraft.users.unknown", "No player named %s is online");
        a.accept("message.bettacraft.users.is_owner", "%s is the owner of this tank");
        a.accept("message.bettacraft.users.full", "A tank can have at most %s users");
        a.accept("message.bettacraft.users.added", "%s is now %s of this tank");
        a.accept("message.bettacraft.users.not_listed", "That player is not on the list of this tank");
        a.accept("log.bettacraft.users.added", "%s added %s as %s");
        a.accept("log.bettacraft.users.role", "%s made %s a %s");
        a.accept("log.bettacraft.users.removed", "%s took %s off the user list");
        a.accept("advancements.bettacraft.shared_tank.title", "Open House");
        a.accept("advancements.bettacraft.shared_tank.description", "Add another player to the users of a tank (users tab of the tank menu)");
        a.accept("quest.bettacraft.shared_tank.title", "Open House");
        a.accept("quest.bettacraft.shared_tank.desc", "In the users tab of the tank menu the owner lists other players: caretakers may do everything the owner may, viewers only look. Players who are not listed are ruled by the lock. Add a second player while they are online: this needs a second player on the server (an optional quest, it blocks nothing).");
        a.accept("bettacraft.configuration.journeyMap", "JourneyMap");
        a.accept("bettacraft.configuration.journeyMap.tooltip", "Put the alarm waypoints into JourneyMap (when it is installed)");
        a.accept("bettacraft.configuration.xaerosMinimap", "Xaero's Minimap / World Map");
        a.accept("bettacraft.configuration.xaerosMinimap.tooltip", "Put the alarm waypoints into Xaero's Minimap and its World Map (when it is installed)");
        a.accept("bettacraft.configuration.ftbChunks", "FTB Chunks");
        a.accept("bettacraft.configuration.ftbChunks.tooltip", "Put the alarm waypoints into FTB Chunks (when it is installed)");
    }
}
