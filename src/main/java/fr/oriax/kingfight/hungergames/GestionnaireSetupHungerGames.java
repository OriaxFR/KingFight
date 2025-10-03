package fr.oriax.kingfight.hungergames;

import fr.oriax.kingfight.KingFight;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GestionnaireSetupHungerGames {

    private final KingFight plugin;

    public GestionnaireSetupHungerGames(KingFight plugin) {
        this.plugin = plugin;
        verifierEtCreerDossiers();
        chargerTousLesSetups();
    }

    private void verifierEtCreerDossiers() {
        File dossierSetups = new File(plugin.getDataFolder(), "hungergames/setups");
        if (!dossierSetups.exists()) {
            dossierSetups.mkdirs();
        }
        
        File dossierBackups = new File(plugin.getDataFolder(), "hungergames/backups");
        if (!dossierBackups.exists()) {
            dossierBackups.mkdirs();
        }
    }

    private void chargerTousLesSetups() {
        File dossierSetups = new File(plugin.getDataFolder(), "hungergames/setups");
        if (!dossierSetups.exists()) return;

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) return;

        int setupsCharges = 0;
        for (File fichier : fichiers) {
            String nomMonde = fichier.getName().replace(".yml", "");
            if (verifierIntegriteSetup(nomMonde)) {
                setupsCharges++;
            }
        }
        
        if (setupsCharges > 0) {
            plugin.getLogger().info("Setups HungerGames charges: " + setupsCharges + " mondes configures");
        }
    }

    private boolean verifierIntegriteSetup(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        
        boolean spawnsOk = config.contains("spawns") && 
                          config.getConfigurationSection("spawns").getKeys(false).size() >= 16;
        boolean bordureOk = config.contains("bordure") && 
                           config.contains("bordure.centre-x") && 
                           config.contains("bordure.centre-z");
        
        return spawnsOk && bordureOk;
    }

    public void definirSpawnJoueur(Player joueur, int numeroSpawn) {
        Location location = joueur.getLocation();
        World monde = location.getWorld();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.HUNGER_GAMES, monde.getName())) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour Hunger Games !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps hungergames ajouter " + monde.getName());
            return;
        }

        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");
        fichierSetup.getParentFile().mkdirs();

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        String chemin = "spawns.joueur-" + numeroSpawn;
        config.set(chemin + ".x", location.getX());
        config.set(chemin + ".y", location.getY());
        config.set(chemin + ".z", location.getZ());
        config.set(chemin + ".yaw", location.getYaw());
        config.set(chemin + ".pitch", location.getPitch());

        try {
            config.save(fichierSetup);
            joueur.sendMessage("§aSpawn joueur " + numeroSpawn + " defini !");
        } catch (IOException e) {
            joueur.sendMessage("§cErreur lors de la sauvegarde !");
        }
    }





    public void definirCentreBordure(Player joueur, int rayon) {
        Location location = joueur.getLocation();
        World monde = location.getWorld();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.HUNGER_GAMES, monde.getName())) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour Hunger Games !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps hungergames ajouter " + monde.getName());
            return;
        }

        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");
        fichierSetup.getParentFile().mkdirs();

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        config.set("bordure.centre-x", location.getX());
        config.set("bordure.centre-z", location.getZ());
        config.set("bordure.rayon", rayon);

        monde.getWorldBorder().setCenter(location.getX(), location.getZ());
        monde.getWorldBorder().setSize(rayon * 2);

        try {
            config.save(fichierSetup);
            joueur.sendMessage("§aCentre de la bordure defini !");
            joueur.sendMessage("§7Position: " + (int)location.getX() + ", " + (int)location.getZ());
            joueur.sendMessage("§7Rayon initial: " + rayon + " blocs");
        } catch (IOException e) {
            joueur.sendMessage("§cErreur lors de la sauvegarde !");
        }
    }

    public List<Location> getSpawnsJoueurs(World monde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");
        if (!fichierSetup.exists()) return new ArrayList<>();

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        List<Location> spawns = new ArrayList<>();

        if (config.contains("spawns")) {
            for (String key : config.getConfigurationSection("spawns").getKeys(false)) {
                String chemin = "spawns." + key;
                double x = config.getDouble(chemin + ".x");
                double y = config.getDouble(chemin + ".y");
                double z = config.getDouble(chemin + ".z");
                float yaw = (float) config.getDouble(chemin + ".yaw");
                float pitch = (float) config.getDouble(chemin + ".pitch");

                spawns.add(new Location(monde, x, y, z, yaw, pitch));
            }
        }

        return spawns;
    }



    public Location getCentreBordure(World monde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");
        if (!fichierSetup.exists()) return monde.getSpawnLocation();

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        if (config.contains("bordure")) {
            double x = config.getDouble("bordure.centre-x");
            double z = config.getDouble("bordure.centre-z");
            return new Location(monde, x, 100, z);
        }

        return monde.getSpawnLocation();
    }

    public int getRayonBordure(World monde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");
        if (!fichierSetup.exists()) return 500;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        if (config.contains("bordure.rayon")) {
            return config.getInt("bordure.rayon", 500);
        }

        return 500;
    }

    public boolean estMondeConfigurer(World monde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        return config.contains("spawns") &&
               config.contains("bordure") &&
               config.getConfigurationSection("spawns").getKeys(false).size() >= 16;
    }

    public boolean verifierSpawnsConfigures(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        return config.contains("spawns") &&
               config.getConfigurationSection("spawns").getKeys(false).size() >= 16;
    }



    public boolean verifierBordureConfiguree(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);
        return config.contains("bordure") &&
               config.contains("bordure.centre-x") &&
               config.contains("bordure.centre-z");
    }

    public void copierSetupVersMondeTemporaire(String nomMapOriginale, String nomMondeTemporaire) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMapOriginale + ".yml");
        if (!fichierSetupOriginal.exists()) {
            plugin.getLogger().warning("Impossible de copier le setup HungerGames : setup original introuvable pour " + nomMapOriginale);
            return;
        }

        World mondeTemporaire = plugin.getServer().getWorld(nomMondeTemporaire);
        if (mondeTemporaire == null) {
            plugin.getLogger().warning("Impossible de copier le setup HungerGames : monde temporaire introuvable " + nomMondeTemporaire);
            return;
        }

        FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
        File fichierSetupTemporaire = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMondeTemporaire + ".yml");
        fichierSetupTemporaire.getParentFile().mkdirs();

        FileConfiguration configTemporaire = new YamlConfiguration();

        if (configOriginal.contains("spawns")) {
            for (String key : configOriginal.getConfigurationSection("spawns").getKeys(false)) {
                String cheminOriginal = "spawns." + key;
                double x = configOriginal.getDouble(cheminOriginal + ".x");
                double y = configOriginal.getDouble(cheminOriginal + ".y");
                double z = configOriginal.getDouble(cheminOriginal + ".z");
                float yaw = (float) configOriginal.getDouble(cheminOriginal + ".yaw");
                float pitch = (float) configOriginal.getDouble(cheminOriginal + ".pitch");

                configTemporaire.set(cheminOriginal + ".x", x);
                configTemporaire.set(cheminOriginal + ".y", y);
                configTemporaire.set(cheminOriginal + ".z", z);
                configTemporaire.set(cheminOriginal + ".yaw", yaw);
                configTemporaire.set(cheminOriginal + ".pitch", pitch);
            }
        }



        if (configOriginal.contains("bordure")) {
            configTemporaire.set("bordure.centre-x", configOriginal.getDouble("bordure.centre-x"));
            configTemporaire.set("bordure.centre-z", configOriginal.getDouble("bordure.centre-z"));
            configTemporaire.set("bordure.rayon", configOriginal.getInt("bordure.rayon", 500));
        }

        try {
            configTemporaire.save(fichierSetupTemporaire);
            plugin.getLogger().info("Setup HungerGames copie de " + nomMapOriginale + " vers " + nomMondeTemporaire);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la copie du setup HungerGames : " + e.getMessage());
        }
    }

    public void supprimerSetupTemporaire(String nomMondeTemporaire) {
        File fichierSetupTemporaire = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMondeTemporaire + ".yml");
        if (fichierSetupTemporaire.exists()) {
            if (fichierSetupTemporaire.delete()) {
                plugin.getLogger().info("Setup HungerGames temporaire supprime pour " + nomMondeTemporaire);
            }
        }
    }

    public void supprimerDernierSpawn(Player joueur) {
        World monde = joueur.getWorld();
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + monde.getName() + ".yml");

        if (!fichierSetup.exists()) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        if (!config.contains("spawns")) {
            joueur.sendMessage("§cAucun spawn a supprimer !");
            return;
        }

        Set<String> spawns = config.getConfigurationSection("spawns").getKeys(false);
        if (spawns.isEmpty()) {
            joueur.sendMessage("§cAucun spawn a supprimer !");
            return;
        }

        int dernierNumero = spawns.size();
        String dernierSpawn = "joueur-" + dernierNumero;

        config.set("spawns." + dernierSpawn, null);

        try {
            config.save(fichierSetup);
            joueur.sendMessage("§aSpawn " + dernierNumero + " supprime !");
        } catch (IOException e) {
            joueur.sendMessage("§cErreur lors de la sauvegarde !");
        }
    }

    public boolean sauvegarderSetupVersBackup(String nomMonde) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        if (!fichierSetupOriginal.exists()) {
            return false;
        }

        File dossierBackups = new File(plugin.getDataFolder(), "hungergames/backups");
        dossierBackups.mkdirs();

        String timestamp = String.valueOf(System.currentTimeMillis());
        File fichierBackup = new File(dossierBackups, nomMonde + "_" + timestamp + ".yml");

        try {
            FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
            configOriginal.save(fichierBackup);
            plugin.getLogger().info("Backup du setup HungerGames cree pour " + nomMonde + ": " + fichierBackup.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la creation du backup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean restaurerSetupDepuisBackup(String nomMonde, String nomFichierBackup) {
        File fichierBackup = new File(plugin.getDataFolder(), "hungergames/backups/" + nomFichierBackup);
        if (!fichierBackup.exists()) {
            return false;
        }

        File fichierSetupDestination = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        fichierSetupDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configBackup = YamlConfiguration.loadConfiguration(fichierBackup);
            configBackup.save(fichierSetupDestination);
            plugin.getLogger().info("Setup HungerGames restaure pour " + nomMonde + " depuis " + nomFichierBackup);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la restauration du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean exporterSetup(String nomMonde, String cheminDestination) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        if (!fichierSetupOriginal.exists()) {
            return false;
        }

        File fichierDestination = new File(cheminDestination);
        fichierDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
            configOriginal.save(fichierDestination);
            plugin.getLogger().info("Setup HungerGames exporte pour " + nomMonde + " vers " + cheminDestination);
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

        File fichierSetupDestination = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        fichierSetupDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configSource = YamlConfiguration.loadConfiguration(fichierSource);
            configSource.save(fichierSetupDestination);
            plugin.getLogger().info("Setup HungerGames importe pour " + nomMonde + " depuis " + cheminSource);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de l'importation du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public void rechargerSetup(String nomMonde) {
        if (verifierIntegriteSetup(nomMonde)) {
            plugin.getLogger().info("Setup HungerGames recharge pour " + nomMonde);
        }
    }

    public List<String> listerBackupsDisponibles(String nomMonde) {
        File dossierBackups = new File(plugin.getDataFolder(), "hungergames/backups");
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


}
