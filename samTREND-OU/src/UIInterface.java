import java.util.ArrayList;


public class UIInterface {

	public void MakeUIInterface(DataMaster DataMasterObj) {
		try {
			Utils util = new Utils();
			if(DataMasterObj.Param.UIInterfaceShow == 0) {
				return;
			}
			
			
			ArrayList Trades = util.LoadDataFromFile(DataMasterObj.Param.TradesPath);
			ArrayList Position = util.LoadDataFromFile(DataMasterObj.Param.PositionPath);
			ArrayList ClosePosition = util.LoadDataFromFile(DataMasterObj.Param.ClosePosnPath);
			
			ArrayList NewPositionList = new ArrayList();
			ArrayList NetPositionList = new ArrayList();
			
			double[] NetPosition = new double[DataMasterObj.FuturesCode.length];
			double ShortExposure = 0;
			double LongExposure = 0;
			double TotalExposure = 0;
			double OpenPL = 0;
			double ClosePL = 0;
			int OpenTradeCount = Position.size();
			
			//fill the open positions first
			for(int i=0;i<Position.size();i++) {
				String[] PosStr = ((String)Position.get(i)).split("\t");
				String NewPosStr = PosStr[0];
				
				//if the bet number is less then minimum bet required then dont log the dummy P&L of the open position
				String PropertyString = PosStr[10];
				String[] PropertyArray = PropertyString.split("&");
				double BetNumber = Double.parseDouble(PropertyArray[PropertyArray.length-2]);
				if(BetNumber < DataMasterObj.Param.MinBetOpen) {
					continue;
				}
				
				//short position
				if(PosStr[1].equals(DataMasterObj.FuturesCode[0])) {
					String StockCode = PosStr[2];
					int StockIndex = (int)util.GetQuotesData(DataMasterObj, StockCode, "STOCKINDEX");
					double Qty = Double.parseDouble(PosStr[8]);
					double TradeOpenPrice = Double.parseDouble(PosStr[6]);
					double LTP = util.GetQuotesData(DataMasterObj, StockCode, "LTP");
					if (LTP <=0){
						LTP = util.GetQuotesData(DataMasterObj, StockCode, "BID");
					}
					if (LTP <=0){
						LTP = util.GetQuotesData(DataMasterObj, StockCode, "ASK");
					}

					NetPosition[StockIndex] = NetPosition[StockIndex] - Qty;
					double Exposure = -1*LTP*Qty*util.GetMultiplier(StockCode,DataMasterObj);
					double Return = (TradeOpenPrice-LTP)/TradeOpenPrice;
					double PL = util.GetMultiplier(StockCode,DataMasterObj)*(TradeOpenPrice-LTP)*Qty;
					OpenPL = OpenPL + PL;
					NewPosStr = NewPosStr+"\t"+StockCode+"\t"+"SHORT"+"\t"+DataMasterObj.Param.UIPriceFormat.format(TradeOpenPrice)+"\t"+(-1*Qty)+"\t"+DataMasterObj.Param.UIPLFormat.format(Exposure)+"\t"+LTP+"\t"+DataMasterObj.Param.PercentFormat.format(Return)+"\t"+DataMasterObj.Param.UIPriceFormat.format(PL);
				}
				else {
					String StockCode = PosStr[1];
					int StockIndex = (int)util.GetQuotesData(DataMasterObj, StockCode, "STOCKINDEX");
					double Qty = Double.parseDouble(PosStr[7]);
					double TradeOpenPrice = Double.parseDouble(PosStr[5]);
					double LTP = util.GetQuotesData(DataMasterObj, StockCode, "LTP");
					if (LTP <=0){
						LTP = util.GetQuotesData(DataMasterObj, StockCode, "BID");
					}
					if (LTP <=0){
						LTP = util.GetQuotesData(DataMasterObj, StockCode, "ASK");
					}

					NetPosition[StockIndex] = NetPosition[StockIndex] + Qty;
					double Exposure = LTP*Qty*util.GetMultiplier(StockCode,DataMasterObj);
					double Return = (LTP-TradeOpenPrice)/TradeOpenPrice;
					double PL = util.GetMultiplier(StockCode,DataMasterObj)*(LTP-TradeOpenPrice)*Qty;
					OpenPL = OpenPL + PL;
					NewPosStr = NewPosStr+"\t"+StockCode+"\t"+"LONG"+"\t"+DataMasterObj.Param.UIPriceFormat.format(TradeOpenPrice)+"\t"+Qty+"\t"+DataMasterObj.Param.UIPLFormat.format(Exposure)+"\t"+LTP+"\t"+DataMasterObj.Param.PercentFormat.format(Return)+"\t"+DataMasterObj.Param.UIPriceFormat.format(PL);
				}		
				
				NewPositionList.add(NewPosStr);
			}
			util.WriteToFile(DataMasterObj.Param.UIPositionPath, null, false);
			for (int i=0;i<NewPositionList.size();i++){
				String TempString = (String)NewPositionList.get(i);
				util.WriteToFile(DataMasterObj.Param.UIPositionPath, TempString, true);
			}
			for (int i=1;i<DataMasterObj.FuturesCode.length;i++){
				String NetPosStr = DataMasterObj.FuturesCode[i];
				double LTP = util.GetQuotesData(DataMasterObj, NetPosStr, "LTP");
				if (LTP <=0){
					LTP = util.GetQuotesData(DataMasterObj, NetPosStr, "BID");
				}
				if (LTP <=0){
					LTP = util.GetQuotesData(DataMasterObj, NetPosStr, "ASK");
				}
				
				double Exposure = LTP * NetPosition[i]*util.GetMultiplier(NetPosStr, DataMasterObj);
				if (Exposure > 0){
					LongExposure = LongExposure + Exposure;
				}else{
					ShortExposure = ShortExposure + Exposure;
				}
				NetPosStr = NetPosStr + "\t" + NetPosition[i] +"\t" + LTP + "\t" + DataMasterObj.Param.UIPLFormat.format(Exposure); 
				NetPositionList.add(NetPosStr);
			}
			util.WriteToFile(DataMasterObj.Param.UINetPosPath, null, false);
			for (int i=0;i<NetPositionList.size();i++){
				String TempString = (String)NetPositionList.get(i);
				util.WriteToFile(DataMasterObj.Param.UINetPosPath, TempString, true);
			}
			
			for (int i=0;i<ClosePosition.size();i++){
				String[] CloseData = ((String)ClosePosition.get(i)).split("\t");
				double TradePL = Double.parseDouble(CloseData[15]);
				ClosePL = ClosePL + TradePL;
			}

			DataMasterObj.PLTotalAsString = DataMasterObj.Param.UIPLFormat.format((ClosePL+OpenPL));
			DataMasterObj.GrossExposureAsString = String.valueOf(Math.round((LongExposure-ShortExposure)/100000));
			
			String Str1 = "Connection Status" + "\t" + DataMasterObj.ConnectionStatus;
			String Str2 = "Model Status" + "\t" + DataMasterObj.StrategyStatus;
			String Str3 = "Long Exposure" + "\t" + DataMasterObj.Param.UIPLFormat.format(LongExposure);
			String Str4 = "Short Exposure" + "\t" + DataMasterObj.Param.UIPLFormat.format(ShortExposure);
			String Str5 = "Net Exposure" + "\t" + DataMasterObj.Param.UIPLFormat.format((LongExposure+ShortExposure));
			String Str6 = "Total Exposure" + "\t" + DataMasterObj.Param.UIPLFormat.format((LongExposure-ShortExposure));
			String Str7 = "Unrealized P/L" + "\t" + DataMasterObj.Param.UIPLFormat.format(OpenPL);
			String Str8 = "Realized P/L" + "\t" + DataMasterObj.Param.UIPLFormat.format(ClosePL);
			String Str9 = "Total PL" + "\t" + DataMasterObj.Param.UIPLFormat.format((ClosePL+OpenPL));
			
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, null, false);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str1, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str2, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str3, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str4, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str5, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str6, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str7, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str8, true);
			util.WriteToFile(DataMasterObj.Param.UIStatusPath, Str9, true);
			
			ArrayList UITradeList = util.LoadDataFromFile(DataMasterObj.Param.UITradesPath);
			if (UITradeList.size()>100){
				util.WriteToFile(DataMasterObj.Param.UITradesPath, null, false);
				for (int k=0;k<50;k++){
					util.WriteToFile(DataMasterObj.Param.UITradesPath, (String)UITradeList.get(k), true);
				}
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}	
}
