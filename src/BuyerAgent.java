import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BuyerAgent extends Agent implements Serializable {

    private enum Critere {
        PRIX("prix", -1), // À minimiser
        QUALITE("qualite", 1), // À maximiser
        COUT_LIVRAISON("coutLivraison", -1); // À minimiser

        private String nom;
        private int direction; // 1 pour maximiser, -1 pour minimiser

        Critere(String nom, int direction) {
            this.nom = nom;
            this.direction = direction;
        }

        public String getNom() {
            return nom;
        }

        public int getDirection() {
            return direction;
        }
    }
    private enum Preference {
        PRIX("prix", 0.4, -1), // À minimiser
        QUALITE("qualite", 0.4, 1), // À maximiser
        COUT_LIVRAISON("coutLivraison", 0.2, -1); // À minimiser

        private String nom;
        private double valeur;
        private int direction; // 1 pour maximiser, -1 pour minimiser

        Preference(String nom, double valeur, int direction) {
            this.nom = nom;
            this.valeur = valeur;
            this.direction = direction;
        }

        public String getNom() {
            return nom;
        }

        public double getValeur() {
            return valeur;
        }

        public int getDirection() {
            return direction;
        }
    }

    // Critères et préférences de l'acheteur
    private Map<String, Double> criteria = new HashMap<>();
    private Map<String, Double> preferences = new HashMap<>();

    protected void setup() {
        // Initialisation des critères et préférences
        for (Critere critere : Critere.values()) {
            criteria.put(critere.getNom(), critere.getDirection() * 100.0); // Valeur de démonstration
        }

        for (Preference preference : Preference.values()) {
            preferences.put(preference.getNom(), preference.getValeur());
        }
        // Enregistrement auprès du DF
        registerWithDF();

        // Comportement périodique pour envoyer les demandes d'offre
        addBehaviour(new TickerBehaviour(this, 10000) { // Période de 10 secondes
            @Override
            protected void onTick() {
                sendRequestForProposals();

                // Attendre les réponses des vendeurs
                final MessageTemplate proposalTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                addBehaviour(new CyclicBehaviour(BuyerAgent.this) {
                    ArrayList<ACLMessage> repliesReceived = new ArrayList<>();

                    public void action() {
                        ACLMessage reply = receive(proposalTemplate);
                        if (reply != null) {
                            repliesReceived.add(reply);
                            // Si toutes les réponses attendues sont reçues, traiter les réponses
                            if (repliesReceived.size() == getVendorsCount()) {
                                processReplies(repliesReceived);
                                removeBehaviour(this); // Supprimer ce comportement une fois terminé
                            }
                        } else {
                            block(); // Bloquer jusqu'à ce qu'un message soit reçu
                        }
                    }
                });
            }
        });

        System.out.println("L'agent acheteur " + getAID().getName() + " est prêt.");
    }

    protected void takeDown() {
        // Nettoyage avant la fermeture de l'agent
        System.out.println("L'agent acheteur " + getAID().getName() + " se termine.");
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("acheteur");
        sd.setName("acheteur");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void sendRequestForProposals() {
        // Obtenir la liste des agents vendeurs à partir du DF
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vendeur");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription vendeur : result) {
                // Envoyer un message de demande d'offre à chaque vendeur
                ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.addReceiver(vendeur.getName());
                msg.setContent("Demande d'offre pour le produit X");
                send(msg);
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private int getVendorsCount() {
        // Obtenir la liste des agents vendeurs à partir du DF
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vendeur");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            return result.length; // Retourne le nombre de vendeurs trouvés
        } catch (FIPAException fe) {
            fe.printStackTrace();
            return 0; // En cas d'erreur, retourner 0
        }
    }

    private void processReplies(ArrayList<ACLMessage> repliesReceived) {
        // Évaluer les offres reçues et sélectionner la meilleure
        Map<String, Double> bestOffer = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ACLMessage reply : repliesReceived) {
            Map<String, Double> offer = extractOfferFromMessage(reply);
            double score = evaluateOffer(offer);
            if (score > bestScore) {
                bestScore = score;
                bestOffer = offer;
            }
        }

        // Accepter la meilleure offre
        if (bestOffer != null) {
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.setContent(bestOffer.toString());
            accept.addReceiver(repliesReceived.get(0).getSender()); // Utilise le premier vendeur
            send(accept);
        }
    }

    private double evaluateOffer(Map<String, Double> offer) {
        double score = 0.0;
        for (Map.Entry<String, Double> entry : offer.entrySet()) {
            String critere = entry.getKey();
            double valeur = entry.getValue();
            Preference preference = Preference.valueOf(critere.toUpperCase());
            if (preference.getDirection() < 0) {
                score += preference.getValeur() * (criteria.get(critere) - valeur);
            } else {
                score += preference.getValeur() * valeur;
            }
        }
        return score;
    }

    private Map<String, Double> extractOfferFromMessage(ACLMessage msg) {
        Map<String, Double> offer = new HashMap<>();
        String content = msg.getContent();
        String[] parts = content.split(";");
        for (String part : parts) {
            String[] kvp = part.split("=");
            if (kvp.length == 2) {
                String key = kvp[0].trim();
                double value = Double.parseDouble(kvp[1].trim());
                offer.put(key, value);
            }
        }
        return offer;
    }

    // Autres méthodes pour la migration de l'agent
    // ...
}
