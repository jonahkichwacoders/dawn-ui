package org.dawnsci.spectrum.ui.ReflectivityUI;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.SliceND;

public class OverlapFinder{

	
	public static double[][] overlapFinderOperation (ArrayList<ILazyDataset> arrayILDx) {
		
		IDataset[] xArray = new IDataset[arrayILDx.size()];
		
		double[][] maxMinArray = new double[arrayILDx.size()][2];
		double[][] overlap = new double[arrayILDx.size()][2];
		
		
		
		for(int k =0;k<arrayILDx.size();k++){
			
			SliceND sliceX =new SliceND(arrayILDx.get(k).getShape());
			
			try {
				xArray[k] =arrayILDx.get(k).getSlice(sliceX);
				maxMinArray[k][0] = (double) xArray[k].max(null);
				maxMinArray[k][1] = (double) xArray[k].min(null);
				
			} catch (DatasetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		
		
		for (int k=0; k<arrayILDx.size()-1;k++){
			
			ArrayList<Integer> overlapLower = new ArrayList<Integer>();
			ArrayList<Integer> overlapHigher = new ArrayList<Integer>();
			
			
			//if (xArray[k+1] != null){
				if (maxMinArray[k][0]>maxMinArray[k+1][1]){
					for(int l=0; l<xArray[k].getSize();l++){
						if (xArray[k].getDouble(l)>maxMinArray[k+1][1]){
							overlapLower.add(l);
						}
					}
					for(int m=0; m<xArray[k+1].getSize();m++){
						if (xArray[k+1].getDouble(m)<maxMinArray[k][0]){
							overlapHigher.add(m);
						}
					}
				}
			
			overlap[k][1]=(double) xArray[k].getDouble(Collections.min(overlapLower));
			overlap[k][0]=(double) maxMinArray[k][0];		
				}
		
		
		
		
		
		
		return overlap;
	}
}