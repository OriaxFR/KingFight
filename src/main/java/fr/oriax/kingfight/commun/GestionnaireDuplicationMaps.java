package fr.oriax.kingfight.commun;

import fr.oriax.kingfight.KingFight;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GestionnaireDuplicationMaps {

    private final KingFight plugin;

    private final Map<TypeMiniJeu, Set<String>> mapsOriginales;

    private final Map<String, ConfigurationPartieMultiMaps> partiesActives;

    private final Map<String, Queue<String>> poolMapsTemporaires;

    public GestionnaireDuplicationMaps(KingFight plugin) {
        this.plugin = plugin;
        this.mapsOriginales = new ConcurrentHashMap<>();
        this.partiesActives = new ConcurrentHashMap<>();
        this.poolMapsTemporaires = new ConcurrentHashMap<>();

        initialiserPools();
    }

    public static class ConfigurationPartieMultiMaps {
        private final String idPartie;
        private final TypeMiniJeu typeMiniJeu;
        private final Map<String, String> mapOriginaleVersTemporaire;
        private final long tempsCreation;

        public ConfigurationPartieMultiMaps(String idPartie, TypeMiniJeu typeMiniJeu) {
            this.idPartie = idPartie;
            this.typeMiniJeu = typeMiniJeu;
            this.mapOriginaleVersTemporaire = new HashMap<>();
            this.tempsCreation = System.currentTimeMillis();
        }

        public String getIdPartie() { return idPartie; }
        public TypeMiniJeu getTypeMiniJeu() { return typeMiniJeu; }
        public Map<String, String> getMapOriginaleVersTemporaire() { return mapOriginaleVersTemporaire; }
        public long getTempsCreation() { return tempsCreation; }

        public void ajouterMapping(String mapOriginale, String mapTemporaire) {
            mapOriginaleVersTemporaire.put(mapOriginale, mapTemporaire);
        }

        public String obtenirMapTemporaire(String mapOriginale) {
            return mapOriginaleVersTemporaire.get(mapOriginale);
        }
    }

    private void initialiserPools() {
        for (TypeMiniJeu type : TypeMiniJeu.values()) {
            mapsOriginales.put(type, new HashSet<>());
            poolMapsTemporaires.put(type.name(), new LinkedList<>());
        }
    }

    public void enregistrerMapOriginale(TypeMiniJeu type, String nomMap) {
        mapsOriginales.get(type).add(nomMap);
        plugin.getLogger().info("Map originale enregistrée: " + nomMap + " pour " + type.name());
    }

    public ConfigurationPartieMultiMaps creerConfigurationPartie(String idPartie, TypeMiniJeu typeMiniJeu, List<String> mapsNecessaires) {
        ConfigurationPartieMultiMaps config = new ConfigurationPartieMultiMaps(idPartie, typeMiniJeu);

        for (String mapOriginale : mapsNecessaires) {
            String mapTemporaire = obtenirMapTemporaire(mapOriginale, idPartie);
            if (mapTemporaire != null) {
                config.ajouterMapping(mapOriginale, mapTemporaire);
                plugin.getLogger().info("Mapping créé: " + mapOriginale + " -> " + mapTemporaire + " pour partie " + idPartie);
            } else {
                plugin.getLogger().warning("Impossible de créer une map temporaire pour " + mapOriginale + " (partie " + idPartie + ")");
            }
        }

        partiesActives.put(idPartie, config);
        return config;
    }

    private String obtenirMapTemporaire(String mapOriginale, String idPartie) {
        World mondeOriginal = plugin.getServer().getWorld(mapOriginale);
        if (mondeOriginal == null) {
            plugin.getLogger().warning("Map originale introuvable: " + mapOriginale);
            return null;
        }

        String nomMapTemporaire = genererNomMapTemporaire(mapOriginale, idPartie);

        boolean succes = demanderDuplicationMap(mapOriginale, nomMapTemporaire);

        if (succes) {
            return nomMapTemporaire;
        } else {
            plugin.getLogger().severe("Échec de la duplication de map: " + mapOriginale + " -> " + nomMapTemporaire);
            return null;
        }
    }

    private String genererNomMapTemporaire(String mapOriginale, String idPartie) {
        long timestamp = System.currentTimeMillis();
        return mapOriginale + "_temp_" + idPartie + "_" + timestamp;
    }

    private boolean demanderDuplicationMap(String mapOriginale, String nomMapTemporaire) {

        try {
            plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                "mv clone " + mapOriginale + " " + nomMapTemporaire
            );

            int tentatives = 0;
            while (tentatives < 50) {
                if (plugin.getServer().getWorld(nomMapTemporaire) != null) {
                    return true;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                tentatives++;
            }

            return plugin.getServer().getWorld(nomMapTemporaire) != null;

        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la duplication de map: " + e.getMessage());
            return false;
        }
    }

    public void libererMapsPartie(String idPartie) {
        ConfigurationPartieMultiMaps config = partiesActives.get(idPartie);
        if (config == null) {
            return;
        }

        for (String mapTemporaire : config.getMapOriginaleVersTemporaire().values()) {
            supprimerMapTemporaire(mapTemporaire);
        }

        partiesActives.remove(idPartie);

        plugin.getLogger().info("Maps temporaires libérées pour la partie " + idPartie);
    }

    private void supprimerMapTemporaire(String nomMapTemporaire) {
        try {
            World monde = plugin.getServer().getWorld(nomMapTemporaire);
            if (monde != null) {
                monde.getPlayers().forEach(player -> {
                    player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                });

                plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    "mv remove " + nomMapTemporaire
                );

                plugin.getLogger().info("Map temporaire supprimée: " + nomMapTemporaire);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la suppression de la map temporaire " + nomMapTemporaire + ": " + e.getMessage());
        }
    }

    public ConfigurationPartieMultiMaps obtenirConfigurationPartie(String idPartie) {
        return partiesActives.get(idPartie);
    }

    public boolean estPartieMultiMaps(String idPartie) {
        return partiesActives.containsKey(idPartie);
    }

    public String obtenirMapTemporairePourPartie(String idPartie, String mapOriginale) {
        ConfigurationPartieMultiMaps config = partiesActives.get(idPartie);
        if (config == null) {
            return mapOriginale;
        }

        String mapTemporaire = config.obtenirMapTemporaire(mapOriginale);
        return mapTemporaire != null ? mapTemporaire : mapOriginale;
    }

    public void nettoyerPartiesExpirees() {
        long maintenant = System.currentTimeMillis();
        long delaiExpiration = 2 * 60 * 60 * 1000;

        List<String> partiesExpirees = new ArrayList<>();

        for (Map.Entry<String, ConfigurationPartieMultiMaps> entry : partiesActives.entrySet()) {
            if (maintenant - entry.getValue().getTempsCreation() > delaiExpiration) {
                partiesExpirees.add(entry.getKey());
            }
        }

        for (String idPartie : partiesExpirees) {
            plugin.getLogger().info("Nettoyage de la partie expirée: " + idPartie);
            libererMapsPartie(idPartie);
        }
    }

    public void afficherStatistiques() {
        plugin.getLogger().info("━━━ STATISTIQUES MAPS MULTI-MAPS ━━━");
        plugin.getLogger().info("Parties actives: " + partiesActives.size());

        for (Map.Entry<String, ConfigurationPartieMultiMaps> entry : partiesActives.entrySet()) {
            ConfigurationPartieMultiMaps config = entry.getValue();
            plugin.getLogger().info("- Partie " + entry.getKey() + " (" + config.getTypeMiniJeu() + "): " +
                                  config.getMapOriginaleVersTemporaire().size() + " maps temporaires");
        }
    }
}
