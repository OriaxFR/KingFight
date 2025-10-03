package fr.oriax.kingfight.gestionnaire;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GestionnaireTournois {

    private final KingFight plugin;
    private boolean annoncesActivees;

    public GestionnaireTournois(KingFight plugin) {
        this.plugin = plugin;
        this.annoncesActivees = true;

        demarrerAnnoncesAutomatiques();
    }

    private void demarrerAnnoncesAutomatiques() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (annoncesActivees) {
                    verifierEtAnnoncerEvenements();
                }
            }
        }.runTaskTimer(plugin, 0L, 6000L);
    }

    private void verifierEtAnnoncerEvenements() {
        if (!plugin.getGestionnairePrincipal().getConfigPrincipale().getBoolean("annonces.activees", true)) {
            return;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        int heure = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        for (TypeMiniJeu type : TypeMiniJeu.values()) {
            List<Integer> heuresEvenements = plugin.getGestionnairePrincipal().getConfigPrincipale()
                    .getIntegerList("tournois." + type.getNomConfig() + ".heures");

            for (int heureEvenement : heuresEvenements) {
                if (heure == heureEvenement && minute == 0) {
                    if (!plugin.getGestionnairePrincipal().estEvenementActif(type)) {
                        annoncerDebutEvenement(type);
                    }
                } else if (heure == heureEvenement - 1 && minute == 30) {
                    if (!plugin.getGestionnairePrincipal().estEvenementActif(type)) {
                        annoncerEvenementAVenir(type, 30);
                    }
                } else if (heure == heureEvenement - 1 && minute == 45) {
                    if (!plugin.getGestionnairePrincipal().estEvenementActif(type)) {
                        annoncerEvenementAVenir(type, 15);
                    }
                } else if (heure == heureEvenement - 1 && minute == 50) {
                    if (!plugin.getGestionnairePrincipal().estEvenementActif(type)) {
                        annoncerEvenementAVenir(type, 10);
                    }
                } else if (heure == heureEvenement - 1 && minute == 55) {
                    if (!plugin.getGestionnairePrincipal().estEvenementActif(type)) {
                        annoncerEvenementAVenir(type, 5);
                    }
                } else if (heure == heureEvenement + 1 && minute == 30) {
                    if (plugin.getGestionnairePrincipal().estEvenementActif(type) && 
                        plugin.getGestionnairePrincipal().getEvenementActuel() == type &&
                        !plugin.getGestionnairePrincipal().estEvenementForce()) {
                        annoncerFinEvenement(type, 30);
                    }
                } else if (heure == heureEvenement + 1 && minute == 45) {
                    if (plugin.getGestionnairePrincipal().estEvenementActif(type) && 
                        plugin.getGestionnairePrincipal().getEvenementActuel() == type &&
                        !plugin.getGestionnairePrincipal().estEvenementForce()) {
                        annoncerFinEvenement(type, 15);
                    }
                } else if (heure == heureEvenement + 1 && minute == 50) {
                    if (plugin.getGestionnairePrincipal().estEvenementActif(type) && 
                        plugin.getGestionnairePrincipal().getEvenementActuel() == type &&
                        !plugin.getGestionnairePrincipal().estEvenementForce()) {
                        annoncerFinEvenement(type, 10);
                    }
                } else if (heure == heureEvenement + 1 && minute == 55) {
                    if (plugin.getGestionnairePrincipal().estEvenementActif(type) && 
                        plugin.getGestionnairePrincipal().getEvenementActuel() == type &&
                        !plugin.getGestionnairePrincipal().estEvenementForce()) {
                        annoncerFinEvenement(type, 5);
                    }
                } else if (heure == heureEvenement + 2 && minute == 0) {
                    if (plugin.getGestionnairePrincipal().estEvenementActif(type) && 
                        plugin.getGestionnairePrincipal().getEvenementActuel() == type &&
                        !plugin.getGestionnairePrincipal().estEvenementForce()) {
                        annoncerFinEvenementAvecAttente(type);
                    }
                }
            }
        }
    }

    private void annoncerEvenementAVenir(TypeMiniJeu type, int minutes) {
        String message = ChatColor.GOLD + "━━━━━━━━━━ TOURNOI KINGFIGHT ━━━━━━━━━━\n" +
                        ChatColor.YELLOW + "Le tournoi " + ChatColor.AQUA + type.getNomAffichage() +
                        ChatColor.YELLOW + " commence dans " + ChatColor.RED + minutes + " minutes" +
                        ChatColor.YELLOW + " !\n" +
                        ChatColor.GREEN + "Utilisez " + ChatColor.WHITE + "/tournoi" + ChatColor.GREEN +
                        " pour vous inscrire !\n" +
                        ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        Bukkit.broadcastMessage(message);
    }

    private void annoncerFinEvenement(TypeMiniJeu type, int minutes) {
        String message = "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "§6§l                » TOURNOI KINGFIGHT «\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "\n" +
                        "§e§l» Le tournoi §b§l" + type.getNomAffichage().toUpperCase() + " §e§lse termine dans §c§l" + minutes + " minutes §e§l!\n" +
                        "\n" +
                        "§a» Derniere chance pour gagner des points !\n" +
                        "\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        Bukkit.broadcastMessage(message);
    }

    public void annoncerDebutEvenement(TypeMiniJeu type) {
        int maxParties = plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getMaxPartiesParJoueurPublic();
        String message = "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "§6§l                » TOURNOI KINGFIGHT «\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "\n" +
                        "§a§l» Le tournoi §b§l" + type.getNomAffichage().toUpperCase() + " §a§la commence !\n" +
                        "\n" +
                        "§e» Duree: §f2 heures\n" +
                        "§e» Parties par joueur: §f" + maxParties + " maximum\n" +
                        "§a» Utilisez §f/tournoi §apour participer !\n" +
                        "\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        Bukkit.broadcastMessage(message);
    }

    public void annoncerFinEvenement(TypeMiniJeu type) {
        String message = "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "§6§l                » TOURNOI KINGFIGHT «\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "\n" +
                        "§c§l» Le tournoi §b§l" + type.getNomAffichage().toUpperCase() + " §c§lest termine !\n" +
                        "\n" +
                        "§e§l» CLASSEMENT FINAL TOP 10 «\n" +
                        obtenirClassementFinal(type) +
                        "\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        Bukkit.broadcastMessage(message);
    }

    public void annoncerFinEvenementAvecAttente(TypeMiniJeu type) {
        String message = "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "§6§l                » TOURNOI KINGFIGHT «\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "\n" +
                        "§e§l» Le tournoi §b§l" + type.getNomAffichage().toUpperCase() + " §e§lest officiellement termine !\n" +
                        "\n" +
                        "§d» Attente de la fin des parties en cours...\n" +
                        "§a» Le classement final sera affiche une fois\n" +
                        "§a» toutes les parties terminees !\n" +
                        "\n" +
                        "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        Bukkit.broadcastMessage(message);
    }

    public void activerAnnonces() {
        this.annoncesActivees = true;
    }

    public void desactiverAnnonces() {
        this.annoncesActivees = false;
    }

    public boolean sontAnnoncesActivees() {
        return annoncesActivees;
    }

    private String obtenirClassementFinal(TypeMiniJeu type) {
        List<Map.Entry<UUID, Integer>> classementFinal = plugin.getGestionnairePrincipal()
                .getGestionnaireClassements().getClassementTrie(type, 10);
        
        if (classementFinal.isEmpty()) {
            return "§7Aucun participant dans ce tournoi.\n";
        }
        
        StringBuilder classement = new StringBuilder();
        for (int i = 0; i < Math.min(10, classementFinal.size()); i++) {
            Map.Entry<UUID, Integer> entry = classementFinal.get(i);
            String nomJoueur = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (nomJoueur == null) nomJoueur = "Joueur inconnu";
            
            String couleur = i == 0 ? "§6§l" : i == 1 ? "§e§l" : i == 2 ? "§c§l" : "§f";
            String position = String.format("%2d", i + 1);
            String points = String.format("%,d", entry.getValue());
            
            classement.append(couleur).append("#").append(position).append(" §f")
                     .append(nomJoueur).append(" §7- §a").append(points).append(" pts\n");
        }
        
        return classement.toString();
    }
}