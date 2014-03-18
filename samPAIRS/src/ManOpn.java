import java.util.*;
import java.io.*;

public class ManOpn {
	
	public void TradeCloseRecording(DataMaster DataMasterObj) {
		Utils UtilObj = new Utils();
		Stats stats = new Stats();
		String PositionFile = DataMasterObj.RootDirectory + "Position.txt";
		String CloseFile = DataMasterObj.RootDirectory + "ClosePosition.txt";
		String TradeFile = DataMasterObj.RootDirectory + "Trades.txt";
		String Trade2File = DataMasterObj.RootDirectory + "Trades2.txt";
		String MarketFile = DataMasterObj.RootDirectory + "Market.txt";
		ArrayList StocksTraded = new ArrayList();
		ArrayList TradeList = new ArrayList();
		int TradeSent = 0;
		String EndLine = System.getProperty("line.separator");
		try {			
			//read the pairs data from the Position sheet first
			ArrayList OpenPosition = UtilObj.LoadDataFromFile(PositionFile);
			ArrayList NewOpenPosition = new ArrayList();
		
			int UpdatePositionInTheFile = 0;
			for(int i=0;i<OpenPosition.size();i++) {
				
			String OpenPositionData[] = ((String)OpenPosition.get(i)).split("\t");	
			if (TradeSent == 0){
				String StockCode1 = OpenPositionData[1];
				String StockCode2 = OpenPositionData[2];
				String StockName1 = OpenPositionData[3];
				String StockName2 = OpenPositionData[4];
				
				//Finding the Trade open prices
				double TradeOpenPrice1 = Double.valueOf(OpenPositionData[5].trim()).doubleValue();
				double TradeOpenPrice2 = Double.valueOf(OpenPositionData[6].trim()).doubleValue();
				double TradeOpenRatio = TradeOpenPrice1/TradeOpenPrice2;

				//Get the index of the stocks in the Futures sheet
				int StockIndex1 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
				int StockIndex2 = (int) UtilObj.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");				

				int PairIndex = UtilObj.GetPairsIndex(StockCode1, StockCode2, DataMasterObj.Pairs);

				//cannot close on exchange close as the open position then is not appended
				//if exchange for this stock it not open then dont proceed further
				int ExchangeOpen1 = UtilObj.CheckExchangeRunning(DataMasterObj, StockIndex1);
				int ExchangeOpen2 = UtilObj.CheckExchangeRunning(DataMasterObj, StockIndex2);
				if(ExchangeOpen1 == 0 || ExchangeOpen2 == 0) {
					NewOpenPosition.add(OpenPosition.get(i));
					continue;
				}

				int PairsIndex = UtilObj.GetPairsIndex(StockCode1, StockCode2, DataMasterObj.Pairs);
				double LiveBidActual = UtilObj.GetQuotesData(DataMasterObj, StockCode1, "BID");
				double LiveAskActual = UtilObj.GetQuotesData(DataMasterObj, StockCode2, "ASK");
								
				double LiveBid = UtilObj.GetQuotesData(DataMasterObj, StockCode1, "BID");
				double LiveAsk = UtilObj.GetQuotesData(DataMasterObj, StockCode2, "ASK");
				
				//Get the LTP of the stock
				double LTP1 = UtilObj.GetQuotesData(DataMasterObj, StockCode1, "LTP");
				double LTP2 = UtilObj.GetQuotesData(DataMasterObj, StockCode2, "LTP");
				double LiveRatio = LiveBid/LiveAsk;
				//Calculating the Trade Return and the Trade PL
				double TradeReturn = (LiveRatio/TradeOpenRatio)-1;
				
				double Vol1=0; double Vol2=0;
				if(DataMasterObj.Param.VolStopLossMultiplier != 0) {
					Vol1 = UtilObj.GetVolatility(DataMasterObj, PairIndex);
					Vol2 = UtilObj.GetVolatility(DataMasterObj, PairIndex);
				}
				
				RiskManager RiskObj = new RiskManager();
				double ExpiryRisk = RiskObj.CalcLiveRisk(DataMasterObj, StockCode1, StockCode2);
				// check to see if the stock has been traded in the loop
				int StockRisk = 0;

				String ForceSell = OpenPositionData[11];
				
				//
				int MaxValueRisk = 1;
				String LotsData[] = ((String)OpenPositionData[9]).split("&");
				int Lots1 = Integer.parseInt(LotsData[0]);
				int Lots2 = Integer.parseInt(LotsData[1]);
				double Multiplier1 = UtilObj.GetMultiplier(StockCode1,DataMasterObj); 
				double Multiplier2 = UtilObj.GetMultiplier(StockCode2,DataMasterObj); 
				
				if (Lots1*LiveBid*Multiplier1 < DataMasterObj.Param.MaxTradeValue
					&& Lots2*LiveAsk*Multiplier2 < DataMasterObj.Param.MaxTradeValue){
					MaxValueRisk = 0;
				}
				
				
				// Check to see the no of trades for day have reached a limit
				int MaxTradeRisk = 0;
				int TradeNo = UtilObj.GetNoOfTrades(DataMasterObj);
				if (TradeNo < (DataMasterObj.Param.MaxTrades*DataMasterObj.Param.PermissibleVisits)){
					MaxTradeRisk = 0;
				}
				
				// Get Time from Trade open time 				
				//To make sure that trade is being closed at 60 min intervals only so that 
				//Trade opened at 11:46 AM should be closed at 12:46 PM, 13:46 PM and so on only - rolling candle
				//get the daily stats decision here
				String MainStockCode = StockCode1; 
				if(StockCode1.equals(DataMasterObj.FuturesCode[0].trim())) {MainStockCode = StockCode2;}								
				int MainStockIndex = (int) UtilObj.GetQuotesData(DataMasterObj, MainStockCode, "STOCKINDEX");			
				
				//**************** Condition for unwinding the trade goes here
				//Read Current & Prev Value of decision from RI.txt
				
				//if the vol stop loss exists then use this else use normal factor
				double CurrStopLoss = 0;
				double MaxVol = Math.max(Vol1, Vol2);
				if(DataMasterObj.Param.VolStopLossMultiplier != 0) {
					double VolStopLoss =  MaxVol*DataMasterObj.Param.VolStopLossMultiplier;
					CurrStopLoss = VolStopLoss;
				}				

				// ******* TRD Unwinding Flag from Indicator ******				
				// **** If Trd Open is "LONG" then CurrDecision should be "SHORT" 				
				int TrdCloseFlag = 0;
//				double FixedStopLoss = DataMasterObj.Param.StopLoss*MaxVol;
				double FixedStopLoss = DataMasterObj.Param.StopLoss;
				if(TradeReturn < (FixedStopLoss/-100)){
					TrdCloseFlag = 1; 
				}
				
				String LotData[] = ((String)OpenPositionData[9]).split("&");					
				int Lot1 = Integer.parseInt(LotData[0]);
				int Lot2 = Integer.parseInt(LotData[1]);		
				int NoOfVisits = Integer.parseInt(LotData[2]);

				double CurrBIDQty = DataMasterObj.BIDQty[StockIndex1];
				double CurrASKQty = DataMasterObj.ASKQty[StockIndex2];
				if(ExpiryRisk == 1) {
					CurrBIDQty = DataMasterObj.BIDQtyNextExpiry[StockIndex1];							
					CurrASKQty = DataMasterObj.ASKQtyNextExpiry[StockIndex2];
				}
				
				double ActualVolRatioOnBid = 0;
				double ActualVolRatioOnAsk = 0;
				if(Lot1 > 0 && Lot2 > 0) {
					ActualVolRatioOnBid = Math.min(1.0, CurrBIDQty/Lot1);
					ActualVolRatioOnAsk = Math.min(1.0, CurrASKQty/Lot2);							
				}
				double AvgVolAvailable = (ActualVolRatioOnBid+ActualVolRatioOnAsk)/2.0;
				
				int VolumeCutOffRisk = 0;
				if(AvgVolAvailable < DataMasterObj.Param.VolumeCutOff 
						&& DataMasterObj.Param.VolumeCutOff != 0 && CurrASKQty != 0 && CurrBIDQty != 0 && Lot1 != 0 && Lot2 != 0) {
					VolumeCutOffRisk = 1;
				}						

	
				double CurrDecisionMagnitude = DataMasterObj.DecisionMagnitudeInverse[PairIndex][0];
				int MeanValueReached = 0;
				if(CurrDecisionMagnitude > 0 && VolumeCutOffRisk == 0){
				//if(CurrDecision.equals("SHORT")){				
					MeanValueReached = 1; 
				}
				
				if(FixedStopLoss <= 0 || CurrStopLoss <= 0) {
					UtilObj.WriteLogFile("Fixed Stop Loss is 0 : Cannot proceed further...");
					continue;
					//System.exit(0);
				}


				//********************* Moving Stop Loss Logic****
				//access if the live LTP has crossed the moving stop loss value
				int StopLossReached = 0;
				double StopLossLimit  = 0;
				String PropertyString = OpenPositionData[10];
				String[] PropertyArray = PropertyString.split("&");
				
				//if the stock is in expiry risk and dont close the near month trades
				//the near month trades will be rolled over to next expiry
				int PositionWillBeRolledOver = 0;
				if(ExpiryRisk == 1) {
					String ExpiryDataForPosition = PropertyArray[3];
					//if this position is in tender period then - dont close it - rollit over first
					if(ExpiryDataForPosition.trim().equals(UtilObj.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesCurrentExpiry[MainStockIndex]))) {
						PositionWillBeRolledOver = 1;
					}
				}
				
				//check for the stop losses
				if(DataMasterObj.Param.MovingStopLoss.equals("ON")) {
					//if the trade is a short				
						StopLossLimit = CurrStopLoss;
						double MinOfTrade = Double.valueOf(PropertyArray[1]);
						double MaxOfTrade = Double.valueOf(PropertyArray[0]);		
						if( (100*((MaxOfTrade-LiveRatio)/LiveRatio)) > StopLossLimit && LiveRatio > 0 && MinOfTrade > 0 && StopLossLimit > 0) {
							StopLossReached = 1;
						}
					}
				//********************* End of moving Stop Loss Logic****
							
				//trade unwind intra-day condition
				Helper help = new Helper();

				String Trd_Opn_Time = OpenPositionData[0];	
				double CurrCandle = Double.parseDouble(PropertyArray[4]);
				double CandleLength = Double.parseDouble(PropertyArray[5]);
				
				//if the data is still that of RSI etc and gaps are not being made properly
				if(Double.isNaN(CurrCandle) || CurrCandle <= 5) {
					CurrCandle = DataMasterObj.BBGTimeSeriesGapForEachAsset[MainStockIndex];
				}
				int Trade_Time_Check = UtilObj.TradeOpnTimeCheck(Trd_Opn_Time,DataMasterObj,MainStockIndex,CurrCandle);
				double TradeTimeMultiple = UtilObj.TradeTimeMultiple(Trd_Opn_Time,DataMasterObj,MainStockIndex,CurrCandle);

				//check for the trade unwind due to time elapse
				if(Trade_Time_Check == 1 && DataMasterObj.Param.TimeStopLoss > 0) {
					if(TradeTimeMultiple >= Math.round(CandleLength * DataMasterObj.Param.TimeStopLoss)) {
						TrdCloseFlag = 1;
					}
				}
				
				long CurrTime = UtilObj.GetTimeInSeconds(Calendar.getInstance());
				if(CurrTime >= DataMasterObj.Param.TradeUnwindTime && DataMasterObj.Param.TradeUnwindTime != 0) {
					TrdCloseFlag = 1;
					Trade_Time_Check =1;
					PositionWillBeRolledOver = 0;
				}		
				

				if((LiveBidActual > 0 && LiveAskActual > 0 && LiveBid > 0 && LiveAsk > 0 && StockRisk == 0 && MaxValueRisk == 0 && MaxTradeRisk == 0)
						&& CurrTime < DataMasterObj.Param.ModelStopTime-DataMasterObj.Param.StopTradingBeforeModelStopTime
					&&( ( (MeanValueReached == 1) || (TrdCloseFlag == 1 && Trade_Time_Check ==1) || ForceSell.equals("FS") || (StopLossReached==1 && Trade_Time_Check ==1))
					&& UtilObj.CheckConnection(DataMasterObj) == 0 && PositionWillBeRolledOver == 0)) {

					String TradeOpenTime = OpenPositionData[0];
					//Lot sizes, CurreOpen lots and No of Visits in Open Positions
					int PrevOpenLot1 = Integer.parseInt(OpenPositionData[7]);
					int PrevOpenLot2 = Integer.parseInt(OpenPositionData[8]);
					

					String OpenTime = OpenPositionData[0];
					
					
					String TradeType="";
					String OrderType="";
					String StockName = "";
					double TradeClosePrice1 = LiveBid;
					double TradeClosePrice2 = LiveAsk;
					int Lot=0;
					
					
					double TradePrice1=0;
					double TradePrice2=0;
					if (!DataMasterObj.Param.RunningMode.equalsIgnoreCase("LIVE")){
						TradePrice1 = Math.round(1.3 * TradeClosePrice1);
						TradePrice2 = Math.round(0.7 * TradeClosePrice2);
					}
					
					String Expiry = "";
					String TradeExpiryStr = "";
					if (ExpiryRisk == 0){
						Expiry = "1";
						TradeExpiryStr = DataMasterObj.ExpiryDatesCurrentExpiry[MainStockIndex];
					}else{
						TradeExpiryStr = DataMasterObj.ExpiryDatesNextExpiry[MainStockIndex];
						Expiry = "2";
					}
					
					if(DataMasterObj.Param.LimitOrderExecution == 1) {					
//						double[] TradePriceVal = UtilObj.GetExecutionPrice(DataMasterObj, StockCode1, StockCode2, "SELL", "BUY", UtilObj.ConvertToDDMMMYYYY(PropertyArray[3]), Expiry);
//						TradePrice1 = TradePriceVal[0];
//						TradePrice2 = TradePriceVal[1];
						
//						TradePrice1 = UtilObj.GetExecutionPrice2(DataMasterObj, StockCode1, "SELL", UtilObj.ConvertToDDMMMYYYY(PropertyArray[3]), Expiry);		
//						TradePrice2 = UtilObj.GetExecutionPrice2(DataMasterObj, StockCode2, "BUY", UtilObj.ConvertToDDMMMYYYY(PropertyArray[3]), Expiry);		
						TradePrice1 = LiveBidActual;
						TradePrice2 = LiveAskActual;
					}

					String TradePriceStr1 = DataMasterObj.Param.PriceFormat.format(TradePrice1);
					String TradePriceStr2 = DataMasterObj.Param.PriceFormat.format(TradePrice2);
					//create the trade list here to be sent to the ModelImpl in AU
					//String TradeString = OrderType+"|"+DataMasterObj.Param.ClientCode+"|"+StockName+"|"+"|"+Lot+"|"+TradePriceStr+EndLine;
					String TradeString1 = "SELL"+"|"+DataMasterObj.InstrumentCode[StockIndex1]+"|"+StockName1+"|"+Expiry+"|"+Lot1+"|"+TradePriceStr1+"|"+DataMasterObj.Param.ClientCode+EndLine;										
					String TradeString2 = "BUY"+"|"+DataMasterObj.InstrumentCode[StockIndex2]+"|"+StockName2+"|"+Expiry+"|"+Lot2+"|"+TradePriceStr2+"|"+DataMasterObj.Param.ClientCode+EndLine;										

					if(DataMasterObj.Param.OptionsTradingActive == 1) {
						TradeString1 = "SELL"+"|"+DataMasterObj.InstrumentCode[StockIndex1]+"|"+StockName1+"|"+Expiry+"|"+Lot1+"|"+TradePriceStr1+"|"+DataMasterObj.Param.ClientCode;
						TradeString1 = OptionsHandler.GetFullTradeString(TradeString1, StockCode1);
						
						TradeString2 = "BUY"+"|"+DataMasterObj.InstrumentCode[StockIndex2]+"|"+StockName2+"|"+Expiry+"|"+Lot2+"|"+TradePriceStr2+"|"+DataMasterObj.Param.ClientCode;
						TradeString2 = OptionsHandler.GetFullTradeString(TradeString2, StockCode2);								
					}

					
					String OutputString1 = DataMasterObj.ExchangeCode[StockIndex1]+"|"+DataMasterObj.Param.ClientCode+"|"+StockName1+"|"+"|"+Lot1+"|"+TradePriceStr1;										
					String OutputString2 = DataMasterObj.ExchangeCode[StockIndex2]+"|"+DataMasterObj.Param.ClientCode+"|"+StockName2+"|"+"|"+Lot2+"|"+TradePriceStr2;										
					
					UpdatePositionInTheFile = 1;

					TradeReturn = (LiveRatio/TradeOpenRatio)-1;
					double TradePL = ((LiveBid-TradeOpenPrice1) * Lot1 * Multiplier1)+ ((TradeOpenPrice2 - LiveAsk) * Lot2 * Multiplier2);
					
					String TradePLStr = DataMasterObj.Param.PLFormat.format(TradePL);
					
					// Loading the Closed trades and also the open trades
					ArrayList ClosePosition = UtilObj.LoadDataFromFile(CloseFile);
					ArrayList NewClosePosition = new ArrayList();
					
					// Creating the Closed trade string to write into trades.txt
					String CloseTradeStr[] = ((String)OpenPosition.get(i)).split("\t");
					CloseTradeStr[7] = Integer.toString(Lot1);
					CloseTradeStr[8] = Integer.toString(Lot2);
					CloseTradeStr[9] = Lot1+"&"+Lot2+"&"+"1";
					
					if(TradeTimeMultiple >= Math.round(CandleLength/4.0) && DataMasterObj.Param.TimeStopLoss == 1) {
						CloseTradeStr[11] = "TU";
					}					
					if (TradeReturn < (FixedStopLoss/-100) || StopLossReached == 1){
						CloseTradeStr[11] = "SL";
					}
					if(MeanValueReached == 1) {
						CloseTradeStr[11] = "MR";						
					}

					String CurrCloseString = CloseTradeStr[0];
					for (int k=1;k<CloseTradeStr.length;k++){
						CurrCloseString = CurrCloseString + "\t"+CloseTradeStr[k];
					}
					CurrCloseString = CurrCloseString + "\t" + LiveBid + "\t" + LiveAsk + "\t"+TradeReturn+"\t"+TradePLStr+"\t"+UtilObj.NowDateTime(); 					
					String TradePosition1 = UtilObj.NowDateTime()+"\t"+"TradeClose"+"\t"+StockCode1+"\t"+DataMasterObj.FuturesCode[0]+"\t"+
					StockName1+"\t"+DataMasterObj.FuturesCode[0]+"\t"+LiveBid+"\t"+"1"+"\t"+
					Lot1+"\t"+"1"+"\t"+ TradeExpiryStr;
					
					String TradePosition2 = UtilObj.NowDateTime()+"\t"+"TradeClose"+"\t"+DataMasterObj.FuturesCode[0]+"\t"+StockCode2+"\t"+
					DataMasterObj.FuturesCode[0]+"\t"+StockName2+"\t"+"1"+"\t"+LiveAsk+"\t"+
					"1"+"\t"+Lot2+"\t"+ TradeExpiryStr;

					String UITradePosition1 = UtilObj.NowDateTime()+"\t"+"TradeClose"+"\t"+StockName1+"\t"+"SELL"+"\t"+Lot1+"\t"+TradeClosePrice1;
					String UITradePosition2 = UtilObj.NowDateTime()+"\t"+"TradeClose"+"\t"+StockName2+"\t"+"BUY"+"\t"+Lot2+"\t"+TradeClosePrice2;
					String UILogString1 = "CLOSING TRADE -> "+"ACTION:"+"SELL"+"     "+"CODE:"+StockName1+"     "+" Qty:"+Lot1;
					String UILogString2 = "CLOSING TRADE -> "+"ACTION:"+"BUY"+"     "+"CODE:"+StockName2+"     "+" Qty:"+Lot2;
					
					
					//Counter to check if a trade needs to be appended in Closed Sheet
					int CloseTradeAppend = 0;
					for(int j=0;j<ClosePosition.size();j++){
						String ClosePositionData[] = ((String)ClosePosition.get(j)).split("\t");
						//Checking to see if a portion of existing trade is already unwind in ClosePosition
						//matching the date and stock codes to find the trade
						if (OpenPositionData[0].equals(ClosePositionData[0]) && 
								ClosePositionData[1].equals(OpenPositionData[1]) && 
								ClosePositionData[2].equals(OpenPositionData[2])){ 
							CloseTradeAppend = 1;
							String ClosePositionDataStr = ClosePositionData[0];
							

							//Calculate the new average close prices and new closed quantities 
							double PrevClosePrice1 = Double.valueOf(ClosePositionData[12].trim()).doubleValue();
							double PrevClosePrice2 = Double.valueOf(ClosePositionData[13].trim()).doubleValue();
							
							//Closed positions previously and updating them
							int PrevCloseLot1 = Integer.parseInt(ClosePositionData[7]);
							int PrevCloseLot2 = Integer.parseInt(ClosePositionData[8]);
							int ClosedVisits = (PrevCloseLot1/Lot1)+1;
							int CloseLot1 = PrevCloseLot1 + Lot1;
							int CloseLot2 = PrevCloseLot2 + Lot2;

							// Calculating the new Avg prices for the trades
							double AvgClosePrice1 = ((PrevClosePrice1*PrevCloseLot1)+(LiveBid*Lot1))/(CloseLot1);
							double AvgClosePrice2 = ((PrevClosePrice2*PrevCloseLot2)+(LiveAsk*Lot2))/(CloseLot2);
							
							//Calculating the Trade Return and the Trade PL
							if (TradeType.equals("LONG")){
								TradeReturn = (AvgClosePrice1/TradeOpenPrice1)-1;
								TradePL = (AvgClosePrice1 - TradeOpenPrice1) * CloseLot1 * Multiplier1;
							}else{
								TradeReturn = 1-(AvgClosePrice2/TradeOpenPrice2);
								TradePL = (TradeOpenPrice2 - AvgClosePrice2) * CloseLot2 * Multiplier2;
								
							}
							TradePLStr = DataMasterObj.Param.PLFormat.format(TradePL);
							
							//Updating the prices and quantities to append the close position
							ClosePositionData[12] = Double.toString(AvgClosePrice1);
							ClosePositionData[13] = Double.toString(AvgClosePrice2);
							ClosePositionData[7] = Integer.toString(CloseLot1);
							ClosePositionData[8] = Integer.toString(CloseLot2);
							ClosePositionData[9] = Lot1 + "&"+Lot2+"&"+ClosedVisits;
							ClosePositionData[14] = Double.toString(TradeReturn);
							ClosePositionData[15] = TradePLStr;
							ClosePositionData[11] = "FS";
						
							//Creating the appended string
							for (int k=1;k<ClosePositionData.length;k++){
								ClosePositionDataStr = ClosePositionDataStr + "\t"+ClosePositionData[k];
							}
							NewClosePosition.add(ClosePositionDataStr);
						}
						// If the trade is not appended add it as it is
						else{
							NewClosePosition.add(ClosePosition.get(j));
						}

					}
					
					// Checking to see if there are open position left in Position sheet
					// after this visit of closing trade
					if (NoOfVisits >1){
						String OpenPositionDataStr = OpenPositionData[0];
						NoOfVisits = NoOfVisits - 1;
						OpenPositionData[7] = Integer.toString(PrevOpenLot1 - Lot1);
						OpenPositionData[8] = Integer.toString(PrevOpenLot2 - Lot2);
						OpenPositionData[9] = Integer.toString(Lot1)+"&"+Integer.toString(Lot2)+"&"+Integer.toString(NoOfVisits);
						for (int k=1;k<OpenPositionData.length;k++){
							OpenPositionDataStr = OpenPositionDataStr + "\t"+OpenPositionData[k];
						}
						//Appending the trade in Position Sheet
						NewOpenPosition.add(OpenPositionDataStr);
					}				
					
					//If a closed trades needs to be appended then open the file 
					//and write all the trades again
				    if (CloseTradeAppend == 1){
						//Write the first trade in ClosePosition

					    String CurrClosePosition = (String) NewClosePosition.get(0);
					    UtilObj.WriteToFile(CloseFile, CurrClosePosition, false);

					    //Write the rest of the trades in the file 
					    for (int k=1;k<NewClosePosition.size();k++){
						    CurrClosePosition = (String) NewClosePosition.get(k);
						    UtilObj.WriteToFile(CloseFile, CurrClosePosition, true);
					    }
				    }
				    //If there is not trade to append write the closed trade in a new line
				    else{
					    UtilObj.WriteToFile(CloseFile, CurrCloseString, true);
				    }
			    
				    //write the data into the trades.txt file
					DataMaster.logger.info("\r"+DataMasterObj.Param.CurrentCountry + ":\t"+TradePosition1);
					DataMaster.logger.info("\r"+DataMasterObj.Param.CurrentCountry + ":\t"+TradePosition2);
					// Write to the trade file
				    UtilObj.WriteToFile(TradeFile, TradePosition1, true);
				    UtilObj.WriteToFile(TradeFile, TradePosition2, true);
				    UtilObj.WriteToFile(Trade2File, TradePosition1, true);
				    UtilObj.WriteToFile(Trade2File, TradePosition2, true);
					
				    ArrayList UITradeList = UtilObj.LoadDataFromFile(DataMasterObj.Param.UITradesPath);
					UtilObj.WriteToFile(DataMasterObj.Param.UITradesPath, UITradePosition1, false);
					UtilObj.WriteToFile(DataMasterObj.Param.UITradesPath, UITradePosition2, true);
					for (int k=0;k<UITradeList.size();k++){
						String TempString = (String)UITradeList.get(k);
						UtilObj.WriteToFile(DataMasterObj.Param.UITradesPath, TempString, true);
					}
					UtilObj.WriteLogFile(UILogString1);
					UtilObj.WriteLogFile(UILogString2);
					//Sending the orders to the market if AutoMode is ON
					if (DataMasterObj.Param.AutoMode.equals("ON")){
						//Writing the Trade to socket and flushing it as well
						DataMasterObj.os.write(TradeString1);
						DataMasterObj.os.flush();
						DataMaster.logger.info("Trade Sent to the Market : " + TradeString1);
						DataMasterObj.os.write(TradeString2);
						DataMasterObj.os.flush();
						DataMaster.logger.info("Trade Sent to the Market : " + TradeString2);
						UtilObj.WriteLogFile("Trade Sent to the Market!");
						UtilObj.WriteToFile(MarketFile, TradeString1, true);
						UtilObj.WriteToFile(MarketFile, TradeString2, true);
					}

					
				}
				//else this trade is not unwounded and it remains in the open position
				else {
					if (ExpiryRisk == 0){
						NewOpenPosition.add(OpenPosition.get(i));
					}else{
						UpdatePositionInTheFile = 1;
						//OpenPositionData[11]="FS"; //if you make FS it continously opens and closes the trades
						OpenPositionData[11]="HL";						
						String ExpiryPosition = OpenPositionData[0];
						for (int k=1;k<OpenPositionData.length;k++){
							ExpiryPosition = ExpiryPosition + "\t" + OpenPositionData[k];
						}
						NewOpenPosition.add(ExpiryPosition);
					}
				}
				}
				else{
					NewOpenPosition.add(OpenPosition.get(i));
				}
			}
			
			//if the trade has been unwind then write the all the new array list to file
			if(UpdatePositionInTheFile == 1) {
				if (NewOpenPosition.size()>0){
					//first write the first position as a new position in the file
				    String FirstPosition = (String) NewOpenPosition.get(0);
				    UtilObj.WriteToFile(PositionFile, FirstPosition, false);
				   
				    //then append all the other positions to the file
				    for(int k=1;k<NewOpenPosition.size();k++) {
						String CurrPosition = (String) NewOpenPosition.get(k);
						UtilObj.WriteToFile(PositionFile, CurrPosition, true);

					}			    

				}
				//if there is only one trade then remove that trade from the file as an overwrite
				//and then wipe out all the positions from position.txt
				else {
					
					FileWriter fstream = new FileWriter(PositionFile, false);				
				    BufferedWriter out = new BufferedWriter(fstream);
				    out.close();
				}		
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			e.printStackTrace();
			UtilObj.WriteLogFile("There is a problem with the Close Position Sheet. Please CHECK!");
			System.exit(0);
		}

	}

}
