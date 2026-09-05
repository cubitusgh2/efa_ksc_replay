/**
* Title:        efa - elektronisches Fahrtenbuch fuer Ruderer
* Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
* Website:      http://efa.nmichael.de/
* License:      GNU General Public License v2
*
* @author Nicolas Michael
* @version 2
*/

package de.nmichael.efa.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeHashtable;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.International;

// @i18n complete
public class EfaConfigDialog extends BaseTabbedDialog {

    private static final String CARD_EMPTY = "__empty__";
    private static final String GROUP_CARD_PREFIX = "__group__::";
    private static final int NAV_INDENT_PER_LEVEL = 10;
    private static final String BREADCRUMB_SEPARATOR = " \u203A ";
    private static final String ACTION_NAV_ACTIVATE = "nav.activate";
    private static final String ACTION_NAV_FOCUS_FILTER = "nav.focusFilter";
    private static final String ACTION_FILTER_ARROW_UP = "filter.arrowUp";
    private static final String ACTION_FILTER_ARROW_DOWN = "filter.arrowDown";
    private static final String HIGHLIGHT_STYLE = "background-color:#fff176; font:bold; color:#000000;";
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final int FILTER_DELAY_MS = 500;
    private static final int MIN_FILTER_LENGTH = 2;
    private static final int NAVIGATIONLIST_WIDTH = 210;

    private EfaConfig myEfaConfig;

    private JList<NavEntry> navigationList;
    private DefaultListModel<NavEntry> navigationModel;
    private List<NavEntry> allNavigationEntries;
    private JTextField navigationFilterField;
    private Timer navigationFilterTimer;

    private JPanel cardPanel;
    private CardLayout cardLayout;
    private String lastSelectedCardKey;
    private RoundedLabel breadcrumbLabel;

    // Persisted UI state across rebuilds
    private String persistedFilterText = "";
    private String persistedSelectedCardKey = null;
    private String persistedSelectedPanelKey = null;

    private final List<JPanel> leafCardPanels = new ArrayList<JPanel>();
    private final Map<JLabel, String> originalLabelTexts = new HashMap<JLabel, String>();
    private final Map<AbstractButton, String> originalButtonTexts = new HashMap<AbstractButton, String>();

    public EfaConfigDialog(Frame parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig, String selectedPanel) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this._selectedPanel = selectedPanel;
        this.myEfaConfig = efaConfig;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        super.iniDialog();
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        getValuesFromGui();
        synchronized (myEfaConfig) {
            for (int i = 0; i < allGuiItems.size(); i++) {
                IItemType item = allGuiItems.get(i);
                if (item.isChanged()) {
                    myEfaConfig.setValue(item.getName(), item.toString());
                }
            }
        }
        myEfaConfig.checkNewConfigValues();
        myEfaConfig.setExternalParameters(true);
        myEfaConfig.checkForRequiredPlugins();
        super.closeButton_actionPerformed(e);
        setDialogResult(true);
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesBoat() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesBoat().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesNumSeats() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesNumSeats().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesRigging() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesRigging().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesCoxing() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesCoxing().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesGender() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesGender().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesSession() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesSession().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesStatus() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesStatus().getName());
    }

    protected int recursiveBuildGui(Hashtable<String, Hashtable> categories,
                                    Hashtable<String, Vector<IItemType>> items,
                                    String catKey,
                                    JComponent currentPane,
                                    String selectedPanel,
                                    int otherPanelHeight) {

        navigationModel = new DefaultListModel<NavEntry>();
        allNavigationEntries = new ArrayList<NavEntry>();

        leafCardPanels.clear();
        originalLabelTexts.clear();
        originalButtonTexts.clear();

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(createEmptyCard(), CARD_EMPTY);

        int itmcnt = collectNavAndCards(categories, items, catKey, otherPanelHeight, 0);

        navigationList = new JList<NavEntry>(navigationModel);
        navigationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navigationList.setCellRenderer(new NavEntryRenderer());

        navigationList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            NavEntry entry = navigationList.getSelectedValue();
            if (entry != null && entry.selectable) {
                persistedSelectedPanelKey = entry.key;
                showCard(entry.cardKey, entry.key);
            } else {
                showCard(CARD_EMPTY, null);
            }
        });

        navigationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = navigationList.locationToIndex(e.getPoint());
                if (idx < 0) {
                    return;
                }
                navigationList.setSelectedIndex(idx);
                activateNavigationEntryAt(idx);
            }
        });

        navigationList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), ACTION_NAV_ACTIVATE);
        navigationList.getActionMap().put(ACTION_NAV_ACTIVATE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateNavigationEntryAt(navigationList.getSelectedIndex());
            }
        });

        navigationFilterField = new JTextField();
        navigationFilterField.setToolTipText(International.getString("STRG+F für Suche"));

        navigationFilterTimer = new Timer(FILTER_DELAY_MS, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyNavigationFilterDeferred();
            }
        });
        navigationFilterTimer.setRepeats(false);

        navigationFilterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                scheduleNavigationFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                scheduleNavigationFilter();
            }

            public void changedUpdate(DocumentEvent e) {
                scheduleNavigationFilter();
            }
        });

        navigationFilterField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ACTION_FILTER_ARROW_DOWN);
        navigationFilterField.getActionMap().put(ACTION_FILTER_ARROW_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveFocusFromFilterToList(true);
            }
        });

        navigationFilterField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ACTION_FILTER_ARROW_UP);
        navigationFilterField.getActionMap().put(ACTION_FILTER_ARROW_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveFocusFromFilterToList(false);
            }
        });

        JPanel navPanel = new JPanel(new BorderLayout(0, 4));
        navPanel.add(navigationFilterField, BorderLayout.NORTH);
        JScrollPane myScroller = new JScrollPane(navigationList);
        myScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navPanel.add(myScroller, BorderLayout.CENTER);
        navPanel.setPreferredSize(new Dimension(NAVIGATIONLIST_WIDTH, 10));

        breadcrumbLabel = new RoundedLabel();
        breadcrumbLabel.setBorder(new RoundedBorder(Daten.efaConfig.getHeaderForegroundColor()));
        breadcrumbLabel.setFont(breadcrumbLabel.getFont().deriveFont(Font.BOLD));
        breadcrumbLabel.setToolTipText(null);
        breadcrumbLabel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
        breadcrumbLabel.setBackground(Daten.efaConfig.getHeaderBackgroundColor().darker());
        breadcrumbLabel.setOpaque(true);

        JScrollPane cardScrollWrapper = new JScrollPane(cardPanel);
        cardScrollWrapper.getVerticalScrollBar().setUnitIncrement(12);
        cardScrollWrapper.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(breadcrumbLabel, BorderLayout.NORTH);
        rightPanel.add(cardScrollWrapper, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(8, 0));
        contentPanel.add(navPanel, BorderLayout.WEST);
        contentPanel.add(rightPanel, BorderLayout.CENTER);

        currentPane.setLayout(new BorderLayout(8, 0));
        currentPane.add(contentPanel, BorderLayout.CENTER);

        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        currentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(findKey, ACTION_NAV_FOCUS_FILTER);
        currentPane.getActionMap().put(ACTION_NAV_FOCUS_FILTER, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focusFilterField();
            }
        });

        // Restore filter text before applying filter model
        if (persistedFilterText != null && persistedFilterText.length() > 0) {
            navigationFilterField.setText(persistedFilterText);
        }

        applyNavigationFilterWithMinLength();

        // Try to restore the exact panel/card after filter is active.
        if (!restoreSelectionAfterRebuild()) {
            selectInitialEntry(selectedPanel);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                focusFilterField();
            }
        });

        return itmcnt;
    }

    private JPanel createEmptyCard() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(International.getString("STRG+F für Suche"), SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.ITALIC));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void moveFocusFromFilterToList(boolean toNext) {
        if (navigationList == null || navigationModel == null || navigationModel.getSize() == 0) {
            return;
        }

        int idx = navigationList.getSelectedIndex();
        if (idx < 0) {
            idx = toNext ? 0 : (navigationModel.getSize() - 1);
        } else if (toNext && idx < navigationModel.getSize() - 1) {
            idx++;
        } else if (!toNext && idx > 0) {
            idx--;
        }

        navigationList.setSelectedIndex(idx);
        if (navigationList.getFirstVisibleIndex()>idx || navigationList.getLastVisibleIndex()<idx) {
        	navigationList.ensureIndexIsVisible(idx);
        }
        navigationList.requestFocusInWindow();
    }

    private void focusFilterField() {
        if (navigationFilterField != null) {
            navigationFilterField.requestFocusInWindow();
            navigationFilterField.selectAll();
        }
    }

    private void persistCurrentFilterText() {
        persistedFilterText = navigationFilterField != null ? navigationFilterField.getText() : "";
    }

    private void scheduleNavigationFilter() {
        persistCurrentFilterText();

        if (navigationFilterTimer != null) {
            navigationFilterTimer.restart();
        }
    }

    private void applyNavigationFilterDeferred() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    applyNavigationFilterDeferred();
                }
            });
            return;
        }

        CursorState cursorState = setWaitCursor(true);
        try {
            applyNavigationFilterWithMinLength();
        } finally {
            restoreCursor(cursorState);
        }
    }

    private void applyNavigationFilterWithMinLength() {
        String raw = navigationFilterField != null ? navigationFilterField.getText() : "";
        String trimmed = raw == null ? "" : raw.trim();
        String filter = normalizeSearchText(trimmed);

        if (filter.length() > 0 && filter.length() < MIN_FILTER_LENGTH) {
            filter = "";
        }

        applyNavigationFilter(filter);
    }

    private CursorState setWaitCursor(boolean wait) {
        Cursor cursor = wait
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor();

        CursorState state = new CursorState();
        state.dialog = this;
        state.dialogCursor = this.getCursor();

        if (navigationFilterField != null) {
            state.filterField = navigationFilterField;
            state.filterFieldCursor = navigationFilterField.getCursor();
        }

        if (navigationList != null) {
            state.navigationList = navigationList;
            state.navigationListCursor = navigationList.getCursor();
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            state.window = window;
            state.windowCursor = window.getCursor();
            window.setCursor(cursor);
        }

        this.setCursor(cursor);

        if (navigationFilterField != null) {
            navigationFilterField.setCursor(cursor);
        }
        if (navigationList != null) {
            navigationList.setCursor(cursor);
        }

        return state;
    }

    private void restoreCursor(CursorState state) {
        if (state == null) {
            return;
        }

        if (state.window != null) {
            state.window.setCursor(state.windowCursor != null
                    ? state.windowCursor
                    : Cursor.getDefaultCursor());
        }

        if (state.dialog != null) {
            state.dialog.setCursor(state.dialogCursor != null
                    ? state.dialogCursor
                    : Cursor.getDefaultCursor());
        }

        if (state.filterField != null) {
            state.filterField.setCursor(state.filterFieldCursor != null
                    ? state.filterFieldCursor
                    : Cursor.getDefaultCursor());
        }

        if (state.navigationList != null) {
            state.navigationList.setCursor(state.navigationListCursor != null
                    ? state.navigationListCursor
                    : Cursor.getDefaultCursor());
        }
    }

    private boolean restoreSelectionAfterRebuild() {
        if (navigationModel == null || navigationModel.getSize() == 0) {
            return false;
        }

        String desiredCard = persistedSelectedCardKey != null ? persistedSelectedCardKey : lastSelectedCardKey;
        String desiredPanel = persistedSelectedPanelKey;

        // 1) Try exact panel key first
        if (desiredPanel != null && desiredPanel.length() > 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && desiredPanel.equals(e.key)) {
                    navigationList.setSelectedIndex(i);
                    if (navigationList.getFirstVisibleIndex()>i || navigationList.getLastVisibleIndex()<i) {
                    	navigationList.ensureIndexIsVisible(i);
                    }
                    showCard(e.cardKey, e.key);
                    return true;
                }
            }
        }

        // 2) Fallback: match by card key
        if (desiredCard != null && desiredCard.length() > 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && desiredCard.equals(e.cardKey)) {
                    navigationList.setSelectedIndex(i);
                    if (navigationList.getFirstVisibleIndex()>i || navigationList.getLastVisibleIndex()<i) {
                    	navigationList.ensureIndexIsVisible(i);
                    }
                    showCard(e.cardKey, e.key);
                    return true;
                }
            }
        }

        return false;
    }

    public void rebuildConfigGui() {
        displayedGuiItems.clear();

        // Capture current state before components are disposed/rebuilt.
        persistCurrentFilterText();

        if (navigationFilterTimer != null && navigationFilterTimer.isRunning()) {
            navigationFilterTimer.stop();
        }

        persistedSelectedCardKey = lastSelectedCardKey;
        persistedSelectedPanelKey = null;
        if (navigationList != null) {
            NavEntry selected = navigationList.getSelectedValue();
            if (selected != null) {
                persistedSelectedPanelKey = selected.key;
                if (selected.cardKey != null) {
                    persistedSelectedCardKey = selected.cardKey;
                }
            }
        }

        removeAll();
        revalidate();
        repaint();
    }

    private int collectNavAndCards(Hashtable<String, Hashtable> categories,
                                   Hashtable<String, Vector<IItemType>> items,
                                   String catKey,
                                   int otherPanelHeight,
                                   int level) {

        int itmcnt = 0;
        Object[] cats = categories.keySet().toArray();
        Arrays.sort(cats);

        for (int i = 0; i < cats.length; i++) {
            String key = (String) cats[i];
            String thisCatKey = (catKey.length() == 0 ? key : makeCategory(catKey, key));
            String catName = getCatName(thisCatKey);
            Hashtable<String, Hashtable> subCat = categories.get(key);

            if (subCat != null && subCat.size() != 0) {
                String parentKey = getParentKey(thisCatKey);
                String groupSearchText = normalizeSearchText(catName);
                String groupCardKey = GROUP_CARD_PREFIX + thisCatKey;

                int groupIndex = allNavigationEntries.size();
                allNavigationEntries.add(new NavEntry(
                        thisCatKey, catName, level, true, false, null, parentKey, groupSearchText));

                int before = itmcnt;
                itmcnt += collectNavAndCards(subCat, items, thisCatKey, otherPanelHeight, level + 1);

                boolean hasSelectableChild = itmcnt > before;
                if (!hasSelectableChild) {
                    cardPanel.add(createGroupPlaceholderPanel(catName), groupCardKey);
                }

                allNavigationEntries.set(groupIndex, new NavEntry(
                        thisCatKey,
                        catName,
                        level,
                        true,
                        !hasSelectableChild,
                        hasSelectableChild ? null : groupCardKey,
                        parentKey,
                        groupSearchText));
            } else {
                JPanel panel = buildLeafPanel(items, thisCatKey);
                if (panel != null) {
                    cardPanel.add(panel, thisCatKey);
                    leafCardPanels.add(panel);

                    String parentKey = getParentKey(thisCatKey);
                    String cardContentSearch = buildSearchText(panel);
                    String leafSearchText = normalizeSearchText(catName + " " + cardContentSearch);

                    allNavigationEntries.add(new NavEntry(
                            thisCatKey, catName, level, false, true, thisCatKey, parentKey, leafSearchText));
                    itmcnt++;
                }
            }
        }

        return itmcnt;
    }

    private void activateNavigationEntryAt(int idx) {
        if (idx < 0 || idx >= navigationList.getModel().getSize()) {
            return;
        }

        NavEntry entry = navigationList.getModel().getElementAt(idx);
        if (entry == null) {
            return;
        }

        if (entry.selectable) {
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
            return;
        }

        if (entry.group) {
            int childIndex = findFirstVisibleSelectableChildIndex(entry.key);
            if (childIndex >= 0 && childIndex != idx) {
                navigationList.setSelectedIndex(childIndex);
                if (navigationList.getFirstVisibleIndex()>childIndex || navigationList.getLastVisibleIndex()<childIndex) {
                	navigationList.ensureIndexIsVisible(childIndex);
                }
            }
        }
    }

    private JPanel buildLeafPanel(Hashtable<String, Vector<IItemType>> items, String thisCatKey) {
        JPanel panel = new JPanel();
        panels.put(panel, thisCatKey);
        JPanel innerPanel = new JPanel();

        JScrollPane scrollPane = new JScrollPane(innerPanel);
        scrollPane.setPreferredSize(EfaGuiUtils.getTabPanelPreferredSizeEfaConfig(this, NAVIGATIONLIST_WIDTH+50, 200));
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        innerPanel.setLayout(new GridBagLayout());
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        Vector<IItemType> v = items.get(thisCatKey);
        int y = 0;
        for (int j = 0; v != null && j < v.size(); j++) {
            IItemType itm = v.get(j);
            if (itm.getType() == IItemType.TYPE_PUBLIC
                    || (itm.getType() == IItemType.TYPE_EXPERT && expertModeEnabled)) {
                y += itm.displayOnGui(this, innerPanel, y);
                displayedGuiItems.add(itm);
            }
        }

        return y > 0 ? panel : null;
    }

    private JPanel createGroupPlaceholderPanel(String groupName) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(groupName, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void applyNavigationFilter(String filter) {
        String effectiveFilter = filter == null ? "" : filter;

        applyHighlightsToCards(effectiveFilter);

        String keepCard = lastSelectedCardKey;
        if (keepCard == null && navigationList != null && navigationList.getSelectedValue() != null) {
            keepCard = navigationList.getSelectedValue().cardKey;
        }

        navigationModel.clear();

        if (effectiveFilter.length() == 0) {
            for (int i = 0; i < allNavigationEntries.size(); i++) {
                navigationModel.addElement(allNavigationEntries.get(i));
            }
        } else {
            HashSet<String> visibleKeys = new HashSet<String>();

            for (int i = 0; i < allNavigationEntries.size(); i++) {
                NavEntry e = allNavigationEntries.get(i);
                if (e.searchText != null && e.searchText.contains(effectiveFilter)) {
                    visibleKeys.add(e.key);
                    String p = e.parentKey;
                    while (p != null) {
                        visibleKeys.add(p);
                        p = getParentKey(p);
                    }
                }
            }

            for (int i = 0; i < allNavigationEntries.size(); i++) {
                NavEntry e = allNavigationEntries.get(i);
                if (visibleKeys.contains(e.key)) {
                    navigationModel.addElement(e);
                }
            }
        }

        int selectedIndex = -1;
        if (keepCard != null) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && keepCard.equals(e.cardKey)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (selectedIndex < 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (selectedIndex >= 0) {
            navigationList.setSelectedIndex(selectedIndex);
            if (navigationList.getFirstVisibleIndex()>selectedIndex || navigationList.getLastVisibleIndex()<selectedIndex) {
            	navigationList.ensureIndexIsVisible(selectedIndex);
            }
            NavEntry entry = navigationModel.get(selectedIndex);
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
        } else {
            showCard(CARD_EMPTY, null);
        }
    }

    private void applyHighlightsToCards(String normalizedFilter) {
    	if (normalizedFilter!=null && normalizedFilter.trim().length()>0) {
	        for (int i = 0; i < leafCardPanels.size(); i++) {
	            applyHighlightsRecursive(leafCardPanels.get(i), normalizedFilter);
	        }
    	}
    }

    private void applyHighlightsRecursive(Component c, String normalizedFilter) {
        if (c == null) {
            return;
        }

        if (c instanceof JLabel) {
            JLabel label = (JLabel) c;
            if (!originalLabelTexts.containsKey(label)) {
                originalLabelTexts.put(label, label.getText());
            }
            String original = originalLabelTexts.get(label);
            label.setText(highlightForDisplay(original, normalizedFilter));
        } else if (c instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) c;
            if (!originalButtonTexts.containsKey(button)) {
                originalButtonTexts.put(button, button.getText());
            }
            String original = originalButtonTexts.get(button);
            button.setText(highlightForDisplay(original, normalizedFilter));
        }

        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                applyHighlightsRecursive(children[i], normalizedFilter);
            }
        }
    }

    private String highlightForDisplay(String originalText, String normalizedFilter) {
        if (originalText == null) {
            return null;
        }

        String filter = normalizedFilter == null ? "" : normalizedFilter.trim();
        if (filter.length() == 0) {
            return originalText;
        }

        String plain = stripHtmlTags(originalText);
        String plainLower = plain.toLowerCase();
        int first = plainLower.indexOf(filter);
        if (first < 0) {
            return originalText;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        int pos = 0;
        while (first >= 0) {
            html.append(escapeHtml(plain.substring(pos, first)));
            html.append("<span style='").append(HIGHLIGHT_STYLE).append("'>");
            html.append(escapeHtml(plain.substring(first, first + filter.length())));
            html.append("</span>");
            pos = first + filter.length();
            first = plainLower.indexOf(filter, pos);
        }
        html.append(escapeHtml(plain.substring(pos)));
        html.append("</html>");
        return html.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private int findFirstVisibleSelectableChildIndex(String parentKey) {
        if (parentKey == null || parentKey.length() == 0 || navigationModel == null) {
            return -1;
        }
        String prefix = parentKey + CATEGORY_SEPARATOR;
        for (int i = 0; i < navigationModel.size(); i++) {
            NavEntry e = navigationModel.get(i);
            if (e.selectable && e.key != null && e.key.startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private void selectInitialEntry(String selectedPanel) {
        int indexToSelect = -1;

        if (selectedPanel != null && selectedPanel.length() > 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && selectedPanel.equals(e.key)) {
                    indexToSelect = i;
                    break;
                }
            }
        }

        if (indexToSelect < 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable) {
                    indexToSelect = i;
                    break;
                }
            }
        }

        if (indexToSelect >= 0) {
            navigationList.setSelectedIndex(indexToSelect);
            if (navigationList.getFirstVisibleIndex()>indexToSelect || navigationList.getLastVisibleIndex()<indexToSelect) {
            	navigationList.ensureIndexIsVisible(indexToSelect);
            }
            NavEntry entry = navigationModel.get(indexToSelect);
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
        } else {
            showCard(CARD_EMPTY, null);
        }
    }

    private void showCard(String cardKey, String fullCategoryKey) {
        if (cardKey == null) {
            cardLayout.show(cardPanel, CARD_EMPTY);
            lastSelectedCardKey = CARD_EMPTY;
            persistedSelectedCardKey = CARD_EMPTY;
            updateBreadcrumb(null);
            return;
        }
        cardLayout.show(cardPanel, cardKey);
        lastSelectedCardKey = cardKey;
        persistedSelectedCardKey = cardKey;
        updateBreadcrumb(fullCategoryKey);
    }

    private void updateBreadcrumb(String fullCategoryKey) {
        if (breadcrumbLabel == null) {
            return;
        }
        if (fullCategoryKey == null || fullCategoryKey.length() == 0) {
            breadcrumbLabel.setText(" ");
            breadcrumbLabel.setToolTipText(null);
            return;
        }

        List<String> parts = splitCategoryKey(fullCategoryKey);
        if (parts.isEmpty()) {
            breadcrumbLabel.setText(" ");
            breadcrumbLabel.setToolTipText(null);
            return;
        }

        StringBuilder sb = new StringBuilder();
        String partialKey = "";
        for (int i = 0; i < parts.size(); i++) {
            partialKey = (i == 0) ? parts.get(i) : makeCategory(partialKey, parts.get(i));
            String name = getCatName(partialKey);
            if (name == null || name.length() == 0) {
                name = parts.get(i);
            }
            if (sb.length() > 0) {
                sb.append(BREADCRUMB_SEPARATOR);
            } else {
                sb.append(" "); // indent the label text by one space
            }
            sb.append(name);
        }

        String breadcrumb = sb.toString();
        breadcrumbLabel.setText(breadcrumb);
        breadcrumbLabel.setToolTipText(breadcrumb);
        breadcrumbLabel.revalidate();
        breadcrumbLabel.repaint();
    }

    private List<String> splitCategoryKey(String fullCategoryKey) {
        ArrayList<String> parts = new ArrayList<String>();
        if (fullCategoryKey == null || fullCategoryKey.length() == 0) {
            return parts;
        }
        if (CATEGORY_SEPARATOR_STRING == null || CATEGORY_SEPARATOR_STRING.length() == 0) {
            parts.add(fullCategoryKey);
            return parts;
        }

        int start = 0;
        int sepLen = CATEGORY_SEPARATOR_STRING.length();
        int pos = fullCategoryKey.indexOf(CATEGORY_SEPARATOR, start);
        while (pos >= 0) {
            parts.add(fullCategoryKey.substring(start, pos));
            start = pos + sepLen;
            pos = fullCategoryKey.indexOf(CATEGORY_SEPARATOR, start);
        }
        parts.add(fullCategoryKey.substring(start));
        return parts;
    }

    private String getParentKey(String fullKey) {
        if (fullKey == null || fullKey.length() == 0) {
            return null;
        }
        int pos = fullKey.lastIndexOf(CATEGORY_SEPARATOR);
        if (pos < 0) {
            return null;
        }
        return fullKey.substring(0, pos);
    }

    private String buildSearchText(Component c) {
        StringBuilder sb = new StringBuilder();
        appendSearchText(c, sb);
        return sb.toString();
    }

    private void appendSearchText(Component c, StringBuilder sb) {
        if (c == null) {
            return;
        }

        if (c instanceof JLabel) {
            String t = ((JLabel) c).getText();
            if (t != null && t.length() > 0) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof AbstractButton) {
            String t = ((AbstractButton) c).getText();
            if (t != null && t.length() > 0) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof JTextComponent) {
            String t = ((JTextComponent) c).getText();
            if (t != null && t.length() > 0) {
                sb.append(' ').append(t);
            }
        }

        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                appendSearchText(children[i], sb);
            }
        }
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        String noTags = stripHtmlTags(text);
        return noTags.toLowerCase();
    }

    private String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = TAG_PATTERN.matcher(text);
        return m.replaceAll(" ");
    }

    private static class CursorState {
        private Window window;
        private Cursor windowCursor;

        private Component dialog;
        private Cursor dialogCursor;

        private Component filterField;
        private Cursor filterFieldCursor;

        private Component navigationList;
        private Cursor navigationListCursor;
    }

    private static class NavEntry {
        private final String key;
        private final String label;
        private final int level;
        private final boolean group;
        private final boolean selectable;
        private final String cardKey;
        private final String parentKey;
        private final String searchText;

        private NavEntry(String key, String label, int level, boolean group, boolean selectable,
                         String cardKey, String parentKey, String searchText) {
            this.key = key;
            this.label = label;
            this.level = level;
            this.group = group;
            this.selectable = selectable;
            this.cardKey = cardKey;
            this.parentKey = parentKey;
            this.searchText = searchText;
        }

        public String toString() {
            return label;
        }
    }

    private static class NavEntryRenderer extends DefaultListCellRenderer {
        private static final Color TOP_GROUP_BG = new Color(230, 230, 230);

        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (!(value instanceof NavEntry)) {
                return this;
            }

            NavEntry entry = (NavEntry) value;

            setText(entry.label);
            setBorder(BorderFactory.createEmptyBorder(4, 8 + (entry.level * NAV_INDENT_PER_LEVEL), 4, 8));

            Font f = getFont();
            if (entry.group) {
                setFont(f.deriveFont(Font.BOLD));
            } else {
                setFont(f.deriveFont(Font.PLAIN));
            }

            if (!isSelected && entry.group && entry.level == 0) {
                setBackground(TOP_GROUP_BG);
            }

            return this;
        }
    }
}
