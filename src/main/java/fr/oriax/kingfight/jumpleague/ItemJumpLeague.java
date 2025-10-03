package fr.oriax.kingfight.jumpleague;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemJumpLeague {
    private final Material material;
    private final double chance;
    private final int quantite;
    private final String nomCustom;
    private final List<String> loreCustom;
    private final Map<Enchantment, Integer> enchantements;
    private final List<Short> typesPotion;

    public ItemJumpLeague(Material material, double chance, int quantite, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements, List<Short> typesPotion) {
        this.material = material;
        this.chance = chance;
        this.quantite = quantite;
        this.nomCustom = nomCustom;
        this.loreCustom = loreCustom != null ? new ArrayList<>(loreCustom) : new ArrayList<>();
        this.enchantements = enchantements != null ? new HashMap<>(enchantements) : new HashMap<>();
        this.typesPotion = typesPotion != null ? new ArrayList<>(typesPotion) : new ArrayList<>();
    }
    
    public ItemJumpLeague(Material material, double chance, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements, List<Short> typesPotion) {
        this(material, chance, 1, nomCustom, loreCustom, enchantements, typesPotion);
    }
    
    public ItemJumpLeague(Material material, double chance, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements) {
        this(material, chance, 1, nomCustom, loreCustom, enchantements, null);
    }
    
    public ItemJumpLeague(Material material, double chance) {
        this(material, chance, 1, null, null, null, null);
    }

    public Material getMaterial() {
        return material;
    }

    public double getChance() {
        return chance;
    }

    public int getQuantite() {
        return quantite;
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
        return "ItemJumpLeague{" +
                "material=" + material +
                ", chance=" + chance +
                ", quantite=" + quantite +
                ", nomCustom='" + nomCustom + '\'' +
                ", loreCustom=" + loreCustom +
                ", enchantements=" + enchantements +
                ", typesPotion=" + typesPotion +
                '}';
    }
}