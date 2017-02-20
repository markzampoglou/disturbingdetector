package gr.iti.mklab.reveal.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import gr.iti.mklab.reveal.dnn.api.QueueObject;
import gr.iti.mklab.reveal.dnn.api.ReportManagement;

import gr.iti.mklab.reveal.dnn.api.logObject;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;


@Controller
@RequestMapping("/disturbingdetector")
public class DisturbingDetector {
    private final String USER_AGENT = "Mozilla/5.0";

    public DisturbingDetector() throws Exception {
        Configuration.load(getClass().getResourceAsStream("/remote.properties"));
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

            String[] response_split=response.toString().split(":");

            JSONResponse.prediction=Float.valueOf(response_split[0]);
            JSONResponse.prediction_nsfw=Float.valueOf(response_split[1]);
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
        MongoClientURI mongoURI = new MongoClientURI(Configuration.MONGO_URI);
        MongoClient mongoclient = new MongoClient(mongoURI);
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
    public String getfrombytearray(@RequestPart(value = "bytearray", required = true) MultipartFile bytes, @RequestPart(value = "url", required = true) String url, @RequestPart(value = "collection", required = true) String collection, @RequestPart(value = "id", required = false) String itemId, @RequestPart(value = "type", required = true) String type) {
        MongoClientURI mongoURI = new MongoClientURI(Configuration.MONGO_URI);
        MongoClient mongoclient = new MongoClient(mongoURI);
        Morphia morphia = new Morphia();
        morphia.map(QueueObject.class);
        Datastore ds = new Morphia().createDatastore(mongoclient, "DisturbingQueue");
        ds.ensureCaps();

            try {
                String imgHash = null;
                byte[] urlBytes = (url + collection).getBytes("UTF-8");
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.reset();
                md.update(urlBytes);
                byte[] digest = md.digest();
                imgHash = String.format("%032x", new java.math.BigInteger(1, digest));
                System.out.println("Disturbing image downloader received a byteArray. " + url + " | Collection: " + collection + " | id: " + itemId + " | type: " + type + " | hash : " + imgHash);

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


    @RequestMapping(value = "/get_logs", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public logObject get_logs(){
        logObject JSONResponse=new logObject();
        String CatalinaLogPath="/opt/apache-tomcat-8.0.37/logs/catalina.out";
        String ManagerLogPath="/logs/disturbing-detect-manager.log";
        String PythonLogPath="/logs/disturbing-python-service.log";

        byte[] catByte = new byte[0];
        try {
            catByte = Files.readAllBytes(Paths.get(CatalinaLogPath));
            JSONResponse.CatalinaLog=new String(catByte);
            if (JSONResponse.CatalinaLog.length()>100000){
                JSONResponse.CatalinaLog=JSONResponse.CatalinaLog.substring(JSONResponse.CatalinaLog.length()-100000);
            }
            byte[] managerByte = Files.readAllBytes(Paths.get(ManagerLogPath));
            JSONResponse.ManagerLog=new String(managerByte);
            if (JSONResponse.ManagerLog.length()>100000) {
                JSONResponse.ManagerLog = JSONResponse.ManagerLog.substring(JSONResponse.ManagerLog.length() - 100000);
            }
            File f= new File(PythonLogPath);
            if (f.exists()) {
                byte[] pythonByte = Files.readAllBytes(Paths.get(PythonLogPath));
                JSONResponse.PythonLog = new String(pythonByte);
                if (JSONResponse.PythonLog.length() > 100000) {
                    JSONResponse.PythonLog = JSONResponse.PythonLog.substring(JSONResponse.PythonLog.length() - 100000);
                }
            }
            else {JSONResponse.PythonLog=new String("Not Found");}
        } catch (IOException e) {
            e.printStackTrace();
            if (JSONResponse.ErrorLog.length() > 100000) {
                JSONResponse.ErrorLog = JSONResponse.ErrorLog.substring(JSONResponse.ErrorLog.length() - 100000);
            }
        }
        return JSONResponse;
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