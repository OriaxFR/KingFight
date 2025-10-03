package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GestionnaireLobby {

    private final KingFight plugin;
    private Location spawnLobby;
    private final Map<UUID, Long> dernierMessageLobby;

    public GestionnaireLobby(KingFight plugin) {
        this.plugin = plugin;
        this.dernierMessageLobby = new HashMap<>();
    }

    private void chargerSpawnLobby() {
        try {
            if (plugin.getGestionnairePrincipal() != null &&
                plugin.getGestionnairePrincipal().getGestionnaireMaps() != null) {
                spawnLobby = plugin.getGestionnairePrincipal().getGestionnaireMaps().obtenirSpawnLobby();
            } else {
                plugin.getLogger().warning("GestionnaireMaps non disponible lors du chargement du spawn lobby");
                spawnLobby = null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du chargement du spawn lobby: " + e.getMessage());
            spawnLobby = null;
        }
    }

    public void teleporterVersLobby(Player joueur) {
        if (spawnLobby == null) {
            chargerSpawnLobby();
        }

        if (spawnLobby != null) {
            joueur.teleport(spawnLobby);
            preparerJoueurLobby(joueur);
        } else {
            joueur.sendMessage("§cErreur: Le spawn du lobby n'est pas defini ! Contactez un administrateur.");
            plugin.getLogger().warning("Tentative de teleportation vers un lobby non defini pour le joueur " + joueur.getName());
        }
    }

    private void preparerJoueurLobby(Player joueur) {
        joueur.setGameMode(org.bukkit.GameMode.SURVIVAL);
        joueur.setHealth(20.0);
        joueur.setFoodLevel(20);
        joueur.setSaturation(20.0f);
        joueur.setLevel(0);
        joueur.setExp(0.0f);

        joueur.getInventory().clear();
        joueur.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);

        joueur.setAllowFlight(false);
        joueur.setFlying(false);

        for (org.bukkit.potion.PotionEffect effet : joueur.getActivePotionEffects()) {
            joueur.removePotionEffect(effet.getType());
        }

        joueur.setDisplayName(joueur.getName());
        joueur.setPlayerListName(joueur.getName());

        for (Player autreJoueur : plugin.getServer().getOnlinePlayers()) {
            autreJoueur.showPlayer(joueur);
            joueur.showPlayer(autreJoueur);
        }

        long maintenant = System.currentTimeMillis();
        Long dernierMessage = dernierMessageLobby.get(joueur.getUniqueId());

        if (dernierMessage == null || maintenant - dernierMessage > 5000) {
            joueur.sendMessage("§aBienvenue dans le lobby ! Utilisez §e/tournoi §apour jouer !");
            dernierMessageLobby.put(joueur.getUniqueId(), maintenant);
        }
    }

    public boolean estSpawnLobbyDefini() {
        if (spawnLobby == null) {
            chargerSpawnLobby();
        }
        return spawnLobby != null;
    }

    public Location getSpawnLobby() {
        if (spawnLobby == null) {
            chargerSpawnLobby();
        }
        return spawnLobby;
    }

    public void definirSpawnLobby(Location location) {
        this.spawnLobby = location;
        plugin.getGestionnairePrincipal().getGestionnaireMaps().definirSpawnLobby(location);
        rechargerSpawnLobby();
    }

    public void rechargerSpawnLobby() {
        chargerSpawnLobby();
    }
}
