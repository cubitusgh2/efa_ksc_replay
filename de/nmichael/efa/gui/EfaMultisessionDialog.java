package de.nmichael.efa.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeDistance;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.core.items.ItemTypeItemList.Orientation;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.DestinationRecord;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.International;

public class EfaMultisessionDialog extends BaseTabbedDialog implements IItemFactory, IItemListener {

	private static final int FIELD_HEIGHT=21;
    private static final int VERTICAL_WHITESPACE_PADDING_GROUPS=26;
    
    private long logbookValidFrom = 0;
    private long logbookInvalidFrom = 0;
    
    private boolean isLateEntry = false;
    private Logbook logbook;                    

    private ItemTypeLabel captionMultisession;
    private ItemTypeDate date;
    private ItemTypeDate enddate;
    private ItemTypeTime starttime;
    private ItemTypeTime endtime;
    private ItemTypeStringList sessiontype;
    private ItemTypeLabel sessionTypeInfo;
    
    private ItemTypeItemList participantWithBoat;
	
    private ItemTypeLabel captionTargetAndMore;
    private ItemTypeStringAutoComplete destination;
    private ItemTypeString destinationInfo;
    private ItemTypeStringAutoComplete waters;
    private ItemTypeDistance distance;
    
    private ItemTypeString comments;
	
	
    public EfaMultisessionDialog(Frame parent, String title, String closeButtonText, Boolean isLateEntry, Logbook logbook) {
        super(parent, title, closeButtonText, null, true);
        this.isLateEntry = isLateEntry;
        this.logbook = logbook;
        iniItems();
    }

    public EfaMultisessionDialog(JDialog parent, String title, String closeButtonText, Boolean isLateEntry, Logbook logbook) {
        super(parent, title, closeButtonText, null, true);
        this.isLateEntry = isLateEntry;
        this.logbook = logbook;
        iniItems();
    }	
	
    private void iniItems() {

	    logbookValidFrom = logbook.getValidFrom();
	    logbookInvalidFrom = logbook.getInvalidFrom();
            	
        Vector<IItemType> guiItems = new Vector<IItemType>();        
        
        AutoCompleteList autoCompleteListDestinations = new AutoCompleteList();
        AutoCompleteList autoCompleteListWaters = new AutoCompleteList();
    	
        autoCompleteListDestinations.setDataAccess(Daten.project.getDestinations(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
        autoCompleteListWaters.setDataAccess(Daten.project.getWaters(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
        
        //Caption
        captionMultisession = new ItemTypeLabelHeader("_GUIITEM_MULTISESSION_CAPTION", IItemType.TYPE_PUBLIC, null, 
        		" "+International.getString("Multi-Fahrt anlegen"));
        captionMultisession.setPadding(0, 0, 0, 10);
        captionMultisession.setFieldGrid(9,GridBagConstraints.EAST, GridBagConstraints.BOTH);

        // Date
        date = new ItemTypeDate(LogbookRecord.DATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Datum"));
        date.showWeekday(true);
        date.setFieldSize(100, FIELD_HEIGHT);
        date.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        date.setFieldGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setWeekdayGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
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
        enddate.setOptionalButtonText("+ " + International.getString("Enddatum"));
        
        enddate.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        enddate.registerItemListener(this);
        if (!Daten.efaConfig.getValueAllowEnterEndDate()) {
            enddate.setVisible(false);
        }
        
        // StartTime
        starttime = new ItemTypeTime(LogbookRecord.STARTTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Abfahrt"));
        starttime.setFieldSize(200, FIELD_HEIGHT);
        starttime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        starttime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttime.enableSeconds(false);
        starttime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        starttime.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        starttime.registerItemListener(this);


        // EndTime
        endtime = new ItemTypeTime(LogbookRecord.ENDTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Ankunft"));
        endtime.setFieldSize(200, FIELD_HEIGHT);
        endtime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        endtime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtime.enableSeconds(false);
        endtime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        endtime.registerItemListener(this);        
        
        // Session Type
        sessiontype = new ItemTypeStringList(LogbookRecord.SESSIONTYPE, EfaTypes.TYPE_SESSION_NORMAL,
                EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, null, International.getString("Fahrtart"));
        sessiontype.setFieldSize(200, FIELD_HEIGHT);
        sessiontype.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        sessiontype.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessiontype.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        sessiontype.registerItemListener(this);
        sessiontype.setReplaceValues(Daten.efaTypes.getSessionTypeReplaceValues());

        // Session Type Info
        sessionTypeInfo = new ItemTypeLabel("SESSIONTYPE_LABEL", IItemType.TYPE_PUBLIC, null, "");
        sessionTypeInfo.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessionTypeInfo.registerItemListener(this);
        sessionTypeInfo.activateMouseClickListener();

        Vector<IItemType[]> participantWithBoatList = new Vector<IItemType[]>();
        participantWithBoat = new ItemTypeItemList("GUIITEM_PARTICIPANTBOATLIST", participantWithBoatList, this,
                IItemType.TYPE_PUBLIC, "", International.getString("Teilnehmer und Boote"));
        participantWithBoat.setAppendPositionToEachElement(true);
        participantWithBoat.setRepeatTitle(false);
        participantWithBoat.setXForAddDelButtons(3);
        participantWithBoat.setPadYbetween(0);
        participantWithBoat.setItemsOrientation(Orientation.horizontal);
        //participantWithBoat.setFieldGrid(11,GridBagConstraints.EAST, GridBagConstraints.BOTH);
        
        //Caption
        captionTargetAndMore = new ItemTypeLabelHeader("_GUIITEM_TARGET_CAPTION", IItemType.TYPE_PUBLIC, null, 
        		" "+International.getString("Ziel und weitere Angaben"));
        captionTargetAndMore.setPadding(0, 0, 0, 10);
        captionTargetAndMore.setFieldGrid(9,GridBagConstraints.EAST, GridBagConstraints.BOTH);

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

        destination.registerItemListener(this);
        destinationInfo = new ItemTypeString("GUIITEM_DESTINATIONINFO", "",
                IItemType.TYPE_PUBLIC, null, International.getString("Gewässer"));
        destinationInfo.setFieldSize(400, FIELD_HEIGHT);
        destinationInfo.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destinationInfo.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destinationInfo.setEditable(false);
        destinationInfo.setVisible(false);

        // Waters
        waters = new ItemTypeStringAutoComplete("GUIITEM_ADDITIONALWATERS", "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Gewässer"), true);
        waters.setFieldSize(400, FIELD_HEIGHT);
        waters.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        waters.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        waters.setAutoCompleteData(autoCompleteListWaters);
        waters.setChecks(true, false);
        waters.setIgnoreEverythingAfter(LogbookRecord.WATERS_SEPARATORS);
        waters.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        waters.registerItemListener(this);
        waters.setVisible(true);

        // Distance
        distance = new ItemTypeDistance(LogbookRecord.DISTANCE, null, IItemType.TYPE_PUBLIC, null,
                DataTypeDistance.getDefaultUnitName());
        distance.setFieldSize(200, FIELD_HEIGHT);
        distance.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        distance.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        distance.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        distance.registerItemListener(this);

        // Comments
        comments = new ItemTypeString(LogbookRecord.COMMENTS, null, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Bemerkungen"));
        comments.setFieldSize(400, FIELD_HEIGHT);
        comments.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        comments.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        comments.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        comments.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        comments.registerItemListener(this);        

        
        guiItems.add(captionMultisession);
        guiItems.add(date);
        //if (guiItems.add(enddate);
        guiItems.add(starttime);
        guiItems.add(endtime);
        guiItems.add(sessiontype);
        guiItems.add(sessionTypeInfo);
        guiItems.add(participantWithBoat);
        guiItems.add(captionTargetAndMore);
        guiItems.add(destination);
        guiItems.add(destinationInfo);
        guiItems.add(waters);
        guiItems.add(distance);
        guiItems.add(comments);

        super.setItems(guiItems);
    }
    
    @Override
    public IItemType[] getDefaultItems(String itemName) {
    	
	    long logbookValidFrom = logbook.getValidFrom();
	    long logbookInvalidFrom = logbook.getInvalidFrom();
        // simply create an empty personid field
        Persons persons = Daten.project.getPersons(false);
        Boats boats = Daten.project.getBoats(false);
        IItemType[] items = new IItemType[2];
        items[0] = getGuiItemTypeStringAutoComplete("PERSONID", null,
                IItemType.TYPE_PUBLIC, "",
                persons, logbookValidFrom, logbookInvalidFrom - 1,
                International.getString("Name"));
        //items[0].setFieldSize(200, -1);

       
        items[1] = getGuiItemTypeStringAutoComplete("BOATID", null,
                IItemType.TYPE_PUBLIC, "",
                boats, logbookValidFrom, logbookInvalidFrom - 1,
                International.getString("Boot"));
        //items[1].setFieldSize(200, -1);
        
        //filter boats only for one-seaters
        ((ItemTypeStringAutoComplete) items [1]).getAutoCompleteData().setFilterDataOnlyOneSeaterBoats(true);
        
        return items;
    }
    
    protected ItemTypeStringAutoComplete getGuiItemTypeStringAutoComplete(String name, UUID value, int type, String category,
            StorageObject persistence, long validFrom, long validUntil,
            String description) {
        AutoCompleteList list = new AutoCompleteList();
        list.setDataAccess(persistence.data(), validFrom, validUntil);
        String svalue = (value != null ? list.getValueForId(value.toString()) : "");
        ItemTypeStringAutoComplete item = new ItemTypeStringAutoComplete(name, svalue, type, category, description, true);
        item.setFieldSize(200, 21); // 21 pixels high for new flatlaf, otherwise chars y and p get cut off 
        item.setAutoCompleteData(list);
        item.setChecks(true, true);
        return item;
    }
    
    public void itemListenerAction(IItemType item, AWTEvent e) {

    }
}
