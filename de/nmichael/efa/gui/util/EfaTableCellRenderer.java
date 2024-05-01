/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.Logger;


/**
 * EfaTableCellRenderer 
 * 
 * This renderer uses a panel consisting of two labels:
 * - textlabel (left-aligned) for the text
 * - iconLabel (right-aligned) for icons.
 * 
 * The iconlabel can show multiple icons, which can be specified with addIcon() method.
 * Icons are displayed in the order they are added.
 *   
 */
public class EfaTableCellRenderer implements TableCellRenderer {

	private boolean markedBold = true;
    private Color markedBkgColor = new Color(0xff,0xff,0xaa);
    private Color alternateColor = new Color(219,234,249);
    private Color markedFgColor = null;
    private Color disabledBkgColor = null;
    private Color disabledFgColor = Color.gray;
    private int fontSize = -1;
    private Icon hiddenIcon = ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_BUTTON_HIDE);
    private boolean useAlternatingColor= false;
    
    private JPanel c;
    private JLabel textLabel;
    private JLabel iconLabel;
    
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        try {

            c = new JPanel();
            c.setLayout(new BorderLayout());
            textLabel = new JLabel();
            textLabel.setHorizontalAlignment(SwingConstants.LEFT);
            c.add(textLabel, BorderLayout.WEST);

            Vector <Icon> iconList = new Vector<Icon>();
            
            TableItem myItem = (value instanceof TableItem ? ((TableItem) value) : null);
            
            boolean isMarked = myItem != null && myItem.isMarked();
            boolean isDisabled = myItem != null && myItem.isDisabled();
            boolean isInvisible = myItem != null && myItem.isInvisible();

            textLabel.setText(myItem!=null ? myItem.toString() : "");
            
            //Update for standard tables: indent cell content for better readability
            c.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
            
            if (isMarked && markedBold) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }

            Color bkgColor = null;
            Color fgColor = Color.black;

            if (this.useAlternatingColor) {
	            bkgColor = (row % 2 == 0 ? alternateColor : null);
            }  
            
            if (isSelected) {
                bkgColor = table.getSelectionBackground();
                // Update for standard tables: when selected, we should always use the selection foreground.
                fgColor= table.getSelectionForeground();
                if (c.getFont().isBold()) {
                	c.setFont(c.getFont().deriveFont(Font.BOLD | Font.ITALIC));
                } else {
                	c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
            } else {
                if (isDisabled && disabledBkgColor != null) {
                    bkgColor = disabledBkgColor;
                }
                if (isMarked && markedBkgColor != null) {
                    bkgColor = markedBkgColor;
                }
            }
            
        	// Set Icon for first column only if item is hidden.
            // So users get an idea why this entry is displayed in disabled color.
            // The tooltip is set in TableItem.java and shown in Table.java

            if (column==0 && (isInvisible)) {
            	iconList.add(hiddenIcon);
            }
            
            if (column==0 && myItem!=null && myItem.getIcons()!=null) {
            	iconList.addAll(myItem.getIcons());
            }
            
            if (iconList.size()>0) {
            	CompoundIcon myIcon=  new CompoundIcon(CompoundIcon.Axis.X_AXIS,2,CompoundIcon.RIGHT, CompoundIcon.CENTER,iconList);
                iconLabel = new JLabel();            
                iconLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            	iconLabel.setIcon(myIcon);
            	c.add(iconLabel, BorderLayout.EAST);
            }
            

            if (isDisabled && disabledFgColor != null) {
            	// disabled fgColor only to be used when col is not selected
            	// if disabled AND selection applies, use italic font to display the disabled state.
                if (!isSelected) {
                	fgColor = disabledFgColor;
                } else {
            		c.setFont(c.getFont().deriveFont(Font.ITALIC));
                }
            }

            
            
            if (isMarked && markedFgColor != null) {
            	if (isSelected) {
                    // SGB Update for standard tables: when selected, we should always use the selection foreground.
                    // Marked FG color is red by default, and does not work well with blue as alternating line color.

            		fgColor = table.getSelectionForeground(); 

            	}
            	else {
            		fgColor = markedFgColor;
            	}
            }
            if (fontSize > 0) {
                c.setFont(c.getFont().deriveFont((float)fontSize));
            }

            c.setBackground(bkgColor);
            c.setForeground(fgColor);
            textLabel.setBackground(c.getBackground());
            textLabel.setForeground(c.getForeground());
            textLabel.setFont(c.getFont());
            
            return c;
        } catch (Exception e) {
            Logger.logdebug(e);
        	return null;
        }
    }

    public void setMarkedBold(boolean bold) {
        this.markedBold = bold;
    }

    public void setMarkedForegroundColor(Color c) {
        this.markedFgColor = c;
    }

    public void setMarkedBackgroundColor(Color c) {
        this.markedBkgColor = c;
    }

    public void setDisabledForegroundColor(Color c) {
        this.disabledFgColor = c;
    }

    public void setDisabledBackgroundColor(Color c) {
        this.disabledBkgColor = c;
    }

    public void setAlternatingRowColor(Color c) {
    	this.alternateColor = c;
    	this.useAlternatingColor=(c!=null && Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben());
    }
    public void setFontSize(int size) {
        this.fontSize = size;
    }

}

