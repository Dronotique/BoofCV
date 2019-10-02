package boofcv.app.mjpeg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;



public class MjpegLiveViewCameraSimpleImpl extends MjpegLiveViewCamera {

	private MjpegInputStream mJpegIS;
	private long frameNum = 0;
	
	public MjpegLiveViewCameraSimpleImpl() {
		super();
	}

	public MjpegLiveViewCameraSimpleImpl(URL urlVideoServer) {
		super(urlVideoServer);
	}
	
	public MjpegLiveViewCameraSimpleImpl(URI urlVideoServer) {
		super(urlVideoServer);
	}
	
	private void init() {
		String contentType = cnx.getContentType();
		String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
		mJpegIS = new MjpegInputStream(super.urlStream, boundary);
	}

	
	@Override
	public boolean startLiveView() throws Exception {
		super.connectToServeur();
		init();
		liveViewStarted = true;
		(new Thread("HttpLiveViewCameraSimpleImpl"){
	  		public void run() {
	  			while(liveViewStarted){;
					try {
						BufferedImage nextImage = mJpegIS.readJpegAsBufferedImage();
						if(nextImage != null) {
							setNewBuffImage(nextImage);
							frameNum++;
						}
						
					}catch(SocketTimeoutException ste){
						ste.printStackTrace(System.out);
						//stopLiveView();
					}catch(IOException e){
						e.printStackTrace(System.out);
						//stopLiveView();
					}catch(Exception e) {
						e.printStackTrace(System.out);
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

	public long getFrameNum() {
		return frameNum;
	}

}
