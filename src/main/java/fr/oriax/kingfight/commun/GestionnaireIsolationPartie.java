package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GestionnaireIsolationPartie implements Listener {

    private final KingFight plugin;

    public GestionnaireIsolationPartie(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void surChatJoueur(AsyncPlayerChatEvent event) {
        Player joueur = event.getPlayer();
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();

        if (!gestionnaireJoueurs.estEnPartie(joueur)) {
            filtrerChatPourJoueursLobby(event);
            return;
        }

        String idPartie = gestionnaireJoueurs.getIdPartie(joueur);
        TypeMiniJeu typePartie = gestionnaireJoueurs.getTypePartie(joueur);

        Set<Player> joueursPartie = obtenirJoueursPartie(idPartie, typePartie);
        
        event.getRecipients().clear();
        event.getRecipients().addAll(joueursPartie);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void surConnexionJoueur(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();

        for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
            if (autreJoueur.equals(joueur)) continue;

            if (gestionnaireJoueurs.estEnPartie(autreJoueur)) {
                autreJoueur.hidePlayer(joueur);
            }
        }

        if (gestionnaireJoueurs.estEnPartie(joueur)) {
            String idPartie = gestionnaireJoueurs.getIdPartie(joueur);
            TypeMiniJeu typePartie = gestionnaireJoueurs.getTypePartie(joueur);
            Set<Player> joueursPartie = obtenirJoueursPartie(idPartie, typePartie);

            for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
                if (!joueursPartie.contains(autreJoueur)) {
                    joueur.hidePlayer(autreJoueur);
                    autreJoueur.hidePlayer(joueur);
                }
            }

            event.setJoinMessage(null);
        } else {
            filtrerMessageConnexionPourPartiesEnCours(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void surDeconnexionJoueur(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();

        if (gestionnaireJoueurs.estEnPartie(joueur)) {
            event.setQuitMessage(null);
        } else {
            filtrerMessageDeconnexionPourPartiesEnCours(event);
        }
    }

    public void isolerJoueurEnPartie(Player joueur) {
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();
        
        if (!gestionnaireJoueurs.estEnPartie(joueur)) return;

        String idPartie = gestionnaireJoueurs.getIdPartie(joueur);
        TypeMiniJeu typePartie = gestionnaireJoueurs.getTypePartie(joueur);
        Set<Player> joueursPartie = obtenirJoueursPartie(idPartie, typePartie);

        for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
            if (joueursPartie.contains(autreJoueur)) {
                joueur.showPlayer(autreJoueur);
                autreJoueur.showPlayer(joueur);
            } else {
                joueur.hidePlayer(autreJoueur);
                autreJoueur.hidePlayer(joueur);
            }
        }
    }

    public void desIsolerJoueur(Player joueur) {
        for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
            joueur.showPlayer(autreJoueur);
            autreJoueur.showPlayer(joueur);
        }
    }

    public void mettreAJourIsolationPartie(String idPartie, TypeMiniJeu typePartie) {
        Set<Player> joueursPartie = obtenirJoueursPartie(idPartie, typePartie);

        for (Player joueur : joueursPartie) {
            isolerJoueurEnPartie(joueur);
        }
    }

    private Set<Player> obtenirJoueursPartie(String idPartie, TypeMiniJeu typePartie) {
        Set<Player> joueursPartie = new HashSet<>();
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();

        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            if (gestionnaireJoueurs.estEnPartie(joueur) &&
                idPartie.equals(gestionnaireJoueurs.getIdPartie(joueur)) &&
                typePartie == gestionnaireJoueurs.getTypePartie(joueur)) {
                joueursPartie.add(joueur);
            }
        }

        return joueursPartie;
    }

    private void filtrerChatPourJoueursLobby(AsyncPlayerChatEvent event) {
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();
        Set<Player> destinataires = new HashSet<>();

        for (Player joueur : event.getRecipients()) {
            if (!gestionnaireJoueurs.estEnPartie(joueur)) {
                destinataires.add(joueur);
            }
        }

        event.getRecipients().clear();
        event.getRecipients().addAll(destinataires);
    }

    private void filtrerMessageConnexionPourPartiesEnCours(PlayerJoinEvent event) {
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();
        
        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            if (gestionnaireJoueurs.estEnPartie(joueur)) {
                event.setJoinMessage(null);
                return;
            }
        }
    }

    private void filtrerMessageDeconnexionPourPartiesEnCours(PlayerQuitEvent event) {
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();
        
        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            if (gestionnaireJoueurs.estEnPartie(joueur)) {
                event.setQuitMessage(null);
                return;
            }
        }
    }
}