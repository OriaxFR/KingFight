package fr.oriax.kingfight.hungergames;

import fr.oriax.kingfight.commun.TraducteurItems;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GestionnaireCoffresHungerGames {

    private static final int TAILLE_CHUNK = 16;
    private static final int HAUTEUR_MONDE_MAX = 256;
    private static final short[] POTIONS_DISPONIBLES = {
        8193, 8194, 8195, 8196, 8197, 8201, 8202, 8204, 8205, 8206, 8207, 8208, 8209, 8210, 8211, 8212, 8213, 8214, 8215, 8216, 8217, 8218, 8219, 8220, 8221, 8222, 8223, 8224, 8225, 8226, 8227, 8228, 8229, 8230, 8231, 8232, 8233, 8234, 8235, 8236, 8237, 8238, 8239, 8240, 8241, 8242, 8243, 8244, 8245, 8246, 8247, 8248, 8249, 8250, 8251, 8252, 8253, 8254, 8255, 8256, 8257, 8258, 8259, 8260, 8261, 8262, 8263, 8264, 8265, 8266, 8267
    };

    public static void remplirCoffres(World monde, ConfigurationHungerGames config, GestionnaireSetupHungerGames gestionnaireSetup) {
        Location centreBordure = gestionnaireSetup.getCentreBordure(monde);
        int rayonBordure = gestionnaireSetup.getRayonBordure(monde);
        
        chargerChunksDansZone(monde, centreBordure, rayonBordure);
        
        List<Location> coffres = trouverCoffresDansZone(monde, centreBordure, rayonBordure);

        for (Location locationCoffre : coffres) {
            Block block = locationCoffre.getBlock();
            if (block.getType() == Material.CHEST) {
                Chest coffre = (Chest) block.getState();
                remplirCoffre(coffre.getInventory(), config);
            }
        }
    }

    public static void remplirCoffresSpecifiques(List<Location> coffres, ConfigurationHungerGames config) {
        for (Location locationCoffre : coffres) {
            Block block = locationCoffre.getBlock();
            if (block.getType() == Material.CHEST) {
                Chest coffre = (Chest) block.getState();
                remplirCoffre(coffre.getInventory(), config);
            }
        }
    }

    private static void chargerChunksDansZone(World monde, Location centre, int rayon) {
        int chunkCentreX = centre.getBlockX() >> 4;
        int chunkCentreZ = centre.getBlockZ() >> 4;
        int rayonChunks = (rayon >> 4) + 1;
        
        for (int chunkX = chunkCentreX - rayonChunks; chunkX <= chunkCentreX + rayonChunks; chunkX++) {
            for (int chunkZ = chunkCentreZ - rayonChunks; chunkZ <= chunkCentreZ + rayonChunks; chunkZ++) {
                monde.loadChunk(chunkX, chunkZ);
            }
        }
    }

    private static List<Location> trouverCoffresDansZone(World monde, Location centre, int rayon) {
        List<Location> coffres = new ArrayList<>();
        int chunkCentreX = centre.getBlockX() >> 4;
        int chunkCentreZ = centre.getBlockZ() >> 4;
        int rayonChunks = (rayon >> 4) + 1;
        
        for (int chunkX = chunkCentreX - rayonChunks; chunkX <= chunkCentreX + rayonChunks; chunkX++) {
            for (int chunkZ = chunkCentreZ - rayonChunks; chunkZ <= chunkCentreZ + rayonChunks; chunkZ++) {
                Chunk chunk = monde.getChunkAt(chunkX, chunkZ);
                
                for (int x = 0; x < TAILLE_CHUNK; x++) {
                    for (int z = 0; z < TAILLE_CHUNK; z++) {
                        for (int y = 0; y < HAUTEUR_MONDE_MAX; y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (block.getType() == Material.CHEST) {
                                Location locationCoffre = block.getLocation();
                                double distance = locationCoffre.distance(centre);
                                if (distance <= rayon) {
                                    coffres.add(locationCoffre);
                                }
                            }
                        }
                    }
                }
            }
        }

        return coffres;
    }

    private static void remplirCoffre(Inventory inventaire, ConfigurationHungerGames config) {
        inventaire.clear();

        List<ItemStack> itemsDisponibles;

        if (config.getLootCoffres() != null && !config.getLootCoffres().isEmpty()) {
            itemsDisponibles = genererItemsHybrides(config);
        } else {
            itemsDisponibles = genererItemsDisponibles(config);
        }
        
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

    private static List<ItemStack> genererItemsDisponibles(ConfigurationHungerGames config) {
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
    private static List<ItemStack> genererItemsHybrides(ConfigurationHungerGames config) {
        List<ItemStack> items = new ArrayList<>();
        Random random = new Random();
        List<ItemCoffre> lootCoffres = config.getLootCoffres();
        
        if (lootCoffres == null || lootCoffres.isEmpty()) {
            return items;
        }

        int itemsParCoffre = config.getItemsParCoffre();
        int tentativesMax = itemsParCoffre * 3;
        int tentatives = 0;

        for (ItemCoffre itemCoffre : lootCoffres) {
            if (items.size() >= itemsParCoffre) {
                break;
            }
            
            if (random.nextDouble() * 100 < itemCoffre.getChance()) {
                ItemStack item = creerItemAvecQuantiteEtTypes(itemCoffre.getMaterial(), itemCoffre.getQuantite(), itemCoffre.getTypesPotion());
                if (item != null) {
                    appliquerPersonnalisation(item, itemCoffre.getNomCustom(), itemCoffre.getLoreCustom(), itemCoffre.getEnchantements());
                    items.add(item);
                }
            }
        }

        while (items.size() < itemsParCoffre && tentatives < tentativesMax) {
            ItemCoffre itemCoffre = lootCoffres.get(random.nextInt(lootCoffres.size()));
            ItemStack item = creerItemAvecQuantiteEtTypes(itemCoffre.getMaterial(), itemCoffre.getQuantite(), itemCoffre.getTypesPotion());
            if (item != null) {
                appliquerPersonnalisation(item, itemCoffre.getNomCustom(), itemCoffre.getLoreCustom(), itemCoffre.getEnchantements());
                items.add(item);
            }
            tentatives++;
        }

        return items;
    }

    private static ItemStack creerItem(Material material) {
        switch (material) {
            case ARROW:
                return new ItemStack(material, 16 + new Random().nextInt(17));
            case COOKED_BEEF:
            case BREAD:
                return new ItemStack(material, 2 + new Random().nextInt(4));
            case APPLE:
                return new ItemStack(material, 1 + new Random().nextInt(3));
            case GOLDEN_APPLE:
                return new ItemStack(material, 1);
            case POTION:
                return creerPotionAleatoire();
            case TNT:
                return new ItemStack(material, 1 + new Random().nextInt(3));
            default:
                return new ItemStack(material, 1);
        }
    }

    private static ItemStack creerPotionAleatoire() {
        Random random = new Random();
        short potionData = POTIONS_DISPONIBLES[random.nextInt(POTIONS_DISPONIBLES.length)];
        return new ItemStack(Material.POTION, 1, potionData);
    }

    private static ItemStack creerItemAvecQuantite(Material material, int quantite) {
        switch (material) {
            case POTION:
                if (quantite == 1) {
                    return creerPotionAleatoire();
                } else {
                    ItemStack potion = creerPotionAleatoire();
                    potion.setAmount(quantite);
                    return potion;
                }
            default:
                return new ItemStack(material, quantite);
        }
    }

    private static ItemStack creerItemAvecQuantiteEtTypes(Material material, int quantite, List<Short> typesPotion) {
        switch (material) {
            case POTION:
                if (quantite == 1) {
                    return creerPotionAvecTypes(typesPotion);
                } else {
                    ItemStack potion = creerPotionAvecTypes(typesPotion);
                    potion.setAmount(quantite);
                    return potion;
                }
            default:
                return new ItemStack(material, quantite);
        }
    }

    private static ItemStack creerPotionAvecTypes(List<Short> typesPotion) {
        if (typesPotion == null || typesPotion.isEmpty()) {
            return creerPotionAleatoire();
        }
        Random random = new Random();
        short potionData = typesPotion.get(random.nextInt(typesPotion.size()));
        return new ItemStack(Material.POTION, 1, potionData);
    }

    public static void creerColisStrategique(Location location, ConfigurationHungerGames config) {
        Block block = location.getBlock();
        block.setType(Material.CHEST);

        Chest coffre = (Chest) block.getState();
        Inventory inventaire = coffre.getInventory();

        List<ItemStack> itemsPremium = genererItemsPremium(config);

        List<Integer> slotsDisponibles = new ArrayList<>();
        for (int i = 0; i < inventaire.getSize(); i++) {
            slotsDisponibles.add(i);
        }
        Collections.shuffle(slotsDisponibles);

        for (int i = 0; i < itemsPremium.size() && i < slotsDisponibles.size(); i++) {
            ItemStack item = itemsPremium.get(i);
            item = TraducteurItems.traduireItem(item);
            int slot = slotsDisponibles.get(i);
            inventaire.setItem(slot, item);
        }

        coffre.update();
    }

    private static List<ItemStack> genererItemsPremium(ConfigurationHungerGames config) {
        List<ItemStack> items = new ArrayList<>();
        Random random = new Random();

        List<ItemColis> lootColis = config.getLootColis();
        
        for (ItemColis itemColis : lootColis) {
            Material material = itemColis.getMaterial();
            int quantite = itemColis.getQuantite();
            double chance = itemColis.getChance();
            List<Short> typesPotion = itemColis.getTypesPotion();

            if (random.nextDouble() * 100 < chance) {
                ItemStack item;
                if (material == Material.POTION) {
                    for (int i = 0; i < quantite; i++) {
                        items.add(creerPotionAvecTypes(typesPotion));
                    }
                    continue;
                } else {
                    item = new ItemStack(material, quantite);
                }
                appliquerPersonnalisation(item, itemColis.getNomCustom(), itemColis.getLoreCustom(), itemColis.getEnchantements());
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.DIAMOND_SWORD)));
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.DIAMOND_HELMET)));
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.DIAMOND_CHESTPLATE)));
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.GOLDEN_APPLE, 3)));
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.ENDER_PEARL, 2)));
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.BOW)));
            items.add(TraducteurItems.traduireItem(new ItemStack(Material.ARROW, 32)));
        }

        return items;
    }

    private static void appliquerPersonnalisation(ItemStack item, String nomCustom, List<String> loreCustom, Map<org.bukkit.enchantments.Enchantment, Integer> enchantements) {
        if (item == null) return;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (nomCustom != null && !nomCustom.isEmpty()) {
                meta.setDisplayName(nomCustom.replace("&", "§"));
            }

            if (loreCustom != null && !loreCustom.isEmpty()) {
                List<String> loreFormate = new ArrayList<>();
                for (String ligne : loreCustom) {
                    loreFormate.add(ligne.replace("&", "§"));
                }
                meta.setLore(loreFormate);
            }

            item.setItemMeta(meta);
        }

        if (enchantements != null && !enchantements.isEmpty()) {
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantements.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }
    }
}
