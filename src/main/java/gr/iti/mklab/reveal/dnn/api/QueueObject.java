package gr.iti.mklab.reveal.dnn.api;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by marzampoglou on 6/17/16.
 */

@Entity
public class QueueObject {
    public @Id String id;
    public String sourceURL;
    public boolean processing = false;
    public String itemId;
    public String type;
    public double value = 10000;
    public String collection;
}
