package boofcv.app.mjpeg;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import javax.imageio.ImageIO;


public class MjpegLiveViewCamera {
	
	private BufferedImage nextBuffImage;

	protected int nbreTentativeCnx = 3;
	
	private static final String CONTENT_LENGTH = "Content-length: ";
	private static final String CONTENT_TYPE = "Content-type: image/jpeg";
	private StringWriter stringWriter = new StringWriter(128);
	
	private static String cameraHost = "http://localhost";
	private static String cameraPort = "8080";
	private static URL defaultUrlVideoServer;
	
	static {
		try {
			defaultUrlVideoServer = new URL("http", cameraHost, Integer.parseInt(cameraPort), "");
		} catch (MalformedURLException e) {
			e.printStackTrace(System.out);
		}
	}
	
	protected InputStream urlStream;
	protected boolean liveViewStarted = false; 
	protected URLConnection cnx;
	private URL urlVideoServer = null;
	private URI uriVideoServer = null;
	
	public MjpegLiveViewCamera() {
		this(defaultUrlVideoServer);
	}
	
	public MjpegLiveViewCamera(URL urlVideoServer) {
		this.urlVideoServer = urlVideoServer;
		
	}
	
	public MjpegLiveViewCamera(URI uriVideoServer) {
		this.uriVideoServer = uriVideoServer;
		
	}
	
	public String getName() {
		return "Http Camera";
	}

	protected void connectToServeur() throws Exception {
		for(int i=1; i <= nbreTentativeCnx; i++) {
			try {
				if(urlVideoServer != null) {
					cnx = urlVideoServer.openConnection();
					cnx.setReadTimeout(5000);
					urlStream = cnx.getInputStream();
				}else {
					//Mode URI pour TCP
					Socket socketClient =  new Socket(uriVideoServer.getHost(), uriVideoServer.getPort());
					urlStream = socketClient.getInputStream();
					BufferedReader bf = new BufferedReader(new InputStreamReader(urlStream));
					String line;
					while(bf.ready()) {
						line = bf.readLine();
						if(line != "-1") {
							System.out.println(Base64.getDecoder().decode(line));
						}
					}
				}
				break;
			} catch (IOException ioe) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				if(i == nbreTentativeCnx) {
					throw ioe;
				}
			}
		}
	}
	
	public boolean startLiveView() throws Exception {
		connectToServeur();
		liveViewStarted = true;
		(new Thread("HttpLiveViewCamera"){
	  		public void run() {
	  			while(liveViewStarted){;
					try {
						byte[] imageBytes = retrieveNextImage();
						ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
						
						setNewBuffImage(ImageIO.read(bais));
					}catch(SocketTimeoutException ste){
						 ste.printStackTrace(System.err);
						//stopLiveView();
					}catch(IOException e){
						e.printStackTrace(System.err);
						//stopLiveView();
					}catch(Exception e) {
						e.printStackTrace(System.err);
					}
				}
	  			try {
					urlStream.close();
				} catch (IOException e) {
				}
			}
	  	}).start();
		return true ;
	}
	
	protected byte[] retrieveNextImage() throws IOException
	{
		boolean haveHeader = false; 
		int currByte = -1;
		
		String header = null;
		// build headers
		// the DCS-930L stops it's headers
		while((currByte = urlStream.read()) > -1 && !haveHeader)
		{
			stringWriter.write(currByte);
			
			String tempString = stringWriter.toString(); 
			int indexOf = tempString.indexOf(CONTENT_TYPE);
			if(indexOf > 0)
			{
				haveHeader = true;
				header = tempString;
			}
		}		
		
		// 255 indicates the start of the jpeg image
		while((urlStream.read()) != 255)
		{
			// just skip extras
		}
		
		// rest is the buffer
		int contentLength = contentLength(header);
		byte[] imageBytes = new byte[contentLength + 1];
		// since we ate the original 255 , shove it back in
		imageBytes[0] = (byte)255;
		int offset = 1;
		int numRead = 0;
		while (offset < imageBytes.length
			&& (numRead=urlStream.read(imageBytes, offset, imageBytes.length-offset)) >= 0) 
		{
			offset += numRead;
		}       
		
		stringWriter = new StringWriter(128);
		
		return imageBytes;
	}
	
	// dirty but it works content-length parsing
	private static int contentLength(String header)
	{
		int indexOfContentLength = header.indexOf(CONTENT_LENGTH);
		int valueStartPos = indexOfContentLength + CONTENT_LENGTH.length();
		int indexOfEOL = header.indexOf('\n', indexOfContentLength);
		
		String lengthValStr = header.substring(valueStartPos, indexOfEOL).trim();
		
		int retValue = Integer.parseInt(lengthValStr);
		
		return retValue;
	}

	public boolean stopLiveView() throws Exception {
		liveViewStarted = false;
		try {
			urlStream.close();
		} catch (IOException e) {
		}
		return true;
	}
	
	public void setNewBuffImage(BufferedImage img) {
		nextBuffImage = img;
	}

	public BufferedImage getNextBufferedImage() {
		return nextBuffImage;
	}


}
