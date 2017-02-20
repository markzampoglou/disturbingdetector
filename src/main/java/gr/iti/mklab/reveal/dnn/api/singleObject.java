package gr.iti.mklab.reveal.dnn.api;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by marzampoglou on 6/28/16.
 */
@Entity
public class singleObject {
    public float prediction=-10000;
    public float prediction_nsfw=-10000;
}
