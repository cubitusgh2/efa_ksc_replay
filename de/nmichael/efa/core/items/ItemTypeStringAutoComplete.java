/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.SimpleOptionInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaSortStringComparator;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

/*
 * Input field for Strings, auto-completing using an AutoCompleteList.
 *
 * This version does not depend on AutoCompletePopupWindow or
 * AutoCompletePopupWindowCallback anymore.
 *
 * The popup is implemented using standard Swing components:
 * - JPopupMenu
 * - JList
 * - JScrollPane
 * - custom ListCellRenderer
 *
 * Key handling:
 * - DOWN / UP / PAGE DOWN / PAGE UP: move selection in popup
 * - ENTER: accept selected item
 * - ESC: close popup without accepting
 * - typing: updates the popup/filter from the text field
 *
 * The popup is "remote-controlled" from the text field; focus remains in the text field.
 */
public class ItemTypeStringAutoComplete extends ItemTypeString {

    protected boolean showButton; // is the status button (green/orange/red) visible?
    protected boolean showButtonFocusable; // is the status button focusable with keyboard?
    protected boolean useAutocompleteList;
    protected JButton button;
    protected Color originalButtonColor;
    protected AutoCompleteList autoCompleteList;
    protected Object rememberedId;
    protected boolean withPopup = true;
    protected boolean valueIsKnown = false;
    protected boolean isCheckSpelling = false;
    protected boolean isCheckPermutations = false;
    protected boolean isVisibleSticky = true;
    protected String ignoreEverythingAfter = null;
    protected String alternateFieldNameForPlainText = null;
    protected boolean alwaysReturnPlainText = false;
    protected ItemTypeDate validAtDateItem;
    protected ItemTypeTime validAtTimeItem;
    protected boolean onChoosenDeleteFromList = false;
    protected ItemTypeStringAutoComplete otherField;

    private JPopupMenu popup;
    private JList<String> popupList;
    private JScrollPane popupScrollPane;

    public ItemTypeStringAutoComplete(String name, String value, int type,
            String category, String description, boolean showButton) {
        super(name, value, type, category, description);
        this.showButton = showButton;
        this.useAutocompleteList = Daten.efaConfig == null || Daten.efaConfig.getValuePopupComplete();
    }

    public ItemTypeStringAutoComplete(String name, String value, int type,
            String category, String description, boolean showButton,
            AutoCompleteList autoCompleteList) {
        super(name, value, type, category, description);
        this.showButton = showButton;
        this.useAutocompleteList = Daten.efaConfig == null || Daten.efaConfig.getValuePopupComplete();
        setAutoCompleteData(autoCompleteList);
    }

    public IItemType copyOf() {
        ItemTypeStringAutoComplete copy = new ItemTypeStringAutoComplete(name, value, type, category, description, showButton, autoCompleteList);
        copy.setFieldSize(fieldWidth, fieldHeight);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        copy.setIsItemOnSameRowAsPreviousItem(itemOnSameRowAsPreviousItem);
        copy.setItemOnNewRow(itemOnNewRow);
        copy.setFieldGrid(fieldGridWidth, fieldGridHeight, fieldGridAnchor, fieldGridFill);
        return copy;
    }

    public void setValidAt(ItemTypeDate validAtDate, ItemTypeTime validAtTime) {
        this.validAtDateItem = validAtDate;
        this.validAtTimeItem = validAtTime;
    }

    public void iniDisplay() {
        if (showButton) {
            button = new JButton();
            originalButtonColor = button.getBackground();
            button.setFocusable(showButtonFocusable);
            Dialog.setPreferredSize(button, fieldHeight - 4, fieldHeight - 8);
            button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    buttonPressed(e);
                }
            });
            button.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    field_focusLost(e);
                }
                public void focusGained(FocusEvent e) {
                    field_focusGained(e);
                }
            });
        }

        super.iniDisplay();

        ensurePopupCreated();

        final JTextField tf = (JTextField) this.field;

        // TAB aus Focus-Traversal-Keys entfernen, damit KeyListener es bekommt
        tf.setFocusTraversalKeysEnabled(false);

        iniDisplay_KeyListener(tf);

        tf.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                field_focusLost(e);
            }
            public void focusGained(FocusEvent e) {
                field_focusGained(e);
            }
        });
    }
    
    /*
	 * KeyListener for the text field to handle auto-completion and popup navigation.
	 * ENTER, TAB: Use the selected item from the popup if visible, otherwise transfer focus.
	 * ESC: Clear the text field and close the popup if visible.
	 * Other keys: Call autoComplete() to update the text field and popup.
	 */
    private void iniDisplay_KeyListener(JTextField tf) {
        tf.addKeyListener(new KeyAdapter() {

        	/*
        	 * keyPressed works best for keys which are usually hit once.
        	 * all other keys (navigation) are handled in keyReleased()
        	 */
        	public void keyPressed(KeyEvent e) {
        	    if (handlePopupNavigationKey(e)) {
        	        e.consume();
        	        return;
        	    }

        	    if (e.getKeyCode() == KeyEvent.VK_ENTER && isPopupVisible()) {
        	        acceptPopupSelection();
        	        e.consume();
    	            ((JTextField) field).transferFocus();
        	        return;
        	    } 
        	    
        	    if (e.getKeyCode() == KeyEvent.VK_TAB && isPopupVisible()) {
        	        acceptPopupSelection();
        	        e.consume();
    	            ((JTextField) field).transferFocus();
        	        return;
        	    }

        	    // TAB ohne Popup: Fokus manuell weitergeben
        	    if (e.getKeyCode() == KeyEvent.VK_TAB) {
        	        if (isPopupVisible()) {
        	            acceptPopupSelection();
        	        }
        	        e.consume();
        	        if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
        	            ((JTextField) field).transferFocusBackward();
        	        } else {
        	            ((JTextField) field).transferFocus();
        	        }
        	        return;
        	    }
        	    
        	    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        	    	// ESC: no, we do not want this value to be taken.
        	    	// and the textfield shall be cleared
        	        if (field instanceof JTextField) {
        	            JTextField tf = (JTextField) field;
        	            tf.setText("");
        	            tf.setCaretPosition(0);
        	        }

        	        // Close the popup if neccessary
        	        if (isPopupVisible()) {
        	            closePopup();
        	        }

        	        // Do not consume() the event. -> super ESC-handlers shall see the key
        	        return;
        	    }

        	    if (e.getKeyCode() == KeyEvent.VK_ENTER && Daten.efaConfig.getValuePopupContainsMode()) {
        	        autoComplete(e);
        	        e.consume();
        	    }
        	}


            public void keyReleased(KeyEvent e) {
                if (e == null) {
                    return;
                }

                int code = e.getKeyCode();

                // Navigationstasten dürfen NICHT nochmal in autoComplete() landen.
                if (code == KeyEvent.VK_UP
                        || code == KeyEvent.VK_DOWN
                        || code == KeyEvent.VK_LEFT
                        || code == KeyEvent.VK_RIGHT
                        || code == KeyEvent.VK_PAGE_UP
                        || code == KeyEvent.VK_PAGE_DOWN
                        || code == KeyEvent.VK_HOME
                        || code == KeyEvent.VK_END
                        || code == KeyEvent.VK_ESCAPE
                        || code == KeyEvent.VK_ENTER
                        || code == KeyEvent.VK_TAB) {
                    return;
                }

                autoComplete(e);
            }
        });
    }


    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        int plusy = super.displayOnGui(dlg, panel, x, y);
        if (button != null) {
            panel.add(button, new GridBagConstraints(x + labelGridWidth + fieldGridWidth, y, 1, fieldGridHeight, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, padYafter, 0), 0, 0));
        }
        return plusy;
    }

    public void setAutoCompleteData(AutoCompleteList autoCompleteList) {
        this.autoCompleteList = autoCompleteList;
    }

    public void setAutoCompleteData(AutoCompleteList autoCompleteList, boolean deleteFromList) {
        this.autoCompleteList = autoCompleteList;
        this.onChoosenDeleteFromList = deleteFromList;
    }

    public AutoCompleteList getAutoCompleteData() {
        return this.autoCompleteList;
    }

    public void setChecks(boolean checkSpelling, boolean checkPermutations) {
        this.isCheckSpelling = checkSpelling;
        this.isCheckPermutations = checkPermutations;
    }

    public void setIgnoreEverythingAfter(String s) {
        ignoreEverythingAfter = s;
    }

    public void setVisible(boolean visible) {
        if (visible == true && isVisibleSticky() == false) {
            return;
        }
        super.setVisible(visible);
        if (button != null) {
            button.setVisible(visible);
        }
    }

    public void setVisibleSticky(boolean visible) {
        isVisibleSticky = visible;
        setVisible(visible);
    }

    public boolean isVisibleSticky() {
        return isVisibleSticky;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    public void showValue() {
        super.showValue();
        autoComplete(null);
    }

    public void setId(Object id) {
        if (autoCompleteList != null && id != null) {
            value = autoCompleteList.getValueForId(id.toString());
        }
    }

    public Object getId(String qname) {
        return (autoCompleteList != null ? autoCompleteList.getId(qname) : null);
    }

    public void setRememberedId(Object id) {
        rememberedId = id;
    }

    public Object getRememberedId() {
        return rememberedId;
    }

    protected void field_focusLost(FocusEvent e) {
        if (e != null && e.isTemporary()) {
            return;
        }
        if (useAutocompleteList) {
            closePopup();
        }
        if (isCheckSpelling && Daten.efaConfig != null && Daten.efaConfig.getValueCorrectMisspelledNames()) {
            checkSpelling();
        }
        super.field_focusLost(e);
        if (onChoosenDeleteFromList && valueIsKnown && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if (vis.remove(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
    }

    protected void field_focusGained(FocusEvent e) {
        if (onChoosenDeleteFromList && valueIsKnown && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if (!vis.contains(value) && vis.add(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
        super.field_focusGained(e);
    }

    public void showOrRemoveAutoCompletePopupWindow() {
        if (!useAutocompleteList) {
            return;
        }
        JTextField f = (JTextField) field;
        if (f.isEnabled() && f.isEditable()) {
            if (!isPopupVisible()) {
                showPopupForField(f);
            } else {
                closePopup();
            }
        }
    }

    private void buttonPressed(ActionEvent e) {
        showOrRemoveAutoCompletePopupWindow();
        actionEvent(e);
    }

    private void autoComplete(KeyEvent e) {
        if (Daten.efaConfig.getValuePopupContainsMode()) {
            handleFilteredList(e);
            return;
        }

        if (field == null) {
            return;
        }
        JTextField field = (JTextField) this.field;

        AutoCompleteList list = getAutoCompleteList();
        if (list == null) {
            setButtonColor(null);
            closePopup();
            return;
        } else {
            list.update();
        }

        if (e != null && e.getKeyCode() == -23) {
            return;
        }

        if (field.getText().trim().length() == 0) {
            setButtonColor(null);
        }

        String complete = null;
        String prefix = null;
        String base = null;

        Mode mode = Mode.none;
        if (e == null || ((EfaUtil.isRealChar(e) && (e.getKeyCode() != KeyEvent.VK_ENTER) && (e.getKeyCode() != KeyEvent.VK_TAB))
                || (e.getKeyCode() == KeyEvent.VK_DOWN))) {
            mode = Mode.normal;
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            mode = Mode.up;
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            mode = Mode.delete;
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            mode = Mode.enter;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mode = Mode.escape;
        }

        if (e == null || mode == Mode.enter || mode == Mode.escape) {
            field.setText(field.getText().trim());
        }

        boolean matching = false;

        if (mode == Mode.normal
                || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none) && field.getText().length() > 0)) {

            String spc = field.getText();
            if (spc.startsWith(" ")) {
                int i = 0;
                do {
                    i++;
                } while (i < spc.length() && spc.charAt(i) == ' ');
                if (i >= spc.length()) {
                    field.setText("");
                } else {
                    field.setText(spc.substring(i));
                }
            }

            if (field.getSelectedText() != null) {
                prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
            } else {
                prefix = field.getText().toLowerCase();
            }

            if (e != null && e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (isPopupVisible() && useAutocompleteList) {
                    complete = list.getNext();
                } else {
                    complete = list.getNext(prefix);
                }
                if (complete == null) {
                    complete = list.getFirst(prefix);
                }
            } else {
                if (e != null) {
                    complete = list.getFirst(prefix);
                } else {
                    complete = list.getExact(field.getText().toLowerCase());
                }
                if (list.getAlias(prefix) != null) {
                    complete = list.getAlias(prefix);
                }
            }

            if (e == null && complete != null) {
                complete = list.getExact(complete);
            }
            if (complete != null) {
                if (e != null && mode != Mode.none) {
                    field.setText(complete);
                    field.select(prefix.length(), complete.length());
                }
                matching = true;
            }

            if (withPopup && useAutocompleteList && e != null && mode != Mode.none) {
                showPopupForField(field);
                updatePopupSelection(complete != null ? complete : field.getText(), prefix);
            }
        }

        if (mode == Mode.up) {
            if (field.getSelectedText() != null) {
                prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
            } else {
                prefix = field.getText().toLowerCase();
            }

            if (isPopupVisible() && useAutocompleteList) {
                complete = list.getPrev();
            } else {
                complete = list.getPrev(prefix);
            }

            if (complete == null) {
                complete = list.getLast(prefix);
            }
            if (complete != null) {
                field.setText(complete);
                field.select(prefix.length(), complete.length());
                matching = true;
            }
            if (withPopup && useAutocompleteList) {
                showPopupForField(field);
                updatePopupSelection(complete != null ? complete : field.getText(), prefix);
            }
        }

        if (mode == Mode.delete) {
            if ((complete = list.getFirst(field.getText().toLowerCase().trim())) != null
                    && (complete.equals(field.getText()))) {
                matching = true;
            }
        }

        String ignoredString = null;
        int ignorePos = -1;
        for (int i = 0; ignoreEverythingAfter != null && i < ignoreEverythingAfter.length(); i++) {
            ignorePos = (prefix != null ? prefix.indexOf(ignoreEverythingAfter.charAt(i))
                    : field.getText().indexOf(ignoreEverythingAfter.charAt(i)));
            if (ignorePos >= 0) {
                break;
            }
        }
        if (ignorePos >= 0) {
            String s = (prefix != null ? prefix : field.getText());
            base = s.substring(0, ignorePos).trim();
            ignoredString = s.substring(ignorePos + 1).trim();
            if (ignoredString.length() == 0) {
                ignoredString = null;
            }
        }
        if (base != null && !matching) {
            String firstInList = list.getFirst(base.trim());
            if (firstInList != null && field.getText().startsWith(firstInList)) {
                matching = true;
            }
        }

        if (prefix != null && !matching && prefix.endsWith(" ")) {
            String firstInList = list.getFirst(prefix.trim());
            if (firstInList != null && field.getText().startsWith(firstInList)) {
                matching = true;
            }
        }

        boolean valid = false;
        if (matching && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            if (ignoredString == null && complete != null) {
                valid = autoCompleteList.isValidAt(complete, t);
            }
            if (ignoredString != null && base != null) {
                valid = autoCompleteList.isValidAt(base, t);
            }
            if (!valid) {
                matching = false;
            }
        } else {
            valid = true;
        }

        if (matching) {
            setButtonColor((ignoredString == null ? Color.green : Color.yellow));
        } else {
            setButtonColor((valid ? Color.red : Color.orange));
        }

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_AUTOCOMPLETE,
                    "field=" + field.getText()
                    + ", complete=" + complete
                    + ", matching=" + matching
                    + ", valid=" + valid
                    + ", validAtDateItem=" + validAtDateItem
                    + ", ignoredString=" + ignoredString);
        }

        if (mode == Mode.enter) {
            field.select(-1, -1);
            field.setCaretPosition(field.getText().length());
            closePopup();
        }

        if (mode == Mode.escape) {
            closePopup();
        }

        if (field.getText().length() == 0) {
            setButtonColor(null);
        }
    }

    private void checkSpelling() {
        String name = getValueFromField().trim();
        if (name.length() == 0) {
            return;
        }

        int ignorePos = -1;
        for (int i = 0; ignoreEverythingAfter != null && i < ignoreEverythingAfter.length(); i++) {
            ignorePos = name.indexOf(ignoreEverythingAfter.charAt(i));
            if (ignorePos >= 0) {
                break;
            }
        }
        if (ignorePos >= 0) {
            name = name.substring(0, ignorePos).trim();
        }

        AutoCompleteList list = getAutoCompleteList();
        if (list == null) {
            return;
        }

        Vector<String> neighbours = null;
        if (list.getExact(name.toLowerCase()) == null) {
            int radius = (name.length() < 6 ? name.length() / 2 : 3);
            neighbours = list.getNeighbours(name, radius, (isCheckPermutations ? 6 : 0));
        }

        if (Daten.efaConfig.getValuePopupContainsMode()) {
            if (list.getExact(name) == null) {
                if (neighbours == null) {
                    neighbours = new Vector<String>();
                }
                list.setFilterText(name);
                addButAvoidDuplicates(neighbours, list.getDataVisibleFiltered());
                neighbours.remove(name);
                Collections.sort(neighbours, new EfaSortStringComparator());
                list.setFilterText(null);
            }
        }

        if (neighbours != null && neighbours.size() > 0) {
            ItemTypeList item = new ItemTypeList("NAME", IItemType.TYPE_PUBLIC, "",
                    LogString.itemIsUnknown(name, International.getString("Name")) + "\n" +
                    International.getString("Meintest Du ...?"));
            for (int i = 0; i < neighbours.size(); i++) {
                item.addItem(neighbours.get(i), neighbours.get(i), null, null, neighbours.get(i), false, '\0');
            }
            item.setFieldSize(350, 200);
            item.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
            item.setGridWeightX(1.0);
            item.setGridWeightY(1.0);

            if (field == null || !field.isValid()) {
                return;
            }
            if (SimpleOptionInputDialog.showOptionInputDialog(dlg,
                    International.getString("Tippfehler?"), item,
                    new String[] { International.getString("Ersetzen"),
                                   International.getString("Abbruch") },
                    new int[] { SimpleOptionInputDialog.OPTION_OK,
                                SimpleOptionInputDialog.OPTION_CANCEL },
                                null)) {
                String suggestedName = item.getSelectedText();
                if (suggestedName != null && suggestedName.length() > 0) {
                    this.parseAndShowValue(suggestedName);
                }
            }
        }
    }

    private void addButAvoidDuplicates(Vector<String> target, Vector<String> source) {
        String value;
        for (int i = 0; i < source.size(); i++) {
            value = source.get(i);
            if (!target.contains(value)) {
                target.add(value);
            }
        }
    }

    private void handleFilteredList(KeyEvent e) {
        if (field == null) {
            return;
        }

        JTextField textField = (JTextField) this.field;
        AutoCompleteList list = getAutoCompleteList();

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, this.getName());
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "KeyEvent :" + (e == null ? "null" : e.toString()));
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "AutoCompleteList.size() = " + (list == null ? "null" : list.getSizes()));
        }

        if (list == null) {
            setButtonColor(null);
            closePopup();
            return;
        } else {
            list.update();
            list.setFilterText(textField.getText().trim());
        }

        if (e != null && e.getKeyCode() == -23) {
            return;
        }

        if (textField.getText().trim().length() == 0) {
            setButtonColor(null);
        }

        Mode mode = Mode.none;
        if (e == null || ((EfaUtil.isRealChar(e) && (e.getKeyCode() != KeyEvent.VK_ENTER) && (e.getKeyCode() != KeyEvent.VK_TAB))
                || e.getKeyCode() == KeyEvent.VK_DOWN)
                || (e.getKeyCode() == KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))) {
            mode = Mode.normal;
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            mode = Mode.up;
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            mode = Mode.delete;
        } else if ((e.getKeyCode() == KeyEvent.VK_ENTER) || e.getKeyCode() == KeyEvent.VK_TAB) {
            mode = Mode.enter;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mode = Mode.escape;
        }

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "Mode " + mode);
        }

        if (e == null || mode == Mode.enter || mode == Mode.escape) {
            textField.setText(textField.getText().trim());
        }

        boolean matching = false;

        String searchFor = textField.getText().trim().toLowerCase();
        String complete = "";

        if (mode == Mode.normal || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none))) {

            if (e != null && ((e.getKeyCode() == KeyEvent.VK_DOWN)
                    || (e.getKeyCode() == KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)))) {
                complete = list.getNext(searchFor);
                if (complete == null) {
                    complete = list.getFirst(searchFor);
                }

            } else if (e != null && e.getKeyCode() == KeyEvent.VK_UP) {
                complete = list.getPrev(searchFor);
                if (complete == null) {
                    complete = list.getLast(searchFor);
                }
            } else {
                if ((e != null) && e.getKeyCode() != KeyEvent.VK_DOWN) {
                    if (Daten.efaConfig.getValuePopupContainsModeSelectPrefixItem()) {
                        complete = list.getFirstByPrefix(textField.getText());
                        if (complete == null) {
                            complete = list.getFirst(textField.getText());
                        }
                    } else {
                        complete = list.getFirst(textField.getText());
                    }
                }
            }

            if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "User entered text=" + searchFor + " matching item=" + complete);
            }

            if (e != null && (mode != Mode.normal && (
                    (e.getKeyCode() == KeyEvent.VK_ENTER) ||
                    ((e.getKeyCode() == KeyEvent.VK_TAB) && !searchFor.isEmpty())
                    ) &&
            		isPopupVisible())) {
                complete = popupList.getSelectedValue();
                if (complete != null && !complete.isEmpty()) {
                    textField.setText(complete);
                }
                matching = true;
            }

            if (withPopup && useAutocompleteList && e != null && mode != Mode.none && e.getKeyCode() != KeyEvent.VK_TAB) {
                showPopupForField(textField);
                updatePopupSelection(complete != null ? complete : "", searchFor);
            }
        }

        if (mode == Mode.up) {
            if (isPopupVisible() && useAutocompleteList) {
                complete = list.getPrev(searchFor);
            } else {
                complete = list.getPrev(searchFor);
            }

            if (complete == null) {
                complete = list.getLast(searchFor);
            }
            if (withPopup && useAutocompleteList) {
                showPopupForField(textField);
                updatePopupSelection(complete != null ? complete : "", searchFor);
            }
        }

        if (mode == Mode.delete) {
            if (withPopup && useAutocompleteList && e != null && mode != Mode.none) {
                if (Daten.efaConfig.getValuePopupContainsModeSelectPrefixItem()) {
                    complete = list.getFirstByPrefix(textField.getText());
                    if (complete == null) {
                        complete = list.getFirst(textField.getText());
                    }
                } else {
                    complete = list.getFirst(textField.getText());
                }
                showPopupForField(textField);
                updatePopupSelection(complete != null ? complete : textField.getText(), searchFor);
            }
        }

        matching = (list.getExact(textField.getText()) != null);

        boolean valid = false;
        if (matching && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            valid = autoCompleteList.isValidAt(textField.getText(), t);
            if (!valid) {
                matching = false;
            }
        } else {
            valid = true;
        }

        if (matching) {
            setButtonColor(Color.green);
        } else {
            setButtonColor((valid ? Color.red : Color.orange));
        }

        if (mode == Mode.enter) {
            textField.select(-1, -1);
            textField.setCaretPosition(textField.getText().length());
            closePopup();
        }

        if (mode == Mode.escape) {
            closePopup();
        }

        if (textField.getText().length() == 0) {
            setButtonColor(null);
        }
    }

    public boolean isCurrentTextMatching() {
        AutoCompleteList list = getAutoCompleteList();
        JTextField textField = (JTextField) this.field;
        if (list != null && textField != null) {
            return list.getExact(textField.getText()) != null;
        }
        return false;
    }

    public boolean isCurrentTextValid() {
        boolean valid = false;
        JTextField textField = (JTextField) this.field;

        if (textField != null && isCurrentTextMatching() && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            valid = autoCompleteList.isValidAt(textField.getText(), t);
        } else {
            valid = false;
        }

        return valid;
    }

    private AutoCompleteList getAutoCompleteList() {
        return autoCompleteList;
    }

    private void setButtonColor(Color color) {
        valueIsKnown = (color == Color.green || color == Color.yellow);
        if (button != null) {
            if (color != null) {
                EfaUtil.handleButtonOpaqueForLookAndFeels(button);
                button.setBackground(color);
            } else {
                button.setBackground(originalButtonColor);
            }
        }
    }

    public boolean isKnown() {
        return valueIsKnown;
    }

    public void setAlternateFieldNameForPlainText(String fieldName) {
        alternateFieldNameForPlainText = fieldName;
    }

    public String getAlternateFieldNameForPlainText() {
        return alternateFieldNameForPlainText;
    }

    public void setAlwaysReturnPlainText(boolean alwaysReturnPlainText) {
        this.alwaysReturnPlainText = alwaysReturnPlainText;
    }

    public boolean getAlwaysReturnPlainText() {
        return alwaysReturnPlainText;
    }

    public void requestButtonFocus() {
        if (button != null) {
            button.requestFocus();
        }
    }

    public JButton getButton() {
        return button;
    }

    public boolean isValidInput() {
        if (!alwaysReturnPlainText && alternateFieldNameForPlainText == null && value != null && value.length() > 0) {
            if (autoCompleteList.getId(value) == null) {
                lastInvalidErrorText = International.getString("Unbekannter Name nicht erlaubt");
                return false;
            }
        }
        return super.isValidInput();
    }

    public void setShowButtonFocusable(boolean value) {
        this.showButtonFocusable = value;
        if (button != null) {
            button.setFocusable(value);
        }
    }

    public boolean getShowButton() {
        return this.showButton;
    }

    public ItemTypeStringAutoComplete getOtherField() {
        return this.otherField;
    }

    public void setOtherField(ItemTypeStringAutoComplete other) {
        this.otherField = other;
    }

    public void removeFromVisible(String value) {
        if (onChoosenDeleteFromList && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if (vis.remove(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
    }

    // ============================================================
    // Popup handling
    // ============================================================

    private enum Mode {
        none,
        normal,
        up,
        delete,
        enter,
        escape
    }

    private void ensurePopupCreated() {
        if (popup != null) {
            return;
        }

        popupList = new JList<String>();
        popupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        popupList.setCellRenderer(new PaintHighlightRenderer());
        popupList.setVisibleRowCount(6);
        popupList.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        popupScrollPane = new JScrollPane(popupList);
        popupScrollPane.setBorder(//UIManager.getBorder("PopupMenu.border"));
        		BorderFactory.createEmptyBorder());
        popupScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        popup = new JPopupMenu();
        popup.setFocusable(false);
        popup.add(popupScrollPane);
        //popup.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        /*
        popupList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    acceptPopupSelection();
                }
            }
        });*/
        popupList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Index unter dem Mauszeiger selektieren und übernehmen
                    int idx = popupList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        popupList.setSelectedIndex(idx);
                        acceptPopupSelection();
                    }
                }
            }
        });

    }

    private void showPopupForField(JTextField textField) {
        if (!useAutocompleteList || autoCompleteList == null || textField == null 
        		|| !textField.isEnabled() || !textField.isEditable()) {
            closePopup();
            return;
        }

        ensurePopupCreated();

        autoCompleteList.update();

        String filterText = textField.getText() == null ? "" : textField.getText().trim();
        if (Daten.efaConfig != null && Daten.efaConfig.getValuePopupContainsMode()) {
            autoCompleteList.setFilterText(filterText);
        } else {
            autoCompleteList.setFilterText(null);
        }

        Vector<String> data = autoCompleteList.getDataVisibleFiltered();
        popupList.setListData(data);

        if (data == null || data.isEmpty()) {
            closePopup();
            return;
        }

        int width = Math.max(textField.getWidth(), 240);
        int rowCount = Math.min(Math.max(data.size(), 1), 6);
        int rowHeight = Math.max(
                18,
                popupList.getFixedCellHeight() > 0
                        ? popupList.getFixedCellHeight()
                        : popupList.getFontMetrics(popupList.getFont()).getHeight() + 6
        );
        // Ensure the Dropdown height is at least 150 pix, so that the user recognizes
        // for search terms with no result, that the result is empty.
        // also, the screen does not flicker so much when the resulting popup gets smaller
        // after consecutively adding new characters to the search term as the result set gets smaller
        int height = Math.max(150, rowCount * rowHeight + 8);

        popupScrollPane.setPreferredSize(new Dimension(width, height));

        if (!isPopupVisible()) {
            try {
                popup.show(textField, 0, textField.getHeight());
            } catch (Exception ex) {
                Logger.logdebug(ex);
                return;
            }
        } else {
            popup.setPopupSize(width, height);
        }

        updatePopupSelection(textField.getText(), filterText);
    }

    /* 
	 * Updates the selection in the popup list based on the given value to select and filter text.
	 * If the value to select is found in the data, it will be selected. Otherwise, it will try to find
	 * a match based on the filter text, either by contains or starts with, depending on the configuration.
	 * If no match is found, the first item in the list will be selected.
	 */
    private void updatePopupSelection(String valueToSelect, String filterText) {
        if (popupList == null) {
            return;
        }

        Vector<String> data = autoCompleteList != null ? autoCompleteList.getDataVisibleFiltered() : null;
        if (data == null || data.isEmpty()) {
            popupList.clearSelection();
            return;
        }

        int idx = -1;

        if (valueToSelect != null && valueToSelect.length() > 0) {
            idx = data.indexOf(valueToSelect);
        }

        boolean isContainsMode = Daten.efaConfig.getValuePopupContainsMode();
        if (idx < 0 && filterText != null && filterText.length() > 0) {
            String lowerFilter = filterText.toLowerCase();
            for (int i = 0; i < data.size(); i++) {
                String s = data.get(i);
                if (isContainsMode) {
                	//search for the first item which contains the element
	                if (s != null && s.toLowerCase().contains(lowerFilter)) {
	                    idx = i;
	                    break;
	                }
                } else {
                	//search for the first item which starts with element
                    if (s != null && s.toLowerCase().startsWith(lowerFilter)) {
	                    idx = i;
	                    break;
	                }                	
                }
            }
        }

        if (idx < 0) {
            idx = 0;
        }

        popupList.setSelectedIndex(idx);
        popupList.ensureIndexIsVisible(idx);
        popupList.repaint();
    }

    /*
	 * Handles navigation keys (up, down, page up, page down, escape) for the popup list.
	 * Returns true if the key event was handled and consumed, false otherwise.
	 */
    private boolean handlePopupNavigationKey(KeyEvent e) {
        if (!isPopupVisible() || popupList == null || e == null) {
            return false;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                closePopup();
                return true;
                
            // we don't want to handle VK_ENTER or VK_TAB here as it is handled in the calling method.

            case KeyEvent.VK_DOWN:
                movePopupSelection(1, false);
                return true;

            case KeyEvent.VK_UP:
                movePopupSelection(-1, false);
                return true;

            case KeyEvent.VK_PAGE_DOWN:
                movePopupSelection(Math.max(1, popupList.getVisibleRowCount()), false);
                return true;

            case KeyEvent.VK_PAGE_UP:
                movePopupSelection(-Math.max(1, popupList.getVisibleRowCount()), false);
                return true;

            default:
                return false;
        }
    }

    /* 
     * Moves the selection in the popup list by the specified delta. 
     * If wrap is true, the selection will wrap around when reaching the beginning or end of the list.
     * Also, it mimics the standard selection behavior of JList (move the selection bar and only scroll the
     * visible items when necessary).
     */
    private void movePopupSelection(int delta, boolean wrap) {
        if (popupList == null) {
            return;
        }

        int size = popupList.getModel().getSize();
        if (size <= 0) {
            return;
        }

        int current = popupList.getSelectedIndex();
        int idx;

        if (current < 0) {
            idx = (delta >= 0 ? 0 : size - 1);
        } else {
            idx = current + delta;
        }

        if (wrap) {
            while (idx < 0) {
                idx += size;
            }
            while (idx >= size) {
                idx -= size;
            }
        } else {
            if (idx < 0) {
                idx = 0;
            } else if (idx >= size) {
                idx = size - 1;
            }
        }

        if (idx == current) {
            return;
        }

        popupList.setSelectedIndex(idx);
        ensureSelectionVisibleNaturally(idx);
        popupList.repaint();
    }


    /* 
     * Only scroll the list if the selected item is not fully visible, mimicking the standard JList behavior. 
     */
    private void ensureSelectionVisibleNaturally(int idx) {
        if (popupList == null) {
            return;
        }

        Rectangle cellBounds = popupList.getCellBounds(idx, idx);
        if (cellBounds == null) {
            return;
        }

        Rectangle visible = popupList.getVisibleRect();
        if (visible == null) {
            return;
        }

        if (cellBounds.y < visible.y) {
            popupList.scrollRectToVisible(cellBounds);
        } else if (cellBounds.y + cellBounds.height > visible.y + visible.height) {
            popupList.scrollRectToVisible(cellBounds);
        }
    }

    /* 
	 * Accepts the currently selected value in the popup list and applies it to the text field.
	 * If no value is selected, it does nothing. After applying the value, it closes the popup.
	 */
    private void acceptPopupSelection() {
        if (!isPopupVisible() || popupList == null) {
            return;
        }

        String selected = popupList.getSelectedValue();
        if (selected != null) {
            applyPopupValue(selected);
        }
        closePopup();
    }
    
    /* 
     * Puts the currently selected value from the popup list into the text field and updates the internal state.
     */
    private void applyPopupValue(String selected) {
        if (field == null || selected == null) {
            return;
        }
        JTextField tf = (JTextField) field;

        tf.setText(selected);
        tf.setCaretPosition(selected.length());
        tf.select(-1, -1);

        // Update internal state immediately.
        autoCompleteList.setFilterText(null);
        autoComplete(null);
    }

    /*
     * Closes the popup if it is currently visible and resets the filter text in the autoCompleteList.
     */
    private void closePopup() {
        if (popup != null && isPopupVisible()) {
            popup.setVisible(false);
        }

        if (autoCompleteList != null) {
            autoCompleteList.setFilterText(null);
        }
    }

    private boolean isPopupVisible() {
    	return (popup!=null && popup.isVisible());
    }
    
    /*
     * Callback method for the auto-complete functionality. 
     * This method is called when the text in the associated JTextField changes, and it triggers the auto-complete logic.
     * It is also called from outside (efaBaseFrame, efaBaseFrameMultisession) 
     * to ensure that the value of the text field is checked against the auto-complete list and updated accordingly.
     */
    public void acpwCallback(JTextField field) {
        autoComplete(null);
    }

    /* 
     * A custom ListCellRenderer that highlights the matching part of the text in the popup list based on the current search term.
     * The highlighting is only performed if the "contains mode" is active, as determined by the configuration.
     * If the old search mode is active (search by prefix), the matching part is not highlighted, and the text is displayed normally.
     */
    private class PaintHighlightRenderer extends JComponent implements ListCellRenderer<String> {

        private static final long TEN_SECONDS_IN_MILLIS = 1000*10;
		private String fullText;
        private String search;
        private boolean selectedFlag;
        
        // Cached colors (sollten sich nicht staendig aendern)
        private Color selectionBackground=UIManager.getColor("List.selectionBackground");
        private Color selectionForeground=UIManager.getColor("List.selectionForeground");
        private Color listBackground;
        private Color listForeground;
        private Color highlightColor = new Color(255, 255, 180);
        private Color matchForeground = Color.BLACK;
        private boolean isContainsMode = true;
        private long lastContainsModeCheck=0;

        public PaintHighlightRenderer() {
            setFont(UIManager.getFont("Label.font"));
            setOpaque(true);
            updateCachedColors();
        }
        
        private void updateCachedColors() { 
            listBackground = UIManager.getColor("List.background");
            listForeground = UIManager.getColor("List.foreground");
            // for some LookAndFeels, the selection colors might be null, so we provide a fallback to the default JList colors.
            selectionBackground = (selectionBackground != null ? selectionBackground : new JList().getSelectionBackground());
            selectionForeground = (selectionForeground != null ? selectionForeground : new JList().getSelectionForeground());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            this.fullText = value;
            this.search = (field instanceof JTextField) ? ((JTextField) field).getText().trim().toLowerCase() : "";
            this.selectedFlag = isSelected;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            String t = (fullText == null ? "" : fullText);
            return new Dimension(fm.stringWidth(t) + 8, fm.getHeight() + 4);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (fullText == null) return;
            Graphics2D g2 = (Graphics2D) g;
            FontMetrics fm = g2.getFontMetrics();
            
            EfaGuiUtils.setStandardRenderingHints(g2);
            int x = 8;
            int y = fm.getAscent() + 2;

            // Draw background
            if (selectedFlag) {
                g2.setColor(selectionBackground);
                int selectionBarPadding = 3;
                g2.fillRoundRect(selectionBarPadding, 1, getWidth() - (selectionBarPadding * 2), getHeight() - 1, 12, 12);
            } else {
                g2.setColor(listBackground);
                g2.fillRect(x-2, y, getWidth(), getHeight());
            }
            
            g2.setColor(selectedFlag ? selectionForeground : listForeground);

            String lower = fullText.toLowerCase();
            int pos = (search != null && !search.isEmpty()) ? lower.indexOf(search) : -1;

            if (pos >= 0 && !search.isEmpty()) {
                String before = fullText.substring(0, pos);
                String match = fullText.substring(pos, pos + search.length());
                String after = fullText.substring(pos + search.length());

                // Draw "before" text
                g2.setColor(selectedFlag ? selectionForeground  : listForeground);
                g2.drawString(before, x, y);
                x += fm.stringWidth(before);

                int w = fm.stringWidth(match);
                if (isContainsMode()) {
	                // Draw highlight background for match only if containsmode is active.
	                g2.setColor(highlightColor);
	                g2.fillRect(x+1, y - fm.getAscent() + 2, w, fm.getHeight() - 3);
	                // Draw match text (in black for contrast)
	                g2.setColor(matchForeground);
                }
                g2.drawString(match, x, y);
                x += w;

                // Draw "after" text
                g2.setColor(selectedFlag ? selectionForeground  : listForeground);
                g2.drawString(after, x, y);
            } else {
                g2.drawString(fullText, x, y);
            }
        }
        
        private boolean isContainsMode() {        	
        	if (System.currentTimeMillis() >= this.lastContainsModeCheck+ (TEN_SECONDS_IN_MILLIS)) {
        		this.isContainsMode= Daten.efaConfig.getValuePopupContainsMode();
        		lastContainsModeCheck=System.currentTimeMillis();
        	}
        	return this.isContainsMode;
        }
    }
}
