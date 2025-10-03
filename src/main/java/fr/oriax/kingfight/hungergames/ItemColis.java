package fr.oriax.kingfight.hungergames;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemColis {
    
    private final Material material;
    private final int quantite;
    private final double chance;
    private final String nomCustom;
    private final List<String> loreCustom;
    private final Map<Enchantment, Integer> enchantements;
    private final List<Short> typesPotion;
    
    public ItemColis(Material material, int quantite, double chance, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements, List<Short> typesPotion) {
        this.material = material;
        this.quantite = quantite;
        this.chance = chance;
        this.nomCustom = nomCustom;
        this.loreCustom = loreCustom != null ? new ArrayList<>(loreCustom) : new ArrayList<>();
        this.enchantements = enchantements != null ? new HashMap<>(enchantements) : new HashMap<>();
        this.typesPotion = typesPotion != null ? new ArrayList<>(typesPotion) : new ArrayList<>();
    }
    
    public ItemColis(Material material, int quantite, double chance, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements) {
        this(material, quantite, chance, nomCustom, loreCustom, enchantements, null);
    }
    
    public ItemColis(Material material, int quantite, double chance) {
        this(material, quantite, chance, null, null, null, null);
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public int getQuantite() {
        return quantite;
    }
    
    public double getChance() {
        return chance;
    }

    public String getNomCustom() {
        return nomCustom;
    }

    public List<String> getLoreCustom() {
        return new ArrayList<>(loreCustom);
    }

    public Map<Enchantment, Integer> getEnchantements() {
        return new HashMap<>(enchantements);
    }
    
    public List<Short> getTypesPotion() {
        return new ArrayList<>(typesPotion);
    }
    
    @Override
    public String toString() {
        return "ItemColis{" +
                "material=" + material +
                ", quantite=" + quantite +
                ", chance=" + chance +
                ", nomCustom='" + nomCustom + '\'' +
                ", loreCustom=" + loreCustom +
                ", enchantements=" + enchantements +
                ", typesPotion=" + typesPotion +
                '}';
    }
}