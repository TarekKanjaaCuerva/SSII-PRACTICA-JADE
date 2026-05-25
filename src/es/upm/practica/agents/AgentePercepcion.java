package es.upm.practica.agents;

import es.upm.practica.agentLauncher.AgentBase;
import es.upm.practica.agentLauncher.AgentModel;
import es.upm.practica.common.ContentResult;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AgentePercepcion extends AgentBase {

    private static final long serialVersionUID = 1L;
    
    public static final String NICKNAME = "contentAcquisition";

    @Override
    protected void setup() {
        super.setup();
        
        registerService(AgentModel.CONTENT_ACQUISITION);
        log("Servicio de adquisición de contenido registrado correctamente.");
        addBehaviour(new ProcesarPeticionContenido());
    }

    private class ProcesarPeticionContenido extends CyclicBehaviour {
        private static final long serialVersionUID = 1L;

        @Override
        public void action() {
            // Escuchar solo mensajes de tipo REQUEST
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage request = receive(mt);

            if (request != null) {
                log("Petición recibida del agente: " + request.getSender().getLocalName());
                // El contenido del request será el String introducido por el usuario en la interfaz
                String input = request.getContent(); 
                ACLMessage reply = request.createReply();

                try {
                    String extractedText = extraerTexto(input);
                    
                    ContentResult result = new ContentResult(extractedText);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(result);
                    send(reply);
                    
                    log("Texto extraído y enviado correctamente a " + request.getSender().getLocalName());
                    
                } catch (Exception e) {
                    // Si el archivo no existe, la URL falla, etc., devolvemos un FAILURE
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Error al extraer el contenido: " + e.getMessage());
                    send(reply);
                    logError("Fallo al procesar la petición de contenido", e);
                }
            } else {
                block();
            }
        }
        
        // Lógica de extracción de texto para soportar las 3 vías propuestas.
        private String extraerTexto(String input) throws IOException {
            if (input == null || input.trim().isEmpty()) {
                throw new IllegalArgumentException("La entrada recibida está vacía.");
            }
            
            input = input.trim();
            
            // Es una URL (no probado, lo hemos puesto por si daba tiempo a mejorar la interfaz y el usuario podía pegar una URL directamente)
            if (input.startsWith("http://") || input.startsWith("https://")) {
                log("Detectada URL. Extrayendo contenido web con JSoup...");
                Document doc = Jsoup.connect(input).get();
                return doc.text(); 
            } 
            // Es la ruta a un fichero de texto
            else if (input.endsWith(".txt")) {
                log("Detectado fichero .txt. Leyendo contenido...");
                return new String(Files.readAllBytes(Paths.get(input)));
            } 
            // Es texto pegado directamente por el usuario
            else {
                log("Detectado texto plano directo.");
                return input;
            }
        }
    }
}