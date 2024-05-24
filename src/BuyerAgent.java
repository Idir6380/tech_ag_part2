import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.core.mobility.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BuyerAgent extends Agent implements Serializable {

    // Critères et préférences de l'acheteur
    private Map<String, Double> criteria = new HashMap<>();
    private Map<String, Double> preferences = new HashMap<>();

    protected void setup() {
        // Initialisation des critères et préférences
        criteria.put("prix", 100.0);
        criteria.put("qualite", 8.0);
        criteria.put("coutLivraison", 20.0);

        preferences.put("prix", 0.4); // À minimiser
        preferences.put("qualite", 0.4); // À maximiser
        preferences.put("coutLivraison", 0.2); // À minimiser

        // Enregistrement auprès du DF
        registerWithDF();

        // Comportement périodique pour envoyer les demandes d'offre
        addBehaviour(new TickerBehaviour(this, 10000) {
            @Override
            protected void onTick() {
                sendRequestForProposals();
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

    private double evaluateOffer(Map<String, Double> offer) {
        double score = 0.0;
        for (Map.Entry<String, Double> entry : offer.entrySet()) {
            String critere = entry.getKey();
            double valeur = entry.getValue();
            double preference = preferences.get(critere);
            if (preference < 0) {
                score += preference * (criteria.get(critere) - valeur);
            } else {
                score += preference * valeur;
            }
        }
        return score;
    }

    // Autres méthodes pour la migration de l'agent
    // ...
}