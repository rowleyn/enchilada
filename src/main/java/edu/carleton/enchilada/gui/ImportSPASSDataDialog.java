/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is EDAM Enchilada's ClusterDialog class.
 *
 * The Initial Developer of the Original Code is
 * The EDAM Project at Carleton College.
 * Portions created by the Initial Developer are Copyright (C) 2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Ben J Anderson andersbe@gmail.com
 * David R Musicant dmusican@carleton.edu
 * Anna Ritz ritza@carleton.edu
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package edu.carleton.enchilada.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import edu.carleton.enchilada.dataImporters.SPASSDataSetImporter;
import edu.carleton.enchilada.database.InfoWarehouse;
import edu.carleton.enchilada.errorframework.*;
import edu.carleton.enchilada.externalswing.SwingWorker;
/**
 * Standard importation dialog for SPASS data files.
 * 
 * @author rzeszotj with credit to other dialog authors
 *
 */
public class ImportSPASSDataDialog extends JDialog implements ActionListener{

	private JButton okButton;
	private JButton cancelButton;
	private JCheckBox parentButton;
	private JLabel parentLabel;
	private SPASSTableModel spassTableModel;
	private JProgressBar progressBar;
	private int dataSetCount;
	private static JFrame parent = null;
	private boolean importedTogether = false;
	private int parentID = 0; //default parent collection is root
	private InfoWarehouse db;
	
	/**
	 * Extends JDialog to form a modal dialogue box for importing 
	 * SPASS.txt files.  
	 * @param owner The parent frame of this dialog box, should be the 
	 * main frame.  This frame will become inactive while ImportPars is
	 * active.  
	 * @throws java.awt.HeadlessException From the constructor of 
	 * JDialog.  
	 */
	public ImportSPASSDataDialog(JFrame owner, InfoWarehouse db) throws HeadlessException {
		// calls the constructor of the superclass (JDialog), sets the title and makes the
		// dialog modal.  
		super(owner, "Import SPASS Datasets as Collections", true);
		parent = owner;
		this.db = db;
		setSize(1000,600);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JTable spassTable = getSPASSTable();
		JScrollPane scrollPane = new JScrollPane(spassTable);
		
		okButton = new JButton("OK");
		okButton.setMnemonic(KeyEvent.VK_O);
		okButton.addActionListener(this);
		
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(this);
		
		//an option to import all the datasets into one parent collection
		parentButton = new JCheckBox(
				"Create a parent collection for all incoming datasets.",
				false);
		parentButton.setMnemonic(KeyEvent.VK_P);
		parentButton.addActionListener(this);
		
		parentLabel = new JLabel();
		
		scrollPane.setPreferredSize(new Dimension(795,500));
		
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
		JLabel label = new JLabel("Choose Datasets to Convert");
		label.setLabelFor(spassTable);
		listPane.add(label);
		listPane.add(Box.createRigidArea(new Dimension(0,5)));
		listPane.add(scrollPane);
		listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));
		
		buttonPane.add(parentButton);
		buttonPane.add(parentLabel);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPane.add(cancelButton);
				
		add(listPane, BorderLayout.CENTER);
		add(buttonPane, BorderLayout.SOUTH);
		
		setVisible(true);	
	}
	
    private JTable getSPASSTable()
	{
		spassTableModel = new SPASSTableModel();
		JTable pTable = new JTable(spassTableModel);		
		TableColumn[] tableColumns = new TableColumn[2];
		for (int i = 0; i < 1; i++)
			tableColumns[i] = pTable.getColumnModel().getColumn(i+1);
		tableColumns[0].setCellEditor(
				new FileDialogPickerEditor("txt","Dataset",this));
		tableColumns[0].setPreferredWidth(750);
	
		TableColumn numColumn = pTable.getColumnModel().getColumn(0);
		numColumn.setPreferredWidth(10);
		
		return pTable;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if (source == okButton) {
			//set necessary values in the data importer
			final ProgressBarWrapper progressBar = 
				new ProgressBarWrapper(parent, SPASSDataSetImporter.TITLE, 100);			
			final SPASSDataSetImporter spass = 
					new SPASSDataSetImporter(spassTableModel, parent, db, progressBar);
			// If a .txt file is missing, don't start the process.
			try {
				spass.errorCheck();
			} catch (DisplayException e1) {
				ErrorLogger.displayException(this,e1.toString());
				return;
			}
			
			//for collections imported into a parent collection
			if (importedTogether)
				spass.setParentID(parentID);
			
			//Create the progress bar and spin off a thread to do the work in
			//Database transactions are not currently used.
			progressBar.constructThis();
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					try {
						spass.collectTableInfo();
					}
					catch (DisplayException e1) {
						ErrorLogger.displayException(progressBar,e1.toString());
					}
					catch (WriteException e2) {
						ErrorLogger.displayException(progressBar,e2.toString());
					}
					return null;
				}
				public void finished() {
					progressBar.disposeThis();
					dispose();
				}
			};
			worker.start();
		}
		else if (source == parentButton){
			if (parentButton.isSelected())
			{
				//pop up a "create new collections" dialog box & keep number of new
				//collection
				EmptyCollectionDialog ecd = 
					new EmptyCollectionDialog((JFrame)parent, "ATOFMS", false, db);
				parentID = ecd.getCollectionID();
				
				if (parentID == -1) {
					parentButton.setSelected(false);
				} else {
					parentLabel.setText("Importing into collection " + ecd.getCollectionName());
					importedTogether = true;
				}
			}
			else {
				if(!EmptyCollectionDialog.removeEmptyCollection(db, parentID))
					System.err.println("Error deleting temporary collection");
				parentLabel.setText("");
				importedTogether = false;				
			}
		}
		else if (source == cancelButton) {
			if (importedTogether) {
				if(!EmptyCollectionDialog.removeEmptyCollection(db, parentID))
					System.err.println("Error deleting temporary collection");
				parentLabel.setText("");
				importedTogether = false;				
			}
			dispose();
		}
			
	}
	
	/**
	 * Method to determine whether the datasets are being imported together into
	 * a single parent collection.
	 * 
	 * @return True if datasets are being imported together.
	 */
	public boolean parentExists(){
		return importedTogether;
	}
	
	/**
	 * Accessor method to obtain the parent collection's ID for the datasets being
	 * imported.
	 * 
	 * @return	parentID
	 */
	public int getParentID(){
		return parentID;
	}
}
