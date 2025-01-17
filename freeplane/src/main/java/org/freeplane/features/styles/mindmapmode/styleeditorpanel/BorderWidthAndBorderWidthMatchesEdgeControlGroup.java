/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2016 jberry
 *
 *  This file author is jberry
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.styles.mindmapmode.styleeditorpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.resources.components.BooleanProperty;
import org.freeplane.core.resources.components.IPropertyControl;
import org.freeplane.core.resources.components.RevertingProperty;
import org.freeplane.core.resources.components.QuantityProperty;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.nodestyle.NodeBorderModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;

import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * @author Joe Berry
 * Dec 1, 2016
 */
public class BorderWidthAndBorderWidthMatchesEdgeControlGroup implements ControlGroup {
	private static final String BORDER_WIDTH_MATCHES_EDGE_WIDTH = "border_width_matches_edge_width";
	private static final String BORDER_WIDTH = "border_width";
	
	private RevertingProperty mSetBorderWidthMatchesEdgeWidth;
	private BooleanProperty mBorderWidthMatchesEdgeWidth;

	private RevertingProperty mSetBorderWidth;
	private QuantityProperty<LengthUnit> mBorderWidth;

	private BorderWidthMatchesEdgeWidthListener borderWidthMatchesEdgeChangeListener;
	private BorderWidthListener borderWidthListener;
	private boolean canEdit;
	
	private class BorderWidthMatchesEdgeWidthListener extends ControlGroupChangeListener {
		public BorderWidthMatchesEdgeWidthListener(final RevertingProperty mSet,final IPropertyControl mProperty) {
			super(mSet, mProperty);
		}

		@Override
		void applyValue(final boolean enabled, final NodeModel node, final PropertyChangeEvent evt) {
			final MNodeStyleController styleController = (MNodeStyleController) Controller
			.getCurrentModeController().getExtension(NodeStyleController.class);
			styleController.setBorderWidthMatchesEdgeWidth(node, enabled ? mBorderWidthMatchesEdgeWidth.getBooleanValue(): null);
		}

		@Override
		void setStyleOnExternalChange(NodeModel node) {
			final NodeStyleController styleController = NodeStyleController.getController();
			final NodeBorderModel nodeBorderModel = NodeBorderModel.getModel(node);
			final Boolean match = nodeBorderModel != null ? nodeBorderModel.getBorderWidthMatchesEdgeWidth() : null;
			final Boolean viewMatch = styleController.getBorderWidthMatchesEdgeWidth(node);
			mSetBorderWidthMatchesEdgeWidth.setValue(match != null);
			mBorderWidthMatchesEdgeWidth.setValue(viewMatch);
		}
	}
	
	private class BorderWidthListener extends ControlGroupChangeListener {
		public BorderWidthListener(final RevertingProperty mSet,final IPropertyControl mProperty) {
			super(mSet, mProperty);
		}

		@Override
		void applyValue(final boolean enabled, final NodeModel node, final PropertyChangeEvent evt) {
			final MNodeStyleController styleController = (MNodeStyleController) Controller
			.getCurrentModeController().getExtension(NodeStyleController.class);
			styleController.setBorderWidth(node, enabled ? mBorderWidth.getQuantifiedValue(): null);
		}

		@Override
		void setStyleOnExternalChange(NodeModel node) {
			final NodeStyleController styleController = NodeStyleController.getController();
			final NodeBorderModel nodeBorderModel = NodeBorderModel.getModel(node);
			final Quantity<LengthUnit> width = nodeBorderModel != null ? nodeBorderModel.getBorderWidth() : null;
			final Quantity<LengthUnit> viewWidth = styleController.getBorderWidth(node);
			mSetBorderWidth.setValue(width != null);
			mBorderWidth.setQuantifiedValue(viewWidth);
			enableOrDisableBorderWidthControls();
		}
	}
	
	@Override
	public void addControlGroup(DefaultFormBuilder formBuilder) {
		addBorderWidthControl(formBuilder);
		addBorderWidthMatchesEdgeWidthControl(formBuilder);
	}
	
	private void addBorderWidthControl(DefaultFormBuilder formBuilder) {
		mSetBorderWidth = new RevertingProperty();
		mBorderWidth = new QuantityProperty<LengthUnit>(BORDER_WIDTH, 0, 100000, 0.1, LengthUnit.px);
		borderWidthListener = new BorderWidthListener(mSetBorderWidth, mBorderWidth);
		mSetBorderWidth.addPropertyChangeListener(borderWidthListener);
		mBorderWidth.addPropertyChangeListener(borderWidthListener);
		mBorderWidth.appendToForm(formBuilder);
		mSetBorderWidth.appendToForm(formBuilder);
	}
	
	public void addBorderWidthMatchesEdgeWidthControl(DefaultFormBuilder formBuilder) {
		mSetBorderWidthMatchesEdgeWidth = new RevertingProperty();
		mBorderWidthMatchesEdgeWidth = new BooleanProperty(BORDER_WIDTH_MATCHES_EDGE_WIDTH);
		borderWidthMatchesEdgeChangeListener = new BorderWidthMatchesEdgeWidthListener(mSetBorderWidthMatchesEdgeWidth, mBorderWidthMatchesEdgeWidth);
		mSetBorderWidthMatchesEdgeWidth.addPropertyChangeListener(borderWidthMatchesEdgeChangeListener);
		mBorderWidthMatchesEdgeWidth.addPropertyChangeListener(borderWidthMatchesEdgeChangeListener);
		
		mBorderWidthMatchesEdgeWidth.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				enableOrDisableBorderWidthControls();
			}
		});
		mBorderWidthMatchesEdgeWidth.appendToForm(formBuilder);
		mSetBorderWidthMatchesEdgeWidth.appendToForm(formBuilder);
	}

	public void enableOrDisableBorderWidthControls() {
		final boolean borderWidthCanBeSet = ! mBorderWidthMatchesEdgeWidth.getBooleanValue();
		mSetBorderWidth.setEnabled(borderWidthCanBeSet && canEdit);
		mBorderWidth.setEnabled(borderWidthCanBeSet && canEdit);
	}

	@Override
	public void setStyle(NodeModel node, boolean canEdit) {
		this.canEdit = canEdit;
		borderWidthListener.setStyle(node);
		borderWidthMatchesEdgeChangeListener.setStyle(node);
	}

}
