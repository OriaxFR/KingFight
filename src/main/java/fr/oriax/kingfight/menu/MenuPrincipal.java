package fr.oriax.kingfight.menu;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import fr.oriax.kingfight.gestionnaire.GestionnairePrincipal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;

public class MenuPrincipal implements Listener {

    private final KingFight plugin;
    private final String titreMenu = ChatColor.DARK_PURPLE + "KingFight - Tournois";
    private BukkitTask tacheMiseAJour;

    public MenuPrincipal(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        demarrerMiseAJourAutomatique();
    }

    public void ouvrirMenu(Player joueur) {
        Inventory menu = Bukkit.createInventory(null, 45, titreMenu);
        GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();

        menu.setItem(10, creerItemHungerGames(joueur, gestionnaire));
        menu.setItem(13, creerItemJumpLeague(joueur, gestionnaire));
        menu.setItem(16, creerItemGetDown(joueur, gestionnaire));

        menu.setItem(18, creerLigneEspacement());
        menu.setItem(19, creerLigneEspacement());
        menu.setItem(20, creerLigneEspacement());
        menu.setItem(21, creerLigneEspacement());
        menu.setItem(22, creerLigneEspacement());
        menu.setItem(23, creerLigneEspacement());
        menu.setItem(24, creerLigneEspacement());
        menu.setItem(25, creerLigneEspacement());
        menu.setItem(26, creerLigneEspacement());

        menu.setItem(31, creerItemStatistiques(joueur));

        menu.setItem(36, creerLigneEspacement());
        menu.setItem(37, creerLigneEspacement());
        menu.setItem(38, creerLigneEspacement());
        menu.setItem(39, creerLigneEspacement());
        menu.setItem(40, creerLigneEspacement());
        menu.setItem(41, creerLigneEspacement());
        menu.setItem(42, creerLigneEspacement());
        menu.setItem(43, creerLigneEspacement());
        menu.setItem(44, creerLigneEspacement());

        remplirEspacesVides(menu);

        joueur.openInventory(menu);
    }

    private ItemStack creerItemHungerGames(Player joueur, GestionnairePrincipal gestionnaire) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);

        meta.setDisplayName(ChatColor.RED + "Hunger Games");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Battle Royale classique");
        lore.add("");

        if (gestionnaire.estEvenementActif(TypeMiniJeu.HUNGER_GAMES)) {
            long tempsRestant = gestionnaire.getTempsRestantEvenement();
            int minutes = (int) (tempsRestant / 60000);
            int heures = minutes / 60;
            minutes = minutes % 60;

            lore.add(ChatColor.GREEN + "✓ Evenement actif");
            lore.add(ChatColor.YELLOW + "Temps restant: " + ChatColor.WHITE + heures + "h " + minutes + "m");
            
            lore.add("");
            
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, TypeMiniJeu.HUNGER_GAMES);
            lore.add(ChatColor.AQUA + "Parties restantes: " + ChatColor.WHITE + partiesRestantes);

            int points = gestionnaire.getGestionnaireClassements().getPoints(joueur, TypeMiniJeu.HUNGER_GAMES);
            lore.add(ChatColor.GOLD + "Vos points: " + ChatColor.WHITE + points);
            
            int position = gestionnaire.getGestionnaireClassements().getPosition(joueur, TypeMiniJeu.HUNGER_GAMES);
            lore.add(ChatColor.WHITE + "Position: " + ChatColor.GREEN + (position > 0 ? "#" + position : "Non classé"));

            int partiesEnCours = gestionnaire.getGestionnaireHungerGames().getNombrePartiesEnCours();
            lore.add(ChatColor.LIGHT_PURPLE + "Parties en cours: " + ChatColor.WHITE + partiesEnCours);
            
            lore = ajouterTopEvenement(lore, TypeMiniJeu.HUNGER_GAMES, gestionnaire);
        } else if (gestionnaire.estEvenementEnFinition(TypeMiniJeu.HUNGER_GAMES)) {
            lore.add(ChatColor.GOLD + "⏳ Evenement en finition");
            lore.add(ChatColor.YELLOW + "Attente fin des parties en cours...");
        } else {
            lore.add(ChatColor.RED + "✗ Evenement inactif");
            String prochainEvenement = obtenirProchainEvenement(TypeMiniJeu.HUNGER_GAMES);
            lore.add(ChatColor.GRAY + "Prochain: " + prochainEvenement);
            
            lore.add("");
            
            lore.add(ChatColor.GOLD + "Vos points: " + ChatColor.GRAY + "Mode Non Actif");
            lore.add(ChatColor.WHITE + "Position: " + ChatColor.GRAY + "Mode Non Actif");
        }

        lore.add("");
        if (gestionnaire.estEvenementActif(TypeMiniJeu.HUNGER_GAMES)) {
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, TypeMiniJeu.HUNGER_GAMES);
            boolean enFileAttente = gestionnaire.getGestionnaireHungerGames().estEnFileAttente(joueur);
            
            if (enFileAttente) {
                int positionFile = gestionnaire.getGestionnaireHungerGames().getTailleFileAttente();
                lore.add(ChatColor.YELLOW + "📋 En file d'attente (" + positionFile + " joueurs)");
                lore.add(ChatColor.GRAY + "Cliquez pour quitter la file !");
            } else if (partiesRestantes > 0) {
                lore.add(ChatColor.GREEN + "Cliquez pour rejoindre !");
            } else {
                lore.add(ChatColor.GOLD + "Plus de parties disponibles");
            }
        } else if (gestionnaire.estEvenementEnFinition(TypeMiniJeu.HUNGER_GAMES)) {
            lore.add(ChatColor.GOLD + "Evenement en finition");
        } else {
            lore.add(ChatColor.RED + "Non disponible");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack creerItemJumpLeague(Player joueur, GestionnairePrincipal gestionnaire) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        meta.setDisplayName(ChatColor.AQUA + "Jump League");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Parkour + PvP");
        lore.add("");

        if (gestionnaire.estEvenementActif(TypeMiniJeu.JUMP_LEAGUE)) {
            long tempsRestant = gestionnaire.getTempsRestantEvenement();
            int minutes = (int) (tempsRestant / 60000);
            int heures = minutes / 60;
            minutes = minutes % 60;

            lore.add(ChatColor.GREEN + "✓ Evenement actif");
            lore.add(ChatColor.YELLOW + "Temps restant: " + ChatColor.WHITE + heures + "h " + minutes + "m");
            
            lore.add("");
            
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, TypeMiniJeu.JUMP_LEAGUE);
            lore.add(ChatColor.AQUA + "Parties restantes: " + ChatColor.WHITE + partiesRestantes);

            int points = gestionnaire.getGestionnaireClassements().getPoints(joueur, TypeMiniJeu.JUMP_LEAGUE);
            lore.add(ChatColor.GOLD + "Vos points: " + ChatColor.WHITE + points);
            
            int position = gestionnaire.getGestionnaireClassements().getPosition(joueur, TypeMiniJeu.JUMP_LEAGUE);
            lore.add(ChatColor.WHITE + "Position: " + ChatColor.GREEN + (position > 0 ? "#" + position : "Non classé"));

            int partiesEnCours = gestionnaire.getGestionnaireJumpLeague().getNombrePartiesEnCours();
            lore.add(ChatColor.LIGHT_PURPLE + "Parties en cours: " + ChatColor.WHITE + partiesEnCours);
            
            lore = ajouterTopEvenement(lore, TypeMiniJeu.JUMP_LEAGUE, gestionnaire);
        } else if (gestionnaire.estEvenementEnFinition(TypeMiniJeu.JUMP_LEAGUE)) {
            lore.add(ChatColor.GOLD + "⏳ Evenement en finition");
            lore.add(ChatColor.YELLOW + "Attente fin des parties en cours...");
        } else {
            lore.add(ChatColor.RED + "✗ Evenement inactif");
            String prochainEvenement = obtenirProchainEvenement(TypeMiniJeu.JUMP_LEAGUE);
            lore.add(ChatColor.GRAY + "Prochain: " + prochainEvenement);
            
            lore.add("");
            
            lore.add(ChatColor.GOLD + "Vos points: " + ChatColor.GRAY + "Mode Non Actif");
            lore.add(ChatColor.WHITE + "Position: " + ChatColor.GRAY + "Mode Non Actif");
        }

        lore.add("");
        if (gestionnaire.estEvenementActif(TypeMiniJeu.JUMP_LEAGUE)) {
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, TypeMiniJeu.JUMP_LEAGUE);
            boolean enFileAttente = gestionnaire.getGestionnaireJumpLeague().estEnFileAttente(joueur);
            
            if (enFileAttente) {
                int positionFile = gestionnaire.getGestionnaireJumpLeague().getTailleFileAttente();
                lore.add(ChatColor.YELLOW + "📋 En file d'attente (" + positionFile + " joueurs)");
                lore.add(ChatColor.GRAY + "Cliquez pour quitter la file !");
            } else if (partiesRestantes > 0) {
                lore.add(ChatColor.GREEN + "Cliquez pour rejoindre !");
            } else {
                lore.add(ChatColor.GOLD + "Plus de parties disponibles");
            }
        } else if (gestionnaire.estEvenementEnFinition(TypeMiniJeu.JUMP_LEAGUE)) {
            lore.add(ChatColor.GOLD + "Evenement en finition");
        } else {
            lore.add(ChatColor.RED + "Non disponible");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack creerItemGetDown(Player joueur, GestionnairePrincipal gestionnaire) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        meta.setDisplayName(ChatColor.DARK_PURPLE + "GetDown");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Descente + Shop + PvP");
        lore.add("");

        if (gestionnaire.estEvenementActif(TypeMiniJeu.GET_DOWN)) {
            long tempsRestant = gestionnaire.getTempsRestantEvenement();
            int minutes = (int) (tempsRestant / 60000);
            int heures = minutes / 60;
            minutes = minutes % 60;

            lore.add(ChatColor.GREEN + "✓ Evenement actif");
            lore.add(ChatColor.YELLOW + "Temps restant: " + ChatColor.WHITE + heures + "h " + minutes + "m");
            
            lore.add("");
            
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, TypeMiniJeu.GET_DOWN);
            lore.add(ChatColor.AQUA + "Parties restantes: " + ChatColor.WHITE + partiesRestantes);

            int points = gestionnaire.getGestionnaireClassements().getPoints(joueur, TypeMiniJeu.GET_DOWN);
            lore.add(ChatColor.GOLD + "Vos points: " + ChatColor.WHITE + points);
            
            int position = gestionnaire.getGestionnaireClassements().getPosition(joueur, TypeMiniJeu.GET_DOWN);
            lore.add(ChatColor.WHITE + "Position: " + ChatColor.GREEN + (position > 0 ? "#" + position : "Non classé"));

            int partiesEnCours = gestionnaire.getGestionnaireGetDown().getNombrePartiesEnCours();
            lore.add(ChatColor.LIGHT_PURPLE + "Parties en cours: " + ChatColor.WHITE + partiesEnCours);
            
            lore = ajouterTopEvenement(lore, TypeMiniJeu.GET_DOWN, gestionnaire);
        } else if (gestionnaire.estEvenementEnFinition(TypeMiniJeu.GET_DOWN)) {
            lore.add(ChatColor.GOLD + "⏳ Evenement en finition");
            lore.add(ChatColor.YELLOW + "Attente fin des parties en cours...");
        } else {
            lore.add(ChatColor.RED + "✗ Evenement inactif");
            String prochainEvenement = obtenirProchainEvenement(TypeMiniJeu.GET_DOWN);
            lore.add(ChatColor.GRAY + "Prochain: " + prochainEvenement);
            
            lore.add("");
            
            lore.add(ChatColor.GOLD + "Vos points: " + ChatColor.GRAY + "Mode Non Actif");
            lore.add(ChatColor.WHITE + "Position: " + ChatColor.GRAY + "Mode Non Actif");
        }

        lore.add("");
        if (gestionnaire.estEvenementActif(TypeMiniJeu.GET_DOWN)) {
            int partiesRestantes = gestionnaire.getGestionnaireJoueurs().getPartiesRestantes(joueur, TypeMiniJeu.GET_DOWN);
            boolean enFileAttente = gestionnaire.getGestionnaireGetDown().estEnFileAttente(joueur);
            
            if (enFileAttente) {
                int positionFile = gestionnaire.getGestionnaireGetDown().getTailleFileAttente();
                lore.add(ChatColor.YELLOW + "📋 En file d'attente (" + positionFile + " joueurs)");
                lore.add(ChatColor.GRAY + "Cliquez pour quitter la file !");
            } else if (partiesRestantes > 0) {
                lore.add(ChatColor.GREEN + "Cliquez pour rejoindre !");
            } else {
                lore.add(ChatColor.GOLD + "Plus de parties disponibles");
            }
        } else if (gestionnaire.estEvenementEnFinition(TypeMiniJeu.GET_DOWN)) {
            lore.add(ChatColor.GOLD + "Evenement en finition");
        } else {
            lore.add(ChatColor.RED + "Non disponible");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack creerItemStatistiques(Player joueur) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        meta.setDisplayName(ChatColor.YELLOW + "Informations Joueur");

        List<String> lore = new ArrayList<>();
        
        Calendar maintenant = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        String[] joursNoms = {"", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
        String[] moisNoms = {"", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", 
                            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        
        String jourNom = joursNoms[maintenant.get(Calendar.DAY_OF_WEEK)];
        String moisNom = moisNoms[maintenant.get(Calendar.MONTH) + 1];
        int jour = maintenant.get(Calendar.DAY_OF_MONTH);
        int annee = maintenant.get(Calendar.YEAR);
        int heure = maintenant.get(Calendar.HOUR_OF_DAY);
        int minute = maintenant.get(Calendar.MINUTE);
        
        lore.add(ChatColor.AQUA + "Joueur: " + ChatColor.WHITE + joueur.getName());
        lore.add("");
        lore.add(ChatColor.GOLD + "Date: " + ChatColor.WHITE + jourNom + " " + jour + " " + moisNom + " " + annee);
        lore.add(ChatColor.GOLD + "Heure: " + ChatColor.WHITE + String.format("%02d:%02d", heure, minute));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack creerLigneEspacement() {
        ItemStack verre = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = verre.getItemMeta();
        meta.setDisplayName(" ");
        verre.setItemMeta(meta);
        return verre;
    }

    private void remplirEspacesVides(Inventory menu) {
        ItemStack verre = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = verre.getItemMeta();
        meta.setDisplayName(" ");
        verre.setItemMeta(meta);

        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, verre);
            }
        }
    }

    @EventHandler
    public void surClicMenu(InventoryClickEvent event) {
        if (!event.getInventory().getTitle().equals(titreMenu)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player joueur = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta()) return;

        String nom = item.getItemMeta().getDisplayName();
        GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();

        if (nom.contains("Hunger Games")) {
            if (gestionnaire.estEvenementActif(TypeMiniJeu.HUNGER_GAMES) &&
                !gestionnaire.estEvenementEnFinition(TypeMiniJeu.HUNGER_GAMES)) {

                joueur.closeInventory();

                if (gestionnaire.getGestionnaireHungerGames().estEnFileAttente(joueur)) {
                    gestionnaire.getGestionnaireHungerGames().retirerJoueurDeFileAttente(joueur);
                } else if (gestionnaire.getGestionnaireJoueurs().peutJouer(joueur, TypeMiniJeu.HUNGER_GAMES)) {
                    gestionnaire.getGestionnaireHungerGames().ajouterJoueurEnFileAttente(joueur);
                } else {
                    joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce mode de jeu actuellement.");
                }
            } else {
                joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce mode de jeu actuellement.");
            }
        } else if (nom.contains("Jump League")) {
            if (gestionnaire.estEvenementActif(TypeMiniJeu.JUMP_LEAGUE) &&
                !gestionnaire.estEvenementEnFinition(TypeMiniJeu.JUMP_LEAGUE)) {

                joueur.closeInventory();

                if (gestionnaire.getGestionnaireJumpLeague().estEnFileAttente(joueur)) {
                    gestionnaire.getGestionnaireJumpLeague().retirerJoueurDeFileAttente(joueur);
                } else if (gestionnaire.getGestionnaireJoueurs().peutJouer(joueur, TypeMiniJeu.JUMP_LEAGUE)) {
                    gestionnaire.getGestionnaireJumpLeague().ajouterJoueurEnFileAttente(joueur);
                } else {
                    joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce mode de jeu actuellement.");
                }
            } else {
                joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce mode de jeu actuellement.");
            }
        } else if (nom.contains("GetDown")) {
            if (gestionnaire.estEvenementActif(TypeMiniJeu.GET_DOWN) &&
                !gestionnaire.estEvenementEnFinition(TypeMiniJeu.GET_DOWN)) {

                joueur.closeInventory();

                if (gestionnaire.getGestionnaireGetDown().estEnFileAttente(joueur)) {
                    gestionnaire.getGestionnaireGetDown().retirerJoueurDeFileAttente(joueur);
                } else if (gestionnaire.getGestionnaireJoueurs().peutJouer(joueur, TypeMiniJeu.GET_DOWN)) {
                    gestionnaire.getGestionnaireGetDown().ajouterJoueurEnFileAttente(joueur);
                } else {
                    joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce mode de jeu actuellement.");
                }
            } else {
                joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce mode de jeu actuellement.");
            }
        }
    }

    private String obtenirProchainEvenement(TypeMiniJeu type) {
        FileConfiguration config = plugin.getConfig();
        Calendar maintenant = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        int jourActuel = maintenant.get(Calendar.DAY_OF_WEEK);
        int heureActuelle = maintenant.get(Calendar.HOUR_OF_DAY);
        
        String[] joursConfig = {"dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};
        String[] joursAffichage = {"Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
        
        String typeConfig = type.getNomConfig();
        
        for (int i = 0; i < 7; i++) {
            int jourIndex = (jourActuel - 1 + i) % 7;
            String jourConfig = joursConfig[jourIndex];
            
            if (config.contains("evenements.horaires." + jourConfig)) {
                String typeJour = config.getString("evenements.horaires." + jourConfig + ".type");
                int heureDebut = config.getInt("evenements.horaires." + jourConfig + ".debut");
                
                if (typeConfig.equals(typeJour)) {
                    if (i == 0 && heureDebut > heureActuelle) {
                        return "Aujourd'hui " + heureDebut + "h00";
                    } else if (i == 1) {
                        return "Demain " + heureDebut + "h00";
                    } else if (i > 1) {
                        return joursAffichage[jourIndex] + " " + heureDebut + "h00";
                    }
                }
            }
        }
        
        return "Prochainement";
    }

    private List<String> ajouterTopEvenement(List<String> lore, TypeMiniJeu type, GestionnairePrincipal gestionnaire) {
        FileConfiguration config = plugin.getConfig();
        
        if (!config.getBoolean("menu.afficher-top-evenement", true)) {
            return lore;
        }
        
        int tailleTop = config.getInt("menu.taille-top-evenement", 10);
        if (tailleTop < 1) tailleTop = 1;
        if (tailleTop > 20) tailleTop = 20;
        
        List<Map.Entry<UUID, Integer>> topJoueurs = gestionnaire.getGestionnaireClassements()
                .getClassementTrie(type, tailleTop);
        
        if (topJoueurs.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Aucun joueur classé pour le moment");
            return lore;
        }
        
        lore.add("");
        lore.add(ChatColor.GOLD + "━━━ TOP " + Math.min(tailleTop, topJoueurs.size()) + " ━━━");
        
        for (int i = 0; i < topJoueurs.size(); i++) {
            Map.Entry<UUID, Integer> entry = topJoueurs.get(i);
            String nomJoueur = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (nomJoueur == null) nomJoueur = "Joueur inconnu";
            
            int position = i + 1;
            String cashPrize = obtenirCashPrize(position, config);
            
            String couleurPosition = obtenirCouleurPosition(position);
            String ligne = couleurPosition + "#" + position + " " + ChatColor.WHITE + nomJoueur + 
                          ChatColor.GRAY + " - " + ChatColor.YELLOW + entry.getValue() + " pts";
            
            if (!cashPrize.isEmpty()) {
                ligne += ChatColor.GREEN + " (" + cashPrize + ")";
            }
            
            lore.add(ligne);
        }
        
        return lore;
    }
    
    private String obtenirCashPrize(int position, FileConfiguration config) {
        String key = "menu.cash-prizes.top" + position;
        return config.getString(key, "");
    }
    
    private String obtenirCouleurPosition(int position) {
        switch (position) {
            case 1: return ChatColor.GOLD.toString();
            case 2: return ChatColor.GRAY.toString();
            case 3: return ChatColor.DARK_RED.toString();
            case 4:
            case 5: return ChatColor.GREEN.toString();
            default: return ChatColor.AQUA.toString();
        }
    }
    
    private void demarrerMiseAJourAutomatique() {
        if (tacheMiseAJour != null) {
            tacheMiseAJour.cancel();
        }
        
        tacheMiseAJour = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            mettreAJourMenusOuverts();
        }, 20L, 60L);
    }
    
    private void mettreAJourMenusOuverts() {
        GestionnairePrincipal gestionnaire = plugin.getGestionnairePrincipal();
        
        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            if (joueur.getOpenInventory() != null && 
                joueur.getOpenInventory().getTitle().equals(titreMenu)) {
                
                boolean evenementActif = gestionnaire.estEvenementActif(TypeMiniJeu.HUNGER_GAMES) ||
                                       gestionnaire.estEvenementActif(TypeMiniJeu.JUMP_LEAGUE) ||
                                       gestionnaire.estEvenementActif(TypeMiniJeu.GET_DOWN);
                
                if (evenementActif) {
                    Inventory menu = joueur.getOpenInventory().getTopInventory();
                    
                    menu.setItem(10, creerItemHungerGames(joueur, gestionnaire));
                    menu.setItem(13, creerItemJumpLeague(joueur, gestionnaire));
                    menu.setItem(16, creerItemGetDown(joueur, gestionnaire));
                }
            }
        }
    }
    
    public void arreter() {
        if (tacheMiseAJour != null) {
            tacheMiseAJour.cancel();
            tacheMiseAJour = null;
        }
    }
}