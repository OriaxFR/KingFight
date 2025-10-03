package fr.oriax.kingfight.commun;

import org.bukkit.entity.Player;
import java.util.UUID;

public class JoueurPartie {

    private final UUID uuid;
    private final String nomOriginal;
    private final String nomAnonymise;
    private int points;
    private int kills;
    private int position;
    private boolean vivant;
    private boolean spectateur;

    public JoueurPartie(Player joueur, String nomAnonymise) {
        this.uuid = joueur.getUniqueId();
        this.nomOriginal = joueur.getName();
        this.nomAnonymise = nomAnonymise;
        this.points = 0;
        this.kills = 0;
        this.position = 0;
        this.vivant = true;
        this.spectateur = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNomOriginal() {
        return nomOriginal;
    }

    public String getNomAnonymise() {
        return nomAnonymise;
    }

    public int getPoints() {
        return points;
    }

    public void ajouterPoints(int points) {
        this.points += points;
    }

    public int getKills() {
        return kills;
    }

    public void ajouterKill() {
        this.kills++;
    }

    public int getPosition() {
        return position;
    }

    public void definirPosition(int position) {
        this.position = position;
    }

    public boolean estVivant() {
        return vivant;
    }

    public void definirVivant(boolean vivant) {
        this.vivant = vivant;
    }

    public boolean estSpectateur() {
        return spectateur;
    }

    public void definirSpectateur(boolean spectateur) {
        this.spectateur = spectateur;
    }
}
