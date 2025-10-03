package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.HashSet;

public class GestionnaireTabList implements Listener {

    private final KingFight plugin;

    public GestionnaireTabList(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        demarrerMiseAJourPeriodique();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void surConnexionJoueur(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                mettreAJourTabListTousJoueurs();
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void surDeconnexionJoueur(PlayerQuitEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                mettreAJourTabListTousJoueurs();
            }
        }.runTaskLater(plugin, 5L);
    }

    public void mettreAJourTabListJoueur(Player joueur) {
        GestionnaireJoueurs gestionnaireJoueurs = plugin.getGestionnairePrincipal().getGestionnaireJoueurs();

        if (!gestionnaireJoueurs.estEnPartie(joueur)) {
            for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
                if (!gestionnaireJoueurs.estEnPartie(autreJoueur)) {
                    joueur.showPlayer(autreJoueur);
                }
            }
            return;
        }

        String idPartie = gestionnaireJoueurs.getIdPartie(joueur);
        TypeMiniJeu typePartie = gestionnaireJoueurs.getTypePartie(joueur);
        Set<Player> joueursPartie = obtenirJoueursPartie(idPartie, typePartie);

        for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
            if (joueursPartie.contains(autreJoueur)) {
                joueur.showPlayer(autreJoueur);
            } else {
                joueur.hidePlayer(autreJoueur);
            }
        }
    }

    public void mettreAJourTabListTousJoueurs() {
        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            mettreAJourTabListJoueur(joueur);
        }
    }

    public void mettreAJourTabListPartie(String idPartie, TypeMiniJeu typePartie) {
        Set<Player> joueursPartie = obtenirJoueursPartie(idPartie, typePartie);
        for (Player joueur : joueursPartie) {
            mettreAJourTabListJoueur(joueur);
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

    private void demarrerMiseAJourPeriodique() {
        new BukkitRunnable() {
            @Override
            public void run() {
                mettreAJourTabListTousJoueurs();
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }
}