package fr.oriax.kingfight.commun;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemPersonnalise {
    private final Material material;
    private final int quantite;
    private final String nomCustom;
    private final List<String> loreCustom;
    private final Map<Enchantment, Integer> enchantements;

    public ItemPersonnalise(Material material, int quantite, String nomCustom, List<String> loreCustom, Map<Enchantment, Integer> enchantements) {
        this.material = material;
        this.quantite = quantite;
        this.nomCustom = nomCustom;
        this.loreCustom = loreCustom != null ? new ArrayList<>(loreCustom) : new ArrayList<>();
        this.enchantements = enchantements != null ? new HashMap<>(enchantements) : new HashMap<>();
    }

    public Material getMaterial() {
        return material;
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

    public ItemStack creerItemStack() {
        ItemStack item = new ItemStack(material, quantite);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (nomCustom != null && !nomCustom.isEmpty()) {
                meta.setDisplayName(nomCustom.replace("&", "§"));
            }

            if (!loreCustom.isEmpty()) {
                List<String> loreFormate = new ArrayList<>();
                for (String ligne : loreCustom) {
                    loreFormate.add(ligne.replace("&", "§"));
                }
                meta.setLore(loreFormate);
            }

            item.setItemMeta(meta);

            if (!enchantements.isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : enchantements.entrySet()) {
                    item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                }
            }
        }

        return item;
    }

    @Override
    public String toString() {
        return "ItemPersonnalise{" +
                "material=" + material +
                ", quantite=" + quantite +
                ", nomCustom='" + nomCustom + '\'' +
                ", loreCustom=" + loreCustom +
                ", enchantements=" + enchantements +
                '}';
    }
}