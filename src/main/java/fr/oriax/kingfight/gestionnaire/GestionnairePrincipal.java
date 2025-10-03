package fr.oriax.kingfight.gestionnaire;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.hungergames.GestionnaireHungerGames;
import fr.oriax.kingfight.jumpleague.GestionnaireJumpLeague;
import fr.oriax.kingfight.getdown.GestionnaireGetDown;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import fr.oriax.kingfight.commun.GestionnaireJoueurs;
import fr.oriax.kingfight.commun.GestionnaireClassements;
import fr.oriax.kingfight.commun.GestionnaireLobby;
import fr.oriax.kingfight.commun.GestionnaireMaps;
import fr.oriax.kingfight.commun.EvenementsLobby;
import fr.oriax.kingfight.commun.GestionnairePartiesMultiMaps;
import fr.oriax.kingfight.commun.GestionnaireIsolationPartie;
import fr.oriax.kingfight.commun.GestionnaireTabList;
import fr.oriax.kingfight.commun.GestionnaireSetupsUtilitaire;
import fr.oriax.kingfight.commun.GestionnaireBackupAutomatique;
import fr.oriax.kingfight.commun.SurveillantFichiersSetup;
import fr.oriax.kingfight.gestionnaire.GestionnaireEvenementsAutomatiques;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class GestionnairePrincipal {

    private final KingFight plugin;
    private final GestionnaireHungerGames gestionnaireHungerGames;
    private final GestionnaireJumpLeague gestionnaireJumpLeague;
    private final GestionnaireGetDown gestionnaireGetDown;
    private final GestionnaireJoueurs gestionnaireJoueurs;
    private final GestionnaireClassements gestionnaireClassements;
    private final GestionnaireLobby gestionnaireLobby;
    private final GestionnaireMaps gestionnaireMaps;
    private final GestionnaireEvenementsAutomatiques gestionnaireEvenementsAutomatiques;
    private final EvenementsLobby evenementsLobby;
    private final GestionnairePartiesMultiMaps gestionnairePartiesMultiMaps;
    private final GestionnaireIsolationPartie gestionnaireIsolationPartie;
    private final GestionnaireTabList gestionnaireTabList;
    private final GestionnaireSetupsUtilitaire gestionnaireSetupsUtilitaire;
    private final GestionnaireBackupAutomatique gestionnaireBackupAutomatique;
    private final SurveillantFichiersSetup surveillantFichiersSetup;

    private FileConfiguration configPrincipale;
    private TypeMiniJeu evenementActuel;
    private long finEvenementActuel;
    private boolean evenementForce;
    private Map<TypeMiniJeu, Boolean> evenementsActifs;
    private Map<TypeMiniJeu, Boolean> evenementsEnFinition;
    private Map<TypeMiniJeu, Boolean> evenementsFinalises;

    public GestionnairePrincipal(KingFight plugin) {
        this.plugin = plugin;
        this.gestionnaireJoueurs = new GestionnaireJoueurs();
        this.evenementsActifs = new HashMap<>();
        this.evenementsEnFinition = new HashMap<>();
        this.evenementsFinalises = new HashMap<>();
        this.evenementForce = false;

        chargerConfiguration();

        this.gestionnaireMaps = new GestionnaireMaps(plugin);
        this.gestionnaireClassements = new GestionnaireClassements(plugin);
        this.gestionnaireLobby = new GestionnaireLobby(plugin);

        this.gestionnaireHungerGames = new GestionnaireHungerGames(plugin, this);
        this.gestionnaireJumpLeague = new GestionnaireJumpLeague(plugin, this);
        this.gestionnaireGetDown = new GestionnaireGetDown(plugin, this);
        this.gestionnaireEvenementsAutomatiques = new GestionnaireEvenementsAutomatiques(plugin);
        this.evenementsLobby = new EvenementsLobby(plugin);
        this.gestionnairePartiesMultiMaps = new GestionnairePartiesMultiMaps(plugin);
        this.gestionnaireIsolationPartie = new GestionnaireIsolationPartie(plugin);
        this.gestionnaireTabList = new GestionnaireTabList(plugin);
        this.gestionnaireSetupsUtilitaire = new GestionnaireSetupsUtilitaire(plugin);
        this.gestionnaireBackupAutomatique = new GestionnaireBackupAutomatique(plugin);
        this.surveillantFichiersSetup = new SurveillantFichiersSetup(plugin);

        initialiserEvenements();
        verifierEtRechargerSetups();

        gestionnaireLobby.rechargerSpawnLobby();
    }

    private void chargerConfiguration() {
        File fichierConfig = new File(plugin.getDataFolder(), "config.yml");
        if (!fichierConfig.exists()) {
            plugin.saveDefaultConfig();
        }
        configPrincipale = YamlConfiguration.loadConfiguration(fichierConfig);
    }

    /**
     * Recharge la configuration principale depuis le fichier
     */
    public void rechargerConfiguration() {
        chargerConfiguration();
        plugin.getLogger().info("Configuration principale rechargee depuis le fichier config.yml");
    }

    private void initialiserEvenements() {
        evenementsActifs.put(TypeMiniJeu.HUNGER_GAMES, false);
        evenementsActifs.put(TypeMiniJeu.JUMP_LEAGUE, false);
        evenementsActifs.put(TypeMiniJeu.GET_DOWN, false);

        evenementsEnFinition.put(TypeMiniJeu.HUNGER_GAMES, false);
        evenementsEnFinition.put(TypeMiniJeu.JUMP_LEAGUE, false);
        evenementsEnFinition.put(TypeMiniJeu.GET_DOWN, false);
    }

    private void verifierEtRechargerSetups() {
        int setupsCharges = 0;
        
        for (String nomMonde : gestionnaireMaps.obtenirMapsDisponibles(TypeMiniJeu.HUNGER_GAMES)) {
            if (gestionnaireSetupsUtilitaire.verifierExistenceSetup(nomMonde, TypeMiniJeu.HUNGER_GAMES)) {
                gestionnaireHungerGames.getGestionnaireSetup().rechargerSetup(nomMonde);
                setupsCharges++;
            }
        }
        
        for (String nomMonde : gestionnaireMaps.obtenirMapsDisponibles(TypeMiniJeu.JUMP_LEAGUE)) {
            if (gestionnaireSetupsUtilitaire.verifierExistenceSetup(nomMonde, TypeMiniJeu.JUMP_LEAGUE)) {
                gestionnaireJumpLeague.getGestionnaireSetup().rechargerSetup(nomMonde);
                setupsCharges++;
            }
        }
        
        for (String nomMonde : gestionnaireMaps.obtenirMapsDisponibles(TypeMiniJeu.GET_DOWN)) {
            if (gestionnaireSetupsUtilitaire.verifierExistenceSetup(nomMonde, TypeMiniJeu.GET_DOWN)) {
                gestionnaireGetDown.getGestionnaireSetup().rechargerSetup(nomMonde);
                setupsCharges++;
            }
        }
        
        if (setupsCharges > 0) {
            plugin.getLogger().info("Verification des setups terminee: " + setupsCharges + " setups recharges");
        }
        
        gestionnaireBackupAutomatique.nettoyerAnciennesBackups();
    }

    private void demarrerSauvegardeAutomatique() {
        new BukkitRunnable() {
            @Override
            public void run() {
                sauvegarderTousLesSetups();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L * 15L, 20L * 60L * 15L);
    }

    public void arreterGestionnaires() {
        if (gestionnaireEvenementsAutomatiques != null) {
            gestionnaireEvenementsAutomatiques.arreter();
        }
        arreterToutesLesParties();

        sauvegarderTousLesSetups();
        plugin.getLogger().info("Sauvegarde finale des setups effectuee");

        if (gestionnaireMaps != null) {
            gestionnaireMaps.arreter();
        }
    }

    public void changerEvenementActuel(TypeMiniJeu nouvelEvenement) {
        changerEvenementActuel(nouvelEvenement, false);
    }

    public boolean peutDemarrerEvenement(TypeMiniJeu type) {
        if (!gestionnaireMaps.aMapsConfigurees(type) || !gestionnaireLobby.estSpawnLobbyDefini()) {
            return false;
        }

        if (type == TypeMiniJeu.JUMP_LEAGUE) {
            return gestionnaireJumpLeague.validerConfiguration().estValide();
        }

        return true;
    }

    public void changerEvenementActuel(TypeMiniJeu nouvelEvenement, boolean force) {
        if (nouvelEvenement != evenementActuel) {
            if (evenementActuel != null) {
                arreterEvenement(evenementActuel);
            }

            if (nouvelEvenement != null) {
                demarrerEvenement(nouvelEvenement);
                this.evenementForce = force;
            } else {
                this.evenementForce = false;
            }

            evenementActuel = nouvelEvenement;
        }
    }

    public void demarrerEvenement(TypeMiniJeu type) {
        evenementsActifs.put(type, true);
        evenementsFinalises.put(type, false);
        gestionnaireJoueurs.reinitialiserPartiesJoueurs();

        gestionnaireClassements.reinitialiserClassement(type);

        Calendar fin = Calendar.getInstance();
        fin.add(Calendar.HOUR, 2);
        finEvenementActuel = fin.getTimeInMillis();

        plugin.getGestionnaireTournois().annoncerDebutEvenement(type);
        plugin.getLogger().info("Evenement " + type.name() + " demarre !");
    }

    public void arreterEvenement(TypeMiniJeu type) {
        evenementsActifs.put(type, false);

        if (aDesPartiesEnCours(type)) {
            evenementsEnFinition.put(type, true);
            plugin.getGestionnaireTournois().annoncerFinEvenementAvecAttente(type);
            plugin.getLogger().info("Evenement " + type.name() + " en cours de finition - attente des parties en cours...");
            demarrerVerificationFinition(type);
        } else {
            finaliserEvenement(type);
        }
    }

    public void arreterToutesLesParties() {
        gestionnaireHungerGames.arreterToutesLesParties();
        gestionnaireJumpLeague.arreterToutesLesParties();
        gestionnaireGetDown.arreterToutesLesParties();
    }

    public boolean estEvenementActif(TypeMiniJeu type) {
        return evenementsActifs.getOrDefault(type, false);
    }

    public boolean estEvenementEnFinition(TypeMiniJeu type) {
        return evenementsEnFinition.getOrDefault(type, false);
    }

    private boolean aDesPartiesEnCours(TypeMiniJeu type) {
        switch (type) {
            case HUNGER_GAMES:
                return gestionnaireHungerGames.getNombrePartiesEnCours() > 0;
            case JUMP_LEAGUE:
                return gestionnaireJumpLeague.getNombrePartiesEnCours() > 0;
            case GET_DOWN:
                return gestionnaireGetDown.getNombrePartiesEnCours() > 0;
            default:
                return false;
        }
    }

    private void demarrerVerificationFinition(TypeMiniJeu type) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!aDesPartiesEnCours(type)) {
                    finaliserEvenement(type);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    private void finaliserEvenement(TypeMiniJeu type) {
        if (evenementsFinalises.getOrDefault(type, false)) {
            return;
        }

        evenementsEnFinition.put(type, false);
        evenementsFinalises.put(type, true);

        switch (type) {
            case HUNGER_GAMES:
                gestionnaireHungerGames.arreterToutesLesParties();
                break;
            case JUMP_LEAGUE:
                gestionnaireJumpLeague.arreterToutesLesParties();
                break;
            case GET_DOWN:
                gestionnaireGetDown.arreterToutesLesParties();
                break;
        }

        gestionnaireClassements.sauvegarderResultatsTournoi(type);
        plugin.getGestionnaireTournois().annoncerFinEvenement(type);
        plugin.getLogger().info("Evenement " + type.name() + " finalise avec succes !");
    }

    public TypeMiniJeu getEvenementActuel() {
        return evenementActuel;
    }

    public long getTempsRestantEvenement() {
        if (finEvenementActuel == 0) return 0;
        return Math.max(0, finEvenementActuel - System.currentTimeMillis());
    }

    public boolean estEvenementForce() {
        return evenementForce;
    }

    public GestionnaireHungerGames getGestionnaireHungerGames() {
        return gestionnaireHungerGames;
    }

    public GestionnaireJumpLeague getGestionnaireJumpLeague() {
        return gestionnaireJumpLeague;
    }

    public GestionnaireGetDown getGestionnaireGetDown() {
        return gestionnaireGetDown;
    }

    public GestionnaireJoueurs getGestionnaireJoueurs() {
        return gestionnaireJoueurs;
    }

    public GestionnaireClassements getGestionnaireClassements() {
        return gestionnaireClassements;
    }

    public GestionnaireLobby getGestionnaireLobby() {
        return gestionnaireLobby;
    }

    public GestionnaireMaps getGestionnaireMaps() {
        return gestionnaireMaps;
    }

    public GestionnairePartiesMultiMaps getGestionnairePartiesMultiMaps() {
        return gestionnairePartiesMultiMaps;
    }

    public GestionnaireIsolationPartie getGestionnaireIsolationPartie() {
        return gestionnaireIsolationPartie;
    }

    public GestionnaireTabList getGestionnaireTabList() {
        return gestionnaireTabList;
    }

    public GestionnaireSetupsUtilitaire getGestionnaireSetupsUtilitaire() {
        return gestionnaireSetupsUtilitaire;
    }

    public GestionnaireBackupAutomatique getGestionnaireBackupAutomatique() {
        return gestionnaireBackupAutomatique;
    }

    public FileConfiguration getConfigPrincipale() {
        return configPrincipale;
    }

    public void notifierFinPartie(TypeMiniJeu type) {
        if (estEvenementEnFinition(type) && !aDesPartiesEnCours(type)) {
            finaliserEvenement(type);
        }
    }

    public void synchroniserSetupsAvecMaps() {
        for (TypeMiniJeu miniJeu : TypeMiniJeu.values()) {
            List<String> mapsDisponibles = gestionnaireMaps.obtenirMapsDisponibles(miniJeu);
            List<String> setupsDisponibles = gestionnaireSetupsUtilitaire.listerSetupsDisponibles(miniJeu);
            
            for (String nomMonde : mapsDisponibles) {
                if (setupsDisponibles.contains(nomMonde)) {
                    switch (miniJeu) {
                        case HUNGER_GAMES:
                            gestionnaireHungerGames.getGestionnaireSetup().rechargerSetup(nomMonde);
                            break;
                        case JUMP_LEAGUE:
                            gestionnaireJumpLeague.getGestionnaireSetup().rechargerSetup(nomMonde);
                            break;
                        case GET_DOWN:
                            gestionnaireGetDown.getGestionnaireSetup().rechargerSetup(nomMonde);
                            break;
                    }
                }
            }
        }
        
        plugin.getLogger().info("Synchronisation des setups avec les maps terminee");
    }

    public void sauvegarderTousLesSetups() {
        for (TypeMiniJeu miniJeu : TypeMiniJeu.values()) {
            gestionnaireSetupsUtilitaire.sauvegarderTousLesSetups(miniJeu);
        }
        plugin.getLogger().fine("Sauvegarde globale de tous les setups terminee");
    }
}
