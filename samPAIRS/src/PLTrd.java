import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Calendar;


public class PLTrd {
		
	public void GeneratePLTrdMain(DataMaster DataMasterObj) {
		Utils util = new Utils();
		Stats stats = new Stats();
		String PositionFile = DataMasterObj.RootDirectory + "Position.txt";
		String TradeFile = DataMasterObj.RootDirectory + "Trades.txt";
		String Trade2File = DataMasterObj.RootDirectory + "Trades2.txt";
		String TrdFile = DataMasterObj.RootDirectory + "Trd.txt";
		String MarketFile = DataMasterObj.RootDirectory + "Market.txt";
		String EndLine = System.getProperty("line.separator");
		try {
			for(int i=0;i<DataMasterObj.Pairs.length;i++) {
				int StockRisk = 0;
				String StockCode1 = DataMasterObj.Pairs[i][0];
				String StockCode2 = DataMasterObj.Pairs[i][1];
								
				//Get the pair index 
				int PairIndex = util.GetPairsIndex(StockCode1, StockCode2, DataMasterObj.Pairs);
				
				double LiveAskActual = util.GetQuotesData(DataMasterObj, StockCode1, "ASK");
				double LiveBidActual = util.GetQuotesData(DataMasterObj, StockCode2, "BID");
				
				double LiveAsk = util.GetQuotesData(DataMasterObj, StockCode1, "ASK");
				double LiveBid = util.GetQuotesData(DataMasterObj, StockCode2, "BID");
				
				double LTP1 = util.GetQuotesData(DataMasterObj, StockCode1, "LTP");
				double LTP2 = util.GetQuotesData(DataMasterObj, StockCode2, "LTP");				
				
				double LiveRatio = LiveAsk/LiveBid;
				
			if (LiveAskActual > 0 && LiveBidActual > 0 && LiveAsk > 0 && LiveBid > 0){
				
				//Get the index of the stocks in the Futures sheet
				int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
				int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												
				
				//if exchange for this stock it not open then dont proceed further
				int ExchangeOpen1 = util.CheckExchangeRunning(DataMasterObj, StockIndex1);
				int ExchangeOpen2 = util.CheckExchangeRunning(DataMasterObj, StockIndex2);
				if(ExchangeOpen1 == 0 || ExchangeOpen2 == 0) {
					continue;
				}
				
				
				//finding the stock Names
				String StockName1 = DataMasterObj.Futures[StockIndex1][4];
				String StockName2 = DataMasterObj.Futures[StockIndex2][4];
				
				//*********** Live Z-Score Calculation here ************
				//getting 20 data points for z-score
												
				Calendar NowTime = Calendar.getInstance();
				int NowTimeSeconds = util.GetTimeInSeconds(NowTime);
				
				double CurrVol = 0;
				if(DataMasterObj.Param.VolStopLossMultiplier != 0) {
					double Vol1 = util.GetVolatility(DataMasterObj, PairIndex);
					double Vol2 = util.GetVolatility(DataMasterObj, PairIndex);					
					CurrVol = Math.max(Vol1, Vol2);
				}
				
				// Get Curent & Prev Decision values 
				String[] Decisions = new String[DataMasterObj.Decision[0].length]; 					
				for(int kk=0;kk<DataMasterObj.Decision[0].length;kk++) {
					Decisions[kk] = DataMasterObj.Decision[PairIndex][kk];
				}

				//for checking the last time to take position on
				Helper help = new Helper();
				int IntraDayLastTimeElapsed = 0;
				long CurrTime = util.GetTimeInSeconds(Calendar.getInstance());
				if(DataMasterObj.Param.TradeUnwindTime != 0) {
					if(CurrTime >= DataMasterObj.Param.TradeUnwindTime) {
						IntraDayLastTimeElapsed = 1;
					}						
				}
				
				//for checking the bet sizing and adding to the positions here
				//this will also get the bet sized number of reference
				double[] FinalBetSizingDecision = GetBetSizingDecision(DataMasterObj, StockCode1, StockCode2, Decisions, CurrVol);								

				//get the daily stats decision here
				String MainStockCode = StockCode1; 
				if(StockCode1.equals(DataMasterObj.FuturesCode[0].trim())) {MainStockCode = StockCode2;}								
				int MainStockIndex = (int) util.GetQuotesData(DataMasterObj, MainStockCode, "STOCKINDEX");			
				//double CurrDailyStatsDecision = DataMasterObj.DailyStatsDecision[MainStockIndex];				
				
				//if the trade has been unwinded intraday then take fresh positions at the start of the day
				//if the trade has to be carried over as LLL RI Indicator from previous day then use this
				if(DataMasterObj.Param.CarryOverTradesAtMktOpen == 1 && 
						DataMasterObj.TradeCarryOverDoneForThisPair[PairIndex] == 0 &&
						DataMasterObj.Param.TradeUnwindTime != 0 && 
						DataMasterObj.Param.TradeUnwindTime < DataMasterObj.Param.ModelStopTime) {
						
						int IntraDayFirstTradeSatisfied = GetFirstTradeSatsified(DataMasterObj, Decisions);						
						if(IntraDayFirstTradeSatisfied == 1) {
							FinalBetSizingDecision[0] = 1;
							FinalBetSizingDecision[1] = 0;					
						}					
						
						//make the carry over decision as 1 - as this decision is taken only once at the beginning of the market
						DataMasterObj.TradeCarryOverDoneForThisPair[PairIndex] = 1;
				}
				
				//dont open new trades on commodities on saturdays
				int SpecialRestrictionOnTrades = 0;
				if(DataMasterObj.Param.OpenTradesOnSaturday == 0 
						&& Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7) {					
					SpecialRestrictionOnTrades = 1;
				}
				
				if(DataMasterObj.Param.OptionsTradingActive == 1) {
					OptionsHandler OptionsHandlerObj = new OptionsHandler();
					SpecialRestrictionOnTrades = OptionsHandlerObj.IsOptionsRestricted(DataMasterObj, StockCode1, StockCode2);
					//if there is no restrictions on options trades then get the net quantity of options to be traded
					if(SpecialRestrictionOnTrades == 0) {
						OptionsHandlerObj.SetOptionsQuantity(DataMasterObj, StockIndex1, StockIndex2, StockCode1, StockCode2, PairIndex);
					}				
				}
				
				if(FinalBetSizingDecision[0] > 0) {
					int aa = 10;
				}

								
				if (FinalBetSizingDecision[0] > 0 && IntraDayLastTimeElapsed == 0
						&& CurrTime < DataMasterObj.Param.LastTradeOpenTime
						&& CurrTime < DataMasterObj.Param.ModelStopTime-DataMasterObj.Param.StopTradingBeforeModelStopTime //dont take trades in last 60 sec - as they miss the exchange time
						&& SpecialRestrictionOnTrades == 0   //no new trades to be done on saturday
						)
				{								
						RiskManager RiskMan = new RiskManager();
						int ExpiryRisk = RiskMan.CalcLiveRisk(DataMasterObj, StockCode1, StockCode2);
						//Calculating the lot size of both stocks
						int Lot1 = (int)(DataMasterObj.PLot1[PairIndex][0]);
						int Lot2 = (int)(DataMasterObj.PLot1[PairIndex][1]);
						
						
						//make the lots rupee neutral
						if(DataMasterObj.Param.OptionsTradingActive == 0) {
							double Value1 = Lot1*LTP1;
							double Value2 = Lot2*LTP2;
							if(Value1 > 0 && Value2 > 0) {						
								int Lot1New = (int)Math.round(DataMasterObj.Param.OrderValue / (LTP1*Lot1)) * Lot1;
								int Lot2New = (int)Math.round((Lot1New*LTP1) / (LTP2*Lot2)) * Lot2;												
								
								Lot1 = Lot1New;
								Lot2 = Lot2New;
							}							
						}
						
						//logic to see if enough volume is available on bid/offer to snap the pair or not
						double CurrASKQty = DataMasterObj.ASKQty[StockIndex1];
						double CurrBIDQty = DataMasterObj.BIDQty[StockIndex2];
						if(ExpiryRisk == 1) {
							CurrASKQty = DataMasterObj.ASKQtyNextExpiry[StockIndex1];							
							CurrBIDQty = DataMasterObj.BIDQtyNextExpiry[StockIndex2];
						}
						
						double ActualVolRatioOnBid = 0;
						double ActualVolRatioOnAsk = 0;
						if(Lot1 > 0 && Lot2 > 0) {
							ActualVolRatioOnBid = Math.min(1.0, CurrASKQty/Lot1);
							ActualVolRatioOnAsk = Math.min(1.0, CurrBIDQty/Lot2);							
						}
						double AvgVolAvailable = (ActualVolRatioOnBid+ActualVolRatioOnAsk)/2.0;
						
						int VolumeCutOffRisk = 0;
						if(AvgVolAvailable < DataMasterObj.Param.VolumeCutOff 
								&& DataMasterObj.Param.VolumeCutOff != 0 && CurrASKQty != 0 && CurrBIDQty != 0 && Lot1 != 0 && Lot2 != 0) {
							VolumeCutOffRisk = 1;
						}						
																		
						//No of visits already had today
						//int NoOfVisits = RiskMan.GetNoOfVisits(DataMasterObj, StockCode1, StockCode2, Lot1, Lot2);
						String ForceSell = RiskMan.GetForceSell(DataMasterObj, StockCode1, StockCode2);
												
						//check to see if a single value does not e
						int MaxValueRisk = 1;
						double Multiplier1 = util.GetMultiplier(StockCode1,DataMasterObj); 
						double Multiplier2 = util.GetMultiplier(StockCode2,DataMasterObj); 
						int NoOfVisits = 0;
						
						if (Lot1*LiveAsk*Multiplier1< DataMasterObj.Param.MaxTradeValue
							&& Lot2*LiveBid *Multiplier2 <DataMasterObj.Param.MaxTradeValue){
							MaxValueRisk = 0;
						}						
						String RiskStr;
						
						int MaxTradeRisk = 1;
						int TradeNo = util.GetNoOfTrades(DataMasterObj);
						if (TradeNo < (DataMasterObj.Param.MaxTrades*DataMasterObj.Param.PermissibleVisits)){
							MaxTradeRisk = 0;
						}
						//Checking for all the risk
						if (Lot1 > 0 && Lot2 > 0 
								&& VolumeCutOffRisk == 0
								&& ExpiryRisk < 1000 
								&& StockRisk == 0
								&& MaxValueRisk == 0
								&& MaxTradeRisk == 0
								&& ForceSell.equals("HL")
								&& util.CheckConnection(DataMasterObj) == 0) {
							
							String Expiry = "";
							String ExpiryStr = "";
							String TradeExpiryStr = "";
							if (ExpiryRisk == 0){
								Expiry = "1";
								TradeExpiryStr = DataMasterObj.ExpiryDatesCurrentExpiry[MainStockIndex];
								ExpiryStr = util.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesCurrentExpiry[MainStockIndex]);
							}else{
								Expiry = "2";
								TradeExpiryStr = DataMasterObj.ExpiryDatesNextExpiry[MainStockIndex];
								ExpiryStr = util.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesNextExpiry[MainStockIndex]);							
							}
							
							double TradeOpenPrice1 = LiveAsk;
							double TradeOpenPrice2 = LiveBid;
							double TradePrice1 = 0;
							double TradePrice2 = 0;
							double TradePrice = 0;
							if(DataMasterObj.Param.LimitOrderExecution == 1) {
								//double[] TradePriceVal = util.GetExecutionPrice(DataMasterObj, StockCode1, StockCode2, "BUY", "SELL", TradeExpiryStr, Expiry);								
								//TradePrice1 = TradePriceVal[0];
								//TradePrice2 = TradePriceVal[1];								
								//TradePrice1 = util.GetExecutionPrice2(DataMasterObj, StockCode1, "BUY", TradeExpiryStr, Expiry);		
								//TradePrice2 = util.GetExecutionPrice2(DataMasterObj, StockCode2, "SELL", TradeExpiryStr, Expiry);																		
								TradePrice1 = LiveAskActual;
								TradePrice2 = LiveBidActual;
							}

							if (!DataMasterObj.Param.RunningMode.equalsIgnoreCase("LIVE")){
									TradePrice1 = Math.round(0.7 * TradeOpenPrice1);
									TradePrice2 = Math.round(1.3 * TradeOpenPrice2);
							}
							String TradePriceStr1 = DataMasterObj.Param.PriceFormat.format(TradePrice1);
							String TradePriceStr2 = DataMasterObj.Param.PriceFormat.format(TradePrice2);
							//Creating the string to send the order to the market
							//String TradeString = OrderType+"|"+DataMasterObj.Param.ClientCode+"|"+StockName+"|"+"|"+Lot+"|"+TradePriceStr+EndLine;
							String TradeString1 = "BUY"+"|"+DataMasterObj.InstrumentCode[StockIndex1]+"|"+StockName1+"|"+Expiry+"|"+Lot1+"|"+TradePriceStr1+"|"+DataMasterObj.Param.ClientCode+EndLine;
							String TradeString2 = "SELL"+"|"+DataMasterObj.InstrumentCode[StockIndex2]+"|"+StockName2+"|"+Expiry+"|"+Lot2+"|"+TradePriceStr2+"|"+DataMasterObj.Param.ClientCode+EndLine;

							if(DataMasterObj.Param.OptionsTradingActive == 1) {
								TradeString1 = "BUY"+"|"+DataMasterObj.InstrumentCode[StockIndex1]+"|"+StockName1+"|"+Expiry+"|"+Lot1+"|"+TradePriceStr1+"|"+DataMasterObj.Param.ClientCode;
								TradeString1 = OptionsHandler.GetFullTradeString(TradeString1, StockCode1);
								
								TradeString2 = "SELL"+"|"+DataMasterObj.InstrumentCode[StockIndex2]+"|"+StockName2+"|"+Expiry+"|"+Lot2+"|"+TradePriceStr2+"|"+DataMasterObj.Param.ClientCode;
								TradeString2 = OptionsHandler.GetFullTradeString(TradeString2, StockCode2);								
							}

							
							String UILogString1 = "NEW TRADE OPEN " + " -> "+"ACTION:BUY"+"     "+"CODE:"+StockName1+"     "+"QTY:"+Lot1;
							String UILogString2 = "NEW TRADE OPEN " + " -> "+"ACTION:SELL"+"     "+"CODE:"+StockName2+"     "+"QTY:"+Lot2;
//							String OutputString = DataMasterObj.ExchangeCode[MainStockIndex]+"|"+DataMasterObj.Param.ClientCode+"|"+StockName+"|"+"|"+Lot+"|"+TradePriceStr;														

							//no of visits will always be 1 for bet sized results
							NoOfVisits = 1;
							
							//Creating the string to write in the trade sheet
							String CurrOpenPosition = util.NowDateTime()+"\t"+StockCode1 + "\t" + StockCode2 + "\t" + 
							StockName1 + "\t"+ StockName2 + "\t"+LiveAsk+"\t"+LiveBid+"\t"+
							Lot1 +"\t"+Lot2+"\t"+Lot1+"&"+Lot2+"&"+NoOfVisits+"\t"+
							LiveRatio+"&" +LiveRatio+"&"+
							Expiry+"&"+ExpiryStr+"&" +
							DataMasterObj.Param.BBGTimeSeriesGap+"&"+DataMasterObj.CurrDecisionLength[PairIndex]+"&"+DataMasterObj.OUValues[PairIndex]+"&"+
							DataMasterObj.VRTValues[PairIndex]+"&"+DataMasterObj.DecisionMagnitude[PairIndex][0]+"&"+DataMasterObj.DecisionMagnitude[PairIndex][1]+"&"+CurrASKQty+"&"+CurrBIDQty+"&"+AvgVolAvailable+"&"+FinalBetSizingDecision[0]+"&"+FinalBetSizingDecision[1] + "\t"+"HL";
	
							String TradePosition1 = util.NowDateTime()+"\t"+"TradeOpen"+"\t"+StockCode1+"\t"+DataMasterObj.FuturesCode[0]+"\t"+
							StockName1+"\t"+DataMasterObj.FuturesCode[0]+"\t"+LiveAsk+"\t"+"1"+"\t"+
							Lot1+"\t"+"1"+"\t"+TradeExpiryStr;
						
							String TradePosition2 = util.NowDateTime()+"\t"+"TradeOpen"+"\t"+DataMasterObj.FuturesCode[0]+"\t"+StockCode2+"\t"+
							DataMasterObj.FuturesCode[0]+"\t"+StockName2+"\t"+"1"+"\t"+LiveBid+"\t"+
							"1"+"\t"+Lot2+"\t"+TradeExpiryStr;

							String UITradePosition1 = util.NowDateTime()+"\t"+"TradeOpen"+"\t"+StockName1+"\t"+"BUY"+"\t"+Lot1+"\t"+TradeOpenPrice1;
							String UITradePosition2 = util.NowDateTime()+"\t"+"TradeOpen"+"\t"+StockName2+"\t"+"SELL"+"\t"+Lot2+"\t"+TradeOpenPrice2;
							//If its the first visit write directly to the Position sheet
							if (NoOfVisits == 1){							
								util.WriteToFile(PositionFile, CurrOpenPosition, true);	
							}
							//Find the previous trade in the position sheet and append that 
							else{
								ArrayList Position = util.LoadDataFromFile(PositionFile);
								ArrayList NewPosition = new ArrayList();
								for(int j=0;j<Position.size();j++){
									String PositionData[] = ((String)Position.get(j)).split("\t");
									Calendar TodayDate = Calendar.getInstance();
									String TodayD = util.DateString(TodayDate);
									String TradeOpenDate = PositionData[0].substring(0,8);
									if (TodayD.equals(TradeOpenDate.trim()) && 
											StockCode1.equals(PositionData[1]) && 
											StockCode2.equals(PositionData[2])){//matching the date and stock codes to find the trade 
										String PositionDataStr = PositionData[0];
										double PrevPrice1 = Double.valueOf(PositionData[5].trim()).doubleValue();
										double PrevPrice2 = Double.valueOf(PositionData[6].trim()).doubleValue();									
										int PrevLot1 = Integer.parseInt(PositionData[7]);
										int PrevLot2 = Integer.parseInt(PositionData[8]);
										
										// Calculating the new Avg prices for the trades
										double AvgPrice1 = ((PrevPrice1*PrevLot1)+(LiveAsk*Lot1))/(Lot1+PrevLot1);
										double AvgPrice2 = ((PrevPrice2*PrevLot2)+(LiveBid*Lot2))/(Lot2+PrevLot2);
										
										//Updating the prices and quantities to write 
										PositionData[5] = Double.toString(AvgPrice1);
										PositionData[6] = Double.toString(AvgPrice2);
										PositionData[7] = Integer.toString(PrevLot1+Lot1);
										PositionData[8] = Integer.toString(PrevLot2+Lot2);
										PositionData[9] = Lot1 + "&"+Lot2+"&"+NoOfVisits;
										//Creating the appended string
										for (i=1;i<PositionData.length;i++){
											PositionDataStr = PositionDataStr + "\t"+PositionData[i];
										}
										NewPosition.add(PositionDataStr);
									}
									else{
										NewPosition.add(Position.get(j));
									}
								}
								
								// Writing the first position
							    String CurrPosition = (String) NewPosition.get(0);
								util.WriteToFile(PositionFile, CurrPosition, false);
	
							    //Writing the rest of the positions.
	
							    for(i=1;i<NewPosition.size();i++) {
									CurrPosition = (String) NewPosition.get(i);
									util.WriteToFile(PositionFile, CurrPosition, true);
							    }
							    
							}
	
							DataMaster.logger.info(DataMasterObj.Param.CurrentCountry + ":\t"+TradePosition1);
							DataMaster.logger.info(DataMasterObj.Param.CurrentCountry + ":\t"+TradePosition2);

							//write the trade to the trades.txt file
							util.WriteToFile(TradeFile, TradePosition1, true);
							util.WriteToFile(TradeFile, TradePosition2, true);
							util.WriteToFile(Trade2File, TradePosition1, true);
							util.WriteToFile(Trade2File, TradePosition2, true);
							ArrayList UITradeList = util.LoadDataFromFile(DataMasterObj.Param.UITradesPath);
							util.WriteToFile(DataMasterObj.Param.UITradesPath, UITradePosition1, false);
							util.WriteToFile(DataMasterObj.Param.UITradesPath, UITradePosition2, true);
							for (int k=0;k<UITradeList.size();k++){
								String TempString = (String)UITradeList.get(k);
								util.WriteToFile(DataMasterObj.Param.UITradesPath, TempString, true);
							}
							RiskStr = "Done";
							String TrdString = util.NowDateTime()+"\t"+StockCode1+"\t"+StockCode2+"\t"+
							StockName1+"\t"+StockName2+"\t"+LiveAsk+"\t"+LiveBid+"\t"+Lot1+"\t"+Lot2+"\t"+RiskStr;
							util.WriteToFile(TrdFile, TrdString, true);
							util.WriteLogFile(UILogString1);
							util.WriteLogFile(UILogString2);
							DataMasterObj.Trd[PairIndex]=1;

							//Sending the orders to the market if AutoMode is ON
							if (DataMasterObj.Param.AutoMode.equals("ON")){
								//Writing the Trade to socket and flushing it as well
								DataMasterObj.os.write(TradeString1);
								DataMasterObj.os.flush();
								DataMaster.logger.info(DataMasterObj.Param.CurrentCountry+" : Trade Sent to the Market : " + TradeString1);
								DataMasterObj.os.write(TradeString2);
								DataMasterObj.os.flush();
								DataMaster.logger.info(DataMasterObj.Param.CurrentCountry+" : Trade Sent to the Market : " + TradeString2);
								util.WriteLogFile("Trade Sent to the Market");
								util.WriteToFile(MarketFile, TradeString1, true);
								util.WriteToFile(MarketFile, TradeString2, true);
								//Writing the Trade to socket and flushing it as well
							}

						} //end of risk check
						else if(VolumeCutOffRisk !=0){
							RiskStr = "VolumeCutOffRisk_"+AvgVolAvailable;
						}						
						else if(ExpiryRisk > 1000){
								RiskStr = "ExpiryRisk";
							}
						else if(MaxValueRisk !=0){
							RiskStr = "MaxValue";
						}
						else if(NoOfVisits >=DataMasterObj.Param.PermissibleVisits){
							RiskStr = "MaxVisit";
						}
						else if(MaxTradeRisk != 0){
							RiskStr = "MaxTrades";
						}
						else if(StockRisk !=0){
							RiskStr = "StockRisk";
						}
						else{
							RiskStr="Error";
						}
						if (DataMasterObj.Trd[PairIndex] == 0 && !RiskStr.equals("Done")){
							String TrdString = util.NowDateTime()+"\t"+StockCode1+"\t"+StockCode2+"\t"+
							StockName1+"\t"+StockName2+"\t"+LiveAsk+"\t"+LiveBid+"\t"+Lot1+"\t"+Lot2+"\t"+RiskStr;
							util.WriteToFile(TrdFile, TrdString, true);
							DataMasterObj.Trd[PairIndex]=1;
						}
					}	//end of trade open condition check			
			}		
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			e.printStackTrace();
			util.WriteLogFile("There is a problem with the Open Position Sheet. Please CHECK!");
			DataMasterObj.StopStrategy = true;
		}
	}
	
	
	// Function for Getting Result for Given Bet Sizing 
	public static int GetBetSizeResult(String CurrBetStr, String[] DecisionArray) {
		int TradeTrueFlag = 0 ; 	
		try{			
			int sum = 0 ;			
			for(int k= 0 ; k<CurrBetStr.length(); k++){
				if(CurrBetStr.substring(k, k+1).equals(DecisionArray[k])){
					sum = sum + 1; 
				}
			}			
			if(sum == CurrBetStr.length()){
				TradeTrueFlag = 1; 
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			e.printStackTrace();
		}
		return TradeTrueFlag;
	}
	
	public static double[] GetBetSizingDecision(DataMaster DataMasterObj, String StockCode1, String StockCode2, String[] DecisionArray, double CurrVol) {
		double[] BetSizeFine = new double[2];
		//in 0 will be the totalvisit and in 1 will be the perc return
		BetSizeFine[0] = 0;
		BetSizeFine[1] = 0;
		
		try {
				//check if the position is a repeated one for this day or not
				Utils util = new Utils();
				int[] TotalVisits = new int[DataMasterObj.Param.PermissibleVisits]; 
				double[] LastPrice1= new double[DataMasterObj.Param.PermissibleVisits];
				double[] LastPrice2= new double[DataMasterObj.Param.PermissibleVisits];
				int IndexOfCurrBetToIncrease = 0;
				double LastBetPrice1 = 0;
				double LastBetPrice2 = 0;
				
				//initialize all values to 0
				for(int i=0;i<DataMasterObj.Param.PermissibleVisits;i++){
					TotalVisits[i] = 0;
					LastPrice1[i] = 0;
					LastPrice2[i] = 0;
				}

				try	{
					ArrayList Position = util.LoadDataFromFile(DataMasterObj.RootDirectory + "Position.txt");
					for(int j=0;j<Position.size();j++){
						//get the bet value of the position first
						String PositionData[] = ((String)Position.get(j)).split("\t");
						String ParaString = PositionData[10];
						String[] ParaValues = ParaString.split("&");
						int PosBetValue = (int) (Double.parseDouble(ParaValues[ParaValues.length-2]));
						
						//use the bet value and position code to fill the visits for each case
						if (StockCode1.equals(PositionData[1]) && 
								StockCode2.equals(PositionData[2])){
							
							TotalVisits[PosBetValue-1] = TotalVisits[PosBetValue-1]+1;
							LastPrice1[PosBetValue-1] = Double.valueOf(PositionData[5]);
							LastPrice2[PosBetValue-1] = Double.valueOf(PositionData[6]);						
						}										
					}

					//get the perm visit qty from the parameters string for bet sizing
					String[] PermissibleVisitStr = DataMasterObj.Param.PermissibleVisitsQty.split(",");
					//check if the perm qty is equal to perm visit or not
					if(PermissibleVisitStr.length < DataMasterObj.Param.PermissibleVisits) {
						util.WriteLogFile("Bet Size Permissble Qty is less then Permissible Visit : Cannot bet size");
						DataMasterObj.StopStrategy = true;
					}
					int[] PermissibleVisitQty = new int[PermissibleVisitStr.length];
					for(int i=0;i<PermissibleVisitStr.length;i++) {
						PermissibleVisitQty[i] = Integer.parseInt(PermissibleVisitStr[i]);
					}
					
					//do the check to see which perm visit is satisfied for bet sizing
					for(int i=0;i<DataMasterObj.Param.PermissibleVisits;i++) {
						if(TotalVisits[i] < PermissibleVisitQty[i]) {
							//this is the index to bet size on : can be 0,1,2 etc.
							IndexOfCurrBetToIncrease = i;
							
							//take the last bet price to increase the next betting
							if(i==0) {
								LastBetPrice1 = 0;
								LastBetPrice2 = 0;
							}
							else {
								LastBetPrice1 = LastPrice1[i-1];
								LastBetPrice2 = LastPrice2[i-1];																
							}
							
							//get out of the loop as this bet next needs to be filled
							break;
						}
						//else the current bet is completely filled and dont fill it any more
						else {
							IndexOfCurrBetToIncrease = DataMasterObj.Param.PermissibleVisits;
						}
					}
					
					//see if the total visits is less then the pesmissible visits for this stock
					if(IndexOfCurrBetToIncrease < DataMasterObj.Param.PermissibleVisits) {
						//there is still scope to add to the LS here and check that condition
						String ModelType = DataMasterObj.Param.ModelType;
						//ModelType = GetModelType(DataMasterObj, StockCode1, StockCode2);
						String NewModelType = ModelType;
						//add extra LLS to this number to meet the L+LS criteria for bet sizing
						for(int i=0;i<IndexOfCurrBetToIncrease;i++) {
							NewModelType = "L" + NewModelType;
						}
						
						//the new position has to meet the new model type for it to be a bet sized product
//						int BetSizeSatisfied = PLTrd.GetBetSizeResult(NewModelType, DecisionArray);
						int BetSizeSatisfied = PLTrd.CheckDecisionMagnitude(DataMasterObj, NewModelType, StockCode1, StockCode2);
						
						//also check if the price traded at is better then the previous price by a certain margin or not						
						//if both prices are 0 then this is the first visit and add as it is
						if(BetSizeSatisfied == 1 && LastBetPrice1 == 0 && LastBetPrice2 == 0) {
							//correlation needs to be checked only for the first time the stock comes into the p0osi
							double CorrelRisk = 0;
							if(DataMasterObj.Param.CriticalCorrelValue != 0) {
								CorrelRisk = util.CheckCorrelRisk(DataMasterObj, StockCode1, StockCode2, Position); 								
							}
							
							//only if the position correlation risk does not exist then execute the trade
							if(CorrelRisk == 0) {
								BetSizeFine[0] = 1;
								BetSizeFine[1] = 0;								
							}
							//else record the correl risk in sample.txt file
							else {
								//Get the index of the stocks in the Futures sheet
								int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
								int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");				
								
								//finding the stock Names
								String StockName1 = DataMasterObj.Futures[StockIndex1][4];
								String StockName2 = DataMasterObj.Futures[StockIndex2][4];

								String SampleStr = util.NowDateTime() + "\t" + StockName1 + "\t" + StockName2 + "\t" + "CorrelRisk:" + CorrelRisk; 
								util.WriteToFile(DataMasterObj.Param.NotesPath, SampleStr, true);
							}
						}
						else {
							double LTP1 = util.GetQuotesData(DataMasterObj, StockCode1, "LTP");
							double LTP2 = util.GetQuotesData(DataMasterObj, StockCode2, "LTP");														
							double DiffInPrice = 100*((LTP1-LastBetPrice1)/LastBetPrice1+(LastBetPrice2-LTP2)/LastBetPrice2);
							
							double CriticalValueForBetSize = DataMasterObj.Param.PercToBetSizeAt;
							//if the vol multiplier exists then use this multiplier to bet size
							if(DataMasterObj.Param.VolStopLossMultiplier != 0) {
								//CriticalValueForBetSize = DataMasterObj.Param.PercToBetSizeAt*CurrVol;
								CriticalValueForBetSize = Math.min(DataMasterObj.Param.StopLoss, DataMasterObj.Param.PercToBetSizeAt*CurrVol);	
								
								//do some error checks here and throw error is these critical values are 0
								if(DataMasterObj.Param.StopLoss <= 0) {
									util.WriteLogFile("ERROR: Stop Loss is Zero, cannot proceed further ...");
									DataMasterObj.StopStrategy = true;
								}
								if(DataMasterObj.Param.PercToBetSizeAt <= 0) {
									util.WriteLogFile("ERROR: Bet Size Perc is zero, cannot proceed further ...");									
									DataMasterObj.StopStrategy = true;
								}
							}
							
							//this is where bets > 1 is increased further
							if(DiffInPrice >= CriticalValueForBetSize && BetSizeSatisfied == 1 && CriticalValueForBetSize > 0) {								
								BetSizeFine[0] = IndexOfCurrBetToIncrease+1;
								BetSizeFine[1] = DiffInPrice;
							}														
						}
					}
			}
			catch(Exception e) {
				DataMaster.logger.warning(e.toString());
				e.printStackTrace();
				util.WriteLogFile("There is a problem with the Bet Calculations Sheet. Please CHECK!");
				DataMasterObj.StopStrategy = true;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return BetSizeFine;
	}
	
	public int GetFirstTradeSatsified(DataMaster DataMasterObj, String[] DecisionArray) {
		int FirstTradeSatistfied = 0;
		try {
			Utils util = new Utils();
			ArrayList<String> HighFreqDataAll = util.LoadDataFromFile(DataMasterObj.Param.HighFreqDataFilePath);
			int HighFreqIndex = HighFreqDataAll.size()-1;
			
			String PrevRowDate = util.GetDateValueForRowIndex(HighFreqDataAll, HighFreqIndex-1);		
			String CurrRowDate = util.GetDateValueForRowIndex(HighFreqDataAll, HighFreqIndex);
						
			//if the current date is different from the prev date then return LLL as the decision to start a new trade
			if(! CurrRowDate.equals(PrevRowDate)) {
				if(DecisionArray[0] != null 
						&& DecisionArray[0].equals("L")
						&& DecisionArray[1].equals("L")
						&& DecisionArray[2].equals("L")) {
						FirstTradeSatistfied = 1;
				}
			}						
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return FirstTradeSatistfied;
	}
	
	
	
	//the model type has to be made as per the volatility of the asset class
    public static String GetModelType(DataMaster DataMasterObj, String StockCode1, String StockCode2) {
          String ModelType = "";
          try {
                Utils util = new Utils();
                //Get the index of the stocks in the Futures sheet
                String MainStockCode = StockCode1; 
                if(StockCode1.equals(DataMasterObj.FuturesCode[0].trim())) {MainStockCode = StockCode2;}                                                
                int MainStockIndex = (int) util.GetQuotesData(DataMasterObj, MainStockCode, "STOCKINDEX");                

//                double CurrVol = util.GetVolatility(DataMasterObj, MainStockIndex);
//                double CurrBBGTimeSeriesGap = DataMasterObj.BBGTimeSeriesGapForEachAsset[MainStockIndex];
//                double CurrMarketTimeinHours = DataMasterObj.MarketTimeInHoursForEachAsset[MainStockIndex];
//                double CurrAnnualVol = (CurrVol)*Math.sqrt((CurrMarketTimeinHours*60)/CurrBBGTimeSeriesGap)*Math.sqrt(252);                                    
//                
//                int ModelNumInt = 0;
//                if(CurrAnnualVol <= 20)ModelNumInt=6;
//                if(CurrAnnualVol > 20 && CurrAnnualVol <= 30)ModelNumInt=5;
//                if(CurrAnnualVol > 30 && CurrAnnualVol <= 40)ModelNumInt=4;
//                if(CurrAnnualVol > 40 && CurrAnnualVol <= 50)ModelNumInt=3;             
//                if(CurrAnnualVol > 50)ModelNumInt=2;
                
    			double OUStats = DataMasterObj.OUValues[MainStockIndex];				
    			int ModelNumInt = 0;
    			if(OUStats > 9)ModelNumInt=2;
    			if(OUStats > 7 && OUStats <= 9)ModelNumInt=3;
    			if(OUStats > 5 && OUStats <= 7)ModelNumInt=4;
    			if(OUStats > 3 && OUStats <= 5)ModelNumInt=5;
    			if(OUStats > 1 && OUStats <= 3)ModelNumInt=6;
    			if(OUStats <= 1)ModelNumInt=6;				
                
                for(int i=0;i<ModelNumInt;i++) {
                      if(i==0) {
                            ModelType = "L";
                      }
                      else {
                            ModelType = ModelType+"L";
                      }
                }
                ModelType = ModelType+"S";    
          }
          catch(Exception e) {
                e.printStackTrace();
          }
          return ModelType;
    }

	public static int CheckDecisionMagnitude(DataMaster DataMasterObj, String CurrBetStr, String StockCode1, String StockCode2) {
		int MagnitudeOK = 0;
		try {
			Utils util = new Utils();
			//Get the index of the stocks in the Futures sheet
			int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
			int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

			int PairIndex = util.GetPairsIndex(StockCode1, StockCode2, DataMasterObj.Pairs);
			double[] DecisionArrayMagnitude = new double[DataMasterObj.DecisionMagnitude[0].length];
			for(int i=0;i<DataMasterObj.DecisionMagnitude[0].length;i++) {
				DecisionArrayMagnitude[i] = DataMasterObj.DecisionMagnitude[PairIndex][i];
			}
			
			double PrevMagnitude = 3*DecisionArrayMagnitude[1]+2*DecisionArrayMagnitude[2]+1*DecisionArrayMagnitude[3];

			//if(DecisionArrayMagnitude[0] < DataMasterObj.Param.CricticalZValue && PrevMagnitude > 0 && DecisionArrayMagnitude[1] > 0 && DataMasterObj.OUValues[PairIndex] > 11 && DataMasterObj.VRTValues[PairIndex] < 1) {
			//if(DecisionArrayMagnitude[0] < DataMasterObj.Param.CricticalZValue && DecisionArrayMagnitude[1] > 0 && DataMasterObj.OUValues[PairIndex] > 11 && DataMasterObj.VRTValues[PairIndex] < 1) {
			if(DecisionArrayMagnitude[0] < DataMasterObj.Param.CricticalZValue 
					&& DecisionArrayMagnitude[1] > DataMasterObj.Param.PrevCricticalZValue 
					&& DataMasterObj.VRTValues[PairIndex] < DataMasterObj.Param.CriticalVRT 
					&& DataMasterObj.OUValues[PairIndex] < DataMasterObj.Param.CriticalOU) {			
				MagnitudeOK = 1;
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return MagnitudeOK;
	}



}
