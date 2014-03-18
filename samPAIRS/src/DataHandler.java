import java.util.ArrayList;
import java.util.Calendar;
//comment by aakash
//comment by dipyant

public class DataHandler {
	
	
	public void LoadHighFreqDataFromFiles(DataMaster DataMasterObj) {
		DataMasterObj.TimeListAllAssetCurrExpiry = new ArrayList();
		DataMasterObj.PriceListAllAssetCurrExpiry = new ArrayList();
		DataMasterObj.TimeListAllAssetNextExpiry = new ArrayList();
		DataMasterObj.PriceListAllAssetNextExpiry = new ArrayList();
		
		try {
			Utils UtilObj = new Utils();			
			UtilObj.UpdateAllQuotes(DataMasterObj);
			
			String DateOfCurrentExpiry = DataMasterObj.ExpiryDatesCurrentExpiry[1];
			String DateOfNextExpiry = DataMasterObj.ExpiryDatesNextExpiry[1];
			
			String CurrExpiryFile = DataMasterObj.AssetDataDirectory+"ClosePriceData"+"_"+DateOfCurrentExpiry+".txt";
			String NextExpiryFile = DataMasterObj.AssetDataDirectory+"ClosePriceData"+"_"+DateOfNextExpiry+".txt";
			
			ArrayList<String> ClosePriceCurrExpiryList = UtilObj.LoadDataFromFile(CurrExpiryFile);
			ArrayList<String> ClosePriceNextExpiryList = UtilObj.LoadDataFromFile(NextExpiryFile);
			
			double[][] ClosePriceCurrExpiry = new double[ClosePriceCurrExpiryList.size()][DataMasterObj.FuturesCode.length];
			double[][] ClosePriceNextExpiry = new double[ClosePriceNextExpiryList.size()][DataMasterObj.FuturesCode.length];
			String[] TimeCurrExpiry = new String[ClosePriceCurrExpiryList.size()];
			String[] TimeNextExpiry = new String[ClosePriceNextExpiryList.size()];
			
			//get data for time and curr expiry close values
			for(int i=0;i<ClosePriceCurrExpiryList.size();i++) {
				String[] CurrDataRow = ClosePriceCurrExpiryList.get(i).split("\t");
				TimeCurrExpiry[i] = CurrDataRow[0];
				for(int j=1;j<CurrDataRow.length;j++) {
					ClosePriceCurrExpiry[i][j-1] = Double.parseDouble(CurrDataRow[j]);
				}				
			}
			
			//get data for time and next expiry close values
			for(int i=0;i<ClosePriceNextExpiryList.size();i++) {
				String[] CurrDataRow = ClosePriceNextExpiryList.get(i).split("\t");
				TimeNextExpiry[i] = CurrDataRow[0];
				for(int j=1;j<CurrDataRow.length;j++) {
					ClosePriceNextExpiry[i][j-1] = Double.parseDouble(CurrDataRow[j]);
				}				
			}
			
			for(int i=0; i<DataMasterObj.Pairs.length ; i++){
				String StockCode1 = DataMasterObj.Pairs[i][0];
				String StockCode2 = DataMasterObj.Pairs[i][1];
				int StockIndex1 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
				int StockIndex2 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												
				
				double TimeGap = DataMasterObj.BBGTimeSeriesGapForEachAsset[StockIndex1];
				ArrayList<String> HighFreqDataCurrExp = new ArrayList();
				ArrayList<String> HighFreqDataNextExp = new ArrayList();
				
				for(int j=0;j<TimeCurrExpiry.length;j++) {
					String CurrRatio = Double.valueOf(UtilObj.GetFinalRatio(ClosePriceCurrExpiry[j][StockIndex1], ClosePriceCurrExpiry[j][StockIndex2])).toString();
					HighFreqDataCurrExp.add(TimeCurrExpiry[j] + "\t" + CurrRatio);					
				}
				
				for(int j=0;j<TimeNextExpiry.length;j++) {
					String CurrRatio = Double.valueOf(UtilObj.GetFinalRatio(ClosePriceNextExpiry[j][StockIndex1],ClosePriceNextExpiry[j][StockIndex2])).toString();
					HighFreqDataNextExp.add(TimeNextExpiry[j] + "\t" + CurrRatio);					
				}
				
				//add data for curr expiry first
				ArrayList TimePriceListForEachAsset = GetTimePriceList(DataMasterObj, StockCode1+"-"+StockCode2, HighFreqDataCurrExp, TimeGap);
				DataMasterObj.TimeListAllAssetCurrExpiry.add(TimePriceListForEachAsset.get(0));
				DataMasterObj.PriceListAllAssetCurrExpiry.add(TimePriceListForEachAsset.get(1));
				
				
				//add data for next expiry next
				TimePriceListForEachAsset = GetTimePriceList(DataMasterObj, StockCode1+"-"+StockCode2, HighFreqDataNextExp, TimeGap);
				DataMasterObj.TimeListAllAssetNextExpiry.add(TimePriceListForEachAsset.get(0));
				DataMasterObj.PriceListAllAssetNextExpiry.add(TimePriceListForEachAsset.get(1));				
			}						
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//function to read the highfreqdata and convert into arrays
	public ArrayList GetTimePriceList(DataMaster DataMasterObj, String CurrAsset, ArrayList HighFreqList, double TimeGap) {
		ArrayList TimePriceList = new ArrayList();
		Utils UtilObj = new Utils();
		try {
			ArrayList TimeList = new ArrayList();
			ArrayList PriceList = new ArrayList();
			
			//get data upto 3*TimeGap*60 data points of data and load into memory
			int StartingPointForData = (int)Math.round(TimeGap*10*60);
			//int StartingPointForData = 0;
			if(HighFreqList.size() < StartingPointForData) {
				StartingPointForData = HighFreqList.size();
			}
			
			for(int i=HighFreqList.size()-StartingPointForData;i<HighFreqList.size();i++) {
				String[] CurrLine = ((String)HighFreqList.get(i)).trim().split("\t");
				String CurrDateTime = CurrLine[0].trim();						
				double CurrPrice = Double.valueOf(CurrLine[1].trim());
				TimeList.add(CurrDateTime);
				PriceList.add(CurrPrice);				
			}
			
			TimePriceList.add(TimeList);
			TimePriceList.add(PriceList);
		}
		catch(Exception e) {
			DataMasterObj.GlobalErrors = "FATAL_ERROR_IN_LOADING_HIGHFREQDATA_"+CurrAsset;
			e.printStackTrace();
			System.exit(0);
		}
		return TimePriceList;
	}
	
	 int HighFreqLastTickDay = 0;
	 public long WriteHighFreqDataFast(DataMaster DataMasterObj) {
		 long HighFreqLastLineTime = 0;
		 Utils util = new Utils();
		 try {	
			 // get string from quotes file
				String CurrDateTime = util.NowDateTimeHighFreq(DataMasterObj);
				int DataConsistent = 1;
				
				for(int j=0; j<DataMasterObj.Pairs.length ; j++){
					DataConsistent = 1;
					String StockCode1 = DataMasterObj.Pairs[j][0];
					String StockCode2 = DataMasterObj.Pairs[j][1];
						
					int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
					int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

					//get the start and stop time for this asset class
					int ExchangeRunning = util.CheckExchangeRunning(DataMasterObj, StockIndex1);
					
					if(DataMasterObj.Quotes[StockIndex1][0] <= 0 || DataMasterObj.Quotes[StockIndex2][0] <=0) {
						DataConsistent = 0;
					}else{
						if(DataMasterObj.Param.OptionsTradingActive == 2) {
							double ImpliedVol1 = GetImpliedVolatility(DataMasterObj, StockIndex1, "CurrExpiry", "");
							double ImpliedVol2 = GetImpliedVolatility(DataMasterObj, StockIndex2, "CurrExpiry", "");
							
							if(ImpliedVol1 > 0 && ImpliedVol2 > 0) {
								DataMasterObj.PriceRatio[j]= util.GetFinalRatio(ImpliedVol1 , ImpliedVol2);																						
							}
							else {
								DataConsistent = 0;
							}
						}
						else {
							DataMasterObj.PriceRatio[j]= util.GetFinalRatio(DataMasterObj.Quotes[StockIndex1][0], DataMasterObj.Quotes[StockIndex2][0]);							
						}
					}
					
				
					int DataRepeat = 1;
					ArrayList HighFreqData = (ArrayList)(DataMasterObj.PriceListAllAssetCurrExpiry.get(j));
					int EndIndex = HighFreqData.size()-1;
					if (EndIndex < DataMasterObj.Param.DataRepeatRows+1){
						DataRepeat = 0;
					}else{
						for (int i=0;i<DataMasterObj.Param.DataRepeatRows;i++){
							double QuoteData = ((Double)HighFreqData.get(EndIndex-i));
							//j=0 is for 100000 - whichis always 1
							if (DataMasterObj.PriceRatio[j] != QuoteData){
								DataRepeat = 0;
							}
						}
					}
					
					//j=0 is dummy so use it as it is
//					if(j==0) {
//						DataConsistent = 1;DataRepeat=0;ExchangeRunning=1;
//					}
					
					if(DataConsistent == 1 && DataRepeat== 0 && ExchangeRunning == 1) {
						((ArrayList)(DataMasterObj.TimeListAllAssetCurrExpiry.get(j))).add(CurrDateTime);										
						((ArrayList)(DataMasterObj.PriceListAllAssetCurrExpiry.get(j))).add(DataMasterObj.PriceRatio[j]);				
					}	
					else {
						if(DataConsistent == 0) {
							//WriteLogFile("Not writing data for " + CurrAsset + "... DataInconsistent");
						}
						if(DataRepeat== 1) {
							//util.WriteLogFile("Not writing data for " + StockCode1 + "-" + StockCode2 + "... DataRepeat");
						}
						if(ExchangeRunning== 0) {
							//WriteLogFile("Not writing data for " + CurrAsset + "... ExchangeTimeOver");
						}						
					}				
				}
		 }
		 catch(Exception e) {
			 DataMaster.logger.warning(e.toString());
			 e.printStackTrace();
			 DataMasterObj.GlobalErrors = "ERROR_HIGHFREQ_FILE_WRITING";
		 }
		 return HighFreqLastLineTime ; 
	 }
	 
	 // function for writing HighFreqData file 	 
	 public long WriteHighFreqDataNextExpiryFast(DataMaster DataMasterObj) {
		 long HighFreqLastLineTime = 0;
		 Utils util = new Utils();
		 try {	
			 // get string from quotes file
				String CurrDateTime = util.NowDateTimeHighFreq(DataMasterObj);
				int DataConsistent = 1;
				
				//do not write for 100000 - only write data for non-dummy ones
				for(int j=0; j<DataMasterObj.Pairs.length ; j++){
					DataConsistent = 1;

					String StockCode1 = DataMasterObj.Pairs[j][0];
					String StockCode2 = DataMasterObj.Pairs[j][1];
						
					int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
					int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

					if(DataMasterObj.QuotesNextExpiry[StockIndex1][0] <= 0 || DataMasterObj.QuotesNextExpiry[StockIndex2][0] <=0) {
						DataConsistent = 0;
						continue;
					}else{
						if(DataMasterObj.Param.OptionsTradingActive == 2) {
							double ImpliedVol1 = GetImpliedVolatility(DataMasterObj, StockIndex1, "NextExpiry", "");
							double ImpliedVol2 = GetImpliedVolatility(DataMasterObj, StockIndex2, "NextExpiry", "");
							
							if(ImpliedVol1 > 0 && ImpliedVol2 > 0) {
								DataMasterObj.PriceRatioNextExpiry[j] = util.GetFinalRatio(ImpliedVol1 , ImpliedVol2);																						
							}
							else {
								DataConsistent = 0;
							}
						}
						else {
							DataMasterObj.PriceRatioNextExpiry[j] = util.GetFinalRatio(DataMasterObj.QuotesNextExpiry[StockIndex1][0], DataMasterObj.QuotesNextExpiry[StockIndex2][0]);
						}
					}

					//get the start and stop time for this asset class
					int ExchangeRunning = util.CheckExchangeRunning(DataMasterObj, StockIndex1);
									
					int DataRepeat = 1;
					ArrayList HighFreqData = (ArrayList)(DataMasterObj.PriceListAllAssetNextExpiry.get(j));
					int EndIndex = HighFreqData.size()-1;
					if (EndIndex < DataMasterObj.Param.DataRepeatRows+1){
						DataRepeat = 0;
					}else{
						for (int i=0;i<DataMasterObj.Param.DataRepeatRows;i++){
							double QuoteData = ((Double)HighFreqData.get(EndIndex-i));
							//j=0 is for 100000 - whichis always 1
							if (DataMasterObj.PriceRatioNextExpiry[j] != QuoteData){
								DataRepeat = 0;
							}
						}
					}
					
					//j=0 is dummy so use it as it is
//					if(j==0) {
//						DataConsistent = 1;DataRepeat=0;ExchangeRunning=1;
//					}
					
					if(DataConsistent == 1 && DataRepeat== 0 && ExchangeRunning == 1) {
						((ArrayList)(DataMasterObj.TimeListAllAssetNextExpiry.get(j))).add(CurrDateTime);										
						((ArrayList)(DataMasterObj.PriceListAllAssetNextExpiry.get(j))).add(DataMasterObj.PriceRatioNextExpiry[j]);				
					}	
					else {
						if(DataConsistent == 0) {
							//util.WriteLogFile("Not writing next expiry data for " + StockCode1 + "-" + StockCode2 + "... DataInconsistent");
						}
						if(DataRepeat== 1) {
							//util.WriteLogFile("Not writing next expiry data for " + StockCode1 + "-" + StockCode2 + "... DataRepeat");
						}
						if(ExchangeRunning== 0) {
							//WriteLogFile("Not writing next expiry data for " + CurrAsset + "... ExchangeTimeOver");
						}						
					}				
				}
		 }
		 catch(Exception e) {
			 DataMaster.logger.warning(e.toString());
			 e.printStackTrace();
			 DataMasterObj.GlobalErrors = "ERROR_HIGHFREQ_NEXTEXPIRY_FILE_WRITING";
		 }
		 return HighFreqLastLineTime ; 
	 }
	 
		// function for filling BBGData file Time Series from High Freq Data file 
		ArrayList BBGData = null;	
		public void FillBBGSheetFast(DataMaster DataMasterObj) {
			Utils UtilObj = new Utils();	
			int DataSeriesLength = DataMasterObj.Param.BBGTimeSeriesLength; ; 
			
			try {
				for(int j=0; j<DataMasterObj.Pairs.length ; j++){
					String StockCode1 = DataMasterObj.Pairs[j][0];
					String StockCode2 = DataMasterObj.Pairs[j][1];
						
					int StockIndex1 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
					int StockIndex2 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

					double TimeGap = DataMasterObj.BBGTimeSeriesGapForEachAsset[StockIndex1];
			
					double OrignalTimeGap = TimeGap ; 
					ArrayList<String> TimeList = new ArrayList();
					ArrayList<Double> PriceList = new ArrayList();					
					
					RiskManager RiskMan = new RiskManager();
					ArrayList<String> HighFreqData = new ArrayList();
					int ExpiryRisk = RiskMan.CalcLiveRisk(DataMasterObj, StockCode1, DataMasterObj.FuturesCode[0]);
					if(ExpiryRisk == 0) {
						// Load HighFreqData.txt file into arraylist			
						TimeList = (ArrayList)(DataMasterObj.TimeListAllAssetCurrExpiry.get(j));
						PriceList = (ArrayList)(DataMasterObj.PriceListAllAssetCurrExpiry.get(j));
					}
					else {
						// Load HighFreqData.txt file into arraylist			
						TimeList = (ArrayList)(DataMasterObj.TimeListAllAssetNextExpiry.get(j));
						PriceList = (ArrayList)(DataMasterObj.PriceListAllAssetNextExpiry.get(j));
					}
										
					BBGData = new ArrayList();
					DataMasterObj.BbgList[j] = new ArrayList();
					DataMasterObj.BbgListInverse[j] = new ArrayList();
					if(TimeList.size() <= 1) {
						continue;
					}				
					
					// Review HighFreqData arraylist & fill into bbgArrayList untill 60 data points are done
					int LastIndex = TimeList.size()-1 ;
					BBGData.add(TimeList.get(LastIndex) + "\t" + PriceList.get(LastIndex));
										
					String CurrPivotTime = TimeList.get(LastIndex);	
							
					// Loop through all rows of HighFreqData array list 
					for(int i = TimeList.size()-1 ; i > -1 ; i--){
						// get current pivot Time in seconds & Current Pivot Day (integer)
						long CurrPivotTimeInSec = UtilObj.GetTimeInSecondsForStr(CurrPivotTime);
						String CurrPivotDay = CurrPivotTime.substring(0,8);
						
						String CurrTime = TimeList.get(i);
						long CurrTimeInSec = UtilObj.GetTimeInSecondsForStr(CurrTime);
						String CurrDay = CurrTime.substring(0,8);
						
						if(!(CurrDay.equals(CurrPivotDay))){
							// check if the day has changed then change the time gap
							long NewHighFreqPivot = i ;
							
							String TempPivotDate = TimeList.get(i+1);
							long TempPivotTimeInSec = UtilObj.GetTimeInSecondsForStr(TempPivotDate);
							TimeGap = TimeGap - (CurrPivotTimeInSec- TempPivotTimeInSec)/60.0 ;
							
							CurrPivotTime = TimeList.get(i);
							CurrPivotTimeInSec = UtilObj.GetTimeInSecondsForStr(CurrPivotTime);
							CurrPivotDay = CurrPivotTime.substring(0,8);
						}
						// if on same day CurrPivotTimeInSec CurrTimeInSec >= TimeGap *60 then add that row to BBG data 
						if(CurrDay.equals(CurrPivotDay)){
							if(CurrPivotTimeInSec - CurrTimeInSec >= TimeGap *60 ){
								CurrPivotTime = CurrTime;
								// insert HighFreqdata row into BBG data arraylist
								BBGData.add(TimeList.get(i) + "\t" + PriceList.get(i));
								TimeGap = OrignalTimeGap ;
							}
						}
						
						// if bbgdata length is more than 60 than break from the loop 
						if(BBGData.size() >= DataSeriesLength){
							break; 
						}
					}
					
					//reverse the BBGList here
					ArrayList TempBBGList = new ArrayList();
					ArrayList TempBBGListInverse = new ArrayList();					
					int TempBBGListIndex = 0;
					for(int i=BBGData.size()-1;i>=0;i--) {
						TempBBGList.add((String) (BBGData.get(i)));
						TempBBGListInverse.add((String) (BBGData.get(i)));
					}			
					DataMasterObj.BbgList[j] = TempBBGList;	
					DataMasterObj.BbgListInverse[j] = TempBBGListInverse;						
				}
							
				//make the return series here
				UtilObj.CreateReturnSeries(DataMasterObj);
				DataMasterObj.OUValues = UtilObj.GetOUValues(DataMasterObj);
			}
			catch(Exception e) {
				e.printStackTrace();
				UtilObj.WriteLogFile("Error in BBG File printing");
				DataMasterObj.GlobalErrors = "FATAL_ERROR_BBG_FILE_WRITING";
			}
		}
		
		
		public void ModifyBBGSheetEachSecond(DataMaster DataMasterObj) {
			 long HighFreqLastLineTime = 0;
			 Utils util = new Utils();
			 try {	
				 // get string from quotes file
					String CurrDateTime = util.NowDateTimeHighFreq(DataMasterObj);
					int DataConsistent = 1;
					
					for(int j=0; j<DataMasterObj.Pairs.length ; j++){
						
						//if the BBGList length is not sufficient then return immediately
						if(DataMasterObj.BbgList[j].size() < DataMasterObj.Param.BBGTimeSeriesLength) {
							continue;
						}
						
						double CurrRatio = 0;
						double CurrRatioInverse = 0;
						DataConsistent = 1;
						String StockCode1 = DataMasterObj.Pairs[j][0];
						String StockCode2 = DataMasterObj.Pairs[j][1];
							
						int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
						int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												
						
						RiskManager RiskMan = new RiskManager();
						ArrayList<String> HighFreqData = new ArrayList();
						int ExpiryRisk = RiskMan.CalcLiveRisk(DataMasterObj, StockCode1, DataMasterObj.FuturesCode[0]);
						
						String ExpiryType = "CurrExpiry";
						
						double BID1 = DataMasterObj.Quotes[StockIndex1][1];
						double BID2 = DataMasterObj.Quotes[StockIndex2][1];
						double ASK1 = DataMasterObj.Quotes[StockIndex1][2];
						double ASK2 = DataMasterObj.Quotes[StockIndex2][2];
						
						if(ExpiryRisk != 0) {
							ExpiryType = "NextExpiry";
							BID1 = DataMasterObj.QuotesNextExpiry[StockIndex1][1];
							BID2 = DataMasterObj.QuotesNextExpiry[StockIndex2][1];
							ASK1 = DataMasterObj.QuotesNextExpiry[StockIndex1][2];
							ASK2 = DataMasterObj.QuotesNextExpiry[StockIndex2][2];
						}
						
						//get the start and stop time for this asset class
						int ExchangeRunning = util.CheckExchangeRunning(DataMasterObj, StockIndex1);
						if(BID1 <= 0 || BID2 <=0 || ASK1 <= 0 || ASK2 <= 0) {
							DataConsistent = 0;
						}else{
							if(DataMasterObj.Param.OptionsTradingActive == 2) {
								double ImpliedVol1 = GetImpliedVolatility(DataMasterObj, StockIndex1, ExpiryType, "BUY");
								double ImpliedVol2 = GetImpliedVolatility(DataMasterObj, StockIndex2, ExpiryType, "SELL");
								
								double ImpliedVol1Inverse = GetImpliedVolatility(DataMasterObj, StockIndex1, ExpiryType, "SELL");
								double ImpliedVol2Inverse = GetImpliedVolatility(DataMasterObj, StockIndex2, ExpiryType, "BUY");
								
								if(ImpliedVol1 > 0 && ImpliedVol2 > 0 && ImpliedVol1Inverse > 0 && ImpliedVol2Inverse > 0) {
									CurrRatio = util.GetFinalRatio(ImpliedVol1 , ImpliedVol2);	
									CurrRatioInverse = util.GetFinalRatio(ImpliedVol1Inverse , ImpliedVol2Inverse);
								}
								else {
									DataConsistent = 0;
								}
							}
							else {
								CurrRatio = util.GetFinalRatio(ASK1, BID2);
								CurrRatioInverse = util.GetFinalRatio(BID1, ASK2);
							}
						}
						
						//if the ratio is clean then add to the BBGList
						if(DataConsistent == 1 && ExchangeRunning == 1 && CurrRatio != 0) {
							ArrayList TempBBGList = DataMasterObj.BbgList[j];
							TempBBGList.remove(TempBBGList.size()-1);
							TempBBGList.add(CurrDateTime + "\t" + CurrRatio);
							DataMasterObj.BbgList[j] = TempBBGList;
							
							ArrayList TempBBGListInverse = DataMasterObj.BbgListInverse[j];
							TempBBGListInverse.remove(TempBBGListInverse.size()-1);
							TempBBGListInverse.add(CurrDateTime + "\t" + CurrRatioInverse);
							DataMasterObj.BbgListInverse[j] = TempBBGListInverse;							
						}						
					}
					
					//make the return series here
					util.CreateReturnSeries(DataMasterObj);
					DataMasterObj.OUValues = util.GetOUValues(DataMasterObj);
			}
			catch(Exception e) {
				e.printStackTrace();
				DataMasterObj.GlobalErrors = "FATAL_ERROR_BBG_FILE_WRITING";
			}
		}

		
		public void FillBBGSheetDynamicFast(DataMaster DataMasterObj) {
			Utils UtilObj = new Utils();	
			int DataSeriesLength = DataMasterObj.Param.BBGTimeSeriesLength; ; 
			
			try {
				for(int j=0; j<DataMasterObj.Pairs.length ; j++){
					DataMasterObj.BbgListDynamic[j] = new ArrayList();

					String StockCode1 = DataMasterObj.Pairs[j][0];
					String StockCode2 = DataMasterObj.Pairs[j][1];
						
					int StockIndex1 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
					int StockIndex2 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

					double TimeGap = DataMasterObj.BBGTimeSeriesGapForEachAsset[StockIndex1];
					String DateOfCurrentExpiry = DataMasterObj.ExpiryDatesCurrentExpiry[StockIndex1];
					String DateOfNextExpiry = DataMasterObj.ExpiryDatesNextExpiry[StockIndex1];
			
					// clean the BBG.txt file first
					String CurrBBGFile = DataMasterObj.AssetDataDirectory+StockCode1+"-"+StockCode2+"_BBG.txt";
					UtilObj.WriteToFile(CurrBBGFile, null, false);
										
					if(DataMasterObj.BbgList[j].size() < DataMasterObj.Param.BBGTimeSeriesLength) {
						continue;
					}
					
					//if the vol=0 or volgap=0 then use the normal time series gap
					//if(CurrAnnualVol != 0 && DataMasterObj.Param.VolTimeGapDivider != 0) {
					if(DataMasterObj.OUValues[j] != 0) {
						double OUStats = DataMasterObj.OUValues[j];					
						//double Mult = 3.33*DataMasterObj.Param.BBGTimeSeriesGap;					
						double Mult = 3.33*DataMasterObj.BBGTimeSeriesGapForEachAsset[StockIndex1];
						if(OUStats < 2) TimeGap = Math.round(Mult/Math.sqrt(2));
						else if(OUStats > 11)Math.round(Mult/Math.sqrt(11));
						else {
							TimeGap = Math.round(Mult/Math.sqrt(OUStats));
						}					
						//TimeGap = Math.round(DataMasterObj.Param.VolTimeGapDivider/CurrAnnualVol);					
					}
					else {
						TimeGap = DataMasterObj.Param.BBGTimeSeriesGap;
					}				
					DataMasterObj.CurrStocksBBGTimeSeriesGap[j] = TimeGap;
					double OrignalTimeGap = TimeGap; 
					
					RiskManager RiskMan = new RiskManager();
					ArrayList<String> TimeList = new ArrayList();
					ArrayList<Double> PriceList = new ArrayList();					
					ArrayList<String> HighFreqData = new ArrayList();
					int ExpiryRisk = RiskMan.CalcLiveRisk(DataMasterObj, StockCode1, StockCode2);
					if(ExpiryRisk == 0) {
						// Load HighFreqData.txt file into arraylist			
						TimeList = (ArrayList)(DataMasterObj.TimeListAllAssetCurrExpiry.get(j));
						PriceList = (ArrayList)(DataMasterObj.PriceListAllAssetCurrExpiry.get(j));
					}
					else {
						// Load HighFreqData.txt file into arraylist			
						TimeList = (ArrayList)(DataMasterObj.TimeListAllAssetNextExpiry.get(j));
						PriceList = (ArrayList)(DataMasterObj.PriceListAllAssetNextExpiry.get(j));
					}
					
					BBGData = new ArrayList();
					if(TimeList.size() <= 1) {
						continue;
					}				
					
					// Review HighFreqData arraylist & fill into bbgArrayList untill 60 data points are done
					int LastIndex = TimeList.size()-1 ;
					BBGData.add(TimeList.get(LastIndex) + "\t" + PriceList.get(LastIndex));
										
					String CurrPivotTime = TimeList.get(LastIndex);	
							
					// Loop through all rows of HighFreqData array list 
					for(int i = TimeList.size()-1 ; i > -1 ; i--){
						// get current pivot Time in seconds & Current Pivot Day (integer)
						long CurrPivotTimeInSec = UtilObj.GetTimeInSecondsForStr(CurrPivotTime);
						String CurrPivotDay = CurrPivotTime.substring(0,8);
						
						String CurrTime = TimeList.get(i);
						long CurrTimeInSec = UtilObj.GetTimeInSecondsForStr(CurrTime);
						String CurrDay = CurrTime.substring(0,8);
						
						if(!(CurrDay.equals(CurrPivotDay))){
							// check if the day has changed then change the time gap
							long NewHighFreqPivot = i ;
							
							String TempPivotDate = TimeList.get(i+1);
							long TempPivotTimeInSec = UtilObj.GetTimeInSecondsForStr(TempPivotDate);
							TimeGap = TimeGap - (CurrPivotTimeInSec- TempPivotTimeInSec)/60.0 ;
							
							CurrPivotTime = TimeList.get(i);
							CurrPivotTimeInSec = UtilObj.GetTimeInSecondsForStr(CurrPivotTime);
							CurrPivotDay = CurrPivotTime.substring(0,8);
						}
						// if on same day CurrPivotTimeInSec CurrTimeInSec >= TimeGap *60 then add that row to BBG data 
						if(CurrDay.equals(CurrPivotDay)){
							if(CurrPivotTimeInSec - CurrTimeInSec >= TimeGap *60 ){
								CurrPivotTime = CurrTime;
								// insert HighFreqdata row into BBG data arraylist
								BBGData.add(TimeList.get(i) + "\t" + PriceList.get(i));
								TimeGap = OrignalTimeGap ;
							}
						}
						
						// if bbgdata length is more than 60 than break from the loop 
						if(BBGData.size() >= DataSeriesLength){
							break; 
						}
					}
					
					//reverse the BBGList here
					ArrayList TempBBGList = new ArrayList();
					int TempBBGListIndex = 0;
					for(int i=BBGData.size()-1;i>=0;i--) {
						TempBBGList.add((String) (BBGData.get(i)));
					}			
					DataMasterObj.BbgListDynamic[j] = TempBBGList;
					
					// print bbgArrayList into bbg.txt (in reverse way)
					// imp: BBGData has 1st row as current row, but indicator needs 60th backmost data point as first point in bbg.txt file
					if(DataMasterObj.Param.WriteToBBGFile == 1) {
						UtilObj.WriteToBBGFile(CurrBBGFile, BBGData, true);													
					}
				}						
			}
			catch(Exception e) {
				e.printStackTrace();
				UtilObj.WriteLogFile("Error in BBG File printing");
				DataMasterObj.GlobalErrors = "FATAL_ERROR_BBG_DYNAMIC_FILE_WRITING";
			}
		}
		
		public void CheckBBGSheetCorrect(ArrayList[] BBGList, String FilePath) {
			try {
				Utils UtilObj = new Utils();
				UtilObj.WriteToFile(FilePath, null, false);
				
				for(int i=0;i<BBGList.length;i++) {
					ArrayList<String> BBGMinuteList = BBGList[i];
					for(int j=0;j<BBGMinuteList.size();j++) {
						String CurrStr = BBGMinuteList.get(j);
						UtilObj.WriteToFile(FilePath, CurrStr, true);
					}					
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public double GetImpliedVolatility(DataMaster DataMasterObj, int StockIndex, String ExpiryType, String OrderType) {
			double ImpliedVol = 0;
			try {
				Stats StatObj = new Stats();
				Utils UtilObj = new Utils();
				RiskManager RiskMan = new RiskManager();
				
				String CurrStockCode = DataMasterObj.FuturesCode[StockIndex];
				String[] StockCodeStr = CurrStockCode.split("_");
				int NiftyIndex = (int) UtilObj.GetQuotesData(DataMasterObj, StockCodeStr[0], "STOCKINDEX");
				double StrikePrice = Double.parseDouble(StockCodeStr[1]);
				String CallOrPutStr = StockCodeStr[2];
				
				
				double OptionPrice = 0;
				if(OrderType.equals("BUY")) {
					OptionPrice = DataMasterObj.Quotes[StockIndex][2];														
				}
				else if(OrderType.equals("SELL")) {
					OptionPrice = DataMasterObj.Quotes[StockIndex][1];									
				}
				else {
					OptionPrice = DataMasterObj.Quotes[StockIndex][0];														
				}
				
				double NiftyPrice = DataMasterObj.Quotes[NiftyIndex][0];
				String ExpiryDate = UtilObj.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesCurrentExpiry[StockIndex]);					
				
				if(ExpiryType.equals("NextExpiry")) {
					if(OrderType.equals("BUY")) {
						OptionPrice = DataMasterObj.QuotesNextExpiry[StockIndex][2];										
					}
					else if(OrderType.equals("SELL")) {
						OptionPrice = DataMasterObj.QuotesNextExpiry[StockIndex][1];										
					}
					else {
						OptionPrice = DataMasterObj.QuotesNextExpiry[StockIndex][0];																
					}
					NiftyPrice = DataMasterObj.QuotesNextExpiry[NiftyIndex][0];
					ExpiryDate = UtilObj.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesNextExpiry[StockIndex]);										
				}
				
				//max error for nifty has to be 500 to be close to the convergence value
				double ErrorVal = NiftyPrice/9.0;				
				long DaysToExpiry = RiskMan.GetDiffToTodaysDate(ExpiryDate);
				
				ImpliedVol = StatObj.BlackScholesImpliedVolatility(NiftyPrice, StrikePrice, 0.08, DaysToExpiry/365.0, 0, OptionPrice, ErrorVal, CallOrPutStr);														
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return ImpliedVol;
		}
		

}
