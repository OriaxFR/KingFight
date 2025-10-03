package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GestionnaireMaps {

    private final KingFight plugin;
    private final Map<String, String> mapsTemporaires;
    private final Map<TypeMiniJeu, List<String>> mapsParMiniJeu;
    private FileConfiguration configMaps;
    private File fichierConfigMaps;

    public GestionnaireMaps(KingFight plugin) {
        this.plugin = plugin;
        this.mapsTemporaires = new ConcurrentHashMap<>();
        this.mapsParMiniJeu = new HashMap<>();

        chargerConfigurationMaps();
        initialiserMapsParMiniJeu();
    }

    private void chargerConfigurationMaps() {
        fichierConfigMaps = new File(plugin.getDataFolder(), "maps.yml");

        if (!fichierConfigMaps.exists()) {
            creerConfigurationMapsParDefaut();
        }

        configMaps = YamlConfiguration.loadConfiguration(fichierConfigMaps);
    }

    private void creerConfigurationMapsParDefaut() {
        try {
            fichierConfigMaps.createNewFile();
            configMaps = YamlConfiguration.loadConfiguration(fichierConfigMaps);

            configMaps.set("lobby.monde", "world");
            configMaps.set("lobby.spawn.x", 0.0);
            configMaps.set("lobby.spawn.y", 100.0);
            configMaps.set("lobby.spawn.z", 0.0);

            configMaps.set("hungergames.maps", new ArrayList<String>());
            configMaps.set("jumpleague.maps", new ArrayList<String>());
            configMaps.set("getdown.maps", new ArrayList<String>());

            configMaps.set("duplication.dossier-temporaire", "temp_maps");
            configMaps.set("duplication.nettoyer-au-demarrage", true);
            configMaps.set("duplication.delai-nettoyage-minutes", 5);

            configMaps.save(fichierConfigMaps);

        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de creer la configuration des maps: " + e.getMessage());
        }
    }

    private void initialiserMapsParMiniJeu() {
        mapsParMiniJeu.put(TypeMiniJeu.HUNGER_GAMES,
            new ArrayList<>(configMaps.getStringList("hungergames.maps")));
        mapsParMiniJeu.put(TypeMiniJeu.JUMP_LEAGUE,
            new ArrayList<>(configMaps.getStringList("jumpleague.maps")));
        mapsParMiniJeu.put(TypeMiniJeu.GET_DOWN,
            new ArrayList<>(configMaps.getStringList("getdown.maps")));

        nettoyerMapsInexistantes();

        if (configMaps.getBoolean("duplication.nettoyer-au-demarrage", true)) {
            nettoyerMapsTemporaires();
        }
    }

    public String obtenirMapAleatoire(TypeMiniJeu miniJeu) {
        List<String> mapsDisponibles = mapsParMiniJeu.get(miniJeu);

        if (mapsDisponibles == null || mapsDisponibles.isEmpty()) {
            plugin.getLogger().warning("Aucune map configuree pour " + miniJeu.name());
            return null;
        }

        return mapsDisponibles.get(new Random().nextInt(mapsDisponibles.size()));
    }

    public World creerMapTemporaire(String nomMapOriginale, String idPartie) {
        String nomMapTemporaire = genererNomMapTemporaire(nomMapOriginale, idPartie);
        plugin.getLogger().info("Tentative de creation de map temporaire: " + nomMapTemporaire);

        try {
            if (dupliquerMonde(nomMapOriginale, nomMapTemporaire)) {
                plugin.getLogger().info("Duplication reussie, chargement du monde...");
                World mondeTemporaire = chargerMonde(nomMapTemporaire);

                if (mondeTemporaire != null) {
                    mapsTemporaires.put(idPartie, nomMapTemporaire);

                    configurerMondeTemporaire(mondeTemporaire);
                    
                    plugin.getLogger().info("Map temporaire creee avec succes: " + nomMapTemporaire + " pour la partie " + idPartie);
                    return mondeTemporaire;
                } else {
                    plugin.getLogger().severe("Echec du chargement du monde temporaire: " + nomMapTemporaire);
                }
            } else {
                plugin.getLogger().severe("Echec de la duplication du monde: " + nomMapOriginale);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la creation de la map temporaire: " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getLogger().warning("ATTENTION: Utilisation de la map originale: " + nomMapOriginale + " - Les modifications seront permanentes!");
        World mondeOriginal = Bukkit.getWorld(nomMapOriginale);

        if (mondeOriginal != null) {
            configurerMondeTemporaire(mondeOriginal);
        }
        
        return mondeOriginal;
    }

    private void configurerMondeTemporaire(World monde) {
        monde.setTime(6000);
        monde.setStorm(false);
        monde.setThundering(false);
        monde.setWeatherDuration(0);

        monde.setGameRuleValue("doDaylightCycle", "false");

        monde.setDifficulty(Difficulty.PEACEFUL);

        monde.setGameRuleValue("doMobSpawning", "false");

        monde.setGameRuleValue("keepInventory", "false");
        monde.setGameRuleValue("doFireTick", "false");
        monde.setGameRuleValue("mobGriefing", "false");
        monde.setGameRuleValue("doTileDrops", "true");
        monde.setGameRuleValue("naturalRegeneration", "true");
        
        plugin.getLogger().info("Monde temporaire " + monde.getName() + " configuré automatiquement en mode Peaceful avec jour permanent");
    }

    private String genererNomMapTemporaire(String nomMapOriginale, String idPartie) {
        return configMaps.getString("duplication.dossier-temporaire", "temp_maps") +
               "_" + nomMapOriginale + "_" + idPartie;
    }

    private boolean dupliquerMonde(String nomSource, String nomDestination) {
        try {
            File dossierServeur = plugin.getServer().getWorldContainer();
            File mondeSource = new File(dossierServeur, nomSource);
            File mondeDestination = new File(dossierServeur, nomDestination);

            if (!mondeSource.exists()) {
                plugin.getLogger().warning("Monde source introuvable: " + nomSource);
                return false;
            }

            if (mondeDestination.exists()) {
                supprimerDossierRecursivement(mondeDestination);
            }

            copierDossierRecursivement(mondeSource.toPath(), mondeDestination.toPath());

            supprimerFichiersSession(mondeDestination);

            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la duplication du monde " + nomSource + ": " + e.getMessage());
            return false;
        }
    }

    private void copierDossierRecursivement(Path source, Path destination) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path destinationPath = destination.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(destinationPath);
                } else {
                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur lors de la copie de " + sourcePath + ": " + e.getMessage());
            }
        });
    }

    private void supprimerFichiersSession(File dossierMonde) {
        File[] fichiersSession = {
            new File(dossierMonde, "session.lock"),
            new File(dossierMonde, "uid.dat")
        };

        for (File fichier : fichiersSession) {
            if (fichier.exists()) {
                fichier.delete();
            }
        }
    }

    private World chargerMonde(String nomMonde) {
        try {
            WorldCreator creator = new WorldCreator(nomMonde);
            return creator.createWorld();
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du chargement du monde " + nomMonde + ": " + e.getMessage());
            return null;
        }
    }

    public void supprimerMapTemporaire(String idPartie) {
        String nomMapTemporaire = mapsTemporaires.remove(idPartie);

        if (nomMapTemporaire != null) {
            supprimerMapTemporaireParNom(nomMapTemporaire);
        }
    }

    public void supprimerMapTemporaireParNom(String nomMapTemporaire) {
        supprimerMapTemporaireParNom(nomMapTemporaire, false);
    }

    public void supprimerMapTemporaireParNom(String nomMapTemporaire, boolean synchrone) {
        World monde = Bukkit.getWorld(nomMapTemporaire);

        if (monde != null) {
            for (org.bukkit.entity.Player joueur : monde.getPlayers()) {
                Location lobby = plugin.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
                if (lobby != null) {
                    joueur.teleport(lobby);
                } else {
                    joueur.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                }
                plugin.getGestionnairePrincipal().getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
                joueur.sendMessage("§c» Vous avez été téléporté au lobby car la partie est terminée.");
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (!monde.getPlayers().isEmpty()) {
                        plugin.getLogger().warning("Forçage de la téléportation des joueurs restants dans " + nomMapTemporaire);
                        for (org.bukkit.entity.Player joueur : new ArrayList<>(monde.getPlayers())) {
                            Location lobby = plugin.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
                            if (lobby != null) {
                                joueur.teleport(lobby);
                            } else {
                                joueur.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                            }
                            plugin.getGestionnairePrincipal().getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
                        }
                    }
                    
                    Bukkit.unloadWorld(monde, true);
                    plugin.getLogger().info("Monde temporaire decharge: " + nomMapTemporaire);
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du dechargement du monde " + nomMapTemporaire + ": " + e.getMessage());
                    try {
                        Bukkit.unloadWorld(monde, false);
                        plugin.getLogger().info("Monde temporaire decharge (sans sauvegarde): " + nomMapTemporaire);
                    } catch (Exception e2) {
                        plugin.getLogger().severe("Impossible de decharger le monde " + nomMapTemporaire + ": " + e2.getMessage());
                    }
                }
            }, 20L);
        }

        if (synchrone || !plugin.isEnabled()) {
            File dossierMonde = new File(plugin.getServer().getWorldContainer(), nomMapTemporaire);
            if (dossierMonde.exists()) {
                supprimerDossierRecursivement(dossierMonde);
                plugin.getLogger().info("Map temporaire supprimee: " + nomMapTemporaire);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                File dossierMonde = new File(plugin.getServer().getWorldContainer(), nomMapTemporaire);
                if (dossierMonde.exists()) {
                    try {
                        supprimerDossierRecursivement(dossierMonde);
                        plugin.getLogger().info("Map temporaire supprimee: " + nomMapTemporaire);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erreur lors de la suppression de la map " + nomMapTemporaire + ": " + e.getMessage());
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            try {
                                supprimerDossierRecursivement(dossierMonde);
                                plugin.getLogger().info("Map temporaire supprimee (2e tentative): " + nomMapTemporaire);
                            } catch (Exception e2) {
                                plugin.getLogger().severe("Impossible de supprimer la map " + nomMapTemporaire + " apres 2 tentatives: " + e2.getMessage());
                            }
                        }, 100L);
                    }
                }
            }, 100L);
        }
    }

    private void supprimerDossierRecursivement(File dossier) {
        if (dossier.exists()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (fichier.isDirectory()) {
                        supprimerDossierRecursivement(fichier);
                    } else {
                        if (!fichier.delete()) {
                            fichier.deleteOnExit();
                            plugin.getLogger().warning("Fichier verrouille, suppression reportee: " + fichier.getName());
                        }
                    }
                }
            }
            if (!dossier.delete()) {
                dossier.deleteOnExit();
                plugin.getLogger().warning("Dossier verrouille, suppression reportee: " + dossier.getName());
            }
        }
    }

    public void nettoyerMapsTemporaires() {
        File dossierServeur = plugin.getServer().getWorldContainer();
        String prefixeTemp = configMaps.getString("duplication.dossier-temporaire", "temp_maps");

        File[] dossiers = dossierServeur.listFiles();
        if (dossiers != null) {
            for (File dossier : dossiers) {
                if (dossier.isDirectory() && dossier.getName().startsWith(prefixeTemp)) {
                    World monde = Bukkit.getWorld(dossier.getName());
                    if (monde != null) {
                        Bukkit.unloadWorld(monde, false);
                    }
                    supprimerDossierRecursivement(dossier);
                    plugin.getLogger().info("Map temporaire nettoyee: " + dossier.getName());
                }
            }
        }
    }

    public org.bukkit.Location obtenirSpawnLobby() {
        String nomMonde = configMaps.getString("lobby.monde", "world");
        World monde = Bukkit.getWorld(nomMonde);

        if (monde == null) {
            monde = Bukkit.getWorlds().get(0);
        }

        double x = configMaps.getDouble("lobby.spawn.x", 0.0);
        double y = configMaps.getDouble("lobby.spawn.y", 100.0);
        double z = configMaps.getDouble("lobby.spawn.z", 0.0);

        return new org.bukkit.Location(monde, x, y, z);
    }

    public void definirSpawnLobby(org.bukkit.Location location) {
        configMaps.set("lobby.monde", location.getWorld().getName());
        configMaps.set("lobby.spawn.x", location.getX());
        configMaps.set("lobby.spawn.y", location.getY());
        configMaps.set("lobby.spawn.z", location.getZ());

        sauvegarderConfiguration();
    }

    public boolean ajouterMapPourMiniJeu(TypeMiniJeu miniJeu, String nomMap) {
        if (!verifierExistenceMap(nomMap)) {
            return false;
        }

        List<String> maps = mapsParMiniJeu.get(miniJeu);
        if (maps != null && !maps.contains(nomMap)) {
            maps.add(nomMap);

            String cleMiniJeu = miniJeu.name().toLowerCase().replace("_", "");
            configMaps.set(cleMiniJeu + ".maps", maps);
            sauvegarderConfiguration();

            plugin.getLogger().info("Map " + nomMap + " ajoutee pour " + miniJeu.name());
            return true;
        }
        return false;
    }

    public void retirerMapPourMiniJeu(TypeMiniJeu miniJeu, String nomMap) {
        plugin.getGestionnairePrincipal().getGestionnaireBackupAutomatique()
                .sauvegarderSetupAvantSuppression(nomMap, miniJeu);
        
        List<String> maps = mapsParMiniJeu.get(miniJeu);
        if (maps != null && maps.remove(nomMap)) {
            String cleMiniJeu = miniJeu.name().toLowerCase().replace("_", "");
            configMaps.set(cleMiniJeu + ".maps", maps);
            sauvegarderConfiguration();

            plugin.getLogger().info("Map " + nomMap + " retiree pour " + miniJeu.name());
        }
    }

    public List<String> obtenirMapsDisponibles(TypeMiniJeu miniJeu) {
        return new ArrayList<>(mapsParMiniJeu.getOrDefault(miniJeu, new ArrayList<>()));
    }

    public boolean estMapDisponible(TypeMiniJeu miniJeu, String nomMap) {
        List<String> maps = mapsParMiniJeu.get(miniJeu);
        return maps != null && maps.contains(nomMap);
    }

    public boolean verifierExistenceMap(String nomMap) {
        File dossierServeur = plugin.getServer().getWorldContainer();
        File dossierMap = new File(dossierServeur, nomMap);

        if (!dossierMap.exists() || !dossierMap.isDirectory()) {
            return false;
        }

        File fichierLevel = new File(dossierMap, "level.dat");
        return fichierLevel.exists();
    }

    private void sauvegarderConfiguration() {
        try {
            configMaps.save(fichierConfigMaps);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de la configuration des maps: " + e.getMessage());
        }
    }

    public void rechargerConfiguration() {
        configMaps = YamlConfiguration.loadConfiguration(fichierConfigMaps);
        initialiserMapsParMiniJeu();
        nettoyerMapsInexistantes();
    }

    private void nettoyerMapsInexistantes() {
        boolean modificationEffectuee = false;

        for (TypeMiniJeu miniJeu : TypeMiniJeu.values()) {
            List<String> maps = mapsParMiniJeu.get(miniJeu);
            if (maps != null) {
                List<String> mapsValides = new ArrayList<>();
                for (String nomMap : maps) {
                    if (verifierExistenceMap(nomMap)) {
                        mapsValides.add(nomMap);
                    } else {
                        plugin.getLogger().warning("Map supprimee de la configuration (inexistante): " + nomMap);
                        modificationEffectuee = true;
                    }
                }

                if (mapsValides.size() != maps.size()) {
                    mapsParMiniJeu.put(miniJeu, mapsValides);
                    String cleMiniJeu = miniJeu.name().toLowerCase().replace("_", "");
                    configMaps.set(cleMiniJeu + ".maps", mapsValides);
                }
            }
        }

        if (modificationEffectuee) {
            sauvegarderConfiguration();
        }
    }

    public boolean aMapsConfigurees(TypeMiniJeu miniJeu) {
        List<String> maps = mapsParMiniJeu.get(miniJeu);
        return maps != null && !maps.isEmpty();
    }

    public int getNombreMapsTemporaires() {
        return mapsTemporaires.size();
    }

    public void arreter() {
        for (String idPartie : new ArrayList<>(mapsTemporaires.keySet())) {
            String nomMapTemporaire = mapsTemporaires.remove(idPartie);
            if (nomMapTemporaire != null) {
                supprimerMapTemporaireParNom(nomMapTemporaire, true);
            }
        }

        if (configMaps.getBoolean("duplication.nettoyer-au-demarrage", true)) {
            nettoyerMapsTemporaires();
        }
    }
}
