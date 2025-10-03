package fr.oriax.kingfight.hungergames;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class EvenementsHungerGames implements Listener {

    private final KingFight plugin;

    public EvenementsHungerGames(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void surDegatsJoueur(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player joueur = (Player) event.getEntity();

        if (!estDansPartieHungerGames(joueur)) return;

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.getEtat() != PartieHungerGames.EtatPartie.EN_COURS) {
            event.setCancelled(true);
            return;
        }

        if (partie.estInvincibiliteActive() && event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent eventPvP = (EntityDamageByEntityEvent) event;
            if (eventPvP.getDamager() instanceof Player) {
                event.setCancelled(true);
                joueur.sendMessage("§cLe PvP est desactive pendant l'invincibilite !");
                return;
            }
        }
    }

    @EventHandler
    public void surMouvementJoueur(PlayerMoveEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieHungerGames(joueur)) return;

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.sontJoueursFreeze()) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
        }
    }

    @EventHandler
    public void surMortJoueur(PlayerDeathEvent event) {
        Player joueur = event.getEntity();

        if (!estDansPartieHungerGames(joueur)) return;

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        event.setDeathMessage(null);
        event.setDroppedExp(0);

        Player tueur = joueur.getKiller();
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            joueur.spigot().respawn();
            partie.eliminerJoueur(joueur, tueur);
        }, 1L);
    }

    @EventHandler
    public void surReapparitionJoueur(PlayerRespawnEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieHungerGames(joueur)) {
            String nomMonde = joueur.getWorld().getName();
            if (nomMonde.startsWith("HG_temp_")) {
                Location lobby = plugin.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
                if (lobby != null) {
                    event.setRespawnLocation(lobby);
                } else {
                    event.setRespawnLocation(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    joueur.sendMessage("§c» Vous avez été téléporté au lobby pour des raisons de sécurité.");
                }, 1L);
            }
            return;
        }

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null || partie.getEtat() == PartieHungerGames.EtatPartie.TERMINEE) {
            Location lobby = plugin.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
            if (lobby != null) {
                event.setRespawnLocation(lobby);
            } else {
                event.setRespawnLocation(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            }
            return;
        }

        event.setRespawnLocation(joueur.getLocation());
    }

    @EventHandler
    public void surCassageBloc(BlockBreakEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieHungerGames(joueur)) {
            if (!joueur.hasPermission("kingfight.admin") || joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) return;
            if (event.getBlock().getType() != Material.CHEST) return;

            String nomMonde = joueur.getWorld().getName();
            if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                    .estMapDisponible(TypeMiniJeu.HUNGER_GAMES, nomMonde)) return;

            Location locationCoffre = event.getBlock().getLocation();
            if (supprimerCoffreDuSetup(nomMonde, locationCoffre)) {
                joueur.sendMessage("§aCoffre supprimé du setup Hunger Games !");
            }
            return;
        }

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surPoseBloc(BlockPlaceEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieHungerGames(joueur)) return;

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surInteractionJoueur(PlayerInteractEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieHungerGames(joueur)) return;

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && 
            event.getClickedBlock() != null && 
            event.getClickedBlock().getType() == Material.CHEST) {
            
            event.setCancelled(true);
            partie.gererOuvertureCoffre(joueur, event.getClickedBlock().getLocation());
            return;
        }
        
        if (event.getItem() != null) {
            Material itemType = event.getItem().getType();
            if (itemType == Material.FLINT_AND_STEEL ||
                itemType == Material.POTION ||
                itemType == Material.ENDER_PEARL ||
                itemType == Material.GOLDEN_APPLE ||
                itemType == Material.BOW ||
                itemType.isEdible()) {
                return;
            }
        }
    }

    private boolean supprimerCoffreDuSetup(String nomMonde, Location locationCoffre) {
        File fichierSetup = new File(plugin.getDataFolder(), "hungergames/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        if (!config.contains("coffres")) return false;

        String coffreASupprimer = null;
        for (String key : config.getConfigurationSection("coffres").getKeys(false)) {
            String chemin = "coffres." + key;
            double x = config.getDouble(chemin + ".x");
            double y = config.getDouble(chemin + ".y");
            double z = config.getDouble(chemin + ".z");

            if (Math.abs(x - locationCoffre.getX()) < 0.1 &&
                Math.abs(y - locationCoffre.getY()) < 0.1 &&
                Math.abs(z - locationCoffre.getZ()) < 0.1) {
                coffreASupprimer = key;
                break;
            }
        }

        if (coffreASupprimer != null) {
            config.set("coffres." + coffreASupprimer, null);
            try {
                config.save(fichierSetup);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Erreur lors de la suppression du coffre : " + e.getMessage());
            }
        }

        return false;
    }

    @EventHandler
    public void surKickJoueur(PlayerKickEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieHungerGames(joueur)) return;

        PartieHungerGames partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.estJoueurProtegeFly(joueur) &&
            (event.getReason().toLowerCase().contains("fly") ||
             event.getReason().toLowerCase().contains("flying") ||
             event.getReason().toLowerCase().contains("moved too quickly"))) {
            event.setCancelled(true);
        }
    }

    private boolean estDansPartieHungerGames(Player joueur) {
        return plugin.getGestionnairePrincipal().getGestionnaireJoueurs().estEnPartie(joueur) &&
               plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getTypePartie(joueur) == TypeMiniJeu.HUNGER_GAMES;
    }

    private PartieHungerGames obtenirPartieJoueur(Player joueur) {
        if (!estDansPartieHungerGames(joueur)) return null;

        String idPartie = plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getIdPartie(joueur);
        return plugin.getGestionnairePrincipal().getGestionnaireHungerGames().getPartieParId(idPartie);
    }
}
