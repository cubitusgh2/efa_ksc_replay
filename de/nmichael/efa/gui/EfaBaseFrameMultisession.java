package de.nmichael.efa.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.batik.ext.swing.GridBagConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.AdminTask;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeButton;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeDistance;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.data.DestinationRecord;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.EfaBaseFrame.EfaBaseFrameFocusManager;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

public class EfaBaseFrameMultisession extends EfaBaseFrame implements IItemListener, IItemFactory {

	private final static String  NOT_STORED_ITEM_PREFIX = "_";
	private final static String  STR_SPACER = "   ";
	private ItemTypeItemList nameAndBoat;
    private AutoCompleteList autoCompleteListSingleBoats = new AutoCompleteList();
    private JPanel teilnehmerUndBoot;
    

    public EfaBaseFrameMultisession(int mode) {
        super(mode);
    }

    public EfaBaseFrameMultisession(JDialog parent, int mode) {
        super(parent, mode);
    }
    
    public EfaBaseFrameMultisession(JDialog parent, int mode, AdminRecord admin,
            Logbook logbook, String entryNo) {
        super(parent, mode, admin, logbook, entryNo);
    }

    public EfaBaseFrameMultisession(EfaBoathouseFrame efaBoathouseFrame, int mode) {
        super(efaBoathouseFrame, mode);
    }
	

    /**
     * 
     */
    /* mostly same elements, but slightly diffrent order of the values */
    protected void iniGuiMain() {
    	int yPos=0;
    	int HEADER_WIDTH=9;
    	
        autoCompleteListSingleBoats.setDataAccess(Daten.project.getBoats(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
    	autoCompleteListSingleBoats.setFilterDataOnlyOneSeaterBoats(true);
    	
    	
        JPanel mainInputPanel = new JPanel();
        mainInputPanel.setLayout(new GridBagLayout());
        mainPanel.add(mainInputPanel, BorderLayout.CENTER);

        ItemTypeLabelHeader header = createHeader("CREATE_MULTISESSION", 0, null, 
        		(mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION ? International.getString("Multi-Fahrt anlegen") : International.getString("Multi-Nachtrag erfassen")),
        				HEADER_WIDTH);
        header.displayOnGui(this,  mainInputPanel, 0, yPos);
        yPos++;

        // Date, Enddate, optionally a button to append end date
        date = new ItemTypeDate(LogbookRecord.DATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Datum"));
        date.showWeekday(true);
        date.setFieldSize(100, FIELD_HEIGHT);
        date.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        date.setFieldGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setWeekdayGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        date.displayOnGui(this, mainInputPanel, 0, yPos);
        date.registerItemListener(this);

        // End Date
        enddate = new ItemTypeDate(LogbookRecord.ENDDATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("bis"));
        enddate.setMustBeAfter(date, false);
        enddate.showWeekday(true);
        enddate.setFieldSize(100, FIELD_HEIGHT);
        enddate.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        enddate.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        enddate.setWeekdayGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        enddate.showOptional(true);
        if (isModeBoathouse()) {
            enddate.setOptionalButtonText("+ " + International.getString("Enddatum"));
        }
        enddate.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        enddate.displayOnGui(this, mainInputPanel, 4, yPos);
        enddate.registerItemListener(this);
        if (isModeBoathouse() && !Daten.efaConfig.getValueAllowEnterEndDate()) {
            enddate.setVisible(false);
        }

        yPos++;

        
        // Start Time, End Time, including according labels  AND Session Type.
        
        // StartTime
        starttime = new ItemTypeTime(LogbookRecord.STARTTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Abfahrt"));
        starttime.setFieldSize(200, FIELD_HEIGHT);
        starttime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        starttime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttime.enableSeconds(false);
        starttime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        starttime.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        starttime.displayOnGui(this, mainInputPanel, 0, yPos);
        starttime.registerItemListener(this);

        starttimeInfoLabel = new ItemTypeLabel("GUIITEM_STARTTIME_INFOLABEL",
                IItemType.TYPE_PUBLIC, null, "");
        starttimeInfoLabel.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttimeInfoLabel.setVisible(false);
        starttimeInfoLabel.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);        
        starttimeInfoLabel.displayOnGui(this, mainInputPanel, 3, yPos);
        
        yPos++;

        // EndTime
        endtime = new ItemTypeTime(LogbookRecord.ENDTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Ankunft"));
        endtime.setFieldSize(200, FIELD_HEIGHT);
        endtime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        endtime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtime.enableSeconds(false);
        endtime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        endtime.displayOnGui(this, mainInputPanel, 0, yPos);
        endtime.registerItemListener(this);

        endtimeInfoLabel = new ItemTypeLabel("GUIITEM_ENDTIME_INFOLABEL",
                IItemType.TYPE_PUBLIC, null, "");
        endtimeInfoLabel.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtimeInfoLabel.setVisible(false);
        endtimeInfoLabel.displayOnGui(this, mainInputPanel, 3, yPos);

        endtime.setVisible(mode == EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION);
        endtimeInfoLabel.setVisible(endtime.isVisible());
        
        yPos++;
        
        // Session Type
        sessiontype = new ItemTypeStringList(LogbookRecord.SESSIONTYPE, EfaTypes.TYPE_SESSION_NORMAL,
                EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, null, International.getString("Fahrtart"));
        sessiontype.setFieldSize(200, FIELD_HEIGHT);
        sessiontype.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        sessiontype.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessiontype.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        sessiontype.displayOnGui(this, mainInputPanel, 0, yPos);
        sessiontype.registerItemListener(this);
        sessiontype.setReplaceValues(Daten.efaTypes.getSessionTypeReplaceValues());
        
        // Session Type Info
        sessionTypeInfo = new ItemTypeLabel("SESSIONTYPE_LABEL", IItemType.TYPE_PUBLIC, null, "");
        sessionTypeInfo.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessionTypeInfo.registerItemListener(this);
        sessionTypeInfo.activateMouseClickListener();
        sessionTypeInfo.displayOnGui(this, mainInputPanel, 5, yPos);
        
        yPos++;
        //---------------------------------------------------------------------
        
       /* header = createHeader("CREATE_CREW_BOAT", 0, null, International.getString("Teilnehmer und Boot"),HEADER_WIDTH);
        header.displayOnGui(this,  mainInputPanel, 0, yPos);
        yPos++;        
        */ 
        
        teilnehmerUndBoot=new JPanel();
        teilnehmerUndBoot.setLayout(new GridBagLayout());
        teilnehmerUndBoot.removeAll();
		teilnehmerUndBoot.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        mainInputPanel.add(teilnehmerUndBoot, new GridBagConstraints(0, yPos, HEADER_WIDTH, 1, 0, 0,
                GridBagConstants.WEST, GridBagConstants.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        
        
       // mainInputPanel.ad
		nameAndBoat = new ItemTypeItemList("NameAndBoat", new Vector<IItemType[]>(), this,
				IItemType.TYPE_PUBLIC, null,
				International.getString("Teilnehmer und Boot"));
		//crontab.setScrollPane(1000, 400);
		nameAndBoat.setRepeatTitle(false);        
		nameAndBoat.setAppendPositionToEachElement(true);
		nameAndBoat.setXForAddDelButtons(6); // two columns, both with name, edit field, autocomplete button
		nameAndBoat.setPadYbetween(0);	
		nameAndBoat.setItemsOrientation(ItemTypeItemList.Orientation.horizontal);
		nameAndBoat.setFieldGrid(8, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		nameAndBoat.setFirstColumnMinWidth(getLongestLabelTextWidth(mainInputPanel));
		//nameAndBoat.setFirstColumnMinWidth(mainInputPanelGrid.getLayoutDimensions()[0][0]);
		// Multisession means at least two persons with an individual boat are to go
		addTwoItems(nameAndBoat);
		nameAndBoat.displayOnGui(this, teilnehmerUndBoot, 0, 0);
		nameAndBoat.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		
        // Name (crew) and Boat (single boat items only)
                
        yPos++;
        
        header = createHeader("CREATE_DESTINATION", 0, null, International.getString("Ziel und weitere Angaben"),HEADER_WIDTH);
        header.displayOnGui(this,  mainInputPanel, 0, yPos);
        yPos++;

        // Destination
        destination = new ItemTypeStringAutoComplete(LogbookRecord.DESTINATIONNAME, "", IItemType.TYPE_PUBLIC, null, 
                International.getStringWithMnemonic("Ziel") + " / " +
                International.getStringWithMnemonic("Strecke"), true);
        destination.setFieldSize(400, FIELD_HEIGHT);
        destination.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destination.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destination.setAutoCompleteData(autoCompleteListDestinations);
        destination.setChecks(true, false);
        destination.setIgnoreEverythingAfter(DestinationRecord.DESTINATION_VARIANT_SEPARATOR);
        destination.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        destination.displayOnGui(this, mainInputPanel, 0, yPos);
        destination.registerItemListener(this);
        yPos++;
        
        destinationInfo = new ItemTypeString("GUIITEM_DESTINATIONINFO", "",
                IItemType.TYPE_PUBLIC, null, International.getString("Gewässer"));
        destinationInfo.setFieldSize(400, FIELD_HEIGHT);
        destinationInfo.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destinationInfo.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destinationInfo.displayOnGui(this, mainInputPanel, 0, yPos);
        destinationInfo.setEditable(false);
        destinationInfo.setVisible(false);
        yPos++;
        
        // Waters
        waters = new ItemTypeStringAutoComplete(GUIITEM_ADDITIONALWATERS, "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Gewässer"), true);
        waters.setFieldSize(400, FIELD_HEIGHT);
        waters.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        waters.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        waters.setAutoCompleteData(autoCompleteListWaters);
        waters.setChecks(true, false);
        waters.setIgnoreEverythingAfter(LogbookRecord.WATERS_SEPARATORS);
        waters.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        waters.displayOnGui(this, mainInputPanel, 0, yPos);
        waters.registerItemListener(this);
        waters.setVisible(false);        
        
        yPos++;
        	
        // Distance
        distance = new ItemTypeDistance(LogbookRecord.DISTANCE, null, IItemType.TYPE_PUBLIC, null,
                DataTypeDistance.getDefaultUnitName());
        distance.setFieldSize(200, FIELD_HEIGHT);
        distance.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        distance.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        distance.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        distance.displayOnGui(this, mainInputPanel, 0, yPos);
        distance.registerItemListener(this);     
        distance.setVisible(mode == EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION);
        
        yPos++;
        // Comments
        comments = new ItemTypeString(LogbookRecord.COMMENTS, null, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Bemerkungen"));
        comments.setFieldSize(400, FIELD_HEIGHT);
        comments.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        comments.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        comments.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        comments.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        comments.displayOnGui(this, mainInputPanel, 0, yPos);
        comments.registerItemListener(this);
        yPos++;
        
        //---------------- old *-----------------------
        
        
        // Boat
        boat = new ItemTypeStringAutoComplete(LogbookRecord.BOATNAME, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Boot"), true);
        boat.setFieldSize(200, FIELD_HEIGHT);
        boat.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        boat.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.BOTH);
        boat.setAutoCompleteData(autoCompleteListBoats);
        boat.setChecks(true, true);
        boat.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        boat.displayOnGui(this, mainInputPanel, 0, yPos);
        boat.registerItemListener(this);

        yPos++;


        // Crew
        crew = new ItemTypeStringAutoComplete[LogbookRecord.CREW_MAX];
        for (int i=1; i<=crew.length; i++) {
            int j = i-1;
            boolean left = ((j/4) % 2) == 0;
            crew[j] = new ItemTypeStringAutoComplete(LogbookRecord.getCrewFieldNameName(i), "", IItemType.TYPE_PUBLIC, null,
                    (i == 1 ? International.getString("Mannschaft") + " " : (i < 10 ? "  " :"")) + Integer.toString(i), true);
            crew[j].setPadding( (left ? 0 : 10), 0, 0, 0);
            crew[j].setFieldSize(200, FIELD_HEIGHT);
            crew[j].setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
            crew[j].setFieldGrid((left ? 2 : 3), GridBagConstraints.WEST, GridBagConstraints.NONE);
            crew[j].setAutoCompleteData(autoCompleteListPersons, true);
            crew[j].setChecks(true, true);
            crew[j].setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
            crew[j].displayOnGui(this, mainInputPanel, (left ? 0 : 4), yPos + j%4);
            crew[j].setVisible(j < 8);
            crew[j].registerItemListener(this);
        }
        crew1defaultText = crew[0].getDescription();

        //----------------------------


        yPos=yPos+4;



 
        // Info Label
        infoLabel.setForeground(Color.blue);
        infoLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        infoLabel.setText(" ");
        mainInputPanel.add(infoLabel,
                new GridBagConstraints(0, yPos, 8, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 20, 10, 20), 0, 0));
 
        // Save Button
        saveButton = new ItemTypeButton("SAVE", IItemType.TYPE_PUBLIC, null, International.getString("Eintrag speichern"));
        saveButton.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        saveButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        saveButton.displayOnGui(this, mainPanel, BorderLayout.SOUTH);
        saveButton.registerItemListener(this);

        // Set Valid Date and Time Fields for Autocomplete Lists
        boat.setValidAt(date, starttime);

        for (int i=0; i<crew.length; i++) {
            crew[i].setValidAt(date, starttime);
        }
        destination.setValidAt(date, starttime);

    }    
	
    
    protected void iniDialog() {

    	iniGuiBase();
        iniGuiMain();
    }    
    
    private int getLongestLabelTextWidth(JPanel panel) {
    	
    	int lBemerk = panel.getFontMetrics(panel.getFont()).stringWidth(International.getString("Bemerkungen")+": ");
    	int lSessType = panel.getFontMetrics(panel.getFont()).stringWidth(International.getString("Fahrtart")+": ");
    	int lDest = panel.getFontMetrics(panel.getFont()).stringWidth(International.getStringWithMnemonic("Ziel") + " / " + International.getStringWithMnemonic("Strecke")+": ");
    	
    	return Math.max(lBemerk, Math.max(lSessType, lDest));
    	
    	
    }
    
    
	/**
	 * Adds a header item in an efa GUI. This header value is not safed within
	 * efaConfig. There is no word-wrap for the caption.
	 * 
	 * The header automatically gets a blue background and white text color; this
	 * cannot be configured as efaConfig cannot refer to its own settings whenn
	 * calling the constructor.
	 * 
	 * @param uniqueName Unique name of the element (as for all of efaConfig
	 *                   elements need unique names)
	 * @param type       TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
	 * @param category   Category in which the header is placed
	 * @param caption    Caption
	 * @param gridWidth  How many GridBagLayout cells shall this header be placed
	 *                   in?
	 */
	private ItemTypeLabelHeader createHeader(String uniqueName, int type, String category, String caption, int gridWidth) {
		// ensure that the header value does not get saved in efaConfig file by adding a
		// special prefix
		ItemTypeLabelHeader item = new ItemTypeLabelHeader(NOT_STORED_ITEM_PREFIX + uniqueName, type, category, " " + caption);
		item.setPadding(0, 0, 10, 10);
		item.setFieldGrid(gridWidth, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		return item;
	}    

	public void itemListenerAction(IItemType item, AWTEvent event) {
		super.itemListenerAction(item, event);
	}	

	/**
	 * Creates an Item consisting of Name and Boat
	 * Where boat only contains single person boats
	 * 
	 */
    public IItemType[] getDefaultItems(String itemName) {

    	ItemTypeStringAutoComplete[] items = new ItemTypeStringAutoComplete[2];
        //Name
        	items[0] = getGuiNameAutoComplete(itemName+"NAME_LOOKUP", International.getString("Name"));
	        items[0].setFieldSize(200, -1);
	        
	    // Boat
        	items[1] = getGuiBoatAutoComplete(itemName+"BOAT_LOOKUP", STR_SPACER+International.getString("Boot"));
	        items[1].setFieldSize(200, -1);
	        
        return items;
        
    }
    
    private void addTwoItems(ItemTypeItemList target) {
	    target.addItems(this.getDefaultItems(target.getName()));
	    target.addItems(this.getDefaultItems(target.getName()));
    }
    
    /*
     * updateGui()
     * This method is called when an ItemTypeList gets new items or gets items removed.
     * Usually, this happens on a BaseTabbedDialog. But unfortunately, efaBaseFrame is just a BaseDialog.
     * 
     * The only component which grows on this screen is "nameAndBoat". 
     * It is put in a specific panel. So if nameAndBoat changes its items, we just have to remove all components
     * from the container Panel teilnehmerUndBoot, and just re-add nameAndBoat to that panel.
     * This leads to a screen refresh.
     */
    public void updateGui() {
    	teilnehmerUndBoot.removeAll();
		nameAndBoat.displayOnGui(this, teilnehmerUndBoot, 0, 1);
		this.pack();
    }
	
    protected ItemTypeStringAutoComplete getGuiNameAutoComplete(String name, String description) {
        AutoCompleteList list = this.autoCompleteListPersons;
        ItemTypeStringAutoComplete item = new ItemTypeStringAutoComplete(name, "", IItemType.TYPE_PUBLIC , null, description, true);
        item.setFieldSize(200, FIELD_HEIGHT); // 21 pixels high for new flatlaf, otherwise chars y and p get cut off 
        item.setAutoCompleteData(list);
        item.setChecks(true, true);
        return item;
    }    
    
    protected ItemTypeStringAutoComplete getGuiBoatAutoComplete(String name, String description) {
        AutoCompleteList list = this.autoCompleteListSingleBoats;
        ItemTypeStringAutoComplete item = new ItemTypeStringAutoComplete(name, "", IItemType.TYPE_PUBLIC , null, description, true);
        item.setFieldSize(200, FIELD_HEIGHT); // 21 pixels high for new flatlaf, otherwise chars y and p get cut off 
        item.setAutoCompleteData(list);
        item.setChecks(true, true);
        return item;
    }        
    
    // Datensatz speichern
    // liefert "true", wenn erfolgreich
    protected boolean saveEntry() {
        if (!isLogbookReady()) {
            return false;
        }

        // Da das Hinzufügen eines Eintrags in der Bootshausversion wegen des damit verbundenen
        // Speicherns lange dauern kann, könnte ein ungeduldiger Nutzer mehrfach auf den "Hinzufügen"-
        // Button klicken. "synchronized" hilft hier nicht, da sowieso erst nach Ausführung des
        // Threads der Klick ein zweites Mal registriert wird. Da aber nach Abarbeitung dieser
        // Methode der Frame "EfaFrame" vom Stack genommen wurde und bei der zweiten Methode damit
        // schon nicht mehr auf dem Stack ist, kann eine Überprüfung, ob der aktuelle Frame
        // "EfaFrame" ist, benutzt werden, um eine doppelte Ausführung dieser Methode zu verhindern.
        if (Dialog.frameCurrent() != this) {
            return false;
        }
        
        // make sure to autocomplete all texts once more in the input fields.
        // users have found strange ways of working around completion...
        autocompleteAllFields();

        // run all checks before saving this entry
        if (!checkMisspelledInput() ||
            !checkDuplicatePersons() ||
            !checkPersonsForBoatType() ||
            !checkDuplicateEntry() ||
            !checkEntryNo() ||
            !checkBoatCaptain() ||
            !checkBoatStatus() ||
            !checkMultiDayTours() ||
            !checkSessionType() ||
            !checkDate() ||
            !checkTime() ||
            !checkAllowedDateForLogbook() ||
            !checkAllDataEntered() ||
            !checkNamesValid() ||
            !checkUnknownNames() ||
            !checkProperUnknownNames() ||
            !checkAllowedPersons()) {
            return false;
        }

        boolean success = saveEntriesInLogbook();

        if (isModeFull()) {
            if (success) {
                setEntryUnchanged();
                boolean createNewRecord = false;
                if (isModeFull()) { // used to be: getMode() == MODE_BASE
                    try {
                        LogbookRecord rlast = (LogbookRecord) logbook.data().getLast();
                        if (currentRecord.getEntryId().equals(rlast.getEntryId())) {
                            createNewRecord = true;
                        }
                    } catch (Exception eignore) {
                    }
                }
                if (createNewRecord) {
                    createNewRecord(false);
                } else {
                    entryno.requestFocus();
                }
            }
        } else {
            finishBoathouseAction(success);
        }
        autoCompleteListPersons.reset();
        return success;
    }

    // den Datensatz nun wirklich speichern;
    protected boolean saveEntriesInLogbook() {
    	return false;
    	/*
        if (!isLogbookReady()) {
            return false;
        }

        long lock = 0;
        Exception myE = null;
        try {
            boolean changeEntryNo = false;
            if (!isNewRecord && currentRecord != null && !currentRecord.getEntryId().toString().equals(entryno.toString())) {
                // Datensatz mit geänderter LfdNr: Der alte Datensatz muß gelöscht werden!
                lock = logbook.data().acquireGlobalLock();
                logbook.data().delete(currentRecord.getKey(), lock);
                changeEntryNo = true;
            }
            currentRecord = getFields();
            
            if (mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT) {
                currentRecord.setSessionIsOpen(true);
            } else {
                currentRecord.setSessionIsOpen(false); // all other updates to an open entry (incl. Admin Mode) will mark it as finished
            }

            if (isNewRecord || changeEntryNo) {
                logbook.data().add(currentRecord, lock);
            } else {
                DataRecord newRecord = logbook.data().update(currentRecord, lock);
                if (newRecord != null && newRecord instanceof LogbookRecord) {
                    currentRecord = (LogbookRecord)newRecord;
                }
            }
            isNewRecord = false;
        } catch (Exception e) {
            Logger.log(e);
            myE = e;
        } finally {
            if (lock != 0) {
                logbook.data().releaseGlobalLock(lock);
            }
        }
        if (myE != null) {
            Dialog.error(International.getString("Fahrtenbucheintrag konnte nicht gespeichert werden.") + "\n" + myE.toString());
            return false;
        }

        if (isModeFull()) {
            logAdminEvent(Logger.INFO, (isNewRecord ? Logger.MSG_ADMIN_LOGBOOK_ENTRYADDED : Logger.MSG_ADMIN_LOGBOOK_ENTRYMODIFIED),
                    (isNewRecord ? International.getString("Eintrag hinzugefügt") : International.getString("Eintrag geändert")) , currentRecord);
        }
        return true;*/
    }	
	
}
