package fr.oriax.kingfight.gestionnaire;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GestionnaireCommandesSetup implements CommandExecutor {

    private final KingFight plugin;

    public GestionnaireCommandesSetup(KingFight plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("kingfight.setup")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut etre executee que par un joueur !");
            return true;
        }

        Player joueur = (Player) sender;

        if (args.length < 1) {
            afficherAideSetup(sender);
            return true;
        }

        String premierArg = args[0];

        if (premierArg.equalsIgnoreCase("maps")) {
            gererCommandesMaps(joueur, args);
            return true;
        }

        if (premierArg.equalsIgnoreCase("lobby")) {
            gererCommandeLobby(joueur, args);
            return true;
        }

        if (premierArg.equalsIgnoreCase("info")) {
            afficherInfosMaps(joueur);
            return true;
        }

        if (premierArg.equalsIgnoreCase("nettoyer")) {
            nettoyerMapsTemporaires(joueur);
            return true;
        }

        if (args.length < 2) {
            afficherAideSetup(sender);
            return true;
        }

        String typeJeu = args[0];
        String action = args[1];

        TypeMiniJeu type = TypeMiniJeu.parNom(typeJeu);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Type de mini-jeu invalide ! (hungergames, jumpleague, getdown)");
            return true;
        }

        switch (type) {
            case HUNGER_GAMES:
                gererSetupHungerGames(joueur, action, args);
                break;
            case JUMP_LEAGUE:
                gererSetupJumpLeague(joueur, action, args);
                break;
            case GET_DOWN:
                gererSetupGetDown(joueur, action, args);
                break;
        }

        return true;
    }

    private void gererSetupHungerGames(Player joueur, String action, String[] args) {
        switch (action.toLowerCase()) {
            case "spawn":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup hungergames spawn <numero>");
                    return;
                }
                try {
                    int numero = Integer.parseInt(args[2]);
                    plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                            .getGestionnaireSetup().definirSpawnJoueur(joueur, numero);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero invalide !");
                }
                break;



            case "bordure":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup hungergames bordure <rayon>");
                    joueur.sendMessage(ChatColor.YELLOW + "» rayon: Rayon initial de la bordure en blocs");
                    return;
                }
                try {
                    int rayon = Integer.parseInt(args[2]);
                    if (rayon <= 0) {
                        joueur.sendMessage(ChatColor.RED + "Le rayon doit etre superieur a 0 !");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                            .getGestionnaireSetup().definirCentreBordure(joueur, rayon);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Rayon invalide !");
                }
                break;

            case "supprimer":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup hungergames supprimer spawn");
                    return;
                }
                String typeSupprimer = args[2].toLowerCase();
                if (typeSupprimer.equals("spawn")) {
                    plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                            .getGestionnaireSetup().supprimerDernierSpawn(joueur);
                } else {
                    joueur.sendMessage(ChatColor.RED + "Type invalide ! Utilisez: spawn");
                }
                break;

            default:
                joueur.sendMessage(ChatColor.RED + "Action invalide ! (spawn, bordure, supprimer)");
                break;
        }
    }

    private void gererSetupJumpLeague(Player joueur, String action, String[] args) {
        switch (action.toLowerCase()) {
            case "spawn":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague spawn <numero_parcours>");
                    joueur.sendMessage(ChatColor.YELLOW + "» Definit le spawn de depart pour un parcours (1-16)");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    if (numeroParcours < 1 || numeroParcours > 16) {
                        joueur.sendMessage(ChatColor.RED + "Le numero de parcours doit etre entre 1 et 16 !");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().definirSpawnParcours(joueur, numeroParcours);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide !");
                }
                break;

            case "checkpoint":
                if (args.length < 4) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague checkpoint <numero_joueur> <numero_checkpoint>");
                    return;
                }
                try {
                    int numeroJoueur = Integer.parseInt(args[2]);
                    int numeroCheckpoint = Integer.parseInt(args[3]);
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().definirCheckpoint(joueur, numeroJoueur, numeroCheckpoint);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numeros invalides !");
                }
                break;

            case "arrivee":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague arrivee <numero_parcours>");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().definirArriveeParcours(joueur, numeroParcours);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide !");
                }
                break;

            case "coffre":
                if (args.length < 4) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague coffre <numero_parcours> <numero_coffre>");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    int numeroCoffre = Integer.parseInt(args[3]);
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().definirCoffre(joueur, numeroParcours, numeroCoffre);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numeros invalides !");
                }
                break;

            case "pvp":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague pvp <numero_spawn>");
                    return;
                }
                try {
                    int numeroSpawn = Integer.parseInt(args[2]);
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().definirSpawnPvp(joueur, numeroSpawn);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de spawn invalide !");
                }
                break;

            case "info":
                String nomMonde = joueur.getWorld().getName();
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                        .getGestionnaireSetup().afficherInfosSetup(joueur, nomMonde);
                break;

            case "supprimer":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague supprimer <spawn|checkpoint|pvp> [numero]");
                    return;
                }
                String typeSupprimerJL = args[2].toLowerCase();
                if (typeSupprimerJL.equals("checkpoint")) {
                    if (args.length < 4) {
                        joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague supprimer checkpoint <numero_parcours>");
                        return;
                    }
                    try {
                        int numeroParcours = Integer.parseInt(args[3]);
                        plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                                .getGestionnaireSetup().supprimerDernierCheckpoint(joueur, numeroParcours);
                    } catch (NumberFormatException e) {
                        joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide !");
                    }
                } else if (typeSupprimerJL.equals("spawn")) {
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().supprimerDernierSpawnParcours(joueur);
                } else if (typeSupprimerJL.equals("pvp")) {
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().supprimerDernierSpawnPvp(joueur);
                } else {
                    joueur.sendMessage(ChatColor.RED + "Type invalide ! Utilisez: spawn, checkpoint ou pvp");
                }
                break;

            case "bordure":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague bordure <rayon>");
                    joueur.sendMessage(ChatColor.YELLOW + "» rayon: Rayon initial de la bordure en blocs");
                    return;
                }
                try {
                    int rayon = Integer.parseInt(args[2]);
                    if (rayon <= 0) {
                        joueur.sendMessage(ChatColor.RED + "Le rayon doit etre superieur a 0 !");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().definirCentreBordure(joueur, rayon);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Rayon invalide !");
                }
                break;

            case "map-parcours":
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                        .getGestionnaireSetup().definirMapParcours(joueur);
                break;

            case "map-pvp":
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                        .getGestionnaireSetup().definirMapPvp(joueur);
                break;

            case "corriger-checkpoints":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup jumpleague corriger-checkpoints <nom_monde>");
                    joueur.sendMessage(ChatColor.YELLOW + "» Corrige la précision des coordonnées des checkpoints existants");
                    return;
                }
                String nomMondeCorrection = args[2];
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                        .getGestionnaireSetup().corrigerCoordonneesPrecisionCheckpoints(joueur, nomMondeCorrection);
                break;

            case "multi-maps":
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                        .getGestionnaireSetup().afficherInfosMultiMaps(joueur);
                break;

            case "reset-multi-maps":
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                        .getGestionnaireSetup().resetConfigurationMultiMaps(joueur);
                break;

            case "reload":
                if (args.length >= 3 && args[2].equalsIgnoreCase("all")) {
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().rechargerTousLesSetups();
                    joueur.sendMessage(ChatColor.GREEN + "Tous les setups JumpLeague ont ete recharges !");
                } else {
                    String mondeActuel = joueur.getWorld().getName();
                    plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                            .getGestionnaireSetup().rechargerSetup(mondeActuel);
                    joueur.sendMessage(ChatColor.GREEN + "Setup JumpLeague recharge pour " + mondeActuel + " !");
                }
                break;

            default:
                joueur.sendMessage(ChatColor.RED + "Action invalide ! (spawn, checkpoint, arrivee, coffre, pvp, bordure, info, supprimer, map-parcours, map-pvp, multi-maps, reset-multi-maps, reload)");
                joueur.sendMessage(ChatColor.YELLOW + "» map-parcours : Definir map pour les parcours");
                joueur.sendMessage(ChatColor.YELLOW + "» map-pvp : Definir map pour la phase PvP");
                joueur.sendMessage(ChatColor.YELLOW + "» multi-maps : Afficher configuration multi-maps");
                joueur.sendMessage(ChatColor.YELLOW + "» reset-multi-maps : Reinitialiser configuration multi-maps");
                joueur.sendMessage(ChatColor.YELLOW + "» reload : Recharger le setup du monde actuel");
                joueur.sendMessage(ChatColor.YELLOW + "» reload all : Recharger tous les setups");
                break;
        }
    }

    private void gererSetupGetDown(Player joueur, String action, String[] args) {
        switch (action.toLowerCase()) {
            case "spawn":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup getdown spawn <parcours>");
                    joueur.sendMessage(ChatColor.YELLOW + "» parcours: 1 (premier), 2 (deuxieme), 3 (troisieme)");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    if (numeroParcours < 1 || numeroParcours > 3) {
                        joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide ! Utilisez 1, 2 ou 3");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().definirSpawnMap(joueur, numeroParcours - 1);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide ! Utilisez 1, 2 ou 3");
                }
                break;



            case "arrivee":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup getdown arrivee <parcours>");
                    joueur.sendMessage(ChatColor.YELLOW + "» parcours: 1 (premier), 2 (deuxieme), 3 (troisieme)");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    if (numeroParcours < 1 || numeroParcours > 3) {
                        joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide ! Utilisez 1, 2 ou 3");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().definirArriveeMap(joueur, numeroParcours - 1);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide ! Utilisez 1, 2 ou 3");
                }
                break;

            case "hauteur":
                if (args.length < 4) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup getdown hauteur <parcours> <hauteur-y>");
                    joueur.sendMessage(ChatColor.YELLOW + "» parcours: 1 (premier), 2 (deuxieme), 3 (troisieme)");
                    joueur.sendMessage(ChatColor.YELLOW + "» hauteur-y: Hauteur Y d'arrivee (0-256)");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    if (numeroParcours < 1 || numeroParcours > 3) {
                        joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide ! Utilisez 1, 2 ou 3");
                        return;
                    }
                    int hauteur = Integer.parseInt(args[3]);
                    
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().definirHauteurArrivee(joueur, numeroParcours - 1, hauteur);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Valeurs invalides ! Utilisez des nombres entiers");
                }
                break;

            case "pvp":
                if (args.length >= 3) {
                    try {
                        int numeroSpawn = Integer.parseInt(args[2]);
                        plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                                .getGestionnaireSetup().ajouterSpawnPvPMultiple(joueur, numeroSpawn);
                    } catch (NumberFormatException e) {
                        joueur.sendMessage(ChatColor.RED + "Numero de spawn invalide !");
                    }
                } else {
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().definirSpawnPvP(joueur);
                }
                break;

            case "spawn-pvp":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup getdown spawn-pvp <numero>");
                    joueur.sendMessage(ChatColor.YELLOW + "» numero: 1-16 (position du joueur en PvP)");
                    return;
                }
                try {
                    int numeroSpawn = Integer.parseInt(args[2]);
                    if (numeroSpawn < 1 || numeroSpawn > 16) {
                        joueur.sendMessage(ChatColor.RED + "Numero de spawn invalide ! Utilisez 1-16");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().definirSpawnPvPNumero(joueur, numeroSpawn);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de spawn invalide !");
                }
                break;

            case "info":
                String nomMonde = joueur.getWorld().getName();
                plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                        .getGestionnaireSetup().afficherInfosSetup(joueur, nomMonde);
                break;

            case "bordure":
                int rayon = 500;
                if (args.length >= 3) {
                    try {
                        rayon = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        joueur.sendMessage(ChatColor.RED + "Rayon invalide ! Utilisation du rayon par defaut: 500");
                    }
                }
                plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                        .getGestionnaireSetup().definirCentreBordure(joueur, rayon);
                break;

            case "supprimer":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup getdown supprimer <spawn|pvp>");
                    return;
                }
                String typeSupprimerGD = args[2].toLowerCase();
                if (typeSupprimerGD.equals("spawn")) {
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().supprimerDernierSpawn(joueur);
                } else if (typeSupprimerGD.equals("pvp")) {
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().supprimerDernierSpawnPvp(joueur);
                } else {
                    joueur.sendMessage(ChatColor.RED + "Type invalide ! Utilisez: spawn ou pvp");
                }
                break;

            case "map-parcours":
                if (args.length < 3) {
                    joueur.sendMessage(ChatColor.RED + "Usage: /setup getdown map-parcours <1|2|3>");
                    return;
                }
                try {
                    int numeroParcours = Integer.parseInt(args[2]);
                    if (numeroParcours < 1 || numeroParcours > 3) {
                        joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide ! Utilisez 1, 2 ou 3");
                        return;
                    }
                    plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                            .getGestionnaireSetup().definirMapParcours(joueur, numeroParcours);
                } catch (NumberFormatException e) {
                    joueur.sendMessage(ChatColor.RED + "Numero de parcours invalide !");
                }
                break;

            case "map-pvp":
                plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                        .getGestionnaireSetup().definirMapPvp(joueur);
                break;

            case "multi-maps":
                plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                        .getGestionnaireSetup().afficherInfosMultiMaps(joueur);
                break;

            default:
                joueur.sendMessage(ChatColor.RED + "Action invalide ! Commandes GetDown:");
                joueur.sendMessage(ChatColor.YELLOW + "» spawn <parcours> : Spawn de descente (1=1er, 2=2e, 3=3e)");
                joueur.sendMessage(ChatColor.YELLOW + "» spawn-joueurs <parcours> <num> : Spawn joueur sur map parcours");
                joueur.sendMessage(ChatColor.YELLOW + "» arrivee <parcours> : Point d'arrivee (ancien systeme)");
                joueur.sendMessage(ChatColor.YELLOW + "» hauteur <parcours> <hauteur-y> : Hauteur d'arrivee");
                joueur.sendMessage(ChatColor.YELLOW + "» stuff : Spawn d'achat stuff");
                joueur.sendMessage(ChatColor.YELLOW + "» pvp [numero] : Spawn PvP (ancien systeme)");
                joueur.sendMessage(ChatColor.YELLOW + "» spawn-pvp <num> : Spawn PvP sur map PvP (1-16)");
                joueur.sendMessage(ChatColor.YELLOW + "» bordure [rayon] : Centre de bordure");
                joueur.sendMessage(ChatColor.YELLOW + "» supprimer <spawn|pvp> : Supprimer dernier element");
                joueur.sendMessage(ChatColor.YELLOW + "» map-parcours <1|2|3> : Definir map originale parcours");
                joueur.sendMessage(ChatColor.YELLOW + "» map-pvp : Definir map originale PvP");
                joueur.sendMessage(ChatColor.YELLOW + "» multi-maps : Afficher configuration multi-maps");
                joueur.sendMessage(ChatColor.YELLOW + "» info : Afficher informations du setup");
                break;
        }
    }

    private void gererCommandesMaps(Player joueur, String[] args) {
        if (args.length < 3) {
            joueur.sendMessage(ChatColor.RED + "Usage: /setup maps <minijeu> <ajouter/retirer> <nom_map>");
            joueur.sendMessage(ChatColor.YELLOW + "Mini-jeux: hungergames, jumpleague, getdown");
            return;
        }

        String nomMiniJeu = args[1];
        String action = args[2];

        TypeMiniJeu type = TypeMiniJeu.parNom(nomMiniJeu);
        if (type == null) {
            joueur.sendMessage(ChatColor.RED + "Mini-jeu invalide ! (hungergames, jumpleague, getdown)");
            return;
        }

        if (args.length < 4) {
            if (action.equalsIgnoreCase("lister")) {
                listerMapsDisponibles(joueur, type);
                return;
            }
            joueur.sendMessage(ChatColor.RED + "Usage: /setup maps " + nomMiniJeu + " <ajouter/retirer/lister> <nom_map>");
            return;
        }

        String nomMap = args[3];

        switch (action.toLowerCase()) {
            case "ajouter":
                if (plugin.getGestionnairePrincipal().getGestionnaireMaps().ajouterMapPourMiniJeu(type, nomMap)) {
                    joueur.sendMessage(ChatColor.GREEN + "Map " + nomMap + " ajoutee pour " + type.name());
                } else {
                    if (!plugin.getGestionnairePrincipal().getGestionnaireMaps().verifierExistenceMap(nomMap)) {
                        joueur.sendMessage(ChatColor.RED + "Erreur : La map " + nomMap + " n'existe pas sur le serveur !");
                        joueur.sendMessage(ChatColor.YELLOW + "Verifiez que le dossier de la map est present dans le repertoire du serveur.");
                    } else {
                        joueur.sendMessage(ChatColor.RED + "La map " + nomMap + " est deja configuree pour ce mode de jeu.");
                    }
                }
                break;

            case "retirer":
                plugin.getGestionnairePrincipal().getGestionnaireMaps().retirerMapPourMiniJeu(type, nomMap);
                joueur.sendMessage(ChatColor.GREEN + "Map " + nomMap + " retiree pour " + type.name());
                break;

            default:
                joueur.sendMessage(ChatColor.RED + "Action invalide ! (ajouter, retirer, lister)");
                break;
        }
    }

    private void listerMapsDisponibles(Player joueur, TypeMiniJeu type) {
        java.util.List<String> maps = plugin.getGestionnairePrincipal().getGestionnaireMaps().obtenirMapsDisponibles(type);

        joueur.sendMessage(ChatColor.GOLD + "Maps disponibles pour " + type.name() + ":");
        if (maps.isEmpty()) {
            joueur.sendMessage(ChatColor.RED + "  Aucune map configuree");
        } else {
            for (String map : maps) {
                joueur.sendMessage(ChatColor.WHITE + "  - " + map);
            }
        }
    }

    private void gererCommandeLobby(Player joueur, String[] args) {
        if (args.length < 2) {
            joueur.sendMessage(ChatColor.RED + "Usage: /setup lobby <spawn/info>");
            return;
        }

        String action = args[1];

        switch (action.toLowerCase()) {
            case "spawn":
                plugin.getGestionnairePrincipal().getGestionnaireMaps().definirSpawnLobby(joueur.getLocation());
                plugin.getGestionnairePrincipal().getGestionnaireLobby().rechargerSpawnLobby();
                joueur.sendMessage(ChatColor.GREEN + "Spawn du lobby defini a votre position !");
                break;

            case "info":
                if (plugin.getGestionnairePrincipal().getGestionnaireLobby().estSpawnLobbyDefini()) {
                    org.bukkit.Location spawn = plugin.getGestionnairePrincipal().getGestionnaireMaps().obtenirSpawnLobby();
                    joueur.sendMessage(ChatColor.GOLD + "Informations du lobby:");
                    joueur.sendMessage(ChatColor.WHITE + "  Monde: " + spawn.getWorld().getName());
                    joueur.sendMessage(ChatColor.WHITE + "  Position Spawn: " +
                        String.format("%.1f, %.1f, %.1f", spawn.getX(), spawn.getY(), spawn.getZ()));
                } else {
                    joueur.sendMessage(ChatColor.RED + "Le spawn du lobby n'est pas encore defini !");
                    joueur.sendMessage(ChatColor.YELLOW + "Utilisez /setup lobby spawn pour le definir");
                }
                break;

            default:
                joueur.sendMessage(ChatColor.RED + "Action invalide ! (spawn, info)");
                break;
        }
    }

    private void afficherInfosMaps(Player joueur) {
        joueur.sendMessage(ChatColor.GOLD + "========== Informations Maps ==========");

        joueur.sendMessage(ChatColor.AQUA + "Lobby:");
        if (plugin.getGestionnairePrincipal().getGestionnaireLobby().estSpawnLobbyDefini()) {
            org.bukkit.Location spawn = plugin.getGestionnairePrincipal().getGestionnaireMaps().obtenirSpawnLobby();
            joueur.sendMessage(ChatColor.WHITE + "  Monde: " + spawn.getWorld().getName());
            joueur.sendMessage(ChatColor.WHITE + "  Position Spawn: " +
                String.format("%.1f, %.1f, %.1f", spawn.getX(), spawn.getY(), spawn.getZ()));
        } else {
            joueur.sendMessage(ChatColor.RED + "  Spawn non defini ! Utilisez /setup lobby spawn");
        }

        joueur.sendMessage("");
        for (fr.oriax.kingfight.commun.TypeMiniJeu type : fr.oriax.kingfight.commun.TypeMiniJeu.values()) {
            java.util.List<String> maps = plugin.getGestionnairePrincipal().getGestionnaireMaps().obtenirMapsDisponibles(type);
            joueur.sendMessage(ChatColor.YELLOW + type.name() + ":");
            if (maps.isEmpty()) {
                joueur.sendMessage(ChatColor.RED + "  Aucune map configuree");
            } else {
                for (String map : maps) {
                    org.bukkit.World monde = org.bukkit.Bukkit.getWorld(map);
                    String statut = obtenirStatutConfigurationMap(type, map, monde);
                    joueur.sendMessage(ChatColor.WHITE + "  " + statut + " " + map);
                }
            }
        }

        int mapsTemporaires = plugin.getGestionnairePrincipal().getGestionnaireMaps().getNombreMapsTemporaires();
        joueur.sendMessage("");
        joueur.sendMessage(ChatColor.AQUA + "Maps temporaires actives: " + ChatColor.WHITE + mapsTemporaires);
        joueur.sendMessage("");
        joueur.sendMessage(ChatColor.GRAY + "Legende:");
        joueur.sendMessage(ChatColor.GREEN + "  ✓“ " + ChatColor.WHITE + "Map completement configuree");
        joueur.sendMessage(ChatColor.GOLD + "  ⚠ " + ChatColor.WHITE + "Map existe mais configuration incomplete");
        joueur.sendMessage(ChatColor.RED + "  ✗ " + ChatColor.WHITE + "Map inexistante ou non chargee");
        joueur.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void nettoyerMapsTemporaires(Player joueur) {
        int nombreMapsAvant = plugin.getGestionnairePrincipal().getGestionnaireMaps().getNombreMapsTemporaires();

        joueur.sendMessage(ChatColor.YELLOW + "Nettoyage des maps temporaires en cours...");

        plugin.getGestionnairePrincipal().getGestionnaireMaps().nettoyerMapsTemporaires();

        joueur.sendMessage(ChatColor.GREEN + "Nettoyage termine ! " + nombreMapsAvant + " maps temporaires supprimees.");
    }

    private String obtenirStatutConfigurationMap(TypeMiniJeu type, String nomMap, org.bukkit.World monde) {
        if (monde == null) {
            return ChatColor.RED + "✗";
        }

        boolean configurationComplete = verifierConfigurationComplete(type, nomMap);

        if (configurationComplete) {
            return ChatColor.GREEN + "✓“";
        } else {
            return ChatColor.GOLD + "⚠";
        }
    }

    private boolean verifierConfigurationComplete(TypeMiniJeu type, String nomMap) {
        switch (type) {
            case HUNGER_GAMES:
                return verifierConfigurationHungerGames(nomMap);
            case JUMP_LEAGUE:
                return verifierConfigurationJumpLeague(nomMap);
            case GET_DOWN:
                return verifierConfigurationGetDown(nomMap);
            default:
                return false;
        }
    }

    private boolean verifierConfigurationHungerGames(String nomMap) {
        try {
            boolean aSpawns = plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                    .getGestionnaireSetup().verifierSpawnsConfigures(nomMap);
            boolean aBordure = plugin.getGestionnairePrincipal().getGestionnaireHungerGames()
                    .getGestionnaireSetup().verifierBordureConfiguree(nomMap);

            return aSpawns && aBordure;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifierConfigurationJumpLeague(String nomMap) {
        try {
            boolean aSpawn = plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                    .getGestionnaireSetup().verifierSpawnConfigure(nomMap);
            boolean aCheckpoints = plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                    .getGestionnaireSetup().verifierCheckpointsConfigures(nomMap);
            boolean aArrivees = plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                    .getGestionnaireSetup().verifierArriveesConfigurees(nomMap);
            boolean aPvp = plugin.getGestionnairePrincipal().getGestionnaireJumpLeague()
                    .getGestionnaireSetup().verifierPvpConfigure(nomMap);

            return aSpawn && aCheckpoints && aArrivees && aPvp;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifierConfigurationGetDown(String nomMap) {
        try {
            boolean aSpawns = plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                    .getGestionnaireSetup().verifierSpawnsConfigures(nomMap);
            boolean aArrivees = plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                    .getGestionnaireSetup().verifierArriveesConfigurees(nomMap);
            boolean aStuff = plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                    .getGestionnaireSetup().verifierStuffConfigure(nomMap);
            boolean aPvp = plugin.getGestionnairePrincipal().getGestionnaireGetDown()
                    .getGestionnaireSetup().verifierPvpConfigure(nomMap);

            return aSpawns && aArrivees && aStuff && aPvp;
        } catch (Exception e) {
            return false;
        }
    }

    private void afficherAideSetup(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━ KingFight - Aide ━━━━━━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "/tournoi" + ChatColor.WHITE + " - Ouvre le menu des mini-jeux");
        sender.sendMessage(ChatColor.YELLOW + "/kingfight stats" + ChatColor.WHITE + " - Affiche vos statistiques");
        sender.sendMessage(ChatColor.YELLOW + "/kingfight classement <type>" + ChatColor.WHITE + " - Affiche le classement");

        if (sender.hasPermission("kingfight.admin")) {
            sender.sendMessage(ChatColor.RED + "Commandes Admin:");
            sender.sendMessage(ChatColor.YELLOW + "/forcer <type>" + ChatColor.WHITE + " - Force un evenement (manuel, sans limite de temps)");
            sender.sendMessage(ChatColor.YELLOW + "/kingfight start <type>" + ChatColor.WHITE + " - Demarre un evenement (automatique, 2h)");
            sender.sendMessage(ChatColor.YELLOW + "/kingfight stop <type>" + ChatColor.WHITE + " - Arrete un evenement");
            sender.sendMessage(ChatColor.YELLOW + "/kingfight statut" + ChatColor.WHITE + " - Affiche le statut des evenements");
            sender.sendMessage(ChatColor.YELLOW + "/kingfight reload" + ChatColor.WHITE + " - Recharge la configuration");
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
