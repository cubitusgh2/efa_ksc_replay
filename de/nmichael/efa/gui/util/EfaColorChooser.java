package de.nmichael.efa.gui.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.nmichael.efa.util.International;

/**
 * A drop-in replacement / wrapper for the standard {@link JColorChooser} that
 * adds a single additional chooser tab offering a set of curated pastel color
 * palettes (Fresh Bright Pastels and Professional Pastels, each in a Light
 * and a Dark variant), selectable via a drop-down box.
 * <p>
 * The tab shows a palette selector directly above the palette swatches on the
 * left side, and a control area on the right side (saturation adjustment, a
 * read-only hex code field for copy &amp; paste, and a "recently used"
 * history).
 *
 * @author efa (generated)
 */
public class EfaColorChooser extends JColorChooser {

    private static final long serialVersionUID = 1L;

    /** Shared history of recently selected colors, used by all palette panels. */
    private final RecentColorsModel recentColors = new RecentColorsModel(16);

    /**
     * Creates a new efa color chooser with white as the initially selected color.
     */
    public EfaColorChooser() {
        this(Color.WHITE);
    }

    /**
     * Creates a new efa color chooser and registers the palette panel.
     *
     * @param initialColor the color that is initially selected
     */
    public EfaColorChooser(Color initialColor) {
        super(initialColor);
        addChooserPanel(new PaletteChooserPanel(recentColors));
    }

    /**
     * Removes the built-in "Swatches" tab from this chooser, if present, so that
     * only the custom palette tabs (and the other standard tabs such as HSB/RGB)
     * remain visible.
     */
    public void removeDefaultSwatchesPanel() {
        AbstractColorChooserPanel[] panels = getChooserPanels();
        for (AbstractColorChooserPanel p : panels) {
            if ("Swatches".equals(p.getDisplayName())) {
                removeChooserPanel(p);
                break;
            }
        }
    }

    /**
     * Convenience method that displays an {@link EfaColorChooser} inside a modal
     * dialog, similar to {@link JColorChooser#showDialog(Component, String, Color)}.
     *
     * @param parent       the parent component for the dialog
     * @param title        the dialog title
     * @param initialColor the initially selected color
     * @return the color selected by the user, or {@code null} if the dialog was cancelled
     */
    public static Color showDialog(Component parent, String title, Color initialColor) {
        EfaColorChooser chooser = new EfaColorChooser(initialColor != null ? initialColor : Color.WHITE);
        final Color[] result = new Color[1];
        JDialog dialog = JColorChooser.createDialog(parent, title, true, chooser,
                e -> result[0] = chooser.getColor(),
                null);
        dialog.setVisible(true);
        return result[0];
    }

    // =====================================================================
    // Shared "recently used colors" model
    // =====================================================================

    /**
     * Simple observable list of recently selected colors. The most recently
     * used color is always kept at the front of the list, duplicates are
     * removed, and the list size is capped at {@link #maxSize}.
     */
    static class RecentColorsModel {
        private final int maxSize;
        private final LinkedList<Color> colors = new LinkedList<>();
        private final List<Runnable> listeners = new ArrayList<>();

        /**
         * Creates a new model.
         *
         * @param maxSize maximum number of colors kept in the history
         */
        RecentColorsModel(int maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * Adds a color to the front of the history, removing any previous
         * occurrence of the same color and trimming the list to {@link #maxSize}.
         *
         * @param c the color to remember
         */
        void addColor(Color c) {
            colors.remove(c);
            colors.addFirst(c);
            while (colors.size() > maxSize) {
                colors.removeLast();
            }
            fireChanged();
        }

        /**
         * @return the current list of recently used colors, most recent first
         */
        List<Color> getColors() {
            return colors;
        }

        /**
         * Registers a listener that is notified whenever the history changes.
         *
         * @param r callback to invoke on change
         */
        void addListener(Runnable r) {
            listeners.add(r);
        }

        /**
         * Notifies all registered listeners about a change in the history.
         */
        private void fireChanged() {
            for (Runnable r : listeners) {
                r.run();
            }
        }
    }

    // =====================================================================
    // Palette data
    // =====================================================================

    /**
     * Immutable description of a single color palette: a title, the names of
     * the color families (rows), the names of the shade columns, the actual
     * color matrix, and the background color to be used behind the swatches.
     */
    static class PaletteData {
        final String title;
        final String[] familyNames;
        final String[] columnNames;
        final Color[][] colors; // [family][column]
        final Color panelBackground;

        PaletteData(String title, String[] familyNames, String[] columnNames,
                    Color[][] colors, Color panelBackground) {
            this.title = title;
            this.familyNames = familyNames;
            this.columnNames = columnNames;
            this.colors = colors;
            this.panelBackground = panelBackground;
        }

        /** Used by the palette drop-down's default renderer. */
        @Override
        public String toString() {
            return title;
        }

        /**
         * @return all available palettes, in the order they should appear in
         *         the drop-down box; index 0 is the default selection.
         */
        static PaletteData[] values() {
            return new PaletteData[] {
                freshBrightLight(),
                professionalLight(),
                freshBrightDark(),
                professionalDark()
            };
        }

        /** Shorthand for {@link Color#decode(String)}. */
        private static Color c(String hex) {
            return Color.decode(hex);
        }

        /**
         * @return the "Fresh Bright Pastels (Light)" palette definition.
         *         Families are ordered along the rainbow spectrum
         *         (Rot -&gt; Koralle -&gt; Gelb -&gt; Gruen -&gt; Mint -&gt; Blau -&gt; Navy -&gt; Violett -&gt; Pink),
         *         followed by the non-spectral neutrals Sand and Braun.
         */
        static PaletteData freshBrightLight() {
            String[] families = {"Rot", "Koralle", "Gelb", "Grün", "Mint", "Blau", "Navy", "Violett", "Pink", "Sand", "Braun"};
            String[] cols = {"Very Light", "Light", "Accent", "Primary", "Hover"};
            Color[][] colors = {
                {c("#FFF4F4"), c("#FFE0E0"), c("#FFB3B3"), c("#FF7A7A"), c("#FF5C5C")},
                {c("#FFF6F4"), c("#FFE7E1"), c("#FFCFC4"), c("#FFB6A5"), c("#FFA08A")},
                {c("#FFFDF3"), c("#FFF8D8"), c("#FFF2B8"), c("#FFE98E"), c("#FFE06D")},
                {c("#F6FFF2"), c("#E8FBDD"), c("#C8F5B0"), c("#9EEA7A"), c("#82E058")},
                {c("#F2FFF9"), c("#DFFBF0"), c("#BFF5D8"), c("#95EDC0"), c("#78E3B0")},
                {c("#F4FBFF"), c("#EAF7FF"), c("#AEE2FF"), c("#73CAFF"), c("#5CBEFF")},
                {c("#F3F6FF"), c("#E6ECFF"), c("#B0C4FF"), c("#6F8EFF"), c("#5579FF")},
                {c("#FAF7FF"), c("#EFE8FF"), c("#DDD0FF"), c("#C2AEFF"), c("#AD93FF")},
                {c("#FFF6FC"), c("#FFE7F7"), c("#FFCDED"), c("#FFA9DD"), c("#FF8CD3")},
                {c("#FFFDF7"), c("#FDF6E8"), c("#F7E9C9"), c("#EFDBAF"), c("#E7CE9A")},
                {c("#FBF8F5"), c("#F1E8DF"), c("#E4D4C4"), c("#D0B69D"), c("#BF9E7F")}
            };
            return new PaletteData("Fresh Bright Pastels (Light)", families, cols, colors, Color.WHITE);
        }

        /**
         * @return the "Professional Pastels (Light)" palette definition.
         *         Families are ordered along the rainbow spectrum
         *         (Rot -&gt; Koralle -&gt; Gelb -&gt; Gruen -&gt; Mint -&gt; Blau -&gt; Navy -&gt; Violett -&gt; Pink),
         *         followed by the non-spectral neutrals Sand and Braun.
         */
        static PaletteData professionalLight() {
            String[] families = {"Rot", "Koralle", "Gelb", "Grün", "Mint", "Blau", "Navy", "Violett", "Pink", "Sand", "Braun"};
            String[] cols = {"Very Light", "Light", "Accent", "Primary", "Hover"};
            Color[][] colors = {
                {c("#FDF5F5"), c("#F9E6E6"), c("#EEC2C2"), c("#E3A0A0"), c("#D68888")},
                {c("#FFF8F6"), c("#F7E6E1"), c("#EDC8BE"), c("#F2B19E"), c("#E39A84")},
                {c("#FFFDF9"), c("#FBF4DE"), c("#F4E6B0"), c("#F8DF88"), c("#EBCF68")},
                {c("#F6FBF3"), c("#E9F3E0"), c("#CFE6BC"), c("#B3D99A"), c("#9BCB81")},
                {c("#F5FCF8"), c("#E8F5EE"), c("#C8E8D7"), c("#A5E3C5"), c("#8FD1B1")},
                {c("#F5FAFD"), c("#E9F3F9"), c("#BEDAEC"), c("#7DBFEA"), c("#69AFDC")},
                {c("#F4F6FA"), c("#E3E8F2"), c("#B9C6DE"), c("#8AA0C9"), c("#7290BD")},
                {c("#FAF8FD"), c("#EDE8F6"), c("#D6CAE8"), c("#BBA8DA"), c("#A28CC6")},
                {c("#FFF8FB"), c("#F8E8EF"), c("#EDC8D9"), c("#E5AFC4"), c("#D493AB")},
                {c("#FDFBF8"), c("#F6EEE2"), c("#EBDDBF"), c("#E5D0A8"), c("#D7BE92")},
                {c("#FAF9F7"), c("#EFE8DF"), c("#DCCDBE"), c("#C4AF9B"), c("#AE9782")}
            };
            return new PaletteData("Professional Pastels (Light)", families, cols, colors, Color.WHITE);
        }

        /**
         * @return the "Fresh Bright Pastels (Dark)" palette definition.
         *         Families are ordered along the rainbow spectrum
         *         (Red -&gt; Coral -&gt; Yellow -&gt; Green -&gt; Mint -&gt; Blue -&gt; Navy -&gt; Violet -&gt; Pink),
         *         followed by the non-spectral neutrals Sand and Brown.
         */
        static PaletteData freshBrightDark() {
            String[] families = {"Red", "Coral", "Yellow", "Green", "Mint", "Blue", "Navy", "Violet", "Pink", "Sand", "Brown"};
            String[] cols = {"Deep", "Surface", "Accent", "Primary", "Glow"};
            Color[][] colors = {
                {c("#441A1A"), c("#732E2E"), c("#C94C4C"), c("#FF7A7A"), c("#FFADAD")},
                {c("#44261F"), c("#734136"), c("#C87862"), c("#F2B19E"), c("#FFD8CC")},
                {c("#473B17"), c("#7B6527"), c("#C9A53A"), c("#F8DF88"), c("#FFF0AE")},
                {c("#2B3A17"), c("#4D6B28"), c("#7DAE3F"), c("#9EEA7A"), c("#C7F5A8")},
                {c("#173D34"), c("#276856"), c("#52B694"), c("#95EDC0"), c("#C8F9E0")},
                {c("#183044"), c("#255273"), c("#4C9FD9"), c("#73CAFF"), c("#A9E3FF")},
                {c("#12203A"), c("#1F3660"), c("#3E6FB0"), c("#6F9FE0"), c("#A8CDFF")},
                {c("#332B4A"), c("#574977"), c("#8D78B8"), c("#BBA8DA"), c("#E1D7F3")},
                {c("#442A38"), c("#75485F"), c("#C17898"), c("#E5AFC4"), c("#FFD9E8")},
                {c("#403626"), c("#6B5A3F"), c("#B89B5B"), c("#E5D0A8"), c("#F5E7C8")},
                {c("#362D26"), c("#635245"), c("#987F6B"), c("#C4AF9B"), c("#E5D6C7")}
            };
            return new PaletteData("Fresh Bright Pastels (Dark)", families, cols, colors, new Color(0x2B2B2B));
        }

        /**
         * @return the "Professional Pastels (Dark)" palette definition.
         *         Families are ordered along the rainbow spectrum
         *         (Red -&gt; Coral -&gt; Yellow -&gt; Green -&gt; Mint -&gt; Blue -&gt; Navy -&gt; Violet -&gt; Pink),
         *         followed by the non-spectral neutrals Sand and Brown.
         */
        static PaletteData professionalDark() {
            String[] families = {"Red", "Coral", "Yellow", "Green", "Mint", "Blue", "Navy", "Violet", "Pink", "Sand", "Brown"};
            String[] cols = {"Surface Dark", "Light Accent", "Accent", "Primary", "Strong"};
            Color[][] colors = {
                {c("#301F1F"), c("#6B3A3A"), c("#9C5C5C"), c("#E3A0A0"), c("#F0C4C4")},
                {c("#31231F"), c("#8A6052"), c("#C18673"), c("#F2B19E"), c("#F7D0C1")},
                {c("#2D291D"), c("#7A6840"), c("#BFA052"), c("#F8DF88"), c("#FCECB9")},
                {c("#232B1D"), c("#4A5A3A"), c("#78915C"), c("#B3D99A"), c("#D6ECC4")},
                {c("#1F2A25"), c("#44695A"), c("#72A68D"), c("#A5E3C5"), c("#C7F0DA")},
                {c("#1B2832"), c("#385A70"), c("#5F8FB0"), c("#7DBFEA"), c("#A5D7F4")},
                {c("#151F2E"), c("#2E4766"), c("#4F6F95"), c("#7FA3C8"), c("#A9C6E0")},
                {c("#262230"), c("#6E608D"), c("#9887BC"), c("#BBA8DA"), c("#D6CAF0")},
                {c("#2D2328"), c("#8B6674"), c("#BC8FA0"), c("#E5AFC4"), c("#F1CEDB")},
                {c("#2A2620"), c("#6A5E49"), c("#A89166"), c("#E5D0A8"), c("#F1E2C6")},
                {c("#292421"), c("#746355"), c("#9E8673"), c("#C4AF9B"), c("#E0D1C4")}
            };
            return new PaletteData("Professional Pastels (Dark)", families, cols, colors, new Color(0x2B2B2B));
        }
    }

    // =====================================================================
    // Palette panel (one tab in the color chooser)
    // =====================================================================

    /**
     * The single color chooser tab that renders a {@link PaletteData} palette.
     * The active palette is selectable at runtime via a drop-down box placed
     * directly above the swatch grid; the default selection is
     * "Fresh Bright Pastels (Light)".
     * <p>
     * Layout (via {@link GridBagLayout}):
     * <ul>
     *   <li>Top-left: the palette selector drop-down.</li>
     *   <li>Left side, below the drop-down: the palette swatches, arranged in
     *       an anti-aliased grid and painted on a rounded-corner background
     *       panel.</li>
     *   <li>Right side: exactly one saturation control (a slider paired with
     *       a synchronized numeric spinner - together forming a single
     *       logical control), a read-only hex code field, and the shared
     *       "recently used" history.</li>
     * </ul>
     */
    static class PaletteChooserPanel extends AbstractColorChooserPanel {

        private static final long serialVersionUID = 1L;

        /** Corner radius used for the rounded palette background. */
        private static final int CORNER_ARC = 12;

        /** Maximum number of swatch rows visible without scrolling; beyond this, a vertical scrollbar appears. */
        private static final int VISIBLE_ROWS = 6;

        /** Vertical gap between swatch rows in the grid, matching the {@link GridLayout} vgap used in {@link #buildSwatchGrid()}. */
        private static final int SWATCH_ROW_GAP = 4;

        /** Top+bottom empty border applied around the swatch grid. */
        private static final int SWATCH_GRID_BORDER = 8;

        private final RecentColorsModel recentColors;

        /** All available palettes; built once so the combo box model and the initial selection share the same instances. */
        private final PaletteData[] palettes = PaletteData.values();

        /** The palette currently displayed; changes when the drop-down selection changes. */
        private PaletteData data;

        private JComboBox<PaletteData> paletteSelector;
        private RoundedBackgroundPanel swatchGrid;
        private RoundedBackgroundPanel recentPanel;
        private JSlider saturationSlider;
        private JSpinner saturationSpinner;
        private JTextField hexField;
        private ColorSwatch[][] swatches;

        /** Guard flag to prevent feedback loops between the slider and the spinner. */
        private boolean adjustingSaturationControls = false;

        /**
         * Creates a new palette panel, initially showing the default palette
         * ("Fresh Bright Pastels (Light)").
         *
         * @param recentColors the shared "recently used colors" model
         */
        PaletteChooserPanel(RecentColorsModel recentColors) {
            this.recentColors = recentColors;
            this.data = palettes[0];
        }

        /** {@inheritDoc} */
        @Override
        public String getDisplayName() {
            return International.getString("Pastell-Paletten");
        }

        /** {@inheritDoc} */
        @Override
        public Icon getSmallDisplayIcon() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Icon getLargeDisplayIcon() {
            return null;
        }

        /**
         * Builds the panel UI: the palette selector and swatch grid on the
         * left, and the control column (saturation, hex field, recent colors)
         * on the right, using a {@link GridBagLayout}.
         */
        @Override
        protected void buildChooser() {
            setLayout(new GridBagLayout());

            JComboBox<PaletteData> selector = buildPaletteSelector();
            JScrollPane scrollSwatchPane = buildSwatchGrid();
            JPanel controlsPanel = buildControlsPanel();

            GridBagConstraints gbc = new GridBagConstraints();

            // Top-left: palette selector, directly above the swatch grid.
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.6;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(8, 8, 4, 4);
            add(selector, gbc);

            // Left side, below the selector: palette swatches, takes most of the space.
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 1;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(4, 8, 8, 4);
            add(scrollSwatchPane, gbc);

            // Right column: saturation control, hex field, recent colors.
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            gbc.weightx = 0.4;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(8, 4, 8, 8);
            add(controlsPanel, gbc);
        }

        /**
         * Creates the drop-down box used to pick the active palette at
         * runtime. The default selection is "Fresh Bright Pastels (Light)".
         * Selecting a new entry triggers {@link #reloadPalette()}.
         *
         * @return the palette selector combo box
         */
        private JComboBox<PaletteData> buildPaletteSelector() {
            paletteSelector = new JComboBox<>(palettes);
            paletteSelector.setSelectedItem(data);
            paletteSelector.addActionListener(e -> {
                data = (PaletteData) paletteSelector.getSelectedItem();
                reloadPalette();
            });
            return paletteSelector;
        }

        /**
         * Creates the scrollable, rounded-corner grid of color swatches that
         * represents the currently selected palette on the left side of the
         * panel. Rendering quality hints (anti-aliasing) are applied by the
         * individual swatches and the rounded background for the crispest
         * possible display.
         * <p>
         * The scroll pane never shows a horizontal scrollbar (the grid always
         * fits its available width); a vertical scrollbar automatically
         * appears whenever the available viewport height is smaller than the
         * palette's natural height. The initial preferred viewport height
         * corresponds to {@link #VISIBLE_ROWS} swatch rows, but once the
         * surrounding dialog/window is resized larger by the user, the grid
         * grows to fill the extra space (see {@link ScrollableRoundedPanel}),
         * shrinking back and showing the scrollbar again if space becomes
         * scarce.
         *
         * @return a scroll pane containing the swatch grid
         */
        private JScrollPane buildSwatchGrid() {
            int rows = data.familyNames.length;
            int cols = data.columnNames.length;

            swatchGrid = new ScrollableRoundedPanel(new GridLayout(rows, cols, 4, SWATCH_ROW_GAP), CORNER_ARC);
            swatchGrid.setBackground(data.panelBackground);
            swatchGrid.setBorder(BorderFactory.createEmptyBorder(SWATCH_GRID_BORDER, SWATCH_GRID_BORDER, SWATCH_GRID_BORDER, SWATCH_GRID_BORDER));

            swatches = new ColorSwatch[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int cIdx = 0; cIdx < cols; cIdx++) {
                    ColorSwatch sw = new ColorSwatch(data.colors[r][cIdx]);
                    sw.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            onSwatchClicked(sw.getCurrentColor());
                        }
                    });
                    swatches[r][cIdx] = sw;
                    swatchGrid.add(sw);
                }
            }

            JScrollPane scrollPane = new JScrollPane(swatchGrid,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);

            // Set only the *initial* preferred viewport height (VISIBLE_ROWS swatch
            // rows); this merely influences the dialog's initial size. Actual growth
            // or shrinking when the user resizes the surrounding window is handled
            // by ScrollableRoundedPanel's Scrollable implementation, which stretches
            // the grid to fill any extra viewport space (or reverts to its natural
            // size - triggering the scrollbar - when space is scarce).
            int swatchHeight = swatches[0][0].getPreferredSize().height;
            int visibleHeight = VISIBLE_ROWS * swatchHeight
                    + (VISIBLE_ROWS - 1) * SWATCH_ROW_GAP
                    + 2 * SWATCH_GRID_BORDER;
            Dimension gridPreferred = swatchGrid.getPreferredSize();
            scrollPane.getViewport().setPreferredSize(new Dimension(gridPreferred.width, visibleHeight));

            return scrollPane;
        }

        /**
         * Rebuilds the swatch colors, grid background, and recent-colors
         * background after the user selects a different palette from the
         * drop-down box, and resets the single saturation control back to
         * its neutral (0%) position.
         */
        private void reloadPalette() {
            swatchGrid.setBackground(data.panelBackground);
            for (int r = 0; r < data.familyNames.length; r++) {
                for (int cIdx = 0; cIdx < data.columnNames.length; cIdx++) {
                    swatches[r][cIdx].setCurrentColor(data.colors[r][cIdx]);
                }
            }
            if (recentPanel != null) {
                recentPanel.setBackground(data.panelBackground);
            }

            // Reset the one-and-only saturation control (slider + spinner) to neutral.
            adjustingSaturationControls = true;
            saturationSlider.setValue(0);
            saturationSpinner.setValue(0);
            adjustingSaturationControls = false;

            swatchGrid.revalidate();
            swatchGrid.repaint();
            if (recentPanel != null) {
                recentPanel.repaint();
            }
        }

        /**
         * Creates the right-hand control column containing the saturation
         * control, the read-only hex code field, and the recent colors history.
         *
         * @return the assembled controls panel
         */
        private JPanel buildControlsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 8, 0);

            gbc.gridy = 0;
            gbc.weighty = 0;
            panel.add(buildSaturationPanel(), gbc);

            gbc.gridy = 1;
            panel.add(buildHexFieldPanel(), gbc);

            gbc.gridy = 2;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            panel.add(buildRecentColorsPanel(), gbc);

            return panel;
        }

        /**
         * Creates the saturation control group - the single user interface
         * element for adjusting color saturation. It consists of a slider and
         * a spinner (numeric field with up/down buttons, like the standard
         * RGB chooser panel) that are kept perfectly in sync with each other:
         * changing either one updates the other and immediately re-applies
         * the saturation to every swatch in the grid.
         *
         * @return the saturation control panel
         */
        private JPanel buildSaturationPanel() {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(createRoundedTitledBorder(International.getString("Sättigung")));

            saturationSlider = new JSlider(-100, 100, 0);
            saturationSlider.setMajorTickSpacing(50);
            saturationSlider.setPaintTicks(true);
            saturationSlider.setPaintLabels(true);

            saturationSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
            // Keep the spinner reasonably narrow, like a classic up/down numeric field.
            ((JSpinner.DefaultEditor) saturationSpinner.getEditor()).getTextField().setColumns(4);

            saturationSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (adjustingSaturationControls) {
                        return;
                    }
                    adjustingSaturationControls = true;
                    int value = saturationSlider.getValue();
                    saturationSpinner.setValue(value);
                    applySaturation(value);
                    adjustingSaturationControls = false;
                }
            });

            saturationSpinner.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (adjustingSaturationControls) {
                        return;
                    }
                    adjustingSaturationControls = true;
                    int value = (Integer) saturationSpinner.getValue();
                    saturationSlider.setValue(value);
                    applySaturation(value);
                    adjustingSaturationControls = false;
                }
            });

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 4, 2, 4);
            p.add(saturationSlider, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            p.add(saturationSpinner, gbc);

            return p;
        }

        /**
         * Creates the read-only hex code field that displays the currently
         * selected swatch color, ready to be copied via copy &amp; paste.
         *
         * @return the hex field panel
         */
        private JPanel buildHexFieldPanel() {
            JPanel p = new JPanel(new BorderLayout(4, 0));
            p.setBorder(createRoundedTitledBorder(International.getString("Farbcode")));
            hexField = new JTextField();
            hexField.setEditable(false);
            hexField.setFocusable(true);
            hexField.setHorizontalAlignment(JTextField.CENTER);
            p.add(hexField, BorderLayout.CENTER);
            return p;
        }

        /**
         * Creates the "recently used colors" panel, rendered on the same
         * rounded background style as the main palette, and registers this
         * panel as a listener on the shared {@link RecentColorsModel}.
         *
         * @return the recent colors panel
         */
        private JPanel buildRecentColorsPanel() {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBorder(createRoundedTitledBorder(International.getString("Zuletzt verwendet")));

            recentPanel = new RoundedBackgroundPanel(new FlowLayout(FlowLayout.LEFT, 4, 4), CORNER_ARC);
            recentPanel.setBackground(data.panelBackground);
            refreshRecentPanel();
            recentColors.addListener(this::refreshRecentPanel);

            JScrollPane scrollPane = new JScrollPane(recentPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);

            wrapper.add(scrollPane, BorderLayout.CENTER);
            return wrapper;
        }

        /**
         * Handles a swatch (or recent-color) click: publishes the hex code to
         * the read-only text field, updates the shared color selection model,
         * and records the color in the recent colors history.
         *
         * @param c the color that was clicked
         */
        private void onSwatchClicked(Color c) {
            hexField.setText(String.format("#%06X", c.getRGB() & 0xFFFFFF));
            hexField.selectAll();
            getColorSelectionModel().setSelectedColor(c);
            recentColors.addColor(c);
        }

        /**
         * Re-computes every swatch color based on the palette's base colors
         * and the given saturation percentage, then repaints the grid.
         *
         * @param percent saturation adjustment in the range -100..+100
         */
        private void applySaturation(int percent) {
            for (int r = 0; r < data.familyNames.length; r++) {
                for (int cIdx = 0; cIdx < data.columnNames.length; cIdx++) {
                    Color base = data.colors[r][cIdx];
                    Color adjusted = adjustSaturation(base, percent);
                    swatches[r][cIdx].setCurrentColor(adjusted);
                }
            }
            swatchGrid.repaint();
        }

        /**
         * Adjusts the saturation (HSB "S" component) of a color by a relative
         * percentage, without modifying its hue or brightness.
         * <p>
         * A positive percentage moves the saturation towards full saturation
         * (1.0), a negative percentage moves it towards gray (0.0).
         *
         * @param base    the original color
         * @param percent desired adjustment in the range -100..+100
         * @return the resulting color
         */
        private Color adjustSaturation(Color base, int percent) {
            float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
            float s = hsb[1];
            if (percent >= 0) {
                s = s + (1f - s) * (percent / 100f);
            } else {
                s = s * (1f + percent / 100f);
            }
            s = Math.max(0f, Math.min(1f, s));
            return Color.getHSBColor(hsb[0], s, hsb[2]);
        }

        /**
         * Rebuilds the "recently used" swatch row from the shared model.
         * Invoked both at initialization and whenever the shared model
         * notifies this panel of a change.
         */
        private void refreshRecentPanel() {
            if (recentPanel == null) {
                return;
            }
            recentPanel.removeAll();
            for (Color c : recentColors.getColors()) {
                ColorSwatch sw = new ColorSwatch(c);
                sw.setPreferredSize(new Dimension(48, 32));
                sw.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        onSwatchClicked(c);
                    }
                });
                recentPanel.add(sw);
            }
            recentPanel.revalidate();
            recentPanel.repaint();
        }

        /** {@inheritDoc} */
        @Override
        public void updateChooser() {
            // No external synchronization required: selection changes made outside
            // this panel do not need to be reflected back into the swatch grid.
        }
    }

    // =====================================================================
    // Rounded-corner background container
    // =====================================================================

    /**
     * A simple, non-opaque {@link JPanel} that paints its background as a
     * rounded rectangle before delegating to the normal Swing paint pipeline.
     * Used to give the palette and recent-colors areas rounded corners.
     */
    static class RoundedBackgroundPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final int arcSize;

        /**
         * Creates a rounded background panel.
         *
         * @param layout  the layout manager for the panel's children
         * @param arcSize the corner arc diameter, in pixels
         */
        RoundedBackgroundPanel(LayoutManager layout, int arcSize) {
            super(layout);
            this.arcSize = arcSize;
            setOpaque(false);
        }

        /** {@inheritDoc} */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arcSize, arcSize);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================
    // Scrollable rounded-corner background container (grows with viewport)
    // =====================================================================

    /**
     * A {@link RoundedBackgroundPanel} that additionally implements
     * {@link Scrollable} so that, when placed inside a {@link JScrollPane},
     * it stretches to fill the full height of the viewport whenever the
     * surrounding window is resized larger than the panel's natural
     * (preferred) size - instead of leaving blank space below it, as a plain
     * {@link JPanel} would.
     * <p>
     * If the viewport becomes smaller than the panel's preferred size again
     * (e.g. the window is shrunk, or the palette has many rows), the panel
     * reverts to its natural size and the scroll pane's vertical scrollbar
     * reappears as needed.
     */
    static class ScrollableRoundedPanel extends RoundedBackgroundPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a scrollable rounded background panel.
         *
         * @param layout  the layout manager for the panel's children
         * @param arcSize the corner arc diameter, in pixels
         */
        ScrollableRoundedPanel(LayoutManager layout, int arcSize) {
            super(layout, arcSize);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        /**
         * The grid never needs to scroll horizontally, so it always tracks
         * (matches) the viewport's width.
         */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        /**
         * Tracks (stretches to fill) the viewport's height only while the
         * viewport is at least as tall as this panel's preferred height;
         * otherwise reports {@code false} so the scroll pane falls back to
         * the panel's natural size and offers a vertical scrollbar.
         */
        @Override
        public boolean getScrollableTracksViewportHeight() {
            Container parent = getParent();
            if (parent instanceof JViewport) {
                return ((JViewport) parent).getHeight() > getPreferredSize().height;
            }
            return false;
        }
    }

    // =====================================================================
    // Rounded-corner titled border
    // =====================================================================

    /**
     * A thin, rounded-corner line border (arc radius 8px) used as the
     * decorative frame for {@link TitledBorder}s throughout this chooser,
     * giving every titled group box (saturation, hex code, recently used)
     * softly rounded corners instead of sharp ones.
     */
    static class RoundedLineBorder extends AbstractBorder {

        private static final long serialVersionUID = 1L;

        /** Arc radius (in pixels) applied to all four corners. */
        private static final int ARC = 8;

        private final Color lineColor;

        /**
         * Creates a new rounded line border.
         *
         * @param lineColor the color used to stroke the border line
         */
        RoundedLineBorder(Color lineColor) {
            this.lineColor = lineColor;
        }

        /** {@inheritDoc} */
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(lineColor);
            g2.drawRoundRect(x, y, width - 1, height - 1, ARC, ARC);
            g2.dispose();
        }

        /** {@inheritDoc} */
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 6, 4, 6);
        }

        /** {@inheritDoc} */
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(4, 6, 4, 6);
            return insets;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /**
     * Creates a {@link TitledBorder} whose decorative frame is a
     * {@link RoundedLineBorder} (8px corner arc) instead of the default
     * sharp-cornered line border, and whose title text is rendered in bold.
     *
     * @param title the border's title text
     * @return the rounded-corner titled border with a bold title
     */
    private static TitledBorder createRoundedTitledBorder(String title) {
        Color lineColor = UIManager.getColor("controlShadow");
        Border roundedLine = new RoundedLineBorder(lineColor != null ? lineColor : Color.GRAY);
        TitledBorder border = BorderFactory.createTitledBorder(roundedLine, title);
        Font titleFont = UIManager.getFont("TitledBorder.font");
        if (titleFont == null) {
            titleFont = new JLabel().getFont();
        }
        border.setTitleFont(titleFont.deriveFont(Font.BOLD));
        return border;
    }



    /**
     * A clickable, rounded-rectangle color swatch that displays its own hex
     * color code twice - once in black and once in white, stacked on top of
     * each other in bold - so the code remains legible regardless of the
     * swatch's brightness.
     */
    static class ColorSwatch extends JComponent {

        private static final long serialVersionUID = 1L;

        private Color currentColor;

        /**
         * Creates a new swatch for the given color.
         *
         * @param color the initial color to display
         */
        ColorSwatch(Color color) {
            this.currentColor = color;
            setPreferredSize(new Dimension(70, 44));
            setToolTipText(hexOf(color));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /** @return the color currently displayed by this swatch */
        Color getCurrentColor() {
            return currentColor;
        }

        /**
         * Updates the displayed color (e.g. after a saturation change) and
         * repaints the swatch.
         *
         * @param c the new color
         */
        void setCurrentColor(Color c) {
            this.currentColor = c;
            setToolTipText(hexOf(c));
            repaint();
        }

        /**
         * Formats a color as an uppercase "#RRGGBB" hex string.
         *
         * @param c the color to format
         * @return the hex representation
         */
        private static String hexOf(Color c) {
            return String.format("#%06X", c.getRGB() & 0xFFFFFF);
        }

        /** {@inheritDoc} */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int arc = Math.min(16, Math.min(w, h) / 3);

            g2.setColor(currentColor);
            g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

            g2.setColor(new Color(0, 0, 0, 40));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);

            String hex = hexOf(currentColor);
            Font f = getFont().deriveFont(Font.BOLD, 11f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(hex);
            int x = (w - textWidth) / 2;
            int centerY = h / 2;

            // Black label drawn above the vertical center.
            g2.setColor(Color.BLACK);
            g2.drawString(hex, x, centerY - 1);

            // White label drawn below the vertical center.
            g2.setColor(Color.WHITE);
            g2.drawString(hex, x, centerY + fm.getAscent());

            g2.dispose();
        }
    }
}