import java.io.*;
import java.util.ArrayList;

public class PLot1 {
	
	double[][] PLot1;
	
	public void MakePairLots(String[][] Pairs, DataMaster DataMasterObj) {		
		Utils util = new Utils();
		PLot1 = new double[Pairs.length][6];
		try {
			//get the parameters
			//double GrossDelta = DataMasterObj.Param.GrossDelta;
			
			ArrayList Plot1Data = util.LoadDataFromFile(DataMasterObj.Param.PLot1Path);
						
			for(int i=0;i<Pairs.length;i++) {
				String Pairs1 = Pairs[i][0];
				String Pairs2 = Pairs[i][1];
								
//				double TotalVal1 = util.GetTotalValue(Futures, Pairs1, BbgList);
//				double TotalVal2 = util.GetTotalValue(Futures, Pairs2, BbgList);
//				
				double LotSize1 = util.GetFuturesData(DataMasterObj, Pairs1, 5);
		        double LotSize2 = util.GetFuturesData(DataMasterObj, Pairs2, 5);		        
//				double LastPrice1 = util.GetLastPrice(Futures, Pairs1, BbgList);
//		        double LastPrice2 = util.GetLastPrice(Futures, Pairs2, BbgList);
		        
        
		        // Search for current Pair in PlotData ArrayList
//		        for(int j=0;j<Plot1Data.size();j++) {
//		        	String CurrLine = (String) Plot1Data.get(j);
//		        	String[] TempArray = CurrLine.split("\t");
//		        	// If search 
//		        	if((Pairs1.equals(TempArray[0])) && (Pairs2.equals(TempArray[1]))){
//		        		LotSize1 = Double.valueOf(TempArray[2]);
//		        		LotSize2 = Double.valueOf(TempArray[3]);
//		        	}
//		        }
		        
//	            LotSize1 = (Math.round((GrossDelta / TotalVal1))) * MinLotQty1;
//	            if(LotSize1 == 0) {
//	            	LotSize1 = MinLotQty1;
//	            }
//	            double LongDelta = LotSize1 * LastPrice1;
//	            
//	            LotSize2 = (Math.round((GrossDelta / TotalVal2))) * MinLotQty2;
//	            if(LotSize2 == 0) {
//	            	LotSize2 = MinLotQty2;
//	            }
//	            double ShortDelta = LotSize2 * LastPrice2;
	            
	            PLot1[i][0] = LotSize1;
	            PLot1[i][1] = LotSize2;
	            PLot1[i][2] = 0;
	            PLot1[i][3] = 0;
	            PLot1[i][4] = 0;
	            PLot1[i][5] = 0;	   				
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
	}
	
	public void MakeVolBasedLots(String[][] Pairs, DataMaster DataMasterObj) {		
		Utils util = new Utils();
		PLot1 = new double[Pairs.length][6];

		try {			
			for(int i=0;i<Pairs.length;i++) {
				String Pairs1 = Pairs[i][0];
				String Pairs2 = Pairs[i][1];

				int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, Pairs1, "STOCKINDEX");
				int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, Pairs2, "STOCKINDEX");											
	
				double LotSize1 = util.GetFuturesData(DataMasterObj, Pairs1, 5);
		        double LotSize2 = util.GetFuturesData(DataMasterObj, Pairs2, 5);		        
		        
				//get the BBGtime series gap for this stock index
		        String MainStockCode = Pairs1; 
				if(Pairs1.equals(DataMasterObj.FuturesCode[0].trim())) {MainStockCode = Pairs2;}								
				int MainStockIndex = (int) util.GetQuotesData(DataMasterObj, MainStockCode, "STOCKINDEX");			
		        double CurrBBGTimeSeriesGap = DataMasterObj.BBGTimeSeriesGapForEachAsset[MainStockIndex];
				double CurrMarketTimeinHours = DataMasterObj.MarketTimeInHoursForEachAsset[MainStockIndex];
		        double VolCutOffValue = DataMasterObj.VolCutOffForDynamicLotEachAsset[MainStockIndex];
				
		        //get the volatility here
				double FinalVol = util.GetVolatility(DataMasterObj, i);
				double CurrAnnualVol = (FinalVol)*Math.sqrt((CurrMarketTimeinHours*60)/CurrBBGTimeSeriesGap)*Math.sqrt(252);			 			 
				
				//if the vol is low then use this for doubling the size
				if(CurrAnnualVol < VolCutOffValue) {
					if(StockIndex1 == 0) {
						LotSize2 = 2*LotSize2;
					}
					else {
						LotSize1 = 2*LotSize1;
					}					
				}
					if(CurrAnnualVol > 3*VolCutOffValue) {
					if(StockIndex1 == 0) {
						LotSize2 = 0;
					}
					else {
						LotSize1 = 0;
					}					
				}
				
				DataMasterObj.PLot1[i][0] = LotSize1;
				DataMasterObj.PLot1[i][1] = LotSize2;
	            PLot1[i][2] = 0;
	            PLot1[i][3] = 0;
	            PLot1[i][4] = 0;
	            PLot1[i][5] = 0;		            
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			e.printStackTrace();
		}
	}
}
