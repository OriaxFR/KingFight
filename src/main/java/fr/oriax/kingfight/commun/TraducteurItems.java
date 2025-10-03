package fr.oriax.kingfight.commun;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
public class TraducteurItems {

    private static final Map<Material, String> TRADUCTIONS_MATERIAUX = new HashMap<>();
    private static final Map<String, String> TRADUCTIONS_ENCHANTEMENTS = new HashMap<>();
    
    private static final Map<Material, String> NOMS_PERSONNALISES = new HashMap<>();

    static {
        initialiserTraductionsMateriaux();
        initialiserTraductionsEnchantements();
    }

    public static void enregistrerNomPersonnalise(Material material, String nomPersonnalise) {
        if (material != null && nomPersonnalise != null && !nomPersonnalise.isEmpty()) {
            String nomFormate = nomPersonnalise.replace("&", "§");
            NOMS_PERSONNALISES.put(material, nomFormate);
        }
    }

    public static void effacerNomsPersonnalises() {
        NOMS_PERSONNALISES.clear();
    }

    public static ItemStack traduireItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        if (!meta.hasDisplayName()) {
            String nomTraduit = traduireMateriau(item.getType(), item.getDurability());
            if (nomTraduit != null) {
                if (nomTraduit.startsWith("§")) {
                    meta.setDisplayName(nomTraduit);
                } else {
                    meta.setDisplayName("§f" + nomTraduit);
                }
            }
        }
        
        if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
            java.util.List<String> lore = new java.util.ArrayList<>();
            
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                String enchantementTraduit = traduireEnchantement(entry.getKey());
                String niveau = getRomanNumeral(entry.getValue());
                lore.add("§7" + enchantementTraduit + " " + niveau);
            }
            
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        
        try {
            item = cacherEnchantementsNBT(item);
        } catch (Exception e) {
        }
        
        return item;
    }

    private static ItemStack cacherEnchantementsNBT(ItemStack item) throws Exception {
        Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack");
        
        Object nmsStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        Object nbtTag = nmsStack.getClass().getMethod("getTag").invoke(nmsStack);
        
        if (nbtTag == null) {
            nbtTag = Class.forName("net.minecraft.server.v1_8_R3.NBTTagCompound").newInstance();
        }
        
        nbtTag.getClass().getMethod("setInt", String.class, int.class).invoke(nbtTag, "HideFlags", 1);
        nmsStack.getClass().getMethod("setTag", nbtTag.getClass()).invoke(nmsStack, nbtTag);
        
        return (ItemStack) craftItemStackClass.getMethod("asBukkitCopy", nmsStack.getClass()).invoke(null, nmsStack);
    }
    
    private static String getRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(number);
        }
    }
    public static String traduireMateriau(Material material, short durability) {
        if (NOMS_PERSONNALISES.containsKey(material)) {
            return NOMS_PERSONNALISES.get(material);
        }
        
        return TRADUCTIONS_MATERIAUX.getOrDefault(material, material.name());
    }
    public static String traduireEnchantement(Enchantment enchantement) {
        String nom = enchantement.getName();
        return TRADUCTIONS_ENCHANTEMENTS.getOrDefault(nom, nom);
    }

    private static void initialiserTraductionsMateriaux() {
        TRADUCTIONS_MATERIAUX.put(Material.WOOD_SWORD, "Épée en bois");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_SWORD, "Épée en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_SWORD, "Épée en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_SWORD, "Épée en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_SWORD, "Épée en diamant");
        
        TRADUCTIONS_MATERIAUX.put(Material.WOOD_AXE, "Hache en bois");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_AXE, "Hache en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_AXE, "Hache en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_AXE, "Hache en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_AXE, "Hache en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.WOOD_PICKAXE, "Pioche en bois");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_PICKAXE, "Pioche en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_PICKAXE, "Pioche en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_PICKAXE, "Pioche en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_PICKAXE, "Pioche en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.WOOD_SPADE, "Pelle en bois");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_SPADE, "Pelle en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_SPADE, "Pelle en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_SPADE, "Pelle en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_SPADE, "Pelle en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.WOOD_HOE, "Houe en bois");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_HOE, "Houe en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_HOE, "Houe en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_HOE, "Houe en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_HOE, "Houe en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.LEATHER_HELMET, "Casque en cuir");
        TRADUCTIONS_MATERIAUX.put(Material.CHAINMAIL_HELMET, "Casque en cotte de mailles");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_HELMET, "Casque en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_HELMET, "Casque en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_HELMET, "Casque en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.LEATHER_CHESTPLATE, "Plastron en cuir");
        TRADUCTIONS_MATERIAUX.put(Material.CHAINMAIL_CHESTPLATE, "Plastron en cotte de mailles");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_CHESTPLATE, "Plastron en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_CHESTPLATE, "Plastron en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_CHESTPLATE, "Plastron en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.LEATHER_LEGGINGS, "Jambières en cuir");
        TRADUCTIONS_MATERIAUX.put(Material.CHAINMAIL_LEGGINGS, "Jambières en cotte de mailles");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_LEGGINGS, "Jambières en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_LEGGINGS, "Jambières en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_LEGGINGS, "Jambières en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.LEATHER_BOOTS, "Bottes en cuir");
        TRADUCTIONS_MATERIAUX.put(Material.CHAINMAIL_BOOTS, "Bottes en cotte de mailles");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_BOOTS, "Bottes en fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_BOOTS, "Bottes en or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND_BOOTS, "Bottes en diamant");

        TRADUCTIONS_MATERIAUX.put(Material.BOW, "Arc");
        TRADUCTIONS_MATERIAUX.put(Material.ARROW, "Flèche");

        TRADUCTIONS_MATERIAUX.put(Material.APPLE, "Pomme");
        TRADUCTIONS_MATERIAUX.put(Material.GOLDEN_APPLE, "Pomme dorée");
        TRADUCTIONS_MATERIAUX.put(Material.BREAD, "Pain");
        TRADUCTIONS_MATERIAUX.put(Material.COOKED_BEEF, "Steak");
        TRADUCTIONS_MATERIAUX.put(Material.COOKED_CHICKEN, "Poulet rôti");
        TRADUCTIONS_MATERIAUX.put(Material.GRILLED_PORK, "Côtelette de porc cuite");
        TRADUCTIONS_MATERIAUX.put(Material.COOKED_FISH, "Poisson cuit");
        TRADUCTIONS_MATERIAUX.put(Material.BAKED_POTATO, "Pomme de terre cuite");
        TRADUCTIONS_MATERIAUX.put(Material.CARROT, "Carotte");
        TRADUCTIONS_MATERIAUX.put(Material.GOLDEN_CARROT, "Carotte dorée");
        TRADUCTIONS_MATERIAUX.put(Material.MELON, "Tranche de pastèque");
        TRADUCTIONS_MATERIAUX.put(Material.MUSHROOM_SOUP, "Soupe aux champignons");
        TRADUCTIONS_MATERIAUX.put(Material.COOKIE, "Cookie");
        TRADUCTIONS_MATERIAUX.put(Material.CAKE, "Gâteau");

        TRADUCTIONS_MATERIAUX.put(Material.POTION, "Potion");

        TRADUCTIONS_MATERIAUX.put(Material.STONE, "Pierre");
        TRADUCTIONS_MATERIAUX.put(Material.COBBLESTONE, "Pierre");
        TRADUCTIONS_MATERIAUX.put(Material.DIRT, "Terre");
        TRADUCTIONS_MATERIAUX.put(Material.GRASS, "Bloc d'herbe");
        TRADUCTIONS_MATERIAUX.put(Material.WOOD, "Bûche de chêne");
        TRADUCTIONS_MATERIAUX.put(Material.LOG, "Bûche");
        TRADUCTIONS_MATERIAUX.put(Material.SAND, "Sable");
        TRADUCTIONS_MATERIAUX.put(Material.GRAVEL, "Gravier");
        TRADUCTIONS_MATERIAUX.put(Material.GLASS, "Verre");
        TRADUCTIONS_MATERIAUX.put(Material.BRICK, "Brique");
        TRADUCTIONS_MATERIAUX.put(Material.OBSIDIAN, "Obsidienne");
        TRADUCTIONS_MATERIAUX.put(Material.BEDROCK, "Bedrock");
        TRADUCTIONS_MATERIAUX.put(Material.SANDSTONE, "Grès");
        TRADUCTIONS_MATERIAUX.put(Material.NETHERRACK, "Netherrack");
        TRADUCTIONS_MATERIAUX.put(Material.SOUL_SAND, "Sable des âmes");
        TRADUCTIONS_MATERIAUX.put(Material.GLOWSTONE, "Pierre lumineuse");
        TRADUCTIONS_MATERIAUX.put(Material.ENDER_STONE, "Pierre de l'End");

        TRADUCTIONS_MATERIAUX.put(Material.WOOL, "Laine blanche");

        TRADUCTIONS_MATERIAUX.put(Material.COAL, "Charbon");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_INGOT, "Lingot de fer");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_INGOT, "Lingot d'or");
        TRADUCTIONS_MATERIAUX.put(Material.DIAMOND, "Diamant");
        TRADUCTIONS_MATERIAUX.put(Material.EMERALD, "Émeraude");
        TRADUCTIONS_MATERIAUX.put(Material.REDSTONE, "Poudre de redstone");
        TRADUCTIONS_MATERIAUX.put(Material.QUARTZ, "Quartz du Nether");

        TRADUCTIONS_MATERIAUX.put(Material.STICK, "Bâton");
        TRADUCTIONS_MATERIAUX.put(Material.BOWL, "Bol");
        TRADUCTIONS_MATERIAUX.put(Material.STRING, "Ficelle");
        TRADUCTIONS_MATERIAUX.put(Material.FEATHER, "Plume");
        TRADUCTIONS_MATERIAUX.put(Material.SULPHUR, "Poudre à canon");
        TRADUCTIONS_MATERIAUX.put(Material.WHEAT, "Blé");
        TRADUCTIONS_MATERIAUX.put(Material.FLINT, "Silex");
        TRADUCTIONS_MATERIAUX.put(Material.BUCKET, "Seau");
        TRADUCTIONS_MATERIAUX.put(Material.WATER_BUCKET, "Seau d'eau");
        TRADUCTIONS_MATERIAUX.put(Material.LAVA_BUCKET, "Seau de lave");
        TRADUCTIONS_MATERIAUX.put(Material.MILK_BUCKET, "Seau de lait");
        TRADUCTIONS_MATERIAUX.put(Material.SNOW_BALL, "Boule de neige");
        TRADUCTIONS_MATERIAUX.put(Material.LEATHER, "Cuir");
        TRADUCTIONS_MATERIAUX.put(Material.CLAY_BALL, "Boule d'argile");
        TRADUCTIONS_MATERIAUX.put(Material.PAPER, "Papier");
        TRADUCTIONS_MATERIAUX.put(Material.BOOK, "Livre");
        TRADUCTIONS_MATERIAUX.put(Material.SLIME_BALL, "Boule de slime");
        TRADUCTIONS_MATERIAUX.put(Material.EGG, "Œuf");
        TRADUCTIONS_MATERIAUX.put(Material.COMPASS, "Boussole");
        TRADUCTIONS_MATERIAUX.put(Material.FISHING_ROD, "Canne à pêche");
        TRADUCTIONS_MATERIAUX.put(Material.WATCH, "Montre");
        TRADUCTIONS_MATERIAUX.put(Material.BONE, "Os");
        TRADUCTIONS_MATERIAUX.put(Material.ENDER_PEARL, "Perle de l'End");
        TRADUCTIONS_MATERIAUX.put(Material.BLAZE_ROD, "Bâton de blaze");
        TRADUCTIONS_MATERIAUX.put(Material.GHAST_TEAR, "Larme de ghast");
        TRADUCTIONS_MATERIAUX.put(Material.NETHER_STAR, "Étoile du Nether");
        TRADUCTIONS_MATERIAUX.put(Material.ENDER_CHEST, "Coffre de l'End");
        TRADUCTIONS_MATERIAUX.put(Material.CHEST, "Coffre");
        TRADUCTIONS_MATERIAUX.put(Material.TRAPPED_CHEST, "Coffre piégé");

        TRADUCTIONS_MATERIAUX.put(Material.TNT, "TNT");
        TRADUCTIONS_MATERIAUX.put(Material.LADDER, "Échelle");
        TRADUCTIONS_MATERIAUX.put(Material.TORCH, "Torche");
        TRADUCTIONS_MATERIAUX.put(Material.WORKBENCH, "Établi");
        TRADUCTIONS_MATERIAUX.put(Material.FURNACE, "Fourneau");
        TRADUCTIONS_MATERIAUX.put(Material.ANVIL, "Enclume");
        TRADUCTIONS_MATERIAUX.put(Material.ENCHANTMENT_TABLE, "Table d'enchantement");
        TRADUCTIONS_MATERIAUX.put(Material.BREWING_STAND, "Alambic");
        TRADUCTIONS_MATERIAUX.put(Material.CAULDRON, "Chaudron");

        TRADUCTIONS_MATERIAUX.put(Material.REDSTONE_TORCH_ON, "Torche de redstone");
        TRADUCTIONS_MATERIAUX.put(Material.LEVER, "Levier");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_BUTTON, "Bouton en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.WOOD_BUTTON, "Bouton en bois");
        TRADUCTIONS_MATERIAUX.put(Material.STONE_PLATE, "Plaque de pression en pierre");
        TRADUCTIONS_MATERIAUX.put(Material.WOOD_PLATE, "Plaque de pression en bois");
        TRADUCTIONS_MATERIAUX.put(Material.GOLD_PLATE, "Plaque de pression en or");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_PLATE, "Plaque de pression en fer");

        TRADUCTIONS_MATERIAUX.put(Material.WOOD_DOOR, "Porte en bois");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_DOOR, "Porte en fer");
        TRADUCTIONS_MATERIAUX.put(Material.TRAP_DOOR, "Trappe");
        TRADUCTIONS_MATERIAUX.put(Material.IRON_TRAPDOOR, "Trappe en fer");

        TRADUCTIONS_MATERIAUX.put(Material.RAILS, "Rails");
        TRADUCTIONS_MATERIAUX.put(Material.POWERED_RAIL, "Rails de propulsion");
        TRADUCTIONS_MATERIAUX.put(Material.DETECTOR_RAIL, "Rails détecteurs");
        TRADUCTIONS_MATERIAUX.put(Material.ACTIVATOR_RAIL, "Rails activateurs");

        TRADUCTIONS_MATERIAUX.put(Material.MINECART, "Wagonnet");
        TRADUCTIONS_MATERIAUX.put(Material.BOAT, "Bateau");

        TRADUCTIONS_MATERIAUX.put(Material.RECORD_3, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_4, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_5, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_6, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_7, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_8, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_9, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_10, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_11, "Disque de musique");
        TRADUCTIONS_MATERIAUX.put(Material.RECORD_12, "Disque de musique");
    }

    private static void initialiserTraductionsEnchantements() {

        TRADUCTIONS_ENCHANTEMENTS.put("DAMAGE_ALL", "Tranchant");
        TRADUCTIONS_ENCHANTEMENTS.put("SHARPNESS", "Tranchant");
        TRADUCTIONS_ENCHANTEMENTS.put("FIRE_ASPECT", "Aura de feu");
        TRADUCTIONS_ENCHANTEMENTS.put("KNOCKBACK", "Recul");
        TRADUCTIONS_ENCHANTEMENTS.put("LOOT_BONUS_MOBS", "Butin");
        TRADUCTIONS_ENCHANTEMENTS.put("LOOTING", "Butin");
        TRADUCTIONS_ENCHANTEMENTS.put("DAMAGE_UNDEAD", "Châtiment");
        TRADUCTIONS_ENCHANTEMENTS.put("SMITE", "Châtiment");
        TRADUCTIONS_ENCHANTEMENTS.put("DAMAGE_ARTHROPODS", "Fléau des arthropodes");
        TRADUCTIONS_ENCHANTEMENTS.put("BANE_OF_ARTHROPODS", "Fléau des arthropodes");

        TRADUCTIONS_ENCHANTEMENTS.put("PROTECTION_ENVIRONMENTAL", "Protection");
        TRADUCTIONS_ENCHANTEMENTS.put("PROTECTION", "Protection");
        TRADUCTIONS_ENCHANTEMENTS.put("DURABILITY", "Solidité");
        TRADUCTIONS_ENCHANTEMENTS.put("UNBREAKING", "Solidité");
        TRADUCTIONS_ENCHANTEMENTS.put("THORNS", "Épines");
        TRADUCTIONS_ENCHANTEMENTS.put("PROTECTION_FIRE", "Protection contre le feu");
        TRADUCTIONS_ENCHANTEMENTS.put("FIRE_PROTECTION", "Protection contre le feu");
        TRADUCTIONS_ENCHANTEMENTS.put("PROTECTION_EXPLOSIONS", "Protection contre les explosions");
        TRADUCTIONS_ENCHANTEMENTS.put("BLAST_PROTECTION", "Protection contre les explosions");
        TRADUCTIONS_ENCHANTEMENTS.put("PROTECTION_PROJECTILE", "Protection contre les projectiles");
        TRADUCTIONS_ENCHANTEMENTS.put("PROJECTILE_PROTECTION", "Protection contre les projectiles");
        TRADUCTIONS_ENCHANTEMENTS.put("PROTECTION_FALL", "Protection contre les chutes");
        TRADUCTIONS_ENCHANTEMENTS.put("FEATHER_FALLING", "Protection contre les chutes");

        TRADUCTIONS_ENCHANTEMENTS.put("OXYGEN", "Respiration");
        TRADUCTIONS_ENCHANTEMENTS.put("RESPIRATION", "Respiration");
        TRADUCTIONS_ENCHANTEMENTS.put("WATER_WORKER", "Affinité aquatique");
        TRADUCTIONS_ENCHANTEMENTS.put("AQUA_AFFINITY", "Affinité aquatique");

        TRADUCTIONS_ENCHANTEMENTS.put("ARROW_DAMAGE", "Puissance");
        TRADUCTIONS_ENCHANTEMENTS.put("POWER", "Puissance");
        TRADUCTIONS_ENCHANTEMENTS.put("ARROW_FIRE", "Flamme");
        TRADUCTIONS_ENCHANTEMENTS.put("FLAME", "Flamme");
        TRADUCTIONS_ENCHANTEMENTS.put("ARROW_INFINITE", "Infinité");
        TRADUCTIONS_ENCHANTEMENTS.put("INFINITY", "Infinité");
        TRADUCTIONS_ENCHANTEMENTS.put("ARROW_KNOCKBACK", "Frappe");
        TRADUCTIONS_ENCHANTEMENTS.put("PUNCH", "Frappe");

        TRADUCTIONS_ENCHANTEMENTS.put("DIG_SPEED", "Efficacité");
        TRADUCTIONS_ENCHANTEMENTS.put("EFFICIENCY", "Efficacité");
        TRADUCTIONS_ENCHANTEMENTS.put("SILK_TOUCH", "Toucher de soie");
        TRADUCTIONS_ENCHANTEMENTS.put("LOOT_BONUS_BLOCKS", "Fortune");
        TRADUCTIONS_ENCHANTEMENTS.put("FORTUNE", "Fortune");

        TRADUCTIONS_ENCHANTEMENTS.put("LUCK", "Chance de la mer");
        TRADUCTIONS_ENCHANTEMENTS.put("LUCK_OF_THE_SEA", "Chance de la mer");
        TRADUCTIONS_ENCHANTEMENTS.put("LURE", "Appât");

        TRADUCTIONS_ENCHANTEMENTS.put("MENDING", "Raccommodage");
        TRADUCTIONS_ENCHANTEMENTS.put("VANISHING_CURSE", "Malédiction de disparition");
        TRADUCTIONS_ENCHANTEMENTS.put("BINDING_CURSE", "Malédiction de liaison");
    }
}