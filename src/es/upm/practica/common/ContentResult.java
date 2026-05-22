package es.upm.practica.common;

import java.io.Serializable;

  // Objeto que transporta el texto extraído por el agente de percepción.
public class ContentResult implements Serializable{
    private static final long serialVersionUID = 1L;
 
    private String text;
 
    public ContentResult(String text) {
        this.text = text;
    }
 
    public String getText() {
        return text;
    }
 
    @Override
    public String toString() {
        return "ContentResult{text='" + (text != null ? text.substring(0, Math.min(text.length(), 80)) + "..." : "null") + "'}";
    }
}
