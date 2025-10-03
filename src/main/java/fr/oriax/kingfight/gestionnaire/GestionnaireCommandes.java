package fr.oriax.kingfight.gestionnaire;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import fr.oriax.kingfight.jumpleague.ValidateurConfigurationJumpLeague;
import fr.oriax.kingfight.menu.MenuPrincipal;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GestionnaireCommandes implements CommandExecutor {

    private final KingFight plugin;
    private final MenuPrincipal menuPrincipal;

    public GestionnaireCommandes(KingFight plugin) {
        this.plugin = plugin;
        this.menuPrincipal = new MenuPrincipal(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("tournoi")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Cette commande ne peut etre executee que par un joueur !");
                return true;
            }

            Player joueur = (Player) sender;
            menuPrincipal.ouvrirMenu(joueur);
            return true;
        }

        if (command.getName().equalsIgnoreCase("lobby")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Cette commande ne peut etre executee que par un joueur !");
                return true;
            }

            Player joueur = (Player) sender;
            GestionnairePrincipal gestionnairePrincipal = plugin.getGestionnairePrincipal();

            if (gestionnairePrincipal.getGestionnaireJoueurs().estEnPartie(joueur)) {
                TypeMiniJeu type = gestionnairePrincipal.getGestionnaireJoueurs().getTypePartie(joueur);
                String idPartie = gestionnairePrincipal.getGestionnaireJoueurs().getIdPartie(joueur);

                try {
                    switch (type) {
                        case HUNGER_GAMES:
                            gestionnairePrincipal.getGestionnaireHungerGames().retirerJoueurDePartie(joueur, idPartie);
                            break;
                        case JUMP_LEAGUE:
                            gestionnairePrincipal.getGestionnaireJumpLeague().retirerJoueurDePartie(joueur, idPartie);
                            break;
                        case GET_DOWN:
                            gestionnairePrincipal.getGestionnaireGetDown().retirerJoueurDePartie(joueur, idPartie);
                            break;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du retrait du joueur " + joueur.getName() + " via /lobby: " + e.getMessage());
                    gestionnairePrincipal.getGestionnaireJoueurs().forcerRetraitJoueur(joueur);
                }
            }

            gestionnairePrincipal.getGestionnaireLobby().teleporterVersLobby(joueur);
            return true;
        }

        if (command.getName().equalsIgnoreCase("forcer")) {
            if (!sender.hasPermission("kingfight.admin")) {
                sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /forcer <hungergames|jumpleague|getdown>");
                return true;
            }

            TypeMiniJeu type = TypeMiniJeu.parNom(args[0]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Type de mini-jeu invalide ! Utilisez: hungergames, jumpleague ou getdown");
                return true;
            }

            GestionnairePrincipal gestionnairePrincipal = plugin.getGestionnairePrincipal();

            if (!gestionnairePrincipal.peutDemarrerEvenement(type)) {
                sender.sendMessage(ChatColor.RED + "Impossible de forcer l'evenement " + type.getNomAffichage() + " !");

                afficherErreursConfiguration(sender, gestionnairePrincipal, type);

                return true;
            }

            gestionnairePrincipal.changerEvenementActuel(type, true);
            sender.sendMessage(ChatColor.GREEN + "Evenement " + type.getNomAffichage() + " force avec succes !");
            return true;
        }

        if (command.getName().equalsIgnoreCase("kingfight")) {
            if (args.length == 0) {
                if (sender.hasPermission("kingfight.admin")) {
                    afficherAideComplete(sender);
                } else {
                    afficherAideGenerale(sender);
                }
                return true;
            }

            String sousCommande = args[0].toLowerCase();

            switch (sousCommande) {
                case "help":
                case "aide":
                    if (sender.hasPermission("kingfight.admin")) {
                        afficherAideComplete(sender);
                    } else {
                        afficherAideGenerale(sender);
                    }
                    break;

                case "stats":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Cette commande ne peut etre executee que par un joueur !");
                        return true;
                    }
                    afficherStatistiques((Player) sender);
                    break;

                case "reload":
                    if (!sender.hasPermission("kingfight.admin")) {
                        sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                        return true;
                    }
                    plugin.reloadConfig();
                    plugin.getGestionnairePrincipal().rechargerConfiguration();
                    sender.sendMessage(ChatColor.GREEN + "Configuration rechargee avec succes !");
                    
                    int maxParties = plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getMaxPartiesParJoueurPublic();
                    sender.sendMessage(ChatColor.YELLOW + "» Nouvelle limite de parties par joueur: " + ChatColor.WHITE + maxParties);
                    sender.sendMessage(ChatColor.GRAY + "» Les limitations seront appliquees immediatement aux nouveaux joueurs");
                    break;

                case "classement":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /kingfight classement <hungergames|jumpleague|getdown>");
                        return true;
                    }

                    TypeMiniJeu typeClassement = TypeMiniJeu.parNom(args[1]);
                    if (typeClassement == null) {
                        sender.sendMessage(ChatColor.RED + "Type de mini-jeu invalide !");
                        return true;
                    }

                    afficherClassement(sender, typeClassement);
                    break;

                case "start":
                    if (!sender.hasPermission("kingfight.admin")) {
                        sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /kingfight start <hungergames|jumpleague|getdown>");
                        return true;
                    }

                    TypeMiniJeu typeStart = TypeMiniJeu.parNom(args[1]);
                    if (typeStart == null) {
                        sender.sendMessage(ChatColor.RED + "Type de mini-jeu invalide !");
                        return true;
                    }

                    GestionnairePrincipal gestionnairePrincipal = plugin.getGestionnairePrincipal();

                    if (gestionnairePrincipal.estEvenementActif(typeStart)) {
                        sender.sendMessage(ChatColor.RED + "L'evenement " + typeStart.getNomAffichage() + " est deja en cours !");
                        return true;
                    }

                    if (!gestionnairePrincipal.peutDemarrerEvenement(typeStart)) {
                        sender.sendMessage(ChatColor.RED + "Impossible de demarrer l'evenement " + typeStart.getNomAffichage() + " !");

                        afficherErreursConfiguration(sender, gestionnairePrincipal, typeStart);

                        return true;
                    }

                    gestionnairePrincipal.changerEvenementActuel(typeStart);
                    sender.sendMessage(ChatColor.GREEN + "Evenement " + typeStart.getNomAffichage() + " demarre !");
                    break;

                case "stop":
                    if (!sender.hasPermission("kingfight.admin")) {
                        sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /kingfight stop <hungergames|jumpleague|getdown>");
                        return true;
                    }

                    TypeMiniJeu typeStop = TypeMiniJeu.parNom(args[1]);
                    if (typeStop == null) {
                        sender.sendMessage(ChatColor.RED + "Type de mini-jeu invalide !");
                        return true;
                    }

                    if (!plugin.getGestionnairePrincipal().estEvenementActif(typeStop)) {
                        sender.sendMessage(ChatColor.RED + "L'evenement " + typeStop.getNomAffichage() + " n'est pas en cours !");
                        return true;
                    }

                    plugin.getGestionnairePrincipal().changerEvenementActuel(null, false);
                    sender.sendMessage(ChatColor.GREEN + "Evenement " + typeStop.getNomAffichage() + " arrete !");
                    break;

                case "statut":
                    if (!sender.hasPermission("kingfight.admin")) {
                        sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                        return true;
                    }

                    afficherStatutEvenements(sender);
                    break;

                case "nettoyer-classements":
                    if (!sender.hasPermission("kingfight.admin")) {
                        sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                        return true;
                    }

                    plugin.getGestionnairePrincipal().getGestionnaireClassements().supprimerFichierClassements();
                    plugin.getGestionnairePrincipal().getGestionnaireClassements().reinitialiserTousLesClassements();
                    sender.sendMessage("§a» Fichier de classements supprimé et classements réinitialisés !");
                    break;

                case "nettoyer-backups":
                    if (!sender.hasPermission("kingfight.admin")) {
                        sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
                        return true;
                    }

                    sender.sendMessage("§e» Nettoyage des anciens backups en cours...");
                    plugin.getGestionnairePrincipal().getGestionnaireBackupAutomatique().nettoyerAnciennesBackups();
                    sender.sendMessage("§a» Nettoyage des backups terminé ! Consultez la console pour les détails.");
                    break;

                default:
                    sender.sendMessage(ChatColor.RED + "Sous-commande inconnue ! Utilisez /kingfight help");
                    break;
            }
            return true;
        }

        return false;
    }

    private void afficherAideGenerale(CommandSender sender) {
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
            sender.sendMessage(ChatColor.YELLOW + "/kingfight nettoyer-backups" + ChatColor.WHITE + " - Nettoie les anciens fichiers de backup");
            sender.sendMessage(ChatColor.YELLOW + "/kingfight nettoyer-classements" + ChatColor.WHITE + " - Remet à zéro les classements");
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void afficherStatistiques(Player joueur) {
        GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();

        joueur.sendMessage(ChatColor.GOLD + "━━━━━━━━━━ Vos Statistiques ━━━━━━━━━━");

        for (TypeMiniJeu type : TypeMiniJeu.values()) {
            int points = gestionnaire.getGestionnaireClassements().getPoints(joueur, type);
            int position = gestionnaire.getGestionnaireClassements().getPosition(joueur, type);
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, type);

            joueur.sendMessage(ChatColor.YELLOW + type.getNomAffichage() + ":");
            joueur.sendMessage(ChatColor.WHITE + "  Points: " + ChatColor.GREEN + points);
            joueur.sendMessage(ChatColor.WHITE + "  Position: " + ChatColor.GREEN + (position > 0 ? "#" + position : "Non classe"));
            joueur.sendMessage(ChatColor.WHITE + "  Parties restantes: " + ChatColor.GREEN + partiesRestantes);
        }

        joueur.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void afficherClassement(CommandSender sender, TypeMiniJeu type) {
        GestionnairePrincipal gestionnairePrincipal = plugin.getGestionnairePrincipal();

        if (!gestionnairePrincipal.estEvenementActif(type)) {
            sender.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§c§l              » CLASSEMENT INDISPONIBLE «");
            sender.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("");
            sender.sendMessage("§e» Le classement " + type.getNomAffichage() + " n'est disponible que pendant les tournois.");
            sender.sendMessage("§7» Attendez qu'un tournoi " + type.getNomAffichage() + " soit lance.");
            sender.sendMessage("");
            return;
        }

        List<Map.Entry<UUID, Integer>> classement = gestionnairePrincipal.getGestionnaireClassements().getClassementTrie(type, 10);

        sender.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§6§l            » CLASSEMENT " + type.getNomAffichage().toUpperCase() + " «");
        sender.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");

        if (classement.isEmpty()) {
            sender.sendMessage("§7» Aucun joueur classe pour le moment.");
            sender.sendMessage("§e» Participez a des parties pour apparaitre dans le classement !");
            sender.sendMessage("");
            return;
        }

        for (int i = 0; i < classement.size(); i++) {
            Map.Entry<UUID, Integer> entry = classement.get(i);
            String nomJoueur = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
            if (nomJoueur == null) nomJoueur = "Joueur Inconnu";

            String couleur = i == 0 ? "§6" : i == 1 ? "§e" : i == 2 ? "§7" : "§f";
            String medaille = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "§8»";

            sender.sendMessage(couleur + medaille + " #" + (i + 1) + " §f" + nomJoueur +
                             " §7- §a" + entry.getValue() + " points");
        }

        sender.sendMessage("");
        sender.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void afficherStatutEvenements(CommandSender sender) {
        GestionnairePrincipal gestionnairePrincipal = plugin.getGestionnairePrincipal();

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━ Statut des Evenements ━━━━━━━━━━");

        for (TypeMiniJeu type : TypeMiniJeu.values()) {
            boolean actif = gestionnairePrincipal.estEvenementActif(type);
            boolean enFinition = gestionnairePrincipal.estEvenementEnFinition(type);

            String statut;
            ChatColor couleur;

            if (actif && !enFinition) {
                statut = "EN COURS";
                couleur = ChatColor.GREEN;
            } else if (enFinition) {
                statut = "EN FINITION";
                couleur = ChatColor.YELLOW;
            } else {
                statut = "ARRETE";
                couleur = ChatColor.RED;
            }

            sender.sendMessage(ChatColor.WHITE + type.getNomAffichage() + ": " + couleur + statut);
        }

        TypeMiniJeu evenementActuel = gestionnairePrincipal.getEvenementActuel();
        if (evenementActuel != null) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "Evenement principal: " + ChatColor.WHITE + evenementActuel.getNomAffichage());

            if (gestionnairePrincipal.estEvenementForce()) {
                sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.RED + "FORCE MANUELLEMENT");
                sender.sendMessage(ChatColor.GRAY + "Cet evenement ne se terminera pas automatiquement");
            } else {
                long tempsRestant = gestionnairePrincipal.getTempsRestantEvenement();
                if (tempsRestant > 0) {
                    long heures = tempsRestant / (1000 * 60 * 60);
                    long minutes = (tempsRestant % (1000 * 60 * 60)) / (1000 * 60);
                    sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.GREEN + "AUTOMATIQUE");
                    sender.sendMessage(ChatColor.AQUA + "Temps restant: " + ChatColor.WHITE + heures + "h " + minutes + "min");
                }
            }
        } else {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "Aucun evenement principal en cours");
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void afficherAideComplete(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "━━━━━━━━━━ KINGFIGHT - AIDE COMPLETE ━━━━━━━━━━");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "COMMANDES JOUEURS:");
        sender.sendMessage(ChatColor.YELLOW + "/tournoi" + ChatColor.WHITE + " - Ouvre le menu des mini-jeux");
        sender.sendMessage(ChatColor.YELLOW + "/lobby" + ChatColor.WHITE + " - Retourne au lobby principal");
        sender.sendMessage(ChatColor.YELLOW + "/kf stats" + ChatColor.WHITE + " - Affiche vos statistiques");
        sender.sendMessage(ChatColor.YELLOW + "/kf classement <type>" + ChatColor.WHITE + " - Classement d'un mini-jeu");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "COMMANDES ADMINISTRATEUR:");
        sender.sendMessage(ChatColor.YELLOW + "/forcer <type>" + ChatColor.WHITE + " - Force un evenement (manuel, sans limite de temps)");
        sender.sendMessage(ChatColor.YELLOW + "/kf start <type>" + ChatColor.WHITE + " - Demarre un evenement (automatique, 2h)");
        sender.sendMessage(ChatColor.YELLOW + "/kf stop <type>" + ChatColor.WHITE + " - Arrete un evenement");
        sender.sendMessage(ChatColor.YELLOW + "/kf statut" + ChatColor.WHITE + " - Affiche le statut des evenements");
        sender.sendMessage(ChatColor.YELLOW + "/kf nettoyer-classements" + ChatColor.WHITE + " - Supprime le fichier de classements persistant");
        sender.sendMessage(ChatColor.YELLOW + "/kf reload" + ChatColor.WHITE + " - Recharge la configuration");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "SETUP GENERAL:");
        sender.sendMessage(ChatColor.YELLOW + "/setup info" + ChatColor.WHITE + " - Infos des maps");
        sender.sendMessage(ChatColor.YELLOW + "/setup nettoyer" + ChatColor.WHITE + " - Nettoie les maps temporaires");
        sender.sendMessage(ChatColor.YELLOW + "/setup lobby spawn" + ChatColor.WHITE + " - Definit le spawn lobby");
        sender.sendMessage(ChatColor.YELLOW + "/setup lobby info" + ChatColor.WHITE + " - Infos du lobby");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "GESTION DES MAPS:");
        sender.sendMessage(ChatColor.YELLOW + "/setup maps <jeu> ajouter <nom>" + ChatColor.WHITE + " - Ajoute une map");
        sender.sendMessage(ChatColor.YELLOW + "/setup maps <jeu> retirer <nom>" + ChatColor.WHITE + " - Retire une map");
        sender.sendMessage(ChatColor.YELLOW + "/setup maps <jeu> lister" + ChatColor.WHITE + " - Liste les maps");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "SETUP HUNGER GAMES:");
        sender.sendMessage(ChatColor.YELLOW + "/setup hungergames spawn <num>" + ChatColor.WHITE + " - Spawn joueur");
        sender.sendMessage(ChatColor.YELLOW + "/setup hungergames bordure <rayon>" + ChatColor.WHITE + " - Centre bordure");
        sender.sendMessage(ChatColor.YELLOW + "/setup hungergames supprimer spawn" + ChatColor.WHITE + " - Supprimer dernier spawn");
        sender.sendMessage(ChatColor.GRAY + "» Les coffres sont detectes automatiquement sur la map");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.BLUE + "" + ChatColor.BOLD + "SETUP JUMP LEAGUE:");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague spawn <num>" + ChatColor.WHITE + " - Spawn joueur (1-16)");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague checkpoint <num> <n>" + ChatColor.WHITE + " - Checkpoint");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague arrivee <num>" + ChatColor.WHITE + " - Ligne arrivee");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague coffre <num> <n>" + ChatColor.WHITE + " - Coffre recompense");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague pvp <num>" + ChatColor.WHITE + " - Spawn PvP (1-16)");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague bordure <rayon>" + ChatColor.WHITE + " - Centre bordure Arene PVP");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague supprimer <spawn|checkpoint|pvp> [num]" + ChatColor.WHITE + " - Supprimer dernier element");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague corriger-checkpoints <monde>" + ChatColor.WHITE + " - Corriger precision checkpoints");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague multi-maps" + ChatColor.WHITE + " - Voir config multi-maps");
        sender.sendMessage(ChatColor.YELLOW + "/setup jumpleague info" + ChatColor.WHITE + " - Infos setup");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "SETUP GET DOWN:");
        sender.sendMessage(ChatColor.GRAY + "1. Configuration des maps:");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown map-parcours <1|2|3>" + ChatColor.WHITE + " - Definir map originale parcours");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown map-pvp" + ChatColor.WHITE + " - Definir map originale PvP");
        sender.sendMessage(ChatColor.GRAY + "2. Configuration des spawns:");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown spawn <parcours>" + ChatColor.WHITE + " - Spawn descente (1-3)");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown spawn-pvp <num>" + ChatColor.WHITE + " - Spawn PvP sur map PvP");
        sender.sendMessage(ChatColor.GRAY + "3. Configuration des zones:");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown hauteur <parcours> <hauteur-y>" + ChatColor.WHITE + " - Hauteur d'arrivee");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown bordure [rayon]" + ChatColor.WHITE + " - Centre bordure Arene PVP");
        sender.sendMessage(ChatColor.GRAY + "4. Utilitaires:");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown info" + ChatColor.WHITE + " - Infos setup");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown multi-maps" + ChatColor.WHITE + " - Voir config multi-maps");
        sender.sendMessage(ChatColor.YELLOW + "/setup getdown supprimer <spawn|pvp>" + ChatColor.WHITE + " - Supprimer dernier element");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.BOLD + "TYPES DE JEUX:");
        sender.sendMessage(ChatColor.DARK_RED + "hungergames" + ChatColor.WHITE + " - Combat survie avec coffres");
        sender.sendMessage(ChatColor.BLUE + "jumpleague" + ChatColor.WHITE + " - Parkour avec checkpoints");
        sender.sendMessage(ChatColor.DARK_GREEN + "getdown" + ChatColor.WHITE + " - Descente rapide avec combat");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
    }

    private void afficherErreursConfiguration(CommandSender sender, GestionnairePrincipal gestionnairePrincipal, TypeMiniJeu type) {
        if (!gestionnairePrincipal.getGestionnaireMaps().aMapsConfigurees(type)) {
            sender.sendMessage(ChatColor.RED + "Aucune map n'est configuree pour ce mini-jeu.");
            sender.sendMessage(ChatColor.YELLOW + "Configurez au moins une map dans le fichier maps.yml");
        }

        if (!gestionnairePrincipal.getGestionnaireLobby().estSpawnLobbyDefini()) {
            sender.sendMessage(ChatColor.RED + "Le spawn du lobby n'est pas defini.");
            sender.sendMessage(ChatColor.YELLOW + "Utilisez /setup lobby spawn pour definir le spawn du lobby");
        }

        if (type == TypeMiniJeu.JUMP_LEAGUE) {
            ValidateurConfigurationJumpLeague.ResultatValidation validation =
                gestionnairePrincipal.getGestionnaireJumpLeague().validerConfiguration();

            if (!validation.estValide()) {
                sender.sendMessage(ChatColor.RED + "Erreurs de configuration JumpLeague :");
                for (String erreur : validation.getErreurs()) {
                    sender.sendMessage(ChatColor.RED + "- " + erreur);
                }

                sender.sendMessage("");
                sender.sendMessage(ChatColor.YELLOW + "Utilisez les commandes /setup jumpleague pour configurer les maps :");
                sender.sendMessage(ChatColor.YELLOW + "- /setup jumpleague spawn <num> : Definir spawn joueur");
                sender.sendMessage(ChatColor.YELLOW + "- /setup jumpleague checkpoint <num> <n> : Ajouter checkpoint");
                sender.sendMessage(ChatColor.YELLOW + "- /setup jumpleague coffre <num> <n> : Ajouter coffre");
                sender.sendMessage(ChatColor.YELLOW + "- /setup jumpleague pvp <num> : Definir spawn PvP");
            }

            if (!validation.getAvertissements().isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Avertissements JumpLeague :");
                for (String avertissement : validation.getAvertissements()) {
                    sender.sendMessage(ChatColor.YELLOW + "- " + avertissement);
                }
            }
        }
    }
}
