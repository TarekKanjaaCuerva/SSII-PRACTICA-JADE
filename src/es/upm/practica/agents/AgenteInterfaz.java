package es.upm.practica.agents;

import es.upm.practica.agentLauncher.AgentBase;
import es.upm.practica.agentLauncher.AgentModel;
import es.upm.practica.common.ContentResult;
import es.upm.practica.common.MoodResult;
import es.upm.practica.common.PlaylistResult;
import es.upm.practica.common.Song;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Agente encargado de gestionar la interfaz gráfica y coordinar el flujo del sistema.
 * 
 * Este agente presenta al usuario una ventana Swing desde la que puede introducir
 * un texto directamente o cargar un fichero .txt. Al iniciar el análisis, orquesta
 * la comunicación con el resto de agentes en cadena: primero envía el texto al agente
 * de percepción, luego el resultado al analizador emocional, y finalmente el estado
 * de ánimo detectado al recomendador musical. Una vez completado el flujo, muestra
 * al usuario la emoción detectada, la playlist recomendada y la explicación del resultado.
 
 */
public class AgenteInterfaz extends AgentBase {

    private static final long serialVersionUID = 1L;

    //Componentes Swing
    private JFrame ventana;
    private JTextArea areaTexto;      // el usuario pega o carga texto aquí
    private JTextArea areaResultado;  // muestra la playlist final
    private JLabel labelEstado;       // barra de estado inferior
    private JButton botonAnalizar;

    //setup
    /**
     * Punto de entrada del agente. Llamada automatica al arranque. 
     * Captura los parametros del agente, lo registra (con registerService)
     */
    @Override
    protected void setup() {
        super.setup();
        registerService(AgentModel.VISUALIZATION);
        SwingUtilities.invokeLater(this::construirInterfaz);
    }

    //takeDown
    /**
     * para cuando el agente va a morir 
     * (al morir el agente la ventana Swing podria quedarse abierta)
     */
    @Override
    protected void takeDown() {
        if (ventana != null) ventana.dispose();
        super.takeDown(); // AgentBase llama a deregisterService()
    }

    // ==============================================================
    //  CONSTRUCCIÓN DE LA INTERFAZ SWING
    // ==============================================================

    /*
     * Contruye la ventana principal
     * Divide en tres zonas (con BorderLayout)
     * 		- El título (north)
     * 		- Para la entrada y el resultado (center)
     * 		- Barra de estado (south) (barra inferior explicativa)
     */
    private void construirInterfaz() {
        ventana = new JFrame("Recomendador Musical por Estado de Ánimo");
        ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        ventana.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                doDelete(); // llama a takeDown() limpiamente
            }
        });
        ventana.setSize(750, 660);
        ventana.setLocationRelativeTo(null);
        ventana.setLayout(new BorderLayout(10, 10));

        // Título
        JLabel titulo = new JLabel("Recomendador Musical por Estado de Ánimo", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 17));
        titulo.setBorder(new EmptyBorder(14, 10, 4, 10));
        ventana.add(titulo, BorderLayout.NORTH);

        // Panel central dividido en entrada (arriba) y resultado (abajo)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                panelEntrada(), panelResultado());
        split.setDividerLocation(270);
        split.setBorder(new EmptyBorder(0, 10, 0, 10));
        ventana.add(split, BorderLayout.CENTER);

        // Barra de estado
        labelEstado = new JLabel("Listo. Escribe o carga un texto y pulsa Analizar.");
        labelEstado.setBorder(new EmptyBorder(5, 10, 8, 10));
        labelEstado.setFont(new Font("SansSerif", Font.ITALIC, 12));
        labelEstado.setForeground(Color.GRAY);
        ventana.add(labelEstado, BorderLayout.SOUTH);

        ventana.setVisible(true);
    }

    
    /**
     * Crea el panel superior de Entrada (con JTextArea), donde el usuario escribe o pega texto y tiene 3 botones a la derecha
     * Incluye placeholder de color gris mientras no se escriba ni esté el cursor sobre este panel
     * @return
     */
    private JPanel panelEntrada() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Texto de entrada"));

        areaTexto = new JTextArea();
        areaTexto.setLineWrap(true);
        areaTexto.setWrapStyleWord(true);
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaTexto.setText("Pega aquí tu texto o carga un fichero .txt...");
        areaTexto.setForeground(Color.GRAY);

        areaTexto.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (areaTexto.getText().startsWith("Pega aquí")) {
                    areaTexto.setText("");
                    areaTexto.setForeground(Color.BLACK); // texto normal al escribir
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (areaTexto.getText().trim().isEmpty()) {
                    areaTexto.setText("Pega aquí tu texto o carga un fichero .txt...");
                    areaTexto.setForeground(Color.GRAY); // vuelve el placeholder si se deja vacío
                }
            }
        });
        panel.add(new JScrollPane(areaTexto), BorderLayout.CENTER);

        // Botones a la derecha
        JPanel botones = new JPanel(new GridLayout(3, 1, 5, 5));
        botones.setBorder(new EmptyBorder(0, 5, 0, 0));

        JButton btnCargar = new JButton("Cargar .txt");
        btnCargar.addActionListener(this::accionCargarFichero);

        botonAnalizar = new JButton("Analizar");
        botonAnalizar.setFont(new Font("SansSerif", Font.BOLD, 13));
        botonAnalizar.setBackground(new Color(70, 130, 180));
        botonAnalizar.setForeground(Color.WHITE);
        botonAnalizar.setOpaque(true);
        botonAnalizar.addActionListener(this::accionAnalizar);

        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            areaTexto.setText("");
            areaResultado.setText("");
            setEstado("Listo.");
        });

        botones.add(btnCargar);
        botones.add(botonAnalizar);
        botones.add(btnLimpiar);
        panel.add(botones, BorderLayout.EAST);

        return panel;
    }

    
    /**
     * Crea panel inferior de Resultado. Solo de lectura.
     * @return
     */
    private JPanel panelResultado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Resultado"));

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);
        areaResultado.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaResultado.setBackground(new Color(250, 250, 250));
        panel.add(new JScrollPane(areaResultado), BorderLayout.CENTER);
        return panel;
    }

    // ==============================================================
    //  ACCIONES DE BOTONES
    // ==============================================================

    /**
     * Abre un JFileChooser para cargar un .txt.
     * Pone el contenido del fichero en el área de texto.
     * El AgentePercepcion también acepta rutas de fichero, pero enviamos
     * el texto ya leído para que el usuario lo vea y pueda editarlo.
     */
    private void accionCargarFichero(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona un fichero de texto");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Ficheros de texto (*.txt)", "txt"));

        if (chooser.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            File fichero = chooser.getSelectedFile();
            try {
                areaTexto.setText(Files.readString(fichero.toPath()));
                setEstado("Fichero cargado: " + fichero.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(ventana,
                        "No se pudo leer el fichero:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Lanza el flujo multiagente.
     * El envío real ocurre dentro de un SequentialBehaviour (hilo JADE),
     * nunca desde el Event Dispatch Thread de Swing.
     */
    private void accionAnalizar(ActionEvent e) {
        String texto = areaTexto.getText().trim();
        if (texto.isEmpty() || texto.startsWith("Pega aquí")) {
            JOptionPane.showMessageDialog(ventana,
                    "Introduce o carga un texto primero.",
                    "Sin texto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        botonAnalizar.setEnabled(false);
        areaResultado.setText("Analizando...");
        setEstado("Iniciando pipeline multiagente...");
        addBehaviour(new PipelineBehaviour(texto));
    }

    // ==============================================================
    //  PIPELINE MULTIAGENTE — SequentialBehaviour con 3 pasos
    // ==============================================================

    /**
     * Encadena los tres pasos del flujo:
     *   Paso 1: texto  → AgentePercepcion      → ContentResult
     *   Paso 2: ContentResult → AgenteAnalizador → MoodResult
     *   Paso 3: MoodResult → AgenteRecomendador → PlaylistResult
     *
     * Por qué SequentialBehaviour: garantiza que cada paso espera
     * al anterior antes de ejecutarse, sin bloquear el scheduler de JADE
     * más de lo necesario.
     */
    private class PipelineBehaviour extends SequentialBehaviour {

        private static final long serialVersionUID = 1L;

        // Se comparten entre pasos como "variables de pipeline"
        private ContentResult contentResult;
        private MoodResult moodResult;
        private PlaylistResult playlistResult;
        private String errorMessage; // si no es null, hay que abortar

        PipelineBehaviour(String textoUsuario) {
            // Paso 1: enviar texto a AgentePercepcion y recoger ContentResult
            addSubBehaviour(new PasoPercepcion(textoUsuario));
            // Paso 2: enviar ContentResult a AgenteAnalizador y recoger MoodResult
            addSubBehaviour(new PasoAnalisis());
            // Paso 3: enviar MoodResult al Recomendador y recoger PlaylistResult
            addSubBehaviour(new PasoRecomendacion());
            // Paso 4: mostrar resultado en la GUI
            addSubBehaviour(new PasoMostrarResultado());
        }

        // ── PASO 1 ────────────────────────────────────────────────
        private class PasoPercepcion extends OneShotBehaviour {
            private static final long serialVersionUID = 1L;
            private final String texto;

            PasoPercepcion(String texto) { this.texto = texto; }

            @Override
            public void action() {
                if (errorMessage != null) return; // abort si paso anterior falló

                setEstado("Paso 1/3: Enviando texto al agente de percepción...");

                AID destino = buscarServicio(AgentModel.CONTENT_ACQUISITION);
                if (destino == null) {
                    errorMessage = "No se encontró el AgentePercepcion en el DF.";
                    return;
                }

                // AgentePercepcion espera el texto en request.getContent() (String plano)
                // según su implementación: String input = request.getContent()
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(destino);
                msg.setConversationId("pipeline-" + System.currentTimeMillis());
                msg.setContent(texto); // texto plano, no objeto serializado
                send(msg);

                // Esperamos la respuesta: INFORM con ContentResult o FAILURE
                // blockingReceive con filtro = filtro bloqueante requerido por la práctica
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
                ACLMessage reply = blockingReceive(mt);

                if (reply.getPerformative() == ACLMessage.FAILURE) {
                    errorMessage = "AgentePercepcion devolvió FAILURE: " + reply.getContent();
                    return;
                }

                try {
                    contentResult = (ContentResult) reply.getContentObject();
                    log("Paso 1 OK — ContentResult recibido: " + contentResult);
                } catch (Exception e) {
                    errorMessage = "Error al leer ContentResult: " + e.getMessage();
                }
            }
        }

        // ── PASO 2 ────────────────────────────────────────────────
        private class PasoAnalisis extends OneShotBehaviour {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                if (errorMessage != null) return;

                setEstado("Paso 2/3: Analizando emoción del texto...");

                AID destino = buscarServicio(AgentModel.MOOD_ANALYSIS);
                if (destino == null) {
                    errorMessage = "No se encontró el AgenteAnalizadorEmocional en el DF.";
                    return;
                }

                // AgenteAnalizadorEmocional espera un ContentResult serializado
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(destino);
                try {
                    msg.setContentObject(contentResult);
                } catch (IOException e) {
                    errorMessage = "Error al serializar ContentResult: " + e.getMessage();
                    return;
                }
                send(msg);

                // Filtro bloqueante esperando INFORM o FAILURE
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
                ACLMessage reply = blockingReceive(mt);

                if (reply.getPerformative() == ACLMessage.FAILURE) {
                    errorMessage = "AgenteAnalizador devolvió FAILURE: " + reply.getContent();
                    return;
                }

                try {
                    moodResult = (MoodResult) reply.getContentObject();
                    log("Paso 2 OK — Mood detectado: " + moodResult.getMood());
                } catch (Exception e) {
                    errorMessage = "Error al leer MoodResult: " + e.getMessage();
                }
            }
        }

        // ── PASO 3 ────────────────────────────────────────────────
        private class PasoRecomendacion extends OneShotBehaviour {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                if (errorMessage != null) return;

                setEstado("Paso 3/3: Buscando canciones para el mood detectado...");

                AID destino = buscarServicio(AgentModel.MUSIC_RECOMMENDATION);
                if (destino == null) {
                    errorMessage = "No se encontró el AgenteRecomendadorMusical en el DF.";
                    return;
                }

                // AgenteRecomendadorMusical espera un MoodResult serializado
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(destino);
                try {
                    msg.setContentObject(moodResult);
                } catch (IOException e) {
                    errorMessage = "Error al serializar MoodResult: " + e.getMessage();
                    return;
                }
                send(msg);

                // Filtro bloqueante esperando INFORM o FAILURE
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
                ACLMessage reply = blockingReceive(mt);

                if (reply.getPerformative() == ACLMessage.FAILURE) {
                    errorMessage = "AgenteRecomendador devolvió FAILURE: " + reply.getContent();
                    return;
                }

                try {
                    playlistResult = (PlaylistResult) reply.getContentObject();
                    log("Paso 3 OK — Playlist recibida con " + playlistResult.getSongs().size() + " canciones.");
                } catch (Exception e) {
                    errorMessage = "Error al leer PlaylistResult: " + e.getMessage();
                }
            }
        }

        // ── PASO 4: mostrar en GUI ─────────────────────────────────
        private class PasoMostrarResultado extends OneShotBehaviour {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                if (errorMessage != null) {
                    SwingUtilities.invokeLater(() -> {
                        areaResultado.setText("Error: " + errorMessage);
                        botonAnalizar.setEnabled(true);
                        setEstado("Error en el análisis.");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        mostrarPlaylist(playlistResult);
                        botonAnalizar.setEnabled(true);
                        setEstado("Análisis completado.");
                    });
                }
            }
        }
    }

    // ==============================================================
    //  PRESENTACIÓN DEL RESULTADO
    // ==============================================================

    /**
     * Formatea el PlaylistResult y lo pinta en el área de resultado.
     * Muestra: emoción detectada + explicación del análisis + canciones.
     */
    private void mostrarPlaylist(PlaylistResult pr) {
        StringBuilder sb = new StringBuilder();

        sb.append("=================================================\n");
        sb.append("  Estado de ánimo detectado: ").append(pr.getMood()).append("\n");
        sb.append("=================================================\n\n");

        if (pr.getExplanation() != null && !pr.getExplanation().isEmpty()) {
            sb.append("Análisis:\n").append(pr.getExplanation()).append("\n\n");
        }

        if (pr.getSongs() == null || pr.getSongs().isEmpty()) {
            sb.append("No se encontraron canciones para este estado de ánimo.\n");
        } else {
            sb.append("Playlist recomendada (").append(pr.getSongs().size()).append(" canciones):\n\n");
            int n = 1;
            for (Song s : pr.getSongs()) {
                sb.append("  ").append(n++).append(". ").append(s.getTitle()).append("\n");
                sb.append("     Artista : ").append(s.getArtist()).append("\n");
                sb.append("     Género  : ").append(s.getGenre()).append("\n");
                if (s.getDescription() != null && !s.getDescription().isEmpty()) {
                    sb.append("     Por qué : ").append(s.getDescription()).append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("=================================================\n");
        areaResultado.setText(sb.toString());
        areaResultado.setCaretPosition(0);
    }

    // ==============================================================
    //  UTILIDADES
    // ==============================================================

    /**
     * Busca en el DF un agente que ofrezca el servicio indicado.
     * Usa searchAgents() de AgentBase, igual que el resto de agentes.
     */
    private AID buscarServicio(AgentModel tipo) {
        DFAgentDescription[] resultados = searchAgents(tipo);
        if (resultados != null && resultados.length > 0) {
            return resultados[0].getName();
        }
        log("No se encontró ningún agente con servicio: " + tipo.getValue());
        return null;
    }

    /** Actualiza la barra de estado de forma segura desde cualquier hilo */
    private void setEstado(String msg) {
        SwingUtilities.invokeLater(() -> labelEstado.setText(msg));
    }
}