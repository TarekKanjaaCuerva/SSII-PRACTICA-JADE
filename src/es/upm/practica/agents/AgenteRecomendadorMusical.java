package es.upm.practica.agents;

import es.upm.practica.agentLauncher.AgentBase;
import es.upm.practica.agentLauncher.AgentModel;
import es.upm.practica.common.MoodResult;
import es.upm.practica.common.PlaylistResult;
import es.upm.practica.common.Song;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AgenteRecomendadorMusical extends AgentBase {

    private static final long serialVersionUID = 1L;

    public static final String NICKNAME = "musicRecommender";

    private static final String CSV_PATH = "data/canciones.csv";
    private static final int NUM_RECOMMENDATIONS = 3;

    @Override
    protected void setup() {
        super.setup();

        registerService(AgentModel.MUSIC_RECOMMENDATION);

        addBehaviour(new RecomendarMusicaBehaviour());

        log("Agente recomendador musical listo.");
    }

    private class RecomendarMusicaBehaviour extends CyclicBehaviour {

        private static final long serialVersionUID = 1L;

        @Override
        public void action() {

            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage request = receive(template);

            if (request == null) {
                block();
                return;
            }

            try {
                Object content = request.getContentObject();

                if (!(content instanceof MoodResult)) {
                    enviarFailure(request, "El contenido recibido no es un MoodResult.");
                    return;
                }

                MoodResult moodResult = (MoodResult) content;
                String mood = normalizarMood(moodResult.getMood());

                log("Recibido mood para recomendar música: " + mood);

                List<Song> allSongs = leerCancionesDesdeCSV();
                List<Song> recommendedSongs = recomendarPorMood(allSongs, mood);

                if (recommendedSongs.isEmpty()) {
                    enviarFailure(request, "No se han encontrado canciones para el mood: " + mood);
                    return;
                }

                String explanation = generarExplicacion(mood, recommendedSongs);

                PlaylistResult playlist = new PlaylistResult(mood, recommendedSongs, explanation);

                ACLMessage response = request.createReply();
                response.setPerformative(ACLMessage.INFORM);
                response.setContentObject(playlist);
                send(response);

                log("Playlist enviada correctamente con " + recommendedSongs.size() + " canciones.");

            } catch (UnreadableException e) {
                enviarFailure(request, "No se ha podido leer el objeto recibido.");
                logError("Error leyendo el contenido del mensaje", e);

            } catch (IOException e) {
                enviarFailure(request, "No se ha podido leer el fichero de canciones.");
                logError("Error leyendo canciones.csv", e);

            } catch (Exception e) {
                enviarFailure(request, "Error inesperado en el recomendador musical.");
                logError("Error inesperado", e);
            }
        }
    }

    private List<Song> leerCancionesDesdeCSV() throws IOException {
        List<Song> songs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_PATH))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(";", -1);

                if (fields.length < 6) {
                    log("Línea ignorada por formato incorrecto: " + line);
                    continue;
                }

                Song song = new Song(
                        fields[0].trim(),
                        fields[1].trim(),
                        fields[2].trim(),
                        fields[3].trim(),
                        normalizarMood(fields[4].trim()),
                        fields[5].trim()
                );

                songs.add(song);
            }
        }

        return songs;
    }

    private List<Song> recomendarPorMood(List<Song> allSongs, String mood) {
        return allSongs.stream()
                .filter(song -> normalizarMood(song.getMood()).equals(mood))
                .limit(NUM_RECOMMENDATIONS)
                .collect(Collectors.toList());
    }

    private String generarExplicacion(String mood, List<Song> songs) {
        return "Se recomiendan estas canciones porque están asociadas al estado de ánimo "
                + mood
                + ", que es la emoción detectada en el texto analizado.";
    }

    private String normalizarMood(String mood) {
        if (mood == null) {
            return "NEUTRO";
        }

        String normalized = mood.trim().toUpperCase();

        normalized = normalized
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");

        return normalized;
    }

    private void enviarFailure(ACLMessage request, String errorMessage) {
        try {
            ACLMessage response = request.createReply();
            response.setPerformative(ACLMessage.FAILURE);
            response.setContent(errorMessage);
            send(response);
            log("Enviado FAILURE: " + errorMessage);
        } catch (Exception e) {
            logError("No se pudo enviar FAILURE", e);
        }
    }
}