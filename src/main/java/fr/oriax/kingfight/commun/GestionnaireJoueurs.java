package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GestionnaireJoueurs {

    private final Map<String, Integer> partiesJouees;
    private final Map<UUID, String> joueursEnPartie;
    private final Map<UUID, TypeMiniJeu> typePartieJoueur;

    public GestionnaireJoueurs() {
        this.partiesJouees = new ConcurrentHashMap<>();
        this.joueursEnPartie = new ConcurrentHashMap<>();
        this.typePartieJoueur = new ConcurrentHashMap<>();
    }

    private int getMaxPartiesParJoueur() {
        try {
            return KingFight.getInstance().getGestionnairePrincipal()
                    .getConfigPrincipale().getInt("general.max-parties-par-joueur", 10);
        } catch (Exception e) {
            return 10;
        }
    }

    public int getMaxPartiesParJoueurPublic() {
        return getMaxPartiesParJoueur();
    }

    public boolean peutJouer(Player joueur, TypeMiniJeu type) {
        UUID uuid = joueur.getUniqueId();

        if (joueursEnPartie.containsKey(uuid)) {
            return false;
        }

        String cle = uuid.toString() + "_" + type.name();
        int parties = partiesJouees.getOrDefault(cle, 0);
        return parties < getMaxPartiesParJoueur();
    }

    public int getPartiesRestantes(Player joueur, TypeMiniJeu type) {
        String cle = joueur.getUniqueId().toString() + "_" + type.name();
        int parties = partiesJouees.getOrDefault(cle, 0);
        return Math.max(0, getMaxPartiesParJoueur() - parties);
    }

    public void ajouterJoueurEnPartie(Player joueur, String idPartie, TypeMiniJeu type) {
        UUID uuid = joueur.getUniqueId();
        joueursEnPartie.put(uuid, idPartie);
        typePartieJoueur.put(uuid, type);
        
        KingFight.getInstance().getGestionnairePrincipal()
            .getGestionnaireIsolationPartie().isolerJoueurEnPartie(joueur);
        KingFight.getInstance().getGestionnairePrincipal()
            .getGestionnaireTabList().mettreAJourTabListJoueur(joueur);
    }

    public void retirerJoueurDePartie(Player joueur) {
        UUID uuid = joueur.getUniqueId();
        String anciennePartie = joueursEnPartie.remove(uuid);
        TypeMiniJeu ancienType = typePartieJoueur.remove(uuid);
        
        try {
            KingFight.getInstance().getGestionnairePrincipal()
                .getGestionnaireIsolationPartie().desIsolerJoueur(joueur);
        } catch (Exception e) {
            KingFight.getInstance().getLogger().warning("Erreur lors de la désisolation du joueur " + joueur.getName() + ": " + e.getMessage());
        }
        
        try {
            KingFight.getInstance().getGestionnairePrincipal()
                .getGestionnaireTabList().mettreAJourTabListJoueur(joueur);
        } catch (Exception e) {
            KingFight.getInstance().getLogger().warning("Erreur lors de la mise à jour de la TabList du joueur " + joueur.getName() + ": " + e.getMessage());
        }
        
        if (anciennePartie != null && ancienType != null) {
            KingFight.getInstance().getLogger().info("Joueur " + joueur.getName() + " retiré de la partie " + ancienType + " (" + anciennePartie + ")");
        }
    }

    public void forcerRetraitJoueur(Player joueur) {
        UUID uuid = joueur.getUniqueId();

        String anciennePartie = joueursEnPartie.remove(uuid);
        TypeMiniJeu ancienType = typePartieJoueur.remove(uuid);

        try {
            joueur.setGameMode(org.bukkit.GameMode.SURVIVAL);
            joueur.setHealth(20.0);
            joueur.setFoodLevel(20);
            joueur.setSaturation(20.0f);
            joueur.setWalkSpeed(0.2f);
            joueur.setFlySpeed(0.1f);
            joueur.getInventory().clear();
            joueur.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);

            for (org.bukkit.potion.PotionEffect effet : joueur.getActivePotionEffects()) {
                joueur.removePotionEffect(effet.getType());
            }

            joueur.setDisplayName(joueur.getName());
            joueur.setPlayerListName(joueur.getName());

            joueur.setScoreboard(KingFight.getInstance().getServer().getScoreboardManager().getNewScoreboard());
            
        } catch (Exception e) {
            KingFight.getInstance().getLogger().warning("Erreur lors du nettoyage d'urgence du joueur " + joueur.getName() + ": " + e.getMessage());
        }

        try {
            KingFight.getInstance().getGestionnairePrincipal()
                .getGestionnaireIsolationPartie().desIsolerJoueur(joueur);
            KingFight.getInstance().getGestionnairePrincipal()
                .getGestionnaireTabList().mettreAJourTabListJoueur(joueur);
        } catch (Exception e) {
            KingFight.getInstance().getLogger().warning("Erreur lors de la désisolation d'urgence du joueur " + joueur.getName() + ": " + e.getMessage());
        }
        
        KingFight.getInstance().getLogger().warning("Nettoyage d'urgence effectué pour le joueur " + joueur.getName() + 
            (anciennePartie != null ? " (ancienne partie: " + ancienType + " - " + anciennePartie + ")" : ""));
    }

    public boolean estEnPartie(Player joueur) {
        return joueursEnPartie.containsKey(joueur.getUniqueId());
    }

    public String getIdPartie(Player joueur) {
        return joueursEnPartie.get(joueur.getUniqueId());
    }

    public TypeMiniJeu getTypePartie(Player joueur) {
        return typePartieJoueur.get(joueur.getUniqueId());
    }

    public void incrementerPartiesJouees(Player joueur, TypeMiniJeu type) {
        String cle = joueur.getUniqueId().toString() + "_" + type.name();
        int parties = partiesJouees.getOrDefault(cle, 0);
        partiesJouees.put(cle, parties + 1);
    }

    public void reinitialiserPartiesJoueurs() {
        partiesJouees.clear();
    }

    public String genererNomAnonymise() {
        String[] prefixes = {"Guerrier", "Ninja", "Mage", "Archer", "Paladin", "Voleur", "Berserker", "Assassin"};
        String[] suffixes = {"Rouge", "Bleu", "Vert", "Jaune", "Violet", "Orange", "Rose", "Noir"};

        Random random = new Random();
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        int numero = random.nextInt(999) + 1;

        return prefix + suffix + numero;
    }

    public List<String> getNomsAnonymises(List<Player> joueurs) {
        List<String> noms = new ArrayList<>();
        Set<String> nomsUtilises = new HashSet<>();

        for (Player joueur : joueurs) {
            String nom;
            do {
                nom = genererNomAnonymise();
            } while (nomsUtilises.contains(nom));

            nomsUtilises.add(nom);
            noms.add(nom);
        }

        return noms;
    }
}
