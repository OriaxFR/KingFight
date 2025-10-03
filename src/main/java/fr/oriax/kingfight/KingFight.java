package fr.oriax.kingfight;

import fr.oriax.kingfight.gestionnaire.GestionnairePrincipal;
import fr.oriax.kingfight.gestionnaire.GestionnaireCommandes;
import fr.oriax.kingfight.gestionnaire.GestionnaireCommandesSetup;
import fr.oriax.kingfight.gestionnaire.GestionnaireEvenements;
import fr.oriax.kingfight.gestionnaire.GestionnaireTournois;
import fr.oriax.kingfight.hungergames.EvenementsHungerGames;
import fr.oriax.kingfight.jumpleague.EvenementsJumpLeague;
import fr.oriax.kingfight.getdown.EvenementsGetDown;
import org.bukkit.plugin.java.JavaPlugin;

public class KingFight extends JavaPlugin {

    private static KingFight instance;
    private GestionnairePrincipal gestionnairePrincipal;
    private GestionnaireCommandes gestionnaireCommandes;
    private GestionnaireCommandesSetup gestionnaireCommandesSetup;
    private GestionnaireEvenements gestionnaireEvenements;
    private GestionnaireTournois gestionnaireTournois;
    private EvenementsHungerGames evenementsHungerGames;
    private EvenementsJumpLeague evenementsJumpLeague;
    private EvenementsGetDown evenementsGetDown;
    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Demarrage de KingFight v1.0...");

        initialiserGestionnaires();
        enregistrerCommandes();
        enregistrerEvenements();
        nettoyerMondesTemporaires();

        getLogger().info("KingFight demarre avec succes !");
    }

    @Override
    public void onDisable() {
        if (gestionnairePrincipal != null) {
            gestionnairePrincipal.arreterGestionnaires();
        }
        getLogger().info("KingFight arrete avec succes !");
    }

    private void initialiserGestionnaires() {
        this.gestionnairePrincipal = new GestionnairePrincipal(this);
        this.gestionnaireCommandes = new GestionnaireCommandes(this);
        this.gestionnaireCommandesSetup = new GestionnaireCommandesSetup(this);
        this.gestionnaireEvenements = new GestionnaireEvenements(this);
        this.gestionnaireTournois = new GestionnaireTournois(this);
    }

    private void enregistrerCommandes() {
        getCommand("kingfight").setExecutor(gestionnaireCommandes);
        getCommand("forcer").setExecutor(gestionnaireCommandes);
        getCommand("tournoi").setExecutor(gestionnaireCommandes);
        getCommand("lobby").setExecutor(gestionnaireCommandes);
        getCommand("setup").setExecutor(gestionnaireCommandesSetup);
    }

    private void enregistrerEvenements() {
        getServer().getPluginManager().registerEvents(gestionnaireEvenements, this);
        this.evenementsHungerGames = new EvenementsHungerGames(this);
        this.evenementsJumpLeague = new EvenementsJumpLeague(this);
        this.evenementsGetDown = new EvenementsGetDown(this);
    }

    private void nettoyerMondesTemporaires() {
        getServer().getScheduler().runTaskLater(this, () -> {
            getLogger().info("Nettoyage des mondes temporaires au démarrage...");
            
            for (org.bukkit.World monde : getServer().getWorlds()) {
                String nomMonde = monde.getName();
                if (nomMonde.startsWith("HG_temp_") || nomMonde.startsWith("JL_temp_") || nomMonde.startsWith("GD_temp_")) {
                    getLogger().info("Suppression du monde temporaire: " + nomMonde);
                    
                    for (org.bukkit.entity.Player joueur : monde.getPlayers()) {
                        gestionnairePrincipal.getGestionnaireLobby().teleporterVersLobby(joueur);
                        joueur.sendMessage("§c» Vous avez été téléporté au lobby car le serveur a redémarré.");
                    }
                    
                    gestionnairePrincipal.getGestionnaireMaps().supprimerMapTemporaireParNom(nomMonde, true);
                }
            }
            
            getLogger().info("Nettoyage des mondes temporaires terminé.");
        }, 40L);
    }

    public static KingFight getInstance() {
        return instance;
    }

    public GestionnairePrincipal getGestionnairePrincipal() {
        return gestionnairePrincipal;
    }

    public GestionnaireCommandes getGestionnaireCommandes() {
        return gestionnaireCommandes;
    }

    public GestionnaireEvenements getGestionnaireEvenements() {
        return gestionnaireEvenements;
    }

    public GestionnaireTournois getGestionnaireTournois() {
        return gestionnaireTournois;
    }
}
