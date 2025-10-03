package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class GestionnaireSetupsUtilitaire {

    private final KingFight plugin;

    public GestionnaireSetupsUtilitaire(KingFight plugin) {
        this.plugin = plugin;
    }

    public boolean copierSetupVersMap(String nomMapSource, String nomMapDestination, TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        
        File fichierSource = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups/" + nomMapSource + ".yml");
        if (!fichierSource.exists()) {
            return false;
        }

        File fichierDestination = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups/" + nomMapDestination + ".yml");
        fichierDestination.getParentFile().mkdirs();

        try {
            Files.copy(fichierSource.toPath(), fichierDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Setup copie de " + nomMapSource + " vers " + nomMapDestination + " pour " + miniJeu.name());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la copie du setup: " + e.getMessage());
            return false;
        }
    }

    public boolean sauvegarderTousLesSetups(TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierSetups = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups");
        File dossierBackupGlobal = new File(plugin.getDataFolder(), dossierMiniJeu + "/backups/global");
        
        if (!dossierSetups.exists()) {
            return false;
        }

        dossierBackupGlobal.mkdirs();
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        File dossierBackupTimestamp = new File(dossierBackupGlobal, "backup_" + timestamp);
        dossierBackupTimestamp.mkdirs();

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) {
            return false;
        }

        int fichiersCopiés = 0;
        for (File fichier : fichiers) {
            try {
                File destination = new File(dossierBackupTimestamp, fichier.getName());
                Files.copy(fichier.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fichiersCopiés++;
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur lors de la copie de " + fichier.getName() + ": " + e.getMessage());
            }
        }

        if (fichiersCopiés > 0) {
            plugin.getLogger().fine("Backup global cree pour " + miniJeu.name() + ": " + fichiersCopiés + " setups sauvegardes");
            return true;
        }
        
        return false;
    }

    public boolean restaurerBackupGlobal(TypeMiniJeu miniJeu, String nomBackup) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierBackup = new File(plugin.getDataFolder(), dossierMiniJeu + "/backups/global/" + nomBackup);
        
        if (!dossierBackup.exists() || !dossierBackup.isDirectory()) {
            return false;
        }

        File dossierSetups = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups");
        dossierSetups.mkdirs();

        File[] fichiers = dossierBackup.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) {
            return false;
        }

        int fichiersRestaures = 0;
        for (File fichier : fichiers) {
            try {
                File destination = new File(dossierSetups, fichier.getName());
                Files.copy(fichier.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fichiersRestaures++;
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur lors de la restauration de " + fichier.getName() + ": " + e.getMessage());
            }
        }

        if (fichiersRestaures > 0) {
            plugin.getLogger().info("Backup global restaure pour " + miniJeu.name() + ": " + fichiersRestaures + " setups restaures");
            return true;
        }
        
        return false;
    }

    public List<String> listerBackupsGlobaux(TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierBackupGlobal = new File(plugin.getDataFolder(), dossierMiniJeu + "/backups/global");
        
        List<String> backups = new ArrayList<>();
        if (!dossierBackupGlobal.exists()) {
            return backups;
        }

        File[] dossiers = dossierBackupGlobal.listFiles(File::isDirectory);
        if (dossiers == null) {
            return backups;
        }

        for (File dossier : dossiers) {
            backups.add(dossier.getName());
        }
        
        backups.sort((a, b) -> b.compareTo(a));
        return backups;
    }

    public boolean exporterTousLesSetups(TypeMiniJeu miniJeu, String cheminDestination) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierSetups = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups");
        
        if (!dossierSetups.exists()) {
            return false;
        }

        File dossierDestination = new File(cheminDestination);
        dossierDestination.mkdirs();

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) {
            return false;
        }

        int fichiersExportes = 0;
        for (File fichier : fichiers) {
            try {
                File destination = new File(dossierDestination, fichier.getName());
                Files.copy(fichier.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fichiersExportes++;
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur lors de l'exportation de " + fichier.getName() + ": " + e.getMessage());
            }
        }

        if (fichiersExportes > 0) {
            plugin.getLogger().info("Setups " + miniJeu.name() + " exportes: " + fichiersExportes + " fichiers vers " + cheminDestination);
            return true;
        }
        
        return false;
    }

    public boolean importerTousLesSetups(TypeMiniJeu miniJeu, String cheminSource) {
        File dossierSource = new File(cheminSource);
        if (!dossierSource.exists() || !dossierSource.isDirectory()) {
            return false;
        }

        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierSetups = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups");
        dossierSetups.mkdirs();

        File[] fichiers = dossierSource.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) {
            return false;
        }

        int fichiersImportes = 0;
        for (File fichier : fichiers) {
            try {
                File destination = new File(dossierSetups, fichier.getName());
                Files.copy(fichier.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fichiersImportes++;
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur lors de l'importation de " + fichier.getName() + ": " + e.getMessage());
            }
        }

        if (fichiersImportes > 0) {
            plugin.getLogger().info("Setups " + miniJeu.name() + " importes: " + fichiersImportes + " fichiers depuis " + cheminSource);
            return true;
        }
        
        return false;
    }

    public boolean verifierExistenceSetup(String nomMonde, TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File fichierSetup = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups/" + nomMonde + ".yml");
        return fichierSetup.exists();
    }

    public List<String> listerSetupsDisponibles(TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierSetups = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups");
        
        List<String> setups = new ArrayList<>();
        if (!dossierSetups.exists()) {
            return setups;
        }

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) {
            return setups;
        }

        for (File fichier : fichiers) {
            setups.add(fichier.getName().replace(".yml", ""));
        }
        
        setups.sort(String::compareTo);
        return setups;
    }

    private String obtenirDossierMiniJeu(TypeMiniJeu miniJeu) {
        switch (miniJeu) {
            case HUNGER_GAMES:
                return "hungergames";
            case JUMP_LEAGUE:
                return "jumpleague";
            case GET_DOWN:
                return "getdown";
            default:
                throw new IllegalArgumentException("Type de mini-jeu non supporté: " + miniJeu);
        }
    }
}