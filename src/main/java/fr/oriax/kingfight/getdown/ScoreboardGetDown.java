package fr.oriax.kingfight.getdown;

import fr.oriax.kingfight.commun.JoueurPartie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class ScoreboardGetDown {

    private final PartieGetDown partie;
    private final ScoreboardManager manager;

    public ScoreboardGetDown(PartieGetDown partie) {
        this.partie = partie;
        this.manager = Bukkit.getScoreboardManager();
    }

    public void creerScoreboard(Player joueur) {
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("getdown", "dummy");

        objective.setDisplayName(ChatColor.LIGHT_PURPLE + "§l» GET DOWN «");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        creerTeamsPourJoueurs(scoreboard);
        mettreAJourScoreboard(joueur, scoreboard, objective);
        joueur.setScoreboard(scoreboard);
    }

    public void mettreAJourScoreboard(Player joueur, Scoreboard scoreboard, Objective objective) {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int ligne = 15;

        objective.getScore("§7§m──────────────").setScore(ligne--);
        objective.getScore("").setScore(ligne--);

        PartieGetDown.JoueurPartieGetDown joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());

        if (partie.getEtat() == PartieGetDown.EtatPartie.PREPARATION) {
            objective.getScore("§b§l» Préparation...").setScore(ligne--);
            objective.getScore("  ").setScore(ligne--);
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP) {
            objective.getScore("§a§l» Phase Jump").setScore(ligne--);
            objective.getScore("   ").setScore(ligne--);
            if (joueurPartie != null) {
                objective.getScore("§9» Map: §f" + (partie.getMapActuelle() + 1)).setScore(ligne--);
                objective.getScore("§6» KingCoins: §f" + joueurPartie.getKingcoins()).setScore(ligne--);
                objective.getScore("    ").setScore(ligne--);
            }
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF) {
            objective.getScore("§e§l» Phase Stuff").setScore(ligne--);
            objective.getScore("     ").setScore(ligne--);
            if (joueurPartie != null) {
                objective.getScore("§6» KingCoins: §f" + joueurPartie.getKingcoins()).setScore(ligne--);
                objective.getScore("      ").setScore(ligne--);
            }
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.PVP) {
            objective.getScore("§c§l» Phase PvP").setScore(ligne--);
            objective.getScore("       ").setScore(ligne--);
            if (joueurPartie != null) {
                int vies = partie.getViesJoueur(joueur.getUniqueId());
                objective.getScore("§c» Vies: §f" + vies).setScore(ligne--);
                objective.getScore("§6» Points: §f" + joueurPartie.getPoints()).setScore(ligne--);
                objective.getScore("        ").setScore(ligne--);
            }
        }

        objective.getScore("§e» Temps: §f" + obtenirTempsFormate()).setScore(ligne--);
        objective.getScore("§2» Joueurs: §f" + partie.getNombreJoueursVivants()).setScore(ligne--);
        objective.getScore("         ").setScore(ligne--);

        objective.getScore("§7§m──────────────").setScore(ligne--);
        objective.getScore("§7kingfight.fr").setScore(ligne--);
    }

    public void mettreAJourTousLesScoreboards() {
        for (Player joueur : partie.getJoueurs()) {
            Scoreboard scoreboard = joueur.getScoreboard();
            if (scoreboard != null) {
                creerTeamsPourJoueurs(scoreboard);
                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (objective != null) {
                    mettreAJourScoreboard(joueur, scoreboard, objective);
                }
            }
        }
    }

    private String obtenirTempsFormate() {
        int tempsRestant = 0;

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP) {
            int dureeParcoursSecondes = partie.getConfig().getDureeParMapSecondes();
            int tempsEcoule = partie.getTempsEcoule();
            tempsRestant = Math.max(0, dureeParcoursSecondes - tempsEcoule);
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF) {
            int dureeStuffSecondes = partie.getConfig().getDureePhaseStuffSecondes();
            int tempsEcoule = partie.getTempsEcoule();
            tempsRestant = Math.max(0, dureeStuffSecondes - tempsEcoule);
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.PVP) {
            int dureePvpSecondes = partie.getConfig().getDureePhasePvpMinutes() * 60;
            int tempsEcoule = partie.getTempsEcoule();
            tempsRestant = Math.max(0, dureePvpSecondes - tempsEcoule);
        }

        int minutes = tempsRestant / 60;
        int secondes = tempsRestant % 60;
        return String.format("%02d:%02d", minutes, secondes);
    }

    private void creerTeamsPourJoueurs(Scoreboard scoreboard) {
        for (Player joueur : partie.getJoueurs()) {
            PartieGetDown.JoueurPartieGetDown joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());
            if (joueurPartie != null) {
                String teamName = "player_" + joueur.getUniqueId().toString().substring(0, 8);
                Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                }
                
                team.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.NEVER);
                team.addEntry(joueur.getName());
            }
        }
    }

    public void supprimerScoreboard(Player joueur) {
        joueur.setScoreboard(manager.getMainScoreboard());
    }
}