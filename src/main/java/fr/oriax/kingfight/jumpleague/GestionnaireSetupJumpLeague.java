package fr.oriax.kingfight.jumpleague;

import fr.oriax.kingfight.KingFight;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestionnaireSetupJumpLeague {

    private final KingFight plugin;
    private final Map<String, SetupMonde> setupsMondes;
    private ConfigurationMultiMaps configurationMultiMaps;

    public GestionnaireSetupJumpLeague(KingFight plugin) {
        this.plugin = plugin;
        this.setupsMondes = new HashMap<>();
        this.configurationMultiMaps = new ConfigurationMultiMaps();
        verifierEtCreerDossiers();
        chargerTousLesSetups();
        chargerConfigurationMultiMaps();
    }

    private void verifierEtCreerDossiers() {
        File dossierSetups = new File(plugin.getDataFolder(), "jumpleague/setups");
        if (!dossierSetups.exists()) {
            dossierSetups.mkdirs();
        }
        
        File dossierBackups = new File(plugin.getDataFolder(), "jumpleague/backups");
        if (!dossierBackups.exists()) {
            dossierBackups.mkdirs();
        }
    }

    public static class ConfigurationMultiMaps {
        private String mapParcours;
        private String mapPvp;

        public ConfigurationMultiMaps() {
            this.mapParcours = null;
            this.mapPvp = null;
        }

        public String getMapParcours() { return mapParcours; }
        public String getMapPvp() { return mapPvp; }

        public void setMapParcours(String map) { this.mapParcours = map; }
        public void setMapPvp(String map) { this.mapPvp = map; }

        public boolean estComplet() {
            return mapParcours != null;
        }

        public boolean aPhasePvp() {
            return mapPvp != null;
        }
    }

    public static class SetupMonde {
        private final Map<Integer, Location> spawnsParParcours;
        private final Map<Integer, List<Location>> checkpointsParParcours;
        private final Map<Integer, List<Location>> coffresParParcours;
        private final Map<Integer, Location> arriveesParParcours;
        private final Map<Integer, Location> spawnsPvp;
        private final Location centreBordure;
        private final int rayonBordureInitial;

        public SetupMonde() {
            this.spawnsParParcours = new HashMap<>();
            this.checkpointsParParcours = new HashMap<>();
            this.coffresParParcours = new HashMap<>();
            this.arriveesParParcours = new HashMap<>();
            this.spawnsPvp = new HashMap<>();
            this.centreBordure = null;
            this.rayonBordureInitial = 100;
        }

        public SetupMonde(Map<Integer, Location> spawnsParParcours, Map<Integer, List<Location>> checkpointsParParcours,
                         Map<Integer, List<Location>> coffresParParcours, Map<Integer, Location> arriveesParParcours,
                         Map<Integer, Location> spawnsPvp, Location centreBordure, int rayonBordureInitial) {
            this.spawnsParParcours = spawnsParParcours;
            this.checkpointsParParcours = checkpointsParParcours;
            this.coffresParParcours = coffresParParcours;
            this.arriveesParParcours = arriveesParParcours;
            this.spawnsPvp = spawnsPvp;
            this.centreBordure = centreBordure;
            this.rayonBordureInitial = rayonBordureInitial;
        }

        public Map<Integer, Location> getSpawnsParParcours() { return spawnsParParcours; }
        public Map<Integer, List<Location>> getCheckpointsParParcours() { return checkpointsParParcours; }
        public Map<Integer, List<Location>> getCoffresParParcours() { return coffresParParcours; }
        public Map<Integer, Location> getArriveesParParcours() { return arriveesParParcours; }
        public Map<Integer, Location> getSpawnsPvp() { return spawnsPvp; }
        public Location getCentreBordure() { return centreBordure; }
        public int getRayonBordureInitial() { return rayonBordureInitial; }

        public Location getSpawnParcours(int numeroParcours) {
            return spawnsParParcours.get(numeroParcours);
        }

        public List<Location> getCheckpointsParcours(int numeroParcours) {
            return checkpointsParParcours.getOrDefault(numeroParcours, new ArrayList<>());
        }

        public List<Location> getCoffresParcours(int numeroParcours) {
            return coffresParParcours.getOrDefault(numeroParcours, new ArrayList<>());
        }

        public void definirSpawnParcours(int numeroParcours, Location location) {
            spawnsParParcours.put(numeroParcours, location);
        }

        public void ajouterCheckpointParcours(int numeroParcours, Location location) {
            checkpointsParParcours.computeIfAbsent(numeroParcours, k -> new ArrayList<>()).add(location);
        }

        public void ajouterCoffreParcours(int numeroParcours, Location location) {
            coffresParParcours.computeIfAbsent(numeroParcours, k -> new ArrayList<>()).add(location);
        }

        public Location getArriveeParcours(int numeroParcours) {
            return arriveesParParcours.get(numeroParcours);
        }

        public Location getSpawnPvp(int numeroSpawn) {
            return spawnsPvp.get(numeroSpawn);
        }

        public void definirArriveeParcours(int numeroParcours, Location location) {
            arriveesParParcours.put(numeroParcours, location);
        }

        public void definirSpawnPvp(int numeroSpawn, Location location) {
            spawnsPvp.put(numeroSpawn, location);
        }
    }

    public void definirSpawnParcours(Player joueur, int numeroParcours) {
        if (numeroParcours < 1 || numeroParcours > 16) {
            joueur.sendMessage("§cLe numero de parcours doit etre entre 1 et 16 !");
            return;
        }

        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.definirSpawnParcours(numeroParcours, location);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aSpawn du parcours " + numeroParcours + " defini pour JumpLeague !");
    }

    public void ajouterSpawnParcours(String nomMonde, int numeroParcours, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.definirSpawnParcours(numeroParcours, location);
        sauvegarderSetup(nomMonde, setup);
    }

    public void definirCheckpoint(Player joueur, int numeroParcours, int numeroCheckpoint) {
        if (numeroParcours < 1 || numeroParcours > 16) {
            joueur.sendMessage("§cLe numero de parcours doit etre entre 1 et 16 !");
            return;
        }

        if (numeroCheckpoint < 1) {
            joueur.sendMessage("§cLe numero de checkpoint doit etre superieur a 0 !");
            return;
        }

        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);

        List<Location> checkpointsExistants = setup.getCheckpointsParcours(numeroParcours);
        int nombreCheckpointsExistants = checkpointsExistants.size();

        if (numeroCheckpoint != nombreCheckpointsExistants + 1) {
            joueur.sendMessage("§cErreur : Les checkpoints doivent etre places dans l'ordre !");
            joueur.sendMessage("§cPour le parcours " + numeroParcours + ", le prochain checkpoint doit etre le numero " + (nombreCheckpointsExistants + 1));
            if (nombreCheckpointsExistants > 0) {
                joueur.sendMessage("§eCheckpoints deja places : 1 a " + nombreCheckpointsExistants);
            } else {
                joueur.sendMessage("§eAucun checkpoint place pour ce parcours, commencez par le checkpoint 1");
            }
            return;
        }

        setup.ajouterCheckpointParcours(numeroParcours, location);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aCheckpoint " + numeroCheckpoint + " du parcours " + numeroParcours + " defini pour JumpLeague !");
    }

    public void ajouterCheckpointParcours(String nomMonde, int numeroParcours, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.ajouterCheckpointParcours(numeroParcours, location);
        sauvegarderSetup(nomMonde, setup);
    }

    public void corrigerCoordonneesPrecisionCheckpoints(Player joueur, String nomMonde) {
        if (!setupsMondes.containsKey(nomMonde)) {
            joueur.sendMessage("§cAucun setup trouvé pour le monde " + nomMonde);
            return;
        }

        SetupMonde setup = setupsMondes.get(nomMonde);
        int checkpointsCorriges = 0;

        for (Map.Entry<Integer, List<Location>> entry : setup.getCheckpointsParParcours().entrySet()) {
            int numeroParcours = entry.getKey();
            List<Location> checkpoints = entry.getValue();
            
            for (int i = 0; i < checkpoints.size(); i++) {
                Location checkpoint = checkpoints.get(i);
                Location checkpointCorrige = new Location(
                    checkpoint.getWorld(),
                    checkpoint.getBlockX() + 0.5,
                    checkpoint.getBlockY(),
                    checkpoint.getBlockZ() + 0.5,
                    checkpoint.getYaw(),
                    checkpoint.getPitch()
                );
                checkpoints.set(i, checkpointCorrige);
                checkpointsCorriges++;
            }
        }

        if (checkpointsCorriges > 0) {
            sauvegarderSetup(nomMonde, setup);
            joueur.sendMessage("§a" + checkpointsCorriges + " checkpoints ont été corrigés pour une détection précise !");
            plugin.getLogger().info("Coordonnées de " + checkpointsCorriges + " checkpoints corrigées pour " + nomMonde);
        } else {
            joueur.sendMessage("§eAucun checkpoint à corriger trouvé pour " + nomMonde);
        }
    }

    public void definirArrivee(Player joueur, int numeroParcours) {
        if (numeroParcours < 1 || numeroParcours > 16) {
            joueur.sendMessage("§cLe numero de parcours doit etre entre 1 et 16 !");
            return;
        }

        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.definirArriveeParcours(numeroParcours, location);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aArrivee du parcours " + numeroParcours + " definie pour JumpLeague !");
    }

    public void definirCoffre(Player joueur, int numeroParcours, int numeroCoffre) {
        if (numeroParcours < 1 || numeroParcours > 16) {
            joueur.sendMessage("§cLe numero de parcours doit etre entre 1 et 16 !");
            return;
        }

        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.ajouterCoffreParcours(numeroParcours, location);

        location.getBlock().setType(Material.CHEST);
        sauvegarderSetup(nomMonde, setup);

        notifierPartiesEnCoursRechargerSetup(nomMonde);

        joueur.sendMessage("§aCoffre " + numeroCoffre + " du parcours " + numeroParcours + " defini pour JumpLeague !");
    }

    public void definirArriveeParcours(Player joueur, int numeroParcours) {
        if (numeroParcours < 1 || numeroParcours > 16) {
            joueur.sendMessage("§cLe numero de parcours doit etre entre 1 et 16 !");
            return;
        }

        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.definirArriveeParcours(numeroParcours, location);

        sauvegarderSetup(nomMonde, setup);
        joueur.sendMessage("§aArrivee definie pour le parcours " + numeroParcours + " !");
    }

    public void definirSpawnPvp(Player joueur, int numeroSpawn) {
        if (numeroSpawn < 1 || numeroSpawn > 16) {
            joueur.sendMessage("§cLe numero de spawn PvP doit etre entre 1 et 16 !");
            return;
        }

        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.definirSpawnPvp(numeroSpawn, location);

        sauvegarderSetup(nomMonde, setup);
        joueur.sendMessage("§aSpawn PvP " + numeroSpawn + " defini !");
    }

    public void ajouterCoffreParcours(String nomMonde, int numeroParcours, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.ajouterCoffreParcours(numeroParcours, location);

        location.getBlock().setType(Material.CHEST);

        sauvegarderSetup(nomMonde, setup);
    }

    public void definirSpawnPvp(String nomMonde, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsParParcours(),
            setup.getCheckpointsParParcours(),
            setup.getCoffresParParcours(),
            setup.getArriveesParParcours(),
            setup.getSpawnsPvp(),
            setup.getCentreBordure(),
            setup.getRayonBordureInitial()
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);
    }

    /**
     * Recharge un setup spécifique depuis le disque vers la mémoire
     */
    public void rechargerSetup(String nomMonde) {
        chargerSetup(nomMonde);
        plugin.getLogger().info("Setup JumpLeague rechargé pour " + nomMonde);
    }

    /**
     * Recharge tous les setups depuis le disque vers la mémoire
     */
    public void rechargerTousLesSetups() {
        setupsMondes.clear();
        chargerTousLesSetups();
        plugin.getLogger().info("Tous les setups JumpLeague ont été rechargés");
    }

    public void definirCentreBordure(Player joueur, int rayon) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        definirCentreBordure(nomMonde, location, rayon);
        joueur.sendMessage("§aCentre de bordure defini pour JumpLeague !");
        joueur.sendMessage("§7Position: " + (int)location.getX() + ", " + (int)location.getZ());
        joueur.sendMessage("§7Rayon initial: " + rayon + " blocs");
    }

    public void definirCentreBordure(String nomMonde, Location location, int rayon) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsParParcours(),
            setup.getCheckpointsParParcours(),
            setup.getCoffresParParcours(),
            setup.getArriveesParParcours(),
            setup.getSpawnsPvp(),
            location,
            rayon
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);
    }

    private SetupMonde obtenirOuCreerSetup(String nomMonde) {
        return setupsMondes.computeIfAbsent(nomMonde, k -> new SetupMonde());
    }

    public SetupMonde getSetupMonde(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) {
            return setupsMondes.get(nomMonde);
        }

        World monde = plugin.getServer().getWorld(nomMonde);
        if (monde == null) {
            return setupsMondes.get(nomMonde);
        }

        return chargerSetupDepuisFichier(monde);
    }

    private SetupMonde chargerSetupDepuisFichier(World monde) {
        File fichierSetup = new File(plugin.getDataFolder(), "jumpleague/setups/" + monde.getName() + ".yml");
        if (!fichierSetup.exists()) {
            return new SetupMonde();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        Map<Integer, Location> spawnsParParcours = new HashMap<>();
        Map<Integer, List<Location>> checkpointsParParcours = new HashMap<>();
        Map<Integer, List<Location>> coffresParParcours = new HashMap<>();
        Map<Integer, Location> arriveesParParcours = new HashMap<>();
        Map<Integer, Location> spawnsPvp = new HashMap<>();
        Location centreBordure = null;
        int rayonBordure = 500;

        if (config.contains("parcours")) {
            for (String numeroParcours : config.getConfigurationSection("parcours").getKeys(false)) {
                int numero = Integer.parseInt(numeroParcours);
                String cheminParcours = "parcours." + numeroParcours;

                if (config.contains(cheminParcours + ".spawn")) {
                    Location spawn = stringVersLocation(config.getString(cheminParcours + ".spawn"), monde);
                    if (spawn != null) {
                        spawnsParParcours.put(numero, spawn);
                    }
                }

                if (config.contains(cheminParcours + ".checkpoints")) {
                    List<Location> checkpoints = new ArrayList<>();
                    for (String checkpointStr : config.getStringList(cheminParcours + ".checkpoints")) {
                        Location checkpoint = stringVersLocation(checkpointStr, monde);
                        if (checkpoint != null) {
                            checkpoints.add(checkpoint);
                        }
                    }
                    checkpointsParParcours.put(numero, checkpoints);
                }

                if (config.contains(cheminParcours + ".coffres")) {
                    List<Location> coffres = new ArrayList<>();
                    for (String coffreStr : config.getStringList(cheminParcours + ".coffres")) {
                        Location coffre = stringVersLocation(coffreStr, monde);
                        if (coffre != null) {
                            coffres.add(coffre);
                        }
                    }
                    coffresParParcours.put(numero, coffres);
                }

                if (config.contains(cheminParcours + ".arrivee")) {
                    Location arrivee = stringVersLocation(config.getString(cheminParcours + ".arrivee"), monde);
                    if (arrivee != null) {
                        arriveesParParcours.put(numero, arrivee);
                    }
                }
            }
        }

        if (config.contains("spawns-pvp")) {
            for (String key : config.getConfigurationSection("spawns-pvp").getKeys(false)) {
                int numero = Integer.parseInt(key);
                Location spawn = stringVersLocation(config.getString("spawns-pvp." + key), monde);
                if (spawn != null) {
                    spawnsPvp.put(numero, spawn);
                }
            }
        }

        if (config.contains("centre-bordure")) {
            centreBordure = stringVersLocation(config.getString("centre-bordure"), monde);
        }

        if (config.contains("rayon-bordure-initial")) {
            rayonBordure = config.getInt("rayon-bordure-initial");
        }

        return new SetupMonde(spawnsParParcours, checkpointsParParcours, coffresParParcours,
                             arriveesParParcours, spawnsPvp, centreBordure, rayonBordure);
    }

    public void copierSetupVersMondeTemporaire(String nomMapOriginale, String nomMondeTemporaire) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMapOriginale + ".yml");
        if (!fichierSetupOriginal.exists()) {
            plugin.getLogger().warning("Impossible de copier le setup JumpLeague : setup original introuvable pour " + nomMapOriginale);
            return;
        }

        World mondeTemporaire = plugin.getServer().getWorld(nomMondeTemporaire);
        if (mondeTemporaire == null) {
            plugin.getLogger().warning("Impossible de copier le setup JumpLeague : monde temporaire introuvable " + nomMondeTemporaire);
            return;
        }

        FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
        File fichierSetupTemporaire = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMondeTemporaire + ".yml");
        fichierSetupTemporaire.getParentFile().mkdirs();

        FileConfiguration configTemporaire = new YamlConfiguration();

        if (configOriginal.contains("parcours")) {
            for (String numeroParcours : configOriginal.getConfigurationSection("parcours").getKeys(false)) {
                String cheminParcours = "parcours." + numeroParcours;

                if (configOriginal.contains(cheminParcours + ".spawn")) {
                    String spawn = configOriginal.getString(cheminParcours + ".spawn");
                    configTemporaire.set(cheminParcours + ".spawn", spawn);
                }

                if (configOriginal.contains(cheminParcours + ".checkpoints")) {
                    List<String> checkpoints = configOriginal.getStringList(cheminParcours + ".checkpoints");
                    configTemporaire.set(cheminParcours + ".checkpoints", checkpoints);
                }

                if (configOriginal.contains(cheminParcours + ".coffres")) {
                    List<String> coffres = configOriginal.getStringList(cheminParcours + ".coffres");
                    configTemporaire.set(cheminParcours + ".coffres", coffres);
                }

                if (configOriginal.contains(cheminParcours + ".arrivee")) {
                    String arrivee = configOriginal.getString(cheminParcours + ".arrivee");
                    configTemporaire.set(cheminParcours + ".arrivee", arrivee);
                }
            }
        }

        if (configOriginal.contains("spawns-pvp")) {
            for (String key : configOriginal.getConfigurationSection("spawns-pvp").getKeys(false)) {
                String cheminPvp = "spawns-pvp." + key;
                String spawn = configOriginal.getString(cheminPvp);
                configTemporaire.set(cheminPvp, spawn);
            }
        }

        if (configOriginal.contains("centre-bordure")) {
            configTemporaire.set("centre-bordure", configOriginal.getString("centre-bordure"));
            configTemporaire.set("rayon-bordure-initial", configOriginal.getInt("rayon-bordure-initial"));
        }

        try {
            configTemporaire.save(fichierSetupTemporaire);
            plugin.getLogger().info("Setup JumpLeague copie de " + nomMapOriginale + " vers " + nomMondeTemporaire);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la copie du setup JumpLeague : " + e.getMessage());
        }
    }

    public void rechargerSetupDepuisOriginal(String nomMapOriginale, String nomMondeTemporaire) {
        copierSetupVersMondeTemporaire(nomMapOriginale, nomMondeTemporaire);

        World mondeTemporaire = plugin.getServer().getWorld(nomMondeTemporaire);
        if (mondeTemporaire != null) {
            SetupMonde setupRecharge = chargerSetupDepuisFichier(mondeTemporaire);
            if (setupRecharge != null) {
                setupsMondes.put(nomMondeTemporaire, setupRecharge);
                plugin.getLogger().info("Setup JumpLeague recharge pour " + nomMondeTemporaire + " depuis " + nomMapOriginale);
            }
        }
    }

    private void notifierPartiesEnCoursRechargerSetup(String nomMapOriginale) {
        plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                .rechargerSetupsPartiesEnCours(nomMapOriginale);
    }

    public void supprimerSetupTemporaire(String nomMondeTemporaire) {
        setupsMondes.remove(nomMondeTemporaire);

        File fichierSetupTemporaire = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMondeTemporaire + ".yml");
        if (fichierSetupTemporaire.exists()) {
            fichierSetupTemporaire.delete();
            plugin.getLogger().info("Setup temporaire supprime pour " + nomMondeTemporaire);
        }
    }

    public boolean mondeEstConfigure(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        boolean auMoinsUnParcoursComplet = false;
        for (int i = 1; i <= 16; i++) {
            if (setup.getSpawnParcours(i) != null &&
                !setup.getCheckpointsParcours(i).isEmpty()) {
                auMoinsUnParcoursComplet = true;
                break;
            }
        }

        return auMoinsUnParcoursComplet &&
               !setup.getSpawnsPvp().isEmpty() &&
               setup.getCentreBordure() != null;
    }

    private void sauvegarderSetup(String nomMonde, SetupMonde setup) {
        File dossierSetups = new File(plugin.getDataFolder(), "jumpleague/setups");
        if (!dossierSetups.exists()) {
            dossierSetups.mkdirs();
        }

        File fichierSetup = new File(dossierSetups, nomMonde + ".yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<Integer, Location> entry : setup.getSpawnsParParcours().entrySet()) {
            config.set("parcours." + entry.getKey() + ".spawn", locationVersString(entry.getValue()));
        }

        for (Map.Entry<Integer, List<Location>> entry : setup.getCheckpointsParParcours().entrySet()) {
            List<String> checkpoints = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                checkpoints.add(locationVersStringBloc(loc));
            }
            config.set("parcours." + entry.getKey() + ".checkpoints", checkpoints);
        }

        for (Map.Entry<Integer, List<Location>> entry : setup.getCoffresParParcours().entrySet()) {
            List<String> coffres = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                coffres.add(locationVersString(loc));
            }
            config.set("parcours." + entry.getKey() + ".coffres", coffres);
        }

        for (Map.Entry<Integer, Location> entry : setup.getArriveesParParcours().entrySet()) {
            config.set("parcours." + entry.getKey() + ".arrivee", locationVersString(entry.getValue()));
        }

        for (Map.Entry<Integer, Location> entry : setup.getSpawnsPvp().entrySet()) {
            config.set("spawns-pvp." + entry.getKey(), locationVersString(entry.getValue()));
        }

        if (setup.getCentreBordure() != null) {
            config.set("centre-bordure", locationVersString(setup.getCentreBordure()));
            config.set("rayon-bordure-initial", setup.getRayonBordureInitial());
        }

        try {
            config.save(fichierSetup);
            setupsMondes.put(nomMonde, setup);
            plugin.getLogger().info("Setup JumpLeague sauvegardé et rechargé en mémoire pour " + nomMonde);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde du setup JumpLeague pour " + nomMonde + ": " + e.getMessage());
        }
    }

    private void chargerTousLesSetups() {
        File dossierSetups = new File(plugin.getDataFolder(), "jumpleague/setups");
        if (!dossierSetups.exists()) return;

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) return;

        int setupsCharges = 0;
        for (File fichier : fichiers) {
            String nomMonde = fichier.getName().replace(".yml", "");
            chargerSetup(nomMonde);
            if (verifierIntegriteSetup(nomMonde)) {
                setupsCharges++;
            }
        }
        
        if (setupsCharges > 0) {
            plugin.getLogger().info("Setups JumpLeague charges: " + setupsCharges + " mondes configures");
        }
    }

    private void chargerSetup(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        World monde = plugin.getServer().getWorld(nomMonde);
        if (monde == null) return;

        Map<Integer, Location> spawnsParParcours = new HashMap<>();
        Map<Integer, List<Location>> checkpointsParParcours = new HashMap<>();
        Map<Integer, List<Location>> coffresParParcours = new HashMap<>();
        Map<Integer, Location> arriveesParParcours = new HashMap<>();
        Map<Integer, Location> spawnsPvp = new HashMap<>();

        if (config.contains("parcours")) {
            for (String key : config.getConfigurationSection("parcours").getKeys(false)) {
                try {
                    int numeroParcours = Integer.parseInt(key);

                    if (config.contains("parcours." + key + ".spawn")) {
                        Location spawn = stringVersLocation(config.getString("parcours." + key + ".spawn"), monde);
                        if (spawn != null) {
                            spawnsParParcours.put(numeroParcours, spawn);
                        }
                    }

                    if (config.contains("parcours." + key + ".checkpoints")) {
                        List<Location> checkpoints = new ArrayList<>();
                        for (String locStr : config.getStringList("parcours." + key + ".checkpoints")) {
                            Location loc = stringVersLocation(locStr, monde);
                            if (loc != null) checkpoints.add(loc);
                        }
                        checkpointsParParcours.put(numeroParcours, checkpoints);
                    }

                    if (config.contains("parcours." + key + ".coffres")) {
                        List<Location> coffres = new ArrayList<>();
                        for (String locStr : config.getStringList("parcours." + key + ".coffres")) {
                            Location loc = stringVersLocation(locStr, monde);
                            if (loc != null) coffres.add(loc);
                        }
                        coffresParParcours.put(numeroParcours, coffres);
                    }

                    if (config.contains("parcours." + key + ".arrivee")) {
                        Location arrivee = stringVersLocation(config.getString("parcours." + key + ".arrivee"), monde);
                        if (arrivee != null) {
                            arriveesParParcours.put(numeroParcours, arrivee);
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Numero de parcours invalide dans le fichier de setup: " + key);
                }
            }
        }

        if (config.contains("spawns-pvp")) {
            for (String key : config.getConfigurationSection("spawns-pvp").getKeys(false)) {
                try {
                    int numeroSpawn = Integer.parseInt(key);
                    Location spawn = stringVersLocation(config.getString("spawns-pvp." + key), monde);
                    if (spawn != null) {
                        spawnsPvp.put(numeroSpawn, spawn);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Numero de spawn PvP invalide dans le fichier de setup: " + key);
                }
            }
        }

        Location centreBordure = null;
        int rayonBordure = 100;
        if (config.contains("centre-bordure")) {
            centreBordure = stringVersLocation(config.getString("centre-bordure"), monde);
            rayonBordure = config.getInt("rayon-bordure-initial", 100);
        }

        SetupMonde setup = new SetupMonde(spawnsParParcours, checkpointsParParcours, coffresParParcours, arriveesParParcours, spawnsPvp, centreBordure, rayonBordure);
        setupsMondes.put(nomMonde, setup);
    }

    private String locationVersString(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    private String locationVersStringBloc(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ".0;" + loc.getBlockY() + ".0;" + loc.getBlockZ() + ".0;" + loc.getYaw() + ";" + loc.getPitch();
    }

    private Location stringVersLocation(String str) {
        String[] parts = str.split(";");
        if (parts.length != 6) return null;

        World world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Location stringVersLocation(String str, World mondeForce) {
        if (str == null) return null;

        String[] parts = str.split(";");
        if (parts.length != 6) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(mondeForce, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean verifierSpawnConfigure(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        return !setup.getSpawnsParParcours().isEmpty();
    }

    public boolean verifierCheckpointsConfigures(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        int parcoursAvecCheckpoints = 0;
        for (int i = 1; i <= 16; i++) {
            if (!setup.getCheckpointsParcours(i).isEmpty()) {
                parcoursAvecCheckpoints++;
            }
        }

        return parcoursAvecCheckpoints >= 1;
    }

    public boolean verifierParcoursComplet(String nomMonde, int numeroParcours) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        return setup.getSpawnParcours(numeroParcours) != null &&
               !setup.getCheckpointsParcours(numeroParcours).isEmpty() &&
               setup.getArriveeParcours(numeroParcours) != null;
    }

    public int obtenirNombreParcoursConfigures(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return 0;

        int count = 0;
        for (int i = 1; i <= 16; i++) {
            if (verifierParcoursComplet(nomMonde, i)) {
                count++;
            }
        }
        return count;
    }

    public void afficherInfosSetup(Player joueur, String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour le monde " + nomMonde);
            return;
        }

        joueur.sendMessage("§6========== Setup JumpLeague - " + nomMonde + " ==========");

        int parcoursConfigures = obtenirNombreParcoursConfigures(nomMonde);
        joueur.sendMessage("§eParcours configures: §f" + parcoursConfigures + "/16");

        for (int i = 1; i <= 16; i++) {
            if (setup.getSpawnParcours(i) != null) {
                int checkpoints = setup.getCheckpointsParcours(i).size();
                int coffres = setup.getCoffresParcours(i).size();
                boolean aArrivee = setup.getArriveeParcours(i) != null;
                String statut = checkpoints > 0 && aArrivee ? "§aComplet" : "§cIncomplete";
                String arriveeText = aArrivee ? "§aOui" : "§cNon";

                String checkpointsText = checkpoints > 0 ? "§a" + checkpoints : "§c" + checkpoints;
                String prochainCheckpoint = checkpoints > 0 ? " §7(prochain: §e" + (checkpoints + 1) + "§7)" : " §7(commencer par §e1§7)";

                joueur.sendMessage("§7Parcours " + i + ": " + statut + " §7(§f" + checkpointsText + " §7checkpoints" + prochainCheckpoint + ", §f" + coffres + " §7coffres, arrivee: " + arriveeText + "§7)");
            }
        }

        int spawnsPvpConfigures = setup.getSpawnsPvp().size();
        joueur.sendMessage("§eSpawns PvP: §f" + spawnsPvpConfigures + "/16 " + (spawnsPvpConfigures > 0 ? "§aConfigures" : "§cNon configures"));
        joueur.sendMessage("§eCentre bordure: " + (setup.getCentreBordure() != null ? "§aDefini" : "§cNon defini"));

        if (setup.getCentreBordure() != null) {
            joueur.sendMessage("§eRayon bordure initial: §f" + setup.getRayonBordureInitial());
        }

        joueur.sendMessage("§6================================================");
    }

    public void supprimerDernierCheckpoint(Player joueur, int numeroParcours) {
        if (numeroParcours < 1 || numeroParcours > 16) {
            joueur.sendMessage("§cLe numero de parcours doit etre entre 1 et 16 !");
            return;
        }

        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            return;
        }

        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        List<Location> checkpoints = setup.getCheckpointsParcours(numeroParcours);
        if (checkpoints.isEmpty()) {
            joueur.sendMessage("§cAucun checkpoint a supprimer pour le parcours " + numeroParcours + " !");
            return;
        }

        int dernierNumero = checkpoints.size();
        checkpoints.remove(checkpoints.size() - 1);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aCheckpoint " + dernierNumero + " du parcours " + numeroParcours + " supprime !");
        joueur.sendMessage("§eProchain checkpoint a placer : " + (checkpoints.size() + 1));
    }

    public void supprimerCheckpointParcours(String nomMonde, int numeroParcours, int numeroCheckpoint) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return;

        List<Location> checkpoints = setup.getCheckpointsParcours(numeroParcours);
        if (numeroCheckpoint > 0 && numeroCheckpoint <= checkpoints.size()) {
            checkpoints.remove(numeroCheckpoint - 1);
            sauvegarderSetup(nomMonde, setup);
        }
    }

    public void supprimerCoffreParcours(String nomMonde, int numeroParcours, int numeroCoffre) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return;

        List<Location> coffres = setup.getCoffresParcours(numeroParcours);
        if (numeroCoffre > 0 && numeroCoffre <= coffres.size()) {
            coffres.remove(numeroCoffre - 1);
            sauvegarderSetup(nomMonde, setup);
        }
    }

    public boolean verifierArriveesConfigurees(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        return config.contains("arrivees") &&
               config.getConfigurationSection("arrivees").getKeys(false).size() >= 1;
    }

    public boolean verifierPvpConfigure(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        return config.contains("spawn-pvp") && !config.getString("spawn-pvp").isEmpty();
    }

    public SetupMonde obtenirSetup(String nomMonde) {
        return setupsMondes.get(nomMonde);
    }


    private void chargerConfigurationMultiMaps() {
        File fichierConfig = new File(plugin.getDataFolder(), "jumpleague/multi-maps.yml");
        if (!fichierConfig.exists()) {
            creerConfigurationMultiMapsParDefaut();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierConfig);
        configurationMultiMaps.setMapParcours(config.getString("maps.parcours"));
        configurationMultiMaps.setMapPvp(config.getString("maps.pvp"));
    }

    private void creerConfigurationMultiMapsParDefaut() {
        File fichierConfig = new File(plugin.getDataFolder(), "jumpleague/multi-maps.yml");
        fichierConfig.getParentFile().mkdirs();

        FileConfiguration config = new YamlConfiguration();
        config.set("maps.parcours", null);
        config.set("maps.pvp", null);

        try {
            config.save(fichierConfig);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la creation de la configuration multi-maps JumpLeague: " + e.getMessage());
        }
    }

    private void sauvegarderConfigurationMultiMaps() {
        File fichierConfig = new File(plugin.getDataFolder(), "jumpleague/multi-maps.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierConfig);

        config.set("maps.parcours", configurationMultiMaps.getMapParcours());
        config.set("maps.pvp", configurationMultiMaps.getMapPvp());

        try {
            config.save(fichierConfig);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de la configuration multi-maps JumpLeague: " + e.getMessage());
        }
    }

    public void definirMapParcours(Player joueur) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        configurationMultiMaps.setMapParcours(nomMonde);
        joueur.sendMessage("§aMap du parcours definie : " + nomMonde);
        sauvegarderConfigurationMultiMaps();
    }

    public void definirMapPvp(Player joueur) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour JumpLeague !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps jumpleague ajouter " + nomMonde);
            return;
        }

        configurationMultiMaps.setMapPvp(nomMonde);
        joueur.sendMessage("§aMap PvP definie : " + nomMonde);
        sauvegarderConfigurationMultiMaps();
    }

    public ConfigurationMultiMaps getConfigurationMultiMaps() {
        return configurationMultiMaps;
    }

    public void afficherInfosMultiMaps(Player joueur) {
        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("§6§l           CONFIGURATION MULTI-MAPS JUMPLEAGUE");
        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("");

        String mapParcours = configurationMultiMaps.getMapParcours();
        String mapPvp = configurationMultiMaps.getMapPvp();

        joueur.sendMessage("§e» Map Parcours: " + (mapParcours != null ? "§a" + mapParcours : "§c✗ Non definie"));
        joueur.sendMessage("§e» Map PvP: " + (mapPvp != null ? "§a" + mapPvp : "§7Aucune (optionnel)"));
        joueur.sendMessage("");

        boolean complet = configurationMultiMaps.estComplet();
        String statut = complet ? "§a“ COMPLET" : "§c— INCOMPLET";
        joueur.sendMessage("§e» Statut: " + statut);

        if (!complet) {
            joueur.sendMessage("§7» Pour configurer:");
            joueur.sendMessage("  §7- Allez sur la map de parcours");
            joueur.sendMessage("  §7- Tapez: /setup jumpleague map-parcours");
            joueur.sendMessage("  §7- Optionnel: Allez sur map PvP et tapez: /setup jumpleague map-pvp");
        }

        joueur.sendMessage("");
        joueur.sendMessage("§c» ATTENTION: Si vous utilisez une seule map pour parcours ET PvP,");
        joueur.sendMessage("§c  cette configuration peut causer des conflits !");
        joueur.sendMessage("§e» Utilisez /setup jumpleague reset-multi-maps pour nettoyer");
        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    public void resetConfigurationMultiMaps(Player joueur) {
        configurationMultiMaps.setMapParcours(null);
        configurationMultiMaps.setMapPvp(null);
        sauvegarderConfigurationMultiMaps();
        
        joueur.sendMessage("§a» Configuration multi-maps réinitialisée !");
        joueur.sendMessage("§e» Le système utilisera maintenant le mode normal (une seule map)");
    }

    public void supprimerDernierSpawnParcours(Player joueur) {
        String nomMonde = joueur.getWorld().getName();
        SetupMonde setup = setupsMondes.get(nomMonde);

        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        int dernierParcours = 0;
        for (int i = 16; i >= 1; i--) {
            if (setup.getSpawnParcours(i) != null) {
                dernierParcours = i;
                break;
            }
        }

        if (dernierParcours == 0) {
            joueur.sendMessage("§cAucun spawn de parcours a supprimer !");
            return;
        }

        setup.getSpawnsParParcours().remove(dernierParcours);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aSpawn du parcours " + dernierParcours + " supprime !");
    }

    public void supprimerDernierSpawnPvp(Player joueur) {
        String nomMonde = joueur.getWorld().getName();
        SetupMonde setup = setupsMondes.get(nomMonde);

        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        if (setup.getSpawnsPvp().isEmpty()) {
            joueur.sendMessage("§cAucun spawn PvP a supprimer !");
            return;
        }

        int dernierSpawn = 0;
        for (int i = 16; i >= 1; i--) {
            if (setup.getSpawnsPvp().containsKey(i)) {
                dernierSpawn = i;
                break;
            }
        }

        if (dernierSpawn == 0) {
            joueur.sendMessage("§cAucun spawn PvP a supprimer !");
            return;
        }

        setup.getSpawnsPvp().remove(dernierSpawn);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aSpawn PvP " + dernierSpawn + " supprime !");
    }

    public boolean sauvegarderSetupVersBackup(String nomMonde) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetupOriginal.exists()) {
            return false;
        }

        File dossierBackups = new File(plugin.getDataFolder(), "jumpleague/backups");
        dossierBackups.mkdirs();

        String timestamp = String.valueOf(System.currentTimeMillis());
        File fichierBackup = new File(dossierBackups, nomMonde + "_" + timestamp + ".yml");

        try {
            FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
            configOriginal.save(fichierBackup);
            plugin.getLogger().info("Backup du setup JumpLeague cree pour " + nomMonde + ": " + fichierBackup.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la creation du backup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean restaurerSetupDepuisBackup(String nomMonde, String nomFichierBackup) {
        File fichierBackup = new File(plugin.getDataFolder(), "jumpleague/backups/" + nomFichierBackup);
        if (!fichierBackup.exists()) {
            return false;
        }

        File fichierSetupDestination = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        fichierSetupDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configBackup = YamlConfiguration.loadConfiguration(fichierBackup);
            configBackup.save(fichierSetupDestination);
            
            String nomMondeBackup = nomMonde;
            chargerSetup(nomMondeBackup);
            
            plugin.getLogger().info("Setup JumpLeague restaure pour " + nomMonde + " depuis " + nomFichierBackup);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la restauration du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean exporterSetup(String nomMonde, String cheminDestination) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetupOriginal.exists()) {
            return false;
        }

        File fichierDestination = new File(cheminDestination);
        fichierDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
            configOriginal.save(fichierDestination);
            plugin.getLogger().info("Setup JumpLeague exporte pour " + nomMonde + " vers " + cheminDestination);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de l'exportation du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean importerSetup(String nomMonde, String cheminSource) {
        File fichierSource = new File(cheminSource);
        if (!fichierSource.exists()) {
            return false;
        }

        File fichierSetupDestination = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        fichierSetupDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configSource = YamlConfiguration.loadConfiguration(fichierSource);
            configSource.save(fichierSetupDestination);
            
            chargerSetup(nomMonde);
            
            plugin.getLogger().info("Setup JumpLeague importe pour " + nomMonde + " depuis " + cheminSource);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de l'importation du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public List<String> listerBackupsDisponibles(String nomMonde) {
        File dossierBackups = new File(plugin.getDataFolder(), "jumpleague/backups");
        if (!dossierBackups.exists()) {
            return new ArrayList<>();
        }

        File[] fichiers = dossierBackups.listFiles((dir, name) -> 
            name.startsWith(nomMonde + "_") && name.endsWith(".yml"));
        
        if (fichiers == null) {
            return new ArrayList<>();
        }

        List<String> backups = new ArrayList<>();
        for (File fichier : fichiers) {
            backups.add(fichier.getName());
        }
        
        backups.sort((a, b) -> b.compareTo(a));
        return backups;
    }

    public boolean verifierIntegriteSetup(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        boolean auMoinsUnParcoursComplet = false;
        for (int i = 1; i <= 16; i++) {
            if (setup.getSpawnParcours(i) != null &&
                !setup.getCheckpointsParcours(i).isEmpty()) {
                auMoinsUnParcoursComplet = true;
                break;
            }
        }

        return auMoinsUnParcoursComplet &&
               !setup.getSpawnsPvp().isEmpty() &&
               setup.getCentreBordure() != null;
    }
}
