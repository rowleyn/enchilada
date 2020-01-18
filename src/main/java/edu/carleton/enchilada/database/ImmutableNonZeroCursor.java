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
 * The Original Code is EDAM Enchilada's NonZeroCursor class.
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


/*
 * Created on Dec 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.carleton.enchilada.database;

import java.util.ArrayList;

import edu.carleton.enchilada.ATOFMS.ParticleInfo;
import edu.carleton.enchilada.ATOFMS.Peak;
import edu.carleton.enchilada.analysis.BinnedPeakList;
import edu.carleton.enchilada.analysis.clustering.PeakList;
import edu.carleton.enchilada.atom.ATOFMSAtomFromDB;
import edu.carleton.enchilada.atom.GeneralAtomFromDB;

/*
 * Wraps around another binned cursor, and automatically filters out
 * atoms with no peaks. Also keeps track of how many such atoms there
 * are.
 * 
 *  Modified NonZeroCursor so that only *copies* of the particles in the
 *  result set are returned.
 *  Michael Murphy 2014
 *  
 */
public class ImmutableNonZeroCursor extends NonZeroCursor implements CollectionCursor {

    public ImmutableNonZeroCursor(CollectionCursor wrappee) {
    	super(wrappee);
    }
    
    @Override
    public ParticleInfo getCurrent() {
    	// currently only the BPL is actually deep copied, because operations are only done on that
    	ParticleInfo copy = new ParticleInfo();
    	
    	BinnedPeakList bpl = new BinnedPeakList();
    	bpl.copyBinnedPeakList(particleInfo.getBinnedList());
    	copy.setBinnedList(bpl);
    	
    	copy.setPeakList(particleInfo.getPeakList());
    	copy.setParticleInfo(particleInfo.getParticleInfo());
    	copy.setParticleInfo(particleInfo.getATOFMSParticleInfo());
    	
        return copy;
    }

    @Override
	public BinnedPeakList getPeakListfromAtomID(int atomID) {
		BinnedPeakList copy = new BinnedPeakList();
		copy.copyBinnedPeakList(cursor.getPeakListfromAtomID(atomID));
	    return copy;
	}

}
