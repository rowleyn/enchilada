package edu.carleton.enchilada;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("All unit tests that I could find");
		//$JUnit-BEGIN$
		suite.addTestSuite(edu.carleton.enchilada.analysis.BinnedPeakListTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.MedianFinderTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.NormalizableTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.NormalizerTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.clustering.Art2ATest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.clustering.ClusterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.clustering.ClusterQueryTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.clustering.KMeansTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.clustering.KMediansTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.dataCompression.CFNodeTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.dataCompression.CFTreeTest.class);
		suite.addTestSuite(edu.carleton.enchilada.analysis.dataCompression.ClusterFeatureTest.class);
		suite.addTestSuite(edu.carleton.enchilada.ATOFMS.CalInfoTest.class);
		suite.addTestSuite(edu.carleton.enchilada.chartlib.hist.HistogramDatasetTest.class);
		suite.addTestSuite(edu.carleton.enchilada.database.DatabaseTest.class);
		suite.addTestSuite(edu.carleton.enchilada.database.DynamicTableGeneratorTest.class);
		suite.addTestSuite(edu.carleton.enchilada.database.TSBulkInserterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataImporters.AMSDataSetImporterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataImporters.ATOFMSDataSetImporterTest.class);
		// jtbigwoo - enchilada data format is no longer used in enchilada
		//suite.addTestSuite(edu.carleton.enchilada.dataImporters.EnchiladaDataSetImporterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataImporters.PALMSDataSetImporterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataImporters.SPASSDataSetImporterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataImporters.TSImportTest.class);
		suite.addTestSuite(edu.carleton.enchilada.externalswing.ProgressTaskTest.class);
		suite.addTestSuite(edu.carleton.enchilada.gui.AggregatorTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataExporters.MSAnalyzeDataSetExporterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.dataExporters.CSVDataSetExporterTest.class);
		suite.addTestSuite(edu.carleton.enchilada.gui.DialogHelperTest.class);
		//$JUnit-END$
		return suite;
	}

}
