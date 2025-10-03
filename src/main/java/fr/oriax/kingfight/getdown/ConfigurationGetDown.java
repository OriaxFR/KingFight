package fr.oriax.kingfight.getdown;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TraducteurItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationGetDown {

    private final KingFight plugin;
    private FileConfiguration config;
    private File fichierConfig;

    private int tailleFileAttente;
    private boolean lancementAutomatiqueActif;
    private int minimumJoueursLancementAuto;
    private int delaiLancementAutoSecondes;
    private int dureeParMapSecondes;
    private int nombreMaps;
    private int dureePhaseStuffSecondes;
    private int dureePhasePvpMinutes;
    private int viesPvp;
    private int tempsAvantBordureMinutes;
    private int vitesseBordureBlocsParSeconde;
    private int degatsBordureParSeconde;
    private int dureeRegenerationKillSecondes;

    private Map<Material, BlocSpecial> blocsSpeciaux;

    private Map<Material, Integer> prixShop;
    private Map<Material, ItemShop> itemsShop;
    private List<ItemShop> itemsShopListe;
    private int prixNiveauExperience;

    private int pointsParcoursReussi;
    private int pointsKingcoins100;
    private int pointsKill;
    private int pointsTop12;
    private int pointsTop8;
    private int pointsTop5;
    private int pointsTop3;
    private int pointsTop2;
    private int pointsTop1;

    private int regenerationNiveau;
    private int regenerationDureeSecondes;

    private Material blocRemplacement;

    public static class BlocSpecial {
        private final String nom;
        private final Material material;
        private final int kingcoinsMin;
        private final int kingcoinsMax;
        private final String message;
        private final List<String> effets;
        private final String son;
        private final float volumeSon;
        private final float pitchSon;

        public BlocSpecial(String nom, Material material, int kingcoinsMin, int kingcoinsMax, String message,
                          List<String> effets, String son, float volumeSon, float pitchSon) {
            this.nom = nom;
            this.material = material;
            this.kingcoinsMin = kingcoinsMin;
            this.kingcoinsMax = kingcoinsMax;
            this.message = message;
            this.effets = effets != null ? effets : new ArrayList<>();
            this.son = son;
            this.volumeSon = volumeSon;
            this.pitchSon = pitchSon;
        }

        public String getNom() { return nom; }
        public Material getMaterial() { return material; }
        public int getKingcoinsMin() { return kingcoinsMin; }
        public int getKingcoinsMax() { return kingcoinsMax; }
        public String getMessage() { return message; }
        public List<String> getEffets() { return effets; }
        public String getSon() { return son; }
        public float getVolumeSon() { return volumeSon; }
        public float getPitchSon() { return pitchSon; }
        
        public int getKingcoinsAleatoire() {
            if (kingcoinsMin == kingcoinsMax) return kingcoinsMin;
            return kingcoinsMin + new java.util.Random().nextInt(kingcoinsMax - kingcoinsMin + 1);
        }
    }

    public ConfigurationGetDown(KingFight plugin) {
        this.plugin = plugin;
        this.prixShop = new HashMap<>();
        this.itemsShop = new HashMap<>();
        this.itemsShopListe = new ArrayList<>();
        this.blocsSpeciaux = new HashMap<>();
        chargerConfiguration();
    }

    private void chargerConfiguration() {
        File dossierGetDown = new File(plugin.getDataFolder(), "getdown");
        if (!dossierGetDown.exists()) {
            dossierGetDown.mkdirs();
        }

        fichierConfig = new File(dossierGetDown, "config.yml");

        if (!fichierConfig.exists()) {
            plugin.saveResource("getdown/config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(fichierConfig);

        tailleFileAttente = config.getInt("partie.taille-file-attente", 16);
        lancementAutomatiqueActif = config.getBoolean("partie.lancement-automatique-actif", true);
        minimumJoueursLancementAuto = config.getInt("partie.minimum-joueurs-lancement-auto", 12);
        delaiLancementAutoSecondes = config.getInt("partie.delai-lancement-auto-secondes", 60);
        dureeParMapSecondes = config.getInt("partie.duree-par-map-secondes", 150);
        nombreMaps = config.getInt("partie.nombre-maps", 3);
        dureePhaseStuffSecondes = config.getInt("partie.duree-phase-stuff-secondes", 90);
        dureePhasePvpMinutes = config.getInt("partie.duree-phase-pvp-minutes", 6);
        viesPvp = config.getInt("partie.vies-pvp", 3);
        tempsAvantBordureMinutes = config.getInt("partie.temps-avant-bordure-minutes", 1);
        vitesseBordureBlocsParSeconde = config.getInt("partie.vitesse-bordure-blocs-par-seconde", 2);
        degatsBordureParSeconde = config.getInt("partie.degats-bordure-par-seconde", 4);
        dureeRegenerationKillSecondes = config.getInt("partie.duree-regeneration-kill-secondes", 10);

        chargerBlocsSpeciaux();
        chargerPrixShop();

        pointsParcoursReussi = config.getInt("points.parcours-reussi", 5);
        pointsKingcoins100 = config.getInt("points.kingcoins-100", 1);
        pointsKill = config.getInt("points.kill", 1);
        pointsTop12 = config.getInt("points.top-12", 1);
        pointsTop8 = config.getInt("points.top-8", 1);
        pointsTop5 = config.getInt("points.top-5", 2);
        pointsTop3 = config.getInt("points.top-3", 3);
        pointsTop2 = config.getInt("points.top-2", 3);
        pointsTop1 = config.getInt("points.top-1", 5);

        regenerationNiveau = config.getInt("effets.regeneration-niveau", 2);
        regenerationDureeSecondes = config.getInt("effets.regeneration-duree-secondes", 10);

        String blocRemplacementNom = config.getString("blocs.bloc-remplacement", "REDSTONE_BLOCK");
        try {
            blocRemplacement = Material.valueOf(blocRemplacementNom);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material invalide pour bloc-remplacement: " + blocRemplacementNom + ", utilisation de REDSTONE_BLOCK par défaut");
            blocRemplacement = Material.REDSTONE_BLOCK;
        }
    }

    private void chargerPrixShop() {
        prixShop.clear();
        itemsShop.clear();
        itemsShopListe.clear();

        if (config.contains("shop.prix")) {
            for (String materialName : config.getConfigurationSection("shop.prix").getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    
                    if (material == Material.POTION && config.contains("shop.prix.POTION.configurations")) {
                        continue;
                    }

                    if (config.isInt("shop.prix." + materialName)) {
                        int prix = config.getInt("shop.prix." + materialName);
                        prixShop.put(material, prix);
                        ItemShop itemShop = new ItemShop(material, prix, null, null, null, (short) 0);
                        itemsShop.put(material, itemShop);
                        itemsShopListe.add(itemShop);
                    } else if (config.isConfigurationSection("shop.prix." + materialName)) {
                        ConfigurationSection section = config.getConfigurationSection("shop.prix." + materialName);
                        int prix = section.getInt("prix", 0);
                        String nomCustom = section.getString("nom", null);
                        List<String> loreCustom = section.getStringList("lore");
                        short durability = (short) section.getInt("durability", 0);

                        Map<Enchantment, Integer> enchantements = new HashMap<>();
                        if (section.contains("enchantements")) {
                            ConfigurationSection enchantementsSection = section.getConfigurationSection("enchantements");
                            if (enchantementsSection != null) {
                                for (String enchantName : enchantementsSection.getKeys(false)) {
                                    try {
                                        Enchantment enchant = Enchantment.getByName(enchantName);
                                        if (enchant != null) {
                                            int niveau = enchantementsSection.getInt(enchantName);
                                            enchantements.put(enchant, niveau);
                                        } else {
                                            plugin.getLogger().warning("Enchantement invalide pour " + materialName + ": " + enchantName);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Erreur lors du chargement de l'enchantement " + enchantName + " pour " + materialName);
                                    }
                                }
                            }
                        }

                        if (nomCustom != null && !nomCustom.isEmpty()) {
                            TraducteurItems.enregistrerNomPersonnalise(material, nomCustom);
                        }

                        ItemShop itemShop = new ItemShop(material, prix, nomCustom, loreCustom, enchantements, durability);
                        itemsShop.put(material, itemShop);
                        itemsShopListe.add(itemShop);
                        prixShop.put(material, prix);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material invalide dans la configuration GetDown: " + materialName);
                }
            }
            
            chargerSousConfigurationsPotionShop();
        }

        prixNiveauExperience = config.getInt("shop.niveau-experience", 5);
    }
    
    private void chargerSousConfigurationsPotionShop() {
        String cheminPotion = "shop.prix.POTION";
        
        if (config.contains(cheminPotion + ".configurations")) {
            for (String nomCategorie : config.getConfigurationSection(cheminPotion + ".configurations").getKeys(false)) {
                String cheminCategorie = cheminPotion + ".configurations." + nomCategorie;
                
                int prix = config.getInt(cheminCategorie + ".prix", 0);
                String nomCustom = config.getString(cheminCategorie + ".nom", null);
                List<String> loreCustom = config.getStringList(cheminCategorie + ".lore");
                short durability = (short) config.getInt(cheminCategorie + ".durability", 0);
                String categorie = config.getString(cheminCategorie + ".categorie", "autres");
                
                Map<Enchantment, Integer> enchantements = new HashMap<>();
                if (config.contains(cheminCategorie + ".enchantements")) {
                    ConfigurationSection enchantementsSection = config.getConfigurationSection(cheminCategorie + ".enchantements");
                    if (enchantementsSection != null) {
                        for (String enchantName : enchantementsSection.getKeys(false)) {
                            try {
                                Enchantment enchant = Enchantment.getByName(enchantName);
                                if (enchant != null) {
                                    int niveau = enchantementsSection.getInt(enchantName);
                                    enchantements.put(enchant, niveau);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Erreur lors du chargement de l'enchantement " + enchantName + " pour potion " + nomCategorie);
                            }
                        }
                    }
                }
                
                ItemShop itemShop = new ItemShop(Material.POTION, prix, nomCustom, loreCustom, enchantements, durability, categorie);
                itemsShopListe.add(itemShop);
                if (!prixShop.containsKey(Material.POTION)) {
                    prixShop.put(Material.POTION, prix);
                }
            }
        }
    }

    private void chargerBlocsSpeciaux() {
        blocsSpeciaux.clear();

        if (!config.contains("blocs-speciaux")) {
            creerBlocsSpeciauxParDefaut();
            return;
        }

        for (String nomBloc : config.getConfigurationSection("blocs-speciaux").getKeys(false)) {
            try {
                String materialName = config.getString("blocs-speciaux." + nomBloc + ".material");
                Material material = Material.valueOf(materialName);

                int kingcoinsMin, kingcoinsMax;
                if (config.contains("blocs-speciaux." + nomBloc + ".kingcoins-min")) {
                    kingcoinsMin = config.getInt("blocs-speciaux." + nomBloc + ".kingcoins-min", 0);
                    kingcoinsMax = config.getInt("blocs-speciaux." + nomBloc + ".kingcoins-max", kingcoinsMin);
                } else {
                    int kingcoins = config.getInt("blocs-speciaux." + nomBloc + ".kingcoins", 0);
                    kingcoinsMin = kingcoins;
                    kingcoinsMax = kingcoins;
                }

                String message = config.getString("blocs-speciaux." + nomBloc + ".message", "§7Bloc special !");
                List<String> effets = config.getStringList("blocs-speciaux." + nomBloc + ".effets-possibles");
                if (effets.isEmpty()) {
                    effets = config.getStringList("blocs-speciaux." + nomBloc + ".effets");
                }
                String son = config.getString("blocs-speciaux." + nomBloc + ".son", "LEVEL_UP");
                float volumeSon = (float) config.getDouble("blocs-speciaux." + nomBloc + ".volume-son", 1.0);
                float pitchSon = (float) config.getDouble("blocs-speciaux." + nomBloc + ".pitch-son", 1.0);

                BlocSpecial blocSpecial = new BlocSpecial(nomBloc, material, kingcoinsMin, kingcoinsMax, message,
                                                        effets, son, volumeSon, pitchSon);
                blocsSpeciaux.put(material, blocSpecial);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material invalide pour le bloc special " + nomBloc + ": " +
                                         config.getString("blocs-speciaux." + nomBloc + ".material"));
            }
        }
    }

    private void creerBlocsSpeciauxParDefaut() {
        List<String> effetsTitane = new ArrayList<>();
        effetsTitane.add("SPEED:2:30");
        effetsTitane.add("JUMP_BOOST:1:45");
        effetsTitane.add("REGENERATION:1:20");

        blocsSpeciaux.put(Material.OBSIDIAN, new BlocSpecial("onyx", Material.OBSIDIAN, 40, 60,
                         "§5Bloc d'Onyx ! +{kingcoins} KingCoins !", new ArrayList<>(), "LEVEL_UP", 1.0f, 2.0f));

        blocsSpeciaux.put(Material.LAPIS_BLOCK, new BlocSpecial("saphir", Material.LAPIS_BLOCK, 8, 12,
                         "§9Bloc de Saphir ! +{kingcoins} KingCoins !", new ArrayList<>(), "ORB_PICKUP", 1.0f, 1.5f));

        blocsSpeciaux.put(Material.IRON_BLOCK, new BlocSpecial("titane", Material.IRON_BLOCK, 0, 0,
                         "§7Bloc de Titane ! Effet aleatoire !", effetsTitane, "FIZZ", 1.0f, 1.0f));
    }

    public void sauvegarder() {
        try {
            config.save(fichierConfig);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de la configuration GetDown: " + e.getMessage());
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
    public int getDureeParMapSecondes() { return dureeParMapSecondes; }
    public int getNombreMaps() { return nombreMaps; }
    public int getDureePhaseStuffSecondes() { return dureePhaseStuffSecondes; }
    public int getDureePhasePvpMinutes() { return dureePhasePvpMinutes; }
    public int getViesPvp() { return viesPvp; }
    public int getTempsAvantBordureMinutes() { return tempsAvantBordureMinutes; }
    public int getVitesseBordureBlocsParSeconde() { return vitesseBordureBlocsParSeconde; }
    public int getDegatsBordureParSeconde() { return degatsBordureParSeconde; }
    public int getDureeRegenerationKillSecondes() { return dureeRegenerationKillSecondes; }

    public Map<Material, BlocSpecial> getBlocsSpeciaux() { return blocsSpeciaux; }
    public BlocSpecial getBlocSpecial(Material material) { return blocsSpeciaux.get(material); }

    public Map<Material, Integer> getPrixShop() { return prixShop; }
    public Map<Material, ItemShop> getItemsShop() { return itemsShop; }
    public List<ItemShop> getItemsShopListe() { return itemsShopListe; }
    public ItemShop getItemShop(Material material) { return itemsShop.get(material); }
    public int getPrixNiveauExperience() { return prixNiveauExperience; }

    public int getPointsParcoursReussi() { return pointsParcoursReussi; }
    public int getPointsKingcoins100() { return pointsKingcoins100; }
    public int getPointsKill() { return pointsKill; }
    public int getPointsTop12() { return pointsTop12; }
    public int getPointsTop8() { return pointsTop8; }
    public int getPointsTop5() { return pointsTop5; }
    public int getPointsTop3() { return pointsTop3; }
    public int getPointsTop2() { return pointsTop2; }
    public int getPointsTop1() { return pointsTop1; }

    public int getRegenerationNiveau() { return regenerationNiveau; }
    public int getRegenerationDureeSecondes() { return regenerationDureeSecondes; }

    public Material getBlocRemplacement() { return blocRemplacement; }
}
