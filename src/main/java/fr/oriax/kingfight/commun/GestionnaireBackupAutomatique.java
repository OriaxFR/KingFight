package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GestionnaireBackupAutomatique {

    private final KingFight plugin;
    private final Map<String, Long> derniersBackups;
    private final long intervalleBackupMinutes;
    private long dernierLogBackup = 0;

    public GestionnaireBackupAutomatique(KingFight plugin) {
        this.plugin = plugin;
        this.derniersBackups = new ConcurrentHashMap<>();
        this.intervalleBackupMinutes = 30;
        
        demarrerTacheBackupAutomatique();
        demarrerTacheNettoyageAutomatique();
    }

    private void demarrerTacheBackupAutomatique() {
        new BukkitRunnable() {
            @Override
            public void run() {
                effectuerBackupsAutomatiques();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L * intervalleBackupMinutes, 20L * 60L * intervalleBackupMinutes);
    }

    private void demarrerTacheNettoyageAutomatique() {
        new BukkitRunnable() {
            @Override
            public void run() {
                nettoyerAnciennesBackups();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L * 60L * 2L, 20L * 60L * 60L * 2L);
    }

    private void effectuerBackupsAutomatiques() {
        GestionnaireSetupsUtilitaire utilitaire = plugin.getGestionnairePrincipal().getGestionnaireSetupsUtilitaire();
        
        int backupsEffectues = 0;
        for (TypeMiniJeu miniJeu : TypeMiniJeu.values()) {
            if (doitEffectuerBackup(miniJeu)) {
                if (utilitaire.sauvegarderTousLesSetups(miniJeu)) {
                    derniersBackups.put(miniJeu.name(), System.currentTimeMillis());
                    backupsEffectues++;
                }
            }
        }

        long maintenant = System.currentTimeMillis();
        if (backupsEffectues > 0 && (maintenant - dernierLogBackup) > 3600000) {
            plugin.getLogger().info("Backups automatiques effectues: " + backupsEffectues + " types de jeux sauvegardes");
            dernierLogBackup = maintenant;
        }
    }

    private boolean doitEffectuerBackup(TypeMiniJeu miniJeu) {
        Long dernierBackup = derniersBackups.get(miniJeu.name());
        if (dernierBackup == null) {
            return true;
        }
        
        long tempsEcoule = System.currentTimeMillis() - dernierBackup;
        long intervalleMs = intervalleBackupMinutes * 60 * 1000;
        
        return tempsEcoule >= intervalleMs;
    }

    public void sauvegarderSetupAvantSuppression(String nomMonde, TypeMiniJeu miniJeu) {
        try {
            switch (miniJeu) {
                case HUNGER_GAMES:
                    plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                            .getGestionnaireSetup().sauvegarderSetupVersBackup(nomMonde);
                    break;
                case JUMP_LEAGUE:
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().sauvegarderSetupVersBackup(nomMonde);
                    break;
                case GET_DOWN:
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().sauvegarderSetupVersBackup(nomMonde);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du backup automatique pour " + nomMonde + ": " + e.getMessage());
        }
    }

    public void nettoyerAnciennesBackups() {
        long maintenant = System.currentTimeMillis();
        long uneSemaine = 7L * 24L * 60L * 60L * 1000L;
        long unJour = 24L * 60L * 60L * 1000L;
        
        int totalSupprimes = 0;
        
        for (TypeMiniJeu miniJeu : TypeMiniJeu.values()) {
            String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
            File dossierBackups = new File(plugin.getDataFolder(), dossierMiniJeu + "/backups");
            
            if (dossierBackups.exists()) {
                int supprimes = nettoyerDossierBackups(dossierBackups, maintenant, unJour);
                totalSupprimes += supprimes;
                limiterNombreBackups(dossierBackups, 10);
            }
        }
        
        if (totalSupprimes > 0) {
            plugin.getLogger().info("Nettoyage automatique terminé: " + totalSupprimes + " anciens backups supprimés");
        }
    }

    private int nettoyerDossierBackups(File dossier, long maintenant, long anciennete) {
        File[] fichiers = dossier.listFiles();
        if (fichiers == null) return 0;
        
        int fichiersSupprimes = 0;
        for (File fichier : fichiers) {
            if (fichier.isFile() && fichier.getName().endsWith(".yml")) {
                long ageMs = maintenant - fichier.lastModified();
                if (ageMs > anciennete) {
                    if (fichier.delete()) {
                        fichiersSupprimes++;
                    }
                }
            } else if (fichier.isDirectory()) {
                fichiersSupprimes += nettoyerDossierBackups(fichier, maintenant, anciennete);
                
                File[] contenu = fichier.listFiles();
                if (contenu != null && contenu.length == 0) {
                    fichier.delete();
                }
            }
        }
        
        return fichiersSupprimes;
    }

    private void limiterNombreBackups(File dossierBackups, int nombreMaxBackups) {
        File[] fichiers = dossierBackups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null || fichiers.length <= nombreMaxBackups) {
            return;
        }
        java.util.Arrays.sort(fichiers, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        int fichiersSupprimes = 0;
        for (int i = nombreMaxBackups; i < fichiers.length; i++) {
            if (fichiers[i].delete()) {
                fichiersSupprimes++;
            }
        }

        if (fichiersSupprimes > 0) {
            plugin.getLogger().info("Limitation des backups: " + fichiersSupprimes + " anciens backups supprimés dans " + dossierBackups.getName());
        }
    }

    public void nettoyageRapideApresPartie(TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierBackups = new File(plugin.getDataFolder(), dossierMiniJeu + "/backups");
        
        if (dossierBackups.exists()) {
            long maintenant = System.currentTimeMillis();
            long uneHeure = 60L * 60L * 1000L;
            
            int supprimes = nettoyerDossierBackups(dossierBackups, maintenant, uneHeure);
            limiterNombreBackups(dossierBackups, 5);
            
            if (supprimes > 0) {
                plugin.getLogger().info("Nettoyage post-partie " + miniJeu.name() + ": " + supprimes + " backups supprimés");
            }
        }
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