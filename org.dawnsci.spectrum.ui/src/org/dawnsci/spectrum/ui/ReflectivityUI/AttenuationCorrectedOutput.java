package org.dawnsci.spectrum.ui.ReflectivityUI;

import java.util.ArrayList;

import org.eclipse.dawnsci.analysis.api.roi.IRectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.dataset.SliceND;

public class AttenuationCorrectedOutput {

	public static IDataset[][] StitchingOverlapProcessMethod(ArrayList<ILazyDataset> arrayILDy,
			ArrayList<ILazyDataset> arrayILDx, ReflectivityUIModel model) {
		
		IDataset[] xArray = new IDataset[arrayILDx.size()];
		IDataset[] yArray = new IDataset[arrayILDy.size()];
		
		IDataset[] xArrayCorrected = new IDataset[arrayILDx.size()];
		IDataset[] yArrayCorrected = new IDataset[arrayILDy.size()];
		
		IDataset[][] output = new IDataset[2][];
		
		int k=0;
		
		for(k =0;k<arrayILDx.size();k++){
			
			SliceND sliceX =new SliceND(arrayILDx.get(k).getShape());
			SliceND sliceY =new SliceND(arrayILDy.get(k).getShape());
			
			try {
				xArray[k] =arrayILDx.get(k).getSlice(sliceX);
				yArray[k] =arrayILDy.get(k).getSlice(sliceY);
				
				xArrayCorrected[k] =arrayILDx.get(k).getSlice(sliceX);
				yArrayCorrected[k] =arrayILDy.get(k).getSlice(sliceY);
				
				
				
			} catch (DatasetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		double[][] maxMinArray = new double[arrayILDx.size()][2];
		
		
		for (k=0; k<(arrayILDx.size()-1);k++){
			
			IRectangularROI box = new RectangularROI();
//			System.out.println("k:  "+ k);
			
			box = model.getROIListElement(k).getBounds();
			
			
			maxMinArray[k][0] = box.getPointX()+ box.getLength(0);
			maxMinArray[k][1] = box.getPointX();
		}
		
		double attenuationFactor =1;
		
		double[] correctionRatioArray = new double[arrayILDx.size()];
		correctionRatioArray[0]=1;
		
		//TEST
		for (k=0; k<arrayILDx.size()-1;k++){
			
			ArrayList<Integer> overlapLower = new ArrayList<Integer>();
			ArrayList<Integer> overlapHigher = new ArrayList<Integer>();
			
			
			for(int l=0; l<xArrayCorrected[k].getSize();l++){
				if (xArrayCorrected[k].getDouble(l)>=maxMinArray[k][1]){
					overlapLower.add(l);
				}
			}
			for(int m=0; m<xArrayCorrected[k+1].getSize();m++){
				if (xArrayCorrected[k+1].getDouble(m)<maxMinArray[k][0]){
					overlapHigher.add(m);
				}
			}
				
				

					
					Dataset[] xLowerDataset =new Dataset[1];
					Dataset yLowerDataset =null;
					Dataset[] xHigherDataset =new Dataset[1];
					Dataset yHigherDataset =null;
					
					ArrayList<Double> xLowerList =new ArrayList<>();
					ArrayList<Double> yLowerList =new ArrayList<>();
					ArrayList<Double> xHigherList =new ArrayList<>();
					ArrayList<Double> yHigherList =new ArrayList<>();
					
					for (int l=0; l<overlapLower.size(); l++){
						xLowerList.add(xArray[k].getDouble(overlapLower.get(l)));
						yLowerList.add(yArray[k].getDouble(overlapLower.get(l)));
						xLowerDataset[0] = DatasetFactory.createFromObject(xLowerList);
						yLowerDataset = DatasetFactory.createFromObject(yLowerList);
					}
					
					for (int l=0; l<overlapHigher.size(); l++){
						xHigherList.add(xArray[k+1].getDouble(overlapHigher.get(l)));
						yHigherList.add(yArray[k+1].getDouble(overlapHigher.get(l)));
						xHigherDataset[0] = DatasetFactory.createFromObject(xHigherList);
						yHigherDataset = DatasetFactory.createFromObject(yHigherList);

					}

				
					double correctionRatio = PolynomialOverlap.correctionRatio(xLowerDataset, yLowerDataset, xHigherDataset, yHigherDataset, attenuationFactor);
					
					attenuationFactor = correctionRatio;
					
					yArrayCorrected[k+1] = Maths.multiply(yArray[k+1],attenuationFactor);
					
//					System.out.println("attenuation factor:  " + attenuationFactor + "   k:   " +k);
				//	}
				}
		output[0] = yArrayCorrected;
		output[1] = xArrayCorrected;
		
		return output;
	}
	
	
}
					
