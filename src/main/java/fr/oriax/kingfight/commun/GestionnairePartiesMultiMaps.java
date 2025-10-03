package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.getdown.GestionnaireSetupGetDown;
import fr.oriax.kingfight.jumpleague.GestionnaireSetupJumpLeague;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GestionnairePartiesMultiMaps {

    private final KingFight plugin;
    private final GestionnaireDuplicationMaps gestionnaireDuplication;
    private final GestionnaireTeleportationMultiMaps gestionnaireTeleportation;

    public GestionnairePartiesMultiMaps(KingFight plugin) {
        this.plugin = plugin;
        this.gestionnaireDuplication = new GestionnaireDuplicationMaps(plugin);
        this.gestionnaireTeleportation = new GestionnaireTeleportationMultiMaps(plugin);

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            gestionnaireDuplication.nettoyerPartiesExpirees();
            gestionnaireTeleportation.nettoyerTransitionsExpirees();
        }, 20L * 60 * 5, 20L * 60 * 5);
    }

    public boolean preparerPartieGetDownMultiMaps(String idPartie, List<Player> joueurs) {
        GestionnaireSetupGetDown.ConfigurationMultiMaps config =
            plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                  .getGestionnaireSetup().getConfigurationMultiMaps();

        if (!config.estComplet()) {
            plugin.getLogger().warning("Configuration multi-maps GetDown incomplète pour la partie " + idPartie);
            return false;
        }

        List<String> mapsNecessaires = new ArrayList<>();
        mapsNecessaires.add(config.getMapParcours1());
        mapsNecessaires.add(config.getMapParcours2());
        mapsNecessaires.add(config.getMapParcours3());

        if (config.aPhasePvp()) {
            mapsNecessaires.add(config.getMapPvp());
        }

        GestionnaireDuplicationMaps.ConfigurationPartieMultiMaps configPartie =
            gestionnaireDuplication.creerConfigurationPartie(idPartie, TypeMiniJeu.GET_DOWN, mapsNecessaires);

        for (String mapOriginale : mapsNecessaires) {
            if (configPartie.obtenirMapTemporaire(mapOriginale) == null) {
                plugin.getLogger().severe("Échec de création de map temporaire pour " + mapOriginale);
                gestionnaireDuplication.libererMapsPartie(idPartie);
                return false;
            }
        }

        plugin.getLogger().info("Partie GetDown multi-maps préparée: " + idPartie);
        return true;
    }

    public boolean preparerPartieJumpLeagueMultiMaps(String idPartie, List<Player> joueurs) {
        GestionnaireSetupJumpLeague.ConfigurationMultiMaps config =
            plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                  .getGestionnaireSetup().getConfigurationMultiMaps();

        if (!config.estComplet()) {
            plugin.getLogger().warning("Configuration multi-maps JumpLeague incomplète pour la partie " + idPartie);
            return false;
        }

        List<String> mapsNecessaires = new ArrayList<>();
        mapsNecessaires.add(config.getMapParcours());

        if (config.aPhasePvp()) {
            mapsNecessaires.add(config.getMapPvp());
        }

        GestionnaireDuplicationMaps.ConfigurationPartieMultiMaps configPartie =
            gestionnaireDuplication.creerConfigurationPartie(idPartie, TypeMiniJeu.JUMP_LEAGUE, mapsNecessaires);

        for (String mapOriginale : mapsNecessaires) {
            if (configPartie.obtenirMapTemporaire(mapOriginale) == null) {
                plugin.getLogger().severe("Échec de création de map temporaire pour " + mapOriginale);
                gestionnaireDuplication.libererMapsPartie(idPartie);
                return false;
            }
        }

        plugin.getLogger().info("Partie JumpLeague multi-maps préparée: " + idPartie);
        return true;
    }

    public void demarrerPartieGetDownMultiMaps(String idPartie, List<Player> joueurs) {
        GestionnaireDuplicationMaps.ConfigurationPartieMultiMaps configPartie =
            gestionnaireDuplication.obtenirConfigurationPartie(idPartie);

        if (configPartie == null) {
            plugin.getLogger().severe("Configuration de partie introuvable: " + idPartie);
            return;
        }

        GestionnaireSetupGetDown.ConfigurationMultiMaps config =
            plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                  .getGestionnaireSetup().getConfigurationMultiMaps();

        String mapTemporaireParcours1 = configPartie.obtenirMapTemporaire(config.getMapParcours1());
        if (mapTemporaireParcours1 == null) {
            plugin.getLogger().severe("Map temporaire du parcours 1 introuvable pour la partie " + idPartie);
            return;
        }

        World mondeParcours1 = plugin.getServer().getWorld(mapTemporaireParcours1);
        if (mondeParcours1 == null) {
            plugin.getLogger().severe("Monde temporaire introuvable: " + mapTemporaireParcours1);
            return;
        }

        GestionnaireSetupGetDown.SetupMonde setup =
            plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                  .getGestionnaireSetup().obtenirSetup(config.getMapParcours1());

        if (setup == null) {
            plugin.getLogger().severe("Setup introuvable pour la map " + config.getMapParcours1());
            return;
        }

        List<Location> spawns = setup.getSpawnsJoueurs();
        List<Location> spawnsTemporaires = new ArrayList<>();

        for (Location spawn : spawns) {
            Location spawnTemporaire = new Location(
                mondeParcours1,
                spawn.getX(),
                spawn.getY(),
                spawn.getZ(),
                spawn.getYaw(),
                spawn.getPitch()
            );
            spawnsTemporaires.add(spawnTemporaire);
        }

        gestionnaireTeleportation.teleporterGroupe(
            joueurs,
            mapTemporaireParcours1,
            spawnsTemporaires,
            GestionnaireTeleportationMultiMaps.TypeTransition.PARCOURS_SUIVANT
        );

        plugin.getLogger().info("Partie GetDown multi-maps démarrée: " + idPartie + " sur " + mapTemporaireParcours1);
    }

    public void demarrerPartieJumpLeagueMultiMaps(String idPartie, List<Player> joueurs) {
        GestionnaireDuplicationMaps.ConfigurationPartieMultiMaps configPartie =
            gestionnaireDuplication.obtenirConfigurationPartie(idPartie);

        if (configPartie == null) {
            plugin.getLogger().severe("Configuration de partie introuvable: " + idPartie);
            return;
        }

        GestionnaireSetupJumpLeague.ConfigurationMultiMaps config =
            plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                  .getGestionnaireSetup().getConfigurationMultiMaps();

        String mapTemporaireParcours = configPartie.obtenirMapTemporaire(config.getMapParcours());
        if (mapTemporaireParcours == null) {
            plugin.getLogger().severe("Map temporaire du parcours introuvable pour la partie " + idPartie);
            return;
        }

        World mondeParcours = plugin.getServer().getWorld(mapTemporaireParcours);
        if (mondeParcours == null) {
            plugin.getLogger().severe("Monde temporaire introuvable: " + mapTemporaireParcours);
            return;
        }

        GestionnaireSetupJumpLeague.SetupMonde setup =
            plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                  .getGestionnaireSetup().obtenirSetup(config.getMapParcours());

        if (setup == null) {
            plugin.getLogger().severe("Setup introuvable pour la map " + config.getMapParcours());
            return;
        }

        List<Location> spawnsTemporaires = new ArrayList<>();

        for (int i = 1; i <= joueurs.size() && i <= 16; i++) {
            Location spawn = setup.getSpawnParcours(i);
            if (spawn != null) {
                Location spawnTemporaire = new Location(
                    mondeParcours,
                    spawn.getX(),
                    spawn.getY(),
                    spawn.getZ(),
                    spawn.getYaw(),
                    spawn.getPitch()
                );
                spawnsTemporaires.add(spawnTemporaire);
            }
        }

        gestionnaireTeleportation.teleporterGroupe(
            joueurs,
            mapTemporaireParcours,
            spawnsTemporaires,
            GestionnaireTeleportationMultiMaps.TypeTransition.PARCOURS_SUIVANT
        );

        plugin.getLogger().info("Partie JumpLeague multi-maps démarrée: " + idPartie + " sur " + mapTemporaireParcours);
    }

    public void transitionVersParcoursGetDown(String idPartie, Player joueur, int numeroParcours) {
        if (numeroParcours < 2 || numeroParcours > 3) {
            return;
        }

        GestionnaireDuplicationMaps.ConfigurationPartieMultiMaps configPartie =
            gestionnaireDuplication.obtenirConfigurationPartie(idPartie);

        if (configPartie == null) {
            return;
        }

        GestionnaireSetupGetDown.ConfigurationMultiMaps config =
            plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                  .getGestionnaireSetup().getConfigurationMultiMaps();

        String mapOriginaleDestination = numeroParcours == 2 ? config.getMapParcours2() : config.getMapParcours3();
        String mapTemporaireDestination = configPartie.obtenirMapTemporaire(mapOriginaleDestination);

        if (mapTemporaireDestination == null) {
            plugin.getLogger().severe("Map temporaire introuvable pour le parcours " + numeroParcours);
            return;
        }

        GestionnaireSetupGetDown.SetupMonde setup =
            plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                  .getGestionnaireSetup().obtenirSetup(mapOriginaleDestination);

        if (setup == null || setup.getSpawnsDescente().size() < numeroParcours) {
            plugin.getLogger().severe("Setup ou spawn introuvable pour le parcours " + numeroParcours);
            return;
        }

        Location spawnOriginal = setup.getSpawnsDescente().get(numeroParcours - 1);
        World mondeDestination = plugin.getServer().getWorld(mapTemporaireDestination);

        if (mondeDestination == null) {
            plugin.getLogger().severe("Monde de destination introuvable: " + mapTemporaireDestination);
            return;
        }

        Location spawnDestination = new Location(
            mondeDestination,
            spawnOriginal.getX(),
            spawnOriginal.getY(),
            spawnOriginal.getZ(),
            spawnOriginal.getYaw(),
            spawnOriginal.getPitch()
        );

        gestionnaireTeleportation.teleporterVersParcoursGetDown(
            joueur,
            mapTemporaireDestination,
            spawnDestination,
            numeroParcours
        );
    }

    public void transitionVersPhasePvp(String idPartie, Player joueur, TypeMiniJeu typeMiniJeu) {
        GestionnaireDuplicationMaps.ConfigurationPartieMultiMaps configPartie =
            gestionnaireDuplication.obtenirConfigurationPartie(idPartie);

        if (configPartie == null) {
            return;
        }

        String mapOriginaleDestination = null;
        GestionnaireSetupGetDown.SetupMonde setupGetDown = null;
        GestionnaireSetupJumpLeague.SetupMonde setupJumpLeague = null;

        if (typeMiniJeu == TypeMiniJeu.GET_DOWN) {
            GestionnaireSetupGetDown.ConfigurationMultiMaps config =
                plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                      .getGestionnaireSetup().getConfigurationMultiMaps();

            if (!config.aPhasePvp()) {
                return;
            }

            mapOriginaleDestination = config.getMapPvp();
            setupGetDown = plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                                 .getGestionnaireSetup().obtenirSetup(mapOriginaleDestination);

        } else if (typeMiniJeu == TypeMiniJeu.JUMP_LEAGUE) {
            GestionnaireSetupJumpLeague.ConfigurationMultiMaps config =
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                      .getGestionnaireSetup().getConfigurationMultiMaps();

            if (!config.aPhasePvp()) {
                return;
            }

            mapOriginaleDestination = config.getMapPvp();
            setupJumpLeague = plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                                    .getGestionnaireSetup().obtenirSetup(mapOriginaleDestination);
        }

        if (mapOriginaleDestination == null) {
            return;
        }

        String mapTemporaireDestination = configPartie.obtenirMapTemporaire(mapOriginaleDestination);
        if (mapTemporaireDestination == null) {
            plugin.getLogger().severe("Map temporaire PvP introuvable");
            return;
        }

        Location spawnOriginal = null;

        if (setupGetDown != null && !setupGetDown.getSpawnsPvp().isEmpty()) {
            spawnOriginal = setupGetDown.getSpawnsPvp().get(0);
        } else if (setupJumpLeague != null && !setupJumpLeague.getSpawnsPvp().isEmpty()) {
            spawnOriginal = setupJumpLeague.getSpawnsPvp().values().iterator().next();
        }

        if (spawnOriginal == null) {
            plugin.getLogger().severe("Aucun spawn PvP configuré");
            return;
        }

        World mondeDestination = plugin.getServer().getWorld(mapTemporaireDestination);
        if (mondeDestination == null) {
            plugin.getLogger().severe("Monde PvP de destination introuvable: " + mapTemporaireDestination);
            return;
        }

        Location spawnDestination = new Location(
            mondeDestination,
            spawnOriginal.getX(),
            spawnOriginal.getY(),
            spawnOriginal.getZ(),
            spawnOriginal.getYaw(),
            spawnOriginal.getPitch()
        );

        gestionnaireTeleportation.teleporterVersPhasePvp(
            joueur,
            mapTemporaireDestination,
            spawnDestination,
            typeMiniJeu
        );
    }

    public void terminerPartie(String idPartie) {
        gestionnaireDuplication.libererMapsPartie(idPartie);
        plugin.getLogger().info("Partie multi-maps terminée: " + idPartie);
    }

    public boolean estPartieMultiMaps(String idPartie) {
        return gestionnaireDuplication.estPartieMultiMaps(idPartie);
    }

    public GestionnaireDuplicationMaps getGestionnaireDuplication() {
        return gestionnaireDuplication;
    }

    public GestionnaireTeleportationMultiMaps getGestionnaireTeleportation() {
        return gestionnaireTeleportation;
    }
}
