package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;

public class GestionnaireClassements {

    private final KingFight plugin;
    private final Map<TypeMiniJeu, Map<UUID, Integer>> classements;

    public GestionnaireClassements(KingFight plugin) {
        this.plugin = plugin;
        this.classements = new HashMap<>();

        for (TypeMiniJeu type : TypeMiniJeu.values()) {
            classements.put(type, new HashMap<>());
        }
    }

    public void ajouterPoints(Player joueur, TypeMiniJeu type, int points) {
        UUID uuid = joueur.getUniqueId();
        Map<UUID, Integer> classement = classements.get(type);

        int pointsActuels = classement.getOrDefault(uuid, 0);
        classement.put(uuid, pointsActuels + points);
    }

    public int getPoints(Player joueur, TypeMiniJeu type) {
        return classements.get(type).getOrDefault(joueur.getUniqueId(), 0);
    }

    public List<Map.Entry<UUID, Integer>> getClassementTrie(TypeMiniJeu type, int limite) {
        return classements.get(type).entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limite)
                .collect(Collectors.toList());
    }

    public int getPosition(Player joueur, TypeMiniJeu type) {
        UUID joueurUuid = joueur.getUniqueId();
        Map<UUID, Integer> classement = classements.get(type);
        
        if (!classement.containsKey(joueurUuid)) {
            return -1;
        }
        
        int pointsJoueur = classement.get(joueurUuid);
        long joueursAvecPlusDePoints = classement.values().stream()
                .mapToInt(Integer::intValue)
                .filter(points -> points > pointsJoueur)
                .count();
        
        return (int) joueursAvecPlusDePoints + 1;
    }

    public void reinitialiserClassement(TypeMiniJeu type) {
        classements.get(type).clear();
    }

    public void reinitialiserTousLesClassements() {
        for (TypeMiniJeu type : TypeMiniJeu.values()) {
            classements.get(type).clear();
        }
    }

    public void sauvegarderResultatsTournoi(TypeMiniJeu type) {
        List<Map.Entry<UUID, Integer>> top10 = getClassementTrie(type, 10);

        if (top10.isEmpty()) {
            return;
        }

        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy_HH-mm");
        String dateActuelle = formatDate.format(new Date());

        File dossierResultats = new File(plugin.getDataFolder(), type.getNomConfig());
        if (!dossierResultats.exists()) {
            dossierResultats.mkdirs();
        }

        File fichierResultats = new File(dossierResultats, "resultats_" + dateActuelle + ".txt");

        try (FileWriter writer = new FileWriter(fichierResultats)) {
            writer.write("━━━ RESULTATS DU TOURNOI " + type.getNomAffichage().toUpperCase() + " ━━━\n");
            writer.write("Date: " + new SimpleDateFormat("dd/MM/yyyy à HH:mm").format(new Date()) + "\n");
            writer.write("Nombre de participants: " + classements.get(type).size() + "\n\n");
            writer.write("TOP 10 FINAL:\n");
            writer.write("━━━━━━━━━━\n\n");

            for (int i = 0; i < top10.size(); i++) {
                Map.Entry<UUID, Integer> entry = top10.get(i);
                String nomJoueur = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (nomJoueur == null) nomJoueur = "Joueur inconnu";

                writer.write((i + 1) + ". " + nomJoueur + " - " + entry.getValue() + " points\n");
            }

            writer.write("\n━━━ FIN DU TOURNOI ━━━\n");

        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder les resultats du tournoi " + type.name());
        }

        plugin.getLogger().info("Resultats du tournoi " + type.name() + " sauvegardes dans " + fichierResultats.getName());
    }

    public void supprimerFichierClassements() {
        File fichierClassements = new File(plugin.getDataFolder(), "classements.yml");
        if (fichierClassements.exists()) {
            if (fichierClassements.delete()) {
                plugin.getLogger().info("Fichier classements.yml supprime avec succes");
            } else {
                plugin.getLogger().warning("Impossible de supprimer le fichier classements.yml");
            }
        }
    }
}
