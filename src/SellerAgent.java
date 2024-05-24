import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.HashMap;
import java.util.Map;

public class SellerAgent extends Agent {
    private Map<String, Double> productDetails;

    public SellerAgent(Map<String, Double> productDetails) {
        this.productDetails = productDetails;
    }

    protected void setup() {
        // Enregistrement auprès du DF
        registerWithDF();

        System.out.println("L'agent vendeur " + getAID().getName() + " est prêt avec les détails du produit: " + productDetails);
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