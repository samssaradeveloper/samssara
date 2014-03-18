import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.*;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DataMaster {
	
	//in the constructor firstly clean the earlier log files
	public DataMaster(String RootDirectoryStr) {
		try {
			FileWriter fstream = new FileWriter("TestLog.log", false);
	        BufferedWriter out = new BufferedWriter(fstream);
	        out.write("Starting the log for today...");
		    out.close();		
		}
		catch(Exception e) {
			//e.printStackTrace();
		}
	}	

	DBMain DBObj = new DBMain();
	
	public static Logger logger;	
	Parameters Param;
	String[][] Futures;
	String[][] Decision; 
	String[][] DecisionInverse;
	String[] FuturesCode;
	String[] ExchangeCode;
	String[][] Pairs;
	double[][] DecisionMagnitude;
	double[][] DecisionMagnitudeInverse;	
	double[] VRTValues;
	double[][] Return1;
	double[][] Return2; 
	double[][] Dbr;
	double[][] PLot1;
	double[][] Quotes;
	double[][] QuotesNextExpiry;
	double[] Volume;
	double[] VolumeNextExpiry;
	double[] BIDQty;
	double[] ASKQty;
	double[] BIDQtyNextExpiry;
	double[] ASKQtyNextExpiry;	
	double[][] QuotesPrevMin;
	double[][] QuotesNextExpiryPrevMin;
	double[] PriceRatio;
	double[] PriceRatioNextExpiry;
	String[] ExpiryDatesCurrentExpiry;	
	String[] ExpiryDatesNextExpiry;
	int[] NetPositionCurrentExpiry;
	int[] NetPositionNextExpiry;
	int DBConnectionStatus;
	int[] Trd;
	String RootDirectory;
	Socket SocketSMTP;
	Writer os;
	Vector TestDataVector;
	ArrayList[] BbgList;
	ArrayList[] BbgListInverse;	
	ArrayList[] BbgListDynamic;
	double[][] ReturnSeries;
	int[] TradeCarryOverDoneForThisPair;
	String AssetDataDirectory;
	double[] BBGTimeSeriesGapForEachAsset;
	int[] CurrDecisionLength;
	double[] MarketTimeInHoursForEachAsset;
	long[] MarketStartTimeInSecondsForEachAsset;
	long[] MarketStopTimeInSecondsForEachAsset;
	double[] VolCutOffForDynamicLotEachAsset;
	double GrossExposure;
	double PLTotal;
	double DailyPL;
	double YesterdayPL = 0;
	String DailyPLAsString="";
	String GrossExposureAsString = "";
	String ConnectionStatus;

	String PLTotalAsString = "";
	String PLNSEAsString = "";
	String PLMCXAsString = "";
	String PLNCDEXAsString = "";
	String PLCURRAsString = "";
	String ExposureString = "";
	double[] CurrStocksBBGTimeSeriesGap;
	int[] NetPosition;
	boolean StopStrategy = false;
	boolean StrategyRunStatus = false;
	String StrategyStatus = "STOPPED";
	String[] InstrumentCode;
	double[] OUValues;
	String GlobalErrors = "";
	
	public ArrayList<ArrayList> BidSequenceHF;
	public ArrayList<ArrayList> AskSequenceHF;
	public ArrayList<ArrayList> LtpSequenceHF;
	public ArrayList<ArrayList> VolumeSequenceHF;	
	public ArrayList<ArrayList> TimeSequenceHF;		
	
	//data handlers
	ArrayList TimeListAllAssetCurrExpiry;
	ArrayList PriceListAllAssetCurrExpiry;
	ArrayList TimeListAllAssetNextExpiry;
	ArrayList PriceListAllAssetNextExpiry;
	
	public void setParam(Parameters ParamData) {
		Param = ParamData;
	}
	public void setFutures(String[][] FuturesData) {
		Futures = FuturesData;
	}
	public void setDecision(String[][] DecisionValues) {
		Decision = DecisionValues;
	}
	public void setFuturesCode(String[] FuturesCodeData) {
		FuturesCode = FuturesCodeData;
	}	
	public  void setPairs(String[][] PairsData) {
		Pairs = PairsData;				
	}
	public  void setReturn1(double[][] Return1Data) {
		Return1 = Return1Data;
	}
	public  void setReturn2(double[][] Return2Data) {
		Return2 = Return2Data;
	}
	public void setDbr(double[][] DbrData) {
		Dbr = DbrData;
	}
	public void setPLot1(double[][] PLot1Data) {
		PLot1 = PLot1Data;
	}
	public void setQuotes(double[][] QuotesData) {
		Quotes = QuotesData;
	}
	public void setRootDirectory(String RootDirectoryStr) {
		RootDirectory = RootDirectoryStr;
	}
	public void setTrd(int[] TrdData){
		Trd = TrdData;
	}
	public void setTestDataVector(Vector TestDataVectorObj){
		TestDataVector = TestDataVectorObj;
	}	
	
	//this will be for logging information in the logger
	public void ConnectSocket(){
		try {
			//Connecting to the socket
			SocketSMTP = new Socket(Param.Server, Param.Port);
			//Opening up Buffered output stream to write to the socket
			os = new OutputStreamWriter(SocketSMTP.getOutputStream());
			os = new BufferedWriter(os);
			SocketSMTP.setKeepAlive(true);
			if (SocketSMTP.isConnected()){
				//System.out.print("***Connected***");
				DataMaster.logger.info(Param.CurrentCountry+" : Connected to : "+Param.Server + " Port : "+Param.Port);
			}
		}
		catch(Exception e){
			//System.out.print("###Connection Error###");
			DataMaster.logger.info(Param.CurrentCountry+" : Connection Error...");

		}
	}
	static {
	    try {
	      boolean append = true;
	      FileHandler fh = new FileHandler("TestLog.log", append);
	      fh.setFormatter(new SimpleFormatter());
	      logger = Logger.getLogger("TestLog");
	      logger.addHandler(fh);
	      logger.setUseParentHandlers(false);
	    }
	    catch (IOException e) {
	      DataMaster.logger.warning(e.toString());
	      //e.printStackTrace();
	    }
	}

}
