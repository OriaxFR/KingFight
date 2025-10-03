package fr.oriax.kingfight.jumpleague;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import fr.oriax.kingfight.gestionnaire.GestionnairePrincipal;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class GestionnaireJumpLeague {

    private final KingFight plugin;
    private final GestionnairePrincipal gestionnairePrincipal;
    private final ConfigurationJumpLeague configuration;
    private final GestionnaireSetupJumpLeague gestionnaireSetup;
    private final ValidateurConfigurationJumpLeague validateur;

    private final Map<String, PartieJumpLeague> partiesEnCours;
    private final ConcurrentLinkedQueue<Player> fileAttenteGlobale;
    private final AtomicInteger nombreJoueursEnAttente;
    private final ReentrantLock verrouillageLancement;
    private final AtomicInteger compteurParties;
    
    private BukkitTask tacheLancementAutomatique;
    private BukkitTask tacheCountdownMessages;
    private long timestampDebutAttente;

    public GestionnaireJumpLeague(KingFight plugin, GestionnairePrincipal gestionnairePrincipal) {
        this.plugin = plugin;
        this.gestionnairePrincipal = gestionnairePrincipal;
        this.configuration = new ConfigurationJumpLeague(plugin);
        this.gestionnaireSetup = new GestionnaireSetupJumpLeague(plugin);
        this.validateur = new ValidateurConfigurationJumpLeague(plugin, configuration, gestionnaireSetup);
        this.partiesEnCours = new ConcurrentHashMap<>();
        this.fileAttenteGlobale = new ConcurrentLinkedQueue<>();
        this.nombreJoueursEnAttente = new AtomicInteger(0);
        this.verrouillageLancement = new ReentrantLock();
        this.compteurParties = new AtomicInteger(0);
    }

    public void ajouterJoueurEnFileAttente(Player joueur) {
        if (gestionnairePrincipal.getGestionnaireJoueurs().estEnPartie(joueur)) {
            joueur.sendMessage("§cVous etes deja dans une partie !");
            return;
        }

        if (!gestionnairePrincipal.getGestionnaireJoueurs().peutJouer(joueur, TypeMiniJeu.JUMP_LEAGUE)) {
            joueur.sendMessage("§cVous avez atteint le nombre maximum de parties pour cet evenement !");
            return;
        }

        if (!fileAttenteGlobale.contains(joueur) && fileAttenteGlobale.offer(joueur)) {
            int position = nombreJoueursEnAttente.incrementAndGet();
            int tailleRequise = configuration.getTailleFileAttente();
            joueur.sendMessage("§aVous avez rejoint la file d'attente JumpLeague ! §7Position: " + position + "/" + tailleRequise);

            if (position == 1) {
                timestampDebutAttente = System.currentTimeMillis();
                demarrerTimerLancementAutomatique();
            }

            notifierChangementFileAttente();
            
            essayerLancerPartie();
        } else {
            joueur.sendMessage("§cVous etes deja en file d'attente !");
        }
    }

    public void retirerJoueurDeFileAttente(Player joueur) {
        if (fileAttenteGlobale.remove(joueur)) {
            int nouveauNombre = nombreJoueursEnAttente.decrementAndGet();
            joueur.sendMessage("§eVous avez quitte la file d'attente JumpLeague.");

            if (nouveauNombre == 0) {
                annulerTimerLancementAutomatique();
                annulerCountdownMessages();
            }

            notifierChangementFileAttente();
        }
    }
    
    public boolean estEnFileAttente(Player joueur) {
        return fileAttenteGlobale.contains(joueur);
    }

    private void essayerLancerPartie() {
        essayerLancerPartie(false);
    }
    
    private void essayerLancerPartie(boolean forcerLancementAuto) {
        int tailleRequise = configuration.getTailleFileAttente();
        int nombreActuel = nombreJoueursEnAttente.get();
        
        boolean peutLancer = nombreActuel >= tailleRequise;
        
        if (!peutLancer && forcerLancementAuto && configuration.isLancementAutomatiqueActif()) {
            int minimumAuto = configuration.getMinimumJoueursLancementAuto();
            peutLancer = nombreActuel >= minimumAuto;
        }
        
        if (!peutLancer) {
            return;
        }

        if (verrouillageLancement.tryLock()) {
            try {
                int nombreFinal = nombreJoueursEnAttente.get();
                boolean lancementValide = nombreFinal >= tailleRequise;
                
                if (!lancementValide && forcerLancementAuto && configuration.isLancementAutomatiqueActif()) {
                    int minimumAuto = configuration.getMinimumJoueursLancementAuto();
                    lancementValide = nombreFinal >= minimumAuto;
                }
                
                if (lancementValide) {
                    demarrerPartie(forcerLancementAuto);
                }
            } finally {
                verrouillageLancement.unlock();
            }
        }
    }

    private void demarrerPartie() {
        demarrerPartie(false);
    }
    
    private void demarrerPartie(boolean lancementAutomatique) {
        int tailleRequise = configuration.getTailleFileAttente();
        int minimumRequis = lancementAutomatique && configuration.isLancementAutomatiqueActif() 
            ? configuration.getMinimumJoueursLancementAuto() 
            : tailleRequise;
            
        if (nombreJoueursEnAttente.get() < minimumRequis) return;

        List<Player> joueursPartie = new ArrayList<>();
        int nombreAPrendre = lancementAutomatique ? nombreJoueursEnAttente.get() : tailleRequise;
        
        for (int i = 0; i < nombreAPrendre && !fileAttenteGlobale.isEmpty(); i++) {
            Player joueur = fileAttenteGlobale.poll();
            if (joueur != null && joueur.isOnline()) {
                joueursPartie.add(joueur);
                nombreJoueursEnAttente.decrementAndGet();
            }
        }

        if (joueursPartie.size() < minimumRequis) {
            for (Player joueur : joueursPartie) {
                fileAttenteGlobale.offer(joueur);
                nombreJoueursEnAttente.incrementAndGet();
            }
            return;
        }
        
        annulerTimerLancementAutomatique();
        annulerCountdownMessages();

        int numeroPartie = compteurParties.incrementAndGet();
        String idPartie = "JL_" + System.currentTimeMillis() + "_" + numeroPartie;
        PartieJumpLeague partie = new PartieJumpLeague(plugin, this, idPartie, joueursPartie, configuration);

        partiesEnCours.put(idPartie, partie);

        for (Player joueur : joueursPartie) {
            gestionnairePrincipal.getGestionnaireJoueurs().ajouterJoueurEnPartie(joueur, idPartie, TypeMiniJeu.JUMP_LEAGUE);
        }

        partie.demarrer();

        String typeLancement = lancementAutomatique ? " (lancement automatique)" : "";
        plugin.getLogger().info("Partie JumpLeague demarree: " + idPartie + " avec " + joueursPartie.size() + " joueurs" + typeLancement);
    }

    public void terminerPartie(String idPartie) {
        PartieJumpLeague partie = partiesEnCours.remove(idPartie);
        if (partie != null) {
            for (Player joueur : partie.getJoueurs()) {
                gestionnairePrincipal.getGestionnaireJoueurs().incrementerPartiesJouees(joueur, TypeMiniJeu.JUMP_LEAGUE);
                gestionnairePrincipal.getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
            }
            plugin.getLogger().info("Partie JumpLeague terminee: " + idPartie);
            gestionnairePrincipal.notifierFinPartie(TypeMiniJeu.JUMP_LEAGUE);

            plugin.getGestionnairePrincipal().getGestionnaireBackupAutomatique()
                    .nettoyageRapideApresPartie(TypeMiniJeu.JUMP_LEAGUE);
        }
    }

    public void retirerJoueurDePartie(Player joueur, String idPartie) {
        PartieJumpLeague partie = partiesEnCours.get(idPartie);
        if (partie != null) {
            partie.retirerJoueurDePartie(joueur);
        } else {
            gestionnairePrincipal.getGestionnaireJoueurs().retirerJoueurDePartie(joueur);
            gestionnairePrincipal.getGestionnaireLobby().teleporterVersLobby(joueur);
            joueur.sendMessage("§e» Vous avez quitté votre précédente partie, vous ne pouvez plus y retourner.");
        }
    }

    public boolean estDansMondePartie(Player joueur, String idPartie) {
        PartieJumpLeague partie = partiesEnCours.get(idPartie);
        if (partie == null) return false;

        return joueur.getWorld().equals(partie.getMonde());
    }

    public void mettreJoueurEnSpectateur(Player joueur, String idPartie) {
        PartieJumpLeague partie = partiesEnCours.get(idPartie);
        if (partie != null) {
            joueur.setGameMode(org.bukkit.GameMode.SPECTATOR);
            joueur.sendMessage("§7Vous etes maintenant spectateur.");
        }
    }

    public void arreterToutesLesParties() {
        annulerTimerLancementAutomatique();
        annulerCountdownMessages();
        for (PartieJumpLeague partie : partiesEnCours.values()) {
            partie.arreter();
        }
        partiesEnCours.clear();
        fileAttenteGlobale.clear();
        nombreJoueursEnAttente.set(0);
    }

    public int getNombrePartiesEnCours() {
        return partiesEnCours.size();
    }

    public int getTailleFileAttente() {
        return nombreJoueursEnAttente.get();
    }

    public PartieJumpLeague getPartieParId(String idPartie) {
        return partiesEnCours.get(idPartie);
    }

    public ConfigurationJumpLeague getConfiguration() {
        return configuration;
    }

    public GestionnaireSetupJumpLeague getGestionnaireSetup() {
        return gestionnaireSetup;
    }

    public GestionnairePrincipal getGestionnairePrincipal() {
        return gestionnairePrincipal;
    }

    public ValidateurConfigurationJumpLeague.ResultatValidation validerConfiguration() {
        return validateur.validerConfigurationComplete();
    }

    public void rechargerSetupsPartiesEnCours(String nomMapOriginale) {
        for (PartieJumpLeague partie : partiesEnCours.values()) {
            if (partie.getNomMapOriginale().equals(nomMapOriginale)) {
                partie.rechargerSetup();
            }
        }
    }
    
    private void notifierChangementFileAttente() {
        int nombreActuel = nombreJoueursEnAttente.get();
        int tailleRequise = configuration.getTailleFileAttente();
        
        String messageBase = "§7[File d'attente] §e" + nombreActuel + "/" + tailleRequise + " joueurs";
        
        if (configuration.isLancementAutomatiqueActif() && nombreActuel > 0 && nombreActuel < tailleRequise) {
            int minimumAuto = configuration.getMinimumJoueursLancementAuto();
            if (nombreActuel >= minimumAuto) {
                long tempsEcoule = System.currentTimeMillis() - timestampDebutAttente;
                long delaiTotal = configuration.getDelaiLancementAutoSecondes() * 1000L;
                long tempsRestant = Math.max(0, delaiTotal - tempsEcoule) / 1000;
                
                if (tempsRestant > 0) {
                    messageBase += " §7- Lancement auto dans §c" + tempsRestant + "s";
                }
            }
        }

        for (Player joueurEnFile : fileAttenteGlobale) {
            if (joueurEnFile != null && joueurEnFile.isOnline()) {
                joueurEnFile.sendMessage(messageBase);
            }
        }
    }
    
    private void demarrerTimerLancementAutomatique() {
        if (!configuration.isLancementAutomatiqueActif()) {
            return;
        }
        
        annulerTimerLancementAutomatique();
        annulerCountdownMessages();
        
        int delaiSecondes = configuration.getDelaiLancementAutoSecondes();

        demarrerCountdownMessages();
        
        tacheLancementAutomatique = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int nombreActuel = nombreJoueursEnAttente.get();
            int minimumAuto = configuration.getMinimumJoueursLancementAuto();
            
            if (nombreActuel >= minimumAuto) {
                for (Player joueurEnFile : fileAttenteGlobale) {
                    if (joueurEnFile != null && joueurEnFile.isOnline()) {
                        joueurEnFile.sendMessage("§6§lLancement automatique ! §ePartie demarree avec " + nombreActuel + " joueurs.");
                    }
                }
                essayerLancerPartie(true);
            }
        }, delaiSecondes * 20L);
    }
    
    private void annulerTimerLancementAutomatique() {
        if (tacheLancementAutomatique != null) {
            tacheLancementAutomatique.cancel();
            tacheLancementAutomatique = null;
        }
    }
    
    private void demarrerCountdownMessages() {
        if (!configuration.isLancementAutomatiqueActif()) {
            return;
        }
        
        annulerCountdownMessages();

        tacheCountdownMessages = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int nombreActuel = nombreJoueursEnAttente.get();
            int minimumAuto = configuration.getMinimumJoueursLancementAuto();
            int tailleRequise = configuration.getTailleFileAttente();

            if (nombreActuel >= minimumAuto && nombreActuel < tailleRequise) {
                long tempsEcoule = System.currentTimeMillis() - timestampDebutAttente;
                long delaiTotal = configuration.getDelaiLancementAutoSecondes() * 1000L;
                long tempsRestant = Math.max(0, delaiTotal - tempsEcoule) / 1000;

                if (tempsRestant > 0) {
                    long tempsAffiche = ((tempsRestant + 5) / 10) * 10;
                    if (tempsAffiche > tempsRestant) {
                        tempsAffiche = tempsRestant;
                    }
                    
                    String message = "§6§lLancement automatique dans §e" + tempsAffiche + " secondes §6§l!";
                    
                    for (Player joueurEnFile : fileAttenteGlobale) {
                        if (joueurEnFile != null && joueurEnFile.isOnline()) {
                            joueurEnFile.sendMessage(message);
                            joueurEnFile.playSound(joueurEnFile.getLocation(), 
                                org.bukkit.Sound.NOTE_PLING, 0.5f, 1.0f);
                        }
                    }
                } else {
                    annulerCountdownMessages();
                }
            } else {
                annulerCountdownMessages();
            }
        }, 0L, 200L);
    }
    
    private void annulerCountdownMessages() {
        if (tacheCountdownMessages != null) {
            tacheCountdownMessages.cancel();
            tacheCountdownMessages = null;
        }
    }
}
