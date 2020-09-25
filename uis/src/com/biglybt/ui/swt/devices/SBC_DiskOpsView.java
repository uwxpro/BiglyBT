/* 
 * Copyright (C) Bigly Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package com.biglybt.ui.swt.devices;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.biglybt.core.Core;
import com.biglybt.core.CoreFactory;
import com.biglybt.core.CoreOperation;
import com.biglybt.core.CoreOperationListener;
import com.biglybt.core.CoreRunningListener;
import com.biglybt.ui.common.ToolBarItem;
import com.biglybt.ui.common.table.*;
import com.biglybt.ui.common.table.impl.TableColumnManager;
import com.biglybt.ui.common.updater.UIUpdatable;
import com.biglybt.ui.selectedcontent.SelectedContent;
import com.biglybt.ui.selectedcontent.SelectedContentManager;
import com.biglybt.ui.swt.*;
import com.biglybt.ui.swt.devices.columns.*;
import com.biglybt.ui.swt.mdi.MdiEntrySWT;
import com.biglybt.ui.swt.skin.SWTSkinObject;
import com.biglybt.ui.swt.views.skin.SkinView;
import com.biglybt.ui.swt.views.table.TableViewSWT;
import com.biglybt.ui.swt.views.table.TableViewSWTMenuFillListener;
import com.biglybt.ui.swt.views.table.impl.TableViewFactory;


import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIPluginViewToolBarListener;
import com.biglybt.pif.ui.tables.TableColumn;
import com.biglybt.pif.ui.tables.TableColumnCreationListener;
import com.biglybt.pif.ui.tables.TableManager;
import com.biglybt.pifimpl.local.PluginInitializer;


public class SBC_DiskOpsView
	extends SkinView
	implements  UIUpdatable, UIPluginViewToolBarListener, CoreOperationListener
{
	public static final String TABLE_DISK_OPS = "DiskOps";

	private static boolean columnsAdded = false;

	private static Core		core = CoreFactory.getSingleton();

	private TableViewSWT<CoreOperation> tvDiskOps;

	// private MdiEntrySWT mdiEntry;

	private Composite tableParent;


	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		CoreFactory.addCoreRunningListener(new CoreRunningListener() {
			@Override
			public void coreRunning(Core core) {
				initColumns(core);
			}
		});


		return null;
	}

	private void 
	initColumns(
		Core core) 
	{
		if ( columnsAdded ){
			return;
		}
		
		columnsAdded = true;
		
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		
		TableManager tableManager = uiManager.getTableManager();
		
		tableManager.registerColumn(CoreOperation.class, ColumnFO_Type.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnFO_Type(column);
					}
				});
		
		tableManager.registerColumn(CoreOperation.class, ColumnFO_Name.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnFO_Name(column);
					}
				});
		
		tableManager.registerColumn(CoreOperation.class, ColumnFO_Size.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnFO_Size(column);
					}
				});
		
		tableManager.registerColumn(CoreOperation.class, ColumnFO_Progress.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnFO_Progress(column);
					}
				});

		TableColumnManager tcm = TableColumnManager.getInstance();
		
		String[] defaultLibraryColumns = {
				ColumnFO_Type.COLUMN_ID,
				ColumnFO_Name.COLUMN_ID,
				ColumnFO_Size.COLUMN_ID,
				ColumnFO_Progress.COLUMN_ID,
		};
		
		tcm.setDefaultColumnNames( TABLE_DISK_OPS, defaultLibraryColumns );
	}

	@Override
	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		SWTSkinObject soDeviceList = getSkinObject("disk-ops");
		
		if ( soDeviceList != null ){
			
			initTable((Composite) soDeviceList.getControl());
		}

		core.addOperationListener( this );

		updateSelectedContent();

		return null;
	}

	@Override
	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		core.removeOperationListener( this );

		synchronized( this ){
			
			if ( tvDiskOps != null ){
				
				tvDiskOps.delete();
				
				tvDiskOps = null;
			}
		}

		Utils.disposeSWTObjects( tableParent );
		
		return( super.skinObjectHidden(skinObject, params));
	}

	private void 
	initTable(
		Composite control) 
	{

		tvDiskOps = TableViewFactory.createTableViewSWT(
						CoreOperation.class, 
						TABLE_DISK_OPS,
						TABLE_DISK_OPS, 
						new TableColumnCore[0], 
						ColumnFO_Type.COLUMN_ID, 
						SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		
		// tvDiskOps.setRowDefaultHeightEM(1.5f);
		tvDiskOps.setHeaderVisible( true );

		tableParent = new Composite(control, SWT.NONE);
		tableParent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		tableParent.setLayout(layout);

		tvDiskOps.addSelectionListener(new TableSelectionListener() {

			@Override
			public void selected(TableRowCore[] row) {
				updateSelectedContent();
			}

			@Override
			public void mouseExit(TableRowCore row) {
			}

			@Override
			public void mouseEnter(TableRowCore row) {
			}

			@Override
			public void focusChanged(TableRowCore focus) {
			}

			@Override
			public void deselected(TableRowCore[] rows) {
				updateSelectedContent();
			}

			@Override
			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);

		tvDiskOps.addLifeCycleListener(new TableLifeCycleListener() {
			@Override
			public void tableLifeCycleEventOccurred(TableView tv, int eventType, Map<String, Object> data) {
				switch (eventType) {
					case EVENT_TABLELIFECYCLE_INITIALIZED:
					
						for ( CoreOperation op: core.getOperations()){
							
							if ( ourOperation(op)){
						
								tvDiskOps.addDataSource( op );
							}
						}
						
						updateSelectedContent();
						
						break;
				}
			}
		});

		tvDiskOps.addMenuFillListener(new TableViewSWTMenuFillListener() {
			@Override
			public void fillMenu(String sColumnName, Menu menu) {
				SBC_DiskOpsView.this.fillMenu(menu);
			}

			@Override
			public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
			}
		});

		tvDiskOps.addKeyListener(
			new KeyListener()
			{
				@Override
				public void
				keyPressed(
					KeyEvent e )
				{
					if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){

						
						e.doit = false;
					}
				}

				@Override
				public void
				keyReleased(
					KeyEvent arg0 )
				{
				}
			});

		tvDiskOps.initialize(tableParent);

		control.layout(true, true);
	}

	protected void 
	fillMenu(
		Menu menu ) 
	{


	}
	
	private boolean
	ourOperation(
		CoreOperation op )
	{
		int type = op.getOperationType();
		
		return(	type == CoreOperation.OP_DOWNLOAD_ALLOCATION || 
				type == CoreOperation.OP_DOWNLOAD_CHECKING || 
				type == CoreOperation.OP_DOWNLOAD_EXPORT || 
				type == CoreOperation.OP_FILE_MOVE ); 
	}

	public boolean
	operationExecuteRequest(
		CoreOperation 		operation )
	{
		return( false );
	}
	
	public void
	operationAdded(
		CoreOperation		operation )
	{
		if ( !ourOperation( operation )){
			
			return;
		}
		
		synchronized( this ){
			
			if ( tvDiskOps == null ){
				
				return;
			}
			
			tvDiskOps.addDataSource( operation );
		}		
	}
	
	public void
	operationRemoved(
		CoreOperation		operation )
	{
		if ( !ourOperation( operation )){
			
			return;
		}
		
		synchronized( this ){
			
			if ( tvDiskOps == null ){
				
				return;
			}
			
			tvDiskOps.removeDataSource( operation );
		}
	}

	@Override
	public void 
	refreshToolBarItems(
		Map<String, Long> list) 
	{
	}

	@Override
	public boolean 
	toolBarItemActivated(
		ToolBarItem item, 
		long 		activationType,
	    Object 		datasource ) 
	{


		return false;
	}

	@Override
	public String 
	getUpdateUIName() 
	{
		return "DiskOPs";
	}

	@Override
	public	void 
	updateUI()
	{
		if ( tvDiskOps != null ){
			
			tvDiskOps.refreshTable(false);
		}
	}
	
	public void 
	updateSelectedContent() 
	{	
		Object[] dataSources = tvDiskOps.getSelectedDataSources(true);
		
		List<SelectedContent> listSelected = new ArrayList<>(dataSources.length);
		
		for (Object ds : dataSources) {
				
			listSelected.add( new SelectedContent( ((CoreOperation)ds).getTask().getName()));
		}
		
		SelectedContent[] sc = listSelected.toArray(new SelectedContent[0]);
		
		SelectedContentManager.changeCurrentlySelectedContent(tvDiskOps.getTableID(), sc, tvDiskOps);
	}
}