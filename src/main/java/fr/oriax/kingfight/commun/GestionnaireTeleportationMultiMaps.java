package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GestionnaireTeleportationMultiMaps {

    private final KingFight plugin;
    private final Map<UUID, TransitionEnCours> transitionsEnCours;

    public GestionnaireTeleportationMultiMaps(KingFight plugin) {
        this.plugin = plugin;
        this.transitionsEnCours = new HashMap<>();
    }

    public static class TransitionEnCours {
        private final String mapDestination;
        private final Location locationDestination;
        private final TypeTransition typeTransition;
        private final long tempsDebut;

        public TransitionEnCours(String mapDestination, Location locationDestination, TypeTransition typeTransition) {
            this.mapDestination = mapDestination;
            this.locationDestination = locationDestination;
            this.typeTransition = typeTransition;
            this.tempsDebut = System.currentTimeMillis();
        }

        public String getMapDestination() { return mapDestination; }
        public Location getLocationDestination() { return locationDestination; }
        public TypeTransition getTypeTransition() { return typeTransition; }
        public long getTempsDebut() { return tempsDebut; }
    }

    public enum TypeTransition {
        PARCOURS_SUIVANT,
        PHASE_PVP,
        FIN_PARTIE
    }

    public void teleporterVersMap(Player joueur, String mapDestination, Location locationDestination, TypeTransition typeTransition) {
        UUID joueurId = joueur.getUniqueId();

        if (transitionsEnCours.containsKey(joueurId)) {
            return;
        }

        TransitionEnCours transition = new TransitionEnCours(mapDestination, locationDestination, typeTransition);
        transitionsEnCours.put(joueurId, transition);

        afficherEffetTransition(joueur, typeTransition);

        new BukkitRunnable() {
            @Override
            public void run() {
                World mondeDestination = plugin.getServer().getWorld(mapDestination);
                if (mondeDestination == null) {
                    joueur.sendMessage("§cErreur : Impossible de charger la map " + mapDestination);
                    transitionsEnCours.remove(joueurId);
                    return;
                }

                Location locationFinale = new Location(
                    mondeDestination,
                    locationDestination.getX(),
                    locationDestination.getY(),
                    locationDestination.getZ(),
                    locationDestination.getYaw(),
                    locationDestination.getPitch()
                );

                joueur.teleport(locationFinale);

                envoyerMessageTransition(joueur, typeTransition, mapDestination);

                transitionsEnCours.remove(joueurId);
            }
        }.runTaskLater(plugin, 20L);
    }

    public void teleporterVersParcoursGetDown(Player joueur, String mapParcours, Location spawnParcours, int numeroParcours) {
        String message = numeroParcours == 2 ? "Passage au deuxième parcours !" : "Passage au troisième parcours !";
        joueur.sendMessage("§a§l» " + message);

        teleporterVersMap(joueur, mapParcours, spawnParcours, TypeTransition.PARCOURS_SUIVANT);
    }

    public void teleporterVersPhasePvp(Player joueur, String mapPvp, Location spawnPvp, TypeMiniJeu typeMiniJeu) {
        String nomJeu = typeMiniJeu == TypeMiniJeu.GET_DOWN ? "GetDown" : "JumpLeague";
        joueur.sendMessage("§c§l» Passage à la phase PvP " + nomJeu + " !");

        teleporterVersMap(joueur, mapPvp, spawnPvp, TypeTransition.PHASE_PVP);
    }

    private void afficherEffetTransition(Player joueur, TypeTransition typeTransition) {
        switch (typeTransition) {
            case PARCOURS_SUIVANT:
                joueur.sendMessage("§e§l────────────────────────────────────────────────");
                joueur.sendMessage("§6§l                    CHANGEMENT DE MAP");
                joueur.sendMessage("§e§l────────────────────────────────────────────────");
                break;

            case PHASE_PVP:
                joueur.sendMessage("§c§l────────────────────────────────────────────────");
                joueur.sendMessage("§4§l                      PHASE PVP");
                joueur.sendMessage("§c§l────────────────────────────────────────────────");
                break;

            case FIN_PARTIE:
                joueur.sendMessage("§a§l────────────────────────────────────────────────");
                joueur.sendMessage("§2§l                    FIN DE PARTIE");
                joueur.sendMessage("§a§l────────────────────────────────────────────────");
                break;
        }

        try {
            joueur.playSound(joueur.getLocation(), org.bukkit.Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } catch (Exception e) {
            try {
                joueur.playSound(joueur.getLocation(), org.bukkit.Sound.valueOf("ENTITY_ENDERMAN_TELEPORT"), 1.0f, 1.0f);
            } catch (Exception ex) {
            }
        }
    }

    private void envoyerMessageTransition(Player joueur, TypeTransition typeTransition, String mapDestination) {
        switch (typeTransition) {
            case PARCOURS_SUIVANT:
                joueur.sendMessage("§a✓ Téléporté vers le parcours suivant !");
                joueur.sendMessage("§7Map: " + mapDestination);
                break;

            case PHASE_PVP:
                joueur.sendMessage("§c✓ Téléporté vers la zone PvP !");
                joueur.sendMessage("§7Map: " + mapDestination);
                joueur.sendMessage("§e⚠ Préparez-vous au combat !");
                break;

            case FIN_PARTIE:
                joueur.sendMessage("§a✓ Partie terminée !");
                break;
        }

        try {
            joueur.playSound(joueur.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.5f, 1.2f);
        } catch (Exception e) {
            try {
                joueur.playSound(joueur.getLocation(), org.bukkit.Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 0.5f, 1.2f);
            } catch (Exception ex) {
            }
        }
    }

    public boolean estEnTransition(Player joueur) {
        return transitionsEnCours.containsKey(joueur.getUniqueId());
    }

    public void annulerTransition(Player joueur) {
        transitionsEnCours.remove(joueur.getUniqueId());
    }

    public void nettoyerTransitionsExpirees() {
        long maintenant = System.currentTimeMillis();
        transitionsEnCours.entrySet().removeIf(entry ->
            maintenant - entry.getValue().getTempsDebut() > 30000
        );
    }

    public void teleporterGroupe(java.util.List<Player> joueurs, String mapDestination, java.util.List<Location> spawns, TypeTransition typeTransition) {
        for (int i = 0; i < joueurs.size() && i < spawns.size(); i++) {
            Player joueur = joueurs.get(i);
            Location spawn = spawns.get(i);

            new BukkitRunnable() {
                @Override
                public void run() {
                    teleporterVersMap(joueur, mapDestination, spawn, typeTransition);
                }
            }.runTaskLater(plugin, i * 2L);
        }
    }
}