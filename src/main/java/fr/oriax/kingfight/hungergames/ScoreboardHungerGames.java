package fr.oriax.kingfight.hungergames;

import fr.oriax.kingfight.commun.JoueurPartie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class ScoreboardHungerGames {

    private final PartieHungerGames partie;
    private final ScoreboardManager manager;

    public ScoreboardHungerGames(PartieHungerGames partie) {
        this.partie = partie;
        this.manager = Bukkit.getScoreboardManager();
    }

    public void creerScoreboard(Player joueur) {
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("hungergames", "dummy");

        objective.setDisplayName(ChatColor.GOLD + "§l» HUNGER GAMES «");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        creerTeamsPourJoueurs(scoreboard);
        mettreAJourScoreboard(joueur, scoreboard, objective);
        joueur.setScoreboard(scoreboard);
    }

    public void mettreAJourScoreboard(Player joueur, Scoreboard scoreboard, Objective objective) {
        if (scoreboard == null || objective == null) {
            return;
        }

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int ligne = 15;

        objective.getScore("§7§m──────────────").setScore(ligne--);
        objective.getScore("").setScore(ligne--);

        JoueurPartie joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());

        if (partie.getEtat() == PartieHungerGames.EtatPartie.PREPARATION) {
            objective.getScore("§b» Préparation...").setScore(ligne--);
            objective.getScore("  ").setScore(ligne--);
        } else if (partie.estInvincibiliteActive()) {
            objective.getScore("§e» Invincibilité").setScore(ligne--);
            objective.getScore("   ").setScore(ligne--);
        } else {
            objective.getScore("§c» PvP Actif").setScore(ligne--);
            objective.getScore("    ").setScore(ligne--);
        }

        if (joueurPartie != null) {
            objective.getScore("§4» Kills: §f" + joueurPartie.getKills()).setScore(ligne--);
            objective.getScore("§6» Points: §f" + joueurPartie.getPoints()).setScore(ligne--);
            objective.getScore("     ").setScore(ligne--);
        }

        objective.getScore("§e» Temps: §f" + obtenirTempsFormate()).setScore(ligne--);
        objective.getScore("§2» Joueurs: §f" + partie.getNombreJoueursVivants()).setScore(ligne--);
        objective.getScore("      ").setScore(ligne--);

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

    private void creerTeamsPourJoueurs(Scoreboard scoreboard) {
        for (Player joueur : partie.getJoueurs()) {
            JoueurPartie joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());
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

    private String obtenirTempsFormate() {
        int tempsEcoule = partie.getTempsEcoule();
        int dureeMaxSecondes = partie.getConfig().getDureePartieMax() * 60;
        int tempsRestant = Math.max(0, dureeMaxSecondes - tempsEcoule);
        int minutes = tempsRestant / 60;
        int secondes = tempsRestant % 60;
        return String.format("%02d:%02d", minutes, secondes);
    }
}