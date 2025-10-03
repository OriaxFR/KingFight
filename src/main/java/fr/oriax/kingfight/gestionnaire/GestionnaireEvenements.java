package fr.oriax.kingfight.gestionnaire;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class GestionnaireEvenements implements Listener {

    private final KingFight plugin;

    public GestionnaireEvenements(KingFight plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void surConnexionJoueur(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();

        if (!joueur.hasPlayedBefore()) {
            joueur.sendMessage("§6Bienvenue sur KingFight ! Utilisez §e/tournoi §6pour commencer !");
        }

        String nomMonde = joueur.getWorld().getName();
        if (nomMonde.startsWith("HG_temp_") || nomMonde.startsWith("JL_temp_") || nomMonde.startsWith("GD_temp_")) {
            GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();

            if (!gestionnaire.getGestionnaireJoueurs().estEnPartie(joueur)) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    gestionnaire.getGestionnaireLobby().teleporterVersLobby(joueur);
                    joueur.sendMessage("§c» Vous avez été téléporté au lobby pour des raisons de sécurité.");
                    joueur.sendMessage("§7» Vous étiez dans un monde de partie qui n'existe plus.");
                }, 1L);
            }
        }
    }

    @EventHandler
    public void surDeconnexionJoueur(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();

        if (gestionnaire.getGestionnaireJoueurs().estEnPartie(joueur)) {
            TypeMiniJeu type = gestionnaire.getGestionnaireJoueurs().getTypePartie(joueur);
            String idPartie = gestionnaire.getGestionnaireJoueurs().getIdPartie(joueur);

            plugin.getLogger().info("Joueur " + joueur.getName() + " déconnecté pendant une partie " + type + " (" + idPartie + ")");

            try {
                switch (type) {
                    case HUNGER_GAMES:
                        gestionnaire.getGestionnaireHungerGames().retirerJoueurDePartie(joueur, idPartie);
                        break;
                    case JUMP_LEAGUE:
                        gestionnaire.getGestionnaireJumpLeague().retirerJoueurDePartie(joueur, idPartie);
                        break;
                    case GET_DOWN:
                        gestionnaire.getGestionnaireGetDown().retirerJoueurDePartie(joueur, idPartie);
                        break;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la déconnexion du joueur " + joueur.getName() + " de la partie: " + e.getMessage());
                gestionnaire.getGestionnaireJoueurs().forcerRetraitJoueur(joueur);
            }
        }
    }

    @EventHandler
    public void surChangementMonde(PlayerChangedWorldEvent event) {
        Player joueur = event.getPlayer();
        GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();

        if (gestionnaire.getGestionnaireJoueurs().estEnPartie(joueur)) {
            TypeMiniJeu type = gestionnaire.getGestionnaireJoueurs().getTypePartie(joueur);
            String idPartie = gestionnaire.getGestionnaireJoueurs().getIdPartie(joueur);

            boolean estDansMondePartie = false;

            switch (type) {
                case HUNGER_GAMES:
                    estDansMondePartie = gestionnaire.getGestionnaireHungerGames().estDansMondePartie(joueur, idPartie);
                    break;
                case JUMP_LEAGUE:
                    estDansMondePartie = gestionnaire.getGestionnaireJumpLeague().estDansMondePartie(joueur, idPartie);
                    break;
                case GET_DOWN:
                    estDansMondePartie = gestionnaire.getGestionnaireGetDown().estDansMondePartie(joueur, idPartie);
                    break;
            }

            if (!estDansMondePartie) {
                Location spawnLobby = gestionnaire.getGestionnaireLobby().getSpawnLobby();
                String nomMondeActuel = joueur.getWorld().getName();

                plugin.getLogger().info("Joueur " + joueur.getName() + " a quitté le monde de sa partie " + type + " (" + idPartie + ") vers " + nomMondeActuel);

                if (spawnLobby != null && nomMondeActuel.equals(spawnLobby.getWorld().getName())) {
                    try {
                        switch (type) {
                            case HUNGER_GAMES:
                                gestionnaire.getGestionnaireHungerGames().retirerJoueurDePartie(joueur, idPartie);
                                break;
                            case JUMP_LEAGUE:
                                gestionnaire.getGestionnaireJumpLeague().retirerJoueurDePartie(joueur, idPartie);
                                break;
                            case GET_DOWN:
                                gestionnaire.getGestionnaireGetDown().retirerJoueurDePartie(joueur, idPartie);
                                break;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erreur lors du retrait du joueur " + joueur.getName() + " de la partie: " + e.getMessage());
                        gestionnaire.getGestionnaireJoueurs().forcerRetraitJoueur(joueur);
                    }
                } else {
                    switch (type) {
                        case HUNGER_GAMES:
                            gestionnaire.getGestionnaireHungerGames().mettreJoueurEnSpectateur(joueur, idPartie);
                            break;
                        case JUMP_LEAGUE:
                            gestionnaire.getGestionnaireJumpLeague().mettreJoueurEnSpectateur(joueur, idPartie);
                            break;
                        case GET_DOWN:
                            gestionnaire.getGestionnaireGetDown().mettreJoueurEnSpectateur(joueur, idPartie);
                            break;
                    }
                }
            }
        }
    }
}
