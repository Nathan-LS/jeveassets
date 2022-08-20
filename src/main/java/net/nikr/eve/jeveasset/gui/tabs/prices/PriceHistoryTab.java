/*
 * Copyright 2009-2022 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package net.nikr.eve.jeveasset.gui.tabs.prices;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.sde.Item;
import net.nikr.eve.jeveasset.data.sde.StaticData;
import net.nikr.eve.jeveasset.data.settings.Colors;
import net.nikr.eve.jeveasset.data.settings.PriceDataSettings.PriceMode;
import net.nikr.eve.jeveasset.data.settings.PriceHistoryDatabase;
import net.nikr.eve.jeveasset.data.settings.Settings;
import net.nikr.eve.jeveasset.data.settings.types.LocationType;
import net.nikr.eve.jeveasset.gui.frame.StatusPanel;
import net.nikr.eve.jeveasset.gui.frame.StatusPanel.Progress;
import net.nikr.eve.jeveasset.gui.frame.StatusPanel.UpdateType;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.CaseInsensitiveComparator;
import net.nikr.eve.jeveasset.gui.shared.Formater;
import net.nikr.eve.jeveasset.gui.shared.InstantToolTip;
import net.nikr.eve.jeveasset.gui.shared.components.JAutoCompleteDialog;
import net.nikr.eve.jeveasset.gui.shared.components.JDateChooser;
import net.nikr.eve.jeveasset.gui.shared.components.JDropDownButton;
import net.nikr.eve.jeveasset.gui.shared.components.JLockWindow;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTabSecondary;
import net.nikr.eve.jeveasset.gui.shared.components.JMultiSelectionList;
import net.nikr.eve.jeveasset.gui.shared.table.EventModels;
import net.nikr.eve.jeveasset.gui.tabs.tracker.QuickDate;
import net.nikr.eve.jeveasset.i18n.TabPriceHistory;
import net.nikr.eve.jeveasset.io.online.ZkillboardPricesHistoryGetter;
import net.nikr.eve.jeveasset.io.shared.ApiIdConverter;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PriceHistoryTab extends JMainTabSecondary {

	private static final Logger LOG = LoggerFactory.getLogger(PriceHistoryTab.class);

	private enum PriceHistoryAction {
		QUICK_DATE,
		SOURCE,
		ADD_ITEM,
		CLEAR_ITEMS,
		REMOVE_ITEMS,
		SAVE,
		MANAGE,
		INCLUDE_ZERO,
		LOGARITHMIC
	}

	private enum PriceHistorySource {
		ZKILLBOARD() {
			@Override
			String getText() {
				return TabPriceHistory.get().sourcezKillboard();
			}
		},
		JEVEASSETS() {
			@Override
			String getText() {
				return TabPriceHistory.get().sourcejEveAssets();
			}
		};


		abstract String getText();

		@Override
		public String toString() {
			return getText();
		}
	}

	private final Shape RECTANGLE = new Rectangle(-5, -5, 10, 10);
	private final Shape ELLIPSE = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);
	private final Shape LEGEND = RECTANGLE;
	private final Shape ITEM_SHAPE = new Ellipse2D.Float(-3.0f, -3.0f, 6.0f, 6.0f);
	private final Stroke LEGEND_OUTLINE = new BasicStroke(1);
	private final int PANEL_WIDTH_MINIMUM = 215;
	public final int MAXIMUM_SHOWN = 50;
	private final XYLineAndShapeRenderer renderer;


	private final DateFormat dateFormat = new SimpleDateFormat(Formater.COLUMN_DATE);
	private final NumberFormat iskFormat = new DecimalFormat("#,##0.00 isk");

	//GUI
	private final JComboBox<PriceHistorySource> jSource;
	private final JComboBox<PriceMode> jPriceType;
	private final JComboBox<QuickDate> jQuickDate;
	private final JDateChooser jFrom;
	private final JDateChooser jTo;
	private final JDropDownButton jEdit;
	private final JMenuItem jRemove;
	private final JMenuItem jClear;
	private final JButton jSave;
	private final JDropDownButton jLoad;
	private final JMenuItem jManage;
	private final JAutoCompleteDialog<Item> jAddItemDialog;
	private final JAutoCompleteDialog<String> jSaveItemsDialog;
	private final JManageItemsDialog jManageItemsDialog;
	private final JMultiSelectionList<Item> jItems;
	//Graph
	private final JCheckBoxMenuItem jIncludeZero;
	private final JRadioButtonMenuItem jLogarithmic;
	private final LogarithmicAxis rangeLogarithmicAxis;
	private final NumberAxis rangeLinearAxis;
	private final ChartPanel jChartPanel;
	private final JFreeChart jNextChart;

	//Dialog
	private final JLockWindow jLockWindow;

	private final ListenerClass listener = new ListenerClass();

	//Data
	private final DefaultListModel<Item> itemsModel;
	private final List<Item> shownOrder = new ArrayList<>();
	private final Set<Integer> shownTypeIDs = new HashSet<>();
	private final Map<Item, Set<PriceHistoryData>> shownData = new TreeMap<>();
	private final Map<Item, TimePeriodValues> series = new HashMap<>();
	private final Map<Item, Double> seriesMax = new HashMap<>();
	private final TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();

	//Settings ToDo
	private Date fromDate = null;
	private Date toDate = null;
	public static final String NAME = "pricehistory"; //Not to be changed!



	public PriceHistoryTab(Program program) {
		super(program, NAME, TabPriceHistory.get().title(), Images.TOOL_PRICE_HISTORY.getIcon(), true);

		jLockWindow = new JLockWindow(program.getMainWindow().getFrame());

		jSource = new JComboBox<>(PriceHistorySource.values());
		jSource.setActionCommand(PriceHistoryAction.SOURCE.name());
		jSource.addActionListener(listener);

		jPriceType = new JComboBox<>(PriceMode.values());
		jPriceType.setSelectedItem(Settings.get().getPriceDataSettings().getPriceType());
		jPriceType.setActionCommand(PriceHistoryAction.SOURCE.name());
		jPriceType.addActionListener(listener);
		jPriceType.setEnabled(false);

		JSeparator jDateSeparator = new JSeparator();

		jQuickDate = new JComboBox<>(QuickDate.values());
		jQuickDate.setActionCommand(PriceHistoryAction.QUICK_DATE.name());
		jQuickDate.addActionListener(listener);

		JLabel jFromLabel = new JLabel(TabPriceHistory.get().from());
		jFrom = new JDateChooser(true);
		if (fromDate != null) {
			jFrom.setDate(dateToLocalDate(fromDate));
		}
		jFrom.addDateChangeListener(listener);

		JLabel jToLabel = new JLabel(TabPriceHistory.get().to());
		jTo = new JDateChooser(true);
		if (toDate != null) {
			jTo.setDate(dateToLocalDate(toDate));
		}
		jTo.addDateChangeListener(listener);

		jAddItemDialog = new JAutoCompleteDialog<Item>(program, TabPriceHistory.get().addTitle(), Images.TOOL_PRICE_HISTORY.getImage(), null, true) {
			@Override
			protected Comparator<Item> getComparator() {
				return GlazedLists.comparableComparator();
			}

			@Override
			protected TextFilterator<Item> getFilterator() {
				return new EventModels.ItemFilterator();
			}

			@Override
			protected Item getValue(Object object) {
				if (object instanceof Item) {
					return (Item) object;
				}
				return null;
			}

			@Override
			protected boolean isEmpty(Item t) {
				return false;
			}
		};
		jAddItemDialog.updateData(StaticData.get().getItems().values());

		jSaveItemsDialog = new JAutoCompleteDialog<String>(program, TabPriceHistory.get().saveTitle(), Images.TOOL_PRICE_HISTORY.getImage(), null, false) {
			@Override
			protected Comparator<String> getComparator() {
				return GlazedLists.comparableComparator();
			}

			@Override
			protected TextFilterator<String> getFilterator() {
				return new EventModels.StringFilterator();
			}

			@Override
			protected String getValue(Object object) {
				return String.valueOf(object);
			}

			@Override
			protected boolean isEmpty(String t) {
				return t.isEmpty();
			}
		};
		jManageItemsDialog = new JManageItemsDialog(program, this);

		JDropDownButton jSettings = new JDropDownButton(Images.DIALOG_SETTINGS.getIcon());

		jSettings.addSeparator();

		jIncludeZero = new JCheckBoxMenuItem(TabPriceHistory.get().includeZero());
		jIncludeZero.setSelected(true);
		jIncludeZero.setActionCommand(PriceHistoryAction.INCLUDE_ZERO.name());
		jIncludeZero.addActionListener(listener);
		jSettings.add(jIncludeZero);

		jSettings.addSeparator();

		ButtonGroup buttonGroup = new ButtonGroup();

		JRadioButtonMenuItem jLinear = new JRadioButtonMenuItem(TabPriceHistory.get().scaleLinear());
		jLinear.setSelected(true);
		jLinear.setActionCommand(PriceHistoryAction.LOGARITHMIC.name());
		jLinear.addActionListener(listener);
		jSettings.add(jLinear);
		buttonGroup.add(jLinear);

		jLogarithmic = new JRadioButtonMenuItem(TabPriceHistory.get().scaleLogarithmic());
		jLogarithmic.setSelected(false);
		jLogarithmic.setActionCommand(PriceHistoryAction.LOGARITHMIC.name());
		jLogarithmic.addActionListener(listener);
		jSettings.add(jLogarithmic);
		buttonGroup.add(jLogarithmic);

		jEdit = new JDropDownButton(Images.EDIT_EDIT.getIcon());
		jEdit.setToolTipText(TabPriceHistory.get().edit());

		JMenuItem jAdd = new JMenuItem(TabPriceHistory.get().add(), Images.EDIT_ADD.getIcon());
		jAdd.setActionCommand(PriceHistoryAction.ADD_ITEM.name());
		jAdd.addActionListener(listener);
		jEdit.add(jAdd);

		jRemove = new JMenuItem(TabPriceHistory.get().remove(), Images.EDIT_DELETE.getIcon());
		jRemove.setActionCommand(PriceHistoryAction.REMOVE_ITEMS.name());
		jRemove.addActionListener(listener);
		jRemove.setEnabled(false);
		jEdit.add(jRemove);

		jClear = new JMenuItem(TabPriceHistory.get().clear(), Images.MISC_EXIT.getIcon());
		jClear.setActionCommand(PriceHistoryAction.CLEAR_ITEMS.name());
		jClear.addActionListener(listener);
		jClear.setEnabled(false);
		jEdit.add(jClear);

		jSave = new JButton(Images.FILTER_SAVE.getIcon());
		jSave.setToolTipText(TabPriceHistory.get().save());
		jSave.setActionCommand(PriceHistoryAction.SAVE.name());
		jSave.setEnabled(false);
		jSave.addActionListener(listener);

		jLoad = new JDropDownButton(Images.FILTER_LOAD.getIcon());

		jManage = new JMenuItem(TabPriceHistory.get().manage(), Images.DIALOG_SETTINGS.getIcon());
		jManage.setActionCommand(PriceHistoryAction.MANAGE.name());
		jManage.addActionListener(listener);

		itemsModel = new DefaultListModel<>();
		jItems = new JMultiSelectionList<>(itemsModel);
		jItems.getSelectionModel().addListSelectionListener(listener);
		jItems.addMouseListener(listener);
		JScrollPane jOwnersScroll = new JScrollPane(jItems);

		Font font = new JLabel().getFont();

		DateAxis domainAxis = new DateAxis();
		domainAxis.setDateFormatOverride(dateFormat);
		domainAxis.setVerticalTickLabels(true);
		domainAxis.setAutoTickUnitSelection(true);
		domainAxis.setAutoRange(true);
		domainAxis.setTickLabelFont(font);
		domainAxis.setTickLabelPaint(Colors.TEXTFIELD_FOREGROUND.getColor());

		rangeLogarithmicAxis = new LogarithmicAxis("");
		rangeLogarithmicAxis.setStrictValuesFlag(false);
		rangeLogarithmicAxis.setNumberFormatOverride(Formater.AUTO_FORMAT);
		rangeLogarithmicAxis.setTickLabelFont(jFromLabel.getFont());
		rangeLogarithmicAxis.setTickLabelPaint(Colors.TEXTFIELD_FOREGROUND.getColor());
		rangeLogarithmicAxis.setAutoRangeIncludesZero(true);

		rangeLinearAxis = new NumberAxis();
		rangeLinearAxis.setAutoRange(true);
		rangeLinearAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
		rangeLinearAxis.setTickLabelFont(font);
		rangeLinearAxis.setTickLabelPaint(Colors.TEXTFIELD_FOREGROUND.getColor());
		rangeLinearAxis.setAutoRangeIncludesZero(true);

		renderer = new MyRender();
		renderer.setDefaultToolTipGenerator(new XYToolTipGenerator() {
			@Override
			public String generateToolTip(XYDataset dataset, int series, int item)	{
				Date date = new Date(dataset.getX(series, item).longValue());
				Number isk = dataset.getY(series, item);
				return TabPriceHistory.get().graphToolTip(dataset.getSeriesKey(series), iskFormat.format(isk), dateFormat.format(date));
			}
		});

		XYPlot plot = new XYPlot(dataset, domainAxis, rangeLinearAxis, renderer);
		plot.setBackgroundPaint(Colors.TEXTFIELD_BACKGROUND.getColor());
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setDomainCrosshairLockedOnData(true);
		plot.setDomainCrosshairStroke(new BasicStroke(1));
		plot.setDomainCrosshairPaint(Color.BLACK);
		plot.setDomainCrosshairVisible(false);
		plot.setRangeCrosshairLockedOnData(true);
		plot.setRangeCrosshairVisible(false);

		jNextChart = new JFreeChart(plot);
		jNextChart.setAntiAlias(true);
		jNextChart.setBackgroundPaint(Colors.COMPONENT_BACKGROUND.getColor());
		jNextChart.addProgressListener(null);
		jNextChart.getLegend().setItemFont(font);
		jNextChart.getLegend().setItemPaint(Colors.TEXTFIELD_FOREGROUND.getColor());
		jNextChart.getLegend().setBackgroundPaint(Colors.COMPONENT_BACKGROUND.getColor());
		//jNextChart.setTextAntiAlias(false);
		//jNextChart.setAntiAlias(false);
		/*
		Map<RenderingHints.Key,Object> rh = new HashMap<>();
		rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		//rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);


		//rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
		//rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

		//rh.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		rh.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DEFAULT);
		//rh.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);
		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);
		//rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

		//rh.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		rh.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
		//rh.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

		//rh.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		rh.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		//rh.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		//rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
		//rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

		//rh.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		rh.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_DEFAULT);
		//rh.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

		//rh.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		rh.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
		//rh.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		jNextChart.setRenderingHints(new RenderingHints(rh));
		*/

		jChartPanel = new ChartPanel(jNextChart);
		InstantToolTip.install(jChartPanel);
		jChartPanel.setDomainZoomable(false);
		jChartPanel.setRangeZoomable(false);
		jChartPanel.setPopupMenu(null);
		//jChartPanel.addChartMouseListener(listener);
		jChartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
		jChartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
		jChartPanel.setMinimumDrawWidth(10);
		jChartPanel.setMinimumDrawHeight(10);

		int gapWidth = 5;
		int labelWidth = Math.max(jFromLabel.getPreferredSize().width, jToLabel.getPreferredSize().width);
		int panelWidth = PANEL_WIDTH_MINIMUM;
		panelWidth = Math.max(panelWidth, jFrom.getPreferredSize().width + labelWidth + gapWidth);
		panelWidth = Math.max(panelWidth, jTo.getPreferredSize().width + labelWidth + gapWidth);
		int dateWidth = panelWidth - labelWidth - gapWidth;
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
					.addComponent(jChartPanel)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(jSource, panelWidth, panelWidth, panelWidth)
					.addComponent(jPriceType, panelWidth, panelWidth, panelWidth)
					.addComponent(jDateSeparator, panelWidth, panelWidth, panelWidth)
					.addComponent(jQuickDate, panelWidth, panelWidth, panelWidth)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup()
							.addComponent(jFromLabel, labelWidth, labelWidth, labelWidth)
							.addComponent(jToLabel, labelWidth, labelWidth, labelWidth)
						)
						.addGap(gapWidth)
						.addGroup(layout.createParallelGroup()
							.addComponent(jFrom, dateWidth, dateWidth, dateWidth)
							.addComponent(jTo, dateWidth, dateWidth, dateWidth)
						)
					)
					.addGroup(layout.createSequentialGroup()
						.addComponent(jSettings)
						.addComponent(jEdit)
						.addComponent(jSave)
						.addComponent(jLoad)
					)
					.addComponent(jOwnersScroll, panelWidth, panelWidth, panelWidth)
				)
		);
		layout.setVerticalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addComponent(jChartPanel)
				)
				.addGroup(layout.createSequentialGroup()
					.addComponent(jSource, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jPriceType, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jDateSeparator, 3, 3, 3)
					.addComponent(jQuickDate, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addGroup(layout.createParallelGroup()
						.addComponent(jFromLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jFrom, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					)
					.addGroup(layout.createParallelGroup()
						.addComponent(jToLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jTo, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					)
					.addGroup(layout.createParallelGroup()
						.addComponent(jSettings, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jEdit, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jSave, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jLoad, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					)
					.addComponent(jOwnersScroll, 70, 70, Integer.MAX_VALUE)
				)
		);
	}

	@Override
	public void updateData() {
		setItems(new HashSet<>(shownTypeIDs)); //Copy
		updateSaved();
	}

	@Override
	public void updateCache() { }

	@Override
	public void clearData() { }

	@Override
	public Collection<LocationType> getLocations() {
		return new ArrayList<>(); //No Location
	}

	public void addItems(Set<Integer> typeIDs) {
		Set<Integer> total = new HashSet<>();
		total.addAll(typeIDs);
		total.addAll(shownTypeIDs);
		if (invalid(total)) {
			return;
		}
		showItems(typeIDs);
	}

	public void setItems(Set<Integer> typeIDs) {
		if (invalid(typeIDs)) {
			return;
		}
		jItems.getSelectionModel().removeListSelectionListener(listener);
		clearItems();
		jItems.getSelectionModel().addListSelectionListener(listener);
		showItems(typeIDs);
	}

	private boolean invalid(Collection<Integer> typeIDs) {
		if (typeIDs.size() > MAXIMUM_SHOWN) {
			JOptionPane.showMessageDialog(program.getMainWindow().getFrame(), TabPriceHistory.get().maxItemsMsg(MAXIMUM_SHOWN), TabPriceHistory.get().title(), JOptionPane.PLAIN_MESSAGE);
			return true;
		}
		if (program.getStatusPanel().updateing(UpdateType.PRICE_HISTORY)) {
			JOptionPane.showMessageDialog(program.getMainWindow().getFrame(), TabPriceHistory.get().updatingMsg(), TabPriceHistory.get().updatingTitle(), JOptionPane.PLAIN_MESSAGE, Images.LINK_ZKILLBOARD_32.getIcon());
			return true;
		}
		return false;
	}

	private void addItem() {
		Set<Integer> total = new HashSet<>();
		total.add(Integer.MAX_VALUE); // + 1
		total.addAll(shownTypeIDs);
		if (invalid(total)) {
			return;
		}
		Item item = jAddItemDialog.show();
		if (item == null) {
			return; //Cancelled
		}
		showItems(Collections.singleton(item.getTypeID()));
	}

	private void showItems(Set<Integer> typeIDs) {
		if (program.getStatusPanel().updateing(UpdateType.PRICE_HISTORY)) {
			JOptionPane.showMessageDialog(program.getMainWindow().getFrame(), TabPriceHistory.get().updatingMsg(), TabPriceHistory.get().updatingTitle(), JOptionPane.PLAIN_MESSAGE, Images.LINK_ZKILLBOARD_32.getIcon());
			return;
		}
		switch(getPriceHistorySource()) {
			case ZKILLBOARD:
				jPriceType.setEnabled(false);
				showZKillboard(typeIDs);
				break;
			case JEVEASSETS:
				jPriceType.setEnabled(true);
				showPriceData(typeIDs);
				break;
		}
	}

	private void showPriceData(Set<Integer> typeIDs) {
		addNewItemsLocked(PriceHistoryDatabase.getPriceData(typeIDs, jPriceType.getItemAt(jPriceType.getSelectedIndex())));
	}

	private void showZKillboard(Set<Integer> typeIDs) {
		if (typeIDs == null || typeIDs.isEmpty()) {
			return;
		}
		//Items to update
		Set<Integer> update = new HashSet<>(typeIDs);
		//Items to load
		Set<Integer> show = new HashSet<>(typeIDs);
		//Up-to-date items in the database
		Set<Integer> blacklist = PriceHistoryDatabase.getZBlacklist();
		Set<Integer> current = PriceHistoryDatabase.getZKillboardUpdated();
		current.addAll(blacklist); //Do not update blacklisted items, but, do add them to the GUI
		update.removeAll(current); //Remove up-to-date items
		update.removeAll(shownTypeIDs); //Remove already in the GUI
		if (!update.isEmpty()) { //Update from zKillboard API
			setUpdating(true);
			GetItems addItem = new GetItems(update);
			addItem.execute();
		}
		show.retainAll(current); //Keep up-to-date items
		show.removeAll(shownTypeIDs);//Remove already in the GUI
		if (!show.isEmpty()) { //Load from Database
			Map<Item, Set<PriceHistoryData>> load = PriceHistoryDatabase.getZKillboard(show);
			if (!load.isEmpty()) {
				addNewItemsLocked(load);
			}
		}
	}

	private void addNewItemsLocked(Map<Item, Set<PriceHistoryData>> map) {
		jLockWindow.show(NAME, new JLockWindow.LockWorker() {
			@Override
			public void task() {
				createSeries(map);
			}

			@Override
			public void gui() {
				updateGUI(map);
				updateSeries();
			}

			@Override
			public void hidden() { }
		});
	}

	private void createSeries(Map<Item, Set<PriceHistoryData>> map) {
		if (map == null || map.isEmpty()) {
			return;
		}
		shownData.putAll(map);
		Date from = getFromDate();
		Date to = getToDate();
		for (Map.Entry<Item, Set<PriceHistoryData>> entry : map.entrySet()) {
			Item item = entry.getKey();
			//dataset
			shownOrder.add(item);
			shownTypeIDs.add(item.getTypeID());
			createSeries(from, to, item, entry.getValue());
		}
		Collections.sort(shownOrder);
	}

	private void updateGUI(Map<Item, Set<PriceHistoryData>> map) {
		if (map == null || map.isEmpty()) {
			return;
		}
		jItems.getSelectionModel().removeListSelectionListener(listener);
		//List<Item> items = new ArrayList<>(map.keySet());
		for (int index = 0; index < shownOrder.size(); index++) {
			Item item = shownOrder.get(index);
			if (!map.containsKey(item)) {
				continue;
			}
			//GUI
			itemsModel.add(index, item);
			jItems.addSelection(index, true);
		}
		jItems.getSelectionModel().addListSelectionListener(listener);
	}

	private void clearItems() {
		shownData.clear();
		shownOrder.clear();
		shownTypeIDs.clear();
		seriesMax.clear();
		series.clear();
		//Remove All
		while (dataset.getSeriesCount() != 0) {
			dataset.removeSeries(0);
		}
		itemsModel.clear(); //Must be done after clearing the dataset
	}

	private void removeItem(Item item) {
		removeItems(Collections.singletonList(item));
	}

	private void removeItems(Collection<Item> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		shownData.keySet().removeAll(items);
		shownOrder.removeAll(items);
		seriesMax.keySet().removeAll(items);
		series.keySet().removeAll(items);
		jItems.getSelectionModel().removeListSelectionListener(listener);
		for (Item item : items) {
			shownTypeIDs.remove(item.getTypeID());
			itemsModel.removeElement(item);
		}
		jItems.getSelectionModel().addListSelectionListener(listener);
		updateSeries();
	}

	private PriceHistorySource getPriceHistorySource() {
		return jSource.getItemAt(jSource.getSelectedIndex());
	}

	private void createData() {
		//dataset
		Date from = getFromDate();
		Date to = getToDate();
		for (Map.Entry<Item, Set<PriceHistoryData>> entry : shownData.entrySet()) {
			createSeries(from, to, entry.getKey(), entry.getValue());
		}
		updateSeries();
	}

	private void createSeries(Date from, Date to, Item item, Set<PriceHistoryData> set) {
		TimePeriodValues values = new TimePeriodValues(item.getTypeName());
		double max = 0;
		for (PriceHistoryData priceHistoryData : set) {
			Date date = priceHistoryData.getDate();
			if ((from != null && !date.after(from)) || (to != null && !date.before(to))) {
				continue;
			}
			double price = priceHistoryData.getPrice();
			max = Math.max(max, price);
			values.add(priceHistoryData.getTimePeriod(), price);
		}
		seriesMax.put(item, max);
		series.put(item, values);
	}

	private void updateSeries() {
		//Remove All
		while (dataset.getSeriesCount() != 0) {
			dataset.removeSeries(0);
		}
		//Add all
		for (Item item : shownOrder) {
			dataset.addSeries(series.get(item));
		}
		updateShown();
	}

	private void updateShown() {
		List<Item> selected = jItems.getSelectedValuesList();
		double max = 0;
		int count = 0;
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			Item item = shownOrder.get(i);
			boolean fromVisible = renderer.isSeriesVisible(i);
			boolean toVisible = selected.contains(item);
			if (fromVisible != toVisible) {
				renderer.setSeriesVisible(i, toVisible);
			}
			max = Math.max(max, seriesMax.get(item));
			count = Math.max(count, dataset.getItemCount(i));
		}
		if (max >     1_000_000_000_000.0) { //Higher than 1 Trillion
			rangeLinearAxis.setNumberFormatOverride(Formater.TRILLIONS_FORMAT);
		} else if (max >  1_000_000_000.0) { //Higher than 1 Billion
			rangeLinearAxis.setNumberFormatOverride(Formater.BILLIONS_FORMAT);
		} else if (max >      1_000_000.0) { //Higher than 1 Million
			rangeLinearAxis.setNumberFormatOverride(Formater.MILLIONS_FORMAT);
		} else {
			rangeLinearAxis.setNumberFormatOverride(Formater.LONG_FORMAT); //Default
		}
		renderer.setDefaultShapesVisible(count < 2);
		jRemove.setEnabled(!selected.isEmpty());
		jClear.setEnabled(!itemsModel.isEmpty());
		jSave.setEnabled(!itemsModel.isEmpty());
	}

	public void updateSaved() {
		List<String> sets = new ArrayList<>(Settings.get().getPriceHistorySets().keySet());
		jLoad.removeAll();
		if (sets.isEmpty()) {
			jLoad.setEnabled(false);
			return;
		}
		jLoad.setEnabled(true);

		jLoad.add(jManage);

		jLoad.addSeparator();

		Collections.sort(sets, new CaseInsensitiveComparator());
		for (String name : sets) {
			JMenuItem jMenuItem = new JMenuItem(name, Images.FILTER_LOAD.getIcon());
			jMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
						addItems(Settings.get().getPriceHistorySets().get(name));
					} else {
						setItems(Settings.get().getPriceHistorySets().get(name));
					}
				}
			});
			jLoad.add(jMenuItem);
		}
	}

	private void setUpdating(boolean updating) {
		jSource.setEnabled(!updating);
		jEdit.setEnabled(!updating);
		if (updating) {
			jSave.setEnabled(false);
			jLoad.setEnabled(false);
		} else {
			jSave.setEnabled(!itemsModel.isEmpty());
			jLoad.setEnabled(!Settings.get().getPriceHistorySets().isEmpty());
		}

	}

	private LocalDate dateToLocalDate(Date date) {
		Instant instant = date.toInstant();
		return LocalDateTime.ofInstant(instant, ZoneId.of("GMT")).toLocalDate();
	}

	private Date getFromDate() {
		LocalDate date = jFrom.getDate();
		if (date == null) {
			return null;
		}
		Instant instant = date.atStartOfDay().atZone(ZoneId.of("GMT")).toInstant(); //Start of day - GMT
		return Date.from(instant);
	}

	private Date getToDate() {
		LocalDate date = jTo.getDate();
		if (date == null) {
			return null;
		}
		Instant instant = date.atTime(23, 59, 59).atZone(ZoneId.of("GMT")).toInstant(); //End of day - GMT
		return Date.from(instant);
	}

	private class ListenerClass implements ActionListener, MouseListener, DateChangeListener, ListSelectionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (PriceHistoryAction.QUICK_DATE.name().equals(e.getActionCommand())) {
				QuickDate quickDate = (QuickDate) jQuickDate.getSelectedItem();
				if (quickDate == QuickDate.RESET) {
					jTo.setDate(null);
					jFrom.setDate(null);
				} else {
					Date toDate = getToDate();
					if (toDate == null) {
						toDate = new Date(); //now
					}
					Date fromDate = quickDate.apply(toDate);
					if (fromDate != null) {
						jFrom.setDate(dateToLocalDate(fromDate));
					}
				}
			} else if (PriceHistoryAction.SOURCE.name().equals(e.getActionCommand())) {
				updateData();
			} else if (PriceHistoryAction.ADD_ITEM.name().equals(e.getActionCommand())) {
				addItem();
			} else if (PriceHistoryAction.CLEAR_ITEMS.name().equals(e.getActionCommand())) {
				clearItems();
				updateShown();
			} else if (PriceHistoryAction.REMOVE_ITEMS.name().equals(e.getActionCommand())) {
				removeItems(jItems.getSelectedValuesList());
			} else if (PriceHistoryAction.SAVE.name().equals(e.getActionCommand())) {
				jSaveItemsDialog.updateData(Settings.get().getPriceHistorySets().keySet());
				String name = jSaveItemsDialog.show();
				if (name == null) {
					return;
				}
				Settings.lock("Price History (Save Set)");
				Settings.get().getPriceHistorySets().put(name, new HashSet<>(shownTypeIDs)); //Copy
				Settings.unlock("Price History (Save Set)");
				program.saveSettings("Price History (Save Set)");
				updateSaved();
			} else if (PriceHistoryAction.MANAGE.name().equals(e.getActionCommand())) {
				jManageItemsDialog.setVisible(true);
			} else if (PriceHistoryAction.INCLUDE_ZERO.name().equals(e.getActionCommand())) {
				rangeLogarithmicAxis.setAutoRangeIncludesZero(jIncludeZero.isSelected());
				rangeLinearAxis.setAutoRangeIncludesZero(jIncludeZero.isSelected());
			} else if (PriceHistoryAction.LOGARITHMIC.name().equals(e.getActionCommand())) {
				if (jLogarithmic.isSelected()) {
					jNextChart.getXYPlot().setRangeAxis(rangeLogarithmicAxis);
				} else {
					jNextChart.getXYPlot().setRangeAxis(rangeLinearAxis);
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) { }

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON2) { //Middle mouse button
				return;
			}
			int index = jItems.locationToIndex(e.getPoint());
			if (index < 0 || index > itemsModel.size()) {
				return;
			}
			removeItem(itemsModel.get(index));
		}

		@Override
		public void mouseReleased(MouseEvent e) { }

		@Override
		public void mouseEntered(MouseEvent e) { }

		@Override
		public void mouseExited(MouseEvent e) { }

		@Override
		public void dateChanged(DateChangeEvent event) {
			Date from = getFromDate();
			Date to = getToDate();
			QuickDate quickDate = (QuickDate) jQuickDate.getSelectedItem();
			if (!quickDate.isValid(from, to)) {
				QuickDate selected = quickDate.getSelected(from, to);
				jQuickDate.setSelectedItem(selected);
			}
			if (from != null && to != null && from.after(to)) {
				jTo.setDate(dateToLocalDate(from));
			}
			createData();
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) {
				return;
			}
			updateShown();
		}
	}

	private class GetItems extends SwingWorker<Map<Item, Set<PriceHistoryData>>, Void> {

		private final Map<Item, Set<PriceHistoryData>> data = new TreeMap<>();
		private final Set<Integer> blacklist = new HashSet<>();
		private final Progress progress;
		private final Set<Integer> typeIDs;

		public GetItems(Set<Integer> typeIDs) {
			this.typeIDs = typeIDs;
			if (typeIDs.size() > 1) {
				progress = program.getStatusPanel().addProgress(UpdateType.PRICE_HISTORY, new StatusPanel.ProgressControl() {
					@Override
					public boolean isAuto() {
						return true;
					}
					@Override
					public void show() { }

					@Override
					public void cancel() { }

					@Override
					public void setPause(boolean pause) { }
				});
				progress.setVisible(true);
			} else {
				progress = null;
			}
		}

		@Override
		protected Map<Item, Set<PriceHistoryData>> doInBackground() throws Exception {
			int done = 0;
			int total = typeIDs.size();
			for (Integer typeID : typeIDs) {
				if (typeID == null) {
					done++;
					updateProgress(done, total);
					continue;
				}
				Item item = ApiIdConverter.getItem(typeID);
				//Always Add Item
				Set<PriceHistoryData> output = new TreeSet<>();
				data.put(item, output);
				//Update Item
				Map<String, Double> input = ZkillboardPricesHistoryGetter.getPriceHistory(typeID);
				//Verify Data
				if (input == null || input.isEmpty() || !input.containsKey(PriceHistoryDatabase.getZKillboardDate())) {
					blacklist.add(typeID);
					done++;
					updateProgress(done, total);
					continue;
				}
				//Add Data
				for (Map.Entry<String, Double> entry : input.entrySet()) {
					try {
						PriceHistoryData priceHistoryData = new PriceHistoryData(typeID, item, entry.getKey(), entry.getValue());
						output.add(priceHistoryData);
					} catch (ParseException ex) {
						//Ignore data
					}
				}
				done++;
				updateProgress(done, total);
			}
			return data;
		}

		private void updateProgress(double done, double total) {
			if (progress == null) {
				return;
			}
			int progressDone = (int)(done / total * 100.0);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					progress.setValue(progressDone);
				}
			});
		}

		@Override
		protected void done() {
			if (progress != null) {
				program.getStatusPanel().removeProgress(progress);
			}
			PriceHistoryDatabase.setZBlacklist(blacklist);
			PriceHistoryDatabase.setZKillboard(data); //Save to Database
			addNewItemsLocked(data);
			setUpdating(false);
			try {
				get();
			} catch (InterruptedException ex) {
				LOG.error(ex.getMessage(), ex);
			} catch (ExecutionException ex) {
				LOG.error(ex.getMessage(), ex);
			}
		}
	}

	public static class PriceHistoryData implements Comparable<PriceHistoryData> {

		private final int typeID;
		private final Item item;
		private final String dateString;
		private final double price;
		private final Date date;
		private final SimpleTimePeriod simpleTimePeriod;

		public PriceHistoryData(int typeID, Item item, String dateString, double price) throws ParseException {
			this.typeID = typeID;
			this.item = item;
			this.dateString = dateString;
			this.price = price;
			this.date = PriceHistoryDatabase.DATE.parse(dateString);
			this.simpleTimePeriod = new SimpleTimePeriod(date, date);
		}

		public int getTypeID() {
			return typeID;
		}

		public Item getItem() {
			return item;
		}

		public String getDateString() {
			return dateString;
		}

		public double getPrice() {
			return price;
		}

		public Date getDate() {
			return date;
		}

		public SimpleTimePeriod getTimePeriod() {
			return simpleTimePeriod;
		}

		@Override
		public int compareTo(PriceHistoryData o) {
			int compare = item.getTypeName().compareTo(o.item.getTypeName());
			if (compare != 0) {
				return compare;
			}
			return dateString.compareTo(o.dateString);
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 89 * hash + this.typeID;
			hash = 89 * hash + Objects.hashCode(this.dateString);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final PriceHistoryData other = (PriceHistoryData) obj;
			if (this.typeID != other.typeID) {
				return false;
			}
			if (!Objects.equals(this.dateString, other.dateString)) {
				return false;
			}
			return true;
		}
	}

	private class MyRender extends XYLineAndShapeRenderer {

		public MyRender() {
			super(true, false);
		}

		@Override
		public LegendItem getLegendItem(int datasetIndex, int series) {
			LegendItem original = super.getLegendItem(datasetIndex, series); //To change body of generated methods, choose Tools | Templates.
			LegendItem fixed = new LegendItem(
				original.getLabel(),
				original.getDescription(),
				original.getToolTipText(),
				"", //urlText
				LEGEND,
				original.getLinePaint(), //set fill paint to line paint
				original.getOutlineStroke(),
				Color.BLACK
			);
			fixed.setSeriesIndex(series);
			fixed.setDatasetIndex(datasetIndex);
			return fixed;
		}

		@Override
		public Shape getItemShape(int row, int column) {
			return ITEM_SHAPE;
		}
	}
}
