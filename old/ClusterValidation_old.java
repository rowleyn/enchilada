package analysis.clustering;

// Cluster validation (Dunn index, silhouettes)
// Michael Murphy 2014, University of Toronto
// TODO: think of ways to speed this up (PCA?)

// this needs serious refactoring, redunancy abounds

// there are many ways this could be sped up, this is pretty bad. use symmetry property, xpose. maybe clear out inter matrices that won't be used again
// the memory usage of this thing is INSANE, unacceptable

// can reduce the number of pairwise cluster comparisons, storage too?
// compute the centroids for all clusters, the mean point-centroid distance for all clusters, and the spacing between all centroids
// for a given pair of clusters, their average pairwise distance is BOUNDED ABOVE by the sum of the radii and the inter-centroid spacing
// is an upper bound of any use whatsoever...?

// DON'T STORE THE MATRICES ONCE DONE! STORE THE MEAN DISTANCE!!! all these matrices are unnecessary

import java.sql.SQLException;
import java.util.*;

import collection.Collection;
import ATOFMS.ParticleInfo;
import analysis.BinnedPeakList;
import analysis.DistanceMetric;
import database.InfoWarehouse;
import database.NonZeroCursor;

public class ClusterValidation {
	public static void run(InfoWarehouse db, Collection parent, DistanceMetric dMetric, boolean posNegNorm, ArrayList<Integer> excludeClusters) {		
		System.out.println();
		System.out.println("Calculating Dunn and Silhouette indices...");
		System.out.println();
		
		long startTime = System.nanoTime();
		
		// retrieve cluster collection IDs
		ArrayList<Integer> childrenIds = parent.getSubCollectionIDs();
		
		// remove excluded clusters
		int cid;
		for (int i = 0; i < childrenIds.size(); i++) {
			cid = Integer.parseInt(db.getCollectionName(childrenIds.get(i)));
			if (excludeClusters.contains(cid)) {
				childrenIds.remove(i);
				i--;
			}
		}
		
		int numClusters = childrenIds.size();
		
		float[] silhouettes = new float[numClusters];

		ArrayList<BinnedPeakList> outerCluster, innerCluster;
		NonZeroCursor outerCurs, innerCurs;
		int outerId, innerId, clusterName;
		SymmetricMatrix intraDistances;
		HashMap<Integer,float[][]> interDistances = new HashMap<Integer, float[][]>();
		ParticleInfo p, q;
		
		float intraDist, interDist;
		float maxIntraDist = -1;
		float minInterDist = Float.MAX_VALUE;
		
		// iterate through cluster collections
		for (int i = 0; i < numClusters; i++) {
			outerId = childrenIds.get(i);

			System.out.println("Processing cluster "+db.getCollectionName(outerId)+"... ");
			
			// load outer cluster into memory
			outerCluster = new ArrayList<BinnedPeakList>();
			try {
				outerCurs = new NonZeroCursor(db.getBPLOnlyCursor(db.getCollection(outerId)));
			} catch (SQLException e) {
				return;
			}
			while (outerCurs.next()) {
				p = outerCurs.getCurrent();
				if (posNegNorm)
					p.getBinnedList().posNegNormalize(dMetric);
				else
					p.getBinnedList().normalize(dMetric);
				outerCluster.add(p.getBinnedList());
			}
			outerCurs.close();
			
			intraDistances = distanceMatrix(outerCluster, dMetric);
			
			// load inner cluster into memory
			for (int j = i+1; j < numClusters; j++) {
				innerId = childrenIds.get(j);
				innerCluster = new ArrayList<BinnedPeakList>();
				try {
					innerCurs = new NonZeroCursor(db.getBPLOnlyCursor(db.getCollection(innerId)));
				} catch (SQLException e) {
					return;
				}
				while (innerCurs.next()) {
					q = innerCurs.getCurrent();
					if (posNegNorm)
						q.getBinnedList().posNegNormalize(dMetric);
					else
						q.getBinnedList().normalize(dMetric);
					innerCluster.add(q.getBinnedList());
				}
				innerCurs.close();
				interDistances.put(i*numClusters+j, distanceMatrix(outerCluster, innerCluster, dMetric)); // reduce 
			}
			
			// compute pointwise silhouette index
			float a, b, s, temp;
			float[] array;
			int key;
			s = 0;
			
			for (int k = 0; k < outerCluster.size(); k++) {
				// get mean intracluster distance for this point - bias for diagonal zeros
				a = arrayMean(intraDistances[k], 1);
				if (a < 0)
					System.out.println("a "+a);
				
				// get max intracluster distance
				intraDist = arrayMax(intraDistances[k]);
				if (intraDist > maxIntraDist)
					maxIntraDist = intraDist;
				
				b = Float.MAX_VALUE;
				for (int j = 0; j < numClusters; j++) {					
					if (i == j) {
						continue;
					} else if (i < j) {
						array = interDistances.get(i*numClusters+j)[k];
					} else {
						array = arrayCol(interDistances.get(j*numClusters+i),k);
					}
					
					// get min of mean intercluster distances for this point
					temp = arrayMean(array);
					if (temp < b)
						b = temp;
					
					interDist = arrayMin(array); // save THIS value
					if (interDist < minInterDist)
						minInterDist = interDist;
				}
				if (b < 0)
					System.out.println("b "+b);
				
				s += (b-a) / Math.max(a,b);
			}
			
			silhouettes[i] = s / outerCluster.size();
		}
		
		float dunnIndex = minInterDist / maxIntraDist;
		
		// write description
		String description = "\r\n";
		description += "Dunn index: "+dunnIndex+"\r\n";
		description += "Average silhouette: "+arrayMean(silhouettes)+"\r\n";
		for (int i = 0; i < numClusters; i++)
			description += "Cluster "+db.getCollectionName(childrenIds.get(i))+" silhouette: "+silhouettes[i]+"\r\n";
		description += dMetric;
		if (!posNegNorm)
			description += "; separate +/- normalization";
		description += "\r\n";
		db.setCollectionDescription(parent, db.getCollectionDescription(parent.getCollectionID())+description);
		System.out.println();
		System.out.println(description);
		System.out.println("Time elapsed: "+(System.nanoTime()-startTime)/1000000);
		System.out.println();
		
		outerCluster = null;
		innerCluster = null;
		intraDistances = null;
		interDistances = null;
	}

	// returns distance matrix between two clusters
	private static float[][] distanceMatrix(ArrayList<BinnedPeakList> particlesInCentroid, ArrayList<BinnedPeakList> particlesInOther, DistanceMetric dMetric) {
		int numParticlesInCentroid = particlesInCentroid.size();
		int numParticlesInOther = particlesInOther.size();
		float[][] distances = new float[numParticlesInCentroid][numParticlesInOther];
		float magnitude;
		BinnedPeakList peaks1, peaks2;
		for (int i = 0; i < numParticlesInCentroid; i++) {
			peaks1 = particlesInCentroid.get(i);
			magnitude = peaks1.getMagnitude(dMetric);
			for (int j = 0; j < numParticlesInOther; j++) {
				peaks2 = particlesInOther.get(j);
				distances[i][j] = peaks1.getDistance(peaks2, magnitude, dMetric);
			}
		}
		return distances;
	}
	
	// returns distance matrix within one cluster
	private static SymmetricMatrix distanceMatrix(ArrayList<BinnedPeakList> particlesInCentroid,  DistanceMetric dMetric) {
		int numParticlesInCentroid = particlesInCentroid.size();
		SymmetricMatrix distances = new SymmetricMatrix(numParticlesInCentroid);
		float magnitude, dist;
		BinnedPeakList peaks1, peaks2;
		for (int i = 0; i < numParticlesInCentroid; i++) {
			peaks1 = particlesInCentroid.get(i);
			magnitude = peaks1.getMagnitude(dMetric);
			for (int j = i+1; j < numParticlesInCentroid; j++) {
				peaks2 = particlesInCentroid.get(j);
				dist = peaks1.getDistance(peaks2, magnitude, dMetric);
				distances.set(i, j, dist);
			}
		}
		return distances;
	}
	
	// extracts k-th column of a 2D float array
	private static float[] arrayCol(float[][] array, int k) {
		int m = array.length;
		int n = array[0].length;
		float[] col = new float[m];
		for (int i = 0; i < m; i++) {
			col[i] = array[i][k];
		}
		return col;
	}
	
	// computes min of a float array
	private static float arrayMin(float[] array) {
		float min = Float.MAX_VALUE;
		for (float f : array)
			if (f < min)
				min = f;
		return min;
	}
	
	// computes max of a float array
	private static float arrayMax(float[] array) {
		float max = Float.MIN_VALUE;
		for (float f : array)
			if (f > max)
				max = f;
		return max;
	}
	
	// computes mean of a float array
	private static float arrayMean(float[] array, int bias) {
		if (array.length-bias == 0)
			return 0;
		float sum = 0;
		for (float f : array)
			sum += f;
		return sum / (array.length-bias);
	}
	
	private static float arrayMean(float[] array) {
		return arrayMean(array, 0);
	}
	
	public enum ValidityTest {
		DUNN, SIL, BOTH, NONE;
	}
}

// conserves memory, lets me calculate the pointwise distances within a cluster in one go
class SymmetricMatrix {
	float[] vals;
	int n;
	
	public SymmetricMatrix(int n) {
		this.n = n;
		this.vals = new float[n*(n-1)/2];
	}
	
	public int size() {
		return n;
	}
	
	// this seems like it would be a software interview question
	public float get(int i, int j) {
		if (i <= j)
			return vals[i*n-(i-1)*i/2+j-i];
		else
			return vals[j*n-(j-1)*j/2+i-j];
	}
	
	public void set(int i, int j, float v) {
		if (i <= j)
			vals[i*n-(i-1)*i/2+j-i] = v;
		else
			vals[j*n-(j-1)*j/2+i-j] = v;
	}
	
	public float[] getRow(int i) {
		float[] row = new float[n];
		for (int j = 0; j < n; j++)
			row[j] = this.get(i,j);
		return row;
	}
	
	public float[] getCol(int j) {
		float[] col = new float[n];
		for (int i = 0; i < n; i++)
			col[i] = this.get(i,j);
		return col;
	}
}