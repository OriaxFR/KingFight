package fr.oriax.kingfight.jumpleague;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import fr.oriax.kingfight.jumpleague.PartieJumpLeague.JoueurPartieJumpLeague;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import java.io.File;
import java.io.IOException;

public class EvenementsJumpLeague implements Listener {

    private final KingFight plugin;

    public EvenementsJumpLeague(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void surDegatsJoueur(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player joueur = (Player) event.getEntity();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.getEtat() != PartieJumpLeague.EtatPartie.EN_COURS) {
            event.setCancelled(true);
            return;
        }

        if (partie.estInvincibiliteActive() && event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent eventPvP = (EntityDamageByEntityEvent) event;
            if (eventPvP.getDamager() instanceof Player) {
                event.setCancelled(true);
                joueur.sendMessage("§cLe PvP est désactivé pendant l'invincibilité !");
                return;
            }
        }

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent eventPvP = (EntityDamageByEntityEvent) event;
            if (eventPvP.getDamager() instanceof Player) {
                Player attaquant = (Player) eventPvP.getDamager();
                
                if (partie.estJoueurProtegeSpawn(joueur)) {
                    event.setCancelled(true);
                    attaquant.sendMessage("§cCe joueur est protégé contre le spawn kill !");
                    return;
                }
                
                if (partie.estJoueurProtegeSpawn(attaquant)) {
                    event.setCancelled(true);
                    attaquant.sendMessage("§cVous ne pouvez pas attaquer pendant votre protection spawn !");
                    return;
                }
            }
        }

        if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.JUMP) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                partie.gererMortDansVide(joueur);
                return;
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
                return;
            }

            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent eventPvP = (EntityDamageByEntityEvent) event;
                if (eventPvP.getDamager() instanceof Player) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void surMouvementJoueur(PlayerMoveEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.estJoueurFreeze(joueur)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
            return;
        }

        if (partie.getEtat() != PartieJumpLeague.EtatPartie.EN_COURS) return;

        if (partie.getPhaseActuelle() != PartieJumpLeague.PhasePartie.JUMP) return;

        if (event.getTo().getY() < 0) {
            event.setCancelled(true);
            partie.gererMortDansVide(joueur);
            return;
        }

        partie.verifierCheckpoint(joueur, event.getTo());
    }

    @EventHandler
    public void surMortJoueur(PlayerDeathEvent event) {
        Player joueur = event.getEntity();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        event.setDeathMessage(null);
        event.setDroppedExp(0);
        event.getDrops().clear();

        if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.PVP) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            Player tueur = joueur.getKiller();

            Location positionMort = joueur.getLocation().clone();
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                joueur.spigot().respawn();
                partie.eliminerJoueur(joueur, tueur, positionMort);
            }, 1L);
        }
    }

    @EventHandler
    public void surReapparitionJoueur(PlayerRespawnEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) {
            String nomMonde = joueur.getWorld().getName();
            if (nomMonde.startsWith("JL_temp_")) {
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

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null || partie.getEtat() == PartieJumpLeague.EtatPartie.TERMINEE) {
            Location lobby = plugin.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
            if (lobby != null) {
                event.setRespawnLocation(lobby);
            } else {
                event.setRespawnLocation(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            }
            return;
        }

        if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.JUMP) {
            JoueurPartieJumpLeague joueurPartie = partie.getJoueurPartie(joueur.getUniqueId());
            if (joueurPartie != null && joueurPartie.getDernierCheckpoint() != null) {
                event.setRespawnLocation(joueurPartie.getDernierCheckpoint());
            }
        } else if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.PVP) {
            int viesRestantes = partie.getViesJoueur(joueur.getUniqueId());
            if (viesRestantes > 0) {
                event.setRespawnLocation(partie.obtenirSpawnPvpAleatoire());
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    int viesApresElimination = partie.getViesJoueur(joueur.getUniqueId());
                    if (viesApresElimination > 0) {
                        partie.respawnerJoueurPvpImmediat(joueur);
                    }
                }, 3L);
            } else {
                event.setRespawnLocation(partie.getMonde().getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void surCassageBloc(BlockBreakEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) {
            if (!joueur.hasPermission("kingfight.admin") || joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) return;
            if (event.getBlock().getType() != Material.CHEST) return;

            String nomMonde = joueur.getWorld().getName();
            if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                    .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, nomMonde)) return;

            Location locationCoffre = event.getBlock().getLocation();
            if (supprimerCoffreDuSetup(nomMonde, locationCoffre)) {
                joueur.sendMessage("§aCoffre supprimé du setup Jump League !");
            }
            return;
        }

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surPoseBloc(BlockPlaceEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surInteractionJoueur(PlayerInteractEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.estJoueurFreeze(joueur)) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
            event.getClickedBlock() != null &&
            event.getClickedBlock().getType() == Material.CHEST) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
            event.getClickedBlock() != null &&
            event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);
            partie.gererInteractionEnderChest(joueur, event.getClickedBlock().getLocation());
            return;
        }
        
        if (partie.getPhaseActuelle() == PartieJumpLeague.PhasePartie.PVP) {
            if (event.getItem() != null && event.getItem().getType() == Material.BOW) {
                return;
            }
            if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                event.getItem() != null && 
                (event.getItem().getType() == Material.POTION || 
                 event.getItem().getType().name().contains("SWORD") ||
                 event.getItem().getType().name().contains("AXE") ||
                 event.getItem().getType() == Material.FISHING_ROD ||
                 event.getItem().getType() == Material.SNOW_BALL ||
                 event.getItem().getType() == Material.EGG ||
                 event.getItem().getType() == Material.ENDER_PEARL)) {
                return;
            }
        }
        
        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surKickJoueur(PlayerKickEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.estJoueurProtegeFly(joueur) &&
            (event.getReason().toLowerCase().contains("fly") ||
             event.getReason().toLowerCase().contains("flying") ||
             event.getReason().toLowerCase().contains("moved too quickly") ||
             event.getReason().toLowerCase().contains("moved wrongly") ||
             event.getReason().toLowerCase().contains("illegal") ||
             event.getReason().toLowerCase().contains("cheat"))) {
            event.setCancelled(true);
            plugin.getLogger().info("Protection fly activée pour " + joueur.getName() + " - Raison du kick: " + event.getReason());
        }
    }

    @EventHandler
    public void surChatJoueur(AsyncPlayerChatEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (joueur.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            joueur.sendMessage("§cVous ne pouvez pas parler en mode spectateur !");
        }
    }

    @EventHandler
    public void surFermetureInventaire(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player joueur = (Player) event.getPlayer();
        
        if (!estDansPartieJumpLeague(joueur)) return;

        PartieJumpLeague partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (event.getInventory().getName().equals("§5Coffre du joueur éliminé")) {
            partie.gererFermetureEnderChest(event.getInventory(), joueur.getUniqueId());
        }
    }

    private boolean estDansPartieJumpLeague(Player joueur) {
        return plugin.getGestionnairePrincipal().getGestionnaireJoueurs().estEnPartie(joueur) &&
               plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getTypePartie(joueur) == TypeMiniJeu.JUMP_LEAGUE;
    }

    private PartieJumpLeague obtenirPartieJoueur(Player joueur) {
        if (!estDansPartieJumpLeague(joueur)) return null;

        String idPartie = plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getIdPartie(joueur);
        return plugin.getGestionnairePrincipal().getGestionnaireJumpLeague().getPartieParId(idPartie);
    }

    private boolean supprimerCoffreDuSetup(String nomMonde, Location locationCoffre) {
        File fichierSetup = new File(plugin.getDataFolder(), "jumpleague/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        if (!config.contains("parcours")) return false;

        boolean coffreTrouve = false;
        for (String numeroParcours : config.getConfigurationSection("parcours").getKeys(false)) {
            String cheminCoffres = "parcours." + numeroParcours + ".coffres";
            if (config.contains(cheminCoffres)) {
                java.util.List<String> coffres = config.getStringList(cheminCoffres);
                java.util.List<String> nouveauxCoffres = new java.util.ArrayList<>();
                
                for (String coffreStr : coffres) {
                    String[] parties = coffreStr.split(",");
                    if (parties.length >= 3) {
                        try {
                            double x = Double.parseDouble(parties[0]);
                            double y = Double.parseDouble(parties[1]);
                            double z = Double.parseDouble(parties[2]);
                            
                            if (Math.abs(x - locationCoffre.getX()) < 0.1 &&
                                Math.abs(y - locationCoffre.getY()) < 0.1 &&
                                Math.abs(z - locationCoffre.getZ()) < 0.1) {
                                coffreTrouve = true;
                            } else {
                                nouveauxCoffres.add(coffreStr);
                            }
                        } catch (NumberFormatException e) {
                            nouveauxCoffres.add(coffreStr);
                        }
                    } else {
                        nouveauxCoffres.add(coffreStr);
                    }
                }
                
                if (coffreTrouve) {
                    config.set(cheminCoffres, nouveauxCoffres);
                    break;
                }
            }
        }

        if (coffreTrouve) {
            try {
                config.save(fichierSetup);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Erreur lors de la suppression du coffre : " + e.getMessage());
            }
        }

        return false;
    }
}
