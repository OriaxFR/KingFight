package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class EvenementsLobby implements Listener {

    private final KingFight plugin;

    public EvenementsLobby(KingFight plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void surDegatsJoueur(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player joueur = (Player) event.getEntity();

        if (estDansLobby(joueur)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victime = (Player) event.getEntity();
        Player attaquant = (Player) event.getDamager();

        if (estDansLobby(victime) || estDansLobby(attaquant)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surCassageBloc(BlockBreakEvent event) {
        Player joueur = event.getPlayer();

        if (estDansLobby(joueur) && !joueur.isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surPoseBloc(BlockPlaceEvent event) {
        Player joueur = event.getPlayer();

        if (estDansLobby(joueur) && !joueur.isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surJetObjet(PlayerDropItemEvent event) {
        Player joueur = event.getPlayer();

        if (estDansLobby(joueur)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surRamassageObjet(PlayerPickupItemEvent event) {
        Player joueur = event.getPlayer();

        if (estDansLobby(joueur)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surChangementFaim(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player joueur = (Player) event.getEntity();

        if (estDansLobby(joueur)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void surExplosion(EntityExplodeEvent event) {
        String mondeLobby = plugin.getGestionnairePrincipal().getConfigPrincipale().getString("general.lobby-world", "world");

        if (event.getLocation().getWorld().getName().equals(mondeLobby)) {
            event.setCancelled(true);
        }
    }

    private boolean estDansLobby(Player joueur) {
        if (plugin.getGestionnairePrincipal().getGestionnaireJoueurs().estEnPartie(joueur)) {
            return false;
        }

        String mondeLobby = plugin.getGestionnairePrincipal().getConfigPrincipale().getString("general.lobby-world", "world");
        return joueur.getWorld().getName().equals(mondeLobby);
    }
}
