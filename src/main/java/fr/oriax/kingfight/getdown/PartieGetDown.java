package fr.oriax.kingfight.getdown;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.JoueurPartie;
import fr.oriax.kingfight.commun.TraducteurItems;
import fr.oriax.kingfight.getdown.GestionnaireSetupGetDown.SetupMonde;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PartieGetDown {

    private final KingFight plugin;
    private final GestionnaireGetDown gestionnaire;
    private final String idPartie;
    private final ConfigurationGetDown config;
    private SetupMonde setupMonde;

    private final Map<UUID, JoueurPartieGetDown> joueurs;
    private final List<Player> joueursVivants;
    private final Map<UUID, Integer> viesJoueurs;
    private final Map<UUID, Integer> kingcoinsJoueurs;
    private final Set<Location> blocsConsommes;
    private final Map<UUID, BukkitTask> joueursEnAttenteArrivee;
    private final Map<Location, List<ItemStack>> enderchestsItems;
    private final Map<UUID, Location> joueursEnderChestOuvert;
    private final Set<UUID> joueursAyantTermineMapActuelle;
    private final Set<UUID> joueursProtectionFly;
    private final Set<UUID> joueursProtectionSpawn;
    private final Map<UUID, BukkitTask> tachesProtectionSpawn;

    private final Map<Integer, World> mondesParcours;
    private World mondePvp;
    private World mondeStuff;
    private final Map<Integer, String> nomsMapsOriginales;
    private String nomMapPvpOriginale;
    private String nomMapStuffOriginale;
    
    private EtatPartie etat;
    private PhasePartie phaseActuelle;

    private BukkitTask tachePartie;
    private BukkitTask tacheBordure;
    private BukkitTask tacheScoreboard;

    private int tempsEcoule;
    private int mapActuelle;
    private boolean premierArrive;
    private boolean countdownEnCours;
    private boolean messageTeleportationEnvoye;
    private boolean resultatsAffiches;
    private boolean joueursFreeze;
    private boolean invincibiliteActive;

    private ScoreboardGetDown scoreboardManager;

    public enum EtatPartie {
        ATTENTE, PREPARATION, EN_COURS, TERMINEE
    }

    public enum PhasePartie {
        JUMP, STUFF, PVP
    }

    public static class JoueurPartieGetDown extends JoueurPartie {
        private int kingcoins;
        private int mapsCompletes;

        public JoueurPartieGetDown(Player joueur, String nomAnonymise) {
            super(joueur, nomAnonymise);
            this.kingcoins = 0;
            this.mapsCompletes = 0;
        }

        public int getKingcoins() { return kingcoins; }
        public void ajouterKingcoins(int montant) { this.kingcoins += montant; }
        public void retirerKingcoins(int montant) { this.kingcoins = Math.max(0, this.kingcoins - montant); }

        public int getMapsCompletes() { return mapsCompletes; }
        public void incrementerMapsCompletes() { this.mapsCompletes++; }
    }

    public PartieGetDown(KingFight plugin, GestionnaireGetDown gestionnaire, String idPartie,
                        List<Player> listeJoueurs, ConfigurationGetDown config) {
        this.plugin = plugin;
        this.gestionnaire = gestionnaire;
        this.idPartie = idPartie;
        this.config = config;
        this.joueurs = new HashMap<>();
        this.joueursVivants = new ArrayList<>();
        this.viesJoueurs = new HashMap<>();
        this.kingcoinsJoueurs = new HashMap<>();
        this.blocsConsommes = ConcurrentHashMap.newKeySet();
        this.joueursEnAttenteArrivee = new HashMap<>();
        this.enderchestsItems = new HashMap<>();
        this.joueursEnderChestOuvert = new HashMap<>();
        this.joueursAyantTermineMapActuelle = ConcurrentHashMap.newKeySet();
        this.joueursProtectionFly = new HashSet<>();
        this.joueursProtectionSpawn = new HashSet<>();
        this.tachesProtectionSpawn = new HashMap<>();
        
        this.mondesParcours = new HashMap<>();
        this.nomsMapsOriginales = new HashMap<>();
        
        this.etat = EtatPartie.ATTENTE;
        this.phaseActuelle = PhasePartie.JUMP;
        this.tempsEcoule = 0;
        this.mapActuelle = 0;
        this.premierArrive = false;
        this.countdownEnCours = false;
        this.messageTeleportationEnvoye = false;
        this.resultatsAffiches = false;
        this.joueursFreeze = false;
        this.invincibiliteActive = false;

        initialiserJoueurs(listeJoueurs);
        if (!creerToutesLesMaps()) {
            plugin.getLogger().severe("Impossible de créer toutes les maps nécessaires pour Get Down !");
            return;
        }

        this.setupMonde = obtenirSetupMonde();
        if (setupMonde == null) {
            plugin.getLogger().severe("Setup manquant pour les maps Get Down");
            return;
        }

        this.scoreboardManager = new ScoreboardGetDown(this);
    }

    private SetupMonde obtenirSetupMonde() {
        String nomMapPrincipale = nomsMapsOriginales.get(0);
        if (nomMapPrincipale == null) {
            return null;
        }
        
        if (gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapPrincipale) == null) {
            return null;
        }

        for (int i = 0; i < 3; i++) {
            World mondeParcours = mondesParcours.get(i);
            String nomMapOriginale = nomsMapsOriginales.get(i);
            
            if (mondeParcours != null && !mondeParcours.getName().equals(nomMapOriginale)) {
                gestionnaire.getGestionnaireSetup().copierSetupVersMondeTemporaire(nomMapOriginale, mondeParcours.getName());
            }
        }
        
        if (mondePvp != null && !mondePvp.getName().equals(nomMapPvpOriginale)) {
            gestionnaire.getGestionnaireSetup().copierSetupVersMondeTemporaire(nomMapPvpOriginale, mondePvp.getName());
        }

        return gestionnaire.getGestionnaireSetup().getSetupMonde(mondesParcours.get(0).getName());
    }

    private void initialiserJoueurs(List<Player> listeJoueurs) {
        List<String> nomsAnonymises = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireJoueurs().getNomsAnonymises(listeJoueurs);

        for (int i = 0; i < listeJoueurs.size(); i++) {
            Player joueur = listeJoueurs.get(i);
            JoueurPartieGetDown joueurPartie = new JoueurPartieGetDown(joueur, nomsAnonymises.get(i));
            joueurs.put(joueur.getUniqueId(), joueurPartie);
            joueursVivants.add(joueur);
            viesJoueurs.put(joueur.getUniqueId(), config.getViesPvp());
            kingcoinsJoueurs.put(joueur.getUniqueId(), 0);
        }
    }

    private boolean creerToutesLesMaps() {
        GestionnaireSetupGetDown.ConfigurationMultiMaps configMaps = 
            gestionnaire.getGestionnaireSetup().getConfigurationMultiMaps();
        
        if (!configMaps.estComplet()) {
            plugin.getLogger().severe("Configuration des maps Get Down incomplète ! Utilisez /setup getdown maps pour configurer.");
            return false;
        }
        
        for (int i = 0; i < 3; i++) {
            String nomMapParcours = null;
            switch (i) {
                case 0: nomMapParcours = configMaps.getMapParcours1(); break;
                case 1: nomMapParcours = configMaps.getMapParcours2(); break;
                case 2: nomMapParcours = configMaps.getMapParcours3(); break;
            }
            
            if (nomMapParcours == null) {
                plugin.getLogger().severe("Map parcours " + (i + 1) + " non configurée !");
                return false;
            }
            
            World mondeParcours = gestionnaire.getGestionnairePrincipal()
                    .getGestionnaireMaps().creerMapTemporaire(nomMapParcours, idPartie + "_parcours" + (i + 1));
            
            if (mondeParcours == null) {
                plugin.getLogger().severe("Impossible de créer la map temporaire pour le parcours " + (i + 1));
                return false;
            }
            
            mondesParcours.put(i, mondeParcours);
            nomsMapsOriginales.put(i, nomMapParcours);
        }

        if (configMaps.aPhasePvp()) {
            nomMapPvpOriginale = configMaps.getMapPvp();
            mondePvp = gestionnaire.getGestionnairePrincipal()
                    .getGestionnaireMaps().creerMapTemporaire(nomMapPvpOriginale, idPartie + "_pvp");
            
            if (mondePvp == null) {
                plugin.getLogger().warning("Impossible de créer la map PvP, utilisation de la dernière map de parcours");
                mondePvp = mondesParcours.get(2);
            }
        } else {
            mondePvp = mondesParcours.get(2);
            nomMapPvpOriginale = nomsMapsOriginales.get(2);
        }

        mondeStuff = mondePvp;
        nomMapStuffOriginale = nomMapPvpOriginale;
        
        plugin.getLogger().info("Toutes les maps Get Down créées avec succès pour la partie " + idPartie);
        return true;
    }

    public void demarrer() {
        if (mondesParcours.isEmpty() || setupMonde == null) {
            arreter();
            return;
        }

        etat = EtatPartie.PREPARATION;
        configurerMondes();
        verifierEtChargerSpawnsPvp();
        teleporterJoueursAuSpawn();
        preparerJoueurs();
        creerScoreboards();
        demarrerMiseAJourScoreboards();

        demarrerCountdownPreparation();
    }

    private void configurerMondes() {
        for (World monde : mondesParcours.values()) {
            configurerMonde(monde, "Parcours");
        }

        if (mondePvp != null) {
            configurerMonde(mondePvp, "PvP");
        }

        if (mondeStuff != null && !mondeStuff.equals(mondePvp)) {
            configurerMonde(mondeStuff, "Stuff");
        }
    }
    
    private void configurerMonde(World monde, String type) {
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
        
        plugin.getLogger().info("Monde GetDown " + type + " " + monde.getName() + " configuré en mode Peaceful avec jour permanent");
    }
    
    private void verifierEtChargerSpawnsPvp() {
        plugin.getLogger().info("Vérification et chargement des spawns PVP au démarrage...");
        
        if (mondePvp == null || nomMapPvpOriginale == null) {
            plugin.getLogger().warning("Monde PVP ou nom de map PVP originale null");
            return;
        }
        
        GestionnaireSetupGetDown.SetupMonde setupPvp = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(mondePvp.getName());
        
        if (setupPvp == null) {
            plugin.getLogger().warning("SetupMonde null lors de la vérification des spawns PVP");
            return;
        }
        
        Map<Integer, Location> spawnsPvp = setupPvp.getSpawnsPvp();
        plugin.getLogger().info("Spawns PVP dans le setup temporaire: " + spawnsPvp.size());
        
        if (spawnsPvp.isEmpty()) {
            plugin.getLogger().info("Aucun spawn PVP dans le setup temporaire, chargement depuis l'original...");
            GestionnaireSetupGetDown.SetupMonde setupOriginal = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapPvpOriginale);
            
            if (setupOriginal != null && !setupOriginal.getSpawnsPvp().isEmpty()) {
                plugin.getLogger().info("Spawns PVP trouvés dans le setup original: " + setupOriginal.getSpawnsPvp().size());
                
                for (Map.Entry<Integer, Location> entry : setupOriginal.getSpawnsPvp().entrySet()) {
                    Location originalLoc = entry.getValue();
                    Location newLoc = new Location(mondePvp, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), 
                                                 originalLoc.getYaw(), originalLoc.getPitch());
                    setupPvp.definirSpawnPvp(entry.getKey(), newLoc);
                    plugin.getLogger().info("Spawn PVP " + entry.getKey() + " préchargé: " + 
                        originalLoc.getBlockX() + "," + originalLoc.getBlockY() + "," + originalLoc.getBlockZ());
                }
                
                plugin.getLogger().info("Spawns PVP préchargés avec succès: " + setupPvp.getSpawnsPvp().size());
            } else {
                plugin.getLogger().warning("Aucun spawn PVP trouvé dans le setup original pour la map " + nomMapPvpOriginale);
            }
        } else {
            plugin.getLogger().info("Spawns PVP déjà présents dans le setup temporaire: " + spawnsPvp.size());
        }
    }

    private void teleporterJoueursAuSpawn() {
        String nomMapParcours1 = nomsMapsOriginales.get(0);
        GestionnaireSetupGetDown.SetupMonde setupParcours1 = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapParcours1);
        
        if (setupParcours1 == null) {
            plugin.getLogger().warning("Setup manquant pour la première map de parcours");
            return;
        }
        
        List<Location> spawnsDescente = setupParcours1.getSpawnsDescente();
        if (spawnsDescente.isEmpty() || spawnsDescente.get(0) == null) {
            plugin.getLogger().warning("Aucun spawn configuré pour la première map de parcours");
            return;
        }

        Location spawnOriginal = spawnsDescente.get(0);
        Location spawnDansMondeTemporaire = new Location(
            mondesParcours.get(0),
            spawnOriginal.getX(),
            spawnOriginal.getY(),
            spawnOriginal.getZ(),
            spawnOriginal.getYaw(),
            spawnOriginal.getPitch()
        );

        for (Player joueur : getJoueurs()) {
            joueur.teleport(spawnDansMondeTemporaire);
        }
    }

    private void preparerJoueurs() {
        for (Player joueur : getJoueurs()) {
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

            JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
            joueur.setDisplayName(ChatColor.WHITE + joueurPartie.getNomAnonymise());
            joueur.setPlayerListName(ChatColor.WHITE + joueurPartie.getNomAnonymise());
        }
    }

    private void demarrerCountdownPreparation() {
        joueursFreeze = true;
        for (Player joueur : getJoueurs()) {
            joueur.setWalkSpeed(0.0f);
            joueur.setFlySpeed(0.0f);
            joueur.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
            joueur.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));
            joueur.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, 255, false, false));
            
            if (joueur.isFlying() || joueur.getAllowFlight()) {
                joueursProtectionFly.add(joueur.getUniqueId());
                joueur.setAllowFlight(true);
                joueur.setFlying(false);
            }
            joueur.sendMessage("");
            joueur.sendMessage("§e§l» PREPARATION DE LA PARTIE «");
            joueur.sendMessage("§7La partie commence dans 10 secondes...");
            joueur.sendMessage("");
        }

        final int[] countdown = {10};
        final BukkitTask[] countdownTask = {null};
        countdownTask[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (countdownTask[0] != null) countdownTask[0].cancel();
                return;
            }
            
            if (countdown[0] <= 0) {
                joueursFreeze = false;
                for (Player joueur : getJoueurs()) {
                    joueur.setWalkSpeed(0.2f);
                    joueur.setFlySpeed(0.1f);
                    
                    if (joueursProtectionFly.contains(joueur.getUniqueId())) {
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            joueursProtectionFly.remove(joueur.getUniqueId());
                            if (joueur.isOnline() && joueur.getGameMode() == GameMode.SURVIVAL) {
                                joueur.setAllowFlight(false);
                                joueur.setFlying(false);
                            }
                        }, 40L);
                    }
                    joueur.removePotionEffect(PotionEffectType.SLOW);
                    joueur.removePotionEffect(PotionEffectType.JUMP);
                    joueur.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                    joueur.sendMessage("§a§lC'EST PARTI !");
                    joueur.sendMessage("§7Bonne chance !");
                    joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                }
                etat = EtatPartie.EN_COURS;
                demarrerPhaseJump();
                countdownTask[0].cancel();
                return;
            }

            for (Player joueur : getJoueurs()) {
                String couleur = countdown[0] <= 3 ? "§c" : countdown[0] <= 5 ? "§e" : "§a";
                joueur.sendMessage(couleur + "§l" + countdown[0] + " §7- Préparez-vous...");
                
                if (countdown[0] <= 5) {
                    joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                }
            }
            countdown[0]--;
        }, 0L, 20L);
    }

    private void demarrerPhaseJump() {
        phaseActuelle = PhasePartie.JUMP;
        mapActuelle = 0;

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§a§l                    » PHASE JUMP «");
            joueur.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§e» Descendez les " + config.getNombreMaps() + " maps le plus vite possible !");
            joueur.sendMessage("§7» Collectez les blocs spéciaux pour gagner des KingCoins !");
            joueur.sendMessage("");
        }

        demarrerMap(0);
    }

    private void demarrerMap(int numeroMap) {
        if (numeroMap >= config.getNombreMaps()) {
            demarrerPhaseStuff();
            return;
        }

        mapActuelle = numeroMap;
        premierArrive = false;
        countdownEnCours = false;
        tempsEcoule = 0;

        joueursAyantTermineMapActuelle.clear();

        teleporterJoueursMap(numeroMap);
        demarrerInvincibiliteTemporaire();

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » MAP " + (numeroMap + 1) + "/" + config.getNombreMaps() + " «");
            joueur.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§e» Temps limite: " + (config.getDureeParMapSecondes() / 60) + "m" + (config.getDureeParMapSecondes() % 60) + "s");
            joueur.sendMessage("§c» Invincibilité: 10 secondes");
            joueur.sendMessage("");
        }

        if (tachePartie != null) {
            tachePartie.cancel();
            tachePartie = null;
        }

        tachePartie = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tachePartie != null) tachePartie.cancel();
                return;
            }
            
            tempsEcoule++;

            int tempsRestant = config.getDureeParMapSecondes() - tempsEcoule;
            if (tempsRestant == 10) {
                String prochainePhaseMSG = (numeroMap + 1 >= config.getNombreMaps()) ? 
                    "§e» Téléportation vers la phase STUFF dans 10 secondes !" :
                    "§e» Téléportation vers le parcours " + (numeroMap + 2) + " dans 10 secondes !";
                
                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("");
                    joueur.sendMessage("§6§l⚠ ATTENTION ⚠");
                    joueur.sendMessage(prochainePhaseMSG);
                    joueur.sendMessage("");
                    joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 0.5f);
                }
            }

            if (tempsEcoule >= config.getDureeParMapSecondes()) {
                if (tachePartie != null) {
                    tachePartie.cancel();
                    tachePartie = null;
                }
                
                if (numeroMap + 1 >= config.getNombreMaps()) {
                    demarrerPhaseStuff();
                } else {
                    demarrerMap(numeroMap + 1);
                }
            }
        }, 20L, 20L);
    }

    private void teleporterJoueursMap(int numeroMap) {

        World mondeParcours = mondesParcours.get(numeroMap);
        if (mondeParcours == null) {
            plugin.getLogger().warning("Monde de parcours " + numeroMap + " non trouvé !");
            return;
        }

        String nomMapOriginale = nomsMapsOriginales.get(numeroMap);
        GestionnaireSetupGetDown.SetupMonde setupOriginal = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
        
        if (setupOriginal == null) {
            plugin.getLogger().warning("Setup manquant pour la map originale " + nomMapOriginale);
            return;
        }

        List<Location> spawnsDescente = setupOriginal.getSpawnsDescente();
        if (spawnsDescente.isEmpty() || spawnsDescente.get(0) == null) {
            plugin.getLogger().warning("Aucun spawn configuré pour la map " + nomMapOriginale);
            return;
        }

        Location spawnOriginal = spawnsDescente.get(0);
        Location spawnDansMondeTemporaire = new Location(
            mondeParcours,
            spawnOriginal.getX(),
            spawnOriginal.getY(),
            spawnOriginal.getZ(),
            spawnOriginal.getYaw(),
            spawnOriginal.getPitch()
        );

        for (Player joueur : getJoueurs()) {
            joueur.teleport(spawnDansMondeTemporaire);
        }
    }

    public void gererBlocSpecial(Player joueur, Location locationBloc) {
        JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null || phaseActuelle != PhasePartie.JUMP || countdownEnCours) return;

        if (blocsConsommes.contains(locationBloc)) {
            return;
        }

        Material material = locationBloc.getBlock().getType();
        ConfigurationGetDown.BlocSpecial blocSpecial = config.getBlocSpecial(material);
        if (blocSpecial == null) return;

        blocsConsommes.add(locationBloc);
        locationBloc.getBlock().setType(config.getBlocRemplacement());

        int kingcoinsGagnes = blocSpecial.getKingcoinsAleatoire();
        if (kingcoinsGagnes > 0) {
            joueurPartie.ajouterKingcoins(kingcoinsGagnes);
        } else if (kingcoinsGagnes < 0) {
            joueurPartie.retirerKingcoins(Math.abs(kingcoinsGagnes));
        }

        String effetMessage = "";
        if (!blocSpecial.getEffets().isEmpty()) {
            effetMessage = appliquerEffetsBloc(joueur, blocSpecial.getEffets());
        }

        String message = blocSpecial.getMessage().replace("{kingcoins}", String.valueOf(kingcoinsGagnes));
        joueur.sendMessage(message);
        if (!effetMessage.isEmpty()) {
            joueur.sendMessage(effetMessage);
        }

        try {
            Sound son = Sound.valueOf(blocSpecial.getSon());
            joueur.playSound(joueur.getLocation(), son, blocSpecial.getVolumeSon(), blocSpecial.getPitchSon());
        } catch (IllegalArgumentException e) {
            joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        }

        kingcoinsJoueurs.put(joueur.getUniqueId(), joueurPartie.getKingcoins());
    }

    public void verifierBlocSousJoueur(Player joueur) {
        if (phaseActuelle != PhasePartie.JUMP) return;

        Location positionJoueur = joueur.getLocation();
        Location locationSous = new Location(positionJoueur.getWorld(),
                                           Math.floor(positionJoueur.getX()),
                                           Math.floor(positionJoueur.getY() - 1),
                                           Math.floor(positionJoueur.getZ()));

        Material materialSous = locationSous.getBlock().getType();

        if (config.getBlocSpecial(materialSous) != null) {
            gererBlocSpecial(joueur, locationSous);
        }

        verifierArriveeMap(joueur);
    }

    public void verifierArriveeMap(Player joueur) {
        if (phaseActuelle != PhasePartie.JUMP) return;

        World mondeActuel = mondesParcours.get(mapActuelle);
        if (mondeActuel == null) return;

        String nomMapOriginale = nomsMapsOriginales.get(mapActuelle);
        GestionnaireSetupGetDown.SetupMonde setupActuel = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
        
        if (setupActuel == null) return;

        Integer hauteurArrivee = setupActuel.getHauteurArrivee(mapActuelle);
        if (hauteurArrivee != null) {
            Location positionJoueur = joueur.getLocation();
            if (positionJoueur.getY() <= hauteurArrivee) {
                if (!joueursEnAttenteArrivee.containsKey(joueur.getUniqueId())) {
                    demarrerAttenteArrivee(joueur);
                }
                return;
            }
        }

        List<GestionnaireSetupGetDown.ZoneArrivee> zonesArrivees = setupActuel.getZonesArrivees();
        if (!zonesArrivees.isEmpty() && zonesArrivees.get(0) != null) {
            GestionnaireSetupGetDown.ZoneArrivee zone = zonesArrivees.get(0);
            if (zone.contientJoueur(joueur)) {
                gererArriveeMap(joueur);
                return;
            }
        }

        List<Location> arriveesMaps = setupActuel.getArriveesMaps();
        if (!arriveesMaps.isEmpty() && arriveesMaps.get(0) != null) {
            Location arriveeMap = arriveesMaps.get(0);
            Location positionJoueur = joueur.getLocation();

            if (positionJoueur.distance(arriveeMap) <= 3.0) {
                gererArriveeMap(joueur);
            }
        }
    }

    private void demarrerAttenteArrivee(Player joueur) {
        UUID joueurId = joueur.getUniqueId();

        if (!joueur.isOnline() || joueur.isDead() || joueur.getHealth() <= 0) {
            return;
        }

        BukkitTask tacheAttente = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (joueur.isOnline()) {
                if (joueur.isDead() || joueur.getHealth() <= 0) {
                    joueur.sendMessage("§c» Vous êtes arrivé... mais mort ! Ressayez.");
                } else {
                    World mondeActuel = mondesParcours.get(mapActuelle);
                    if (mondeActuel != null && joueur.getWorld().equals(mondeActuel)) {
                        String nomMapOriginale = nomsMapsOriginales.get(mapActuelle);
                        GestionnaireSetupGetDown.SetupMonde setupActuel = 
                            gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
                        
                        if (setupActuel != null) {
                            Integer hauteurArrivee = setupActuel.getHauteurArrivee(mapActuelle);
                            if (hauteurArrivee != null && joueur.getLocation().getY() <= hauteurArrivee) {
                                gererArriveeMap(joueur);
                            }
                        }
                    }
                }
            }

            joueursEnAttenteArrivee.remove(joueurId);
        }, 60L);

        joueursEnAttenteArrivee.put(joueurId, tacheAttente);

        joueur.sendMessage("§e» Arrivée détectée ! Vérification en cours...");
    }

    private String appliquerEffetsBloc(Player joueur, List<String> effets) {
        if (effets.isEmpty()) return "";

        String effetChoisi = effets.get(new Random().nextInt(effets.size()));
        String[] parts = effetChoisi.split(":");

        if (parts.length >= 3) {
            try {
                PotionEffectType type = obtenirTypePotionParNom(parts[0]);
                int niveau = Integer.parseInt(parts[1]);
                int duree = Integer.parseInt(parts[2]);

                if (type != null) {
                    joueur.addPotionEffect(new PotionEffect(type, duree * 20, niveau - 1));
                    String nomEffet = obtenirNomEffetFrancais(type);
                    String couleurEffet = obtenirCouleurEffet(type);
                    
                    String titre = couleurEffet + nomEffet + " " + niveau;
                    String sousTitre = "§7Durée: " + duree + "s";
                    
                    joueur.sendTitle(titre, sousTitre);
                    
                    return "§7Effet: " + nomEffet + " " + niveau + " pendant " + duree + "s";
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Effet de bloc special invalide: " + effetChoisi);
            }
        }
        return "";
    }

    private PotionEffectType obtenirTypePotionParNom(String nom) {
        switch (nom.toUpperCase()) {
            case "SPEED": return PotionEffectType.SPEED;
            case "SLOWNESS": return PotionEffectType.SLOW;
            case "HASTE": return PotionEffectType.FAST_DIGGING;
            case "MINING_FATIGUE": return PotionEffectType.SLOW_DIGGING;
            case "STRENGTH": return PotionEffectType.INCREASE_DAMAGE;
            case "INSTANT_HEALTH": return PotionEffectType.HEAL;
            case "INSTANT_DAMAGE": return PotionEffectType.HARM;
            case "JUMP_BOOST": return PotionEffectType.JUMP;
            case "NAUSEA": return PotionEffectType.CONFUSION;
            case "REGENERATION": return PotionEffectType.REGENERATION;
            case "RESISTANCE": return PotionEffectType.DAMAGE_RESISTANCE;
            case "FIRE_RESISTANCE": return PotionEffectType.FIRE_RESISTANCE;
            case "WATER_BREATHING": return PotionEffectType.WATER_BREATHING;
            case "INVISIBILITY": return PotionEffectType.INVISIBILITY;
            case "BLINDNESS": return PotionEffectType.BLINDNESS;
            case "NIGHT_VISION": return PotionEffectType.NIGHT_VISION;
            case "HUNGER": return PotionEffectType.HUNGER;
            case "WEAKNESS": return PotionEffectType.WEAKNESS;
            case "POISON": return PotionEffectType.POISON;
            case "WITHER": return PotionEffectType.WITHER;
            case "HEALTH_BOOST": return PotionEffectType.HEALTH_BOOST;
            case "ABSORPTION": return PotionEffectType.ABSORPTION;
            case "SATURATION": return PotionEffectType.SATURATION;
            default: return PotionEffectType.getByName(nom);
        }
    }

    private String obtenirNomEffetFrancais(PotionEffectType type) {
        if (type == PotionEffectType.SPEED) return "Vitesse";
        if (type == PotionEffectType.SLOW) return "Lenteur";
        if (type == PotionEffectType.FAST_DIGGING) return "Célérité";
        if (type == PotionEffectType.SLOW_DIGGING) return "Fatigue";
        if (type == PotionEffectType.INCREASE_DAMAGE) return "Force";
        if (type == PotionEffectType.HEAL) return "Soin instantané";
        if (type == PotionEffectType.HARM) return "Dégâts instantanés";
        if (type == PotionEffectType.JUMP) return "Saut amélioré";
        if (type == PotionEffectType.CONFUSION) return "Nausée";
        if (type == PotionEffectType.REGENERATION) return "Régénération";
        if (type == PotionEffectType.DAMAGE_RESISTANCE) return "Résistance";
        if (type == PotionEffectType.FIRE_RESISTANCE) return "Résistance au feu";
        if (type == PotionEffectType.WATER_BREATHING) return "Respiration aquatique";
        if (type == PotionEffectType.INVISIBILITY) return "Invisibilité";
        if (type == PotionEffectType.BLINDNESS) return "Cécité";
        if (type == PotionEffectType.NIGHT_VISION) return "Vision nocturne";
        if (type == PotionEffectType.HUNGER) return "Faim";
        if (type == PotionEffectType.WEAKNESS) return "Faiblesse";
        if (type == PotionEffectType.POISON) return "Poison";
        if (type == PotionEffectType.WITHER) return "Wither";
        if (type == PotionEffectType.HEALTH_BOOST) return "Boost de vie";
        if (type == PotionEffectType.ABSORPTION) return "Absorption";
        if (type == PotionEffectType.SATURATION) return "Saturation";
        return type.getName();
    }

    private String obtenirCouleurEffet(PotionEffectType type) {
        if (type == PotionEffectType.SPEED) return "§b";
        if (type == PotionEffectType.SLOW) return "§7";
        if (type == PotionEffectType.FAST_DIGGING) return "§e";
        if (type == PotionEffectType.SLOW_DIGGING) return "§8";
        if (type == PotionEffectType.INCREASE_DAMAGE) return "§c";
        if (type == PotionEffectType.HEAL) return "§d";
        if (type == PotionEffectType.HARM) return "§4";
        if (type == PotionEffectType.JUMP) return "§a";
        if (type == PotionEffectType.CONFUSION) return "§5";
        if (type == PotionEffectType.REGENERATION) return "§d";
        if (type == PotionEffectType.DAMAGE_RESISTANCE) return "§9";
        if (type == PotionEffectType.FIRE_RESISTANCE) return "§6";
        if (type == PotionEffectType.WATER_BREATHING) return "§3";
        if (type == PotionEffectType.INVISIBILITY) return "§7";
        if (type == PotionEffectType.BLINDNESS) return "§0";
        if (type == PotionEffectType.NIGHT_VISION) return "§1";
        if (type == PotionEffectType.HUNGER) return "§2";
        if (type == PotionEffectType.WEAKNESS) return "§8";
        if (type == PotionEffectType.POISON) return "§2";
        if (type == PotionEffectType.WITHER) return "§0";
        if (type == PotionEffectType.HEALTH_BOOST) return "§e";
        if (type == PotionEffectType.ABSORPTION) return "§6";
        if (type == PotionEffectType.SATURATION) return "§e";
        return "§f";
    }

    public void gererArriveeMap(Player joueur) {
        if (phaseActuelle != PhasePartie.JUMP || countdownEnCours) return;

        JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null) return;

        UUID joueurId = joueur.getUniqueId();
        if (joueursAyantTermineMapActuelle.contains(joueurId)) {
            return;
        }

        joueursAyantTermineMapActuelle.add(joueurId);

        joueurPartie.incrementerMapsCompletes();
        joueurPartie.ajouterPoints(config.getPointsParcoursReussi());

        joueur.sendMessage("§aMap " + (mapActuelle + 1) + " terminee ! +" + config.getPointsParcoursReussi() + " points");
        joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        if (!premierArrive) {
            premierArrive = true;
            countdownEnCours = true;
            joueur.sendMessage("§a§l» Félicitations vous gagnez les points bonus du premier arrivé !");

            for (Player autreJoueur : getJoueurs()) {
                if (!autreJoueur.equals(joueur)) {
                    autreJoueur.sendMessage("§e» " + joueurPartie.getNomAnonymise() + " est arrivé en premier en bas !");
                }
            }

            if (tachePartie != null) {
                tachePartie.cancel();
            }

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                countdownEnCours = false;
                if (mapActuelle + 1 >= config.getNombreMaps()) {
                    demarrerPhaseStuff();
                } else {
                    demarrerMap(mapActuelle + 1);
                }
            }, 100L);

            final int[] countdown = {5};
            BukkitTask tacheCountdown = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                @Override
                public void run() {
                    if (countdown[0] > 0) {
                        for (Player p : getJoueurs()) {
                            p.sendMessage("§6» Téléportation dans " + countdown[0] + " seconde" + (countdown[0] > 1 ? "s" : "") + "...");
                        }
                        countdown[0]--;
                    }
                }
            }, 20L, 20L);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                tacheCountdown.cancel();
            }, 120L);
        }
    }

    public void gererMortDansVide(Player joueur) {
        if (phaseActuelle != PhasePartie.JUMP) return;

        joueur.playSound(joueur.getLocation(), Sound.VILLAGER_HIT, 1.0f, 0.8f);

        annulerAttenteArrivee(joueur);

        World mondeActuel = mondesParcours.get(mapActuelle);
        if (mondeActuel == null) return;

        GestionnaireSetupGetDown.SetupMonde setupActuel = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(mondeActuel.getName());
        
        if (setupActuel == null) return;

        List<Location> spawnsDescente = setupActuel.getSpawnsDescente();
        if (!spawnsDescente.isEmpty() && spawnsDescente.get(0) != null) {
            respawnFluide(joueur, spawnsDescente.get(0));
        }
    }

    private void respawnFluide(Player joueur, Location locationRespawn) {
        joueur.setHealth(joueur.getMaxHealth());
        joueur.setFoodLevel(20);
        joueur.setSaturation(20.0f);
        joueur.teleport(locationRespawn);
        joueur.playSound(joueur.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        joueur.sendMessage("§eRetour au début de la map !");

        Location loc = joueur.getLocation();
        loc.getWorld().playEffect(loc.add(0, 1, 0), Effect.SMOKE, 0);
    }

    private void annulerAttenteArrivee(Player joueur) {
        UUID joueurId = joueur.getUniqueId();
        BukkitTask tacheAttente = joueursEnAttenteArrivee.remove(joueurId);
        if (tacheAttente != null) {
            tacheAttente.cancel();
        }
    }

    public void retirerJoueurDePartie(Player joueur) {
        if (!joueurs.containsKey(joueur.getUniqueId())) return;

        annulerAttenteArrivee(joueur);

        UUID joueurId = joueur.getUniqueId();
        joueursVivants.remove(joueur);
        joueurs.remove(joueurId);
        viesJoueurs.remove(joueurId);
        kingcoinsJoueurs.remove(joueurId);
        joueursAyantTermineMapActuelle.remove(joueurId);

        scoreboardManager.supprimerScoreboard(joueur);

        joueur.setDisplayName(joueur.getName());
        joueur.setPlayerListName(joueur.getName());
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

        for (Player autreJoueur : getJoueurs()) {
            autreJoueur.showPlayer(joueur);
            joueur.showPlayer(autreJoueur);
            
            org.bukkit.scoreboard.Scoreboard scoreboardAutre = autreJoueur.getScoreboard();
            if (scoreboardAutre != null) {
                String teamName = "player_" + joueur.getUniqueId().toString().substring(0, 8);
                org.bukkit.scoreboard.Team team = scoreboardAutre.getTeam(teamName);
                if (team != null) {
                    team.removeEntry(joueur.getName());
                    team.unregister();
                }
            }
        }
        
        org.bukkit.scoreboard.Scoreboard scoreboardJoueur = joueur.getScoreboard();
        if (scoreboardJoueur != null) {
            for (org.bukkit.scoreboard.Team team : scoreboardJoueur.getTeams()) {
                if (team.getName().startsWith("player_")) {
                    team.unregister();
                }
            }
        }
        joueur.setScoreboard(plugin.getServer().getScoreboardManager().getNewScoreboard());

        joueursVivants.remove(joueur);
        joueurs.remove(joueur.getUniqueId());
        viesJoueurs.remove(joueur.getUniqueId());
        kingcoinsJoueurs.remove(joueur.getUniqueId());
        joueursAyantTermineMapActuelle.remove(joueur.getUniqueId());

        BukkitTask tacheAttente = joueursEnAttenteArrivee.remove(joueur.getUniqueId());
        if (tacheAttente != null) {
            tacheAttente.cancel();
        }
        
        BukkitTask tacheProtection = tachesProtectionSpawn.remove(joueur.getUniqueId());
        if (tacheProtection != null) {
            tacheProtection.cancel();
        }
        joueursProtectionSpawn.remove(joueur.getUniqueId());

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
        
        plugin.getLogger().info("Joueur " + nomJoueur + " retiré de la partie GetDown " + idPartie);

        if (joueursVivants.size() <= 1 && etat == EtatPartie.EN_COURS) {
            terminerPartie();
        }
    }

    private void demarrerPhaseStuff() {
        if (phaseActuelle == PhasePartie.STUFF) return;

        if (tachePartie != null) {
            tachePartie.cancel();
        }

        phaseActuelle = PhasePartie.STUFF;
        tempsEcoule = 0;
        joueursFreeze = false;

        boolean tpOk = teleporterJoueursStuff();
        if (!tpOk) {
            for (Player joueur : getJoueurs()) {
                teleporterJoueurSecurise(joueur, mondePvp.getSpawnLocation());
            }
        }
        joueursFreeze = true;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player joueur : getJoueurs()) {
                joueur.sendMessage("");
                joueur.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                joueur.sendMessage("§e§l                   » PHASE STUFF «");
                joueur.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                joueur.sendMessage("");
                joueur.sendMessage("§a» Vous êtes sur la map PvP ! Utilisez vos KingCoins pour vous équiper !");
                joueur.sendMessage("§7» Temps: " + (config.getDureePhaseStuffSecondes() / 60) + "m" + (config.getDureePhaseStuffSecondes() % 60) + "s");
                joueur.sendMessage("");

                joueur.setWalkSpeed(0.0f);
                joueur.setFlySpeed(0.0f);
                joueur.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
                joueur.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));
                joueur.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, 255, false, false));
                
                if (joueur.isFlying() || joueur.getAllowFlight()) {
                    joueursProtectionFly.add(joueur.getUniqueId());
                    joueur.setAllowFlight(true);
                    joueur.setFlying(false);
                }

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (phaseActuelle == PhasePartie.STUFF && joueur.isOnline()) {
                        ouvrirShop(joueur);
                    }
                }, 1L);
            }
        }, 10L);

        tachePartie = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tachePartie != null) tachePartie.cancel();
                return;
            }
            
            tempsEcoule++;

            int tempsRestant = config.getDureePhaseStuffSecondes() - tempsEcoule;
            if (tempsRestant == 10) {
                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("");
                    joueur.sendMessage("§6§l⚠ ATTENTION ⚠");
                    joueur.sendMessage("§e» Phase PVP dans 10 secondes !");
                    joueur.sendMessage("§7» Finalisez vos achats rapidement !");
                    joueur.sendMessage("");
                    joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 0.5f);
                }
            }

            if (tempsEcoule >= config.getDureePhaseStuffSecondes()) {
                demarrerPhasePvp();
            }
        }, 20L, 20L);
    }

    private boolean teleporterJoueursStuff() {
        GestionnaireSetupGetDown.SetupMonde setupPvp = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapPvpOriginale);
        
        if (setupPvp == null) {
            plugin.getLogger().warning("Setup manquant pour la map PvP");
            return false;
        }

        Map<Integer, Location> spawnsPvpOriginaux = setupPvp.getSpawnsPvp();
        if (spawnsPvpOriginaux.isEmpty()) {
            plugin.getLogger().warning("Aucun spawn PvP configuré");
            return false;
        }

        List<Location> spawnsCopies = new ArrayList<>();
        for (Location locOrig : spawnsPvpOriginaux.values()) {
            if (locOrig != null) {
                spawnsCopies.add(new Location(
                    mondePvp,
                    locOrig.getX(),
                    locOrig.getY(),
                    locOrig.getZ(),
                    locOrig.getYaw(),
                    locOrig.getPitch()
                ));
            }
        }
        if (spawnsCopies.isEmpty()) {
            return false;
        }

        List<Player> joueursActifs = new ArrayList<>(getJoueurs());
        Collections.shuffle(joueursActifs);

        for (int i = 0; i < joueursActifs.size(); i++) {
            Player joueur = joueursActifs.get(i);
            Location spawn = spawnsCopies.get(i % spawnsCopies.size());
            teleporterJoueurSecurise(joueur, spawn);
        }
        return true;
    }

    public void ouvrirShop(Player joueur) {
        JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null) return;

        Inventory shop = plugin.getServer().createInventory(null, 54, "§6Shop - KingCoins: " + joueurPartie.getKingcoins());

        Map<String, List<ItemShop>> categories = genererCategoriesDynamiquesAvecItemShop();

        int colonne = 0;
        for (Map.Entry<String, List<ItemShop>> categorie : categories.entrySet()) {
            int ligne = 0;
            for (ItemShop itemShop : categorie.getValue()) {
                if (colonne >= 9 || ligne >= 6) break;
                
                Material material = itemShop.getMaterial();
                Integer prix = itemShop.getPrix();
                if (prix == null) continue;

                ItemStack item = new ItemStack(material);
                
                ItemMeta meta = item.getItemMeta();
                
                if (itemShop.getNomCustom() != null && !itemShop.getNomCustom().isEmpty()) {
                    meta.setDisplayName(itemShop.getNomCustom().replace("&", "§"));
                } else {
                    meta.setDisplayName("§e" + formatNomMaterial(material.name()));
                }

                List<String> lore = new ArrayList<>();
                
                if (itemShop.getLoreCustom() != null && !itemShop.getLoreCustom().isEmpty()) {
                    for (String ligneCustom : itemShop.getLoreCustom()) {
                        lore.add(ligneCustom.replace("&", "§"));
                    }
                    lore.add("");
                }

                if (itemShop.getEnchantements() != null && !itemShop.getEnchantements().isEmpty()) {
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : itemShop.getEnchantements().entrySet()) {
                        String nomEnchantement = traduireEnchantement(entry.getKey());
                        lore.add("§7" + nomEnchantement + " " + entry.getValue());
                    }
                    lore.add("");
                }
                
                lore.add("§7Catégorie: §b" + categorie.getKey());
                lore.add("§7Prix: §6" + prix + " KingCoins");

                if (joueurPartie.getKingcoins() >= prix) {
                    lore.add("§aCliquez pour acheter !");
                } else {
                    lore.add("§cPas assez de KingCoins !");
                }

                meta.setLore(lore);

                if (itemShop.getEnchantements() != null && !itemShop.getEnchantements().isEmpty()) {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                
                item.setItemMeta(meta);

                if (itemShop.getDurability() != 0) {
                    item.setDurability(itemShop.getDurability());
                }
                
                if (itemShop.getEnchantements() != null && !itemShop.getEnchantements().isEmpty()) {
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : itemShop.getEnchantements().entrySet()) {
                        item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    }
                }

                int slot = ligne * 9 + colonne;
                shop.setItem(slot, item);
                ligne++;
            }
            colonne++;
        }

        joueur.openInventory(shop);
    }

    private Map<String, List<ItemShop>> genererCategoriesDynamiquesAvecItemShop() {
        Map<String, List<ItemShop>> categories = new LinkedHashMap<>();
        
        List<ItemShop> epees = new ArrayList<>();
        ajouterItemShopSiDisponible(epees, Material.WOOD_SWORD);
        ajouterItemShopSiDisponible(epees, Material.STONE_SWORD);
        ajouterItemShopSiDisponible(epees, Material.IRON_SWORD);
        ajouterItemShopSiDisponible(epees, Material.GOLD_SWORD);
        ajouterItemShopSiDisponible(epees, Material.DIAMOND_SWORD);
        if (!epees.isEmpty()) categories.put("Épées", epees);

        List<ItemShop> arcFleches = new ArrayList<>();
        ajouterItemShopSiDisponible(arcFleches, Material.BOW);
        ajouterItemShopSiDisponible(arcFleches, Material.ARROW);
        if (!arcFleches.isEmpty()) categories.put("Arc & Flèches", arcFleches);

        List<ItemShop> armuresDiamant = new ArrayList<>();
        ajouterItemShopSiDisponible(armuresDiamant, Material.DIAMOND_HELMET);
        ajouterItemShopSiDisponible(armuresDiamant, Material.DIAMOND_CHESTPLATE);
        ajouterItemShopSiDisponible(armuresDiamant, Material.DIAMOND_LEGGINGS);
        ajouterItemShopSiDisponible(armuresDiamant, Material.DIAMOND_BOOTS);
        if (!armuresDiamant.isEmpty()) categories.put("Armures Diamant", armuresDiamant);

        List<ItemShop> armuresOr = new ArrayList<>();
        ajouterItemShopSiDisponible(armuresOr, Material.GOLD_HELMET);
        ajouterItemShopSiDisponible(armuresOr, Material.GOLD_CHESTPLATE);
        ajouterItemShopSiDisponible(armuresOr, Material.GOLD_LEGGINGS);
        ajouterItemShopSiDisponible(armuresOr, Material.GOLD_BOOTS);
        if (!armuresOr.isEmpty()) categories.put("Armures Titane", armuresOr);

        List<ItemShop> armuresFer = new ArrayList<>();
        ajouterItemShopSiDisponible(armuresFer, Material.IRON_HELMET);
        ajouterItemShopSiDisponible(armuresFer, Material.IRON_CHESTPLATE);
        ajouterItemShopSiDisponible(armuresFer, Material.IRON_LEGGINGS);
        ajouterItemShopSiDisponible(armuresFer, Material.IRON_BOOTS);
        if (!armuresFer.isEmpty()) categories.put("Armures Saphir", armuresFer);

        List<ItemShop> armuresMailles = new ArrayList<>();
        ajouterItemShopSiDisponible(armuresMailles, Material.CHAINMAIL_HELMET);
        ajouterItemShopSiDisponible(armuresMailles, Material.CHAINMAIL_CHESTPLATE);
        ajouterItemShopSiDisponible(armuresMailles, Material.CHAINMAIL_LEGGINGS);
        ajouterItemShopSiDisponible(armuresMailles, Material.CHAINMAIL_BOOTS);
        if (!armuresMailles.isEmpty()) categories.put("Armures Onyx", armuresMailles);

        List<ItemShop> armuresCuir = new ArrayList<>();
        ajouterItemShopSiDisponible(armuresCuir, Material.LEATHER_HELMET);
        ajouterItemShopSiDisponible(armuresCuir, Material.LEATHER_CHESTPLATE);
        ajouterItemShopSiDisponible(armuresCuir, Material.LEATHER_LEGGINGS);
        ajouterItemShopSiDisponible(armuresCuir, Material.LEATHER_BOOTS);
        if (!armuresCuir.isEmpty()) categories.put("Armures Cuir", armuresCuir);

        List<ItemShop> nourriture = new ArrayList<>();
        ajouterItemShopSiDisponible(nourriture, Material.BREAD);
        ajouterItemShopSiDisponible(nourriture, Material.COOKED_BEEF);
        ajouterItemShopSiDisponible(nourriture, Material.GRILLED_PORK);
        ajouterItemShopSiDisponible(nourriture, Material.COOKED_CHICKEN);
        ajouterItemShopSiDisponible(nourriture, Material.GOLDEN_APPLE);
        ajouterItemShopSiDisponible(nourriture, Material.GOLDEN_CARROT);
        if (!nourriture.isEmpty()) categories.put("Nourriture", nourriture);

        List<ItemShop> objetsSpeciaux = new ArrayList<>();
        ajouterItemShopSiDisponible(objetsSpeciaux, Material.ENDER_PEARL);
        if (!objetsSpeciaux.isEmpty()) categories.put("Objets Spéciaux", objetsSpeciaux);
        
        Map<String, List<ItemShop>> potionsParCategorie = new HashMap<>();
        for (ItemShop itemShop : config.getItemsShopListe()) {
            if (itemShop.getMaterial() == Material.POTION) {
                String categorie = itemShop.getCategorie();
                if (categorie == null || categorie.isEmpty()) {
                    categorie = "Autres";
                }
                potionsParCategorie.computeIfAbsent(categorie, k -> new ArrayList<>()).add(itemShop);
            }
        }
        
        for (Map.Entry<String, List<ItemShop>> entry : potionsParCategorie.entrySet()) {
            String categorie = entry.getKey();
            String nomCategorie;
            if (categorie != null && !categorie.isEmpty()) {
                nomCategorie = "Potions " + categorie.substring(0, 1).toUpperCase() + 
                              (categorie.length() > 1 ? categorie.substring(1) : "");
            } else {
                nomCategorie = "Potions";
            }
            if (!entry.getValue().isEmpty()) {
                categories.put(nomCategorie, entry.getValue());
            }
        }
        
        return categories;
    }

    private void ajouterItemShopSiDisponible(List<ItemShop> liste, Material material) {
        ItemShop itemShop = config.getItemShop(material);
        if (itemShop != null && config.getPrixShop().containsKey(material)) {
            liste.add(itemShop);
        }
    }

    private String formatNomMaterial(String nom) {
        Map<String, String> traductions = new HashMap<>();
        traductions.put("LEATHER_HELMET", "Casque en Cuir");
        traductions.put("LEATHER_CHESTPLATE", "Plastron en Cuir");
        traductions.put("LEATHER_LEGGINGS", "Jambières en Cuir");
        traductions.put("LEATHER_BOOTS", "Bottes en Cuir");
        traductions.put("CHAINMAIL_HELMET", "Casque en Mailles");
        traductions.put("CHAINMAIL_CHESTPLATE", "Plastron en Mailles");
        traductions.put("CHAINMAIL_LEGGINGS", "Jambières en Mailles");
        traductions.put("CHAINMAIL_BOOTS", "Bottes en Mailles");
        traductions.put("IRON_HELMET", "Casque en Fer");
        traductions.put("IRON_CHESTPLATE", "Plastron en Fer");
        traductions.put("IRON_LEGGINGS", "Jambières en Fer");
        traductions.put("IRON_BOOTS", "Bottes en Fer");
        traductions.put("DIAMOND_HELMET", "Casque en Diamant");
        traductions.put("DIAMOND_CHESTPLATE", "Plastron en Diamant");
        traductions.put("DIAMOND_LEGGINGS", "Jambières en Diamant");
        traductions.put("DIAMOND_BOOTS", "Bottes en Diamant");
        traductions.put("GOLD_HELMET", "Casque en Or");
        traductions.put("GOLD_CHESTPLATE", "Plastron en Or");
        traductions.put("GOLD_LEGGINGS", "Jambières en Or");
        traductions.put("GOLD_BOOTS", "Bottes en Or");
        traductions.put("WOOD_SWORD", "Épée en Bois");
        traductions.put("STONE_SWORD", "Épée en Pierre");
        traductions.put("IRON_SWORD", "Épée en Fer");
        traductions.put("GOLD_SWORD", "Épée en Or");
        traductions.put("DIAMOND_SWORD", "Épée en Diamant");
        traductions.put("WOOD_AXE", "Hache en Bois");
        traductions.put("STONE_AXE", "Hache en Pierre");
        traductions.put("IRON_AXE", "Hache en Fer");
        traductions.put("DIAMOND_AXE", "Hache en Diamant");
        traductions.put("BOW", "Arc");
        traductions.put("ARROW", "Flèche");
        traductions.put("BREAD", "Pain");
        traductions.put("COOKED_BEEF", "Steak");
        traductions.put("GRILLED_PORK", "Côtelette de Porc");
        traductions.put("COOKED_CHICKEN", "Poulet Cuit");
        traductions.put("GOLDEN_APPLE", "Pomme Dorée");
        traductions.put("GOLDEN_CARROT", "Carotte Dorée");
        traductions.put("WOOD_PICKAXE", "Pioche en Bois");
        traductions.put("STONE_PICKAXE", "Pioche en Pierre");
        traductions.put("IRON_PICKAXE", "Pioche en Fer");
        traductions.put("DIAMOND_PICKAXE", "Pioche en Diamant");
        traductions.put("WOOD_SPADE", "Pelle en Bois");
        traductions.put("STONE_SPADE", "Pelle en Pierre");
        traductions.put("IRON_SPADE", "Pelle en Fer");
        traductions.put("DIAMOND_SPADE", "Pelle en Diamant");
        traductions.put("COBBLESTONE", "Pierre Taillée");
        traductions.put("WOOD", "Bois");
        traductions.put("STONE", "Pierre");
        traductions.put("DIRT", "Terre");
        traductions.put("SAND", "Sable");
        traductions.put("GRAVEL", "Gravier");
        traductions.put("GLASS", "Verre");
        traductions.put("ENDER_PEARL", "Perle de l'Ender");
        traductions.put("POTION", "Potion");
        
        return traductions.getOrDefault(nom, nom);
    }

    private String traduireEnchantement(org.bukkit.enchantments.Enchantment enchantement) {
        String nom = enchantement.getName();
        
        switch (nom) {
            case "DAMAGE_ALL":
            case "SHARPNESS":
                return "Tranchant";
            case "PROTECTION_ENVIRONMENTAL":
            case "PROTECTION":
                return "Protection";
            case "DURABILITY":
            case "UNBREAKING":
                return "Solidité";
            case "FIRE_ASPECT":
                return "Aura de feu";
            case "KNOCKBACK":
                return "Recul";
            case "ARROW_DAMAGE":
            case "POWER":
                return "Puissance";
            case "ARROW_FIRE":
            case "FLAME":
                return "Flamme";
            case "ARROW_INFINITE":
            case "INFINITY":
                return "Infinité";
            case "ARROW_KNOCKBACK":
            case "PUNCH":
                return "Frappe";
            case "THORNS":
                return "Épines";
            case "PROTECTION_FIRE":
            case "FIRE_PROTECTION":
                return "Protection contre le feu";
            case "PROTECTION_EXPLOSIONS":
            case "BLAST_PROTECTION":
                return "Protection contre les explosions";
            case "PROTECTION_PROJECTILE":
            case "PROJECTILE_PROTECTION":
                return "Protection contre les projectiles";
            case "OXYGEN":
            case "RESPIRATION":
                return "Respiration";
            case "WATER_WORKER":
            case "AQUA_AFFINITY":
                return "Affinité aquatique";
            case "DIG_SPEED":
            case "EFFICIENCY":
                return "Efficacité";
            case "SILK_TOUCH":
                return "Toucher de soie";
            case "LOOT_BONUS_BLOCKS":
            case "FORTUNE":
                return "Fortune";
            case "LOOT_BONUS_MOBS":
            case "LOOTING":
                return "Butin";
            default:
                return nom;
        }
    }

    public void gererAchatShop(Player joueur, ItemStack item) {
        JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null || phaseActuelle != PhasePartie.STUFF) return;

        joueur.setMetadata("achat_en_cours", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        ItemShop itemShop = null;
        Integer prix = null;
        
        if (item.getType() == Material.POTION) {
            for (ItemShop shopItem : config.getItemsShopListe()) {
                if (shopItem.getMaterial() == Material.POTION && shopItem.getDurability() == item.getDurability()) {
                    itemShop = shopItem;
                    prix = shopItem.getPrix();
                    break;
                }
            }
        } else {
            prix = config.getPrixShop().get(item.getType());
            itemShop = config.getItemShop(item.getType());
        }
        
        if (prix != null) {
            if (joueurPartie.getKingcoins() >= prix) {
                joueurPartie.retirerKingcoins(prix);
                
                ItemStack nouvelItem = new ItemStack(item.getType());
                
                if (itemShop != null) {
                    ItemMeta meta = nouvelItem.getItemMeta();
                    if (meta != null) {
                        if (itemShop.getNomCustom() != null && !itemShop.getNomCustom().isEmpty()) {
                            meta.setDisplayName(itemShop.getNomCustom().replace("&", "§"));
                        }
                        
                        if (itemShop.getLoreCustom() != null && !itemShop.getLoreCustom().isEmpty()) {
                            List<String> loreFormate = new ArrayList<>();
                            for (String ligne : itemShop.getLoreCustom()) {
                                loreFormate.add(ligne.replace("&", "§"));
                            }
                            meta.setLore(loreFormate);
                        }

                        if (itemShop.getEnchantements() != null && !itemShop.getEnchantements().isEmpty()) {
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                        }
                        
                        nouvelItem.setItemMeta(meta);
                    }

                    if (itemShop.getDurability() != 0) {
                        nouvelItem.setDurability(itemShop.getDurability());
                    }
                    
                    if (itemShop.getEnchantements() != null && !itemShop.getEnchantements().isEmpty()) {
                        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : itemShop.getEnchantements().entrySet()) {
                            nouvelItem.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                        }
                    }
                }
                
                marquerItemAchete(nouvelItem, prix);

                if (estNourriture(item.getType())) {
                    ajouterItemAvecStacking(joueur, nouvelItem);
                } else {
                    joueur.getInventory().addItem(nouvelItem);
                }
                
                String nomAffiche = (itemShop != null && itemShop.getNomCustom() != null && !itemShop.getNomCustom().isEmpty()) 
                    ? itemShop.getNomCustom().replace("&", "§") 
                    : formatNomMaterial(item.getType().name());
                joueur.sendMessage("§a» Achat: " + nomAffiche + " (-" + prix + " KingCoins)");
                joueur.playSound(joueur.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
                
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (joueur.isOnline() && phaseActuelle == PhasePartie.STUFF) {
                        ouvrirShop(joueur);
                    }
                }, 1L);
            } else {
                joueur.sendMessage("§c» Vous n'avez pas assez de KingCoins ! (Requis: " + prix + ", Vous avez: " + joueurPartie.getKingcoins() + ")");
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (joueur.isOnline() && phaseActuelle == PhasePartie.STUFF) {
                        ouvrirShop(joueur);
                    }
                }, 1L);
            }
        }
    }

    public boolean peutEncoreAcheter(Player joueur) {
        JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null) return false;

        int kingcoins = joueurPartie.getKingcoins();

        for (int prix : config.getPrixShop().values()) {
            if (kingcoins >= prix) {
                return true;
            }
        }
        
        return false;
    }

    private boolean estNourriture(Material material) {
        return material.isEdible();
    }

    private void marquerItemAchete(ItemStack item, int prix) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = plugin.getServer().getItemFactory().getItemMeta(item.getType());
        }
        
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§7§oAcheté pour " + prix + " KingCoins");
        lore.add("§7§oClic droit pour revendre");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private boolean estItemAchete(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String ligne : lore) {
            if (ligne.contains("§7§oAcheté pour") && ligne.contains("KingCoins")) {
                return true;
            }
        }
        return false;
    }

    private int obtenirPrixItemAchete(ItemStack item) {
        if (!estItemAchete(item)) {
            return 0;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String ligne : lore) {
            if (ligne.contains("§7§oAcheté pour") && ligne.contains("KingCoins")) {
                String ligneNettoyee = ChatColor.stripColor(ligne);
                String[] parts = ligneNettoyee.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i + 1].equals("KingCoins")) {
                        try {
                            return Integer.parseInt(parts[i]);
                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }
                }
            }
        }
        return 0;
    }

    public void gererReventeItem(Player joueur, ItemStack item, int slot) {
        JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null || phaseActuelle != PhasePartie.STUFF) return;

        if (!estItemAchete(item)) {
            return;
        }

        int prix = obtenirPrixItemAchete(item);
        if (prix <= 0) {
            return;
        }

        joueur.setMetadata("vente_en_cours", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        String nomItem;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            nomItem = item.getItemMeta().getDisplayName();
        } else {
            ItemShop itemShop = config.getItemShop(item.getType());
            if (itemShop != null && itemShop.getNomCustom() != null && !itemShop.getNomCustom().isEmpty()) {
                nomItem = itemShop.getNomCustom().replace("&", "§");
            } else {
                nomItem = formatNomMaterial(item.getType().name());
            }
        }
        
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            joueur.getInventory().setItem(slot, item);
        } else {
            joueur.getInventory().setItem(slot, null);
        }
        
        joueurPartie.ajouterKingcoins(prix);
        
        joueur.sendMessage("§a» Revente: " + nomItem + " (+" + prix + " KingCoins)");
        joueur.playSound(joueur.getLocation(), Sound.ORB_PICKUP, 1.0f, 0.8f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (joueur.isOnline() && phaseActuelle == PhasePartie.STUFF) {
                ouvrirShop(joueur);
            }
        }, 1L);
    }



    private void demarrerPhasePvp() {
        if (phaseActuelle == PhasePartie.PVP) return;

        if (tachePartie != null) {
            tachePartie.cancel();
        }

        phaseActuelle = PhasePartie.PVP;
        tempsEcoule = 0;
        joueursFreeze = false;

        for (BukkitTask tache : joueursEnAttenteArrivee.values()) {
            if (tache != null) tache.cancel();
        }
        joueursEnAttenteArrivee.clear();

        demarrerInvincibiliteTemporaire();
        teleporterJoueursPvp();

        for (Player joueur : getJoueurs()) {
            joueur.closeInventory();

            if (joueur.hasMetadata("achat_en_cours")) {
                joueur.removeMetadata("achat_en_cours", plugin);
            }

            joueur.setWalkSpeed(0.2f);
            joueur.setFlySpeed(0.1f);
            joueur.removePotionEffect(PotionEffectType.SLOW);
            joueur.removePotionEffect(PotionEffectType.JUMP);
            joueur.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            
            if (joueursProtectionFly.contains(joueur.getUniqueId())) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    joueursProtectionFly.remove(joueur.getUniqueId());
                    if (joueur.isOnline() && joueur.getGameMode() == GameMode.SURVIVAL) {
                        joueur.setAllowFlight(false);
                        joueur.setFlying(false);
                    }
                }, 40L);
            }
            
            joueur.sendMessage("");
            joueur.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§c§l                    » PHASE PVP «");
            joueur.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§e» Vous avez " + config.getViesPvp() + " vies !");
            joueur.sendMessage("§7» Kill = Régénération II pendant " + config.getDureeRegenerationKillSecondes() + "s");
            joueur.sendMessage("§c» Invincibilité: 10 secondes");
            joueur.sendMessage("");
        }

        tachePartie = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tachePartie != null) tachePartie.cancel();
                return;
            }
            
            tempsEcoule++;

            if (tempsEcoule == config.getTempsAvantBordureMinutes() * 60) {
                demarrerBordure();
            }

            if (tempsEcoule >= config.getDureePhasePvpMinutes() * 60) {
                terminerPartie();
                return;
            }

            if (joueursVivants.size() <= 1) {
                terminerPartie();
                return;
            }

            if (etat == EtatPartie.TERMINEE) {
                return;
            }
        }, 20L, 20L);
    }

    private void teleporterJoueursPvp() {
        GestionnaireSetupGetDown.SetupMonde setupPvp = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapPvpOriginale);
        
        if (setupPvp == null) {
            plugin.getLogger().warning("Setup manquant pour la map PvP");
            return;
        }

        Map<Integer, Location> spawnsPvpOriginaux = setupPvp.getSpawnsPvp();
        if (spawnsPvpOriginaux.isEmpty()) {
            plugin.getLogger().warning("Aucun spawn PvP configuré");
            return;
        }

        List<Player> joueursActifs = new ArrayList<>(getJoueurs());
        Collections.shuffle(joueursActifs);
        
        List<Integer> spawnsDisponibles = new ArrayList<>(spawnsPvpOriginaux.keySet());
        Collections.shuffle(spawnsDisponibles);
        
        plugin.getLogger().info("Téléportation de " + joueursActifs.size() + " joueurs vers " + spawnsPvpOriginaux.size() + " spawns PvP");
        plugin.getLogger().info("Spawns disponibles: " + spawnsDisponibles);

        for (int i = 0; i < joueursActifs.size(); i++) {
            Player joueur = joueursActifs.get(i);
            int spawnIndex = spawnsDisponibles.get(i % spawnsDisponibles.size());
            Location spawnOriginal = spawnsPvpOriginaux.get(spawnIndex);
            
            if (spawnOriginal == null) {
                plugin.getLogger().warning("Spawn PvP " + spawnIndex + " est null pour la map " + nomMapPvpOriginale);
                joueur.sendMessage("§cErreur: Spawn PvP " + spawnIndex + " non configuré !");
                continue;
            }

            Location spawn = new Location(
                mondePvp,
                spawnOriginal.getX(),
                spawnOriginal.getY(),
                spawnOriginal.getZ(),
                spawnOriginal.getYaw(),
                spawnOriginal.getPitch()
            );
            
            plugin.getLogger().info("Téléportation du joueur " + joueur.getName() + " vers le spawn PvP " + spawnIndex + 
                                  " aux coordonnées " + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ());

            joueur.teleport(spawn);
            joueur.sendMessage("§a» Téléportation vers la zone PvP réussie ! (Spawn " + spawnIndex + ")");
        }
    }

    private void demarrerBordure() {
        GestionnaireSetupGetDown.SetupMonde setupPvp = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(mondePvp.getName());
        
        if (setupPvp == null) {
            plugin.getLogger().warning("Setup manquant pour la map PvP");
            return;
        }

        Location centre = setupPvp.getCentreBordure();
        if (centre == null) {
            plugin.getLogger().warning("Centre de bordure non configuré pour la map PvP");
            return;
        }

        WorldBorder bordure = mondePvp.getWorldBorder();
        bordure.setCenter(centre);
        bordure.setSize(setupPvp.getRayonBordureInitial() * 2);

        int dureeReduction = (config.getDureePhasePvpMinutes() - config.getTempsAvantBordureMinutes()) * 60;
        bordure.setSize(20, dureeReduction);

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("§cLa bordure commence a se reduire !");
        }

        tacheBordure = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tacheBordure != null) tacheBordure.cancel();
                return;
            }
            
            for (Player joueur : getJoueurs()) {
                if (joueur.getWorld().equals(centre.getWorld())) {
                    double distance = joueur.getLocation().distance(centre);
                    double rayonBordure = bordure.getSize() / 2.0;

                    if (distance > rayonBordure) {
                        joueur.damage(config.getDegatsBordureParSeconde());
                        joueur.sendMessage("§cVous etes dans la zone dangereuse !");
                    }
                }
            }
        }, 20L, 20L);
    }

    public void eliminerJoueur(Player joueur, Player tueur) {
        eliminerJoueur(joueur, tueur, null);
    }
    
    public void eliminerJoueur(Player joueur, Player tueur, Location positionMort) {
        eliminerJoueur(joueur, tueur, positionMort, null, null);
    }
    
    public void eliminerJoueur(Player joueur, Player tueur, Location positionMort, ItemStack[] inventaireSauvegarde, ItemStack[] armureSauvegarde) {
        if (!joueursVivants.contains(joueur)) return;

        int viesRestantes = viesJoueurs.get(joueur.getUniqueId()) - 1;
        viesJoueurs.put(joueur.getUniqueId(), viesRestantes);

        if (viesRestantes <= 0) {
            joueursVivants.remove(joueur);
            
            Location mortLocation = positionMort != null ? positionMort : joueur.getLocation();
            
            mortLocation.getWorld().strikeLightningEffect(mortLocation);
            for (Player p : getJoueurs()) {
                p.playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1.0f, 1.0f);
            }

            creerEnderChestAvecItems(joueur, mortLocation, inventaireSauvegarde, armureSauvegarde);
            joueur.getInventory().clear();
            joueur.getInventory().setArmorContents(new ItemStack[4]);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                mettreJoueurEnSpectateur(joueur);
            }, 20L);
            
            JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
            if (tueur != null && joueursVivants.contains(tueur)) {
                JoueurPartieGetDown tueurPartie = joueurs.get(tueur.getUniqueId());
                for (Player p : getJoueurs()) {
                    p.sendMessage("§c" + joueurPartie.getNomAnonymise() + " §7a été éliminé par §c" + tueurPartie.getNomAnonymise());
                }
            } else {
                for (Player p : getJoueurs()) {
                    p.sendMessage("§c" + joueurPartie.getNomAnonymise() + " §7a été éliminé");
                }
            }
            joueur.sendMessage("§c» Vous avez été éliminé ! Plus de vies restantes.");
        } else {
            joueur.sendMessage("§e» Vous avez été tué ! Il vous reste " + viesRestantes + " vie(s).");
        }

        if (joueursVivants.size() <= 1 && phaseActuelle == PhasePartie.PVP && etat == EtatPartie.EN_COURS) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                terminerPartie();
            }, 40L);
        }

        if (tueur != null && joueursVivants.contains(tueur)) {
            JoueurPartieGetDown tueurPartie = joueurs.get(tueur.getUniqueId());
            if (tueurPartie != null) {
                tueurPartie.ajouterKill();
                tueurPartie.ajouterPoints(config.getPointsKill());
                tueur.sendMessage("§aKill ! +" + config.getPointsKill() + " points");

                tueur.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    config.getRegenerationDureeSecondes() * 20,
                    config.getRegenerationNiveau() - 1
                ));
            }
        }
    }

    private void terminerPartie() {
        if (etat == EtatPartie.TERMINEE) return;
        
        etat = EtatPartie.TERMINEE;

        for (Player joueur : getJoueurs()) {
            gestionnaire.getGestionnairePrincipal().getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
        }

        if (tachePartie != null) {
            tachePartie.cancel();
            tachePartie = null;
        }

        if (tacheBordure != null) {
            tacheBordure.cancel();
            tacheBordure = null;
        }

        for (BukkitTask tache : joueursEnAttenteArrivee.values()) {
            if (tache != null) {
                tache.cancel();
            }
        }
        joueursEnAttenteArrivee.clear();
        joueursProtectionFly.clear();
        
        for (BukkitTask tache : tachesProtectionSpawn.values()) {
            if (tache != null) {
                tache.cancel();
            }
        }
        tachesProtectionSpawn.clear();
        joueursProtectionSpawn.clear();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            enderchestsItems.clear();
            joueursEnderChestOuvert.clear();
        }, 600L);

        if (!resultatsAffiches) {
            attribuerPointsFinaux();
            afficherResultats();
            sauvegarderStatistiques();
            resultatsAffiches = true;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, this::arreter, 400L);
    }

    private void attribuerPointsFinaux() {
        for (Player joueur : getJoueurs()) {
            JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
            if (joueurPartie != null) {
                int pointsKingcoins = joueurPartie.getKingcoins() / 100 * config.getPointsKingcoins100();
                joueurPartie.ajouterPoints(pointsKingcoins);
            }
        }

        List<Player> classement = new ArrayList<>(joueursVivants);
        classement.sort((a, b) -> {
            JoueurPartieGetDown joueurA = joueurs.get(a.getUniqueId());
            JoueurPartieGetDown joueurB = joueurs.get(b.getUniqueId());
            if (joueurA == null && joueurB == null) return 0;
            if (joueurA == null) return 1;
            if (joueurB == null) return -1;
            return Integer.compare(joueurB.getPoints(), joueurA.getPoints());
        });

        for (int i = 0; i < classement.size(); i++) {
            Player joueur = classement.get(i);
            JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
            
            if (joueurPartie == null) continue;

            int pointsPosition = 0;
            if (i == 0) pointsPosition = config.getPointsTop1();
            else if (i == 1) pointsPosition = config.getPointsTop2();
            else if (i <= 2) pointsPosition = config.getPointsTop3();
            else if (i <= 4) pointsPosition = config.getPointsTop5();
            else if (i <= 7) pointsPosition = config.getPointsTop8();
            else if (i <= 11) pointsPosition = config.getPointsTop12();

            if (pointsPosition > 0) {
                joueurPartie.ajouterPoints(pointsPosition);
            }
        }
    }

    private void afficherResultats() {
        List<Player> classement = new ArrayList<>(Arrays.asList(getJoueurs().toArray(new Player[0])));
        classement.sort((a, b) -> {
            JoueurPartieGetDown joueurA = joueurs.get(a.getUniqueId());
            JoueurPartieGetDown joueurB = joueurs.get(b.getUniqueId());
            return Integer.compare(joueurB.getPoints(), joueurA.getPoints());
        });

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§d§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§d§l                » PARTIE TERMINEE «");
            joueur.sendMessage("§d§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");

            joueur.sendMessage("§e» Classement de la partie:");
            for (int i = 0; i < Math.min(5, classement.size()); i++) {
                Player joueurClasse = classement.get(i);
                JoueurPartieGetDown joueurPartie = joueurs.get(joueurClasse.getUniqueId());

                String couleur = i == 0 ? "§6" : i == 1 ? "§e" : i == 2 ? "§7" : "§f";
                String medaille = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "§8▪";
                joueur.sendMessage(couleur + medaille + " #" + (i + 1) + " " + joueurPartie.getNomAnonymise() +
                                 " §f- " + joueurPartie.getPoints() + " points");
            }

            joueur.sendMessage("");
            JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
            if (joueurPartie != null) {
                joueur.sendMessage("§a» Vos statistiques:");
                joueur.sendMessage("§f  • Points: §6" + joueurPartie.getPoints());
                joueur.sendMessage("§f  • Kills: §c" + joueurPartie.getKills());
                joueur.sendMessage("§f  • Maps completees: §d" + joueurPartie.getMapsCompletes());
            }
            
            joueur.sendMessage("");
            joueur.sendMessage("§7» Vous serez teleporte au lobby dans 10 secondes...");
            joueur.sendMessage("");
        }
    }

    private void sauvegarderStatistiques() {
        for (Player joueur : getJoueurs()) {
            JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
            gestionnaire.getGestionnairePrincipal().getGestionnaireClassements()
                    .ajouterPoints(joueur, fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, joueurPartie.getPoints());
        }
    }

    private void creerScoreboards() {
        for (Player joueur : getJoueurs()) {
            scoreboardManager.creerScoreboard(joueur);
        }
    }

    private void demarrerMiseAJourScoreboards() {
        tacheScoreboard = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            scoreboardManager.mettreAJourTousLesScoreboards();
        }, 0L, 20L);
    }

    public void arreter() {
        if (tachePartie != null) tachePartie.cancel();
        if (tacheBordure != null) tacheBordure.cancel();
        if (tacheScoreboard != null) tacheScoreboard.cancel();

        for (BukkitTask tache : joueursEnAttenteArrivee.values()) {
            if (tache != null) tache.cancel();
        }
        joueursEnAttenteArrivee.clear();

        List<Player> joueursActuels = new ArrayList<>(getJoueurs());
        
        for (Player joueur : joueursActuels) {
            scoreboardManager.supprimerScoreboard(joueur);
            joueur.closeInventory();

            if (joueur.hasMetadata("message_menu_envoye")) {
                joueur.removeMetadata("message_menu_envoye", plugin);
            }

            joueur.setWalkSpeed(0.2f);
            joueur.setFlySpeed(0.1f);
            joueur.removePotionEffect(PotionEffectType.SLOW);
            joueur.removePotionEffect(PotionEffectType.JUMP);
            joueur.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            
            joueur.setDisplayName(joueur.getName());
            joueur.setPlayerListName(joueur.getName());

            if (!messageTeleportationEnvoye) {
                joueur.sendMessage("");
                joueur.sendMessage("§a§l» Téléportation vers le lobby dans quelques secondes...");
                joueur.sendMessage("");
            }
        }
        
        messageTeleportationEnvoye = true;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player joueur : joueursActuels) {
                if (joueur.isOnline()) {
                    gestionnaire.getGestionnairePrincipal().getGestionnaireLobby().teleporterVersLobby(joueur);
                }
            }
        }, 60L);

        blocsConsommes.clear();

        for (int i = 0; i < 3; i++) {
            World mondeParcours = mondesParcours.get(i);
            if (mondeParcours != null) {
                gestionnaire.getGestionnairePrincipal().getGestionnaireMaps()
                        .supprimerMapTemporaireParNom(mondeParcours.getName());
                
                String nomMapOriginale = nomsMapsOriginales.get(i);
                if (nomMapOriginale != null && !mondeParcours.getName().equals(nomMapOriginale)) {
                    gestionnaire.getGestionnaireSetup().supprimerSetupTemporaire(mondeParcours.getName());
                }
            }
        }
        if (mondePvp != null && !mondePvp.equals(mondesParcours.get(2))) {
            gestionnaire.getGestionnairePrincipal().getGestionnaireMaps()
                    .supprimerMapTemporaireParNom(mondePvp.getName());
            
            if (nomMapPvpOriginale != null && !mondePvp.getName().equals(nomMapPvpOriginale)) {
                gestionnaire.getGestionnaireSetup().supprimerSetupTemporaire(mondePvp.getName());
            }
        }

        gestionnaire.terminerPartie(idPartie);
    }

    public List<Player> getJoueurs() {
        List<Player> listeJoueurs = new ArrayList<>();
        for (UUID uuid : joueurs.keySet()) {
            Player joueur = plugin.getServer().getPlayer(uuid);
            if (joueur != null) {
                listeJoueurs.add(joueur);
            }
        }
        return listeJoueurs;
    }

    public EtatPartie getEtat() { return etat; }
    public PhasePartie getPhaseActuelle() { return phaseActuelle; }
    public String getIdPartie() { return idPartie; }
    public boolean sontJoueursFreeze() { return joueursFreeze; }
    public World getMonde() {
        if (phaseActuelle == PhasePartie.JUMP) {
            return mondesParcours.get(mapActuelle);
        } else if (phaseActuelle == PhasePartie.STUFF) {
            return mondeStuff;
        } else if (phaseActuelle == PhasePartie.PVP) {
            return mondePvp;
        }
        return mondesParcours.get(0);
    }
    public int getTempsEcoule() { return tempsEcoule; }
    public JoueurPartieGetDown getJoueurPartie(UUID uuid) { return joueurs.get(uuid); }
    public JoueurPartieGetDown getJoueur(UUID uuid) { return joueurs.get(uuid); }
    public GestionnaireGetDown getGestionnaire() { return gestionnaire; }
    public int getViesJoueur(UUID uuid) { return viesJoueurs.getOrDefault(uuid, 0); }
    public ConfigurationGetDown getConfig() { return config; }
    public int getNombreJoueursVivants() { return joueursVivants.size(); }
    public int getMapActuelle() { return mapActuelle; }
    public boolean estJoueurProtegeFly(Player joueur) { return joueursProtectionFly.contains(joueur.getUniqueId()); }
    public boolean estJoueurProtegeSpawn(Player joueur) { return joueursProtectionSpawn.contains(joueur.getUniqueId()); }

    public Location obtenirSpawnPvpAleatoire() {
        GestionnaireSetupGetDown.SetupMonde setupPvp = 
            gestionnaire.getGestionnaireSetup().getSetupMonde(mondePvp.getName());
        if (setupPvp == null && nomMapPvpOriginale != null) {
            setupPvp = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapPvpOriginale);
        }
        
        if (setupPvp == null) {
            plugin.getLogger().warning("Setup manquant pour la map PvP (monde: " + mondePvp.getName() + ", original: " + nomMapPvpOriginale + ")");
            return mondePvp.getSpawnLocation();
        }

        Map<Integer, Location> spawnsPvp = setupPvp.getSpawnsPvp();
        
        if (spawnsPvp.isEmpty() && nomMapPvpOriginale != null && !nomMapPvpOriginale.equals(mondePvp.getName())) {
            GestionnaireSetupGetDown.SetupMonde setupOriginal = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapPvpOriginale);
            if (setupOriginal != null && !setupOriginal.getSpawnsPvp().isEmpty()) {
                for (Map.Entry<Integer, Location> entry : setupOriginal.getSpawnsPvp().entrySet()) {
                    Location originalLoc = entry.getValue();
                    Location newLoc = new Location(mondePvp, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), 
                                                 originalLoc.getYaw(), originalLoc.getPitch());
                    setupPvp.definirSpawnPvp(entry.getKey(), newLoc);
                }
                spawnsPvp = setupPvp.getSpawnsPvp();
            }
        }
        
        if (!spawnsPvp.isEmpty()) {
            List<Integer> spawnsDisponibles = new ArrayList<>(spawnsPvp.keySet());
            int spawnIndex = spawnsDisponibles.get((int) (Math.random() * spawnsDisponibles.size()));
            Location spawn = spawnsPvp.get(spawnIndex);
            
            if (spawn.getWorld() == null || !spawn.getWorld().equals(mondePvp)) {
                spawn = new Location(mondePvp, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
            }
            
            return spawn;
        }
        
        plugin.getLogger().warning("Aucun spawn PvP configuré pour la map " + nomMapPvpOriginale);
        return mondePvp.getSpawnLocation();
    }

    private void respawnerJoueurPvp(Player joueur) {
        Location spawn = obtenirSpawnPvpAleatoire();
        teleporterJoueurSecurise(joueur, spawn);
    }

    public void respawnerJoueurPvpImmediat(Player joueur) {
        int viesRestantes = viesJoueurs.get(joueur.getUniqueId());
        if (viesRestantes > 0) {
            respawnerJoueurPvp(joueur);
            if (!joueursVivants.contains(joueur)) {
                joueursVivants.add(joueur);
            }
            joueur.sendMessage("§eVies restantes: " + viesRestantes);
            joueur.setHealth(20.0);
            joueur.setFoodLevel(20);
            joueur.setSaturation(20.0f);
            activerProtectionSpawn(joueur);
        }
    }

    private void activerProtectionSpawn(Player joueur) {
        UUID uuid = joueur.getUniqueId();
        joueursProtectionSpawn.add(uuid);
        joueur.sendMessage("§aProtection anti-spawn kill activée pendant 5 secondes !");
        
        BukkitTask tacheExistante = tachesProtectionSpawn.get(uuid);
        if (tacheExistante != null) {
            tacheExistante.cancel();
        }
        
        BukkitTask nouvelleTache = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            desactiverProtectionSpawn(joueur);
        }, 100L);
        
        tachesProtectionSpawn.put(uuid, nouvelleTache);
    }

    private void desactiverProtectionSpawn(Player joueur) {
        UUID uuid = joueur.getUniqueId();
        joueursProtectionSpawn.remove(uuid);
        tachesProtectionSpawn.remove(uuid);
        if (joueur.isOnline()) {
            joueur.sendMessage("§cProtection anti-spawn kill désactivée !");
        }
    }

    private void ajouterItemAvecStacking(Player joueur, ItemStack nouvelItem) {
        Inventory inventaire = joueur.getInventory();
        
        for (int i = 0; i < inventaire.getSize(); i++) {
            ItemStack itemExistant = inventaire.getItem(i);
            if (itemExistant != null && itemExistant.isSimilar(nouvelItem) && 
                itemExistant.getAmount() < itemExistant.getMaxStackSize()) {
                
                int quantiteAjoutee = Math.min(nouvelItem.getAmount(), 
                    itemExistant.getMaxStackSize() - itemExistant.getAmount());
                itemExistant.setAmount(itemExistant.getAmount() + quantiteAjoutee);
                nouvelItem.setAmount(nouvelItem.getAmount() - quantiteAjoutee);
                
                if (nouvelItem.getAmount() <= 0) {
                    return;
                }
            }
        }
        
        if (nouvelItem.getAmount() > 0) {
            inventaire.addItem(nouvelItem);
        }
    }

    private void teleporterJoueurSecurise(Player joueur, Location destination) {
        if (joueur == null || destination == null) {
            plugin.getLogger().warning("Tentative de teleportation avec joueur ou destination null");
            return;
        }
        
        if (destination.getWorld() == null) {
            plugin.getLogger().warning("Tentative de teleportation vers un monde null pour " + joueur.getName());
            return;
        }

        joueursProtectionFly.add(joueur.getUniqueId());
        
        joueur.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        joueur.setFallDistance(0);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean success = joueur.teleport(destination);
            if (success) {
                joueur.setFallDistance(0);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    joueursProtectionFly.remove(joueur.getUniqueId());
                }, 60L);
            } else {
                plugin.getLogger().warning("Echec de teleportation pour " + joueur.getName());
                joueursProtectionFly.remove(joueur.getUniqueId());
            }
        });
    }

    public void ouvrirEnderchest(Player joueur, Location enderchestLocation) {
        if (!enderchestLocation.getChunk().isLoaded()) {
            enderchestLocation.getChunk().load();
        }
        
        List<ItemStack> items = enderchestsItems.get(enderchestLocation);
        if (items == null) {
            for (Map.Entry<Location, List<ItemStack>> entry : enderchestsItems.entrySet()) {
                Location storedLoc = entry.getKey();
                if (storedLoc.getWorld().equals(enderchestLocation.getWorld()) &&
                    storedLoc.getBlockX() == enderchestLocation.getBlockX() &&
                    storedLoc.getBlockY() == enderchestLocation.getBlockY() &&
                    storedLoc.getBlockZ() == enderchestLocation.getBlockZ()) {
                    
                    items = entry.getValue();
                    break;
                }
            }
        }
        
        if (items != null && !items.isEmpty()) {
            Inventory inv = plugin.getServer().createInventory(null, 27, "§5Coffre du joueur éliminé");
            
            for (int i = 0; i < Math.min(items.size(), 27); i++) {
                inv.setItem(i, items.get(i));
            }
            
            joueursEnderChestOuvert.put(joueur.getUniqueId(), enderchestLocation);
            joueur.openInventory(inv);
        }
    }

    public void gererFermetureEnderChest(Inventory inventory, Player joueur) {
        Location enderchestLocation = joueursEnderChestOuvert.remove(joueur.getUniqueId());
        if (enderchestLocation == null) return;
        
        List<ItemStack> itemsRestants = new ArrayList<>();
        
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                itemsRestants.add(item);
            }
        }
        
        if (itemsRestants.isEmpty()) {
            Block block = enderchestLocation.getBlock();
            if (block.getType() == Material.ENDER_CHEST) {
                block.setType(Material.AIR);
            }
            enderchestsItems.remove(enderchestLocation);
        } else {
            enderchestsItems.put(enderchestLocation, itemsRestants);
        }
    }

    private void creerEnderChestAvecItems(Player joueur, Location location, ItemStack[] inventaireSauvegarde, ItemStack[] armureSauvegarde) {
        Location positionEnderChest = trouverPositionValideEnderChest(location);
        
        Block block = positionEnderChest.getBlock();
        block.setType(Material.ENDER_CHEST);
        
        List<ItemStack> items = new ArrayList<>();
        
        ItemStack[] contenuInventaire = inventaireSauvegarde != null ? inventaireSauvegarde : joueur.getInventory().getContents();
        ItemStack[] contenuArmure = armureSauvegarde != null ? armureSauvegarde : joueur.getInventory().getArmorContents();
        
        for (ItemStack item : contenuInventaire) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        
        for (ItemStack item : contenuArmure) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        
        enderchestsItems.put(positionEnderChest, items);
        
        plugin.getLogger().info("EnderChest GetDown créé à la position: " + 
            positionEnderChest.getBlockX() + "," + positionEnderChest.getBlockY() + "," + positionEnderChest.getBlockZ() +
            " avec " + items.size() + " items");
    }
    
    private Location trouverPositionValideEnderChest(Location positionOriginale) {
        if (peutPlacerEnderChest(positionOriginale)) {
            return positionOriginale;
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location testLocation = positionOriginale.clone().add(x, y, z);
                    if (peutPlacerEnderChest(testLocation)) {
                        return testLocation;
                    }
                }
            }
        }

        return positionOriginale;
    }
    
    private boolean peutPlacerEnderChest(Location location) {
        Block block = location.getBlock();
        Material type = block.getType();

        return type == Material.AIR || 
               type == Material.WATER || 
               type == Material.STATIONARY_WATER ||
               type == Material.LAVA ||
               type == Material.STATIONARY_LAVA ||
               type == Material.LONG_GRASS ||
               type == Material.DEAD_BUSH ||
               type == Material.YELLOW_FLOWER ||
               type == Material.RED_ROSE ||
               type == Material.SNOW ||
               !block.getType().isSolid();
    }

    private void demarrerInvincibiliteTemporaire() {
        invincibiliteActive = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            invincibiliteActive = false;
            for (Player joueur : getJoueurs()) {
                joueur.sendMessage("§c» L'invincibilité est terminée ! Le PvP est activé !");
            }
        }, 200L);
    }

    public boolean estInvincibiliteActive() {
        return invincibiliteActive;
    }

    private void mettreJoueurEnSpectateur(Player joueur) {
        joueur.setGameMode(GameMode.SPECTATOR);
        joueur.sendMessage("");
        joueur.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("§c§l                    » ÉLIMINÉ «");
        joueur.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("");
        joueur.sendMessage("§7» Vous êtes maintenant en mode spectateur");
        joueur.sendMessage("§7» Vous serez téléporté au lobby à la fin de la partie");
        joueur.sendMessage("§7» Ou tapez §e/lobby §7pour quitter maintenant");
        joueur.sendMessage("");

        for (Player autreJoueur : getJoueurs()) {
            if (!autreJoueur.equals(joueur) && autreJoueur.getGameMode() != GameMode.SPECTATOR) {
                autreJoueur.hidePlayer(joueur);
            }
        }
    }

    public void attendreJoueurAuSol(Player joueur) {
        if (joueursEnAttenteArrivee.containsKey(joueur.getUniqueId())) {
            joueursEnAttenteArrivee.get(joueur.getUniqueId()).cancel();
        }

        BukkitTask tacheAttente = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (phaseActuelle != PhasePartie.STUFF || !joueur.isOnline()) {
                    joueursEnAttenteArrivee.remove(joueur.getUniqueId());
                    return;
                }

                if (joueur.isOnGround()) {
                    JoueurPartieGetDown joueurPartie = joueurs.get(joueur.getUniqueId());
                    if (joueurPartie != null && peutEncoreAcheter(joueur)) {
                        ouvrirShop(joueur);
                        joueur.sendMessage("§c» Vous ne pouvez pas quitter le menu d'achat pendant la phase d'achat !");
                    }
                    joueursEnAttenteArrivee.remove(joueur.getUniqueId());
                }
            }
        }, 5L, 5L);

        joueursEnAttenteArrivee.put(joueur.getUniqueId(), tacheAttente);
    }



}



