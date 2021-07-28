package eu.iv4xr.ux.pxtesting;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import com.opencsv.CSVReader;

public class CSVImport {
	
	public static int[] ImportFromCSV(String Filename, String Path) throws IOException {
		
		String filepath= Path+ File.separator +Filename+".csv";
	    CSVReader reader = new CSVReader(new FileReader(filepath));
	      String [] nextLine;
	      int height = 0;
	      int width = 0;
	      int floornum=0;
	      while ((nextLine = reader.readNext()) != null && floornum!=2) {
	    	  
	    	  if(nextLine[0].startsWith("|w"))	floornum++;
	    	  
	    	  if(nextLine[0].startsWith("b"))	continue;
	    	  else height++;
	    	  
	    	  if(width<nextLine.length) width=nextLine.length;
	      }
	      return new int[] {height-1,width}; 
	}
    
}
