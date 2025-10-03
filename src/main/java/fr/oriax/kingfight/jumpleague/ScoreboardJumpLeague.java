package fr.oriax.kingfight.jumpleague;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.ChatColor;

public class ScoreboardJumpLeague {

    private final PartieJumpLeague partie;
    private final ScoreboardManager manager;

    public ScoreboardJumpLeague(PartieJumpLeague partie) {
        this.partie = partie;
        this.manager = Bukkit.getScoreboardManager();
    }

    public void creerScoreboard(Player joueur) {
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("jumpleague", "dummy");

        objective.setDisplayName("§b§l» JUMP LEAGUE «");
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

        PartieJumpLeague.JoueurPartieJumpLeague joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());

        if (partie.getEtat() == PartieJumpLeague.EtatPartie.PREPARATION) {
            objective.getScore("§b§l» PREPARATION").setScore(ligne--);
            objective.getScore("  ").setScore(ligne--);

        } else if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.JUMP) {
            objective.getScore("§a§l» PHASE PARCOURS").setScore(ligne--);
            objective.getScore("    ").setScore(ligne--);

            if (joueurPartie != null) {
                int checkpointActuel = joueurPartie.getCheckpointActuel();
                int nombreCheckpointsParcours = partie.getNombreCheckpointsParcours(joueurPartie.getNumeroParcours());
                objective.getScore("§9» Checkpoints: §f" + checkpointActuel + "/" + nombreCheckpointsParcours).setScore(ligne--);
                objective.getScore("§6» Points: §f" + joueurPartie.getPoints()).setScore(ligne--);
                objective.getScore("     ").setScore(ligne--);
            }

        } else if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.PVP) {
            objective.getScore("§c§l» PHASE PVP").setScore(ligne--);
            objective.getScore("      ").setScore(ligne--);

            if (joueurPartie != null) {
                int vies = partie.getViesJoueur(joueur.getUniqueId());
                objective.getScore("§c» Vies: §f" + vies).setScore(ligne--);
                objective.getScore("§6» Points: §f" + joueurPartie.getPoints()).setScore(ligne--);
                objective.getScore("       ").setScore(ligne--);
            }
        }

        objective.getScore("§e» Temps: §f" + obtenirTempsFormate()).setScore(ligne--);
        int nombreJoueursVivants = partie.getNombreJoueursVivants();
        objective.getScore("§2» Joueurs: §f" + nombreJoueursVivants).setScore(ligne--);
        objective.getScore("        ").setScore(ligne--);

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

        if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.JUMP) {
            int dureeParcoursSecondes = partie.getConfig().getDureePhaseJumpMinutes() * 60;
            int tempsEcoule = partie.getTempsEcoule();
            tempsRestant = Math.max(0, dureeParcoursSecondes - tempsEcoule);
        } else if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.PVP) {
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
            PartieJumpLeague.JoueurPartieJumpLeague joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());
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