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
			
			DataMasterObj.NetPosition = new int[DataMasterObj.FuturesCode.length];
			DataMasterObj.NetPositionCurrentExpiry = new int[DataMasterObj.FuturesCode.length];
			DataMasterObj.NetPositionNextExpiry = new int[DataMasterObj.FuturesCode.length];
			double ShortExposure = 0;
			double LongExposure = 0;
			double TotalExposure = 0;
			double OpenPL = 0;
			double ClosePL = 0;
			int OpenTradeCount = Position.size();
			
			//fill the open positions first
			for(int i=0;i<Position.size();i++) {
				String[] PosStr = ((String)Position.get(i)).split("\t");
				String PropertyString = PosStr[10];
				String[] PropertyArray = PropertyString.split("&");
				String NewPosStr = PosStr[0];
				String ExpiryDataForPosition = PropertyArray[3];

				//short position
				String StockCode1 = PosStr[1];
				String StockCode2 = PosStr[2];
				
				int StockIndex1 = (int)util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
				int StockIndex2 = (int)util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");
				
				int Qty1 = Integer.parseInt(PosStr[7]);
				int Qty2 = Integer.parseInt(PosStr[8]);
				
				double TradeOpenPrice1 = Double.parseDouble(PosStr[5]);
				double TradeOpenPrice2 = Double.parseDouble(PosStr[6]);
				double TradeOpenRatio = TradeOpenPrice1/TradeOpenPrice2;
				
				double LTP1 = util.GetQuotesData(DataMasterObj, StockCode1, "LTP");
				double LTP2 = util.GetQuotesData(DataMasterObj, StockCode2, "LTP");
				double LiveRatio = 0;
				if (LTP1 <=0){
					LTP1 = util.GetQuotesData(DataMasterObj, StockCode1, "BID");
					if (LTP1 <=0){
						LTP1 = util.GetQuotesData(DataMasterObj, StockCode1, "ASK");
					}
				}

				if (LTP2 <=0){
					LTP2 = util.GetQuotesData(DataMasterObj, StockCode2, "BID");
					if (LTP2 <=0){
						LTP2 = util.GetQuotesData(DataMasterObj, StockCode2, "ASK");
					}
				}
				
				if (LTP2 > 0){
					LiveRatio = LTP1/LTP2;
				}
				
				try{
					if (ExpiryDataForPosition.equals(util.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesCurrentExpiry[StockIndex1]))){
						DataMasterObj.NetPositionCurrentExpiry[StockIndex1] = DataMasterObj.NetPositionCurrentExpiry[StockIndex1] + Qty1;
						DataMasterObj.NetPositionCurrentExpiry[StockIndex2] = DataMasterObj.NetPositionCurrentExpiry[StockIndex2] - Qty2;
					}else if (ExpiryDataForPosition.equals(util.ConvertToYYYYMMDD(DataMasterObj.ExpiryDatesNextExpiry[StockIndex1]))){
						DataMasterObj.NetPositionNextExpiry[StockIndex1] = DataMasterObj.NetPositionNextExpiry[StockIndex1] + Qty1;
						DataMasterObj.NetPositionNextExpiry[StockIndex2] = DataMasterObj.NetPositionNextExpiry[StockIndex2] - Qty2;
					}
				}catch (Exception e){}
				DataMasterObj.NetPosition[StockIndex1] = DataMasterObj.NetPosition[StockIndex1] + Qty1;
				DataMasterObj.NetPosition[StockIndex2] = DataMasterObj.NetPosition[StockIndex2] - Qty2;
				double Exposure1 = 1*LTP1*Qty1*util.GetMultiplier(StockCode1,DataMasterObj);
				double Exposure2 = -1*LTP1*Qty1*util.GetMultiplier(StockCode1,DataMasterObj);
				double Return = (LiveRatio/TradeOpenRatio)-1;
				double PL = util.GetMultiplier(StockCode1,DataMasterObj)*(LTP1 - TradeOpenPrice1)*Qty1;
				PL = PL - (util.GetMultiplier(StockCode2, DataMasterObj)*(LTP2 - TradeOpenPrice2) * Qty2);
				OpenPL = OpenPL + PL;
				NewPosStr = NewPosStr+"\t"+StockCode1+"\t"+StockCode2+"\t"+DataMasterObj.Param.UIPriceFormat.format(TradeOpenPrice1)+"\t"+DataMasterObj.Param.UIPriceFormat.format(TradeOpenPrice2)+"\t"+LTP1+"\t"+LTP2+"\t"+DataMasterObj.Param.PercentFormat.format(Return)+"\t"+DataMasterObj.Param.UIPriceFormat.format(PL);

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
				
				double Exposure = LTP * DataMasterObj.NetPosition[i]*util.GetMultiplier(NetPosStr, DataMasterObj);
				if (Exposure > 0){
					LongExposure = LongExposure + Exposure;
				}else{
					ShortExposure = ShortExposure + Exposure;
				}
				NetPosStr = NetPosStr + "\t" + DataMasterObj.NetPosition[i] +"\t" + LTP + "\t" + DataMasterObj.Param.UIPLFormat.format(Exposure); 
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
			DataMasterObj.ExposureString = DataMasterObj.Param.UIPLFormat.format((LongExposure-ShortExposure));
			DataMasterObj.PLTotal = ClosePL + OpenPL;
			DataMasterObj.PLTotalAsString = DataMasterObj.Param.UIPLFormat.format((DataMasterObj.PLTotal));
			DataMasterObj.DailyPL = DataMasterObj.PLTotal - DataMasterObj.YesterdayPL;
			DataMasterObj.DailyPLAsString = DataMasterObj.Param.UIPLFormat.format(DataMasterObj.DailyPL);
			DataMasterObj.GrossExposure = ((LongExposure-ShortExposure)/100000);
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
