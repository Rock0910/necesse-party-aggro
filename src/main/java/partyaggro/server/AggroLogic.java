package partyaggro.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanAngerTargetAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import partyaggro.data.AggroServerSection;
import partyaggro.data.AggroSource;
import partyaggro.net.PacketAggroHatredAdded;
import partyaggro.util.Debug;

/** Server-authoritative aggro behaviour. */
public final class AggroLogic {
    private static final float ANGER_AMOUNT = 2.0f;
    private static int serverTickCounter = 0;

    private AggroLogic() {
    }

    /**
     * Server-authoritative periodic scan (about once per second). Keeps every player on the
     * hatred list (and their party members) as an active enemy of the owner's party while the
     * system is enabled, so villagers proactively attack them and resume after re-enabling.
     */
    public static void serverTick(Server server) {
        if (server == null) {
            return;
        }
        if ((++serverTickCounter % 20) != 0) {
            return;
        }
        try {
            for (ServerClient owner : server.getClients()) {
                if (owner == null || owner.playerMob == null) {
                    continue;
                }
                pruneEnemies(owner);
                if (AggroServerState.isRuntimeDisabled(owner.authentication)) {
                    continue;
                }
                AggroServerSection section = AggroServerState.get(owner.authentication);
                if (section == null || !section.enabled) {
                    continue;
                }
                Set<Long> hated = section.effectiveHatred();
                if (hated.isEmpty()) {
                    continue;
                }
                Collection<HumanMob> party = owner.adventureParty.getMobs();
                if (party == null || party.isEmpty()) {
                    continue;
                }
                for (Long auth : hated) {
                    if (auth == null || auth.longValue() == owner.authentication) {
                        continue; // never treat the owner as an enemy
                    }
                    ServerClient enemyClient = server.getClientByAuth(auth.longValue());
                    if (enemyClient == null || enemyClient.playerMob == null) {
                        continue;
                    }
                    PlayerMob enemy = enemyClient.playerMob;
                    for (HumanMob member : party) {
                        if (member == null || member.removed() || member.getLevel() != enemy.getLevel()) {
                            continue;
                        }
                        if (!member.canTarget(enemy)) {
                            continue; // not a valid target for this villager (friendly / PvP off)
                        }
                        HumanAngerTargetAINode<?> handler = member.ai.blackboard
                                .getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
                        if (handler == null) {
                            continue;
                        }
                        handler.addEnemy(enemy, ANGER_AMOUNT);
                        Collection<HumanMob> enemyParty = enemyClient.adventureParty.getMobs();
                        if (enemyParty != null) {
                            for (HumanMob derived : enemyParty) {
                                if (derived != null && !derived.removed() && derived != member
                                        && derived.getLevel() == member.getLevel()
                                        && member.canTarget(derived)) {
                                    handler.addEnemy(derived, ANGER_AMOUNT);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Debug.log("serverTick error: " + t);
        }
    }

    /** Called from the Mob.isHit patch on the server after a hit is calculated. */
    public static void onHit(Mob victim, Mob attackerOwner) {
        if (victim == null || attackerOwner == null || !victim.isServer()) {
            return;
        }
        if (attackerOwner instanceof PlayerMob) {
            handlePlayerAttacker(victim, (PlayerMob) attackerOwner);
        } else if (attackerOwner instanceof HumanMob) {
            handleVillagerAttacker(victim, (HumanMob) attackerOwner);
        }
    }

    private static void handlePlayerAttacker(Mob victim, PlayerMob attacker) {
        if (victim instanceof PlayerMob) {
            PlayerMob victimPlayer = (PlayerMob) victim;
            ServerClient victimClient = victimPlayer.getServerClient();
            ServerClient attackerClient = attacker.getServerClient();
            if (victimClient == null || attackerClient == null
                    || attackerClient.authentication == victimClient.authentication) {
                return;
            }

            // Scenario A: the victim's party retaliates against the attacker.
            AggroServerSection victimSection = activeSection(victimClient.authentication);
            Debug.log("scenarioA owner=" + victimClient.getName() + " section=" + (victimSection != null)
                    + " flag=" + (victimSection != null && victimSection.autoAddOnPlayerAttacked)
                    + " white=" + (victimSection != null && victimSection.isWhitelist(attackerClient.authentication)));
            if (victimSection != null && victimSection.autoAddOnPlayerAttacked
                    && !victimSection.isWhitelist(attackerClient.authentication)) {
                addAndRetaliate(victimClient, victimSection, attackerClient, attacker);
            }

            // Scenario B: the attacker's own party supports the attacker against the victim.
            AggroServerSection attackerSection = activeSection(attackerClient.authentication);
            Debug.log("scenarioB owner=" + attackerClient.getName() + " section=" + (attackerSection != null)
                    + " flag=" + (attackerSection != null && attackerSection.autoAddOnPlayerAttacks)
                    + " white=" + (attackerSection != null && attackerSection.isWhitelist(victimClient.authentication)));
            if (attackerSection != null && attackerSection.autoAddOnPlayerAttacks
                    && !attackerSection.isWhitelist(victimClient.authentication)) {
                addAndRetaliate(attackerClient, attackerSection, victimClient, victimPlayer);
            }
        } else if (victim instanceof HumanMob) {
            HumanMob victimMob = (HumanMob) victim;
            ServerClient owner = victimMob.adventureParty.getServerClient();
            ServerClient attackerClient = attacker.getServerClient();
            if (owner == null || attackerClient == null || attackerClient.authentication == owner.authentication) {
                return;
            }
            // Scenario C: a party member was attacked.
            AggroServerSection section = activeSection(owner.authentication);
            Debug.log("scenarioC owner=" + owner.getName() + " section=" + (section != null)
                    + " flag=" + (section != null && section.autoAddOnPartyMemberAttacked)
                    + " white=" + (section != null && section.isWhitelist(attackerClient.authentication)));
            if (section != null && section.autoAddOnPartyMemberAttacked
                    && !section.isWhitelist(attackerClient.authentication)) {
                addAndRetaliate(owner, section, attackerClient, attacker);
            }
        }
    }

    /**
     * Scenario D: an adventure-party villager attacked a player. The victim adds the
     * villager's party owner (and thus the whole enemy team, as derived enemies) to their list.
     */
    private static void handleVillagerAttacker(Mob victim, HumanMob villager) {
        if (!(victim instanceof PlayerMob)) {
            return;
        }
        ServerClient owner = villager.adventureParty.getServerClient();
        if (owner == null || owner.playerMob == null) {
            return; // not an adventure-party villager
        }
        PlayerMob victimPlayer = (PlayerMob) victim;
        ServerClient victimClient = victimPlayer.getServerClient();
        if (victimClient == null || victimClient.authentication == owner.authentication) {
            return;
        }
        AggroServerSection section = activeSection(victimClient.authentication);
        Debug.log("scenarioD victim=" + victimClient.getName() + " enemyOwner=" + owner.getName()
                + " section=" + (section != null)
                + " flag=" + (section != null && section.autoAddOnPlayerAttacked)
                + " white=" + (section != null && section.isWhitelist(owner.authentication)));
        if (section != null && section.autoAddOnPlayerAttacked
                && !section.isWhitelist(owner.authentication)) {
            addAndRetaliate(victimClient, section, owner, owner.playerMob);
        }
    }

    /** Returns the section only when the system is enabled and not temporarily disabled. */
    private static AggroServerSection activeSection(long authentication) {
        if (AggroServerState.isRuntimeDisabled(authentication)) {
            return null;
        }
        AggroServerSection section = AggroServerState.get(authentication);
        return section != null && section.enabled ? section : null;
    }

    private static void addAndRetaliate(ServerClient owner, AggroServerSection section, ServerClient enemyClient, Mob enemyMob) {
        section.addHatred(enemyClient.authentication, AggroSource.AUTO);
        notifyHatred(owner, enemyClient);
        int angered = retaliate(owner, enemyMob);
        Debug.log("hate+ " + enemyClient.getName() + " -> " + owner.getName() + " (angered " + angered + " villagers)");
    }

    private static void notifyHatred(ServerClient owner, ServerClient enemyClient) {
        try {
            owner.sendChatMessage(new LocalMessage("partyaggro", "hatredadded", "name", enemyClient.getName()));
        } catch (Throwable ignored) {
        }
        try {
            owner.sendPacket(new PacketAggroHatredAdded(AggroServerState.worldId(owner.authentication),
                    enemyClient.authentication, enemyClient.getName(), AggroSource.AUTO));
        } catch (Throwable ignored) {
        }
    }

    /** Adds anger to every party member of {@code owner}, including the enemy's own party members as derived enemies. */
    public static int retaliate(ServerClient owner, Mob enemyMob) {
        if (owner == null || enemyMob == null) {
            return 0;
        }
        AggroServerSection section = AggroServerState.get(owner.authentication);
        if (section == null || !section.enabled) {
            return 0;
        }
        Collection<HumanMob> party = owner.adventureParty.getMobs();
        if (party == null || party.isEmpty()) {
            Debug.log("retaliate " + owner.getName() + ": party empty");
            return 0;
        }
        Debug.log("retaliate " + owner.getName() + ": party=" + party.size());

        Set<Mob> derivedEnemies = new HashSet<Mob>();
        if (enemyMob instanceof PlayerMob) {
            ServerClient enemyClient = ((PlayerMob) enemyMob).getServerClient();
            if (enemyClient != null) {
                Collection<HumanMob> enemyParty = enemyClient.adventureParty.getMobs();
                if (enemyParty != null) {
                    derivedEnemies.addAll(enemyParty);
                }
            }
        }

        int angered = 0;
        int noHandler = 0;
        for (HumanMob member : party) {
            if (member == null || member.removed()) {
                continue;
            }
            if (!member.canTarget(enemyMob)) {
                continue; // never attack a friendly target (e.g. the party owner)
            }
            HumanAngerTargetAINode<?> handler = member.ai.blackboard
                    .getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
            if (handler == null) {
                noHandler++;
                continue;
            }
            handler.addAnger(ANGER_AMOUNT, enemyMob, true);
            for (Mob derived : derivedEnemies) {
                if (derived != null && !derived.removed() && derived != member && member.canTarget(derived)) {
                    handler.addEnemy(derived, ANGER_AMOUNT);
                }
            }
            angered++;
        }
        if (noHandler > 0) {
            Debug.log("retaliate " + owner.getName() + ": " + noHandler + " members had no anger handler");
        }
        return angered;
    }

    /**
     * Removes enemies that are no longer valid for the owner (whitelisted, unset, self, or the
     * whole system disabled) so switching a player to whitelist/unset stops the villagers.
     */
    public static void pruneEnemies(ServerClient owner) {
        if (owner == null) {
            return;
        }
        try {
            AggroServerSection section = AggroServerState.get(owner.authentication);
            boolean active = section != null && section.enabled
                    && !AggroServerState.isRuntimeDisabled(owner.authentication);
            Set<Long> allowed = active ? section.effectiveHatred() : Collections.<Long>emptySet();
            Collection<HumanMob> party = owner.adventureParty.getMobs();
            if (party == null) {
                return;
            }
            for (HumanMob member : party) {
                if (member == null || member.removed()) {
                    continue;
                }
                HumanAngerTargetAINode<?> handler = member.ai.blackboard
                        .getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
                if (handler == null) {
                    continue;
                }
                ArrayList<Mob> remove = new ArrayList<Mob>();
                for (Mob enemy : handler.enemies) {
                    if (!isAllowedEnemy(owner, allowed, enemy)) {
                        remove.add(enemy);
                    }
                }
                if (remove.isEmpty()) {
                    continue;
                }
                handler.enemies.removeAll(remove);
                Mob current = member.ai.blackboard.getObject(Mob.class, "currentTarget");
                if (current != null && remove.contains(current)) {
                    member.ai.blackboard.put("currentTarget", null);
                    member.ai.blackboard.submitEvent("resetTarget", new AIEvent());
                    member.ai.blackboard.mover.stopMoving(member);
                }
                if (handler.enemies.isEmpty()) {
                    try {
                        member.buffManager.removeBuff(BuffRegistry.HUMAN_ANGRY, true);
                    } catch (Throwable ignored) {
                    }
                }
                Debug.log("pruned " + remove.size() + " enemies for owner=" + owner.getName());
            }
        } catch (Throwable t) {
            Debug.log("pruneEnemies error: " + t);
        }
    }

    private static boolean isAllowedEnemy(ServerClient owner, Set<Long> allowed, Mob enemy) {
        long auth = -1L;
        if (enemy instanceof PlayerMob) {
            ServerClient ec = ((PlayerMob) enemy).getServerClient();
            if (ec != null) {
                auth = ec.authentication;
            }
        } else if (enemy instanceof HumanMob) {
            ServerClient eo = ((HumanMob) enemy).adventureParty.getServerClient();
            if (eo != null) {
                auth = eo.authentication;
            }
        }
        return auth >= 0L && auth != owner.authentication && allowed.contains(auth);
    }

    /** Clears active anger for all of a party owner's members (used by the runtime hotkey toggle). */
    public static void clearAnger(ServerClient owner) {
        if (owner == null) {
            return;
        }
        try {
            Collection<HumanMob> party = owner.adventureParty.getMobs();
            if (party == null) {
                return;
            }
            int cleared = 0;
            for (HumanMob member : party) {
                if (member == null || member.removed()) {
                    continue;
                }
                HumanAngerTargetAINode<?> handler = member.ai.blackboard
                        .getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
                if (handler != null) {
                    handler.enemies.clear();
                    handler.anger = 0.0f;
                }
                // Also drop the AI's current target and the angry buff, otherwise the
                // attack behaviour keeps running on the already-set blackboard target.
                try {
                    member.ai.blackboard.put("currentTarget", null);
                    member.ai.blackboard.submitEvent("resetTarget", new AIEvent());
                } catch (Throwable ignored) {
                }
                try {
                    member.ai.blackboard.mover.stopMoving(member);
                } catch (Throwable ignored) {
                }
                try {
                    member.buffManager.removeBuff(BuffRegistry.HUMAN_ANGRY, true);
                } catch (Throwable ignored) {
                }
                cleared++;
            }
            Debug.log("clearAnger " + owner.getName() + " (" + cleared + " mobs)");
        } catch (Throwable t) {
            Debug.log("error clearing anger: " + t);
        }
    }
}
