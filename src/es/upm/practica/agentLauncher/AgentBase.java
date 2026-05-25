package es.upm.practica.agentLauncher;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Arrays;

public class AgentBase extends Agent {
	private static final long serialVersionUID = 1L;
	protected AgentModel type;      // Tipo de servicio que ofrece este agente
    protected String[] params;      // Parámetros que se le pasan al crear el agente

    @Override
    protected void setup() {

        // Capturamos los parámetros que se le pasan al agente
        if (getArguments() != null) {
            this.params = Arrays.asList(getArguments())
                                .toArray(new String[getArguments().length]);
        } else {
            this.params = new String[0];
        }

        super.setup();
        
        System.out.println("[" + getLocalName() + "] Agente iniciado");
    }

    // Registra este agente en el Directory Facilitator (DF)
    protected void registerService(AgentModel agentType) {
        this.type = agentType;

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(agentType.getValue());
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println("[" + getLocalName() + "] Registrado en DF como: " + agentType.getValue());
        } catch (FIPAException e) {
            System.err.println("[" + getLocalName() + "] Error al registrar en DF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Busca agentes que ofrezcan un determinado servicio
    protected DFAgentDescription[] searchAgents(AgentModel agentType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(agentType.getValue());
        template.addServices(sd);

        try {
            return DFService.search(this, template);
        } catch (FIPAException e) {
            System.err.println("[" + getLocalName() + "] Error buscando en DF: " + e.getMessage());
            e.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

    // Dar de baja del registro al agente del DF
    protected void deregisterService() {
        try {
            DFService.deregister(this);
            System.out.println("[" + getLocalName() + "] Deregistrado del DF");
        } catch (FIPAException e) {
            // Puede fallar si ya no estaba registrado
        }
    }

    @Override
    protected void takeDown() {
        deregisterService();
        System.out.println("[" + getLocalName() + "] Agente finalizado");
        super.takeDown();
    }

    // MÉTODOS DE AYUDA

    protected void log(String msg) {
        System.out.println("[" + getLocalName() + "] " + msg);
    }

    protected void logError(String msg, Exception e) {
        System.err.println("[" + getLocalName() + "] ERROR: " + msg);
        if (e != null) e.printStackTrace();
    }
}
