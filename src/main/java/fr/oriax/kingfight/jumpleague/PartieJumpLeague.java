package fr.oriax.kingfight.jumpleague;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.JoueurPartie;
import fr.oriax.kingfight.commun.TraducteurItems;
import fr.oriax.kingfight.jumpleague.GestionnaireSetupJumpLeague.SetupMonde;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PartieJumpLeague {

    private final KingFight plugin;
    private final GestionnaireJumpLeague gestionnaire;
    private final String idPartie;
    private final ConfigurationJumpLeague config;
    private SetupMonde setupMonde;

    private final Map<UUID, JoueurPartieJumpLeague> joueurs;
    private final List<Player> joueursVivants;
    private final Map<UUID, Integer> viesJoueurs;
    private final Map<UUID, Integer> parcoursCompletes;

    private World monde;
    private String nomMapOriginale;
    private EtatPartie etat;
    private PhasePartie phaseActuelle;

    private BukkitTask tachePartie;
    private BukkitTask tacheBordure;
    private BukkitTask tacheScoreboard;
    private BukkitTask tacheCountdown;

    private int tempsEcoule;
    private int tempsRestantPhase;
    private boolean premierJoueurTermine;
    private final Set<UUID> joueursFreeze;
    private final Set<UUID> joueursProtectionFly;
    private final Set<UUID> joueursProtectionSpawn;
    private final Map<UUID, BukkitTask> tachesProtectionSpawn;
    private boolean invincibiliteActive;
    private final Map<Location, List<ItemStack>> enderChestsItems;
    private final Map<UUID, Location> joueursEnderChestOuvert;

    private ScoreboardJumpLeague scoreboardManager;

    public enum EtatPartie {
        ATTENTE, PREPARATION, EN_COURS, TERMINEE
    }

    public enum PhasePartie {
        JUMP, PVP
    }

    public static class JoueurPartieJumpLeague extends JoueurPartie {
        private int numeroParcours;
        private int checkpointActuel;
        private Location dernierCheckpoint;
        private boolean parcoursComplete;

        public JoueurPartieJumpLeague(Player joueur, String nomAnonymise) {
            super(joueur, nomAnonymise);
            this.numeroParcours = 1;
            this.checkpointActuel = 0;
            this.parcoursComplete = false;
        }

        public int getNumeroParcours() { return numeroParcours; }
        public void setNumeroParcours(int numeroParcours) { this.numeroParcours = numeroParcours; }

        public int getCheckpointActuel() { return checkpointActuel; }
        public void setCheckpointActuel(int checkpointActuel) { this.checkpointActuel = checkpointActuel; }

        public Location getDernierCheckpoint() { return dernierCheckpoint; }
        public void setDernierCheckpoint(Location dernierCheckpoint) { this.dernierCheckpoint = dernierCheckpoint; }

        public boolean estParcoursComplete() { return parcoursComplete; }
        public void setParcoursComplete(boolean parcoursComplete) { this.parcoursComplete = parcoursComplete; }
    }

    public PartieJumpLeague(KingFight plugin, GestionnaireJumpLeague gestionnaire, String idPartie,
                           List<Player> listeJoueurs, ConfigurationJumpLeague config) {
        this.plugin = plugin;
        this.gestionnaire = gestionnaire;
        this.idPartie = idPartie;
        this.config = config;
        this.joueurs = new HashMap<>();
        this.joueursVivants = new ArrayList<>();
        this.viesJoueurs = new HashMap<>();
        this.parcoursCompletes = new HashMap<>();
        this.etat = EtatPartie.ATTENTE;
        this.phaseActuelle = PhasePartie.JUMP;
        this.tempsEcoule = 0;
        this.tempsRestantPhase = 0;
        this.premierJoueurTermine = false;
        this.joueursFreeze = new HashSet<>();
        this.joueursProtectionFly = new HashSet<>();
        this.joueursProtectionSpawn = new HashSet<>();
        this.tachesProtectionSpawn = new HashMap<>();
        this.invincibiliteActive = false;
        this.enderChestsItems = new HashMap<>();
        this.joueursEnderChestOuvert = new HashMap<>();

        initialiserJoueurs(listeJoueurs);
        choisirMonde();

        if (gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale) == null) {
            plugin.getLogger().severe("Setup manquant pour la map " + nomMapOriginale);
            this.setupMonde = null;
            return;
        }

        if (monde != null && !monde.getName().equals(nomMapOriginale)) {
            gestionnaire.getGestionnaireSetup().rechargerSetupDepuisOriginal(nomMapOriginale, monde.getName());
        }

        this.setupMonde = gestionnaire.getGestionnaireSetup().getSetupMonde(monde != null ? monde.getName() : nomMapOriginale);

        this.scoreboardManager = new ScoreboardJumpLeague(this);
    }

    private void initialiserJoueurs(List<Player> listeJoueurs) {
        List<String> nomsAnonymises = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireJoueurs().getNomsAnonymises(listeJoueurs);

        for (int i = 0; i < listeJoueurs.size(); i++) {
            Player joueur = listeJoueurs.get(i);
            JoueurPartieJumpLeague joueurPartie = new JoueurPartieJumpLeague(joueur, nomsAnonymises.get(i));
            joueurs.put(joueur.getUniqueId(), joueurPartie);
            joueursVivants.add(joueur);
            viesJoueurs.put(joueur.getUniqueId(), config.getViesPvp());
            parcoursCompletes.put(joueur.getUniqueId(), 0);

        }
    }

    private void choisirMonde() {
        nomMapOriginale = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireMaps().obtenirMapAleatoire(fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE);

        if (nomMapOriginale == null) {
            plugin.getLogger().severe("Aucune map configuree pour JumpLeague !");
            return;
        }

        monde = gestionnaire.getGestionnairePrincipal()
                .getGestionnaireMaps().creerMapTemporaire(nomMapOriginale, idPartie);

        if (monde == null) {
            plugin.getLogger().severe("Impossible de creer la map temporaire pour JumpLeague !");
        }
    }

    public void demarrer() {
        if (monde == null) {
            plugin.getLogger().severe("Impossible de demarrer la partie " + idPartie + " - monde null");
            arreter();
            return;
        }

        if (setupMonde == null) {
            plugin.getLogger().severe("Impossible de demarrer la partie " + idPartie + " - setup monde null pour " + monde.getName());
            arreter();
            return;
        }

        if (joueurs.isEmpty()) {
            plugin.getLogger().warning("Tentative de demarrage d'une partie sans joueurs: " + idPartie);
            arreter();
            return;
        }

        etat = EtatPartie.PREPARATION;
        configurerMonde();
        initialiserBordure();
        verifierEtChargerSpawnsPvp();
        teleporterJoueursAuSpawn();
        preparerJoueurs();
        remplirCoffres();
        creerScoreboards();
        demarrerMiseAJourScoreboards();

        freezerTousLesJoueurs();
        demarrerCountdownDebut();
    }

    private void teleporterJoueursAuSpawn() {
        Map<Integer, Location> spawnsParParcours = setupMonde.getSpawnsParParcours();
        if (spawnsParParcours.isEmpty()) return;

        int numeroParcours = 1;
        for (Player joueur : getJoueurs()) {
            Location spawn = spawnsParParcours.get(numeroParcours);
            if (spawn != null) {
                teleporterJoueurSecurise(joueur, spawn);

                JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
                joueurPartie.setNumeroParcours(numeroParcours);
                joueurPartie.setDernierCheckpoint(spawn);

                numeroParcours++;
                if (numeroParcours > spawnsParParcours.size()) {
                    numeroParcours = 1;
                }
            }
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

            JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
            joueur.setDisplayName(ChatColor.WHITE + joueurPartie.getNomAnonymise());
            joueur.setPlayerListName(ChatColor.WHITE + joueurPartie.getNomAnonymise());
        }
    }

    private void remplirCoffres() {
        for (int numeroParcours = 1; numeroParcours <= 16; numeroParcours++) {
            List<Location> coffresParParcours = setupMonde.getCoffresParcours(numeroParcours);
            for (Location locationCoffre : coffresParParcours) {
                Block block = locationCoffre.getBlock();
                if (block.getType() == Material.CHEST) {
                    Chest coffre = (Chest) block.getState();
                    remplirCoffre(coffre.getInventory());
                }
            }
        }
    }

    private void remplirCoffre(Inventory inventaire) {
        inventaire.clear();

        List<ItemStack> itemsDisponibles = genererItemsDisponibles();
        Collections.shuffle(itemsDisponibles);

        int nombreItems = Math.min(config.getItemsParCoffre(), itemsDisponibles.size());

        List<Integer> slotsDisponibles = new ArrayList<>();
        for (int i = 0; i < inventaire.getSize(); i++) {
            slotsDisponibles.add(i);
        }
        Collections.shuffle(slotsDisponibles);

        for (int i = 0; i < nombreItems && i < slotsDisponibles.size(); i++) {
            ItemStack item = itemsDisponibles.get(i);
            item = TraducteurItems.traduireItem(item);
            int slot = slotsDisponibles.get(i);
            inventaire.setItem(slot, item);
        }
    }

    private List<ItemStack> genererItemsDisponibles() {
        List<ItemStack> items = new ArrayList<>();
        Random random = new Random();
        List<Map.Entry<Material, Double>> itemsAvecChances = new ArrayList<>(config.getChancesItems().entrySet());

        int nombreItemsAGenerer = Math.min(config.getItemsParCoffre(), itemsAvecChances.size());

        for (int i = 0; i < nombreItemsAGenerer; i++) {
            if (itemsAvecChances.isEmpty()) break;

            double totalChances = 0;
            for (Map.Entry<Material, Double> entry : itemsAvecChances) {
                totalChances += entry.getValue();
            }
            double randomValue = random.nextDouble() * totalChances;
            double cumulativeChance = 0;
            
            Material materialChoisi = null;
            for (Map.Entry<Material, Double> entry : itemsAvecChances) {
                cumulativeChance += entry.getValue();
                if (randomValue <= cumulativeChance) {
                    materialChoisi = entry.getKey();
                    break;
                }
            }
            if (materialChoisi != null) {
                ItemStack item = creerItem(materialChoisi);
                if (item != null) {
                    items.add(item);
                }
                final Material finalMaterial = materialChoisi;
                itemsAvecChances.removeIf(entry -> entry.getKey() == finalMaterial);
            }
        }

        return items;
    }

    private ItemStack creerItem(Material material) {
        ItemStack item;
        ItemJumpLeague itemJL = config.getItemJumpLeague(material);
        int quantite = 1;
        
        switch (material) {
            case ARROW:
                quantite = 16 + new Random().nextInt(17);
                break;
            case COOKED_BEEF:
            case BREAD:
                quantite = 2 + new Random().nextInt(4);
                break;
            case APPLE:
                quantite = 1 + new Random().nextInt(3);
                break;
            case GOLDEN_APPLE:
                quantite = 1;
                break;
            case POTION:
                return creerPotionAleatoire();
            case TNT:
                quantite = 1 + new Random().nextInt(3);
                break;
            default:
                quantite = 1;
                break;
        }
        
        item = new ItemStack(material, quantite);
        
        if (itemJL != null) {
            appliquerPersonnalisation(item, itemJL);
        }
        
        return item;
    }
    
    private void appliquerPersonnalisation(ItemStack item, ItemJumpLeague itemJL) {
        if (item == null || itemJL == null) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        if (itemJL.getNomCustom() != null && !itemJL.getNomCustom().isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemJL.getNomCustom()));
        }
        
        if (itemJL.getLoreCustom() != null && !itemJL.getLoreCustom().isEmpty()) {
            List<String> loreColore = new ArrayList<>();
            for (String ligne : itemJL.getLoreCustom()) {
                loreColore.add(ChatColor.translateAlternateColorCodes('&', ligne));
            }
            meta.setLore(loreColore);
        }
        
        item.setItemMeta(meta);
        
        if (itemJL.getEnchantements() != null && !itemJL.getEnchantements().isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : itemJL.getEnchantements().entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }
    }

    private ItemStack creerPotionAleatoire() {
        Random random = new Random();
        short[] potions = {8193, 8194, 8195, 8196, 8197, 8201, 8202, 8204, 8205, 8206, 8207, 8208, 8209, 8210, 8211, 8212, 8213, 8214, 8215, 8216, 8217, 8218, 8219, 8220, 8221, 8222, 8223, 8224, 8225, 8226, 8227, 8228, 8229, 8230, 8231, 8232, 8233, 8234, 8235, 8236, 8237, 8238, 8239, 8240, 8241, 8242, 8243, 8244, 8245, 8246, 8247, 8248, 8249, 8250, 8251, 8252, 8253, 8254, 8255, 8256, 8257, 8258, 8259, 8260, 8261, 8262, 8263, 8264, 8265, 8266, 8267};

        short potionData = potions[random.nextInt(potions.length)];
        ItemStack potion = new ItemStack(Material.POTION, 1, potionData);
        return potion;
    }

    private int trouverSlotLibre(Inventory inventaire) {
        for (int i = 0; i < inventaire.getSize(); i++) {
            if (inventaire.getItem(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private void demarrerPhaseJump() {
        phaseActuelle = PhasePartie.JUMP;
        tempsRestantPhase = config.getDureePhaseJumpMinutes() * 60;

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§a§l                    » PHASE PARCOURS «");
            joueur.sendMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§e» Completez votre parcours !");
            joueur.sendMessage("§7» Tombez dans le vide = retour au checkpoint");
            joueur.sendMessage("");
        }

        tachePartie = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tachePartie != null) tachePartie.cancel();
                return;
            }
            
            tempsEcoule++;
            tempsRestantPhase--;

            if (tempsRestantPhase <= 0) {
                mettreJoueursEnSpectateurTransition();
                if (tachePartie != null) {
                    tachePartie.cancel();
                }
                
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    demarrerPhasePvp();
                }, 200L);
            }
            
            verifierFinPartie();
        }, 20L, 20L);
    }

    public void gererFinParcours(Player joueur) {
        JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null) return;

        joueurPartie.setParcoursComplete(true);

        if (!premierJoueurTermine) {
            premierJoueurTermine = true;

            joueurPartie.ajouterPoints(config.getPointsPremierTermine());

            joueur.sendMessage("");
            joueur.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                    * PREMIER ARRIVÉ ! *");
            joueur.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§a» Parcours terminé !");
            joueur.sendMessage("§6$ Bonus: +" + config.getPointsPremierTermine() + " points");
            joueur.sendMessage("§c» Lancement de la phase PvP...");
            joueur.sendMessage("");

            for (Player autreJoueur : getJoueurs()) {
                if (!autreJoueur.equals(joueur)) {
                    autreJoueur.sendMessage("");
                    autreJoueur.sendMessage("§e» " + joueur.getDisplayName() + " §ea terminé son parcours en premier !");
                    autreJoueur.sendMessage("§c§l» La phase PvP commence !");
                    autreJoueur.sendMessage("§7» Vous allez être téléporté vers la zone PvP !");
                    autreJoueur.sendMessage("");
                }
            }

            mettreJoueursEnSpectateurTransition();
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                demarrerPhasePvp();
            }, 200L);

        } else {
            joueur.sendMessage("§aParcours terminé !");
            if (phaseActuelle == PhasePartie.PVP) {
                joueur.sendMessage("§6Teleportation vers la zone PvP...");
                teleporterJoueurPvp(joueur);
            } else {
                joueur.sendMessage("§eEn attente du debut de la phase PvP...");
            }
        }

        joueur.playSound(joueur.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }

    public void gererMortDansVide(Player joueur) {
        if (etat != EtatPartie.EN_COURS) return;

        JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null || phaseActuelle != PhasePartie.JUMP) return;

        Location checkpoint = joueurPartie.getDernierCheckpoint();
        if (checkpoint != null) {
            teleporterJoueurSecurise(joueur, checkpoint);
            joueur.sendMessage("§e» Retour au checkpoint !");
        }
    }

    public void verifierCheckpoint(Player joueur, Location nouvellePosition) {
        if (etat != EtatPartie.EN_COURS || setupMonde == null) return;

        JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null || phaseActuelle != PhasePartie.JUMP) return;

        int numeroParcours = joueurPartie.getNumeroParcours();
        List<Location> checkpoints = setupMonde.getCheckpointsParcours(numeroParcours);

        if (checkpoints.isEmpty()) return;

        int checkpointActuel = joueurPartie.getCheckpointActuel();

        for (int i = checkpointActuel; i < checkpoints.size(); i++) {
            Location checkpoint = checkpoints.get(i);
            if (estSurBlocExact(nouvellePosition, checkpoint)) {
                joueurPartie.setCheckpointActuel(i + 1);
                joueurPartie.setDernierCheckpoint(checkpoint);

                joueurPartie.ajouterPoints(config.getPointsParcoursReussi());

                int parcoursActuels = parcoursCompletes.get(joueur.getUniqueId());
                parcoursCompletes.put(joueur.getUniqueId(), parcoursActuels + 1);

                joueur.sendMessage("§a» Checkpoint " + (i + 1) + " atteint ! §6+" + config.getPointsParcoursReussi() + " points");
                joueur.playSound(joueur.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);

                scoreboardManager.mettreAJourTousLesScoreboards();
                break;
            }
        }

        Location arrivee = setupMonde.getArriveeParcours(numeroParcours);
        if (arrivee != null && estProcheDeLocation(nouvellePosition, arrivee, 2.0)) {
            if (!joueurPartie.estParcoursComplete()) {
                gererFinParcours(joueur);
            }
        }
    }

    private boolean estProcheDeLocation(Location pos1, Location pos2, double distance) {
        if (!pos1.getWorld().equals(pos2.getWorld())) return false;
        return pos1.distance(pos2) <= distance;
    }

    private boolean estSurBlocExact(Location positionJoueur, Location blocCheckpoint) {
        if (!positionJoueur.getWorld().equals(blocCheckpoint.getWorld())) return false;

        int blocX = blocCheckpoint.getBlockX();
        int blocY = blocCheckpoint.getBlockY();
        int blocZ = blocCheckpoint.getBlockZ();

        int joueurX = positionJoueur.getBlockX();
        int joueurY = positionJoueur.getBlockY();
        int joueurZ = positionJoueur.getBlockZ();

        return joueurX == blocX && joueurY == blocY && joueurZ == blocZ;
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

    private void teleporterJoueurPvp(Player joueur) {
        if (setupMonde == null) {
            plugin.getLogger().warning("SetupMonde null lors de la teleportation individuelle PvP pour " + joueur.getName());
            joueur.sendMessage("§cErreur: Configuration manquante !");
            return;
        }

        Map<Integer, Location> spawnsPvp = setupMonde.getSpawnsPvp();
        
        if (spawnsPvp.isEmpty() && nomMapOriginale != null && !nomMapOriginale.equals(monde.getName())) {
            GestionnaireSetupJumpLeague.SetupMonde setupOriginal = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
            if (setupOriginal != null && !setupOriginal.getSpawnsPvp().isEmpty()) {
                spawnsPvp = new HashMap<>();
                for (Map.Entry<Integer, Location> entry : setupOriginal.getSpawnsPvp().entrySet()) {
                    Location originalLoc = entry.getValue();
                    Location newLoc = new Location(monde, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), 
                                                 originalLoc.getYaw(), originalLoc.getPitch());
                    spawnsPvp.put(entry.getKey(), newLoc);
                }
            }
        }
        
        if (!spawnsPvp.isEmpty()) {
            List<Integer> spawnsDisponibles = new ArrayList<>(spawnsPvp.keySet());
            int spawnIndex = spawnsDisponibles.get((int) (Math.random() * spawnsDisponibles.size()));
            Location spawn = spawnsPvp.get(spawnIndex);
            
            if (spawn.getWorld() == null || !spawn.getWorld().equals(monde)) {
                spawn = new Location(monde, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
            }
            

            teleporterJoueurSecurise(joueur, spawn);
            joueur.sendMessage("§a» Téléportation vers la zone PvP réussie !");
        } else {
            joueur.sendMessage("§cErreur: Aucun spawn PvP configure !");
            plugin.getLogger().warning("Aucun spawn PvP configure pour la map " + monde.getName());
        }
    }

    private void demarrerPhasePvp() {
        if (phaseActuelle == PhasePartie.PVP) {
            plugin.getLogger().warning("Phase PvP deja active pour la partie " + idPartie);
            return;
        }

        if (tachePartie != null) {
            tachePartie.cancel();
        }

        demarrerCountdownPvp();
    }

    private void teleporterJoueursPvp() {
        if (setupMonde == null) {
            plugin.getLogger().severe("SetupMonde est null lors de la teleportation PvP pour la map " + monde.getName());
            rechargerSetup();
            if (setupMonde == null) {
                plugin.getLogger().severe("Impossible de recharger le setup pour la map " + monde.getName());
                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("§cErreur: Configuration manquante pour cette map !");
                }
                return;
            }
        }

        GestionnaireSetupJumpLeague.ConfigurationMultiMaps configMultiMaps = 
            gestionnaire.getGestionnaireSetup().getConfigurationMultiMaps();
        
        if (configMultiMaps.aPhasePvp() && !configMultiMaps.getMapPvp().equals(nomMapOriginale)) {
            plugin.getLogger().warning("Configuration multi-maps détectée avec map PvP différente: " + 
                configMultiMaps.getMapPvp() + " vs " + nomMapOriginale);
        }
        gestionnaire.getGestionnaireSetup().rechargerSetupDepuisOriginal(nomMapOriginale, monde.getName());
        setupMonde = gestionnaire.getGestionnaireSetup().getSetupMonde(monde.getName());
        
        Map<Integer, Location> spawnsPvp = setupMonde.getSpawnsPvp();
        
        if (spawnsPvp.isEmpty()) {
            GestionnaireSetupJumpLeague.SetupMonde setupOriginal = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
            
            if (setupOriginal != null) {
                if (!setupOriginal.getSpawnsPvp().isEmpty()) {
                    spawnsPvp = new HashMap<>();
                    for (Map.Entry<Integer, Location> entry : setupOriginal.getSpawnsPvp().entrySet()) {
                        Location originalLoc = entry.getValue();
                        Location newLoc = new Location(monde, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), 
                                                     originalLoc.getYaw(), originalLoc.getPitch());
                        spawnsPvp.put(entry.getKey(), newLoc);
                    }
                    
                    for (Map.Entry<Integer, Location> entry : spawnsPvp.entrySet()) {
                        setupMonde.definirSpawnPvp(entry.getKey(), entry.getValue());
                    }
                    
                    spawnsPvp = setupMonde.getSpawnsPvp();
                }
            }
            
            if (spawnsPvp.isEmpty()) {
                plugin.getLogger().severe("AUCUN SPAWN PVP DISPONIBLE !");
                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("§cErreur: Aucun spawn PvP configure pour cette map !");
                    joueur.sendMessage("§eUtilisez /setup jumpleague pvp <numero> pour configurer les spawns PvP");
                }
                return;
            }
        }

        List<Player> joueursActifs = new ArrayList<>();
        for (Player joueur : getJoueurs()) {
            if (joueursVivants.contains(joueur)) {
                joueursActifs.add(joueur);
            }
        }
        
        plugin.getLogger().info("Teleportation de " + joueursActifs.size() + " joueurs vivants vers " + spawnsPvp.size() + " spawns PvP");
        
        List<Integer> spawnsDisponibles = new ArrayList<>(spawnsPvp.keySet());
        Collections.shuffle(spawnsDisponibles);
        plugin.getLogger().info("Spawns disponibles: " + spawnsDisponibles);

        for (int i = 0; i < joueursActifs.size(); i++) {
            Player joueur = joueursActifs.get(i);
            int spawnIndex = spawnsDisponibles.get(i % spawnsDisponibles.size());
            Location spawn = spawnsPvp.get(spawnIndex);
            
            if (spawn == null) {
                plugin.getLogger().warning("Spawn PvP " + spawnIndex + " est null pour la map " + monde.getName());
                joueur.sendMessage("§cErreur: Spawn PvP " + spawnIndex + " non configure !");
                continue;
            }
            
            if (spawn.getWorld() == null || !spawn.getWorld().equals(monde)) {
                plugin.getLogger().warning("Spawn PvP " + spawnIndex + " dans un monde incorrect. Correction...");
                spawn = new Location(monde, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
            }
            
            plugin.getLogger().info("Teleportation du joueur " + joueur.getName() + " vers le spawn PvP " + spawnIndex + 
                                  " aux coordonnees " + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ());
            
            teleporterJoueurSecurise(joueur, spawn);
            joueur.sendMessage("§a» Téléportation vers la zone PvP réussie ! (Spawn " + spawnIndex + ")");
        }
        
        plugin.getLogger().info("=== FIN TELEPORTATION PVP ===");
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
        
        plugin.getLogger().info("Monde JumpLeague " + monde.getName() + " configuré en mode Peaceful avec jour permanent");
    }

    private void initialiserBordure() {
        WorldBorder bordure = monde.getWorldBorder();
        bordure.setSize(50000);
    }
    
    private void verifierEtChargerSpawnsPvp() {
        plugin.getLogger().info("Vérification et chargement des spawns PVP au démarrage...");
        
        if (setupMonde == null) {
            plugin.getLogger().warning("SetupMonde null lors de la vérification des spawns PVP");
            return;
        }
        
        Map<Integer, Location> spawnsPvp = setupMonde.getSpawnsPvp();
        plugin.getLogger().info("Spawns PVP dans le setup temporaire: " + spawnsPvp.size());
        
        if (spawnsPvp.isEmpty()) {
            plugin.getLogger().info("Aucun spawn PVP dans le setup temporaire, chargement depuis l'original...");
            GestionnaireSetupJumpLeague.SetupMonde setupOriginal = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
            
            if (setupOriginal != null && !setupOriginal.getSpawnsPvp().isEmpty()) {
                plugin.getLogger().info("Spawns PVP trouvés dans le setup original: " + setupOriginal.getSpawnsPvp().size());
                
                for (Map.Entry<Integer, Location> entry : setupOriginal.getSpawnsPvp().entrySet()) {
                    Location originalLoc = entry.getValue();
                    Location newLoc = new Location(monde, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), 
                                                 originalLoc.getYaw(), originalLoc.getPitch());
                    setupMonde.definirSpawnPvp(entry.getKey(), newLoc);
                    plugin.getLogger().info("Spawn PVP " + entry.getKey() + " préchargé: " + 
                        originalLoc.getBlockX() + "," + originalLoc.getBlockY() + "," + originalLoc.getBlockZ());
                }
                
                plugin.getLogger().info("Spawns PVP préchargés avec succès: " + setupMonde.getSpawnsPvp().size());
            } else {
                plugin.getLogger().warning("Aucun spawn PVP trouvé dans le setup original pour " + nomMapOriginale);
            }
        } else {
            plugin.getLogger().info("Spawns PVP déjà présents dans le setup temporaire: " + spawnsPvp.size());
        }
    }

    private void demarrerBordure() {
        Location centre = setupMonde.getCentreBordure();
        if (centre == null) {
            for (Player joueur : getJoueurs()) {
                joueur.sendMessage("§c§lErreur: Centre de bordure non configure !");
                joueur.sendMessage("§7La bordure ne peut pas se reduire.");
            }
            plugin.getLogger().warning("Centre de bordure non configure pour la map " + monde.getName());
            return;
        }

        WorldBorder bordure = monde.getWorldBorder();
        bordure.setCenter(centre);
        bordure.setSize(setupMonde.getRayonBordureInitial() * 2);

        int dureeReduction = (config.getDureePhasePvpMinutes() - config.getTempsAvantBordureMinutes()) * 60;
        bordure.setSize(20, dureeReduction);

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§c§l                » BORDURE ACTIVE «");
            joueur.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§c» La bordure commence a se reduire !");
            joueur.sendMessage("§7» Restez dans la zone de jeu !");
            joueur.sendMessage("");
        }

        tacheBordure = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tacheBordure != null) tacheBordure.cancel();
                return;
            }
            
            double rayonBordure = bordure.getSize() / 2.0;
            
            for (Player joueur : getJoueurs()) {
                if (!joueur.getWorld().equals(centre.getWorld())) continue;
                
                Location locJoueur = joueur.getLocation();
                double deltaX = Math.abs(locJoueur.getX() - centre.getX());
                double deltaZ = Math.abs(locJoueur.getZ() - centre.getZ());
                double distanceMaximale = Math.max(deltaX, deltaZ);

                if (distanceMaximale > rayonBordure) {
                    joueur.damage(config.getDegatsBordureParSeconde());
                    joueur.sendMessage("§cVous etes dans la zone dangereuse !");
                }
            }
        }, 20L, 20L);
    }

    public void eliminerJoueur(Player joueur, Player tueur) {
        eliminerJoueur(joueur, tueur, null);
    }
    
    public void eliminerJoueur(Player joueur, Player tueur, Location positionMort) {
        if (!joueursVivants.contains(joueur)) return;

        int viesRestantes = viesJoueurs.get(joueur.getUniqueId()) - 1;
        viesJoueurs.put(joueur.getUniqueId(), viesRestantes);

        if (viesRestantes <= 0) {
            joueursVivants.remove(joueur);
            plugin.getLogger().info("Joueur " + joueur.getName() + " éliminé. Joueurs vivants restants: " + joueursVivants.size());

            Location mortLocation = positionMort != null ? positionMort : joueur.getLocation();
            
            mortLocation.getWorld().strikeLightningEffect(mortLocation);
            for (Player p : getJoueurs()) {
                p.playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1.0f, 1.0f);
            }
            
            JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
            if (tueur != null && joueursVivants.contains(tueur)) {
                JoueurPartieJumpLeague tueurPartie = joueurs.get(tueur.getUniqueId());
                for (Player p : getJoueurs()) {
                    p.sendMessage("§c" + joueurPartie.getNomAnonymise() + " §7a été éliminé par §c" + tueurPartie.getNomAnonymise());
                }
            } else {
                for (Player p : getJoueurs()) {
                    p.sendMessage("§c" + joueurPartie.getNomAnonymise() + " §7a été éliminé");
                }
            }
            
            creerEnderChestAvecItems(joueur, mortLocation);

            scoreboardManager.mettreAJourTousLesScoreboards();
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                mettreJoueurEnSpectateur(joueur);
            }, 20L);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                verifierFinPartie();
            }, 25L);
        }

        if (tueur != null && joueursVivants.contains(tueur)) {
            JoueurPartieJumpLeague tueurPartie = joueurs.get(tueur.getUniqueId());
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

    private void creerEnderChestAvecItems(Player joueur, Location location) {
        creerEnderChestAvecItems(joueur, location, null, null);
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
        
        enderChestsItems.put(positionEnderChest, items);
        
        plugin.getLogger().info("EnderChest JumpLeague créé à la position: " + 
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
    
    public void gererInteractionEnderChest(Player joueur, Location enderchestLocation) {
        if (!enderchestLocation.getChunk().isLoaded()) {
            enderchestLocation.getChunk().load();
        }
        
        List<ItemStack> items = enderChestsItems.get(enderchestLocation);
        if (items == null) {
            for (Map.Entry<Location, List<ItemStack>> entry : enderChestsItems.entrySet()) {
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
    
    public void gererFermetureEnderChest(Inventory inventory, UUID joueurId) {
        Location enderchestLocation = joueursEnderChestOuvert.remove(joueurId);
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
            enderChestsItems.remove(enderchestLocation);
        } else {
            enderChestsItems.put(enderchestLocation, itemsRestants);
        }
    }

    private void verifierFinPartie() {
        if (joueursVivants.size() <= 1) {
            if (tachePartie != null) {
                tachePartie.cancel();
                tachePartie = null;
            }
            terminerPartie();
        }
    }

    private void terminerPartie() {
        if (etat == EtatPartie.TERMINEE) {
            return;
        }

        etat = EtatPartie.TERMINEE;

        for (Player joueur : getJoueurs()) {
            gestionnaire.getGestionnairePrincipal().getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
        }

        if (tachePartie != null) tachePartie.cancel();
        if (tacheBordure != null) tacheBordure.cancel();
        if (tacheScoreboard != null) tacheScoreboard.cancel();
        if (tacheCountdown != null) tacheCountdown.cancel();
        
        for (BukkitTask tache : tachesProtectionSpawn.values()) {
            if (tache != null) {
                tache.cancel();
            }
        }
        tachesProtectionSpawn.clear();
        joueursProtectionSpawn.clear();

        attribuerPointsFinaux();
        afficherResultats();
        sauvegarderStatistiques();

        plugin.getServer().getScheduler().runTaskLater(plugin, this::arreter, 200L);
    }

    private void attribuerPointsFinaux() {
        List<Player> classement = new ArrayList<>(joueursVivants);
        classement.sort((a, b) -> {
            JoueurPartieJumpLeague joueurA = joueurs.get(a.getUniqueId());
            JoueurPartieJumpLeague joueurB = joueurs.get(b.getUniqueId());
            return Integer.compare(joueurB.getPoints(), joueurA.getPoints());
        });

        for (int i = 0; i < classement.size(); i++) {
            Player joueur = classement.get(i);
            JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());

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
        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§6§l                » PARTIE TERMINEE «");
            joueur.sendMessage("§6§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");

            List<Player> classement = new ArrayList<>(Arrays.asList(getJoueurs().toArray(new Player[0])));
            classement.sort((a, b) -> {
                JoueurPartieJumpLeague joueurA = joueurs.get(a.getUniqueId());
                JoueurPartieJumpLeague joueurB = joueurs.get(b.getUniqueId());
                return Integer.compare(joueurB.getPoints(), joueurA.getPoints());
            });

            joueur.sendMessage("§e» Classement de la partie:");
            for (int i = 0; i < Math.min(3, classement.size()); i++) {
                Player joueurClasse = classement.get(i);
                JoueurPartieJumpLeague joueurPartie = joueurs.get(joueurClasse.getUniqueId());

                String couleur = i == 0 ? "§6" : i == 1 ? "§e" : "§7";
                String medaille = i == 0 ? "🥇" : i == 1 ? "🥈" : "🥉";
                joueur.sendMessage(couleur + medaille + " #" + (i + 1) + " " + joueurPartie.getNomAnonymise() +
                                 " §f- " + joueurPartie.getPoints() + " points");
            }

            joueur.sendMessage("");
            JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
            joueur.sendMessage("§a» Vos statistiques:");
            joueur.sendMessage("§f  • Points: §6" + joueurPartie.getPoints());
            joueur.sendMessage("§f  • Kills: §c" + joueurPartie.getKills());
            joueur.sendMessage("§f  • Parcours completes: §d" + parcoursCompletes.get(joueur.getUniqueId()));
            joueur.sendMessage("");
            joueur.sendMessage("§7» Vous serez teleporte au lobby dans 10 secondes...");
            joueur.sendMessage("");
        }
    }

    private void sauvegarderStatistiques() {
        for (Player joueur : getJoueurs()) {
            JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
            gestionnaire.getGestionnairePrincipal().getGestionnaireClassements()
                    .ajouterPoints(joueur, fr.oriax.kingfight.commun.TypeMiniJeu.JUMP_LEAGUE, joueurPartie.getPoints());
        }
    }

    private void creerScoreboards() {
        for (Player joueur : getJoueurs()) {
            scoreboardManager.creerScoreboard(joueur);
        }
    }

    private void demarrerMiseAJourScoreboards() {
        tacheScoreboard = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tacheScoreboard != null) tacheScoreboard.cancel();
                return;
            }
            
            scoreboardManager.mettreAJourTousLesScoreboards();
            mettreAJourActionBarTousJoueurs();
        }, 0L, 20L);
    }

    private void mettreAJourActionBarTousJoueurs() {
        for (Player joueur : getJoueurs()) {
            mettreAJourActionBar(joueur);
        }
    }

    private void mettreAJourActionBar(Player joueur) {
        JoueurPartieJumpLeague joueurPartie = joueurs.get(joueur.getUniqueId());
        if (joueurPartie == null) return;

        String message = "";

        if (phaseActuelle == PhasePartie.JUMP) {
            int nombreParcours = this.parcoursCompletes.getOrDefault(joueur.getUniqueId(), 0);
            message = "§6$ Points: §f" + joueurPartie.getPoints() + " §7| §d» Parcours: §f" + nombreParcours;
        } else if (phaseActuelle == PhasePartie.PVP) {
            int kills = joueurPartie.getKills();
            message = "§4X Kills: §f" + kills + " §7| §6$ Points: §f" + joueurPartie.getPoints();
        }

        if (!message.isEmpty()) {
            envoyerActionBar(joueur, message);
        }
    }

    private void envoyerActionBar(Player joueur, String message) {

    }

    private void freezerTousLesJoueurs() {
        for (Player joueur : getJoueurs()) {
            freezerJoueur(joueur);
        }
    }

    private void defreezerTousLesJoueurs() {
        for (Player joueur : getJoueurs()) {
            defreezerJoueur(joueur);
        }
    }

    private void freezerJoueur(Player joueur) {
        joueursFreeze.add(joueur.getUniqueId());
        joueur.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
        joueur.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));

        if (joueur.isFlying() || joueur.getAllowFlight()) {
            joueursProtectionFly.add(joueur.getUniqueId());
            joueur.setAllowFlight(true);
            joueur.setFlying(false);
        }
    }

    private void defreezerJoueur(Player joueur) {
        joueursFreeze.remove(joueur.getUniqueId());
        joueur.removePotionEffect(PotionEffectType.SLOW);
        joueur.removePotionEffect(PotionEffectType.JUMP);

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

    private void mettreJoueursEnSpectateurTransition() {
        for (Player joueur : getJoueurs()) {
            if (joueur.getGameMode() != GameMode.SPECTATOR) {
                joueur.setGameMode(GameMode.SPECTATOR);
                
                joueur.sendMessage("");
                joueur.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                joueur.sendMessage("§e§l                » TRANSITION VERS PVP «");
                joueur.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                joueur.sendMessage("");
                joueur.sendMessage("§7» Phase parcours terminée !");
                joueur.sendMessage("§6» Préparation pour la phase PvP...");
                joueur.sendMessage("§7» Mode spectateur temporaire (10 secondes)");
                joueur.sendMessage("");
                
                joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);
            }
        }
    }

    private void remettreJoueursEnSurvie() {
        for (Player joueur : getJoueurs()) {
            if (joueur.getGameMode() == GameMode.SPECTATOR && joueursVivants.contains(joueur)) {
                joueur.setGameMode(GameMode.SURVIVAL);
                plugin.getLogger().info("Joueur " + joueur.getName() + " remis en mode survie pour la phase PVP");
            }
        }
    }

    private void demarrerCountdownDebut() {
        final int[] countdown = {10};

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§a§l                    » JUMP LEAGUE «");
            joueur.sendMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§e» Préparation en cours...");
            joueur.sendMessage("§7» Préparez-vous à courir !");
            joueur.sendMessage("");
        }

        tacheCountdown = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tacheCountdown != null) tacheCountdown.cancel();
                return;
            }
            
            if (countdown[0] > 0) {
                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("§e§l» DÉMARRAGE dans §f§l" + countdown[0] + "§e§l seconde" + (countdown[0] > 1 ? "s" : "") + " !");
                    joueur.playSound(joueur.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.0f);
                    joueur.sendTitle("§e§l" + countdown[0], "§7» Préparez-vous !");
                }
                countdown[0]--;
            } else {
                tacheCountdown.cancel();

                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("");
                    joueur.sendMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    joueur.sendMessage("§a§l                      » C'EST PARTI ! »");
                    joueur.sendMessage("§a§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    joueur.sendMessage("");
                    joueur.playSound(joueur.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);
                    joueur.sendTitle("§a§lGO !", "§eBonne chance !");
                }

                defreezerTousLesJoueurs();
                etat = EtatPartie.EN_COURS;
                demarrerPhaseJump();
            }
        }, 20L, 20L);
    }

    private void demarrerCountdownPvp() {
        plugin.getLogger().info("Debut du countdown PvP pour la partie " + idPartie);
        final int[] countdown = {10};

        plugin.getLogger().info("Les joueurs sont en mode spectateur pour le countdown PvP");

        for (Player joueur : getJoueurs()) {
            joueur.sendMessage("");
            joueur.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("§c§l                    » TRANSITION PVP »");
            joueur.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            joueur.sendMessage("");
            joueur.sendMessage("§e» Préparation pour la phase PvP...");
            joueur.sendMessage("§7» Tous les joueurs vont être téléportés !");
            joueur.sendMessage("");
        }

        tacheCountdown = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (etat == EtatPartie.TERMINEE) {
                if (tacheCountdown != null) tacheCountdown.cancel();
                return;
            }
            
            if (countdown[0] > 0) {
                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("§c§l» PHASE PVP dans §f§l" + countdown[0] + "§c§l seconde" + (countdown[0] > 1 ? "s" : "") + " !");
                    joueur.playSound(joueur.getLocation(), Sound.NOTE_BASS, 1.0f, 0.5f);
                    joueur.sendTitle("§c§l" + countdown[0], "§e» Phase PvP imminente !");
                }
                countdown[0]--;
            } else {
                tacheCountdown.cancel();

                phaseActuelle = PhasePartie.PVP;

                for (Player joueur : getJoueurs()) {
                    joueur.sendMessage("");
                    joueur.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    joueur.sendMessage("§c§l                    » PHASE PVP LANCEE ! »");
                    joueur.sendMessage("§c§l”━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    joueur.sendMessage("");
                    joueur.sendMessage("§c♥ Vies: §f" + config.getViesPvp());
                    joueur.sendMessage("§a+ Kill = Régénération II pendant " + config.getDureeRegenerationKillSecondes() + "s");
                    joueur.sendMessage("§c» Invincibilité: 10 secondes");
                    joueur.sendMessage("");
                    joueur.playSound(joueur.getLocation(), Sound.WITHER_SPAWN, 1.0f, 1.0f);
                    joueur.sendTitle("§c§lPVP !", "§e" + config.getViesPvp() + " vies");
                }

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getLogger().info("Execution de la teleportation PvP programmee");
                    remettreJoueursEnSurvie();
                    teleporterJoueursPvp();
                    defreezerTousLesJoueurs();
                    demarrerInvincibiliteTemporaire();
                }, 20L);
                tempsEcoule = 0;
                tempsRestantPhase = config.getDureePhasePvpMinutes() * 60;

                tachePartie = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                    if (etat == EtatPartie.TERMINEE) {
                        if (tachePartie != null) tachePartie.cancel();
                        return;
                    }
                    
                    tempsEcoule++;
                    tempsRestantPhase--;

                    if (tempsEcoule == config.getTempsAvantBordureMinutes() * 60) {
                        demarrerBordure();
                    }

                    if (tempsRestantPhase <= 0) {
                        terminerPartie();
                    }

                    verifierFinPartie();
                }, 20L, 20L);
            }
        }, 20L, 20L);
    }



    public void arreter() {
        if (tachePartie != null) tachePartie.cancel();
        if (tacheBordure != null) tacheBordure.cancel();
        if (tacheScoreboard != null) tacheScoreboard.cancel();
        if (tacheCountdown != null) tacheCountdown.cancel();

        for (Player joueur : getJoueurs()) {
            if (joueursFreeze.contains(joueur.getUniqueId())) {
                defreezerJoueur(joueur);
            }

            scoreboardManager.supprimerScoreboard(joueur);
            joueur.setDisplayName(joueur.getName());
            joueur.setPlayerListName(joueur.getName());
            gestionnaire.getGestionnairePrincipal().getGestionnaireLobby().teleporterVersLobby(joueur);
        }

        joueursFreeze.clear();
        joueursProtectionFly.clear();
        enderChestsItems.clear();
        joueursEnderChestOuvert.clear();

        gestionnaire.getGestionnairePrincipal().getGestionnaireMaps().supprimerMapTemporaire(idPartie);

        if (monde != null && !monde.getName().equals(nomMapOriginale)) {
            gestionnaire.getGestionnaireSetup().supprimerSetupTemporaire(monde.getName());
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

    public void rechargerSetup() {
        if (nomMapOriginale != null && monde != null && !monde.getName().equals(nomMapOriginale)) {
            gestionnaire.getGestionnaireSetup().rechargerSetupDepuisOriginal(nomMapOriginale, monde.getName());
            SetupMonde nouveauSetup = gestionnaire.getGestionnaireSetup().getSetupMonde(monde.getName());
            if (nouveauSetup != null) {
                this.setupMonde = nouveauSetup;
                plugin.getLogger().info("Setup recharge pour la partie " + idPartie);
            } else {
                plugin.getLogger().warning("Impossible de recharger le setup pour la partie " + idPartie);
            }
        }
    }

    public EtatPartie getEtat() { return etat; }
    public PhasePartie getPhaseActuelle() { return phaseActuelle; }
    public String getIdPartie() { return idPartie; }
    public World getMonde() { return monde; }
    public String getNomMapOriginale() { return nomMapOriginale; }
    public int getTempsEcoule() { return tempsEcoule; }
    public JoueurPartieJumpLeague getJoueurPartie(UUID uuid) { return joueurs.get(uuid); }
    public ConfigurationJumpLeague getConfig() { return config; }
    public int getNombreJoueursVivants() { return joueursVivants.size(); }
    public int getParcoursCompletes(UUID uuid) { return parcoursCompletes.getOrDefault(uuid, 0); }
    public int getViesJoueur(UUID uuid) { return viesJoueurs.getOrDefault(uuid, 0); }
    public boolean estJoueurFreeze(Player joueur) { return joueursFreeze.contains(joueur.getUniqueId()); }
    public boolean estJoueurProtegeFly(Player joueur) { return joueursProtectionFly.contains(joueur.getUniqueId()); }
    public boolean estJoueurProtegeSpawn(Player joueur) { return joueursProtectionSpawn.contains(joueur.getUniqueId()); }
    public int getNombreTotalParcours() { return setupMonde != null ? setupMonde.getSpawnsParParcours().size() : 0; }
    public int getNombreCheckpointsParcours(int numeroParcours) { 
        if (setupMonde == null) return 0;
        List<Location> checkpoints = setupMonde.getCheckpointsParcours(numeroParcours);
        return checkpoints != null ? checkpoints.size() : 0;
    }

    public Location obtenirSpawnPvpAleatoire() {
        if (setupMonde == null) {
            plugin.getLogger().warning("SetupMonde null dans obtenirSpawnPvpAleatoire, rechargement...");
            rechargerSetup();
            if (setupMonde == null) {
                return getMonde().getSpawnLocation();
            }
        }
        
        Map<Integer, Location> spawnsPvp = setupMonde.getSpawnsPvp();
        
        if (spawnsPvp.isEmpty()) {
            GestionnaireSetupJumpLeague.SetupMonde setupOriginal = gestionnaire.getGestionnaireSetup().getSetupMonde(nomMapOriginale);
            if (setupOriginal != null && !setupOriginal.getSpawnsPvp().isEmpty()) {
                for (Map.Entry<Integer, Location> entry : setupOriginal.getSpawnsPvp().entrySet()) {
                    Location originalLoc = entry.getValue();
                    Location newLoc = new Location(monde, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(), 
                                                 originalLoc.getYaw(), originalLoc.getPitch());
                    setupMonde.definirSpawnPvp(entry.getKey(), newLoc);
                }
                spawnsPvp = setupMonde.getSpawnsPvp();
            }
        }
        
        if (!spawnsPvp.isEmpty()) {
            List<Integer> spawnsDisponibles = new ArrayList<>(spawnsPvp.keySet());
            int spawnIndex = spawnsDisponibles.get((int) (Math.random() * spawnsDisponibles.size()));
            Location spawn = spawnsPvp.get(spawnIndex);
            
            if (spawn.getWorld() == null || !spawn.getWorld().equals(monde)) {
                spawn = new Location(monde, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
            }
            
            return spawn;
        }
        return getMonde().getSpawnLocation();
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
                plugin.getLogger().info("Joueur " + joueur.getName() + " rajouté aux joueurs vivants. Total: " + joueursVivants.size());
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

    public void retirerJoueurDePartie(Player joueur) {
        if (!joueurs.containsKey(joueur.getUniqueId())) return;


        if (joueursFreeze.contains(joueur.getUniqueId())) {
            defreezerJoueur(joueur);
        }
        
        joueursVivants.remove(joueur);
        joueurs.remove(joueur.getUniqueId());
        viesJoueurs.remove(joueur.getUniqueId());
        parcoursCompletes.remove(joueur.getUniqueId());
        joueursFreeze.remove(joueur.getUniqueId());
        joueursProtectionFly.remove(joueur.getUniqueId());
        joueursEnderChestOuvert.remove(joueur.getUniqueId());
        
        BukkitTask tacheProtection = tachesProtectionSpawn.remove(joueur.getUniqueId());
        if (tacheProtection != null) {
            tacheProtection.cancel();
        }
        joueursProtectionSpawn.remove(joueur.getUniqueId());


        scoreboardManager.supprimerScoreboard(joueur);
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

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            verifierFinPartie();
        }, 5L);


        joueur.setDisplayName(joueur.getName());
        joueur.setPlayerListName(joueur.getName());


        for (Player autreJoueur : getJoueurs()) {
            autreJoueur.showPlayer(joueur);
            joueur.showPlayer(autreJoueur);
        }
        

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
        
        plugin.getLogger().info("Joueur " + nomJoueur + " retiré de la partie JumpLeague " + idPartie);
        
        verifierFinPartie();
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

    public int getTempsRestantPhase() {
        return tempsRestantPhase;
    }
}
