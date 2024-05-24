import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Démarrage de la plateforme JADE
        Runtime rt = Runtime.instance();

        // Conteneur principal
        ProfileImpl mainProfile = new ProfileImpl();
        mainProfile.setParameter(Profile.GUI, "true");
        mainProfile.setParameter(Profile.LOCAL_HOST, "localhost");
        mainProfile.setParameter(Profile.LOCAL_PORT, "1099");
        mainProfile.setParameter(Profile.CONTAINER_NAME, "MainContainer");
        AgentContainer mainContainer = rt.createMainContainer(mainProfile);

        try {
            // Créer les conteneurs pour les agents vendeurs
            ContainerController sellerContainer1 = rt.createAgentContainer(new ProfileImpl(null, 1099, null, true));
            ContainerController sellerContainer2 = rt.createAgentContainer(new ProfileImpl(null, 1099, null, true));
            ContainerController sellerContainer3 = rt.createAgentContainer(new ProfileImpl(null, 1099, null, true));

            // Lancer les agents vendeurs
            Map<String, Double> productDetails1 = new HashMap<>();
            productDetails1.put("prix", 150.0);
            productDetails1.put("qualite", 80.0);
            productDetails1.put("coutLivraison", 20.0);

            Map<String, Double> productDetails2 = new HashMap<>();
            productDetails2.put("prix", 200.0);
            productDetails2.put("qualite", 90.0);
            productDetails2.put("coutLivraison", 15.0);

            Map<String, Double> productDetails3 = new HashMap<>();
            productDetails3.put("prix", 180.0);
            productDetails3.put("qualite", 85.0);
            productDetails3.put("coutLivraison", 10.0);

            AgentController seller1 = sellerContainer1.createNewAgent("Seller1", SellerAgent.class.getName(), new Object[]{productDetails1});
            AgentController seller2 = sellerContainer2.createNewAgent("Seller2", SellerAgent.class.getName(), new Object[]{productDetails2});
            AgentController seller3 = sellerContainer3.createNewAgent("Seller3", SellerAgent.class.getName(), new Object[]{productDetails3});

            seller1.start();
            seller2.start();
            seller3.start();

            // Lancer l'agent acheteur
            AgentController buyer = mainContainer.createNewAgent("Buyer", BuyerAgent.class.getName(), null);
            buyer.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}