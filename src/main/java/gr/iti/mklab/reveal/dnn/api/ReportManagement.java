package gr.iti.mklab.reveal.dnn.api;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

// Logger to suppress MongoDB message flood

/**
 * Created by marzampoglou on 11/19/15.
 */
public class ReportManagement {

	public static String downloadURL(String urlIn, String folderOut, String hash) throws Exception {
		System.out.println("downloadURL");
		String imgHash = null;

		// connect to URL and get input stream
		URL imageURL = new URL(urlIn);
		File localDir = new File(folderOut);
		localDir.mkdir();

		try{
			InputStream inputStream = null;
			URLConnection urlConnection = null;
			int noOfBytes = 0;
			byte[] byteChunk = new byte[4096];
			ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
			urlConnection = imageURL.openConnection();
			urlConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
			urlConnection.connect();
			inputStream = urlConnection.getInputStream();
			while ((noOfBytes = inputStream.read(byteChunk)) > 0) {
				byteOutputStream.write(byteChunk, 0, noOfBytes);
			}
			File outputFolder=new File(folderOut);
			if (!outputFolder.exists())
				outputFolder.mkdirs();
			File imageFile = new File (folderOut,hash+".raw");
			OutputStream outputStream = new FileOutputStream(imageFile);

			byteOutputStream.writeTo(outputStream);
			outputStream.close();
			BufferedImage downloadedImage=ImageIO.read(imageFile);
			ImageIO.write(downloadedImage, "JPEG", new File(folderOut , hash + ".jpg"));
			// store in database image information
		} catch (Exception e) {
			System.out.println("ERROR1: The requested URL does not respond or does not exist. Exiting.");
			throw(e);
		}
		return folderOut + hash + ".jpg";
	}

	public static String savebytearray(MultipartFile filein, String folderOut, String hash) throws Exception {
		String imgHash = null;

		File localDir = new File(folderOut);
		localDir.mkdir();

		try{

			File outputFolder=new File(folderOut);
			if (!outputFolder.exists())
				outputFolder.mkdirs();
			File imageFile = new File (folderOut,"Raw_" + hash);
			filein.transferTo(imageFile);

			BufferedImage downloadedImage=ImageIO.read(imageFile);
			ImageIO.write(downloadedImage, "JPEG", new File(folderOut , hash + ".jpg"));
			imageFile.delete();
			// store in database image information
		} catch (Exception e) {
			System.out.println("ERROR1: Something went wrong in writing the bytestream. Exiting.");
			throw(e);
		}
		return folderOut +  hash + ".jpg";
	}

}
