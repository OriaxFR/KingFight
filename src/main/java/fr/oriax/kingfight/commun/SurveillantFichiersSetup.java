package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SurveillantFichiersSetup {

    private final KingFight plugin;
    private final Map<String, Long> dernieresModifications;

    public SurveillantFichiersSetup(KingFight plugin) {
        this.plugin = plugin;
        this.dernieresModifications = new HashMap<>();
        
        demarrerSurveillance();
    }

    private void demarrerSurveillance() {
        new BukkitRunnable() {
            @Override
            public void run() {
                verifierModificationsFichiers();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 30L, 20L * 30L);
    }

    private void verifierModificationsFichiers() {
        for (TypeMiniJeu miniJeu : TypeMiniJeu.values()) {
            verifierDossierSetup(miniJeu);
        }
    }

    private void verifierDossierSetup(TypeMiniJeu miniJeu) {
        String dossierMiniJeu = obtenirDossierMiniJeu(miniJeu);
        File dossierSetups = new File(plugin.getDataFolder(), dossierMiniJeu + "/setups");
        
        if (!dossierSetups.exists()) {
            return;
        }

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) {
            return;
        }

        for (File fichier : fichiers) {
            String cleFichier = miniJeu.name() + "_" + fichier.getName();
            long derniereModification = fichier.lastModified();
            
            Long ancienneModification = dernieresModifications.get(cleFichier);
            
            if (ancienneModification == null) {
                dernieresModifications.put(cleFichier, derniereModification);
                rechargerSetup(miniJeu, fichier.getName().replace(".yml", ""), true);
            } else if (derniereModification > ancienneModification) {
                dernieresModifications.put(cleFichier, derniereModification);
                rechargerSetup(miniJeu, fichier.getName().replace(".yml", ""), false);
            }
        }
    }

    private void rechargerSetup(TypeMiniJeu miniJeu, String nomMonde, boolean nouveauSetup) {
        try {
            switch (miniJeu) {
                case HUNGER_GAMES:
                    plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                            .getGestionnaireSetup().rechargerSetup(nomMonde);
                    break;
                case JUMP_LEAGUE:
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().rechargerSetup(nomMonde);
                    break;
                case GET_DOWN:
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().rechargerSetup(nomMonde);
                    break;
            }
            
            String message = nouveauSetup ? 
                "Nouveau setup detecte et charge: " + nomMonde + " (" + miniJeu.name() + ")" :
                "Setup modifie recharge: " + nomMonde + " (" + miniJeu.name() + ")";
            plugin.getLogger().info(message);
        } catch (Exception e) {
            String action = nouveauSetup ? "chargement du nouveau setup" : "rechargement du setup modifie";
            plugin.getLogger().warning("Erreur lors du " + action + " " + nomMonde + ": " + e.getMessage());
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