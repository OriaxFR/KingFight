package fr.oriax.kingfight.hungergames;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.JoueurPartie;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartieHungerGames {

    private final KingFight plugin;
    private final GestionnaireHungerGames gestionnaire;
    private final String idPartie;
    private final ConfigurationHungerGames config;

    private final Map<UUID, JoueurPartie> joueurs;
    private final List<Player> joueursVivants;
    private final List<Player> spectateurs;
    private final List<Location> colisStrategiques;
    private final List<BukkitTask> tachesColisEffets;
    private final Map<UUID, Long> dernierMessageBordure;
    private final Set<UUID> joueursProtectionFly;

    private World monde;
    private String nomMapOriginale;
    private Location centreMap;
    private int rayonBordureInitial;
    private int rayonBordureActuel;

    private EtatPartie etat;
    private long tempsDebut;
    private BukkitTask tachePartie;
    private BukkitTask tacheBordure;
    private BukkitTask tacheColisStrategique;

    private boolean invincibiliteActive;
    private boolean joueursFreeze;
    private int tempsEcoule;
    private ScoreboardHungerGames scoreboardManager;
    private boolean partieTerminee;

    public enum EtatPartie {
        ATTENTE,
        PREPARATION,
        EN_COURS,
        TERMINEE
    }

    public PartieHungerGames(KingFight plugin, GestionnaireHungerGames gestionnaire, String idPartie,
                           List<Player> listeJoueurs, ConfigurationHungerGames config) {
        this.plugin = plugin;
        this.gestionnaire = gestionnaire;
        this.idPartie = idPartie;
        this.config = config;

        this.joueurs = new ConcurrentHashMap<>();
        this.joueursVivants = new ArrayList<>();
        this.spectateurs = new ArrayList<>();
        this.colisStrategiques = new ArrayList<>();
        this.tachesColisEffets = new ArrayList<>();
        this.dernierMessageBordure = new HashMap<>();
        this.joueursProtectionFly = new HashSet<>();

        this.etat = EtatPartie.ATTENTE;
        this.invincibiliteActive = false;
        this.joueursFreeze = false;
        this.tempsEcoule = 0;
        this.partieTerminee = false;

        initialiserJoueurs(listeJoueurs);
        choisirMonde();
        this.scoreboardManager = new ScoreboardHungerGames(this);
    }

    private void initialiserJoueurs(List<Player> listeJoueurs) {
        List<String> nomsAnonymises = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireJoueurs().getNomsAnonymises(listeJoueurs);

        for (int i = 0; i < listeJoueurs.size(); i++) {
            Player joueur = listeJoueurs.get(i);
            JoueurPartie joueurPartie = new JoueurPartie(joueur, nomsAnonymises.get(i));

            joueurs.put(joueur.getUniqueId(), joueurPartie);
            joueursVivants.add(joueur);
        }
    }

    private void choisirMonde() {
        nomMapOriginale = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireMaps().obtenirMapAleatoire(TypeMiniJeu.HUNGER_GAMES);

        if (nomMapOriginale == null) {
            plugin.getLogger().severe("Aucune map configuree pour Hunger Games !");
            return;
        }

        monde = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireMaps().creerMapTemporaire(nomMapOriginale, idPartie);

        if (monde == null) {
            plugin.getLogger().severe("Impossible de creer la map temporaire pour Hunger Games !");
            return;
        }

        if (!monde.getName().equals(nomMapOriginale)) {
            gestionnaire.getGestionnaireSetup().copierSetupVersMondeTemporaire(nomMapOriginale, monde.getName());
        }

        centreMap = gestionnaire.getGestionnaireSetup().getCentreBordure(monde);
        rayonBordureInitial = gestionnaire.getGestionnaireSetup().getRayonBordure(monde);
        rayonBordureActuel = rayonBordureInitial;

        configurerMonde();
    }

    private void configurerMonde() {
        monde.setTime(6000);
        monde.setStorm(false);
        monde.setThundering(false);
        monde.setWeatherDuration(0);

        monde.setGameRuleValue("doDaylightCycle", "false");

        monde.setDifficulty(Difficulty.PEACEFUL);

        monde.setGameRuleValue("doMobSpawning", "false");

        monde.setGameRuleValue("keepInventory", "false");
        monde.setGameRuleValue("doFireTick", "false");
        monde.setGameRuleValue("mobGriefing", "false");

        WorldBorder bordure = monde.getWorldBorder();
        bordure.setCenter(centreMap);
        bordure.setSize(rayonBordureInitial * 2);
        
        plugin.getLogger().info("Monde " + monde.getName() + " configuré en mode Peaceful avec jour permanent");
    }

    public void demarrer() {
        etat = EtatPartie.PREPARATION;
        tempsDebut = System.currentTimeMillis();

        teleporterJoueurs();
        preparerJoueurs();
        remplirCoffres();

        demarrerColisStrategique();

        diffuserMessage("");
        diffuserMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        diffuserMessage("§6§l                  » HUNGER GAMES «");
        diffuserMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        diffuserMessage("");
        diffuserMessage("§e» La partie commence dans 10 secondes !");
        diffuserMessage("§c» Invincibilité: " + config.getTempsInvincibilite() + " secondes");
        diffuserMessage("");

        joueursFreeze = true;
        
        for (Player joueur : joueursVivants) {
            if (joueur.isFlying() || joueur.getAllowFlight()) {
                joueursProtectionFly.add(joueur.getUniqueId());
                joueur.setAllowFlight(true);
                joueur.setFlying(false);
            }
        }

        new BukkitRunnable() {
            int compte = 10;

            @Override
            public void run() {
                if (compte > 0) {
                    diffuserMessage("§e§l» DÉBUT dans §f§l" + compte + "§e§l seconde" + (compte > 1 ? "s" : "") + " !");
                    jouerSon(Sound.NOTE_PLING, 1.0f, 1.0f);
                    compte--;
                } else {
                    etat = EtatPartie.EN_COURS;
                    joueursFreeze = false;
                    invincibiliteActive = true;
                    
                    for (Player joueur : joueursVivants) {
                        if (joueursProtectionFly.contains(joueur.getUniqueId())) {
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                joueursProtectionFly.remove(joueur.getUniqueId());
                                if (joueur.isOnline() && joueur.getGameMode() == GameMode.SURVIVAL) {
                                    joueur.setAllowFlight(false);
                                    joueur.setFlying(false);
                                }
                            }, 40L);
                        }
                    }

                    demarrerCompteurPartie();
                    diffuserMessage("");
                    diffuserMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§a§l                    » C'EST PARTI ! «");
                    diffuserMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("");
                    diffuserMessage("§a» Que les Hunger Games commencent !");
                    diffuserMessage("§c» Invincibilité: " + config.getTempsInvincibilite() + " secondes");
                    diffuserMessage("");
                    jouerSon(Sound.NOTE_PLING, 1.0f, 2.0f);
                    demarrerInvincibilite();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void teleporterJoueurs() {
        List<Location> spawns = gestionnaire.getGestionnaireSetup().getSpawnsJoueurs(monde);

        if (spawns.isEmpty()) {
            plugin.getLogger().warning("Aucun spawn configure pour le monde " + monde.getName());
            return;
        }

        for (int i = 0; i < joueursVivants.size(); i++) {
            Player joueur = joueursVivants.get(i);
            Location spawn = spawns.get(i % spawns.size());
            joueur.teleport(spawn);
        }
    }

    private void preparerJoueurs() {
        for (Player joueur : joueursVivants) {
            joueur.setGameMode(GameMode.SURVIVAL);
            joueur.setHealth(20.0);
            joueur.setFoodLevel(20);
            joueur.setSaturation(20.0f);
            joueur.setLevel(0);
            joueur.setExp(0.0f);

            joueur.getInventory().clear();
            joueur.getInventory().setArmorContents(new ItemStack[4]);

            for (PotionEffect effet : joueur.getActivePotionEffects()) {
                joueur.removePotionEffect(effet.getType());
            }

            JoueurPartie joueurPartie = joueurs.get(joueur.getUniqueId());
            joueur.setDisplayName(ChatColor.WHITE + joueurPartie.getNomAnonymise());
            joueur.setPlayerListName(ChatColor.WHITE + joueurPartie.getNomAnonymise());

            scoreboardManager.creerScoreboard(joueur);
        }
    }

    private void remplirCoffres() {
        new BukkitRunnable() {
            @Override
            public void run() {
                GestionnaireCoffresHungerGames.remplirCoffres(monde, config, gestionnaire.getGestionnaireSetup());
            }
        }.runTask(plugin);
    }

    private void demarrerCompteurPartie() {
        tachePartie = new BukkitRunnable() {
            @Override
            public void run() {
                if (partieTerminee || etat == EtatPartie.TERMINEE) {
                    cancel();
                    return;
                }
                
                tempsEcoule++;

                int tempsAvantBordureSecondes = config.getTempsAvantBordure() * 60;
                int tempsRestantAvantBordure = tempsAvantBordureSecondes - tempsEcoule;

                if (tempsRestantAvantBordure == 120) {
                    diffuserMessage("");
                    diffuserMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§e§l              » AVERTISSEMENT BORDURE «");
                    diffuserMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§c» La bordure commencera à se rétrécir dans §e2 minutes §c!");
                    diffuserMessage("§7» Préparez-vous à vous rapprocher du centre de la map");
                    diffuserMessage("§7» Rayon actuel: §e" + rayonBordureActuel + " blocs");
                    diffuserMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("");
                    jouerSon(Sound.NOTE_PLING, 1.0f, 1.0f);
                    
                } else if (tempsRestantAvantBordure == 60) {
                    diffuserMessage("");
                    diffuserMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§6§l              » AVERTISSEMENT BORDURE «");
                    diffuserMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§c» La bordure commencera à se rétrécir dans §61 minute §c!");
                    diffuserMessage("§7» Commencez à vous diriger vers le centre");
                    diffuserMessage("§7» Rayon actuel: §6" + rayonBordureActuel + " blocs");
                    diffuserMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("");
                    jouerSon(Sound.NOTE_PLING, 1.0f, 1.2f);
                    
                } else if (tempsRestantAvantBordure == 30) {
                    diffuserMessage("");
                    diffuserMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§c§l              » ALERTE BORDURE «");
                    diffuserMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§4» La bordure commencera à se rétrécir dans §c30 secondes §4!");
                    diffuserMessage("§c» Rapprochez-vous du centre MAINTENANT !");
                    diffuserMessage("§7» Rayon actuel: §c" + rayonBordureActuel + " blocs");
                    diffuserMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("");
                    jouerSon(Sound.NOTE_PLING, 1.0f, 1.5f);
                    
                } else if (tempsRestantAvantBordure == 10) {
                    diffuserMessage("");
                    diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§4§l              » ALERTE CRITIQUE «");
                    diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§4§l» La bordure commence à se rétrécir dans 10 secondes !");
                    diffuserMessage("§c§l» DERNIÈRE CHANCE de vous mettre en sécurité !");
                    diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("");
                    jouerSon(Sound.ENDERDRAGON_GROWL, 1.0f, 1.5f);
                    
                } else if (tempsRestantAvantBordure <= 5 && tempsRestantAvantBordure > 0) {
                    diffuserMessage("§4§l» " + tempsRestantAvantBordure + " §4§l...");
                    jouerSon(Sound.NOTE_STICKS, 1.0f, 2.0f);
                }

                if (tempsEcoule == config.getTempsAvantBordure() * 60) {
                    demarrerReductionBordure();
                }

                if (tempsEcoule >= config.getDureePartieMax() * 60) {
                    terminerPartieTempsEcoule();
                }

                mettreAJourScoreboard();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void demarrerInvincibilite() {
        new BukkitRunnable() {
            @Override
            public void run() {
                invincibiliteActive = false;
                diffuserMessage("");
                diffuserMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                diffuserMessage("§c§l                    » PVP ACTIVÉ ! «");
                diffuserMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                diffuserMessage("");
                diffuserMessage("§c» L'invincibilité est terminée ! Le PvP est activé !");
                diffuserMessage("");
                jouerSon(Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f);
            }
        }.runTaskLater(plugin, config.getTempsInvincibilite() * 20L);
    }

    private void demarrerColisStrategique() {
        tacheColisStrategique = new BukkitRunnable() {
            @Override
            public void run() {
                if (partieTerminee || etat == EtatPartie.TERMINEE) {
                    cancel();
                    return;
                }
                
                if (etat == EtatPartie.EN_COURS && !joueursVivants.isEmpty()) {
                    genererColisStrategique();
                }
            }
        }.runTaskTimer(plugin, config.getIntervalleColisStrategique() * 60 * 20L,
                       config.getIntervalleColisStrategique() * 60 * 20L);
    }

    private void genererColisStrategique() {
        Random random = new Random();
        int rayonSpawn = Math.min(config.getRayonSpawnColis(), rayonBordureActuel);

        double x = centreMap.getX() + (random.nextDouble() - 0.5) * rayonSpawn * 2;
        double z = centreMap.getZ() + (random.nextDouble() - 0.5) * rayonSpawn * 2;

        int y = monde.getHighestBlockYAt((int) x, (int) z);

        Location locationTest = new Location(monde, x, y, z);
        while (y > 0 && (locationTest.getBlock().getType() == Material.AIR ||
                         locationTest.getBlock().getType() == Material.WATER ||
                         locationTest.getBlock().getType() == Material.LAVA)) {
            y--;
            locationTest.setY(y);
        }

        Location locationColis = new Location(monde, x, y + 1, z);
        
        if (locationColis.getBlock().getType() == Material.WATER) {
            int surfaceY = y + 1;
            Location surfaceTest = new Location(monde, x, surfaceY, z);
            while (surfaceY < 256 && surfaceTest.getBlock().getType() == Material.WATER) {
                surfaceY++;
                surfaceTest.setY(surfaceY);
            }
            locationColis.setY(surfaceY);
        }

        diffuserMessage(ChatColor.GOLD + "§l» COLIS STRATEGIQUE largué !");
        diffuserMessage(ChatColor.YELLOW + "» Position: " + ChatColor.WHITE + (int) x + ", " + (int) z);
        jouerSon(Sound.ENDERDRAGON_WINGS, 1.0f, 1.5f);
        creerColisAvecEffets(locationColis);
    }

    private void creerColisAvecEffets(Location location) {
        GestionnaireCoffresHungerGames.creerColisStrategique(location, config);
        colisStrategiques.add(location);
        BukkitTask tacheEffets = new BukkitRunnable() {
            int compteur = 0;

            @Override
            public void run() {
                if (partieTerminee || etat == EtatPartie.TERMINEE) {
                    cancel();
                    tachesColisEffets.remove(this);
                    return;
                }
                
                if (location.getBlock().getType() != Material.CHEST) {
                    cancel();
                    tachesColisEffets.remove(this);
                    return;
                }

                if (location.getBlock().getState() instanceof org.bukkit.block.Chest) {
                    org.bukkit.block.Chest coffre = (org.bukkit.block.Chest) location.getBlock().getState();
                    boolean coffreeVide = true;
                    for (org.bukkit.inventory.ItemStack item : coffre.getInventory().getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            coffreeVide = false;
                            break;
                        }
                    }
                    if (coffreeVide) {
                        cancel();
                        tachesColisEffets.remove(this);
                        return;
                    }
                }

                for (int i = 0; i < 10; i++) {
                    double offsetX = (Math.random() - 0.5) * 2;
                    double offsetY = Math.random() * 2;
                    double offsetZ = (Math.random() - 0.5) * 2;

                    Location particleLocation = location.clone().add(offsetX, offsetY, offsetZ);
                    monde.playEffect(particleLocation, org.bukkit.Effect.LAVA_POP, null);
                }

                if (compteur % 20 == 0) {
                    for (Player joueur : joueursVivants) {
                        if (joueur.getWorld().equals(location.getWorld()) && 
                            joueur.getLocation().distance(location) <= 50) {
                            joueur.playSound(location, Sound.NOTE_PLING, 1.0f, 2.0f);
                        }
                    }
                }

                compteur++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        tachesColisEffets.add(tacheEffets);
    }

    private void demarrerReductionBordure() {
        diffuserMessage(ChatColor.RED + "La bordure commence a se reduire !");
        jouerSon(Sound.ENDERDRAGON_GROWL, 1.0f, 0.5f);

        tacheBordure = new BukkitRunnable() {
            int compteurBordure = 0;
            
            @Override
            public void run() {
                if (partieTerminee || etat == EtatPartie.TERMINEE) {
                    cancel();
                    return;
                }
                
                if (rayonBordureActuel > config.getRayonFinalBordure()) {
                    rayonBordureActuel -= config.getVitesseBordure();

                    WorldBorder bordure = monde.getWorldBorder();
                    bordure.setSize(rayonBordureActuel * 2, 1);

                    verifierJoueursDansBordure();
                    
                    compteurBordure++;
                    
                    if (compteurBordure % 30 == 0) {
                        diffuserMessage("");
                        diffuserMessage("§c§l» BORDURE EN COURS DE RÉTRÉCISSEMENT «");
                        diffuserMessage("§7» Rayon actuel: §c" + rayonBordureActuel + " blocs");
                        diffuserMessage("§7» Vitesse: §e" + config.getVitesseBordure() + " blocs/seconde");
                        diffuserMessage("");
                        jouerSon(Sound.NOTE_BASS, 1.0f, 0.8f);
                    }
                    
                    if (rayonBordureActuel == 50) {
                        diffuserMessage("");
                        diffuserMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("§6§l            » BORDURE À 50 BLOCS «");
                        diffuserMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("§e» La zone de jeu devient plus petite !");
                        diffuserMessage("§7» Les combats vont s'intensifier...");
                        diffuserMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("");
                        jouerSon(Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f);
                        
                    } else if (rayonBordureActuel == 25) {
                        diffuserMessage("");
                        diffuserMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("§c§l            » BORDURE À 25 BLOCS «");
                        diffuserMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("§c» Zone de combat très réduite !");
                        diffuserMessage("§7» Les affrontements sont inévitables...");
                        diffuserMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("");
                        jouerSon(Sound.ENDERDRAGON_GROWL, 1.0f, 1.2f);
                        
                    } else if (rayonBordureActuel == 15) {
                        diffuserMessage("");
                        diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("§4§l            » BORDURE CRITIQUE «");
                        diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("§4§l» Seulement 15 blocs de rayon restants !");
                        diffuserMessage("§c§l» Combat final imminent !");
                        diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        diffuserMessage("");
                        jouerSon(Sound.ENDERDRAGON_DEATH, 0.5f, 1.5f);
                    }
                    
                } else {
                    diffuserMessage("");
                    diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§4§l          » BORDURE FINALE ATTEINTE «");
                    diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("§c§l» Rayon final: " + config.getRayonFinalBordure() + " blocs !");
                    diffuserMessage("§c§l» Combat à mort dans l'arène finale !");
                    diffuserMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    diffuserMessage("");
                    jouerSon(Sound.ENDERDRAGON_DEATH, 1.0f, 2.0f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void verifierJoueursDansBordure() {
        for (Player joueur : joueursVivants) {
            if (joueur.getWorld().equals(centreMap.getWorld())) {
                Location locationJoueur = joueur.getLocation();

                double deltaX = Math.abs(locationJoueur.getX() - centreMap.getX());
                double deltaZ = Math.abs(locationJoueur.getZ() - centreMap.getZ());
                double distanceMaximale = Math.max(deltaX, deltaZ);

                UUID joueurId = joueur.getUniqueId();
                long tempsActuel = System.currentTimeMillis();
                Long dernierMessage = dernierMessageBordure.get(joueurId);
                
                if (distanceMaximale > rayonBordureActuel) {
                    double distanceDansBordure = distanceMaximale - rayonBordureActuel;
                    int degatsBase = config.getDegatsParSecondeBordure();

                    int degats = degatsBase;
                    if (distanceDansBordure > 5) {
                        degats = (int) (degatsBase * 1.5);
                    }
                    if (distanceDansBordure > 10) {
                        degats = degatsBase * 2;
                    }
                    
                    joueur.damage(degats);

                    Location locationEffet = joueur.getLocation().add(0, 1, 0);

                    for (int i = 0; i < 8; i++) {
                        Location particleLocation = locationEffet.clone().add(
                            (Math.random() - 0.5) * 1.0,
                            (Math.random() - 0.5) * 1.0,
                            (Math.random() - 0.5) * 1.0
                        );
                        locationEffet.getWorld().playEffect(particleLocation, org.bukkit.Effect.CRIT, null);
                    }
                    
                    if (distanceDansBordure > 5) {
                        for (int i = 0; i < 3; i++) {
                            Location smokeLocation = locationEffet.clone().add(
                                (Math.random() - 0.5) * 0.6,
                                (Math.random() - 0.5) * 0.6,
                                (Math.random() - 0.5) * 0.6
                            );
                            locationEffet.getWorld().playEffect(smokeLocation, org.bukkit.Effect.SMOKE, 4);
                        }
                    }

                    if (dernierMessage == null || tempsActuel - dernierMessage > 3000) {
                        if (distanceDansBordure <= 3) {
                            joueur.sendMessage("§c» Attention ! Vous êtes dans la bordure !");
                            joueur.sendMessage("§7» Retournez au centre rapidement !");
                        } else if (distanceDansBordure <= 8) {
                            joueur.sendMessage("§4» DANGER ! Vous êtes loin dans la bordure !");
                            joueur.sendMessage("§c» Dégâts augmentés ! Retournez au centre !");
                        } else {
                            joueur.sendMessage("§4§l» DANGER EXTRÊME ! Bordure mortelle !");
                            joueur.sendMessage("§c§l» Retournez au centre IMMÉDIATEMENT !");
                        }

                        joueur.playSound(joueur.getLocation(), Sound.NOTE_BASS, 1.0f, 0.5f);
                        
                        dernierMessageBordure.put(joueurId, tempsActuel);
                    }
                    
                } else if (distanceMaximale > rayonBordureActuel - 10) {
                    if (dernierMessage == null || tempsActuel - dernierMessage > 5000) {
                        double distanceRestante = rayonBordureActuel - distanceMaximale;
                        
                        if (distanceRestante <= 3) {
                            joueur.sendMessage("§c» URGENT ! La bordure est très proche !");
                            joueur.sendMessage("§7» Distance restante: §c" + String.format("%.1f", distanceRestante) + " blocs");
                            joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 1.8f);
                        } else if (distanceRestante <= 6) {
                            joueur.sendMessage("§6» Attention ! La bordure approche !");
                            joueur.sendMessage("§7» Distance restante: §6" + String.format("%.1f", distanceRestante) + " blocs");
                            joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 1.4f);
                        } else {
                            joueur.sendMessage("§e» La bordure se rapproche...");
                            joueur.sendMessage("§7» Distance restante: §e" + String.format("%.1f", distanceRestante) + " blocs");
                        }
                        
                        dernierMessageBordure.put(joueurId, tempsActuel);
                    }
                } else {
                    dernierMessageBordure.remove(joueurId);
                }
            }
        }
    }

    public void eliminerJoueur(Player joueur, Player tueur) {
        if (!joueursVivants.contains(joueur)) return;

        joueursVivants.remove(joueur);
        spectateurs.add(joueur);

        dernierMessageBordure.remove(joueur.getUniqueId());

        JoueurPartie joueurPartie = joueurs.get(joueur.getUniqueId());
        joueurPartie.definirVivant(false);
        joueurPartie.definirSpectateur(true);
        joueurPartie.definirPosition(joueursVivants.size() + 1);

        Location locationMort = joueur.getLocation();
        
        monde.strikeLightningEffect(locationMort);
        jouerSon(Sound.AMBIENCE_THUNDER, 1.0f, 1.0f);

        if (tueur != null && joueursVivants.contains(tueur)) {
            JoueurPartie tueurPartie = joueurs.get(tueur.getUniqueId());
            tueurPartie.ajouterKill();
            tueurPartie.ajouterPoints(config.getPointsKill());

            diffuserMessage(ChatColor.RED + joueurPartie.getNomAnonymise() +
                           ChatColor.GRAY + " a ete elimine par " +
                           ChatColor.RED + tueurPartie.getNomAnonymise());
        } else {
            diffuserMessage(ChatColor.RED + joueurPartie.getNomAnonymise() +
                           ChatColor.GRAY + " a ete elimine");
        }

        mettreEnSpectateur(joueur);
        attribuerPointsPosition(joueur);

        notifierPositionsJoueursVivants();

        if (joueursVivants.size() <= 1 && !partieTerminee) {
            terminerPartie();
        }
    }

    private void attribuerPointsPosition(Player joueur) {
        JoueurPartie joueurPartie = joueurs.get(joueur.getUniqueId());
        int position = joueurPartie.getPosition();

        if (position <= 12 && position > 8) {
            int pointsBonus = config.getPointsTop12();
            joueurPartie.ajouterPoints(pointsBonus);
            joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » TOP 12 ATTEINT «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§e» Félicitations ! Vous êtes dans le §6TOP 12 §e!");
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage("§7» Continuez comme ça pour atteindre le TOP 8 !");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        } else if (position <= 8 && position > 5) {
            int pointsBonus = config.getPointsTop8();
            joueurPartie.ajouterPoints(pointsBonus);
            joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.2f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » TOP 8 ATTEINT «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§e» Excellent ! Vous êtes dans le §6TOP 8 §e!");
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage("§7» Plus que quelques joueurs avant le TOP 5 !");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        } else if (position <= 5 && position > 3) {
            int pointsBonus = config.getPointsTop5();
            joueurPartie.ajouterPoints(pointsBonus);
            joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.4f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » TOP 5 ATTEINT «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§e» Incroyable ! Vous êtes dans le §6TOP 5 §e!");
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage("§7» Le podium est à portée de main !");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        } else if (position <= 3 && position > 2) {
            int pointsBonus = config.getPointsTop3();
            joueurPartie.ajouterPoints(pointsBonus);
            joueur.playSound(joueur.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 1.0f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » TOP 3 ATTEINT «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§e» Fantastique ! Vous êtes sur le §6PODIUM §e!");
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage("§7» Vous êtes si proche de la victoire !");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        } else if (position == 2) {
            int pointsBonus = config.getPointsTop2();
            joueurPartie.ajouterPoints(pointsBonus);
            joueur.playSound(joueur.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 1.2f);
            joueur.playSound(joueur.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1.0f, 1.0f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l              » 2ÈME PLACE ATTEINTE «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§e» Extraordinaire ! Vous terminez §62ème §e!");
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage("§7» Une performance remarquable !");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        } else if (position == 1) {
            int pointsBonus = config.getPointsTop1();
            joueurPartie.ajouterPoints(pointsBonus);
            joueur.playSound(joueur.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 1.5f);
            joueur.playSound(joueur.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1.0f, 1.2f);
            joueur.playSound(joueur.getLocation(), Sound.ENDERDRAGON_DEATH, 0.5f, 2.0f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » VICTOIRE ! «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§e» §6§lFÉLICITATIONS ! §e§lVous avez gagné la partie !");
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage("§7» Vous êtes le dernier survivant !");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        }
    }

    private void notifierPositionsJoueursVivants() {
        int nombreJoueursVivants = joueursVivants.size();

        if (nombreJoueursVivants == 12) {
            notifierTousJoueursVivants("TOP 12", config.getPointsTop12(), "§e» Félicitations ! Vous êtes dans le §6TOP 12 §e!", "§7» Continuez comme ça pour atteindre le TOP 10 !");
        } else if (nombreJoueursVivants == 10) {
            notifierTousJoueursVivants("TOP 10", config.getPointsTop10(), "§e» Excellent ! Vous êtes dans le §6TOP 10 §e!", "§7» Plus que quelques joueurs avant le TOP 8 !");
        } else if (nombreJoueursVivants == 8) {
            notifierTousJoueursVivants("TOP 8", config.getPointsTop8(), "§e» Excellent ! Vous êtes dans le §6TOP 8 §e!", "§7» Plus que quelques joueurs avant le TOP 5 !");
        } else if (nombreJoueursVivants == 5) {
            notifierTousJoueursVivants("TOP 5", config.getPointsTop5(), "§e» Incroyable ! Vous êtes dans le §6TOP 5 §e!", "§7» Le podium est à portée de main !");
        } else if (nombreJoueursVivants == 3) {
            notifierTousJoueursVivants("TOP 3", config.getPointsTop3(), "§e» Fantastique ! Vous êtes sur le §6PODIUM §e!", "§7» Vous êtes si proche de la victoire !");
        }
    }

    private void notifierTousJoueursVivants(String nomTop, int pointsBonus, String messagePrincipal, String messageEncouragement) {
        for (Player joueur : joueursVivants) {
            JoueurPartie joueurPartie = joueurs.get(joueur.getUniqueId());
            joueurPartie.ajouterPoints(pointsBonus);
            
            joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.2f);
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » " + nomTop + " ATTEINT «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage(messagePrincipal);
            joueur.sendMessage("§a» Points bonus: §6+" + pointsBonus + " points");
            joueur.sendMessage(messageEncouragement);
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
        }
    }

    public void mettreEnSpectateur(Player joueur) {
        joueur.setGameMode(GameMode.SPECTATOR);

        joueur.sendMessage(ChatColor.YELLOW + "Vous etes maintenant spectateur !");
        joueur.sendMessage(ChatColor.GRAY + "Vous pouvez observer la partie ou quitter avec /lobby");
    }

    private void terminerPartie() {
        if (partieTerminee) return;
        partieTerminee = true;

        etat = EtatPartie.TERMINEE;

        for (Player joueur : getJoueurs()) {
            gestionnaire.getGestionnairePrincipal().getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
        }

        if (tachePartie != null) tachePartie.cancel();
        if (tacheBordure != null) tacheBordure.cancel();
        if (tacheColisStrategique != null) tacheColisStrategique.cancel();
        
        for (BukkitTask tache : tachesColisEffets) {
            if (tache != null) {
                tache.cancel();
            }
        }
        tachesColisEffets.clear();
        joueursProtectionFly.clear();

        if (!joueursVivants.isEmpty()) {
            Player gagnant = joueursVivants.get(0);
            JoueurPartie gagnantPartie = joueurs.get(gagnant.getUniqueId());
            gagnantPartie.definirPosition(1);
            gagnantPartie.ajouterPoints(config.getPointsTop1());

            diffuserMessage(ChatColor.GOLD + "━━━━━━━━━━ VICTOIRE ━━━━━━━━━━");
            diffuserMessage(ChatColor.YELLOW + "Gagnant: " + ChatColor.GREEN + gagnantPartie.getNomAnonymise());
            diffuserMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        afficherResultats();
        sauvegarderPoints();

        new BukkitRunnable() {
            @Override
            public void run() {
                arreter();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void terminerPartieTempsEcoule() {
        if (partieTerminee) return;

        diffuserMessage(ChatColor.RED + "Temps ecoule ! La partie se termine !");
        terminerPartie();
    }

    private void afficherResultats() {
        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » PARTIE TERMINEE «");
            joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");

            List<JoueurPartie> classement = new ArrayList<>(joueurs.values());
            classement.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));

            joueur.sendMessage("§e» Classement de la partie:");
            for (int i = 0; i < Math.min(5, classement.size()); i++) {
                JoueurPartie joueurPartie = classement.get(i);
                String couleur = i == 0 ? "§6" : i == 1 ? "§e" : i == 2 ? "§7" : "§f";
                String medaille = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "§8▪";
                
                joueur.sendMessage(couleur + medaille + " #" + joueurPartie.getPosition() + " " +
                                 joueurPartie.getNomAnonymise() + " §f- " +
                                 joueurPartie.getPoints() + " points (" +
                                 joueurPartie.getKills() + " kills)");
            }

            joueur.sendMessage("");
            JoueurPartie joueurPartie = joueurs.get(joueur.getUniqueId());
            if (joueurPartie != null) {
                joueur.sendMessage("§a» Vos statistiques:");
                joueur.sendMessage("§f  • Position: §6#" + joueurPartie.getPosition());
                joueur.sendMessage("§f  • Points: §6" + joueurPartie.getPoints());
                joueur.sendMessage("§f  • Kills: §c" + joueurPartie.getKills());
                joueur.sendMessage("§f  • Statut: " + (joueurPartie.estVivant() ? "§aVivant" : "§cElimine"));
            }
            
            joueur.sendMessage("");
            joueur.sendMessage("§7» Vous serez teleporte au lobby dans 10 secondes...");
            joueur.sendMessage("");
        }
    }

    private void sauvegarderPoints() {
        for (Map.Entry<UUID, JoueurPartie> entry : joueurs.entrySet()) {
            Player joueur = Bukkit.getPlayer(entry.getKey());
            if (joueur != null) {
                JoueurPartie joueurPartie = entry.getValue();
                gestionnaire.getGestionnairePrincipal().getGestionnaireClassements()
                          .ajouterPoints(joueur, TypeMiniJeu.HUNGER_GAMES, joueurPartie.getPoints());
            }
        }
    }

    private void mettreAJourScoreboard() {
        if (scoreboardManager != null) {
            scoreboardManager.mettreAJourTousLesScoreboards();
        }
    }

    public void arreter() {
        if (tachePartie != null) tachePartie.cancel();
        if (tacheBordure != null) tacheBordure.cancel();
        if (tacheColisStrategique != null) tacheColisStrategique.cancel();
        
        for (BukkitTask tache : tachesColisEffets) {
            if (tache != null) {
                tache.cancel();
            }
        }
        tachesColisEffets.clear();

        for (Player joueur : getJoueurs()) {
            joueur.setDisplayName(joueur.getName());
            joueur.setPlayerListName(joueur.getName());
            scoreboardManager.supprimerScoreboard(joueur);
            gestionnaire.getGestionnairePrincipal().getGestionnaireLobby().teleporterVersLobby(joueur);
        }

        gestionnaire.getGestionnairePrincipal().getGestionnaireMaps().supprimerMapTemporaire(idPartie);

        if (monde != null && !monde.getName().equals(nomMapOriginale)) {
            gestionnaire.getGestionnaireSetup().supprimerSetupTemporaire(monde.getName());
        }

        gestionnaire.terminerPartie(idPartie);
    }

    public void retirerJoueur(Player joueur) {

        if (joueursVivants.contains(joueur)) {
            eliminerJoueur(joueur, null);
        }
        spectateurs.remove(joueur);
        joueurs.remove(joueur.getUniqueId());
        
        if (scoreboardManager != null) {
            scoreboardManager.supprimerScoreboard(joueur);
        }
        joueur.setScoreboard(plugin.getServer().getScoreboardManager().getNewScoreboard());
        
        joueur.setGameMode(GameMode.SURVIVAL);
        joueur.setHealth(20.0);
        joueur.setFoodLevel(20);
        joueur.setSaturation(20.0f);
        joueur.setWalkSpeed(0.2f);
        joueur.setFlySpeed(0.1f);
        joueur.getInventory().clear();
        joueur.getInventory().setArmorContents(new ItemStack[4]);
        
        for (PotionEffect effet : joueur.getActivePotionEffects()) {
            joueur.removePotionEffect(effet.getType());
        }
        
        joueur.setDisplayName(joueur.getName());
        joueur.setPlayerListName(joueur.getName());
        
        gestionnaire.getGestionnairePrincipal().getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
        
        Location lobby = gestionnaire.getGestionnairePrincipal().getGestionnaireLobby().getSpawnLobby();
        if (lobby != null) {
            joueur.teleport(lobby);
        } else {
            joueur.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
        
        String nomJoueur = joueur.getName();
        
        for (Player autreJoueur : getJoueurs()) {
            if (!autreJoueur.equals(joueur)) {
                autreJoueur.sendMessage("§c» " + nomJoueur + " a abandonné la partie et ne pourra plus revenir.");
            }
        }
        
        joueur.sendMessage("§e» Vous avez quitté votre précédente partie, vous ne pouvez plus y retourner.");
        
        plugin.getLogger().info("Joueur " + nomJoueur + " retiré de la partie HungerGames " + idPartie);
    }

    public boolean estDansMondePartie(Player joueur) {
        return joueur.getWorld().equals(monde);
    }

    private void diffuserMessage(String message) {
        for (Player joueur : getJoueurs()) {
            joueur.sendMessage(message);
        }
    }

    private void jouerSon(Sound son, float volume, float pitch) {
        for (Player joueur : getJoueurs()) {
            joueur.playSound(joueur.getLocation(), son, volume, pitch);
        }
    }

    public List<Player> getJoueurs() {
        List<Player> tousJoueurs = new ArrayList<>(joueursVivants);
        tousJoueurs.addAll(spectateurs);
        return tousJoueurs;
    }

    public boolean estInvincibiliteActive() {
        return invincibiliteActive;
    }

    public boolean sontJoueursFreeze() {
        return joueursFreeze;
    }
    
    public boolean estJoueurProtegeFly(Player joueur) {
        return joueursProtectionFly.contains(joueur.getUniqueId());
    }

    public EtatPartie getEtat() {
        return etat;
    }

    public String getIdPartie() {
        return idPartie;
    }

    public int getNombreJoueursVivants() {
        return joueursVivants.size();
    }

    public JoueurPartie getJoueurPartie(UUID uuid) {
        return joueurs.get(uuid);
    }

    public int getTempsEcoule() {
        return tempsEcoule;
    }

    public ConfigurationHungerGames getConfig() {
        return config;
    }

    public void gererOuvertureCoffre(Player joueur, Location locationCoffre) {
        if (etat != EtatPartie.EN_COURS) return;
        
        if (!locationCoffre.getChunk().isLoaded()) {
            locationCoffre.getChunk().load();
        }
        
        Block block = locationCoffre.getBlock();
        if (block.getType() != Material.CHEST) return;
        
        if (!(block.getState() instanceof Chest)) return;
        
        Chest coffre = (Chest) block.getState();

        boolean estColisStrategique = false;
        for (Location colis : colisStrategiques) {
            if (colis.getBlockX() == locationCoffre.getBlockX() && 
                colis.getBlockY() == locationCoffre.getBlockY() && 
                colis.getBlockZ() == locationCoffre.getBlockZ()) {
                estColisStrategique = true;
                colisStrategiques.remove(colis);
                break;
            }
        }
        
        if (estColisStrategique) {
            JoueurPartie joueurPartie = joueurs.get(joueur.getUniqueId());
            if (joueurPartie != null) {
                diffuserMessage(ChatColor.GOLD + "§l» " + joueurPartie.getNomAnonymise() + 
                               ChatColor.YELLOW + " a ouvert un colis stratégique !");
                jouerSon(Sound.LEVEL_UP, 1.0f, 1.2f);
            }
        }
        
        joueur.openInventory(coffre.getInventory());
    }
}
