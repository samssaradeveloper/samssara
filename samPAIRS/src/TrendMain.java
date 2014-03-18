import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrendMain implements Runnable {
	
	public static double Version = 201403.01;
	Thread ThreadObj;
	Utils UtilObj = new Utils();
	String CurrentCommDirectory = "Config";
	String RootDirectory = "data\\" + CurrentCommDirectory + "\\";
	DataMaster DataMasterObj = new DataMaster(RootDirectory);
	UIInterface UIObj= new UIInterface(); 
	public void StopStrategy(){
		DataMasterObj.StopStrategy = true;
		DataMasterObj.StrategyStatus = "STOPPED";
	}
	
	
	public void StartStrategy(){
		if (!DataMasterObj.StrategyRunStatus){
			ThreadObj = new Thread(this);
			ThreadObj.start();
			DataMasterObj.StrategyRunStatus = true;
			DataMasterObj.StopStrategy = false;
			DataMasterObj.StrategyStatus = "RUNNING";
			DataMasterObj.ConnectionStatus = "DIS-CONNECTED";
			UtilObj.WriteLogFile("Strategy is now running live..");
		}else{
			UtilObj.WriteLogFile("Strategy already running....");
		}
	}
	
	public void run(){
		try {
			
			//*****Program Detail
			//*** Dim3, OU20<0.2
			String EndLine = System.getProperty("line.separator");			
			
			//get the root directory information here
			DataMasterObj.setRootDirectory(RootDirectory);
			DataMasterObj.AssetDataDirectory = RootDirectory + "AssetData\\";
			

			Helper help = new Helper();
			ArrayList ParamList = new ArrayList();
			ArrayList PairsList = new ArrayList();
			ArrayList FuturesList = new ArrayList();
			
			MaxMinOfPositions MaxMinOfPositionObj = new MaxMinOfPositions();
			
			//load all the fixed arrays - of stocks and pairs	
			ParamList = UtilObj.LoadDataFromFile(DataMasterObj.RootDirectory + "Parameters.txt");
			FuturesList = UtilObj.LoadDataFromFile(DataMasterObj.RootDirectory + "Futures.txt");
			PairsList = UtilObj.LoadDataFromFile(DataMasterObj.RootDirectory +  "Pairs.txt");	
			
			//set the parameters list first
			Parameters ParamData = new Parameters();
			ParamData.SetParameterValues(ParamList);
			DataMasterObj.setParam(ParamData);
			ParamData.SetTradePaths(RootDirectory);
			DataMasterObj.Param.DBUserName = ParamData.getDBUserName(DataMasterObj);

			DataMasterObj.DBObj.UpdateParamSheet(DataMasterObj);
			ParamList = UtilObj.LoadDataFromFile(DataMasterObj.RootDirectory + "Parameters.txt");
			ParamData.SetParameterValues(ParamList);
			DataMasterObj.setParam(ParamData);
			ParamData.SetTradePaths(RootDirectory);

			if (DataMasterObj.Param.AutoMode.equals("ON")){
				DataMasterObj.ConnectSocket();
				int ConnectionRisk = UtilObj.CheckConnection(DataMasterObj); 
			}
			
			//Get all the data pertaining to the futures list
			String[][] Futures = UtilObj.GetFuturesArray(FuturesList);
			DataMasterObj.setFutures(Futures);			
			DataMasterObj.FuturesCode = new String[Futures.length];
			DataMasterObj.ExchangeCode = new String[Futures.length];
			for(int i=0;i<Futures.length;i++) {
				DataMasterObj.FuturesCode[i] = ((String)Futures[i][1]).trim();
				DataMasterObj.ExchangeCode[i] = "NSE";
			}		

			
			DataMasterObj.InstrumentCode = new String[Futures.length];
			for(int i=0;i<Futures.length;i++) {
				DataMasterObj.InstrumentCode[i] = ((String)Futures[i][2]).trim();
			}		
			
			
			//convert the data of the PairsList (text file) into pairs of 2 by 2
			String[][] Pairs = UtilObj.GetPairsArray(PairsList);
			DataMasterObj.setPairs(Pairs);	
			
			// Initializing PLot1 
			PLot1 PLotObj = new PLot1();
			PLotObj.MakePairLots(Pairs, DataMasterObj);
			double[][] PLot1 = PLotObj.PLot1;
			DataMasterObj.setPLot1(PLot1);
			
			//initialize the trd sheet
			int[] TrdObj = new int[DataMasterObj.Pairs.length];
			for (int k=0;k<DataMasterObj.Pairs.length;k++){
				TrdObj[k] = 0;
			}
			DataMasterObj.setTrd(TrdObj);
			
			DataMasterObj.BbgList = new ArrayList[DataMasterObj.Pairs.length];
			DataMasterObj.BbgListInverse = new ArrayList[DataMasterObj.Pairs.length];			
			DataMasterObj.BbgListDynamic = new ArrayList[DataMasterObj.Pairs.length];			
			DataMasterObj.ExpiryDatesCurrentExpiry = new String[Futures.length];			
			DataMasterObj.ExpiryDatesNextExpiry = new String[Futures.length];
			DataMasterObj.Volume = new double[Futures.length];
			DataMasterObj.VolumeNextExpiry  = new double[Futures.length];
			DataMasterObj.BIDQty = new double[Futures.length];
			DataMasterObj.ASKQty = new double[Futures.length];
			DataMasterObj.BIDQtyNextExpiry = new double[Futures.length];
			DataMasterObj.ASKQtyNextExpiry = new double[Futures.length];
			
			DateFormat formatter =  new SimpleDateFormat("yyyyMMdd|HH:mm:ss");
			// Read Current Date Stamp from TestData Vector
			int TestDataIndex = 0;
			long CurrTime = 0; int CurrDay = 0;
			Calendar CurrRun = Calendar.getInstance();
			Date CurrDate = CurrRun.getTime();
								
			CurrTime = UtilObj.GetTimeInSeconds(CurrRun);
			long PrevTime = CurrTime-10;
			long HighFreqTimeGap = DataMasterObj.Param.TimeGap;  
			long HighFreqLastTime = CurrTime;
			long ScreenTimeGap = DataMasterObj.Param.ScreenRefreshRate*1000;
			long ScreenLastTime = System.currentTimeMillis();
			int LoopStart = 0;			
			int LastRecordedTimeInSec = 0;
			int LastDecisionTimeInSec = 0;
			int LastQtyMatchTimeInSec = 0;
			int LastLimitOrderExecutionDone = 0;
			int LastLimitOrderExecutionDoneConservative = 0;

			String ConnectStr = "";
			UtilObj.WriteToFile(DataMasterObj.Param.Trades2Path, null, false);
			UtilObj.WriteToFile(DataMasterObj.Param.NotesPath, null, false);
						
			//initialize the carry over of the trade here to 0 - as ttrades are still not carried over
			DataMasterObj.TradeCarryOverDoneForThisPair = new int[Pairs.length];
			for(int i=0;i<DataMasterObj.TradeCarryOverDoneForThisPair.length;i++) {
				DataMasterObj.TradeCarryOverDoneForThisPair[i] = 0;
			}		
			
			int ModelStopDay = Integer.parseInt(DataMasterObj.Param.ModelStopOnDayOfWeek.split("-")[0]);
			//if the day of week saturday then close shut the model at 2 pm
			if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == ModelStopDay){
				DataMasterObj.Param.ModelStopTime = Integer.parseInt(DataMasterObj.Param.ModelStopOnDayOfWeek.split("-")[1]);
			}
			
			//set the BBG time series gap for each asset separately
			//6 = MktOpenTime  Eg: 1000
			//7 = MktCloseTime Eg: 1700
			//8 = BBGTimeSeriesGap
			//9 = DynamicVolCutOffValue
			DataMasterObj.BBGTimeSeriesGapForEachAsset = new double[DataMasterObj.Futures.length];
			DataMasterObj.MarketTimeInHoursForEachAsset = new double[DataMasterObj.Futures.length];
			DataMasterObj.MarketStartTimeInSecondsForEachAsset = new long[DataMasterObj.Futures.length];
			DataMasterObj.MarketStopTimeInSecondsForEachAsset = new long[DataMasterObj.Futures.length];			
			DataMasterObj.VolCutOffForDynamicLotEachAsset = new double[DataMasterObj.Futures.length];	
			DataMasterObj.PriceRatio = new double[DataMasterObj.Pairs.length];
			DataMasterObj.PriceRatioNextExpiry = new double[DataMasterObj.Pairs.length];
			DataMasterObj.OUValues = new double[DataMasterObj.Pairs.length];
			UtilObj.SetBBGTimeSeriesGapForEachAsset(DataMasterObj);
			UtilObj.SetMarketTimeinHoursForEachAsset(DataMasterObj);	
			
			//Get the server connectivity to send data thru ftp server
			QtyMatch QtyMatchObj = new QtyMatch();		
			ExpiryRollover ExpiryRollOver = new ExpiryRollover();
			
			//make the BGBtimeseriesgap seprately for each stock
			DataMasterObj.CurrStocksBBGTimeSeriesGap = new double[DataMasterObj.Pairs.length];
			DataMasterObj.CurrDecisionLength = new int[DataMasterObj.Pairs.length];

			//Load the HFT data from the DataHandler
			DataHandler DataHandlerObj = new DataHandler();
			DataHandlerObj.LoadHighFreqDataFromFiles(DataMasterObj);
			
			int LastRecordedSeqHF = 0;
			int LastOrderPlacementCheck = 0;
			int LastDBUpdateTime = 0;
			UtilObj.InitizlizeBidAskSequence(DataMasterObj);
			DataMasterObj.YesterdayPL = UtilObj.YesterdayPL(DataMasterObj);
			
			String UpdateType = "DB";
			if (DataMasterObj.Param.FTPUpdate == 1){
				if (DataMasterObj.Param.DBUpdate == 1){
					UpdateType = "BOTH";
				}else{
					UpdateType = "FTP";
				}
			}

			// If Current tick time more than 10 a.m.
			while(!DataMasterObj.StopStrategy) {					
								
//				double[][] Quotes = new double[DataMasterObj.Futures.length][3];					
//				Quotes = UtilObj.UpdateQuotes(DataMasterObj);
//				UtilObj.UpdateQuotesNextExpiry(DataMasterObj);
//				DataMasterObj.setQuotes(Quotes);
				
				UtilObj.UpdateAllQuotes(DataMasterObj);
				
				//record the bid-ask-hft sequency sequence every 5 seconds
				try {
					int CurrSecond = Calendar.getInstance().get(Calendar.SECOND);
					if(CurrSecond%2 >= 0 && CurrSecond%2 <= 1 && 
							(CurrSecond-LastRecordedSeqHF >= 2 || CurrSecond <= 2)) {
						LastRecordedSeqHF = CurrSecond;					
						UtilObj.RecordBidAskSequenceRollingHF(DataMasterObj);	
						
//						System.out.println("---------------------------");
//						System.out.println(Calendar.getInstance().get(Calendar.MINUTE) + ":" + CurrSecond);
//						for(int i=0;i<DataMasterObj.BidSequenceHF.get(5).size();i++) {
//							System.out.println(i+"|"+(DataMasterObj.BidSequenceHF.get(5)).get(i)+"|"+(DataMasterObj.AskSequenceHF.get(5)).get(i)+"|"+(DataMasterObj.LtpSequenceHF.get(5)).get(i));
//						}

					}					
				}
				catch(Exception e) {
					e.printStackTrace();
					DataMasterObj.GlobalErrors = "ERROR_IN_GETTING_BIDASK_HFT_SEQUENCE";
				}

				if(CurrTime > DataMasterObj.Param.ModelStartTime && CurrTime < DataMasterObj.Param.ModelStopTime && !DataMasterObj.StopStrategy){					
					//This is the point where the Model runs for the first time
					if (LoopStart == 0){
						UtilObj.WriteLogFile("\r"+DataMasterObj.Param.CurrentCountry + " : Model Start Time : "+ CurrRun.getTime());
						UtilObj.CheckForExpiry(DataMasterObj);
					}
					
					// record data in HighFreqData file if HighFreqTimeGap > 30 seconds 
					if(Calendar.getInstance().get(Calendar.SECOND) % DataMasterObj.Param.HighFreqTimeGap <= 10 && 
							UtilObj.GetTimeInSeconds(Calendar.getInstance()) - LastRecordedTimeInSec >= 15) {
						
						DataMasterObj.GlobalErrors = "";
						ExpiryRollOver.CheckExpiryDatesOfPositions(DataMasterObj);
						UtilObj.WriteLogFile("Updating prices...");
						
						LastRecordedTimeInSec = UtilObj.GetTimeInSeconds(Calendar.getInstance());						
						
						//taking max/min of every 30 sec to be compatible with back-test
						//only if moving stop loss exists calculate the max/min
						if(DataMasterObj.Param.MovingStopLoss.equals("ON")) {
							//adjust the max/min of the positions every 30 sec.
							//MaxMinOfPositionObj.AdjustMaxMinOfTrades(DataMasterObj);						
						}
												
						DataHandlerObj.WriteHighFreqDataFast(DataMasterObj);
						DataHandlerObj.WriteHighFreqDataNextExpiryFast(DataMasterObj);
						
						DataHandlerObj.FillBBGSheetFast(DataMasterObj);
																		
						if(DataMasterObj.BbgList[0] != null && DataMasterObj.Param.RunMidMinute == 0) {
							//also calculate these date at the end of every second
							UtilObj.WriteLogFile("Calculating end-of-minute parameters...");
							DataHandlerObj.ModifyBBGSheetEachSecond(DataMasterObj);																
							help.MeanReversionIndicator(DataMasterObj);	
							help.MeanReversionIndicatorInverse(DataMasterObj);
							
							//now start generating the trades in PLTrd logic
							PLTrd PLTrdObj = new PLTrd();
							PLTrdObj.GeneratePLTrdMain(DataMasterObj);								

							//this needs to be checked at least once every minute - to see if there is a trade closing criteria in place
							ManOpn ManOpnObj = new ManOpn();
							ManOpnObj.TradeCloseRecording(DataMasterObj);																		
						}
						
						Calendar LastRun = Calendar.getInstance();
						
						//store the last min data for the next execution of the stock
						DataMasterObj.QuotesPrevMin = new double[DataMasterObj.Futures.length][3];			
						DataMasterObj.QuotesNextExpiryPrevMin = new double[DataMasterObj.Futures.length][3];
						//update the quotes for the previous minute here
						for(int ii=0;ii<DataMasterObj.Quotes.length;ii++) {
							for(int jj=0;jj<DataMasterObj.Quotes[0].length;jj++) {
								DataMasterObj.QuotesPrevMin[ii][jj] = DataMasterObj.Quotes[ii][jj];
								DataMasterObj.QuotesNextExpiryPrevMin[ii][jj] = DataMasterObj.QuotesNextExpiry[ii][jj];								
							}
						}						

						// Fill data in HighFreqData.txt
						//HighFreqLastTime = UtilObj.WriteHighFreqDataFile(DataMasterObj);
						//UtilObj.WriteHighFreqDataFileNextExpiry(DataMasterObj);	
						UtilObj.WriteClosePriceDataFutures(DataMasterObj);
					}										
				}

				if(DataMasterObj.BbgList[0] != null && DataMasterObj.Param.RunMidMinute == 1) {
					//also calculate these date at the end of every second
					UtilObj.WriteLogFile("Calculating mid-minute parameters...");
					DataHandlerObj.ModifyBBGSheetEachSecond(DataMasterObj);																
					help.MeanReversionIndicator(DataMasterObj);	
					help.MeanReversionIndicatorInverse(DataMasterObj);
					
					//now start generating the trades in PLTrd logic
					PLTrd PLTrdObj = new PLTrd();
					PLTrdObj.GeneratePLTrdMain(DataMasterObj);								

					//this needs to be checked at least once every minute - to see if there is a trade closing criteria in place
					ManOpn ManOpnObj = new ManOpn();
					ManOpnObj.TradeCloseRecording(DataMasterObj);																		
				}
				
				UIObj.MakeUIInterface(DataMasterObj);
				
				Thread.sleep(DataMasterObj.Param.RunLoopInTimeInSec * 1000);
				if (CurrTime > DataMasterObj.Param.ModelStopTime && LoopStart > 0){
					//send the last report to the server before stopping the system
					UtilObj.WriteTradePLData(DataMasterObj);
					if(DataMasterObj.Param.LimitOrderExecution == 1) {
						QtyMatchObj.MatchLimitOrderQty(DataMasterObj, UpdateType);
					}
					else {
						QtyMatchObj.match(DataMasterObj, UpdateType);						
					}
					UtilObj.WriteLogFile(DataMasterObj.Param.CurrentCountry + ": Model Stop Time : "+ CurrDate);
					DataMasterObj.StopStrategy = true;
				}
								
				UtilObj.SendSampleData(DataMasterObj);
				Calendar LastRun = Calendar.getInstance();
				CurrDate = LastRun.getTime();
				CurrTime = UtilObj.GetTimeInSeconds(LastRun);
				CurrRun.getInstance();
				
				if (CurrTime > PrevTime + 5){
					PrevTime = CurrTime;
					int ConnectionRisk = UtilObj.CheckConnection(DataMasterObj);
					if(ConnectionRisk == 0) {
						DataMasterObj.ConnectionStatus = "CONNECTED"; 
					}
					else {
						DataMasterObj.ConnectionStatus = "DIS-CONNECTED"; 						
					}
				}
				
				//match for the qty with odin system and send data to the server every 30 min
				if((Calendar.getInstance().get(Calendar.MINUTE) % 10 == 0 
						&& Calendar.getInstance().get(Calendar.SECOND) >= 20 
						&& UtilObj.GetTimeInSeconds(Calendar.getInstance()) - LastQtyMatchTimeInSec > 120)
						//Matching the qty 5 min before the model stops... 
						|| (UtilObj.GetTimeInSeconds(Calendar.getInstance())>DataMasterObj.Param.ModelStopTime-300 && LastQtyMatchTimeInSec < DataMasterObj.Param.ModelStopTime-300)){
										
					LastQtyMatchTimeInSec = UtilObj.GetTimeInSeconds(Calendar.getInstance());
					UtilObj.WriteLogFile("Connecting to Samssara Server...");
					if(DataMasterObj.Param.LimitOrderExecution == 1) {
						QtyMatchObj.MatchLimitOrderQty(DataMasterObj, UpdateType);
					}
					else {
						QtyMatchObj.match(DataMasterObj, UpdateType);						
					}					
					if (Calendar.getInstance().get(Calendar.MINUTE) == 0 && DataMasterObj.Param.FTPUpdate == 1){
						QtyMatchObj.UpdateNetPosition(DataMasterObj);
					}
					UtilObj.WriteLogFile("Connection sequence completed...");
					UtilObj.WriteTradePLData(DataMasterObj);
				}
				
				//limit order execution happens here - after every x minutes
				if(DataMasterObj.Param.LimitOrderExecution == 1) {					
					//aggressive order to cancel all trades and place ask orders for buying
					//happens in the 40th second
					if((Calendar.getInstance().get(Calendar.MINUTE) % 1 == 0 
							&& Calendar.getInstance().get(Calendar.SECOND) >= 45 
							&& UtilObj.GetTimeInSeconds(Calendar.getInstance()) - LastLimitOrderExecutionDone > 30)) {
						LastLimitOrderExecutionDone = UtilObj.GetTimeInSeconds(Calendar.getInstance());
						UtilObj.WriteLogFile("LOC II...");
						
						LimitOrderHandler LimitOrderObj = new LimitOrderHandler();
						LimitOrderObj.CancelLimitOrderTrades(DataMasterObj, "AGGRESSIVE");
					}
					
					//conservative limit order placement on the 15th second - can be changed to cancelling every 5 seconds also
					int CurrSecond = Calendar.getInstance().get(Calendar.SECOND);
					if(CurrSecond < 9) LastLimitOrderExecutionDoneConservative = 0;
					//do from 20th second BidAskSequence may not have been noted correctly
					if(CurrSecond%20 >= 1 && CurrSecond%20 <= 4 && CurrSecond <= 25 && CurrSecond >= 8 &&  
							CurrSecond-LastLimitOrderExecutionDoneConservative >= 5) {

						LastLimitOrderExecutionDoneConservative = CurrSecond;
						UtilObj.WriteLogFile("LOC I...");
						
						LimitOrderHandler LimitOrderObj = new LimitOrderHandler();
						LimitOrderObj.CancelLimitOrderTrades(DataMasterObj, "AGGRESSIVE");
					}
					
					//every 2 second check for the placement of the orders - and place conservative / aggressive orders
					CurrSecond = Calendar.getInstance().get(Calendar.SECOND);
					if(CurrSecond < 20) LastOrderPlacementCheck = 0;
					if(CurrSecond >= 20 && CurrSecond%2 >= 0 && CurrSecond%2 <= 1 && 
							(CurrSecond-LastOrderPlacementCheck >= 2)) {
						LastOrderPlacementCheck = CurrSecond;					
						if(CurrSecond < 45) {
							//UtilObj.WriteLogFile("LOP (Cons)...");
							LimitOrderHandler LimitOrderObj = new LimitOrderHandler();
							LimitOrderObj.PlaceLimitOrderTrades(DataMasterObj, "AGGRESSIVE");							
						}
						else {
							//UtilObj.WriteLogFile("LOP (Agg)...");
							LimitOrderHandler LimitOrderObj = new LimitOrderHandler();
							LimitOrderObj.PlaceLimitOrderTrades(DataMasterObj, "AGGRESSIVE");														
						}						
					}
				}	
				if (Calendar.getInstance().get(Calendar.SECOND) > 15
						&& Calendar.getInstance().get(Calendar.SECOND) < 20
						&& UtilObj.GetTimeInSeconds(Calendar.getInstance())-LastDBUpdateTime > 120
						&& DataMasterObj.Param.DBUpdate == 1){
					LastDBUpdateTime = UtilObj.GetTimeInSeconds(Calendar.getInstance());
					double UpdateTime = System.currentTimeMillis();
					if(DataMasterObj.Param.LimitOrderExecution == 1) {
						QtyMatchObj.MatchLimitOrderQty(DataMasterObj, "DB");
					}
					else {
						QtyMatchObj.match(DataMasterObj, "DB");						
					}
					UpdateTime = (System.currentTimeMillis()) - UpdateTime;
					System.out.println("UpdateTime = "+(UpdateTime));

				}

				if(LastQtyMatchTimeInSec == 0) {
					UtilObj.WriteLogFile("Connecting to Samssara Server...");
					if(DataMasterObj.Param.LimitOrderExecution == 1) {
						QtyMatchObj.MatchLimitOrderQty(DataMasterObj, UpdateType);
					}
					else {
						QtyMatchObj.match(DataMasterObj, UpdateType);						
					}					
					LastQtyMatchTimeInSec = UtilObj.GetTimeInSeconds(Calendar.getInstance());
					UtilObj.WriteLogFile("Connection sequence completed...");
					UtilObj.WriteTradePLData(DataMasterObj);

				}
								
				HighFreqTimeGap = CurrTime - HighFreqLastTime ;
				ScreenTimeGap = System.currentTimeMillis() - ScreenLastTime;
				LoopStart = LoopStart + 1;
//				System.out.print("\r"+DataMasterObj.Param.CurrentCountry + " : " + LastRun.getTime() + " : "+ConnectStr);
				}
			if (DataMasterObj.Param.FTPUpdate == 1){
				QtyMatchObj.UpdateNetPosition(DataMasterObj);
			}
		}
		catch(InterruptedException e) {
			UtilObj.WriteLogFile(DataMasterObj.Param.CurrentCountry + ": ERROR in Main...Exiting!");
			e.printStackTrace();
		}

		DataMasterObj.StrategyRunStatus = false;
		DataMasterObj.StopStrategy = false;
		DataMasterObj.StrategyStatus = "STOPPED";
		DataMasterObj.ConnectionStatus = "DIS-CONNECTED";
		UIObj.MakeUIInterface(DataMasterObj);
		UtilObj.WriteLogFile("Strategy Stopped...");
		UtilObj.infoBox("Strategy Stopped", "samTREND");
	}
	

}	
	