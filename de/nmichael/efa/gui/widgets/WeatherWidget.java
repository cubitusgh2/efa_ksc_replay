/**
 * 
 */
package de.nmichael.efa.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeLongLat;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedPanel;
import de.nmichael.efa.util.International;

/**
 * 
 */
public class WeatherWidget extends Widget {

    static final String PARAM_SHOWWEATHER         = "ShowWeather";
    static final String PARAM_LATITUDE            = "WeatherLatitude";
    static final String PARAM_LONGITUDE           = "WeatherLongitude";
    static final String PARAM_TEMPERATURESCALE    = "TemperatureScale";
    static final String TEMP_CELSIUS              = "CELSIUS";
    static final String TEMP_FAHRENHEIT           = "FAHRENHEIT";
    
    private JPanel mainPanel = new JPanel();
    private RoundedPanel roundPanel = new RoundedPanel();
    private RoundedPanel titlePanel = new RoundedPanel();
    private JLabel titleLabel = new JLabel();
    private JLabel curWeather_temp = new JLabel();
    private JLabel curWeather_icon = new JLabel();
    private JLabel curWeather_minTemp = new JLabel();
    private JLabel curWeather_maxTemp = new JLabel();
    private JLabel curWeather_wind = new JLabel();
    		
    
	/**
	 * @param name
	 * @param description
	 * @param ongui
	 * @param showRefreshInterval
	 */
	public WeatherWidget() {
    	super(International.getString("Wetter"), "Wetter", International.getString("Wetter"), true, true);
    	
     /*   addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHER, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Wetterdaten anzeigen") +
                " (" + International.getString("Internetverbindung erforderlich") + ")"));
       */ 
        addHeader("WeatherWidgetLocationHeader",IItemType.TYPE_PUBLIC, "", International.getString("Wetter Daten"), 3);  
        
        addParameterInternal(new ItemTypeLongLat(PARAM_LATITUDE,
                ItemTypeLongLat.ORIENTATION_NORTH,52,25,9,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Breite")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LONGITUDE,
                ItemTypeLongLat.ORIENTATION_EAST,13,10,15,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Länge")));
        
        addParameterInternal(new ItemTypeStringList(PARAM_TEMPERATURESCALE, TEMP_CELSIUS,
                new String[] { TEMP_CELSIUS, TEMP_FAHRENHEIT },
                new String[] { International.getString("Celsius"),
                               International.getString("Fahrenheit")
                },
                IItemType.TYPE_PUBLIC, "",
                International.getString("Temperaturskala")));
        
        super.setEnabled(true);
        super.setPosition(IWidget.POSITION_CENTER);
        
	}


	@Override
	public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void construct() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());		
		roundPanel = new RoundedPanel();
		roundPanel.setLayout(new GridBagLayout());
		roundPanel.setBackground(Daten.efaConfig.getToolTipBackgroundColor());
		roundPanel.setForeground(Daten.efaConfig.getToolTipForegroundColor());
		roundPanel.setBorder(new RoundedBorder(mainPanel.getForeground()));

		mainPanel.add(roundPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));

		titlePanel = new RoundedPanel();
		titlePanel.setLayout(new GridBagLayout());
		titlePanel.setBackground(Daten.efaConfig.getToolTipHeaderBackgroundColor());
		titlePanel.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());
		
		titleLabel.setText("HANNOVER");
		titleLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		titleLabel.setForeground(titlePanel.getForeground());
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		
		titlePanel.add(titleLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,2,2,2), 0, 0));

		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, /*3*/4, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,2,2,2), 0, 0));
		roundPanel.setMinimumSize(new Dimension(240,120));;
		//mainPanel.setPreferredSize(new Dimension(240,140));
		//addCurrentWeather();

		addForeCast();
		
		
	}


	private void addCurrentWeather() {
		curWeather_temp = new JLabel();
		curWeather_temp.setForeground(mainPanel.getForeground());
		curWeather_temp.setFont(mainPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+10)));
		//curWeather_temp.setFont(curWeather_temp.getFont().deriveFont(Font.BOLD));
		curWeather_temp.setText("28 °C");
		curWeather_temp.setHorizontalTextPosition(SwingConstants.LEFT);
		
		curWeather_icon.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WEATHER_116_64));
		
		curWeather_minTemp.setText("Min: 14 °C");
		curWeather_minTemp.setForeground(mainPanel.getForeground());
		curWeather_minTemp.setHorizontalTextPosition(SwingConstants.RIGHT);
		
		curWeather_maxTemp.setText("Max: 30 °C");
		curWeather_maxTemp.setForeground(mainPanel.getForeground());
		curWeather_maxTemp.setHorizontalTextPosition(SwingConstants.RIGHT);
		
		curWeather_wind.setText("Wind: SO mit 11 km/h");
		curWeather_wind.setForeground(mainPanel.getForeground());
		curWeather_wind.setHorizontalTextPosition(SwingConstants.CENTER);
		
		roundPanel.add(curWeather_temp, new GridBagConstraints(0, 1, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,4,2,4), 0, 0));

		roundPanel.add(curWeather_icon, new GridBagConstraints(1, 1, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,4,0,4), 0, 0));

		roundPanel.add(curWeather_minTemp, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(4,4,0,4), 0, 0));
		roundPanel.add(curWeather_maxTemp, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,4,0,4), 0, 0));
		roundPanel.add(curWeather_wind,    new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, new Insets(0,2,0,2), 0, 0));
	}

	private void addForeCast() {
		roundPanel.add(addForeCastPanel("14:00", ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WEATHER_116_48), "28 °C"),
				 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4,2,2,18), 0, 0));
		roundPanel.add(addForeCastPanel("16:00", ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WEATHER_113_48), "24 °C"),
				 new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,0,2,18), 0, 0));
		roundPanel.add(addForeCastPanel("18:00", ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WEATHER_320_48), "20 °C"),
				 new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,0,2,18), 0, 0));
		roundPanel.add(addForeCastPanel("20:00", ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WEATHER_386_48), "18 °C"),
				 new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,0,2,4), 0, 0));
		
		
	}
	
	private JPanel addForeCastPanel(String time, ImageIcon image, String temp) {
		JPanel myPanel= new JPanel();
		JLabel topLabel = new JLabel();
		JLabel centerLabel= new JLabel(image);
		JLabel bottomLabel = new JLabel();
		
		myPanel.setForeground(Daten.efaConfig.getToolTipForegroundColor());
		myPanel.setBackground(Daten.efaConfig.getToolTipBackgroundColor());
		
		topLabel.setForeground(myPanel.getForeground());
		bottomLabel.setForeground(myPanel.getForeground());
		
		topLabel.setText(time);
		topLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		topLabel.setFont(topLabel.getFont().deriveFont(Font.BOLD));
		/* centerLabel.setIcon(image);
		   centerLabel.setText("");
		   centerLabel.setIconTextGap(0);
		*/
		bottomLabel.setText(temp);
		bottomLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		
/*		
		myPanel.setLayout(new GridBagLayout());
		myPanel.add(topLabel,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,3,2,3), 0, 0));
		myPanel.add(centerLabel,      new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,3,2,3), 0, 0));
		myPanel.add(bottomLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,3,2,), 0, 0));
*/
		myPanel.setLayout(new BorderLayout(0,0));
		myPanel.add(topLabel,  BorderLayout.NORTH );
		myPanel.add(centerLabel, BorderLayout.CENTER);
		myPanel.add(bottomLabel, BorderLayout.SOUTH);

		return myPanel;
		
	}
	
	@Override
	public JComponent getComponent() {
		// TODO Auto-generated method stub
		return mainPanel;
	}

}
