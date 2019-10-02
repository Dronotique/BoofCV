package boofcv.app.mjpeg;


import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Opens a dialog and lets the user configure the camera and select which one
 *
 * @author Aurélien TRICAULT - Dronotique
 */
public class OpenMjpegCamDialog  extends StandardAlgConfigPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	JDialog dialog;

	JFormattedTextField fieldUrl,fieldWidth,fieldHeight;
	String url;
	int width,height;

	JCheckBox cSave = new JCheckBox("Save");

	JButton bCancel = new JButton("Cancel");
	JButton bOK = new JButton("OK");

	volatile boolean concluded = false;


	public OpenMjpegCamDialog( JDialog dialog ) {
		this.dialog = dialog;
		setBorder(BorderFactory.createEmptyBorder(6,6,6,6));


		bOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionOK();
			}
		});
		bCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionCancel();
			}
		});

		cSave.setSelected(true);
		
		fieldUrl  = new JFormattedTextField("http://localhost:8080");//BoofSwingUtil.createTextField(0,0,20000);
		fieldWidth = BoofSwingUtil.createTextField(0,0,20000);
		fieldHeight = BoofSwingUtil.createTextField(0,0,20000);

		fieldUrl.setPreferredSize(new Dimension(150,30));
		fieldWidth.setPreferredSize(new Dimension(150,30));
		fieldHeight.setPreferredSize(new Dimension(150,30));
		fieldUrl.setMaximumSize(fieldWidth.getPreferredSize());
		fieldWidth.setMaximumSize(fieldWidth.getPreferredSize());
		fieldHeight.setMaximumSize(fieldHeight.getPreferredSize());

		fieldUrl.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				url = fieldUrl.getValue().toString();
			}
		});
		
		fieldWidth.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				width = Integer.parseInt(fieldWidth.getValue().toString());
			}
		});
		fieldHeight.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				height = Integer.parseInt(fieldHeight.getValue().toString());
			}
		});

		final JPanel urlPanel = new JPanel();
		urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.X_AXIS));
		urlPanel.add( fieldUrl );
		urlPanel.setMaximumSize(urlPanel.getPreferredSize());
		
		final JPanel sizePanel = new JPanel();
		sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.X_AXIS));
		sizePanel.add( fieldWidth );
		sizePanel.add( Box.createHorizontalStrut(10));
		sizePanel.add( fieldHeight );
		sizePanel.setMaximumSize(sizePanel.getPreferredSize());

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add( bCancel );
		buttonPanel.add( Box.createHorizontalGlue());
		buttonPanel.add( bOK );
//		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		dialog.getRootPane().setDefaultButton(bOK);

		final JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.X_AXIS));
		checkPanel.add( cSave );
		checkPanel.add( Box.createHorizontalGlue());
//		checkPanel.setMaximumSize(checkPanel.getPreferredSize());

		addLabeled(urlPanel,"URL",this);
		addLabeled(sizePanel,"Size",this);
		add(checkPanel);
		add(buttonPanel);

		loadPreferences();
	}

	public boolean loadPreferences() {
		Preferences prefs = Preferences.userRoot().node(getClass().getSimpleName());

		String url = prefs.get("url","http://localhost:8080");
		final int width = prefs.getInt("width",800);
		final int height = prefs.getInt("height",600);

		try {
			fieldUrl.setValue(url);
			fieldWidth.setValue(Integer.toString(width));
			fieldHeight.setValue(Integer.toString(height));
		}catch (Exception ex) {
			
		}

		return true;
	}

	public void savePreferences() {
		Preferences prefs = Preferences.userRoot().node(getClass().getSimpleName());

		prefs.put("url",fieldUrl.getValue().toString());
		prefs.putInt("width",width);
		prefs.putInt("height",height);

		try {
			prefs.flush();
		} catch (BackingStoreException ignore) {}
	}

	public static Selection showDialog( Window owner )
	{
		JDialog dialog = new JDialog(owner,"MJPEG Camera",Dialog.ModalityType.APPLICATION_MODAL);
		final OpenMjpegCamDialog panel = new OpenMjpegCamDialog(dialog);

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				panel.actionCancel();
			}
		});
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(panel,BorderLayout.CENTER);
		dialog.setSize(new Dimension(400,200));
//		dialog.pack();
		if( owner != null )
			dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		// should block at this point
		dialog.dispose();

		if( panel.url != null && !"".equals(panel.url)) {
			Selection s = new Selection();
			s.url = panel.getUrl();
			s.width = panel.getSelectedWidth();
			s.height = panel.getSelectedHeight();
			return s;
		} else {
			return null;
		}
	}

	public void actionOK() {
		if( cSave.isSelected()) {
			savePreferences();
		}
		dialog.setVisible(false);
		concluded = true;
	}

	public void actionCancel() {
		url = null;
		dialog.setVisible(false);
		concluded = true;
	}

	public String getUrl() {
		return url;
	}

	public int getSelectedWidth() {
		return width;
	}

	public int getSelectedHeight() {
		return height;
	}

	public static class Selection {
		public String url;
		public int width,height;
	}

	public static void main(String[] args) {
		Selection s = OpenMjpegCamDialog.showDialog(null);
		if( s != null )
			System.out.println("Selected camera. "+s.width+" "+s.height+" "+s.url);
		else
			System.out.println("Didn't select camera");
		System.exit(0);
	}
}