package es.upm.practica;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class MainLauncher {

    public static void main(String[] args) {
        try {
            // ==================== CONFIGURACIÓN JADE ====================
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.GUI, "true"); // Activa la GUI de JADE (muy útil)

            // Crear contenedor principal
            ContainerController mainContainer = jade.core.Runtime.instance().createMainContainer(profile);

            // ==================== LANZAR AGENTES ====================

            // 1. Agente Interfaz (coordinador + GUI)
            AgentController interfaz = mainContainer.createNewAgent(
                "Interfaz", 
                "es.upm.practica.agents.AgenteInterfaz", 
                null
            );
            interfaz.start();

            // 2. Agente de Percepción
            AgentController percepcion = mainContainer.createNewAgent(
                "Percepcion", 
                "es.upm.practica.agents.AgentePercepcion", 
                null
            );
            percepcion.start();

            // 3. Agente Analizador Emocional
            AgentController analizador = mainContainer.createNewAgent(
                "AnalizadorEmocional", 
                "es.upm.practica.agents.AgenteAnalizadorEmocional", 
                null
            );
            analizador.start();

            // 4. Agente Recomendador Musical
            AgentController recomendador = mainContainer.createNewAgent(
                "RecomendadorMusical", 
                "es.upm.practica.agents.AgenteRecomendadorMusical", 
                null
            );
            recomendador.start();

            System.out.println("Todos los agentes lanzados correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}