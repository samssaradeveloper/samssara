import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JOptionPane;

import com.javaparts.dde.Conversation;
import com.javaparts.dde.DDEClient;
import com.javaparts.dde.DDEException;

public class Utils {
	public static final String DATE_FORMAT_NOW = "yyyyMMdd|HH:mm:ss";
	
	//function to load the data from text file into an arraylist
	public ArrayList<String> LoadDataFromFile(String RelativePath) {
		ArrayList DataFromFile = new ArrayList();
		try {
			// Open the file that is the first 
		    // command line parameter
		    FileInputStream fstream = new FileInputStream(RelativePath);
		    // Get the object of DataInputStream
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String strLine;
		    //Read File Line By Line
		    while ((strLine = br.readLine()) != null)   {
		        //Print the content on the console
		    	//System.out.println(strLine);
		    	DataFromFile.add(strLine);
		    }
		    //Close the input stream
		    in.close();						
		}
		catch(Exception e) {
		//	DataMaster.logger.warning(e.toString());
		//	e.printStackTrace();
		}
		return DataFromFile;
	}
	
	//function to convert the PairsList (from data file) into Pairs Array of n*2
	public String[][] GetPairsArray(ArrayList PairsList) {
		String[][] Pairs = new String[PairsList.size()][2];
		try {
			for(int i=0;i<PairsList.size();i++) {
				//delim the data of the pairs list tab wise
				String data[] = ((String)PairsList.get(i)).split("\t");								
				//now read each pair and put into the Pairs array
				for(int j=0;j<data.length;j++) {
					Pairs[i][j] = data[j];
				}
			}					
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return Pairs;
	}
	
	//function to convert the FuturesList (from data file) into Futures Array of n*20
	public String[][] GetFuturesArray(ArrayList FuturesList) {
		String data1[] = ((String)FuturesList.get(0)).split("\t");								
		String[][] Futures = new String[FuturesList.size()][data1.length];
		try {
			for(int i=0;i<FuturesList.size();i++) 
			{
				//delim the data of the pairs list tab wise
				String data[] = ((String)FuturesList.get(i)).split("\t");								
				//now read each pair and put into the Pairs array
				for(int j=0;j<data.length;j++) 
				{
					Futures[i][j] = data[j];
				}
			}					
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return Futures;
	}
	
	//function to get the index corresponding to a particular stock in an array of stock/futures 
	public int GetIndex(String StockCode, String[] FuturesArr) {
		int StockIndex = 0;
		try {
			for(int i=0;i<FuturesArr.length;i++) {
				if(StockCode.trim().equals(FuturesArr[i].trim())) {
					StockIndex = i;
					break;
				}
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();	
		}
		return StockIndex;
	}
	
	//function to get the pairs index for a stockcode1 and stockcode2 from the pairs list
	public int GetPairsIndex(String StockCode1, String StockCode2, String[][] PairsArr) {
		int PairsIndex = 0;
		try {
			for(int i=0;i<PairsArr.length;i++) {
				if(StockCode1.trim().equals(PairsArr[i][0].trim()) && 
						StockCode2.trim().equals(PairsArr[i][1].trim())) {
					PairsIndex = i;
					break;
				}
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();	
		}
		return PairsIndex;
	}	
	

	//GetTotalQty - function to get the futures data - last price, qty etc. based on the Col Index required
	public double GetFuturesData(DataMaster DataMasterObj, String StockCode, int FuturesColIndex) {				
		double GetFuturesData = 0;
		try {
			for(int i=0;i<DataMasterObj.Futures.length;i++) {
				String CurrStockCode = DataMasterObj.FuturesCode[i];
				if(CurrStockCode.trim().equals(StockCode)) {
					GetFuturesData = Double.parseDouble(DataMasterObj.Futures[i][FuturesColIndex]);
					//System.out.println(GetFuturesData);
					return GetFuturesData;
				}
				
			}						
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}		
		return GetFuturesData;
	}
	
	//function to get the data out from the Quotes array which is LTP-Bid-Ask array corresponding 
	//to a paritcular Futures data
	public double GetQuotesData(DataMaster DataMasterObj,String StockCode, String DataType) {
		String[][] Futures = DataMasterObj.Futures;
		double[][] Quotes = new double[Futures.length][3];
		int StockIndex = 0;
		double QuoteData = 0;
		try {
			for(int i=0; i<Futures.length; i++) {
				if(StockCode.trim().equals(DataMasterObj.FuturesCode[i].trim())) {
					StockIndex = i;
					break;
				}
			}
			
			RiskManager RiskMan = new RiskManager();
			int ExpiryRisk = RiskMan.CalcLiveRisk(DataMasterObj, StockCode, DataMasterObj.FuturesCode[0]);			
			if(ExpiryRisk == 0) {
				Quotes = DataMasterObj.Quotes;
			}
			else {
				Quotes = DataMasterObj.QuotesNextExpiry;
			}			
			
			if(DataType.equalsIgnoreCase("LTP")) {
				QuoteData = Quotes[StockIndex][0];
			}
			else if(DataType.equalsIgnoreCase("BID")) {
				QuoteData = Quotes[StockIndex][1];
			}
			else if(DataType.equalsIgnoreCase("ASK")) {
				QuoteData = Quotes[StockIndex][2];
			}		
			//sometime only the stocks index is required and hence pass that
			else if(DataType.equalsIgnoreCase("STOCKINDEX")) {
				QuoteData = StockIndex;
			}		
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return QuoteData;		 
	}
	
	//function to get the data out from the Quotes array which is LTP-Bid-Ask array corresponding 
	//to a paritcular Futures data
	public double GetQuotesDataWithExpiry(DataMaster DataMasterObj,String StockCode, String Expiry, String DataType) {
		String[][] Futures = DataMasterObj.Futures;
		double[][] Quotes = new double[Futures.length][3];
		double[] Volume = new double[Futures.length];
 		int StockIndex = 0;
		double QuoteData = 0;
		try {
			for(int i=0; i<Futures.length; i++) {
				if(StockCode.trim().equals(Futures[i][0].trim())) {
					StockIndex = i;
					break;
				}
			}
			
			if(Expiry.equals(DataMasterObj.ExpiryDatesCurrentExpiry[StockIndex])) {
				Quotes = DataMasterObj.Quotes;
				Volume = DataMasterObj.Volume;
			}
			else if(Expiry.equals(DataMasterObj.ExpiryDatesNextExpiry[StockIndex])) {
				Quotes = DataMasterObj.QuotesNextExpiry;
				Volume = DataMasterObj.VolumeNextExpiry;
			}			
			//else the expiry does not match with current/next expiry and report a 0 price error
			else {
				return 0;
			}
			
			if(DataType.equalsIgnoreCase("LTP")) {
				QuoteData = Quotes[StockIndex][0];
			}
			else if(DataType.equalsIgnoreCase("BID")) {
				QuoteData = Quotes[StockIndex][1];
			}
			else if(DataType.equalsIgnoreCase("ASK")) {
				QuoteData = Quotes[StockIndex][2];
			}		
			//sometime only the stocks index is required and hence pass that
			else if(DataType.equalsIgnoreCase("STOCKINDEX")) {
				QuoteData = StockIndex;
			}		
			else if(DataType.equalsIgnoreCase("VOLUME")) {
				QuoteData = Volume[StockIndex];
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return QuoteData;		 
	}
	
	//function to get the data out from the Quotes array which is LTP-Bid-Ask array corresponding 
	//to a paritcular Futures data
	public double GetPrevMinQuotesDataWithExpiry(DataMaster DataMasterObj,String StockCode, String Expiry, String DataType) {
		String[][] Futures = DataMasterObj.Futures;
		double[][] Quotes = new double[Futures.length][3];
		int StockIndex = 0;
		double QuoteData = 0;
		try {
			for(int i=0; i<Futures.length; i++) {
				if(StockCode.trim().equals(DataMasterObj.FuturesCode[i].trim())) {
					StockIndex = i;
					break;
				}
			}
			
			if(Expiry.equals(DataMasterObj.ExpiryDatesCurrentExpiry[StockIndex])) {
				Quotes = DataMasterObj.QuotesPrevMin;
			}
			else if(Expiry.equals(DataMasterObj.ExpiryDatesNextExpiry[StockIndex])) {
				Quotes = DataMasterObj.QuotesNextExpiryPrevMin;
			}			
			//else the expiry does not match with current/next expiry and report a 0 price error
			else {
				return 0;
			}
			
			if(DataType.equalsIgnoreCase("LTP")) {
				QuoteData = Quotes[StockIndex][0];
			}
			else if(DataType.equalsIgnoreCase("BID")) {
				QuoteData = Quotes[StockIndex][1];
			}
			else if(DataType.equalsIgnoreCase("ASK")) {
				QuoteData = Quotes[StockIndex][2];
			}		
			//sometime only the stocks index is required and hence pass that
			else if(DataType.equalsIgnoreCase("STOCKINDEX")) {
				QuoteData = StockIndex;
			}		
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return QuoteData;		 
	}

	
	//Function to read the quotes from the excel using DDE Java
	public void UpdateAllQuotes(DataMaster DataMasterObj){
		long EntryTime = System.currentTimeMillis();
		double[][] QuotesPrices = null;		
		double[][] QuotesPricesNextExpiry = null;				
		String[] StockCode = null;
		String[] StockCodeNextExpiry = null;		
		String[] LTP = null;
		String[] LTPNextExpiry = null;		
		String[] BID = null;
		String[] BIDNextExpiry = null;		
		String[] ASK = null;
		String[] ASKNextExpiry = null;		
		String[] ExpiryDates = null;
		String[] ExpiryDatesNextExpiry = null;
		String[] Volume = null;
		String[] VolumeNextExpiry = null;		
		
		String[] BIDQty = null;
		String[] BIDQtyNextExpiry = null;		
		String[] ASKQty = null;
		String[] ASKQtyNextExpiry = null;				
		
		DDEClient Client;
		Conversation QuoteSheet = null;
		try{
			//for the current expiry data
			QuotesPrices = new double[DataMasterObj.Futures.length][3];			
			//for the next expiry data
			QuotesPricesNextExpiry = new double[DataMasterObj.Futures.length][3];

			//start reading from the excel - for current expiry
			String StockCodeStr = "";String LTPStr = "";String BIDStr = "";String ASKStr = "";String ExpiryDateStr = "";String VolumeStr="";
			String BIDQtyStr = "";String ASKQtyStr = "";
			try{
				int CurrQuoteRow = DataMasterObj.Param.QuoteRow;
				Client = DDEClient.getInstance();
				QuoteSheet = Client.connect("Excel", DataMasterObj.Param.QuoteSheet);
				StockCodeStr = new String(QuoteSheet.request("R"+(CurrQuoteRow)+"C2"));
				BIDStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+1)+"C2"));
				ASKStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+2)+"C2"));
				LTPStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+3)+"C2"));
				ExpiryDateStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+4)+"C2"));
				VolumeStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+5)+"C2"));					
				BIDQtyStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+6)+"C2"));
				ASKQtyStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+7)+"C2"));
				
				QuoteSheet.close();
				Client.close();
			}
			catch (DDEException e){
				DataMasterObj.GlobalErrors = "FATAL_ERROR_CANNOT_READ_EXCEL_FILE";
				WriteLogFile("###ERROR### : Can not read current expiry quotes from Excel!");
			}
			StockCode = StockCodeStr.trim().split(",");
			LTP = LTPStr.trim().split(",");
			BID = BIDStr.trim().split(",");
			ASK = ASKStr.trim().split(",");
			ExpiryDates = ExpiryDateStr.trim().split(",");
			Volume = VolumeStr.trim().split(",");
			BIDQty = BIDQtyStr.trim().split(",");
			ASKQty = ASKQtyStr.trim().split(",");
			
			
			//start reading from the next expiry from excel - for NEXT expiry
			String StockCodeStrNextExpiry = "";String LTPStrNextExpiry = "";String BIDStrNextExpiry = "";String ASKStrNextExpiry = "";String ExpiryDateStrNextExpiry = "";String VolumeStrNextExpiry = "";
			String BIDQtyStrNextExpiry = "";String ASKQtyStrNextExpiry = "";
			try{
				int CurrQuoteRow = DataMasterObj.Param.QuoteRow;
				Client = DDEClient.getInstance();
				QuoteSheet = Client.connect("Excel", DataMasterObj.Param.QuoteSheet);
				StockCodeStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow)+"C3"));
				BIDStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+1)+"C3"));
				ASKStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+2)+"C3"));
				LTPStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+3)+"C3"));
				ExpiryDateStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+4)+"C3"));
				VolumeStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+5)+"C3"));				
				BIDQtyStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+6)+"C3"));
				ASKQtyStrNextExpiry = new String(QuoteSheet.request("R"+(CurrQuoteRow+7)+"C3"));

				QuoteSheet.close();
				Client.close();
			}
			catch (DDEException e){
				DataMasterObj.GlobalErrors = "FATAL_ERROR_CANNOT_READ_EXCEL_FILE";
				WriteLogFile("###ERROR### : Can not read next expiry quotes from Excel!");
			}
			StockCodeNextExpiry = StockCodeStrNextExpiry.split(",");
			LTPNextExpiry = LTPStrNextExpiry.trim().split(",");
			BIDNextExpiry = BIDStrNextExpiry.trim().split(",");
			ASKNextExpiry = ASKStrNextExpiry.trim().split(",");
			ExpiryDatesNextExpiry = ExpiryDateStrNextExpiry.trim().split(",");
			VolumeNextExpiry = VolumeStrNextExpiry.trim().split(",");
			BIDQtyNextExpiry = BIDQtyStrNextExpiry.trim().split(",");
			ASKQtyNextExpiry = ASKQtyStrNextExpiry.trim().split(",");			
			
			//initialize the quote / quotes next expiry first to 0
			for(int i=0;i<DataMasterObj.FuturesCode.length;i++) {
				for(int j=0;j<3;j++) {
					QuotesPrices[i][j] = 0;
					QuotesPricesNextExpiry[i][j] = 0;		
					DataMasterObj.Volume[i] = 0;
					DataMasterObj.VolumeNextExpiry[i] = 0;
					DataMasterObj.BIDQty[i] = 0;
					DataMasterObj.ASKQty[i] = 0;
					DataMasterObj.BIDQtyNextExpiry[i] = 0;
					DataMasterObj.ASKQtyNextExpiry[i] = 0;					
				}
			}
			ArrayList ExchangesAvailable = new ArrayList();
			int InstrumentPriceErrors = 0;
			
			for (int j=0;j<StockCode.length;j++){
				//if code of curr and next expiry dont match then throw error and exit
				if(! StockCode[j].trim().equals(StockCodeNextExpiry[j].trim())) {
					WriteLogFile("###ERROR### : Current and Next Expiries dont match for : " + StockCode[j] + "," + StockCodeNextExpiry[j]);
					DataMasterObj.GlobalErrors = "FATAL_ERROR_IN_EXCEL_INSTRUMENT_MISMATCH";
					InstrumentPriceErrors = 1;
					break;
				}
				
				//else continue with finding the right stock code
				for(int k=0;k<DataMasterObj.FuturesCode.length;k++) {
					if (StockCode[j].trim().equals(DataMasterObj.FuturesCode[k].trim())) {
						try{
							QuotesPrices[k][0]=Double.parseDouble(LTP[j].trim());
							QuotesPrices[k][1]=Double.parseDouble(BID[j].trim());
							QuotesPrices[k][2]=Double.parseDouble(ASK[j].trim());
							DataMasterObj.Volume[k]=Double.parseDouble(Volume[j].trim());
							DataMasterObj.BIDQty[k] = Double.parseDouble(BIDQty[j].trim());
							DataMasterObj.ASKQty[k] = Double.parseDouble(ASKQty[j].trim());							
							
						}catch (Exception e){
							QuotesPrices[k][0] = 0;
							QuotesPrices[k][1] = 0;
							QuotesPrices[k][2] = 0;
							DataMasterObj.Volume[k] = 0;
							DataMasterObj.BIDQty[k] = 0;
							DataMasterObj.ASKQty[k] = 0;														
						}
						
						try {
							QuotesPricesNextExpiry[k][0]=Double.parseDouble(LTPNextExpiry[j].trim());
							QuotesPricesNextExpiry[k][1]=Double.parseDouble(BIDNextExpiry[j].trim());
							QuotesPricesNextExpiry[k][2]=Double.parseDouble(ASKNextExpiry[j].trim());
							DataMasterObj.VolumeNextExpiry[k]=Double.parseDouble(VolumeNextExpiry[j].trim());
							DataMasterObj.BIDQtyNextExpiry[k] = Double.parseDouble(BIDQtyNextExpiry[j].trim());
							DataMasterObj.ASKQtyNextExpiry[k] = Double.parseDouble(ASKQtyNextExpiry[j].trim());																			
						}
						catch(Exception e) {
							QuotesPricesNextExpiry[k][0] = 0;
							QuotesPricesNextExpiry[k][1] = 0;							
							QuotesPricesNextExpiry[k][2] = 0;	
							DataMasterObj.VolumeNextExpiry[k] = 0;
							DataMasterObj.BIDQtyNextExpiry[k] = 0;
							DataMasterObj.ASKQtyNextExpiry[k] = 0;																			
						}
						
						DataMasterObj.ExpiryDatesCurrentExpiry[k] = ExpiryDates[j].trim();	
						DataMasterObj.ExpiryDatesNextExpiry[k] = ExpiryDatesNextExpiry[j].trim();
						if(ExpiryDates[j] != null && !ExpiryDates[j].trim().equals("")){
							DataMasterObj.Futures[k][DataMasterObj.Param.EarningsCol] = ConvertToYYYYMMDD(ExpiryDates[j].trim());
						}
				
						//clean the data and remove the outliers for current expiry
						if (QuotesPrices[k][0]<0 || QuotesPrices[k][1]<0 || QuotesPrices[k][2]<0 || QuotesPrices[k][0] == 0 || QuotesPrices[k][1] == 0 && QuotesPrices[k][2] == 0){
							QuotesPrices[k][0] = 0;QuotesPrices[k][1] = 0;QuotesPrices[k][2] = 0;DataMasterObj.Volume[k] = 0;	DataMasterObj.BIDQty[k] = 0;DataMasterObj.ASKQty[k]=0;
						}
						if (QuotesPricesNextExpiry[k][0]<0 || QuotesPricesNextExpiry[k][1]<0 || QuotesPricesNextExpiry[k][2]<0 || QuotesPricesNextExpiry[k][0] == 0 || QuotesPricesNextExpiry[k][1] == 0 && QuotesPricesNextExpiry[k][2] == 0){
							QuotesPricesNextExpiry[k][0] = 0;QuotesPricesNextExpiry[k][1] = 0;QuotesPricesNextExpiry[k][2] = 0;DataMasterObj.VolumeNextExpiry[k]=0;DataMasterObj.BIDQtyNextExpiry[k] = 0;DataMasterObj.ASKQtyNextExpiry[k]=0;
						}
						
						//add this exchange to the exchange list
						String CurrExchange = DataMasterObj.Futures[k][0];
						if(! ExchangesAvailable.contains(CurrExchange)) {
							ExchangesAvailable.add(CurrExchange);
						}
						
					//break from this loop now - this is matched for futures sheet	
					break;	
					}
				}
			}
						
			//check if all the prices for a given exchange has come or not
			int TotalActiveInstrumentInFutures = 0;
			for(int i=0;i<DataMasterObj.FuturesCode.length;i++) {
				String CurrExchange = DataMasterObj.Futures[i][0];
				if(ExchangesAvailable.contains(CurrExchange)) {
					TotalActiveInstrumentInFutures = TotalActiveInstrumentInFutures+1;					
				}
			}
			//if total instrument in active exchange != quotes data instruments then log an errro
			if(TotalActiveInstrumentInFutures != StockCode.length
						&& TotalActiveInstrumentInFutures != StockCodeNextExpiry.length) {
				WriteLogFile("###ERROR### : Instruments missing in quotes file - please check excel quotes sheet");
				DataMasterObj.GlobalErrors = "FATAL_ERROR_IN_EXCEL_INSTRUMENT_MISSING";
				InstrumentPriceErrors = 1;
			}
			
			//if there is error in current/next expiry values
			//and/or all instruments are not available then record all prices as 0
			if(InstrumentPriceErrors == 1) {
				for(int i=0;i<DataMasterObj.FuturesCode.length;i++) {
					for(int j=0;j<3;j++) {
						QuotesPrices[i][j] = 0;
						QuotesPricesNextExpiry[i][j] = 0;					
					}
				}
			}
			
			//set the quotes for current and next expiries here
			DataMasterObj.Quotes = QuotesPrices;
			DataMasterObj.QuotesNextExpiry = QuotesPricesNextExpiry;	
		}
		catch (Exception e){	
			e.printStackTrace();
			WriteLogFile("###ERROR### : Error in reading data for all asset - quotes sheet!");
			DataMasterObj.GlobalErrors = "FATAL_ERROR_CANNOT_READ_EXCEL_FILE1";
		}			
	}

	
	
	
	
	//Function to read the quotes from the excel using DDE Java
	public double[][] UpdateQuotes(DataMaster DataMasterObj){
		long EntryTime = System.currentTimeMillis();
		double[][] QuotesPrices = null;		
		String[] StockCode = null;
		String[] LTP = null;
		String[] BID = null;
		String[] ASK = null;
		String[] ExpiryDates = null;
		DDEClient Client;
		Conversation QuoteSheet = null;
		try{
			QuotesPrices = new double[DataMasterObj.Futures.length][3];
			StockCode = new String[DataMasterObj.Futures.length];
			LTP = new String[DataMasterObj.Futures.length];
			BID = new String[DataMasterObj.Futures.length];
			ASK = new String[DataMasterObj.Futures.length];
			ExpiryDates = new String[DataMasterObj.Futures.length];

			int RowIndex = 3;
			int ColIndex = 1;
			String StockCodeStr = "";
			String LTPStr = "";
			String BIDStr = "";
			String ASKStr = "";
			String ExpiryDateStr = "";
			try{
				int CurrQuoteRow = DataMasterObj.Param.QuoteRow;
				Client = DDEClient.getInstance();
				QuoteSheet = Client.connect("Excel", DataMasterObj.Param.QuoteSheet);
				StockCodeStr = new String(QuoteSheet.request("R"+(CurrQuoteRow)+"C2"));
				BIDStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+1)+"C2"));
				ASKStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+2)+"C2"));
				LTPStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+3)+"C2"));
				ExpiryDateStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+4)+"C2"));
				QuoteSheet.close();
				Client.close();
			}
			catch (DDEException e){
//				WriteLogFile(e.toString());
				WriteLogFile("###ERROR### : Can not read current expiry quotes from Excel!");
			}
			StockCode = StockCodeStr.split(",");
			LTP = LTPStr.split(",");
			BID = BIDStr.split(",");
			ASK = ASKStr.split(",");
			ExpiryDates = ExpiryDateStr.split(",");
			for (int i=0;i<StockCode.length;i++){				
				if (StockCode[i].trim().equals(DataMasterObj.FuturesCode[i].trim())){
					try{
						QuotesPrices[i][0]=Double.parseDouble(LTP[i].trim());
					}catch (Exception e){
						QuotesPrices[i][0] = 0;
					}
					try{
						QuotesPrices[i][1]=Double.parseDouble(BID[i].trim());
					}catch (Exception e){
						QuotesPrices[i][1] = 0;
					}
					try{
						QuotesPrices[i][2]=Double.parseDouble(ASK[i].trim());
					}catch (Exception e){
						QuotesPrices[i][2] = 0;
					}
					DataMasterObj.ExpiryDatesCurrentExpiry[i] = ExpiryDates[i].trim();						
				}else{
					QuotesPrices[i][0] = 0;
					QuotesPrices[i][1] = 0;
					QuotesPrices[i][2] = 0;
					WriteLogFile("Futures code does not match Arbitrage.xls, in current expiry for: " + DataMasterObj.FuturesCode[i].trim());
				}
				if (QuotesPrices[i][0]<0 || QuotesPrices[i][1]<0 || QuotesPrices[i][2]<0){
					QuotesPrices[i][0] = 0;
					QuotesPrices[i][1] = 0;
					QuotesPrices[i][2] = 0;	
				}
				if (QuotesPrices[i][0] == 0 || (QuotesPrices[i][1] == 0 && QuotesPrices[i][2] == 0)){
					QuotesPrices[i][0] = 0;
					QuotesPrices[i][1] = 0;
					QuotesPrices[i][2] = 0;	
				}
				RowIndex = RowIndex + 1;
			}
			
			//set the expiry dates here in the Futures values
			for(int i=0;i<StockCode.length;i++) {
				if(StockCode[i].trim().equals(DataMasterObj.FuturesCode[i])) {
					if(ExpiryDates[i] != null && !ExpiryDates.equals("")) {
						DataMasterObj.Futures[i][DataMasterObj.Param.EarningsCol] = ConvertToYYYYMMDD(ExpiryDates[i].trim());										
					}
				}
			}						
		}
		catch (Exception e){		
			WriteLogFile("###ERROR### : Error in reading data current expiry!");
		}			
		return QuotesPrices; 			
	}
	
	//convert 21MAY2012 to 20120512
	public String ConvertToYYYYMMDD(String ExpiryDate) {
		String CompleteDate = "";
		try {
			String Year = ExpiryDate.substring(5,9);
			String Month = ExpiryDate.substring(2,5);
			String Day = ExpiryDate.substring(0,2);
			
			String MonthInt = "";
			if(Month.equalsIgnoreCase("JAN")){MonthInt = "01";}
			if(Month.equalsIgnoreCase("FEB")){MonthInt = "02";}
			if(Month.equalsIgnoreCase("MAR")){MonthInt = "03";}
			if(Month.equalsIgnoreCase("APR")){MonthInt = "04";}
			if(Month.equalsIgnoreCase("MAY")){MonthInt = "05";}
			if(Month.equalsIgnoreCase("JUN")){MonthInt = "06";}
			if(Month.equalsIgnoreCase("JUL")){MonthInt = "07";}
			if(Month.equalsIgnoreCase("AUG")){MonthInt = "08";}
			if(Month.equalsIgnoreCase("SEP")){MonthInt = "09";}
			if(Month.equalsIgnoreCase("OCT")){MonthInt = "10";}
			if(Month.equalsIgnoreCase("NOV")){MonthInt = "11";}
			if(Month.equalsIgnoreCase("DEC")){MonthInt = "12";}
			
			CompleteDate = Year + MonthInt + Day;						
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return CompleteDate;
	}
	
	//convert 21MAY2012 to 20120512
	public String ConvertToDDMMMYYYY(String ExpiryDate) {
		String CompleteDate = "";
		try {
			String Year = ExpiryDate.substring(0,4);
			String Month = ExpiryDate.substring(4,6);
			String Day = ExpiryDate.substring(6,8);
			
			String MonthInt = "";
			if(Month.equalsIgnoreCase("01")){MonthInt = "JAN";}
			if(Month.equalsIgnoreCase("02")){MonthInt = "FEB";}
			if(Month.equalsIgnoreCase("03")){MonthInt = "MAR";}
			if(Month.equalsIgnoreCase("04")){MonthInt = "APR";}
			if(Month.equalsIgnoreCase("05")){MonthInt = "MAY";}
			if(Month.equalsIgnoreCase("06")){MonthInt = "JUN";}
			if(Month.equalsIgnoreCase("07")){MonthInt = "JUL";}
			if(Month.equalsIgnoreCase("08")){MonthInt = "AUG";}
			if(Month.equalsIgnoreCase("09")){MonthInt = "SEP";}
			if(Month.equalsIgnoreCase("10")){MonthInt = "OCT";}
			if(Month.equalsIgnoreCase("11")){MonthInt = "NOV";}
			if(Month.equalsIgnoreCase("12")){MonthInt = "DEC";}
			
			CompleteDate = Day + MonthInt + Year;						
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return CompleteDate;
	}
	
	//Function to read the quotes from the excel using DDE Java
	public void UpdateQuotesNextExpiry(DataMaster DataMasterObj){
		long EntryTime = System.currentTimeMillis();
		double[][] QuotesPrices = null;		
		String[] StockCode = null;
		String[] LTP = null;
		String[] BID = null;
		String[] ASK = null;
		String[] ExpiryDates = null;
		DDEClient Client;
		Conversation QuoteSheet = null;
		try{
			QuotesPrices = new double[DataMasterObj.Futures.length][3];
			StockCode = new String[DataMasterObj.Futures.length];
			LTP = new String[DataMasterObj.Futures.length];
			BID = new String[DataMasterObj.Futures.length];
			ASK = new String[DataMasterObj.Futures.length];
			ExpiryDates = new String[DataMasterObj.Futures.length];

			int RowIndex = 3;
			String StockCodeStr = "";
			String LTPStr = "";
			String BIDStr = "";
			String ASKStr = "";
			String ExpiryDatesStr = "";
			try{
				int CurrQuoteRow = DataMasterObj.Param.QuoteRow;
				Client = DDEClient.getInstance();
				QuoteSheet = Client.connect("Excel", DataMasterObj.Param.QuoteSheet);
				StockCodeStr = new String(QuoteSheet.request("R"+(CurrQuoteRow)+"C3"));
				BIDStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+1)+"C3"));
				ASKStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+2)+"C3"));
				LTPStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+3)+"C3"));
				ExpiryDatesStr = new String(QuoteSheet.request("R"+(CurrQuoteRow+4)+"C3"));
				QuoteSheet.close();
				Client.close();
			}
			catch (DDEException e){
				WriteLogFile("###ERROR### : Can not read next Expiry quotes from Excel!");
			}
			StockCode = StockCodeStr.split(",");
			LTP = LTPStr.split(",");
			BID = BIDStr.split(",");
			ASK = ASKStr.split(",");
			ExpiryDates = ExpiryDatesStr.split(",");
			for (int i=0;i<StockCode.length;i++){				
				if (StockCode[i].trim().equals(DataMasterObj.FuturesCode[i].trim())){
					try{
						QuotesPrices[i][0]=Double.parseDouble(LTP[i].trim());
					}catch (Exception e){
						QuotesPrices[i][0] = 0;
					}
					try{
						QuotesPrices[i][1]=Double.parseDouble(BID[i].trim());
					}catch (Exception e){
						QuotesPrices[i][1] = 0;
					}
					try{
						QuotesPrices[i][2]=Double.parseDouble(ASK[i].trim());
					}catch (Exception e){
						QuotesPrices[i][2] = 0;
					}
					//set the expiries here
					DataMasterObj.ExpiryDatesNextExpiry[i] = ExpiryDates[i].trim();	
				}else{
					QuotesPrices[i][0] = 0;
					QuotesPrices[i][1] = 0;
					QuotesPrices[i][2] = 0;
					WriteLogFile("Futures code does not match Arbitrage.xls, in next expiry for: " + DataMasterObj.FuturesCode[i].trim());
				}
				if (QuotesPrices[i][0]<0 || QuotesPrices[i][1]<0 || QuotesPrices[i][2]<0){
					QuotesPrices[i][0] = 0;
					QuotesPrices[i][1] = 0;
					QuotesPrices[i][2] = 0;	
				}
				
				if (QuotesPrices[i][0] == 0 || (QuotesPrices[i][1] == 0 && QuotesPrices[i][2] == 0)){
					QuotesPrices[i][0] = 0;
					QuotesPrices[i][1] = 0;
					QuotesPrices[i][2] = 0;	
				}

				RowIndex = RowIndex + 1;
			}
						
			//set the next expiry details here
			DataMasterObj.QuotesNextExpiry = QuotesPrices;
		}
		catch (Exception e){	
			WriteLogFile("###ERROR### : Error in reading data next expiry");
		}					
	}
	//function to read the quotes file and get the data directly in a n*3 matrix (LTP, Bid and Ask) values
	//Quotes file structure assumed
	
	//******************************************
	// Code    LTP  Bid  Ask
	//500085  72.5 72.4  72.6
	//500075   90.0 90.1  90.2
	//....
	//***********************************************	
	public double[][] ReadQuotesFile(DataMaster DataMasterObj) {
		String[][] Futures = DataMasterObj.Futures;
		double[][] QuotesPrices = null;
		try {			
			//first load the data from the file
			ArrayList QuotesList = LoadDataFromFile(DataMasterObj.Param.QuotesPath);
			QuotesPrices = new double[QuotesList.size()][3];
			
			//assuming that the quotes file is of the form - stock code, ltp, bid, ask
			//starting from 1 => as the first row is the header LTP, Bid, Ask etc.
			for(int i=0;i<QuotesList.size();i++) {
				//delim the data of the pairs list tab wise
				String data[] = ((String)QuotesList.get(i)).split("\t");								
				
				//check if the data is consistent with the Futures list or not
				if(data[0].trim().equals(DataMasterObj.FuturesCode[i].trim())) {
					//check if the bid and ask prices are consistent
					double Ltp = Double.parseDouble(data[1].trim());
					double Bid = Double.parseDouble(data[2].trim());
					double Ask = Double.parseDouble(data[3].trim());
					
					//check all the basic conditions for the data
					if(Ltp > 0 && Bid > 0 && Ask >= Bid) {
						//now read each pair and put into the Pairs array
						//starting from reading the 2nd col = LTP
						for(int j=1;j<data.length;j++) {
							QuotesPrices[i][j-1] = Double.parseDouble(data[j].trim());
						}															
					}
					//else there is inconcisteny in the data and fill it with zeros
					else {
						for(int j=1;j<data.length;j++) {
							QuotesPrices[i][j-1] = 0;
						}															
					}					
				}
				else {
					//the data is not consistent for this stock code and send 0
					for(int j=1;j<data.length;j++) {
						QuotesPrices[i][j-1] = 0;
					}									
				}				
			}					
		}
		catch(Exception e) {
			//dont throw any error on reading the quotes files
			//DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return QuotesPrices;
	}
	
	
	//function to get the live single array series for each pair
	//based on HistoricalTimeSeires and Current Bid Ask Values
	//Input: Historical time series, Live Bid and Ask
	//Output: Single time series of a pairs with live bid and ask at the end
	public double[] GetLiveSeries(int LengthOfSeries, int PairsIndex, double[][] PlnAvg) {
		double[] LivePlnAvg = new double[LengthOfSeries];		
		try {
			//make the live PlnAvg time series here
			//get the historical price series and then add the last one as live bid and ask
			for(int i=0; i<LengthOfSeries; i++) {
				LivePlnAvg[i] = PlnAvg[PairsIndex][PlnAvg[0].length-LengthOfSeries+i];								
			}
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return LivePlnAvg;
	}
	
	// Get Current Time 
	 public String NowDateTime() {
	    Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	    return sdf.format(cal.getTime());
	 }
	 
	 // Get Current Time 
	 public String NowDateTimeHighFreq(DataMaster DataMasterObj) {
	    Calendar cal = Calendar.getInstance();
	    int CurrSec = cal.get(Calendar.SECOND);
	    
	    int TimeGap = DataMasterObj.Param.HighFreqTimeGap;
	    //seeing if the bar is closer to 30 sec gap or 00 sec gap to put in right bucket
	    if(TimeGap == 60) {
	    	CurrSec = 0;
	    }
	    else if(Math.abs(CurrSec-TimeGap)%TimeGap <= 10 && CurrSec >= TimeGap) {
	    	CurrSec = TimeGap;
	    }
	    else {
	    	CurrSec = 0;
	    }
	    
	    Calendar cal2 = Calendar.getInstance();
	    cal2.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), CurrSec);

	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	    return sdf.format(cal2.getTime());
	 }
	 
	// Get Time gap b/w Current_Trade_open time & Current time 
	 public int TradeOpnTimeCheck(String CurrTrd_Opn_Time, DataMaster DataMasterObj, int MainStockIndex,double CurrCandleGap) {
	 	int Trd_Time_Check_Flag = 0 ; 	
	    try{
	    	double CurrBBGTimeSeriesGap = CurrCandleGap;
	    	String Curr_Time = NowDateTime();
	    			    	
			int TradeOpenTimeInSec = Integer.parseInt(CurrTrd_Opn_Time.substring(9,11))*3600+Integer.parseInt(CurrTrd_Opn_Time.substring(12,14))*60;
			int CurrentTimeInSec = Integer.parseInt(Curr_Time.substring(9,11))*3600+Integer.parseInt(Curr_Time.substring(12,14))*60;				
			long Trd_Minutes = 0;
			
			//if the trade opening and closing is in the same minutes then dont close the trade
			if(CurrTrd_Opn_Time.substring(0,8).equals(Curr_Time.substring(0,8)) && TradeOpenTimeInSec == CurrentTimeInSec) {
				Trd_Time_Check_Flag = 0 ;
				return Trd_Time_Check_Flag;
			}
			
			if(CurrTrd_Opn_Time.substring(0,8).equals(Curr_Time.substring(0,8))) {
				Trd_Minutes = (CurrentTimeInSec-TradeOpenTimeInSec)/60;
			}
			else {
				Calendar Cal1 = Calendar.getInstance();
				Calendar Cal2 = Calendar.getInstance();
				Cal1.set(Integer.parseInt(CurrTrd_Opn_Time.substring(0,4)),Integer.parseInt(CurrTrd_Opn_Time.substring(4,6))-1,Integer.parseInt(CurrTrd_Opn_Time.substring(6,8)));
				Cal2.set(Integer.parseInt(Curr_Time.substring(0,4)),Integer.parseInt(Curr_Time.substring(4,6))-1,Integer.parseInt(Curr_Time.substring(6,8)));
				int WorkingDays = getWorkingDaysBetweenTwoDates(Cal1.getTime(), Cal2.getTime());					
				
				Trd_Minutes = ((CurrentTimeInSec-DataMasterObj.MarketStartTimeInSecondsForEachAsset[MainStockIndex]) +
								(WorkingDays-1)*(DataMasterObj.MarketStopTimeInSecondsForEachAsset[MainStockIndex]-DataMasterObj.MarketStartTimeInSecondsForEachAsset[MainStockIndex])+
								(DataMasterObj.MarketStopTimeInSecondsForEachAsset[MainStockIndex]-TradeOpenTimeInSec))/60;
			}		
	    			    	
	    	if(Trd_Minutes % (CurrBBGTimeSeriesGap)==0){
	    		Trd_Time_Check_Flag = 1; 
	    	}
	    }
	    catch (Exception e) {
	    	 e.printStackTrace();
		}
	    return Trd_Time_Check_Flag;
	}
	 
		// Get Time gap b/w Current_Trade_open time & Current time 
	 public double TradeTimeMultiple(String CurrTrd_Opn_Time, DataMaster DataMasterObj, int MainStockIndex,double CurrCandleGap) {
	 	double TrdTimeMultiple = 0;
	    try{
	    	double CurrBBGTimeSeriesGap = CurrCandleGap;
	    	String Curr_Time = NowDateTime();
	    			    	
			int TradeOpenTimeInSec = Integer.parseInt(CurrTrd_Opn_Time.substring(9,11))*3600+Integer.parseInt(CurrTrd_Opn_Time.substring(12,14))*60;
			int CurrentTimeInSec = Integer.parseInt(Curr_Time.substring(9,11))*3600+Integer.parseInt(Curr_Time.substring(12,14))*60;				
			long Trd_Minutes = 0;
			
			if(CurrTrd_Opn_Time.substring(0,8).equals(Curr_Time.substring(0,8))) {
				Trd_Minutes = (CurrentTimeInSec-TradeOpenTimeInSec)/60;
			}
			else {
				Calendar Cal1 = Calendar.getInstance();
				Calendar Cal2 = Calendar.getInstance();
				Cal1.set(Integer.parseInt(CurrTrd_Opn_Time.substring(0,4)),Integer.parseInt(CurrTrd_Opn_Time.substring(4,6))-1,Integer.parseInt(CurrTrd_Opn_Time.substring(6,8)));
				Cal2.set(Integer.parseInt(Curr_Time.substring(0,4)),Integer.parseInt(Curr_Time.substring(4,6))-1,Integer.parseInt(Curr_Time.substring(6,8)));
				int WorkingDays = getWorkingDaysBetweenTwoDates(Cal1.getTime(), Cal2.getTime());					
				
				Trd_Minutes = ((CurrentTimeInSec-DataMasterObj.MarketStartTimeInSecondsForEachAsset[MainStockIndex]) +
								(WorkingDays-1)*(DataMasterObj.MarketStopTimeInSecondsForEachAsset[MainStockIndex]-DataMasterObj.MarketStartTimeInSecondsForEachAsset[MainStockIndex])+
								(DataMasterObj.MarketStopTimeInSecondsForEachAsset[MainStockIndex]-TradeOpenTimeInSec))/60;
			}		
	    			    	
			TrdTimeMultiple = (double)Trd_Minutes/(double)CurrBBGTimeSeriesGap;
	    }
	    catch (Exception e) {
	    	 e.printStackTrace();
		}
	    return TrdTimeMultiple;
	}

	 
	 public String DateString(Calendar Dates){
		 int DateYear = Dates.get(Calendar.YEAR);
		 int DateMonth = Dates.get(Calendar.MONTH);
		 int DateDate=Dates.get(Calendar.DATE);
		 if (DateMonth == 12){
			 DateMonth = 1;
		 }
		 else{
			DateMonth = DateMonth + 1;
		 }
		 String YearStr = Integer.toString(DateYear);
		 String MonthStr = Integer.toString(DateMonth);
		 String DateStr = Integer.toString(DateDate);
		 if (MonthStr.length() == 1){
			 MonthStr = "0" + MonthStr;
		 }
		 if (DateStr.length() == 1){
			DateStr = "0" + DateStr;
		 }
		 return (YearStr+MonthStr+DateStr);
	 }		
	 
	 public void WriteToFile(String FilePath, String StringToWrite, boolean Append) {
		 try {			 
				String lp = System.getProperty("line.separator");	
				FileWriter fstream = new FileWriter(FilePath, Append);
				BufferedWriter out = new BufferedWriter(fstream);			
				out.write(StringToWrite);
				out.write(lp);
				out.close();
		 }
		 catch(Exception e) {
			 DataMaster.logger.warning(e.toString());
			// e.printStackTrace();
		 }
	 }
	 
	 
	
	 // function for writing BBG file in reverse order of given ArrayList 
	 public void WriteToBBGFile(String FilePath, ArrayList BBGDataArrayList, boolean Append) {
		 try {	
				String lp = System.getProperty("line.separator");
				FileWriter fstream = new FileWriter(FilePath, Append);
				BufferedWriter out = new BufferedWriter(fstream);
				String StringToWrite = null ;
				for(int i=BBGDataArrayList.size()-1; i >-1; i--){
					StringToWrite = (String) BBGDataArrayList.get(i);
					out.write(StringToWrite);
					out.write(lp);
				}				
				out.close();
		 }
		 catch(Exception e) {
			 DataMaster.logger.warning(e.toString());
			// e.printStackTrace();
		 }
	 }
	 
//	 // function for writing the P&L information
//	 public void WriteTradePLData(DataMaster DataMasterObj) {
//		 Utils util = new Utils();
//		 try {	
//			 	// get string from quotes file
//				String CurrDateTime = NowDateTimeHighFreq(DataMasterObj);				
//				if(!DataMasterObj.PLTotalAsString.equals("")) {
//					String HighFreqStr = CurrDateTime + "\t" + DataMasterObj.PLTotalAsString+"\t"+DataMasterObj.ExposureString;
//					util.WriteToFile(DataMasterObj.Param.PLDataPath, HighFreqStr, true);							
//				}
//		 }
//		 catch(Exception e) {
//			 DataMaster.logger.warning(e.toString());
//			 e.printStackTrace();
//		 }		
//	 }
	 
	 // function for writing the P&L information
	 public void WriteTradePLData(DataMaster DataMasterObj) {
		 Utils util = new Utils();

		 try {	
			 	// get string from quotes file
				String CurrDateTime = NowDateTimeHighFreq(DataMasterObj);				
				if(DataMasterObj.PLTotal !=0) {
					String HighFreqStr = CurrDateTime + "\t" + DataMasterObj.PLTotal;
					util.WriteToFile(DataMasterObj.Param.PLDataPath, HighFreqStr, true);							
				}
	
		 }catch(Exception e) {
			 DataMaster.logger.warning(e.toString());
			 e.printStackTrace();
		 }		
	 }
	 
		// function for writing HighFreqData file 
	 int HighFreqLastTickDay = 0;
	 public long WriteHighFreqDataFile(DataMaster DataMasterObj) {
		 long HighFreqLastLineTime = 0;
		 Utils util = new Utils();
		 try {	
			 // get string from quotes file
				String CurrDateTime = NowDateTimeHighFreq(DataMasterObj);
				int DataConsistent = 1;
				DataHandler DataHandlerObj = new DataHandler();
				
				for(int j=0; j<DataMasterObj.Pairs.length ; j++){
					DataConsistent = 1;
					String StockCode1 = DataMasterObj.Pairs[j][0];
					String StockCode2 = DataMasterObj.Pairs[j][1];
						
					int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
					int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

					String DateOfCurrentExpiry = DataMasterObj.ExpiryDatesCurrentExpiry[1];

					String CurrFile = DataMasterObj.AssetDataDirectory+StockCode1+"-"+StockCode2+"_"+DateOfCurrentExpiry+".txt";
					
					//get the start and stop time for this asset class
					int ExchangeRunning = util.CheckExchangeRunning(DataMasterObj, StockIndex1);
					
					if(DataMasterObj.Quotes[StockIndex1][0] <= 0 || DataMasterObj.Quotes[StockIndex2][0] <=0) {
						DataConsistent = 0;
					}else{
						if(DataMasterObj.Param.OptionsTradingActive == 2) {
							double ImpliedVol1 = DataHandlerObj.GetImpliedVolatility(DataMasterObj, StockIndex1, "CurrExpiry", "");
							double ImpliedVol2 = DataHandlerObj.GetImpliedVolatility(DataMasterObj, StockIndex2, "CurrExpiry", "");
							
							if(ImpliedVol1 > 0 && ImpliedVol2 > 0) {
								DataMasterObj.PriceRatio[j]= ImpliedVol1 / ImpliedVol2;																						
							}
							else {
								DataConsistent = 0;
							}
						}
						else {						
							DataMasterObj.PriceRatio[j]=DataMasterObj.Quotes[StockIndex1][0]/DataMasterObj.Quotes[StockIndex2][0];
						}
					}
					
					String HighFreqStr = CurrDateTime+ "\t" + DataMasterObj.PriceRatio[j];
				
					int DataRepeat = 1;
					ArrayList HighFreqData = util.LoadDataFromFile(CurrFile);
					int EndIndex = HighFreqData.size()-1;
					if (EndIndex < DataMasterObj.Param.DataRepeatRows+1){
						DataRepeat = 0;
					}else{
						for (int i=0;i<DataMasterObj.Param.DataRepeatRows;i++){
							String[] HighFreq = ((String)HighFreqData.get(EndIndex-i)).split("\t");
							double QuoteData = Double.parseDouble(HighFreq[1]);
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
				 		util.WriteToFile(CurrFile, HighFreqStr, true);						
					}	
					else {
						if(DataConsistent == 0) {
							//WriteLogFile("Not writing data for " + CurrAsset + "... DataInconsistent");
						}
						if(DataRepeat== 1) {
							WriteLogFile("Not writing data for " + StockCode1 + "-" + StockCode2 + "... DataRepeat");
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
	 
	 
	 public void WriteClosePriceDataFutures(DataMaster DataMasterObj) {
		 Utils util = new Utils();
		 try {	
				String DateOfCurrentExpiry = DataMasterObj.ExpiryDatesCurrentExpiry[1];
				String DateOfNextExpiry = DataMasterObj.ExpiryDatesNextExpiry[1];
				
			 	String CurrDateTime = NowDateTimeHighFreq(DataMasterObj);
				String HighFreqStr = CurrDateTime;
				String BidFreqStr = CurrDateTime;
				String AskFreqStr = CurrDateTime;		
				
				String HighFreqStrNextExpiry = CurrDateTime;
				String BidFreqStrNextExpiry = CurrDateTime;
				String AskFreqStrNextExpiry = CurrDateTime;		
				int DataCleanCurrExpiry = 1;
				int DataCleanNextExpiry = 1;
				
				
				for(int j=0; j<DataMasterObj.Futures.length ; j++){
					String StockCode1 = DataMasterObj.FuturesCode[j];						
					int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");										
					HighFreqStr = HighFreqStr+ "\t" + DataMasterObj.Quotes[StockIndex1][0];
					BidFreqStr = BidFreqStr+ "\t" + DataMasterObj.Quotes[StockIndex1][1];
					AskFreqStr = AskFreqStr+ "\t" + DataMasterObj.Quotes[StockIndex1][2];
					
					if(DataCleanCurrExpiry == 1) {
						if(DataMasterObj.Quotes[StockIndex1][0] <= 0 || DataMasterObj.Quotes[StockIndex1][1] <= 0 || DataMasterObj.Quotes[StockIndex1][2] <= 0) {
							DataCleanCurrExpiry = 0;
						}
					}
					
					
					HighFreqStrNextExpiry = HighFreqStrNextExpiry + "\t" + DataMasterObj.QuotesNextExpiry[StockIndex1][0];
					BidFreqStrNextExpiry = BidFreqStrNextExpiry + "\t" + DataMasterObj.QuotesNextExpiry[StockIndex1][1];
					AskFreqStrNextExpiry = AskFreqStrNextExpiry + "\t" + DataMasterObj.QuotesNextExpiry[StockIndex1][2];					
				
					if(DataCleanNextExpiry == 1) {
						if(DataMasterObj.QuotesNextExpiry[StockIndex1][0] <= 0 || DataMasterObj.QuotesNextExpiry[StockIndex1][1] <= 0 || DataMasterObj.QuotesNextExpiry[StockIndex1][2] <= 0) {
							DataCleanNextExpiry = 0;
						}
					}
				}					
				
				if(DataCleanCurrExpiry == 1) {
					util.WriteToFile(DataMasterObj.AssetDataDirectory+"ClosePriceData_" + DateOfCurrentExpiry + ".txt", HighFreqStr, true);						
					util.WriteToFile(DataMasterObj.AssetDataDirectory+"BidPriceData_"+DateOfCurrentExpiry+".txt", BidFreqStr, true);						
					util.WriteToFile(DataMasterObj.AssetDataDirectory+"AskPriceData_"+DateOfCurrentExpiry+".txt", AskFreqStr, true);															
				}
				
				if(DataCleanNextExpiry == 1) {
					util.WriteToFile(DataMasterObj.AssetDataDirectory+"ClosePriceData_" + DateOfNextExpiry + ".txt", HighFreqStrNextExpiry, true);						
					util.WriteToFile(DataMasterObj.AssetDataDirectory+"BidPriceData_"+DateOfNextExpiry+".txt", BidFreqStrNextExpiry, true);						
					util.WriteToFile(DataMasterObj.AssetDataDirectory+"AskPriceData_"+DateOfNextExpiry+".txt", AskFreqStrNextExpiry, true);																							
				}
		 }
		 catch(Exception e) {
			 DataMaster.logger.warning(e.toString());
			 e.printStackTrace();
		 }
	 }


	// function for writing HighFreqData file for next expiries
	 public long WriteHighFreqDataFileNextExpiry(DataMaster DataMasterObj) {
		 long HighFreqLastLineTime = 0;
		 Utils util = new Utils();
		 try {	
			 // get string from quotes file
				String CurrDateTime = NowDateTimeHighFreq(DataMasterObj);
				int DataConsistent = 1;
				DataHandler DataHandlerObj = new DataHandler();

				//do not write for 100000 - only write data for non-dummy ones
				for(int j=0; j<DataMasterObj.Pairs.length ; j++){
					DataConsistent = 1;

					String StockCode1 = DataMasterObj.Pairs[j][0];
					String StockCode2 = DataMasterObj.Pairs[j][1];
						
					int StockIndex1 = (int) util.GetQuotesData(DataMasterObj, StockCode1, "STOCKINDEX");
					int StockIndex2 = (int) util.GetQuotesData(DataMasterObj, StockCode2, "STOCKINDEX");												

					String DateOfNextExpiry = DataMasterObj.ExpiryDatesNextExpiry[StockIndex1];

					if(DataMasterObj.QuotesNextExpiry[StockIndex1][0] <= 0 || DataMasterObj.QuotesNextExpiry[StockIndex2][0] <=0 && DateOfNextExpiry == null || DateOfNextExpiry.equals("")) {
						DataConsistent = 0;
						continue;
					}else{
						if(DataMasterObj.Param.OptionsTradingActive == 2) {
							
							if(StockCode1.equals("NIFTY_6200_CE") && StockCode2.equals("NIFTY_6100_CE")) {
								int aa = 10;
							}
							
							double ImpliedVol1 = DataHandlerObj.GetImpliedVolatility(DataMasterObj, StockIndex1, "NextExpiry", "");
							double ImpliedVol2 = DataHandlerObj.GetImpliedVolatility(DataMasterObj, StockIndex2, "NextExpiry", "");
							
							if(ImpliedVol1 > 0 && ImpliedVol2 > 0) {
								DataMasterObj.PriceRatioNextExpiry[j]= ImpliedVol1 / ImpliedVol2;																						
							}
							else {
								DataConsistent = 0;
								continue;
							}
						}
						else {						
							DataMasterObj.PriceRatioNextExpiry[j]=DataMasterObj.QuotesNextExpiry[StockIndex1][0]/DataMasterObj.QuotesNextExpiry[StockIndex2][0];
						}
					}
					String HighFreqStr = CurrDateTime+ "\t" + DataMasterObj.PriceRatioNextExpiry[j];

					String CurrFile = DataMasterObj.AssetDataDirectory+StockCode1+"-"+StockCode2+"_"+DateOfNextExpiry+".txt";					
					//get the start and stop time for this asset class
					int ExchangeRunning1 = util.CheckExchangeRunning(DataMasterObj, StockIndex1);
					int ExchangeRunning2 = util.CheckExchangeRunning(DataMasterObj, StockIndex2);
									
					int DataRepeat = 1;
					ArrayList HighFreqData = util.LoadDataFromFile(CurrFile);
					int EndIndex = HighFreqData.size()-1;
					if (EndIndex < DataMasterObj.Param.DataRepeatRows+1){
						DataRepeat = 0;
					}else{
						for (int i=0;i<DataMasterObj.Param.DataRepeatRows;i++){
							String[] HighFreq = ((String)HighFreqData.get(EndIndex-i)).split("\t");
							double QuoteData = Double.parseDouble(HighFreq[1]);
							//j=0 is for 100000 - whichis always 1
							if (DataMasterObj.PriceRatioNextExpiry[j] != QuoteData){
								DataRepeat = 0;
							}
						}
					}
					
					//j=0 is dummy so use it as it is
					if(j==0) {
						DataConsistent = 1;DataRepeat=0;ExchangeRunning1=1;ExchangeRunning2=1;
					}
					
					if(DataConsistent == 1 && DataRepeat== 0 && ExchangeRunning1 == 1 && ExchangeRunning2 == 1) {
				 		util.WriteToFile(CurrFile, HighFreqStr, true);						
					}	
					else {
						if(DataConsistent == 0) {
							WriteLogFile("Not writing next expiry data for " + StockCode1 + "-" + StockCode2 + "... DataInconsistent");
						}
						if(DataRepeat== 1) {
							WriteLogFile("Not writing next expiry data for " + StockCode1 + "-" + StockCode2 + "... DataRepeat");
						}
						if(ExchangeRunning1== 0 && ExchangeRunning2 == 0) {
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
	
	 
	 
	 public int GetTimeInSeconds(Calendar CurrTime){
		 int SecondsValue = 0;
		 try{
			 int CurrHour = CurrTime.get(Calendar.HOUR_OF_DAY);
			 int CurrMin = CurrTime.get(Calendar.MINUTE);
			 int CurrSec = CurrTime.get(Calendar.SECOND);
			 SecondsValue = (((60*CurrHour)+CurrMin)*60)+CurrSec;
		 }
		catch(Exception e){
			//e.printStackTrace();
		}
		return (SecondsValue);
		
	 }
	 
	 public int GetTimeInSecondsForStr(String CurrDateTime){
		 int SecondsValue = 0;
		 try{
			 int CurrHour = Integer.parseInt(CurrDateTime.substring(9,11));
			 int CurrMin = Integer.parseInt(CurrDateTime.substring(12,14));
			 int CurrSec = Integer.parseInt(CurrDateTime.substring(15,17));
			 SecondsValue = (((60*CurrHour)+CurrMin)*60)+CurrSec;
		 }
		catch(Exception e){
			e.printStackTrace();
		}
		return (SecondsValue);		
	 }

	 
	 public Calendar GetDateFromString(String DateStr){
		 Calendar DateFromString = Calendar.getInstance();
		 try{
		 SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		 Date ThisDate = (Date)sdf.parse(DateStr);
		 DateFromString.setTime(ThisDate);
		 
		 }catch(Exception e){}
		 return DateFromString;
	 }
	 // Function to get multiplier from future sheet for the given StockCode
	 public double GetMultiplier(String CurrStockCode, DataMaster DataMasterObj){
		 double CurrMultiplier = 0;
		 try{
			 for (int i=0; i<DataMasterObj.FuturesCode.length; i++){
				 if(CurrStockCode.equals(DataMasterObj.FuturesCode[i])){
					 CurrMultiplier = Double.valueOf(DataMasterObj.Futures[i][12]);
					 break;
				 }
			 }
		 }
		catch(Exception e){
			//e.printStackTrace();
		}
		return CurrMultiplier;
		
	 }
	 
	
	 public int CheckConnection(DataMaster DataMasterObj){
			String EmptyLine = "#"+System.getProperty("line.separator");
			int CheckConnection = 1;
			if (DataMasterObj.Param.AutoMode.equals("ON")){
				try{
					if(DataMasterObj.SocketSMTP == null){
//						WriteLogFile("Dis-Connected..." + "\r");
						DataMasterObj.ConnectSocket();
						CheckConnection = 1;
					}
					else if(DataMasterObj.SocketSMTP.isConnected()){
						DataMasterObj.os.write(EmptyLine);
						DataMasterObj.os.flush();
//						System.out.print("Connected...      " + "\r");
						CheckConnection = 0;
					}
					else{
//					System.out.print("Dis-Connected..." + "\r");
					DataMasterObj.ConnectSocket();
					CheckConnection = 1;
					}
				}
			catch (Exception e){
//				System.out.print("Dis-Connected..." + "\r");
				DataMasterObj.ConnectSocket();
				CheckConnection = 1;
			}
			}
			else{
				CheckConnection = 0;
			}
			return CheckConnection;
		 }
	 
		public int GetNoOfTrades(DataMaster DataMasterObj){
			int GetNoOfTrades = 0;
			try{
				ArrayList TradeList = LoadDataFromFile(DataMasterObj.RootDirectory + "Trades2.txt");
				GetNoOfTrades = TradeList.size();
			}
			catch (Exception e){
				e.printStackTrace();
			}
			return GetNoOfTrades;
		}

		public void CleanBackTestFiles(DataMaster DataMasterObj){
			try{
				if(DataMasterObj.Param.RunningMode.equals("TEST")){
					WriteToFile(DataMasterObj.Param.HighFreqDataFilePath, null , false);
					WriteToFile(DataMasterObj.Param.PositionPath, null , false);
					WriteToFile(DataMasterObj.Param.TradesPath, null , false);
					WriteToFile(DataMasterObj.Param.TrdPath, null , false);
					WriteToFile(DataMasterObj.Param.ClosePosnPath, null , false);
					WriteToFile(DataMasterObj.Param.Trades2Path, null , false);
				}	
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}

		
		public void SendSampleData(DataMaster DataMasterObj){
			try{
				ArrayList SampleList = LoadDataFromFile(DataMasterObj.Param.SamplePath);
				if (SampleList.size()>0){
					String EndLine = System.getProperty("line.separator");
					String TradeString = ((String)SampleList.get(0)).trim()+EndLine;
					DataMasterObj.os.write(TradeString);
					DataMasterObj.os.flush();
					FileWriter fstream = new FileWriter(DataMasterObj.Param.SamplePath, false);				
				    BufferedWriter out = new BufferedWriter(fstream);
				    out.close();
					}
			}catch (Exception e){
				DataMasterObj.ConnectSocket();
			}
		}
		
		 //fucntiuon to get the volatility of the stock by using the BBG bucket
		 public double GetVolatility(DataMaster DataMasterObj, int StockIndex) {
			 double CurrVol = 0;
			 try {
				 double[] ReturnArr = DataMasterObj.ReturnSeries[StockIndex];
				 
				 //get the stdev of this return series
				 Stats StatsObj = new Stats();
				 double Mean = StatsObj.mean(ReturnArr);
				 double Std = StatsObj.std(ReturnArr, Mean); 
				 
				 //convert the bucketed volatility into annualized vol
				 //double BucketTimeInMin = DataMasterObj.Param.BBGTimeSeriesGap;
				 //double CurrAnnualVol = Std*Math.sqrt((24*60)/BucketTimeInMin)*Math.sqrt(252);			 			 
				 
				 //convert into perc and send across
				 CurrVol = 100*Std;
				 
				 //if the value is not a number then give the vol as 0
				 if(Double.isNaN(CurrVol)) {
					 CurrVol = 0;
				 }
			 }
			 catch(Exception e) {
//				 e.printStackTrace();
			 }
			 return CurrVol;
		 }
		 
		 public void CreateReturnSeries(DataMaster DataMasterObj) {
			 try {
				 double[][] ReturnArr = new double[DataMasterObj.Pairs.length][DataMasterObj.Param.BBGTimeSeriesLength-1];
				 
				 for(int j=0;j<DataMasterObj.Pairs.length;j++) {
					 int PairIndex = j;
					 ArrayList CurrBBGList = DataMasterObj.BbgList[j];
					 for(int i=0;i<CurrBBGList.size()-1;i++) {
						 String CurrLineData = (String) CurrBBGList.get(i);
						 String NextLineData = (String) CurrBBGList.get(i+1);
						 
						 String[] CurrDataArr = CurrLineData.split("\t");
						 String[] NextDataArr = NextLineData.split("\t");
						 
						 //get the bucket return here
						 ReturnArr[PairIndex][i] = Math.log(Double.valueOf(NextDataArr[1])/Double.valueOf(CurrDataArr[1]));				 
					 }				 
				 }
				 
				 DataMasterObj.ReturnSeries = ReturnArr;
			 }
			 catch(Exception e) {
				 e.printStackTrace();
			 }	 
		 }
		 
		 public double CheckCorrelRisk(DataMaster DataMasterObj, String StockCode1, String StockCode2, ArrayList PositionList) {
			 double CorrelRisk = 0;
			 Utils util = new Utils();
			 try {
				 //to check correl of non 100000 type values only
				 //and not of dummy trade
				int PairIndex = util.GetPairsIndex(StockCode1, StockCode2, DataMasterObj.Pairs);
				
				double CurrCorrel = 0;
				for(int i=0;i<PositionList.size();i++) {
					CurrCorrel = 0;
					String PositionData[] = ((String)PositionList.get(i)).split("\t");
					String PositionStockCode1 = PositionData[1];
					String PositionStockCode2 = PositionData[2];
					int PositionPairIndex = util.GetPairsIndex(PositionStockCode1, PositionStockCode2, DataMasterObj.Pairs);
					CurrCorrel = getPearsonCorrelation(DataMasterObj.ReturnSeries[PairIndex], DataMasterObj.ReturnSeries[PositionPairIndex]);					
					
					//if the risk exceeds critical value then report correlation risk
					//correl risk 1 is avaoided for bet sizing there can be multiple positions
					if(CurrCorrel >= DataMasterObj.Param.CriticalCorrelValue && CurrCorrel != 1) {
						CorrelRisk = 1;
						return CorrelRisk;
					}
				}
			 }
			 catch(Exception e) {
				 e.printStackTrace();
			 }
			 return CorrelRisk;
		 }
		 
		 public static double getPearsonCorrelation(double[] scores1,double[] scores2){
			 double result = 0;	        
			 try {
			        double sum_sq_x = 0;
			        double sum_sq_y = 0;
			        double sum_coproduct = 0;
			        double mean_x = scores1[0];
			        double mean_y = scores2[0];
			        for(int i=2;i<scores1.length+1;i+=1){
			            double sweep =Double.valueOf(i-1)/i;
			            double delta_x = scores1[i-1]-mean_x;
			            double delta_y = scores2[i-1]-mean_y;
			            sum_sq_x += delta_x * delta_x * sweep;
			            sum_sq_y += delta_y * delta_y * sweep;
			            sum_coproduct += delta_x * delta_y * sweep;
			            mean_x += delta_x / i;
			            mean_y += delta_y / i;
			        }
			        double pop_sd_x = (double) Math.sqrt(sum_sq_x/scores1.length);
			        double pop_sd_y = (double) Math.sqrt(sum_sq_y/scores1.length);
			        double cov_x_y = sum_coproduct / scores1.length;
			        result = cov_x_y / (pop_sd_x*pop_sd_y);
		        }
			 	catch(Exception e) {
			 		e.printStackTrace();
			 	}
		        return result;
		    }
		 
		public int CheckExchangeRunning(DataMaster DataMasterObj, int FuturesIndex) {
			int ExchangeRunning = 1;
			String StartTime = DataMasterObj.Futures[FuturesIndex][6];
			String EndTime = DataMasterObj.Futures[FuturesIndex][7];
			
			//if either start time or stop time has 0 or does not contain any extra timing - has 0 etc...then return as it is
			if(StartTime.equals("0") || EndTime.equals("0")) {
				return ExchangeRunning;
			}
			
			try {
				Utils util = new Utils();
				double CurrTime = (double) util.GetTimeInSeconds(Calendar.getInstance());
				
				double StartTimeLong = util.GetTimeInSecForStr(StartTime);
				double EndTimeLong = util.GetTimeInSecForStr(EndTime);
								
				//check is the exchange has started or not
				if(CurrTime < StartTimeLong || CurrTime > EndTimeLong) {
					ExchangeRunning = 0;
					return ExchangeRunning;
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return ExchangeRunning;
		}
		
		public long GetTimeInSecForStr(String TimeStr) {
			long TimeInHrs = 0;
			try {
				int Hour = 0;
				int Minute = 0;
				if(TimeStr.length() == 3) {
					Hour = Integer.parseInt(TimeStr.substring(0,1));
					Minute = Integer.parseInt(TimeStr.substring(1,3));
				}
				else if(TimeStr.length() == 4) {
					Hour = Integer.parseInt(TimeStr.substring(0,2));
					Minute = Integer.parseInt(TimeStr.substring(2,4));					
				}
				TimeInHrs = Hour*3600+Minute*60;								
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return TimeInHrs;
		}
		
		//used for intra day trading in PlTrd
		public static String GetDateValueForRowIndex(ArrayList HighFreqDataAll, int RowIndex) {
			String DateStr = "";
			try {
				String CurrHighFreqRow = (String) HighFreqDataAll.get(RowIndex);
				String[] HighFreqRowArr = CurrHighFreqRow.split("\t");
				DateStr = HighFreqRowArr[0].substring(0,8);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return DateStr;
		}
		
		//setting different BBGTimeseries gap for each stock
		public void SetBBGTimeSeriesGapForEachAsset(DataMaster DataMasterObj) {
			try {
				double CurrAssetBBGTimeGap = DataMasterObj.Param.BBGTimeSeriesGap;
				double VolCutOffValue = DataMasterObj.Param.VolCutOffForDynamicLot;
				for(int i=0;i<DataMasterObj.Futures.length;i++) {					
					//for the dummy stock use the data as it is
					if(i==0) {
						DataMasterObj.BBGTimeSeriesGapForEachAsset[i] = DataMasterObj.Param.BBGTimeSeriesGap;
						DataMasterObj.VolCutOffForDynamicLotEachAsset[i] = DataMasterObj.Param.VolCutOffForDynamicLot;
						continue;
					}
					
					//set the BBG time series Gap in Col = 8
					CurrAssetBBGTimeGap = Double.parseDouble(DataMasterObj.Futures[i][8]);										
					//if the gap is less then 10 min then it possibly is 0 or 1 and hence use the deault value
					if(CurrAssetBBGTimeGap == 0 || CurrAssetBBGTimeGap == 1 || CurrAssetBBGTimeGap < 10 || CurrAssetBBGTimeGap > 500) {
						DataMasterObj.BBGTimeSeriesGapForEachAsset[i] = DataMasterObj.Param.BBGTimeSeriesGap;
					}
					//else use the value which is given in futures sheet
					else {
						DataMasterObj.BBGTimeSeriesGapForEachAsset[i] = CurrAssetBBGTimeGap;					
					}
					
					
					//setting the vol cut off valus next in Col = 9
					VolCutOffValue = Double.parseDouble(DataMasterObj.Futures[i][9]);						
					//if the gap is less then 10 min then it possibly is 0 or 1 and hence use the deault value
					if(VolCutOffValue == 0 || VolCutOffValue < 10) {
						DataMasterObj.VolCutOffForDynamicLotEachAsset[i] = DataMasterObj.Param.VolCutOffForDynamicLot;
					}
					//else use the value which is given in futures sheet
					else {
						DataMasterObj.VolCutOffForDynamicLotEachAsset[i] = VolCutOffValue;					
					}										
				}							
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		
		//setting different Market Time in Hours for each asset class
		public void SetMarketTimeinHoursForEachAsset(DataMaster DataMasterObj) {
			try {
				Utils util = new Utils();
				double CurrMarketTimeInHours = DataMasterObj.Param.BBGTimeSeriesGap;
				for(int i=0;i<DataMasterObj.Futures.length;i++) {
					//for the dummy stock use the data as it is
					if(i==0) {
						DataMasterObj.MarketTimeInHoursForEachAsset[i] = DataMasterObj.Param.MarketRunTimeInHrs;
						DataMasterObj.MarketStartTimeInSecondsForEachAsset[i] = DataMasterObj.Param.ModelStartTime;
						DataMasterObj.MarketStopTimeInSecondsForEachAsset[i] = DataMasterObj.Param.ModelStopTime;						
						continue;
					}
					
					String ExchangeStartTime = DataMasterObj.Futures[i][6];
					String ExchangeEndTime = DataMasterObj.Futures[i][7];
					
					if(ExchangeStartTime.equals("0") || ExchangeEndTime.equals("0")) {
						DataMasterObj.MarketTimeInHoursForEachAsset[i] = DataMasterObj.Param.MarketRunTimeInHrs;
						DataMasterObj.MarketStartTimeInSecondsForEachAsset[i] = DataMasterObj.Param.ModelStartTime;
						DataMasterObj.MarketStopTimeInSecondsForEachAsset[i] = DataMasterObj.Param.ModelStopTime;						
					}
					else {
						double MarketTimeInHoursAsset = (util.GetTimeInSecForStr(ExchangeEndTime)-util.GetTimeInSecForStr(ExchangeStartTime))/3600;
						//if market time is in between 0 and 24 hours
						if(MarketTimeInHoursAsset > 0 && MarketTimeInHoursAsset < 25) {
							DataMasterObj.MarketTimeInHoursForEachAsset[i] = MarketTimeInHoursAsset;							
							DataMasterObj.MarketStartTimeInSecondsForEachAsset[i] = util.GetTimeInSecForStr(ExchangeStartTime);
							DataMasterObj.MarketStopTimeInSecondsForEachAsset[i] = util.GetTimeInSecForStr(ExchangeEndTime);						
						}
						else {
							DataMasterObj.MarketTimeInHoursForEachAsset[i] = DataMasterObj.Param.MarketRunTimeInHrs;							
							DataMasterObj.MarketStartTimeInSecondsForEachAsset[i] = DataMasterObj.Param.ModelStartTime;
							DataMasterObj.MarketStopTimeInSecondsForEachAsset[i] = DataMasterObj.Param.ModelStopTime;						
						}
					}					
				}							
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		 public static int getWorkingDaysBetweenTwoDates(Date startDate, Date endDate) {			    
			Calendar startCal;
		    Calendar endCal;
		    startCal = Calendar.getInstance();
		    startCal.setTime(startDate);
		    endCal = Calendar.getInstance();
		    endCal.setTime(endDate);
		    int workDays = 0;

		    //Return 0 if start and end are the same
		    if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
		        return 0;
		    }

		    if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
		        startCal.setTime(endDate);
		        endCal.setTime(startDate);
		    }

		    do {
		        startCal.add(Calendar.DAY_OF_MONTH, 1);
		        if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
		            ++workDays;
		        }
		    } while (startCal.getTimeInMillis() < endCal.getTimeInMillis());

		    return workDays;
		}
		 
		 
		 public void WriteLogFile(String LogString){
			 WriteToFile("data\\LogLine.txt", LogString, true);
		 }
		 public void WriteLogFile1(String LogString){		
			 Calendar cal = Calendar.getInstance();
			 SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd|HH:mm:ss");
			 String CurrTime =  sdf.format(cal.getTime());
			 WriteToFile("data\\LogLine1.txt", CurrTime + " : " + LogString, true);
		 }
		 
		 public static void infoBox(String infoMessage, String location)
		    {
		        JOptionPane.showMessageDialog(null, infoMessage, "Info: " + location, JOptionPane.INFORMATION_MESSAGE);
		    }
		
		public double[] GetOUValues(DataMaster DataMasterObj) {
			 double OUVal= 0;
			 double[] OUValues = new double[DataMasterObj.Pairs.length];
			 DataMasterObj.VRTValues = new double[DataMasterObj.Pairs.length];
			 try {
				 Stats stats = new Stats();				 
				 OUValues[0] = 0;
				 //calculate for all other stocks one by one
				 for(int j=0;j<DataMasterObj.Pairs.length;j++) {
					 ArrayList CurrBBGList = DataMasterObj.BbgList[j];
					 double[] Price = new double[DataMasterObj.BbgList[j].size()];	
					 for(int i=0;i<CurrBBGList.size();i++) {
						 String CurrLineData = (String) CurrBBGList.get(i);	
						 String[] CurrDataArr = CurrLineData.split("\t");
						 Price[i] = Double.valueOf(CurrDataArr[1]);
					 }				 
					 OUVal = 100*stats.RSI(Price);
					 OUValues[j] = OUVal;
					 DataMasterObj.VRTValues[j] = stats.MACD(Price, 5);
				 }			 
			 }
			 catch(Exception e) {
				 e.printStackTrace();
			 }
			 return OUValues;		 
		 } 
	 public void CheckForExpiry(DataMaster DataMasterObj){
			String s="ftp://samssara:samss123$@ftp.samssara.com/Temp/"+DataMasterObj.Param.ClientUniqueCode+"-Expiry.txt";
			try{
				URL u = new URL(s);
				URLConnection uc = u.openConnection();
				uc.setConnectTimeout(20*1000);
				uc.setReadTimeout(20*1000);
				uc.setDoOutput(true);
				java.io.OutputStream out = uc.getOutputStream();
				
			    String br = "\r\n";
			    String WriteStringArray="";

			    RiskManager RiskObj = new RiskManager();
			    for (int i=0;i<DataMasterObj.FuturesCode.length;i++){
			    	int ExpiryRisk = RiskObj.CalcLiveRisk(DataMasterObj, DataMasterObj.FuturesCode[0], DataMasterObj.FuturesCode[i]);
			    	if (ExpiryRisk == 1){
			    		WriteStringArray = WriteStringArray + "Tender period for "+DataMasterObj.FuturesCode[i]+" : "+DataMasterObj.ExpiryDatesCurrentExpiry[i]+br;
			    	}
			    }
		    	out.write(WriteStringArray.getBytes());
		    	out.write(br.getBytes());					   
		    	out.close();
			 }catch (Exception e){
				 
			 }
	 }
	 
		//Get the price on which the execution needs to be done - based on the short term mean revrsion of the prices
		public double GetExecutionPrice2(DataMaster DataMasterObj, String StockCode, String OrderType, String ExpiryCode, String ExpiryNo) {
			double TradePrice = 0;
			try {
				int StockIndex = (int) GetQuotesData(DataMasterObj, StockCode, "STOCKINDEX");
				
				String BidAskSequencyStr = GetBidAskSequenceStr(DataMasterObj, StockIndex);
				double TradesOnBidAskTotal = GetTradesOnBidOrAskTotal(DataMasterObj, StockIndex);
				double GetTradePerSecDiffVal =  GetTradePerSecDiff(DataMasterObj, StockIndex);
				//double OULtp = GetOULtp(DataMasterObj, StockIndex);
				
				//get the live quotes from the excel sheet - and record the ltp, bid and ask - just for the sake of it
				double ModelLtp = GetQuotesDataWithExpiry(DataMasterObj, StockCode, ExpiryCode, "LTP");
				double ModelBid = GetQuotesDataWithExpiry(DataMasterObj, StockCode, ExpiryCode, "BID");
				double ModelAsk = GetQuotesDataWithExpiry(DataMasterObj, StockCode, ExpiryCode, "ASK");
				String CurrPriceStr = ">" + ModelBid  + ">" + ModelAsk + ">" + ModelLtp;
								
				//make decisions
				if(OrderType.equals("BUY")) {
						//too many trades on offer - hence snap the offer directly
						if(GetTradePerSecDiffVal >= DataMasterObj.Param.ExecutionCutOff) {						
							TradePrice = ModelAsk;	
						}
						else {
							TradePrice = ModelBid;														
						}
				}
				if(OrderType.equals("SELL")) {
						//too many trades happening on the bid - hence snap the bid directly
						if(GetTradePerSecDiffVal <= -1 * DataMasterObj.Param.ExecutionCutOff) {
							TradePrice = ModelBid;
						}
						else {
							TradePrice = ModelAsk;																
						}
				}
				
				String FormatTradesOnBidAsk = DataMasterObj.Param.PriceFormat.format(TradesOnBidAskTotal);
				String TradePerSecDiffStr = DataMasterObj.Param.PriceFormat1.format(GetTradePerSecDiffVal);
				WriteLogFile1("BID-ASK DECISION TAKEN: " + StockCode+ "," + OrderType + "," + CurrPriceStr + "," + TradePrice + "," + "E2_"+FormatTradesOnBidAsk + "_" + TradePerSecDiffStr +"|"+BidAskSequencyStr);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return TradePrice;
		}
		
		
		public String GetBidAskSequenceStr(DataMaster DataMasterObj, int StockIndex) {
			String BidAskSequence = "";
			try {
				ArrayList BidListSequence = DataMasterObj.BidSequenceHF.get(StockIndex);
				ArrayList AskListSequence = DataMasterObj.AskSequenceHF.get(StockIndex);
				ArrayList LtpListSequence = DataMasterObj.LtpSequenceHF.get(StockIndex);
				ArrayList VolumeListSequence = DataMasterObj.VolumeSequenceHF.get(StockIndex);
				ArrayList TimeListSequence = DataMasterObj.TimeSequenceHF.get(StockIndex);				
				
				//get all the 13 element
				for(int i=0;i<BidListSequence.size();i++) {
					BidAskSequence = BidAskSequence+"|"+i+"|"+BidListSequence.get(i)+"|"+AskListSequence.get(i)+"|"+LtpListSequence.get(i)+"|"+VolumeListSequence.get(i);					
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return BidAskSequence.trim();
		}

		//get the number of trades done on bid/ask in the last 1 min
		public double GetTradesOnBidOrAskTotalNoVolume(DataMaster DataMasterObj, int StockIndex) {
			double NoOfTradesOnBidAsk = 0;
			try {
				ArrayList BidListSequence = DataMasterObj.BidSequenceHF.get(StockIndex);
				ArrayList AskListSequence = DataMasterObj.AskSequenceHF.get(StockIndex);
				ArrayList LtpListSequence = DataMasterObj.LtpSequenceHF.get(StockIndex);
				
				for(int i=0;i<BidListSequence.size();i++) {
					double CurrBid = (Double)BidListSequence.get(i);
					double CurrAsk = (Double)AskListSequence.get(i);
					double CurrLtp = (Double)LtpListSequence.get(i);
					
					if(CurrLtp > (CurrBid+CurrAsk)/2) {
						NoOfTradesOnBidAsk = NoOfTradesOnBidAsk+1;
					}
					else if(CurrLtp < (CurrBid+CurrAsk)/2) {
						NoOfTradesOnBidAsk = NoOfTradesOnBidAsk-1;
					}
				}
				
				//calculate the % here of the trades on bid/ask
				if(BidListSequence.size() > 0) {
					NoOfTradesOnBidAsk = NoOfTradesOnBidAsk/(BidListSequence.size());					
				}
				else {
					NoOfTradesOnBidAsk = 0;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return NoOfTradesOnBidAsk;
		}
		
		//get the number of trades done on bid/ask in the last 1 min
		public double GetTradesOnBidOrAskTotal(DataMaster DataMasterObj, int StockIndex) {
			double NoOfTradesOnBidAsk = 0;
			double TotalValidTrades = 0;
			double TotalVolume = 0;
			try {
				ArrayList BidListSequence = DataMasterObj.BidSequenceHF.get(StockIndex);
				ArrayList AskListSequence = DataMasterObj.AskSequenceHF.get(StockIndex);
				ArrayList LtpListSequence = DataMasterObj.LtpSequenceHF.get(StockIndex);
				ArrayList VolumeListSequence = DataMasterObj.VolumeSequenceHF.get(StockIndex);				
				
				for(int i=1;i<BidListSequence.size();i++) {
					double CurrBid = (Double)BidListSequence.get(i);
					double CurrAsk = (Double)AskListSequence.get(i);
					double CurrLtp = (Double)LtpListSequence.get(i);
					double CurrVolume = (Double)VolumeListSequence.get(i);
					double PrevVolume = (Double)VolumeListSequence.get(i-1);
					TotalVolume = TotalVolume+CurrVolume;
					
					if(CurrVolume > PrevVolume && PrevVolume != 0) {
						TotalValidTrades = TotalValidTrades+1;
						if(CurrLtp > (CurrBid+CurrAsk)/2) {
							NoOfTradesOnBidAsk = NoOfTradesOnBidAsk+1;
						}
						else if(CurrLtp < (CurrBid+CurrAsk)/2) {
							NoOfTradesOnBidAsk = NoOfTradesOnBidAsk-1;
						}						
					}
				}
				
				//calculate the % here of the trades on bid/ask
				if(TotalValidTrades > 0) {
					NoOfTradesOnBidAsk = NoOfTradesOnBidAsk/TotalValidTrades;					
				}
				else {
					NoOfTradesOnBidAsk = 0;
				}
				
				//if all the volumes are 0 then no volume is recorded in arbitrage sheet and hence following normal bid/ask scheule
				if(TotalVolume == 0) {
					NoOfTradesOnBidAsk = GetTradesOnBidOrAskTotalNoVolume(DataMasterObj, StockIndex);
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return NoOfTradesOnBidAsk;
		}
		
		public double GetTradePerSecDiff(DataMaster DataMasterObj, int StockIndex) {
			double TradePerSecDiffVal = 0;
			try {
				double AskTradePerSec = GetTradePerSec("ask", DataMasterObj, StockIndex);
				double BidTradePerSec = GetTradePerSec("bid", DataMasterObj, StockIndex);
				TradePerSecDiffVal = AskTradePerSec-BidTradePerSec;				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return TradePerSecDiffVal;
		}
		
		public double GetTradePerSec(String BidAsk, DataMaster DataMasterObj, int StockIndex) {
			double TradePerSec = 0;
			try {
				int MomC = 0;
				int CountT = 0;
				int TotalCount = 0;
				
				double MomDistance = 0;
				double PrevMomPoint = -1;
				double CurrMomPoint = -1;
				double TotalVolume = 0;
				
				ArrayList BidListSequence = DataMasterObj.BidSequenceHF.get(StockIndex);
				ArrayList AskListSequence = DataMasterObj.AskSequenceHF.get(StockIndex);
				ArrayList LtpListSequence = DataMasterObj.LtpSequenceHF.get(StockIndex);
				ArrayList VolumeListSequence = DataMasterObj.VolumeSequenceHF.get(StockIndex);				
				
				for(int i=1;i<BidListSequence.size();i++) {
					double CurrBid = (Double)BidListSequence.get(i);
					double CurrAsk = (Double)AskListSequence.get(i);
					double CurrLtp = (Double)LtpListSequence.get(i);
					double CurrVolume = (Double)VolumeListSequence.get(i);
					double PrevVolume = (Double)VolumeListSequence.get(i-1);
					CurrMomPoint = -1;
					TotalCount = TotalCount + 1;
					TotalVolume = TotalVolume+CurrVolume;
					
					if(CurrVolume > PrevVolume && PrevVolume != 0) {
						if(BidAsk.equals("ask")) {
							if(CurrLtp > (CurrBid+CurrAsk)/2) {
								MomC = MomC+1;
								CurrMomPoint = i;
								if(MomC == 1) PrevMomPoint = CurrMomPoint;
							}
						}
						if(BidAsk.equals("bid")) {
							if(CurrLtp < (CurrBid+CurrAsk)/2) {
								MomC = MomC+1;
								CurrMomPoint = i;
								if(MomC == 1) PrevMomPoint = CurrMomPoint;
							}
						}
											    
						//calculating the momentum in terms of unit trade / time taken for the trade
						if (MomC > 1 && CurrMomPoint != -1 && CurrMomPoint != PrevMomPoint) {
					    	MomDistance = MomDistance + (1 / (CurrMomPoint - PrevMomPoint));
					    	if(MomDistance > 0) {
				            	CountT = CountT + 1;
					    	}
					    	PrevMomPoint = CurrMomPoint;
					    }						
					}
				}
				
				//calculate the total momentum/total num of possible trades - as trade per second
				if (MomC == 0) {
			    	TradePerSec = 0;
				}
			    else if (MomC == 1 && TotalCount > 0) {
			    	TradePerSec = 0;
			    }
			    else if (MomC > 1 && TotalCount > 0) {
			    	TradePerSec = MomDistance / TotalCount;
			    }	
				
				//if the volume is not found then make the trade values as 0
				if(TotalVolume == 0) {
					TradePerSec = 0;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}			
			return TradePerSec;
		}
		
		//recording the bid ask sequence here
		public void RecordBidAskSequenceRollingHF(DataMaster DataMasterObj) {
			try {
				//if the sequency no is 1 				
				for(int i=0;i<DataMasterObj.FuturesCode.length;i++) {
					String StockCode = DataMasterObj.FuturesCode[i];
					RiskManager RiskMan = new RiskManager();					
					int ExpiryRisk = RiskMan.CalcLiveRisk(DataMasterObj, StockCode, DataMasterObj.FuturesCode[0]);
					double ModelLtp = 0;double ModelBid = 0;double ModelAsk=0;double ModelVolume=0;double ModelTime = 0;
					if(ExpiryRisk == 0) {
						 ModelBid = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesCurrentExpiry[i], "BID");
						 ModelAsk = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesCurrentExpiry[i], "ASK");					 
						 ModelLtp = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesCurrentExpiry[i], "LTP");
						 ModelVolume = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesCurrentExpiry[i], "VOLUME");
						 ModelTime = GetTimeInSeconds(Calendar.getInstance());
					}
					else {
						 ModelBid = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesNextExpiry[i], "BID"); 
						 ModelAsk = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesNextExpiry[i], "ASK");										
						 ModelLtp = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesNextExpiry[i], "LTP");
						 ModelVolume = GetQuotesDataWithExpiry(DataMasterObj, StockCode, DataMasterObj.ExpiryDatesNextExpiry[i], "VOLUME");
						 ModelTime = GetTimeInSeconds(Calendar.getInstance());
					}
					
					//get the arraylist
					ArrayList BidListSequence = new ArrayList();
					ArrayList AskListSequence = new ArrayList();
					ArrayList LtpListSequence = new ArrayList();
					ArrayList VolumeListSequence = new ArrayList();
					ArrayList TimeListSequence = new ArrayList();					

					for(int j=0;j<DataMasterObj.BidSequenceHF.get(i).size();j++) {
						BidListSequence.add(j, ((ArrayList)DataMasterObj.BidSequenceHF.get(i)).get(j));
						AskListSequence.add(j, ((ArrayList)DataMasterObj.AskSequenceHF.get(i)).get(j));
						LtpListSequence.add(j, ((ArrayList)DataMasterObj.LtpSequenceHF.get(i)).get(j));
						VolumeListSequence.add(j, ((ArrayList)DataMasterObj.VolumeSequenceHF.get(i)).get(j));						
						TimeListSequence.add(j, ((ArrayList)DataMasterObj.TimeSequenceHF.get(i)).get(j));											
					}
					
					//roll the sequence of BidAsk here
					for(int j=0;j<BidListSequence.size();j++){
						//in the last array store the latest values
						if(j == BidListSequence.size()-1) {
							BidListSequence.set(j, ModelBid);
							AskListSequence.set(j, ModelAsk);
							LtpListSequence.set(j, ModelLtp);
							VolumeListSequence.set(j, ModelVolume);
							TimeListSequence.set(j, ModelTime);							
						}
						//else shift the data one point to the left
						else {
							BidListSequence.set(j, BidListSequence.get(j+1));
							AskListSequence.set(j, AskListSequence.get(j+1));
							LtpListSequence.set(j, LtpListSequence.get(j+1));
							VolumeListSequence.set(j, VolumeListSequence.get(j+1));
							TimeListSequence.set(j, TimeListSequence.get(j+1));							
						}						
					}
										
					//set data for the stock here
					DataMasterObj.BidSequenceHF.set(i, BidListSequence);
					DataMasterObj.AskSequenceHF.set(i, AskListSequence);
					DataMasterObj.LtpSequenceHF.set(i, LtpListSequence);
					DataMasterObj.VolumeSequenceHF.set(i, VolumeListSequence);	
					DataMasterObj.TimeSequenceHF.set(i, TimeListSequence);						
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void InitizlizeBidAskSequence(DataMaster DataMasterObj) {
			try {
				DataMasterObj.BidSequenceHF = new ArrayList();
				DataMasterObj.AskSequenceHF = new ArrayList();
				DataMasterObj.LtpSequenceHF = new ArrayList();
				DataMasterObj.VolumeSequenceHF = new ArrayList();	
				DataMasterObj.TimeSequenceHF = new ArrayList();					
				
				for(int i=0;i<DataMasterObj.FuturesCode.length;i++) {
					ArrayList<Double> BidListSequence = new ArrayList();
					ArrayList<Double> AskListSequence = new ArrayList();
					ArrayList<Double> LtpListSequence = new ArrayList();	
					ArrayList<Double> VolumeListSequence = new ArrayList();	
					ArrayList<Double> TimeListSequence = new ArrayList();						
					
					for(int j=0;j<30;j++) {
						BidListSequence.add(new Double(0));
						AskListSequence.add(new Double(0));
						LtpListSequence.add(new Double(0));
						VolumeListSequence.add(new Double(0));	
						TimeListSequence.add(new Double(0));							
					}										
					
					//add to the global list
					DataMasterObj.BidSequenceHF.add(BidListSequence);
					DataMasterObj.AskSequenceHF.add(AskListSequence);
					DataMasterObj.LtpSequenceHF.add(LtpListSequence);						
					DataMasterObj.VolumeSequenceHF.add(VolumeListSequence);
					DataMasterObj.TimeSequenceHF.add(TimeListSequence);					
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		public double YesterdayPL(DataMaster DataMasterObj){
			double YesterdayPL = 0;
			try{
				String CurrentDate = NowDateTime().substring(0,8);
				ArrayList<String> PLList = LoadDataFromFile(DataMasterObj.Param.PLDataPath);
				for (int i=PLList.size()-1;i>0;i--){
					String[] PLData = PLList.get(i).split("\t");
					if (!CurrentDate.equals(PLData[0].substring(0, 8))){
						YesterdayPL = Double.parseDouble(PLData[1]);
						break;
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			return YesterdayPL;
		}
		
		public double GetFinalRatio(double Price1, double Price2) {
			double FinalRatio = 0;
			try {
				//FinalRatio = Price1/Price2;
				FinalRatio = Math.log(Price1) - Math.log(Price2);				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return FinalRatio;
		}


}
