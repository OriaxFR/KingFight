package fr.oriax.kingfight.getdown;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class EvenementsGetDown implements Listener {

    private final KingFight plugin;

    public EvenementsGetDown(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void surDegatsJoueur(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player joueur = (Player) event.getEntity();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.getEtat() != PartieGetDown.EtatPartie.EN_COURS) {
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

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                partie.gererMortDansVide(joueur);
                return;
            }

            if (event.getFinalDamage() >= joueur.getHealth()) {
                event.setCancelled(true);
                partie.gererMortDansVide(joueur);
                return;
            }

            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent eventPvP = (EntityDamageByEntityEvent) event;
                if (eventPvP.getDamager() instanceof Player) {
                    return;
                }
            }
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surMouvementJoueur(PlayerMoveEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.sontJoueursFreeze()) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
            return;
        }

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP) {
            if (event.getTo().getY() < 0) {
                event.setCancelled(true);
                partie.gererMortDansVide(joueur);
                return;
            }

            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

                partie.verifierBlocSousJoueur(joueur);
            }

            if (joueur.isOnGround()) {
                partie.verifierBlocSousJoueur(joueur);
            }

            if (event.getFrom().distance(event.getTo()) > 0.1) {
                partie.verifierArriveeMap(joueur);
            }
        }
    }

    @EventHandler
    public void surCassageBloc(BlockBreakEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.getPhaseActuelle() != PartieGetDown.PhasePartie.JUMP) {
            event.setCancelled(true);
            return;
        }

        Material material = event.getBlock().getType();
        ConfigurationGetDown config = partie.getGestionnaire().getConfiguration();

        if (config.getBlocSpecial(material) != null) {
            partie.gererBlocSpecial(joueur, event.getBlock().getLocation());
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surPoseBloc(BlockPlaceEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surMortJoueur(PlayerDeathEvent event) {
        Player joueur = event.getEntity();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        event.setDeathMessage(null);
        event.setDroppedExp(0);
        event.getDrops().clear();

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.PVP) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            Player tueur = joueur.getKiller();
            Location positionMort = joueur.getLocation().clone();

            ItemStack[] inventaireSauvegarde = joueur.getInventory().getContents().clone();
            ItemStack[] armureSauvegarde = joueur.getInventory().getArmorContents().clone();
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                joueur.spigot().respawn();
                partie.eliminerJoueur(joueur, tueur, positionMort, inventaireSauvegarde, armureSauvegarde);
            }, 1L);
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP) {
            event.setDeathMessage(null);
            event.setDroppedExp(0);
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                joueur.spigot().respawn();
                partie.gererMortDansVide(joueur);
            });
        }
    }

    @EventHandler
    public void surReapparitionJoueur(PlayerRespawnEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) {
            String nomMonde = joueur.getWorld().getName();
            if (nomMonde.startsWith("GD_temp_")) {
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

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null || partie.getEtat() == PartieGetDown.EtatPartie.TERMINEE) {
            Location lobby = plugin.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
            if (lobby != null) {
                event.setRespawnLocation(lobby);
            } else {
                event.setRespawnLocation(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            }
            return;
        }

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP) {
            event.setRespawnLocation(partie.getMonde().getSpawnLocation());
        } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.PVP) {
            int viesRestantes = partie.getViesJoueur(joueur.getUniqueId());
            if (viesRestantes > 0) {
                event.setRespawnLocation(partie.obtenirSpawnPvpAleatoire());
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    partie.respawnerJoueurPvpImmediat(joueur);
                }, 1L);
            } else {
                event.setRespawnLocation(partie.getMonde().getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void surClicInventaire(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player joueur = (Player) event.getWhoClicked();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF) {
            if (event.getInventory().getName().contains("Shop")) {
                event.setCancelled(true);

                if (event.getClickedInventory() != null && 
                    !event.getClickedInventory().equals(joueur.getInventory()) &&
                    event.isLeftClick() && event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    partie.gererAchatShop(joueur, event.getCurrentItem());
                } else if (event.getClickedInventory() != null && 
                           event.getClickedInventory().equals(joueur.getInventory()) &&
                           event.isRightClick() && event.getCurrentItem() != null) {
                    partie.gererReventeItem(joueur, event.getCurrentItem(), event.getSlot());
                }
            }
        }
    }

    @EventHandler
    public void surFermetureInventaire(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player joueur = (Player) event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (event.getInventory().getName().startsWith("§5Coffre du joueur éliminé")) {
            partie.gererFermetureEnderChest(event.getInventory(), joueur);
            return;
        }

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF) {
            if (event.getInventory().getName().contains("Shop")) {
                if (!joueur.hasMetadata("achat_en_cours") && !joueur.hasMetadata("vente_en_cours")) {
                    PartieGetDown.JoueurPartieGetDown joueurPartie = partie.getJoueur(joueur.getUniqueId());
                    if (joueurPartie != null && partie.peutEncoreAcheter(joueur)) {
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF && joueur.isOnline() && joueur.isOnGround()) {
                                partie.ouvrirShop(joueur);
                                joueur.sendMessage("§c» Vous ne pouvez pas quitter le menu d'achat pendant la phase d'achat !");
                            } else if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF && joueur.isOnline() && !joueur.isOnGround()) {
                                partie.attendreJoueurAuSol(joueur);
                            }
                        }, 1L);
                    }
                }
                joueur.removeMetadata("achat_en_cours", plugin);
                joueur.removeMetadata("vente_en_cours", plugin);
            }
        }
    }

    @EventHandler
    public void surChangementFaim(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player joueur = (Player) event.getEntity();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.getPhaseActuelle() == PartieGetDown.PhasePartie.JUMP || 
            partie.getPhaseActuelle() == PartieGetDown.PhasePartie.STUFF) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surChatJoueur(AsyncPlayerChatEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        if (joueur.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            joueur.sendMessage("§cVous ne pouvez pas parler en mode spectateur !");
        }
    }

    @EventHandler
    public void surInteractionJoueur(PlayerInteractEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && 
            event.getClickedBlock() != null && 
            event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            
            event.setCancelled(true);
            partie.ouvrirEnderchest(joueur, event.getClickedBlock().getLocation());
        }
    }

    @EventHandler
    public void surKickJoueur(PlayerKickEvent event) {
        Player joueur = event.getPlayer();

        if (!estDansPartieGetDown(joueur)) return;

        PartieGetDown partie = obtenirPartieJoueur(joueur);
        if (partie == null) return;

        if (partie.estJoueurProtegeFly(joueur) &&
            (event.getReason().toLowerCase().contains("fly") ||
             event.getReason().toLowerCase().contains("flying") ||
             event.getReason().toLowerCase().contains("moved too quickly"))) {
            event.setCancelled(true);
        }
    }

    private boolean estDansPartieGetDown(Player joueur) {
        return plugin.getGestionnairePrincipal().getGestionnaireJoueurs().estEnPartie(joueur) &&
               plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getTypePartie(joueur) == TypeMiniJeu.GET_DOWN;
    }

    private PartieGetDown obtenirPartieJoueur(Player joueur) {
        if (!estDansPartieGetDown(joueur)) return null;

        String idPartie = plugin.getGestionnairePrincipal().getGestionnaireJoueurs().getIdPartie(joueur);
        return plugin.getGestionnairePrincipal().getGestionnaireGetDown().getPartieParId(idPartie);
    }
}
