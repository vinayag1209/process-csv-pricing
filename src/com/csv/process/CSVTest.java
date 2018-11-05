/**
 * 
 */
package com.csv.process;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import com.csv.model.CarrierRange;
import com.csv.model.CarrierZipTerminal;
import com.csv.model.MasterZip;
import com.csv.model.PreOutput;

/**
 * @author uyr334f
 *
 */
public class CSVTest {

	/**
	 * @param args
	 */

	private static String carrierRangeCSV = "C:/3G-TM/TransitFiles/Carrier Range.csv";
	private static String masterZipsCSV = "C:/3G-TM/TransitFiles/Master Zips.csv";
	private static String PDIBZipsCSV = "C:/3G-TM/TransitFiles/PDIBzips.csv";
	private static String SEKWZipsCSV = "C:/3G-TM/TransitFiles/SEKWzips.csv";
	private static String preOutputCSV = "C:/3G-TM/TransitFiles/output/pre_output.csv";
	private static String finalOutputCSV = "C:/3G-TM/TransitFiles/output/FINAL_output.csv";
	private static List<CarrierRange> carrierrangeZips;
	private static List<MasterZip> masterZips;
	private static List<CarrierZipTerminal> sekwZips;
	private static List<CarrierZipTerminal> pdibZips;
	private static final String FILE_HEADER_PRE = "ZIP,TERMINAL,SERVICEOUTBOUND,DAYSOUTBOUND,SERVICEINBOUND,DAYSINBOUND";
	private static final String FILE_HEADER_POST = "LOWZIP,HIGHZIP,TERMINAL,SERV_IN,SERV_OUT,DAYS_IN,DAYS_OUT";
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		processTransitFiles();
	}

	private static void processTransitFiles() throws Exception {
		// calling readcsv to create list of objects from all CSVs.
		readCSV();
		/*
		 * Pre-output file go through list of masterzips, - for each zip check which
		 * carrier services the zip by checking in which carrier range of zips the
		 * master zip falls in once carrier found - go to list of zipTerminals of that
		 * carrier. Find terminal/servOut/daysOut/servIn/daysIn for that zipcode
		 * populate PreOutput object with these values add it to a list of PreOutput
		 * objects end for each print it out to a csv file
		 */
		// create ranges out of carrierrange data
		List<PreOutput> listPreOutput = new ArrayList<>();

		// process and populate preoutput object
		processPreOutput(listPreOutput);

		// print out the preoutput file
		printPreOutput(listPreOutput);
		
		/*access preoutput list
		 * take list of preoutput - make nodes for current and next
		 * 
		 * create final output as list of objects of CarrierZipTerminal
		 * if fields like terminal serviceO/serviceI and daysO/daysI
		 * match for contiguous blocks write them to object of CarrierZipTerminal
		 */
		List<CarrierZipTerminal> finalOuputRange = new ArrayList<>();
		processPopulateFinalOutput(listPreOutput, finalOuputRange);
		
		//print out final
		printFinalOutput(finalOuputRange);
	}

	private static void printFinalOutput(List<CarrierZipTerminal> finalOuputRange) throws IOException {
		// TODO Auto-generated method stub
		FileWriter fileWriter = null;
		CSVFormat csvFileFormat = null;
		CSVPrinter csvFilePrinter = null;
		try {
			fileWriter = new FileWriter(finalOutputCSV);
			String[] postOutput = FILE_HEADER_POST.split(COMMA_DELIMITER); 
			csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR).withHeader(postOutput);

			// initialize CSVPrinter object
			
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

			// Create CSV file header
			 //csvFilePrinter.printRecord(FILE_HEADER_POST.split(COMMA_DELIMITER));
			for (CarrierZipTerminal op : finalOuputRange) {
				
				csvFilePrinter.printRecord(op.getLowZip(), op.getHighZip(), op.getTerminal(), op.getServDorIn()
						, op.getServDaysIn(), op.getServDaysOut(), op.getServDorOut());
			}
		} catch (Exception e) {
			System.out.println("something went wrong" + e.getMessage());
			e.printStackTrace();
		} finally {
			fileWriter.flush();
			fileWriter.close();
			csvFilePrinter.close();
		}
	}

	/**
	 * @param listPreOutput
	 * @param finalOuputRange
	 */
	private static void processPopulateFinalOutput(List<PreOutput> listPreOutput,
			List<CarrierZipTerminal> finalOuputRange) {
		CarrierZipTerminal newOPRecord;
		PreOutput current;
		PreOutput next;
		int min = listPreOutput.get(0).getZip();
		int max = listPreOutput.get(0).getZip();
		System.out.println("size of zipzones " + listPreOutput.size());
		for(int i=0; i< listPreOutput.size()-1; i++) {
			current = listPreOutput.get(i);
			next = listPreOutput.get(i+1);
			
			if(current.getTerminal().equalsIgnoreCase(next.getTerminal()) &&
					current.getDaysOutbound() == next.getDaysOutbound() &&
					current.getDaysInbound() == next.getDaysInbound() &&
					current.getServiceOutbound().equalsIgnoreCase(next.getServiceOutbound()) &&
					current.getServiceInbound().equalsIgnoreCase(next.getServiceInbound())) {
				
				//set max zip code of next node
				max = next.getZip();
				if(i == listPreOutput.size() - 2) {
					newOPRecord = new CarrierZipTerminal();
					newOPRecord.setLowZip(leadZero(min));
					newOPRecord.setHighZip(leadZero(max));
					newOPRecord.setServDaysIn(current.getDaysInbound());
					newOPRecord.setServDaysOut(current.getDaysOutbound());
					newOPRecord.setServDorIn(current.getServiceInbound());
					newOPRecord.setServDorOut(current.getServiceOutbound());
					finalOuputRange.add(newOPRecord);
				}
				
			}else {
				
				newOPRecord  =new CarrierZipTerminal();
				if(min == max) {
					System.out.println("Min = Max current" + current.getZip() + " next " + next.getZip());
					newOPRecord.setLowZip(leadZero(current.getZip()));
					newOPRecord.setHighZip(leadZero(current.getZip()));
				}else {
					newOPRecord.setLowZip(leadZero(min));
					System.out.println("Min != Max - current  " + current.getZip() + " next " + next.getZip());
					newOPRecord.setHighZip(leadZero(max));
					
				}
				newOPRecord.setTerminal(current.getTerminal());
				newOPRecord.setServDaysIn(current.getDaysInbound());
				newOPRecord.setServDaysOut(current.getDaysOutbound());
				newOPRecord.setServDorIn(current.getServiceInbound());
				newOPRecord.setServDorOut(current.getServiceOutbound());
				finalOuputRange.add(newOPRecord);
				max = next.getZip();
				min = next.getZip();
			}
			
		}
		PreOutput last = listPreOutput.get(listPreOutput.size() - 1);
		if(last.getZip() != max) {
			System.out.println("last node");
			newOPRecord = new CarrierZipTerminal();
			newOPRecord.setLowZip(leadZero(last.getZip()));
			newOPRecord.setHighZip(leadZero(last.getZip()));
			newOPRecord.setTerminal(last.getTerminal());
			newOPRecord.setServDaysIn(last.getDaysInbound());
			newOPRecord.setServDaysOut(last.getDaysOutbound());
			newOPRecord.setServDorIn(last.getServiceInbound());
			newOPRecord.setServDorOut(last.getServiceOutbound());
			finalOuputRange.add(newOPRecord);
		}
	}

	/**
	 * @param listPreOutput
	 * @throws IOException
	 */
	private static void printPreOutput(List<PreOutput> listPreOutput) throws IOException {
		FileWriter fileWriter = null;
		CSVFormat csvFileFormat = null;
		CSVPrinter csvFilePrinter = null;
		try {
			fileWriter = new FileWriter(preOutputCSV);
			String[]  preHeader = FILE_HEADER_PRE.split(COMMA_DELIMITER);
			csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR).withHeader(preHeader);

			// initialize CSVPrinter object
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

			// Create CSV file header
			//csvFilePrinter.printRecord(FILE_HEADER_PRE);
			for (PreOutput op : listPreOutput) {
				
				csvFilePrinter.printRecord(op.getZip(), op.getTerminal(), op.getServiceOutbound(), op.getDaysOutbound()
						, op.getServiceInbound(), op.getDaysInbound());
			}
		} catch (Exception e) {
			System.out.println("something went wrong" + e.getMessage());
			e.printStackTrace();
		} finally {
			fileWriter.flush();
			fileWriter.close();
			csvFilePrinter.close();

		}
	}

	/**
	 * @param listPreOutput
	 */
	private static void processPreOutput(List<PreOutput> listPreOutput) {
		for (MasterZip zip : masterZips) {
			try {
				// find range and carrier for zip
				String carrier = checkCarrierRangeForZip(zip);

				// if carrier is SEKW, process SEKW zip-Terminal List
				if (carrier.equalsIgnoreCase("SEKW")) {
					populatePreOutputList(listPreOutput, zip, sekwZips);
				} else if (carrier.equalsIgnoreCase("PDIB")) {
					populatePreOutputList(listPreOutput, zip, pdibZips);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void populatePreOutputList(List<PreOutput> listPreOutput, MasterZip zip,
			List<CarrierZipTerminal> zips) {
		CarrierZipTerminal terminalFound = null;
		Optional<CarrierZipTerminal> matchingZipTerminal = zips.stream().filter(p -> (p.getLowZip() <= zip.getZipCode() && p.getHighZip() >= zip.getZipCode()))
				.findFirst();
		if(matchingZipTerminal.isPresent()) {
			terminalFound = matchingZipTerminal.orElse(null);
		}
		if (terminalFound != null) {
			PreOutput op = new PreOutput(zip.getZipCode(), terminalFound.getTerminal(), terminalFound.getServDorOut(),
					terminalFound.getServDaysOut(), terminalFound.getServDorIn(), terminalFound.getServDaysIn());
			listPreOutput.add(op);
		}else {
			
		}
	}

	private static String checkCarrierRangeForZip(MasterZip zip) throws Exception {
		String carrier = "";
		for (CarrierRange range : carrierrangeZips) {
			Range<Integer> zipRange = Range.between(range.getLowZip(), range.getHighZip());
			if (zipRange.contains(zip.getZipCode())) {
				carrier = range.getCarrier();
				break;
			}
		}
		if (!StringUtils.isAnyEmpty(carrier)) {
			return carrier;
		} else {
			throw new Exception("No Carrier Range Found");
		}

	}

	private static void readCSV() throws Exception {
		Reader in;
		Iterable<CSVRecord> records;
		// parsing CSV for carrier range
		in = new FileReader(carrierRangeCSV);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		carrierrangeZips = new ArrayList<>();
		for (CSVRecord record : records) {
			// accessing values by names assigned to each column
			CarrierRange range = new CarrierRange(Integer.parseInt(record.get(0)), Integer.parseInt(record.get(1)),
					record.get(2));
			carrierrangeZips.add(range);
		}
		carrierrangeZips.sort(Comparator.comparing(CarrierRange::getLowZip));
		try {
			checkifZipRangesContiguous(carrierrangeZips);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
		
		// parsing CSV for master zip
		in = new FileReader(masterZipsCSV);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		masterZips = new ArrayList<>();
		for (CSVRecord record : records) {
			// accessing values by names assigned to each column
			MasterZip range = new MasterZip(Integer.parseInt(record.get(0)));
			masterZips.add(range);
		}
		masterZips.sort(Comparator.comparing(MasterZip::getZipCode));

		// parsing CSV for carrier zip terminal SEKW
		in = new FileReader(SEKWZipsCSV);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		sekwZips = new ArrayList<>();
		for (CSVRecord record : records) {
			// accessing values by names assigned to each column
			if (StringUtils.isNumeric(record.get(0)) && StringUtils.isNumeric(record.get(1))) {
				CarrierZipTerminal range = new CarrierZipTerminal(Integer.parseInt(record.get(0)),
						Integer.parseInt(record.get(1)), record.get(2), record.get(3), Integer.parseInt(record.get(4)),
						Integer.parseInt(record.get(5)), record.get(6));
				sekwZips.add(range);
			}
		}
		sekwZips.sort(Comparator.comparing(CarrierZipTerminal::getLowZip));

		// parsing CSV for carrier zip terminal PDIB
		in = new FileReader(PDIBZipsCSV);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		pdibZips = new ArrayList<>();
		for (CSVRecord record : records) {
			// accessing values by names assigned to each column
			if (StringUtils.isNumeric(record.get(0)) && StringUtils.isNumeric(record.get(1))) {
				CarrierZipTerminal range = new CarrierZipTerminal(Integer.parseInt(record.get(0)),
						Integer.parseInt(record.get(1)), record.get(2), record.get(3), Integer.parseInt(record.get(4)),
						Integer.parseInt(record.get(5)), record.get(6));
				pdibZips.add(range);
			}
		}
		pdibZips.sort(Comparator.comparing(CarrierZipTerminal::getLowZip));
	}
	
	private static void checkifZipRangesContiguous(List<CarrierRange> carrierrangeZips2) throws Exception {
		// TODO Auto-generated method stub
		CarrierRange current;
		CarrierRange next;
		
		for(int i=0; i < carrierrangeZips2.size() -1; i++) {
			 current = carrierrangeZips2.get(i);
			 next = carrierrangeZips2.get(i+1);
			if(next.getLowZip() - current.getHighZip() != 1) {
				System.out.println("next LowZip " + next.getLowZip() + "current HighZip " + current.getHighZip());
				throw new Exception("Zip Ranges not contiguous");
			}
		}
	}

	private static int leadZero(int zip){
		String leadZip;
		if(Integer.toString(zip).length() < 5){
		if(Integer.toString(zip).length() == 4){
			leadZip = "0" + zip;
		}else{
			
			leadZip = "00" + zip;
		}
		}else{
			leadZip= Integer.toString(zip);
		}
		return Integer.parseInt(leadZip);
	}

}
