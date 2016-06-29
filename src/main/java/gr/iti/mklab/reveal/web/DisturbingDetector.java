package gr.iti.mklab.reveal.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import gr.iti.mklab.reveal.dnn.api.QueueObject;
import gr.iti.mklab.reveal.dnn.api.ReportManagement;

import gr.iti.mklab.reveal.dnn.api.singleObject;
import gr.iti.mklab.reveal.util.Configuration;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;


@Controller
@RequestMapping("/disturbingdetector")
public class DisturbingDetector {
    private final String USER_AGENT = "Mozilla/5.0";

    public DisturbingDetector() throws Exception {
        Configuration.load(getClass().getResourceAsStream("/docker.properties"));
    }

    // Suppress MongoDB logging
    static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);
    static {
        root.setLevel(Level.WARN);
    }

    @PreDestroy
    public void cleanUp() throws Exception {
        System.out.println("Spring Container destroy");
        //  MorphiaManager.tearDown();
    }

    ///////////////////////////////////////////////////////
    /////////// DOWNLOAD IMAGE     ////////////////////////
    ///////////////////////////////////////////////////////

    @RequestMapping(value = "/classify_violent", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public singleObject classify_violent(@RequestParam(value = "imageurl", required = true) String url){
        System.out.println("synchronous URL disturbing detector demand received");
        singleObject JSONResponse=new singleObject();
        try {
            String imgHash = null;
            byte[] urlBytes = url.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(urlBytes);
            byte[] digest=md.digest();
            imgHash = String.format("%032x", new java.math.BigInteger(1, digest));
            System.out.println("Hash : " + imgHash);

            String filePath=ReportManagement.downloadURL(url, Configuration.QUEUE_IMAGE_PATH, imgHash);
            System.out.println("Downloaded:");
            System.out.println(filePath);

            URL serviceUrl = new URL("http://localhost:5000/classify_violent?imagepath=" + filePath);
            HttpURLConnection con = (HttpURLConnection) serviceUrl.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            //(new File(Configuration.QUEUE_IMAGE_PATH + imgHash + ".jpg")).delete();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONResponse.prediction=Float.valueOf(response.toString());
            return JSONResponse;

        } catch (Exception ex) {
            ex.printStackTrace();
            return JSONResponse;
        }

    }

    @RequestMapping(value = "/getfromurltuqueue", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getfromurltoqueue(@RequestParam(value = "url", required = true) String url){
        System.out.println("disturbing image detector initialized with URL");
        MongoClient mongoclient = new MongoClient(Configuration.MONGO_HOST, 27017);
        Morphia morphia = new Morphia();
        morphia.map(QueueObject.class);
        Datastore ds = new Morphia().createDatastore(mongoclient, "DisturbingQueue");
        ds.ensureCaps();
        try {

            String imgHash = null;
            byte[] urlBytes = url.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(urlBytes);
            byte[] digest=md.digest();
            imgHash = String.format("%032x", new java.math.BigInteger(1, digest));
            System.out.println("Hash : " + imgHash);


            QueueObject queueItem = ds.get(QueueObject.class, imgHash);
            while (queueItem != null && imgHash.length()<50) {
                imgHash=imgHash+"_";
                queueItem = ds.get(QueueObject.class, imgHash);
            }
            ReportManagement.downloadURL(url, Configuration.QUEUE_IMAGE_PATH, imgHash);

            queueItem = new QueueObject();
            queueItem.id=imgHash;
            queueItem.processing=false;
            queueItem.sourceURL=url;
            ds.save(queueItem);
            mongoclient.close();

/*
            //URL serviceUrl = new URL("http://localhost:5000/classify_violent?imagepath=" + downloadedFilePath);
            HttpURLConnection con = (HttpURLConnection) serviceUrl.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            */
            return "ok";
        } catch (Exception ex) {
            ex.printStackTrace();
            mongoclient.close();
            return "failed";
        }

    }

    @RequestMapping(value = "/getfrombytearray", method = RequestMethod.POST, produces = "application/json", headers = "content-type=multipart/*", consumes = {"multipart/form-data"})
    @ResponseBody
    public String getfrombytearray(@RequestPart(value = "bytearray", required = true) MultipartFile bytes, @RequestPart(value = "url", required = true) String url, @RequestPart(value = "collection", required = true) String collection, @RequestPart(value = "id", required = false) String itemId, @RequestPart(value = "type", required = true) String type){

        System.out.println("Disturbing image downloader received a byteArray. " + url + " | Collection: " + collection + " | id: " + itemId + " | type: " + type);
        MongoClient mongoclient = new MongoClient(Configuration.MONGO_HOST, 27017);
        Morphia morphia = new Morphia();
        morphia.map(QueueObject.class);
        Datastore ds = new Morphia().createDatastore(mongoclient, "DisturbingQueue");
        ds.ensureCaps();

        try {
            String imgHash = null;
            byte[] urlBytes = (url+collection).getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(urlBytes);
            byte[] digest=md.digest();
            imgHash = String.format("%032x", new java.math.BigInteger(1, digest));
            System.out.println("Hash : " + imgHash);


            QueueObject queueItem = ds.get(QueueObject.class, imgHash);
            if (queueItem == null) {

                try {
                    ReportManagement.savebytearray(bytes, Configuration.QUEUE_IMAGE_PATH, imgHash);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                queueItem = new QueueObject();
                queueItem.id = imgHash;
                queueItem.processing = false;
                queueItem.sourceURL = url;
                queueItem.itemId = itemId;
                queueItem.collection = collection;
                queueItem.type = type;
                ds.save(queueItem);
            /*
            URL serviceUrl = new URL("http://localhost:5000/classify_violent?imagepath=" + downloadedFilePath);
            HttpURLConnection con = (HttpURLConnection) serviceUrl.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();


            return response.toString();
            */
            }
            mongoclient.close();
            return "ok";

        } catch (Exception ex) {
            ex.printStackTrace();
            mongoclient.close();
            return "failed";
        }

    }
    ////////////////////////////////////////////////////////
    ///////// EXCEPTION HANDLING ///////////////////////////
    ///////////////////////////////////////////////////////

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RevealException.class)
    @ResponseBody
    public RevealException handleCustomException(RevealException ex) {
        return ex;
    }


    public static void main(String[] args) throws Exception {
    }
}