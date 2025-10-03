package fr.oriax.kingfight.jumpleague;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TraducteurItems;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationJumpLeague {

    private final KingFight plugin;
    private FileConfiguration config;
    private File fichierConfig;

    private int tailleFileAttente;
    private boolean lancementAutomatiqueActif;
    private int minimumJoueursLancementAuto;
    private int delaiLancementAutoSecondes;
    private int dureePhaseJumpMinutes;
    private int dureePhasePvpMinutes;
    private int viesPvp;
    private int tempsAvantBordureMinutes;
    private int vitesseBordureBlocsParSeconde;
    private int degatsBordureParSeconde;
    private int dureeRegenerationKillSecondes;

    private int itemsParCoffre;
    private Map<Material, Double> chancesItems;
    private Map<Material, ItemJumpLeague> itemsJumpLeague;

    private int pointsParcoursReussi;
    private int pointsPremierTermine;
    private int pointsKill;
    private int pointsTop12;
    private int pointsTop8;
    private int pointsTop5;
    private int pointsTop3;
    private int pointsTop2;
    private int pointsTop1;

    private int regenerationNiveau;
    private int regenerationDureeSecondes;

    public ConfigurationJumpLeague(KingFight plugin) {
        this.plugin = plugin;
        this.chancesItems = new HashMap<>();
        this.itemsJumpLeague = new HashMap<>();
        chargerConfiguration();
    }

    private void chargerConfiguration() {
        File dossierJumpLeague = new File(plugin.getDataFolder(), "jumpleague");
        if (!dossierJumpLeague.exists()) {
            dossierJumpLeague.mkdirs();
        }

        fichierConfig = new File(dossierJumpLeague, "config.yml");

        if (!fichierConfig.exists()) {
            plugin.saveResource("jumpleague/config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(fichierConfig);

        tailleFileAttente = config.getInt("partie.taille-file-attente", 16);
        lancementAutomatiqueActif = config.getBoolean("partie.lancement-automatique-actif", true);
        minimumJoueursLancementAuto = config.getInt("partie.minimum-joueurs-lancement-auto", 12);
        delaiLancementAutoSecondes = config.getInt("partie.delai-lancement-auto-secondes", 60);
        dureePhaseJumpMinutes = config.getInt("partie.duree-phase-jump-minutes", 8);
        dureePhasePvpMinutes = config.getInt("partie.duree-phase-pvp-minutes", 7);
        viesPvp = config.getInt("partie.vies-pvp", 3);
        tempsAvantBordureMinutes = config.getInt("partie.temps-avant-bordure-minutes", 1);
        vitesseBordureBlocsParSeconde = config.getInt("partie.vitesse-bordure-blocs-par-seconde", 2);
        degatsBordureParSeconde = config.getInt("partie.degats-bordure-par-seconde", 4);
        dureeRegenerationKillSecondes = config.getInt("partie.duree-regeneration-kill-secondes", 10);

        itemsParCoffre = config.getInt("coffres.items-par-coffre", 4);
        chargerChancesItems();

        pointsParcoursReussi = config.getInt("points.parcours-reussi", 2);
        pointsPremierTermine = config.getInt("points.premier-termine", 10);
        pointsKill = config.getInt("points.kill", 1);
        pointsTop12 = config.getInt("points.top-12", 1);
        pointsTop8 = config.getInt("points.top-8", 1);
        pointsTop5 = config.getInt("points.top-5", 2);
        pointsTop3 = config.getInt("points.top-3", 3);
        pointsTop2 = config.getInt("points.top-2", 3);
        pointsTop1 = config.getInt("points.top-1", 5);

        regenerationNiveau = config.getInt("effets.regeneration-niveau", 2);
        regenerationDureeSecondes = config.getInt("effets.regeneration-duree-secondes", 10);
    }

    private void chargerChancesItems() {
        chancesItems.clear();
        itemsJumpLeague.clear();

        if (config.contains("coffres.chances")) {
            for (String materialName : config.getConfigurationSection("coffres.chances").getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    String chemin = "coffres.chances." + materialName;
                    
                    if (material == Material.POTION && config.contains("coffres.chances.POTION.configurations")) {
                        continue;
                    }
                    
                    if (config.isConfigurationSection(chemin)) {
                        double chance = config.getDouble(chemin + ".chance", 10.0);
                        int quantite = config.getInt(chemin + ".quantite", 1);
                        String nomCustom = config.getString(chemin + ".nom");
                        List<String> loreCustom = config.getStringList(chemin + ".lore");
                        Map<Enchantment, Integer> enchantements = chargerEnchantements(chemin + ".enchantements");
                        List<Short> typesPotion = chargerTypesPotion(chemin + ".types");

                        if (nomCustom != null && !nomCustom.isEmpty()) {
                            TraducteurItems.enregistrerNomPersonnalise(material, nomCustom);
                        }
                        
                        chancesItems.put(material, chance);
                        itemsJumpLeague.put(material, new ItemJumpLeague(material, chance, quantite, nomCustom, loreCustom, enchantements, typesPotion));
                    } else {
                        double chance = config.getDouble(chemin);
                        chancesItems.put(material, chance);
                        itemsJumpLeague.put(material, new ItemJumpLeague(material, chance, 1, null, null, null, null));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material invalide dans la configuration JumpLeague: " + materialName);
                }
            }

            chargerSousConfigurationsPotion("coffres.chances");
        }
    }
    
    private Map<Enchantment, Integer> chargerEnchantements(String chemin) {
        Map<Enchantment, Integer> enchantements = new HashMap<>();
        
        if (config.contains(chemin)) {
            for (String enchantName : config.getConfigurationSection(chemin).getKeys(false)) {
                try {
                    Enchantment enchantement = Enchantment.getByName(enchantName);
                    if (enchantement != null) {
                        int niveau = config.getInt(chemin + "." + enchantName);
                        enchantements.put(enchantement, niveau);
                    } else {
                        plugin.getLogger().warning("Enchantement invalide dans la configuration JumpLeague: " + enchantName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du chargement de l'enchantement " + enchantName + ": " + e.getMessage());
                }
            }
        }
        
        return enchantements;
    }

    private List<Short> chargerTypesPotion(String chemin) {
        List<Short> typesPotion = new ArrayList<>();
        if (config.contains(chemin)) {
            List<Integer> types = config.getIntegerList(chemin);
            for (Integer type : types) {
                typesPotion.add(type.shortValue());
            }
        }
        return typesPotion;
    }

    private void chargerSousConfigurationsPotion(String cheminBase) {
        String cheminPotion = cheminBase + ".POTION";
        
        if (config.contains(cheminPotion + ".configurations")) {
            double chanceMax = 0.0;
            for (String nomCategorie : config.getConfigurationSection(cheminPotion + ".configurations").getKeys(false)) {
                String cheminCategorie = cheminPotion + ".configurations." + nomCategorie;
                
                double chance = config.getDouble(cheminCategorie + ".chance", 10.0);
                int quantite = config.getInt(cheminCategorie + ".quantite", 1);
                String nomCustom = config.getString(cheminCategorie + ".nom", null);
                List<String> loreCustom = config.getStringList(cheminCategorie + ".lore");
                Map<Enchantment, Integer> enchantements = chargerEnchantements(cheminCategorie + ".enchantements");
                List<Short> typesPotion = chargerTypesPotion(cheminCategorie + ".types");
                
                if (chance > 0) {
                    if (chance > chanceMax) {
                        chanceMax = chance;
                        chancesItems.put(Material.POTION, chance);
                        itemsJumpLeague.put(Material.POTION, new ItemJumpLeague(Material.POTION, chance, quantite, nomCustom, loreCustom, enchantements, typesPotion));
                    }
                }
            }
        }
    }

    public void sauvegarder() {
        try {
            config.save(fichierConfig);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de la configuration JumpLeague: " + e.getMessage());
        }
    }

    public void recharger() {
        config = YamlConfiguration.loadConfiguration(fichierConfig);
        chargerConfiguration();
    }

    public int getTailleFileAttente() { return tailleFileAttente; }
    public boolean isLancementAutomatiqueActif() { return lancementAutomatiqueActif; }
    public int getMinimumJoueursLancementAuto() { return minimumJoueursLancementAuto; }
    public int getDelaiLancementAutoSecondes() { return delaiLancementAutoSecondes; }
    public int getDureePhaseJumpMinutes() { return dureePhaseJumpMinutes; }
    public int getDureePhasePvpMinutes() { return dureePhasePvpMinutes; }
    public int getViesPvp() { return viesPvp; }
    public int getTempsAvantBordureMinutes() { return tempsAvantBordureMinutes; }
    public int getVitesseBordureBlocsParSeconde() { return vitesseBordureBlocsParSeconde; }
    public int getDegatsBordureParSeconde() { return degatsBordureParSeconde; }
    public int getDureeRegenerationKillSecondes() { return dureeRegenerationKillSecondes; }

    public int getItemsParCoffre() { return itemsParCoffre; }
    public Map<Material, Double> getChancesItems() { return chancesItems; }
    public Map<Material, ItemJumpLeague> getItemsJumpLeague() { return itemsJumpLeague; }
    public ItemJumpLeague getItemJumpLeague(Material material) { return itemsJumpLeague.get(material); }

    public int getPointsParcoursReussi() { return pointsParcoursReussi; }
    public int getPointsPremierTermine() { return pointsPremierTermine; }
    public int getPointsKill() { return pointsKill; }
    public int getPointsTop12() { return pointsTop12; }
    public int getPointsTop8() { return pointsTop8; }
    public int getPointsTop5() { return pointsTop5; }
    public int getPointsTop3() { return pointsTop3; }
    public int getPointsTop2() { return pointsTop2; }
    public int getPointsTop1() { return pointsTop1; }

    public int getRegenerationNiveau() { return regenerationNiveau; }
    public int getRegenerationDureeSecondes() { return regenerationDureeSecondes; }
}
