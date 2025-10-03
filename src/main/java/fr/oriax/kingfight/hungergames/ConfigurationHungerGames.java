package fr.oriax.kingfight.hungergames;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TraducteurItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigurationHungerGames {

    private final KingFight plugin;
    private FileConfiguration config;
    private File fichierConfig;

    private int tailleFileAttente;
    private boolean lancementAutomatiqueActif;
    private int minimumJoueursLancementAuto;
    private int delaiLancementAutoSecondes;
    private int dureePartieMax;
    private int tempsInvincibilite;
    private int tempsAvantBordure;
    private int vitesseBordure;
    private int degatsParSecondeBordure;
    private int rayonFinalBordure;
    private int itemsParCoffre;
    private int intervalleColisStrategique;
    private int rayonSpawnColis;
    private Map<Material, Double> chancesItems;
    private List<ItemCoffre> lootCoffres;
    private List<ItemColis> lootColis;

    public ConfigurationHungerGames(KingFight plugin) {
        this.plugin = plugin;
        chargerConfiguration();
    }

    private void chargerConfiguration() {
        File dossierHungerGames = new File(plugin.getDataFolder(), "hungergames");
        if (!dossierHungerGames.exists()) {
            dossierHungerGames.mkdirs();
        }

        fichierConfig = new File(dossierHungerGames, "config.yml");

        if (!fichierConfig.exists()) {
            creerConfigurationParDefaut();
        }

        config = YamlConfiguration.loadConfiguration(fichierConfig);
        chargerValeurs();
    }

    private void creerConfigurationParDefaut() {
        try {
            plugin.saveResource("hungergames/config.yml", false);
            plugin.getLogger().info("Configuration Hunger Games créée avec succès depuis les ressources du plugin");
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible de copier la configuration Hunger Games depuis les ressources: " + e.getMessage());
            creerConfigurationBasique();
        }
    }
    
    private void creerConfigurationBasique() {
        try {
            fichierConfig.createNewFile();
            config = YamlConfiguration.loadConfiguration(fichierConfig);

            config.set("partie.taille-file-attente", 16);
            config.set("partie.lancement-automatique-actif", true);
            config.set("partie.minimum-joueurs-lancement-auto", 12);
            config.set("partie.delai-lancement-auto-secondes", 60);
            config.set("partie.duree-max-minutes", 15);
            config.set("partie.temps-invincibilite-secondes", 30);
            config.set("partie.temps-avant-bordure-minutes", 3);
            config.set("partie.vitesse-bordure-blocs-par-seconde", 2);
            config.set("partie.degats-bordure-par-seconde", 4);
            config.set("partie.rayon-final-bordure", 10);

            config.set("coffres.items-par-coffre", 4);

            config.set("coffres.loot.DIAMOND_SWORD.quantite", 1);
            config.set("coffres.loot.DIAMOND_SWORD.chance", 5.0);
            config.set("coffres.loot.IRON_SWORD.quantite", 1);
            config.set("coffres.loot.IRON_SWORD.chance", 15.0);
            config.set("coffres.loot.STONE_SWORD.quantite", 1);
            config.set("coffres.loot.STONE_SWORD.chance", 25.0);
            config.set("coffres.loot.WOOD_SWORD.quantite", 1);
            config.set("coffres.loot.WOOD_SWORD.chance", 35.0);
            config.set("coffres.loot.BOW.quantite", 1);
            config.set("coffres.loot.BOW.chance", 20.0);
            config.set("coffres.loot.ARROW.quantite", 24);
            config.set("coffres.loot.ARROW.chance", 30.0);
            config.set("coffres.loot.DIAMOND_HELMET.quantite", 1);
            config.set("coffres.loot.DIAMOND_HELMET.chance", 3.0);
            config.set("coffres.loot.DIAMOND_CHESTPLATE.quantite", 1);
            config.set("coffres.loot.DIAMOND_CHESTPLATE.chance", 3.0);
            config.set("coffres.loot.DIAMOND_LEGGINGS.quantite", 1);
            config.set("coffres.loot.DIAMOND_LEGGINGS.chance", 3.0);
            config.set("coffres.loot.DIAMOND_BOOTS.quantite", 1);
            config.set("coffres.loot.DIAMOND_BOOTS.chance", 3.0);
            config.set("coffres.loot.IRON_HELMET.quantite", 1);
            config.set("coffres.loot.IRON_HELMET.chance", 10.0);
            config.set("coffres.loot.IRON_CHESTPLATE.quantite", 1);
            config.set("coffres.loot.IRON_CHESTPLATE.chance", 10.0);
            config.set("coffres.loot.IRON_LEGGINGS.quantite", 1);
            config.set("coffres.loot.IRON_LEGGINGS.chance", 10.0);
            config.set("coffres.loot.IRON_BOOTS.quantite", 1);
            config.set("coffres.loot.IRON_BOOTS.chance", 10.0);
            config.set("coffres.loot.COOKED_BEEF.quantite", 4);
            config.set("coffres.loot.COOKED_BEEF.chance", 40.0);
            config.set("coffres.loot.BREAD.quantite", 3);
            config.set("coffres.loot.BREAD.chance", 35.0);
            config.set("coffres.loot.APPLE.quantite", 2);
            config.set("coffres.loot.APPLE.chance", 30.0);
            config.set("coffres.loot.GOLDEN_APPLE.quantite", 1);
            config.set("coffres.loot.GOLDEN_APPLE.chance", 8.0);
            config.set("coffres.loot.POTION.quantite", 1);
            config.set("coffres.loot.POTION.chance", 15.0);
            config.set("coffres.loot.ENDER_PEARL.quantite", 1);
            config.set("coffres.loot.ENDER_PEARL.chance", 12.0);
            config.set("coffres.loot.FLINT_AND_STEEL.quantite", 1);
            config.set("coffres.loot.FLINT_AND_STEEL.chance", 10.0);
            config.set("coffres.loot.TNT.quantite", 2);
            config.set("coffres.loot.TNT.chance", 8.0);

            config.set("colis.intervalle-minutes", 1);
            config.set("colis.rayon-spawn-colis", 100);

            config.set("colis.loot.DIAMOND_SWORD.quantite", 1);
            config.set("colis.loot.DIAMOND_SWORD.chance", 85.0);
            config.set("colis.loot.BOW.quantite", 1);
            config.set("colis.loot.BOW.chance", 90.0);
            config.set("colis.loot.ARROW.quantite", 32);
            config.set("colis.loot.ARROW.chance", 95.0);
            config.set("colis.loot.DIAMOND_HELMET.quantite", 1);
            config.set("colis.loot.DIAMOND_HELMET.chance", 70.0);
            config.set("colis.loot.DIAMOND_CHESTPLATE.quantite", 1);
            config.set("colis.loot.DIAMOND_CHESTPLATE.chance", 75.0);
            config.set("colis.loot.DIAMOND_LEGGINGS.quantite", 1);
            config.set("colis.loot.DIAMOND_LEGGINGS.chance", 0.0);
            config.set("colis.loot.DIAMOND_BOOTS.quantite", 1);
            config.set("colis.loot.DIAMOND_BOOTS.chance", 0.0);
            config.set("colis.loot.GOLDEN_APPLE.quantite", 3);
            config.set("colis.loot.GOLDEN_APPLE.chance", 100.0);
            config.set("colis.loot.ENDER_PEARL.quantite", 2);
            config.set("colis.loot.ENDER_PEARL.chance", 60.0);
            config.set("colis.loot.POTION.quantite", 2);
            config.set("colis.loot.POTION.chance", 80.0);

            config.set("points.kill", 1);
            config.set("points.top-12", 1);
            config.set("points.top-10", 1);
            config.set("points.top-8", 1);
            config.set("points.top-5", 2);
            config.set("points.top-3", 3);
            config.set("points.top-2", 3);
            config.set("points.top-1", 5);

            config.save(fichierConfig);

        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de creer la configuration Hunger Games basique: " + e.getMessage());
        }
    }

    private void chargerValeurs() {
        tailleFileAttente = config.getInt("partie.taille-file-attente", 16);
        lancementAutomatiqueActif = config.getBoolean("partie.lancement-automatique-actif", true);
        minimumJoueursLancementAuto = config.getInt("partie.minimum-joueurs-lancement-auto", 12);
        delaiLancementAutoSecondes = config.getInt("partie.delai-lancement-auto-secondes", 60);
        dureePartieMax = config.getInt("partie.duree-max-minutes", 15);
        tempsInvincibilite = config.getInt("partie.temps-invincibilite-secondes", 30);
        tempsAvantBordure = config.getInt("partie.temps-avant-bordure-minutes", 3);
        vitesseBordure = config.getInt("partie.vitesse-bordure-blocs-par-seconde", 2);
        degatsParSecondeBordure = config.getInt("partie.degats-bordure-par-seconde", 4);
        rayonFinalBordure = config.getInt("partie.rayon-final-bordure", 10);

        itemsParCoffre = config.getInt("coffres.items-par-coffre", 4);

        chancesItems = new HashMap<>();
        if (config.contains("coffres.chances")) {
            for (String materialName : config.getConfigurationSection("coffres.chances").getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    double chance = config.getDouble("coffres.chances." + materialName);
                    chancesItems.put(material, chance);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material invalide dans la configuration Hunger Games: " + materialName);
                }
            }
        }

        lootCoffres = new ArrayList<>();
        if (config.contains("coffres.loot")) {
            for (String materialName : config.getConfigurationSection("coffres.loot").getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);

                    if (material == Material.POTION && config.contains("coffres.loot.POTION.configurations")) {
                        continue;
                    }

                    if (config.contains("coffres.loot." + materialName + ".quantite") && 
                        config.contains("coffres.loot." + materialName + ".chance")) {
                        
                        int quantite = config.getInt("coffres.loot." + materialName + ".quantite");
                        double chance = config.getDouble("coffres.loot." + materialName + ".chance");
                        String nomCustom = config.getString("coffres.loot." + materialName + ".nom", null);
                        List<String> loreCustom = config.getStringList("coffres.loot." + materialName + ".lore");
                        Map<Enchantment, Integer> enchantements = chargerEnchantements("coffres.loot." + materialName + ".enchantements");
                        List<Short> typesPotion = chargerTypesPotion("coffres.loot." + materialName + ".types");

                        if (nomCustom != null && !nomCustom.isEmpty()) {
                            TraducteurItems.enregistrerNomPersonnalise(material, nomCustom);
                        }
                        
                        if (quantite > 0 && chance > 0) {
                            lootCoffres.add(new ItemCoffre(material, quantite, chance, nomCustom, loreCustom, enchantements, typesPotion));
                        }
                    }
                    else if (config.isInt("coffres.loot." + materialName)) {
                        int quantite = config.getInt("coffres.loot." + materialName);
                        if (quantite > 0) {
                            lootCoffres.add(new ItemCoffre(material, quantite, 100.0));
                        }
                    }
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material invalide dans la configuration des coffres Hunger Games: " + materialName);
                }
            }
            
            chargerSousConfigurationsPotion("coffres.loot", lootCoffres, false);
        }

        intervalleColisStrategique = config.getInt("colis.intervalle-minutes", 1);
        rayonSpawnColis = config.getInt("colis.rayon-spawn-colis", 100);

        lootColis = new ArrayList<>();
        if (config.contains("colis.loot")) {
            for (String materialName : config.getConfigurationSection("colis.loot").getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);

                    if (material == Material.POTION && config.contains("colis.loot.POTION.configurations")) {
                        continue;
                    }

                    if (config.contains("colis.loot." + materialName + ".quantite") && 
                        config.contains("colis.loot." + materialName + ".chance")) {
                        
                        int quantite = config.getInt("colis.loot." + materialName + ".quantite");
                        double chance = config.getDouble("colis.loot." + materialName + ".chance");
                        String nomCustom = config.getString("colis.loot." + materialName + ".nom", null);
                        List<String> loreCustom = config.getStringList("colis.loot." + materialName + ".lore");
                        Map<Enchantment, Integer> enchantements = chargerEnchantements("colis.loot." + materialName + ".enchantements");
                        List<Short> typesPotion = chargerTypesPotion("colis.loot." + materialName + ".types");

                        if (nomCustom != null && !nomCustom.isEmpty()) {
                            TraducteurItems.enregistrerNomPersonnalise(material, nomCustom);
                        }
                        
                        if (quantite > 0 && chance > 0) {
                            lootColis.add(new ItemColis(material, quantite, chance, nomCustom, loreCustom, enchantements, typesPotion));
                        }
                    }
                    else if (config.isInt("colis.loot." + materialName)) {
                        int quantite = config.getInt("colis.loot." + materialName);
                        if (quantite > 0) {
                            lootColis.add(new ItemColis(material, quantite, 100.0));
                        }
                    }
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material invalide dans la configuration des colis Hunger Games: " + materialName);
                }
            }
            
            chargerSousConfigurationsPotion("colis.loot", lootColis, true);
        }
    }

    private Map<Enchantment, Integer> chargerEnchantements(String chemin) {
        Map<Enchantment, Integer> enchantements = new HashMap<>();
        if (config.contains(chemin)) {
            for (String enchantName : config.getConfigurationSection(chemin).getKeys(false)) {
                try {
                    Enchantment enchant = Enchantment.getByName(enchantName.toUpperCase());
                    if (enchant != null) {
                        int niveau = config.getInt(chemin + "." + enchantName);
                        enchantements.put(enchant, niveau);
                    } else {
                        plugin.getLogger().warning("Enchantement invalide: " + enchantName);
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

    @SuppressWarnings("unchecked")
    private void chargerSousConfigurationsPotion(String cheminBase, List<?> listeCible, boolean estColis) {
        String cheminPotion = cheminBase + ".POTION";
        
        if (config.contains(cheminPotion + ".configurations")) {
            for (String nomCategorie : config.getConfigurationSection(cheminPotion + ".configurations").getKeys(false)) {
                String cheminCategorie = cheminPotion + ".configurations." + nomCategorie;
                
                int quantite = config.getInt(cheminCategorie + ".quantite", 1);
                double chance = config.getDouble(cheminCategorie + ".chance", 10.0);
                String nomCustom = config.getString(cheminCategorie + ".nom", null);
                List<String> loreCustom = config.getStringList(cheminCategorie + ".lore");
                Map<Enchantment, Integer> enchantements = chargerEnchantements(cheminCategorie + ".enchantements");
                List<Short> typesPotion = chargerTypesPotion(cheminCategorie + ".types");
                
                if (quantite > 0 && chance > 0) {
                    if (estColis) {
                        ((List<ItemColis>) listeCible).add(new ItemColis(Material.POTION, quantite, chance, nomCustom, loreCustom, enchantements, typesPotion));
                    } else {
                        ((List<ItemCoffre>) listeCible).add(new ItemCoffre(Material.POTION, quantite, chance, nomCustom, loreCustom, enchantements, typesPotion));
                    }
                }
            }
        }
    }

    public void recharger() {
        config = YamlConfiguration.loadConfiguration(fichierConfig);
        chargerValeurs();
    }

    public int getTailleFileAttente() {
        return tailleFileAttente;
    }

    public boolean isLancementAutomatiqueActif() {
        return lancementAutomatiqueActif;
    }

    public int getMinimumJoueursLancementAuto() {
        return minimumJoueursLancementAuto;
    }

    public int getDelaiLancementAutoSecondes() {
        return delaiLancementAutoSecondes;
    }

    public int getDureePartieMax() {
        return dureePartieMax;
    }

    public int getTempsInvincibilite() {
        return tempsInvincibilite;
    }

    public int getTempsAvantBordure() {
        return tempsAvantBordure;
    }

    public int getVitesseBordure() {
        return vitesseBordure;
    }

    public int getDegatsParSecondeBordure() {
        return degatsParSecondeBordure;
    }

    public int getRayonFinalBordure() {
        return rayonFinalBordure;
    }

    public int getItemsParCoffre() {
        return itemsParCoffre;
    }

    public Map<Material, Double> getChancesItems() {
        return chancesItems;
    }

    public int getIntervalleColisStrategique() {
        return intervalleColisStrategique;
    }

    public int getRayonSpawnColis() {
        return rayonSpawnColis;
    }

    public int getPointsKill() {
        return config.getInt("points.kill", 1);
    }

    public int getPointsTop12() {
        return config.getInt("points.top-12", 1);
    }

    public int getPointsTop10() {
        return config.getInt("points.top-10", 1);
    }

    public int getPointsTop8() {
        return config.getInt("points.top-8", 1);
    }

    public int getPointsTop5() {
        return config.getInt("points.top-5", 2);
    }

    public int getPointsTop3() {
        return config.getInt("points.top-3", 3);
    }

    public int getPointsTop2() {
        return config.getInt("points.top-2", 3);
    }

    public int getPointsTop1() {
        return config.getInt("points.top-1", 5);
    }
    
    public List<ItemCoffre> getLootCoffres() {
        return lootCoffres;
    }
    
    public List<ItemColis> getLootColis() {
        return lootColis;
    }
}
