import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

public class SellerAgent extends Agent {
    private Map<String, Double> productDetails = new HashMap<>();

    // Constructeur par défaut
    public SellerAgent() {
        super();
    }

    protected void setup() {
        // Récupérer les arguments initiaux (détails du produit) passés lors de la création de l'agent
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            productDetails = (Map<String, Double>) args[0];
        }

        // Enregistrement auprès du DF
        registerWithDF();
        System.out.println("L'agent vendeur " + getAID().getName() + " est prêt avec les détails du produit: " + productDetails);

        // Comportement pour répondre aux demandes d'offre
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Modèle de message pour recevoir les demandes d'offre (CFP)
                MessageTemplate cfpTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage cfpMessage = receive(cfpTemplate);
                if (cfpMessage != null) {
                    // Une demande d'offre a été reçue, envoyer une proposition
                    ACLMessage propose = cfpMessage.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);

                    // Construire le contenu de la proposition (détails du produit)
                    StringBuilder contentBuilder = new StringBuilder();
                    for (Map.Entry<String, Double> entry : productDetails.entrySet()) {
                        contentBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                    }
                    propose.setContent(contentBuilder.toString());
                    send(propose);
                } else {
                    block(); // Bloquer jusqu'à ce qu'un message soit reçu
                }
            }
        });
    }

    protected void takeDown() {
        System.out.println("L'agent vendeur " + getAID().getName() + " se termine.");
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vendeur");
        sd.setName("vendeur");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // Autres comportements de l'agent vendeur (répondre aux demandes d'offre, etc.)
}
