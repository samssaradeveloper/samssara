public class Stats {
	//function to calculate the mean of a time series
	public double mean(double data[]) {
		double mean = 0;
		try {
			for(int i=0;i<data.length;i++) {
				mean =mean+data[i];
			}
			mean = mean / data.length;
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return mean;
	}
	
	//function to calculate the std of a time series given its mean value
	public double std(double data[], double mean) {
		double std = 0;
		try {
			for(int i=0;i<data.length;i++) {
				std =std+((data[i]-mean)*(data[i]-mean));
			}
			std = Math.sqrt(std / (data.length-1));
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return std;
	}
	
	//ou
	public double RSI(double data[]) {
		double r = 0 ;
		double RSI1 = 0 ;
		double Sx = 0 ;
		double Sy = 0 ;
		double Sxx = 0 ;
		double Sxy = 0 ;
		int c = data.length ;
		
		try {
			
		for(int i=0;i< c-1 ;i++) {   
			Sx = Sx + data[i] ;
	        Sy = Sy + data[i+1] ;
	        Sxx = Sxx + (data[i] * data[i]);
	        Sxy = Sxy + (data[i] * data[i+1]);
		}
			r = (((c - 1) * Sxy) - (Sx * Sy)) / (((c - 1) * Sxx) - (Sx * Sx)) ;
		    RSI1 = 1 - r ;
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return RSI1;
	}
	
	//vrt
	public double MACD(double data[], int  period) {
		
		int n = data.length ; 	//get array lenght 	
		double s1 = 0 ; 
		double sq = 0 ; 
		double t = 0 ; 
		double t1 = 0 ; 
		int q = period ;
		double MACD1 = 0 ; 
		double m = 0; 
		
		try {
		double mu = (data[n-1]-data[0])/n ;  // get mean of given array 
		
		for(int i=1 ;i< n ;i++) {             
			// s1 gives variance of 1 period difference data series
			t = data[i] - data[i-1] - mu ;
			s1 = s1 + ((t * t) / (n - 1)) ;			
		}
		
			 double n1 = Double.parseDouble(Integer.toString(n - q + 1)); 
			 double d1 = Double.parseDouble(Integer.toString(n-q)); 
			 m = n1 * (d1/n); // get mean of given array	
		
		 for(int i= q ;i< n ;i++) {
			// sq gives variance of q period difference data series
			t1 = (data[i] - data[i - q] - (q * mu));
			sq = sq + ((t1 * t1) / m) ;
		}
		
		MACD1 = sq / (s1 * q) ;
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return MACD1;
	}
	
	//vrp
	public double Stochastic(double data[], int period) {
		double sum = 0 ;
		double vr = 0 ;
		int q = period ;
		double Stochastic1 = 0 ;
		
		try {
		for(int i=2;i< q+1 ;i++) {   
			vr = MACD(data, i) ;
			sum = sum + (vr*vr) ; 
		}
		
		Stochastic1 = Math.sqrt(sum / (q - 1)) ;
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return Stochastic1;
	}
	

	
	
	public double beta(double[] y, double[] x) {		
		double beta = 0;
		try {
			//first pass: read in data, compute xbar and ybar
	        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
	        int n=0;
	        for(int i=0;i<y.length;i++) {
	            sumx  += x[i];
	            sumx2 += x[i]*x[i];
	            sumy  += y[i];
	            n++;
	        }
	        double xbar = sumx / n;
	        double ybar = sumy / n;

	        // second pass: compute summary statistics
	        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
	        for (int i = 0; i < n; i++) {
	            xxbar += (x[i] - xbar) * (x[i] - xbar);
	            yybar += (y[i] - ybar) * (y[i] - ybar);
	            xybar += (x[i] - xbar) * (y[i] - ybar);
	        }
	        double beta1 = xybar / xxbar;
	        beta = Math.abs(1-beta1);	        
		}
		catch(Exception e) {
			DataMaster.logger.warning(e.toString());
			//e.printStackTrace();
		}
		return beta;
	}
	
	public double BlackScholesImpliedVolatility(double S, double X, double RF, double T, double YIELD, double MPR, double MAXERR, String ch) {
		//	'Black-Scholes Function to calculate European option prices and Greeks
		//	'S = stock price
		//	'X = exercise price
		//	'RF = risk-free rate
		//	'T = time to expiration
		//	'YIELD = dividend yield
		//	'MPR = market price of option
		//	'MAXERR = maximum acceptable difference between market price and model price
		//	'ch = choice - see below
		//	'     Put the choice word in quotes
		//	'       call =   European call price
		//	'       put =   European put price
		
		double BlackScholesImpliedVolatility = 0;
		try {
			double Pi = 3.1415926535;
			double Diff = MAXERR + 1;
			double SIGGUESS = Math.sqrt(Math.abs((Math.log(S * Math.exp(-YIELD * T) / X) + RF * T) * (2 / T)));	
				
			while(Diff > MAXERR) {
				double dsub1 = (Math.log(S * Math.exp(-YIELD * T) / X) + (RF + Math.pow(SIGGUESS, 2) / 2) * T) / (SIGGUESS * Math.sqrt(T));
				double dsub2 = dsub1 - SIGGUESS * Math.sqrt(T);
				double Nd1 = cumulativeDistribution(dsub1);
				double Nd2 = cumulativeDistribution(dsub2);
				
				double BSP = 0;  
				if(ch.equalsIgnoreCase("CE")) {
				     BSP = S * Math.exp(-YIELD * T) * Nd1 - X * Math.exp(-RF * T) * Nd2;			  
				}
				  
				if(ch.equals("PE")) {
			        BSP = X * Math.exp(-RF * T) * (1 - Nd2) - S * Math.exp(-YIELD * T) * (1 - Nd1);			
				}
				Diff = Math.abs(MPR - BSP);
				//Diff = MPR-BSP;
				if(Diff < MAXERR) {
					break;
				}
				
				SIGGUESS = SIGGUESS - ((BSP - MPR) * Math.exp(-1*Math.pow(dsub1 , 2) / 2) * Math.sqrt(2 * Pi)) / (S * Math.exp(-YIELD * T) * Math.sqrt(T));
			}
			BlackScholesImpliedVolatility = SIGGUESS;				
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return BlackScholesImpliedVolatility;
		}

		private static final double P = 0.2316419;
		private static final double B1 = 0.319381530;
		private static final double B2 = -0.356563782;
		private static final double B3 = 1.781477937;
		private static final double B4 = -1.821255978;
		private static final double B5 = 1.330274429;
		
		public static double cumulativeDistribution(double x) {
		    double t = 1 / (1 + P * Math.abs(x));
		    double t1 = B1 * Math.pow(t, 1);
		    double t2 = B2 * Math.pow(t, 2);
		    double t3 = B3 * Math.pow(t, 3);
		    double t4 = B4 * Math.pow(t, 4);
		    double t5 = B5 * Math.pow(t, 5);
		    double b = t1 + t2 + t3 + t4 + t5;
		    double cd = 1 - standardNormalDistribution(x) * b;
		    return x < 0 ? 1 - cd : cd;
		}

		public static double standardNormalDistribution(double x) {
		    double top = Math.exp(-0.5 * Math.pow(x, 2));
		    double PI = 3.1415926535;
		    double bottom = Math.sqrt(2 * PI);
		    return top / bottom;
		}

	
	
	
}
