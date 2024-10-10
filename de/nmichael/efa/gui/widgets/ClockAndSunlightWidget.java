/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Plugins;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeLongLat;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.NotificationDialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.SunRiseSet;
import de.nmichael.efa.util.TMJ;  

public class ClockAndSunlightWidget extends Widget {

    static final String PARAM_SHOWCOCK         = "ShowClock";
    static final String PARAM_SHOWDATE         = "ShowDate";
	
    static final String PARAM_SHOWSUNRISE         = "ShowSunrise";
    static final String PARAM_LATITUDE            = "Latitude";
    static final String PARAM_LONGITUDE           = "Longitude";

    static final String PARAM_WARNDARKNESS          = "WarnDarkness";
    static final String PARAM_WARNTIMEBEFORESUNSET  = "WarnTimeBeforeSunset";
    static final String PARAM_WARNTIMEAFTERSUNSET   = "WarnTimeAfterSunset";
    static final String PARAM_WARNTIMEBEFORESUNRISE = "WarnTimeBeforeSunrise";
    static final String PARAM_WARNTEXTDARKSOON      = "WarnTextDarkSoon";
    static final String PARAM_WARNTEXTDARKNOW       = "WarnTextDarkNow";
    
    static final String LAYOUT_HORIZONTAL         = "HORIZONTAL";
    static final String LAYOUT_VERTICAL           = "VERTICAL";

    static final String PARAM_LAYOUT              = "Layout";

    private JPanel mainPanel = new JPanel();
    private JLabel timeLabel = new JLabel();
    private JLabel dateLabel = new JLabel();
    private JPanel sunPanel = new JPanel();
    private JLabel sunriseLabel = new JLabel();
    private JLabel sunsetLabel = new JLabel();
    private PanelUpdater panelUpdater;

    public ClockAndSunlightWidget() {
        super(International.getString("Uhr und Tageslicht"), "MeteoAstro", International.getString("Uhr und Tageslicht"), true,false);

        addParameterInternal(new ItemTypeStringList(PARAM_LAYOUT, LAYOUT_HORIZONTAL,
                new String[] { LAYOUT_HORIZONTAL, LAYOUT_VERTICAL },
                new String[] { International.getString("horizontal"),
                               International.getString("vertikal")
                },
                IItemType.TYPE_PUBLIC, "",
                International.getString("Layout")));
        
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWCOCK, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Uhrzeit anzeigen")));
        
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWDATE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Datum anzeigen")));
        
        addHeader("WidgetMeteoSunrise",IItemType.TYPE_PUBLIC, "", International.getString("Sonnenaufgang/Sonnenuntergang"), 3);        
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWSUNRISE, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Sonnenaufgangs- und -untergangszeit anzeigen")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LATITUDE,
                ItemTypeLongLat.ORIENTATION_NORTH,52,25,9,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Breite")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LONGITUDE,
                ItemTypeLongLat.ORIENTATION_EAST,13,10,15,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Länge")));
        
        addHeader("WidgetMeteoAstroWarning",IItemType.TYPE_PUBLIC, "", International.getString("Warnhinweise bei Dunkelheit"), 3);          
        addParameterInternal(new ItemTypeBoolean(PARAM_WARNDARKNESS, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Bei Fahrtbeginn vor Dunkelheit warnen")));
        addParameterInternal(new ItemTypeString(PARAM_WARNTEXTDARKSOON,
                International.getString("Achtung, es wird bald dunkel.") + "<br>" +
                International.getString("Bitte nimm Licht mit!"),
                IItemType.TYPE_PUBLIC, "",
                International.getString("Warntext vor Einbruch der Dunkelheit")));
        addParameterInternal(new ItemTypeString(PARAM_WARNTEXTDARKNOW,
                International.getString("Achtung, es ist dunkel.") + "<br>" +
                International.getString("Bitte nimm Licht mit!"),
                IItemType.TYPE_PUBLIC, "",
                International.getString("Warntext bei Dunkelheit")));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEBEFORESUNSET, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEBEFORESUNSET));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEAFTERSUNSET, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEAFTERSUNSET));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEBEFORESUNRISE, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEBEFORESUNRISE));

        super.setEnabled(true);
        super.setPosition(IWidget.POSITION_CENTER);
        
    }

    public boolean isShowSunrise() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWSUNRISE)).getValue();
    }

    public boolean isShowClock() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWCOCK)).getValue();   	
    }
    
    public boolean isShowDate() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWDATE)).getValue();   	
    }
    
    public ItemTypeLongLat getLatitude() {
        return (ItemTypeLongLat)getParameterInternal(PARAM_LATITUDE);
    }

    public ItemTypeLongLat getLongitude() {
        return (ItemTypeLongLat)getParameterInternal(PARAM_LONGITUDE);
    }

    public boolean isWarnDarkness() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_WARNDARKNESS)).getValue();
    }
    public int getWarnTimeBeforeSunset() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEBEFORESUNSET)).getValue();
    }
    public int getWarnTimeAfterSunset() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEAFTERSUNSET)).getValue();
    }
    public int getWarnTimeBeforeSunrise() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEBEFORESUNRISE)).getValue();
    }
    public String getWarnTextDarkSoon() {
        return ((ItemTypeString)getParameterInternal(PARAM_WARNTEXTDARKSOON)).toString();
    }
    public String getWarnTextDarkNow() {
        return ((ItemTypeString)getParameterInternal(PARAM_WARNTEXTDARKNOW)).toString();
    }

    
    public String getLayout() {
        return ((ItemTypeStringList)getParameterInternal(PARAM_LAYOUT)).toString();   
    }
    

    void construct() {

       	/* Layout, created by plain java
    	 
        12:23
   20.03.2024 
(*) 08:20 (o) 18:23

	 */  

	mainPanel = new JPanel();
	mainPanel.setLayout(new GridBagLayout());
	sunPanel.setLayout(new GridBagLayout());
	sunPanel.setOpaque(false);
	Boolean isHorzLayout=getLayout().equals(LAYOUT_HORIZONTAL);
	
	if (isHorzLayout) {
		/*      
		     20:23     (*) 08:00
		  20.12.2024   (o) 18:00
		  
		    or
		    
		    20:23      (*) 08:00
		               (o) 18:00
		    
		    or
		    
		  20.12.2024   (*) 08:00
		               (o) 18:00
		               
		*/
		
		sunPanel.add(sunriseLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		sunPanel.add(sunsetLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(4,0,0,0), 0, 0)); 

		if (isShowClock() && isShowDate()) {
			//date and time in separate rows
			if (isShowSunrise()) {
				mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0));
				mainPanel.add(dateLabel,   new GridBagConstraints(0,1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 4, 0), 0, 0));
			} else {
				mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
				mainPanel.add(dateLabel,   new GridBagConstraints(1,0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 40, 0, 0), 0, 0));
			}
		} else if (isShowClock() && ! isShowDate()) {
			//only add timelabel, span over two rows
			mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0));
		} else if (!isShowClock() && isShowDate()) {
			mainPanel.add(dateLabel,   new GridBagConstraints(0,0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0));
		}

		mainPanel.add(sunPanel,    new GridBagConstraints(1,0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 40, 0, 0), 0, 0));

	} else {
		sunPanel.add(sunriseLabel, new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,4), 0, 0));
		sunPanel.add(sunsetLabel,  new GridBagConstraints(1,0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,4), 0, 0));  
		
		mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
		mainPanel.add(dateLabel,   new GridBagConstraints(0,1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 4, 0), 0, 0));
		mainPanel.add(sunPanel,    new GridBagConstraints(0,2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 0), 0, 0));
	}

	sunriseLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_SUNRISE));
	sunriseLabel.setIconTextGap(2);
	sunsetLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_SUNSET));
	sunsetLabel.setIconTextGap(2);
	
	//Time label shall be bold and huge
	timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
	dateLabel.setHorizontalAlignment(SwingConstants.CENTER);    	
    	
    	try {
            panelUpdater = new PanelUpdater(mainPanel);
            panelUpdater.start();
            
        } catch(Exception e) {
            Logger.log(e);
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void stop() {
        try {
        	// stopHTML also lets the thread die, and efaBths is responsible to set up a new thread.
            panelUpdater.stopRunning();
        } catch(Exception eignore) {
            // nothing to do, might not be initialized
        }
    }

    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
        try {
            if ((mode == EfaBaseFrame.MODE_BOATHOUSE_START ||
                 mode == EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT ||
                 mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION) && !actionBegin &&
                 isWarnDarkness()) {
                Calendar cal = new GregorianCalendar();
                int now = cal.get(Calendar.HOUR_OF_DAY)*60 + cal.get(Calendar.MINUTE);
                String sun[] = SunRiseSet.getSunRiseSet(getLatitude(), getLongitude());
                TMJ tmj = EfaUtil.string2date(sun[0], -1, -1, 0);
                int sunrise = (tmj.tag >= 0 && tmj.monat >= 0 ? tmj.tag*60 + tmj.monat : -1);
                tmj = EfaUtil.string2date(sun[1], -1, -1, 0);
                int sunset = (tmj.tag >= 0 && tmj.monat >= 0 ? tmj.tag*60 + tmj.monat : -1);
                if (sunset < 0 || sunrise < 0 || now < 0) {
                    return;
                }
                String warnText = null;
                if (now <= sunrise-getWarnTimeBeforeSunrise() || now >= sunset+getWarnTimeAfterSunset()) {
                    warnText = getWarnTextDarkNow();
                } else if (now >= sunset-getWarnTimeBeforeSunset() && now < sunset+getWarnTimeAfterSunset()) {
                    warnText = getWarnTextDarkSoon();
                }
                if (warnText != null && Daten.efaConfig.getValueNotificationWindowTimeout() > 0) {
                    NotificationDialog dlg = new NotificationDialog((JFrame)null,
                            warnText,
                            BaseDialog.BIGIMAGE_DARKNESS,
                            "ffffff", "ff0000", Daten.efaConfig.getValueNotificationWindowTimeout());
                    dlg.showDialog();
                }
            }
        } catch(Exception eignore) {
            Logger.logdebug(eignore);
        }
    }

    class PanelUpdater extends Thread {

        volatile boolean keepRunning = true;
        private boolean sunriseError = false;
        private JPanel panel;

        public PanelUpdater(JPanel thePanel) {

        	this.panel=thePanel;
        
        }

        /*
         * Gets the remaining seconds until the next full minute.
         * This sets the update interval.
         */
        private long getMilliSecondsToFullMinute() {
            LocalDateTime start = LocalDateTime.now();
            // Hour + 1, set Minute and Second to 00
            LocalDateTime end = start.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);

            // Get Duration
            Duration duration = Duration.between(start, end);
            long millis = duration.toMillis();
            return millis;
        };
        
        public void run() {
        	this.setName("MeteoAstroWidget.PanelUpdater");
            String clockValue=null;
            String dateValue=null;
            String sunriseValue=null;
            String sunsetValue=null;
            
            while (keepRunning) {
            	
            	try {
	            	clockValue= (isShowClock() ? EfaUtil.getCurrentTimeStampHHMM().toString() : null);
	            	dateValue=(isShowDate() ? EfaUtil.getCurrentTimeStampInDateFormat().toString() : null);
	
	                // Sunrise and Sunset
	                if (isShowSunrise()) {
	                    try {
	                        String sun[] = SunRiseSet.getSunRiseSet(getLatitude(), getLongitude());
	                        sunriseValue = sun[0];
	                        sunsetValue = sun[1];
	                    } catch (NoClassDefFoundError e) {
	                        sunriseValue = "--:--";
	                        sunsetValue = "--:--";
	                        if (!sunriseError) {
	                            Logger.log(Logger.WARNING, Logger.MSG_CORE_MISSINGPLUGIN,
	                                    International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_JSUNTIMES + " - "
	                                    + International.getString("Die Sonnenaufgangs- und Untergangszeiten können nicht angezeigt werden.") + " "
	                                    + International.getMessage("Bitte lade das fehlende Plugin unter der Adresse {url} herunter.", Daten.pluginWebpage));
	                        }
	                        sunriseError = true;
	                    } catch (Exception ee) {
	                        Logger.logdebug(ee);
	                        sunriseValue = "--:--";
	                        sunsetValue = "--:--";
	                    }
	                }
	            	
	            	//Use invokelater as swing threadsafe ways
	            	SwingUtilities.invokeLater(new UpdateClockAstroRunner(this.panel, clockValue, dateValue, sunriseValue, sunsetValue));
	
	            	
	            	//wait until next full minute plus one sec. this is more accurate than just waiting 60.000 msec
	            	//from a random offset.
	           		long waitTime=getMilliSecondsToFullMinute()+1000;
	                Thread.sleep(waitTime);

            	} catch (InterruptedException e) {
                	//This is when the thread gets interrupted when it is sleeping.
                	EfaUtil.foo();            
                } catch (Exception e) {
                	Throwable t = e.getCause();
                	if (t.getClass().getName().equalsIgnoreCase("java.lang.InterruptedException")) {
                		EfaUtil.foo();
                	} else {
                		Logger.logdebug(e);
                	}
                }
	                
            }
        }
        
        public synchronized void stopRunning() {
            keepRunning = false;
            interrupt(); // wake up thread
        }

    }

    private class UpdateClockAstroRunner implements Runnable {
        
    	private JPanel panel=null;
    	private String clockValue = null;
    	private String dateValue = null;
    	private String sunriseValue = null;
    	private String sunsetValue = null;
    	
    	public UpdateClockAstroRunner(JPanel targetPanel, String sclockValue, String sdateValue, String ssunriseValue, String ssunsetValue) {
    		panel = targetPanel;
    		this.clockValue = sclockValue;
    		this.dateValue = sdateValue;
    		this.sunriseValue = ssunriseValue;
    		this.sunsetValue = ssunsetValue;
    	}
    	
    	public void run() {
    		try {
	    		timeLabel.setVisible(clockValue!=null);
	    		dateLabel.setVisible(dateValue!=null);
	    		sunPanel.setVisible(sunriseValue!=null);
	    		
	    		timeLabel.setText(clockValue);
	    		dateLabel.setText(dateValue);
	    		sunriseLabel.setText(sunriseValue);
	    		sunsetLabel.setText(sunsetValue);
	    		
    			mainPanel.setFont(mainPanel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()));	    		
	    		timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
	    		if (Daten.efaConfig != null) {
	    			if (dateValue==null) {
	    				timeLabel.setFont(timeLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+10));
	    			} else {
	    				timeLabel.setFont(timeLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+6));
	    			}
	    			if (clockValue==null) {
	    				dateLabel.setFont(dateLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+6));
	    				dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
	    			} else {
	    				if (isShowSunrise()) {
		    				dateLabel.setFont(dateLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()));
	    				} else {
		    				dateLabel.setFont(dateLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+6));
	    				}	    					
	    				dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN));
		    				
	    			}


	    		}
	
	    		panel.repaint();
    		} catch (Exception e){
    			Logger.log(e);
    		}
    	}
    }

}
