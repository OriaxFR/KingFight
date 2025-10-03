package fr.oriax.kingfight.getdown;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemShop {
    private final Material material;
    private final int prix;
    private final String nomCustom;
    private final List<String> loreCustom;
    private final Map<Enchantment, Integer> enchantements;
    private final short durability;
    private final String categorie;

    public ItemShop(Material material, int prix, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements, short durability, String categorie) {
        this.material = material;
        this.prix = prix;
        this.nomCustom = nomCustom;
        this.loreCustom = loreCustom != null ? new ArrayList<>(loreCustom) : new ArrayList<>();
        this.enchantements = enchantements != null ? new HashMap<>(enchantements) : new HashMap<>();
        this.durability = durability;
        this.categorie = categorie;
    }
    
    public ItemShop(Material material, int prix, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements, short durability) {
        this(material, prix, nomCustom, loreCustom, enchantements, durability, null);
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrix() {
        return prix;
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

    public short getDurability() {
        return durability;
    }

    public String getCategorie() {
        return categorie;
    }

    @Override
    public String toString() {
        return "ItemShop{" +
                "material=" + material +
                ", prix=" + prix +
                ", nomCustom='" + nomCustom + '\'' +
                ", loreCustom=" + loreCustom +
                ", enchantements=" + enchantements +
                ", durability=" + durability +
                ", categorie='" + categorie + '\'' +
                '}';
    }
}