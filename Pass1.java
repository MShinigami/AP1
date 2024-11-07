import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

class Tuple {
	String mnemonic, m_class, opcode;
	int length;
	Tuple() {}
	Tuple(String s1, String s2, String s3, String s4) {
		mnemonic = s1;
		m_class = s2;
		opcode = s3;
		length = Integer.parseInt(s4);
	}
}

class SymTuple {
	String symbol, address;
	int length;
	SymTuple(String s1, String s2, int i1) {
		symbol = s1;
		address = s2;
	 	length = i1;
	}
}

class LitTuple {
	String literal, address;
	int length;
	LitTuple() {}
	LitTuple(String s1, String s2, int i1) {
		literal = s1;
		address = s2;
		length = i1;
	}
}

public class Pass1 {
	static int lc,SymTabPtr=0, LitTabPtr=0, PoolTabPtr=0;
	static int poolTable[] = new int[10];
	static Map<String,Tuple> MOT;
	static Map<String,SymTuple> symtable;
	static ArrayList<LitTuple> littable;
	static Map<String, String> regAddressTable;
	static PrintWriter out_pass2;
	static PrintWriter out_pass1;
	static int line_no;
	
	public static void main(String[] args) throws Exception{
		initializeTables();
		pass1();
	}

	@SuppressWarnings("resource")
	static void pass1() throws Exception {
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream("input.txt")));
		out_pass1 = new PrintWriter(new FileWriter("output.txt"), true);
		PrintWriter out_symtable = new PrintWriter(new FileWriter("symtable.txt"), true);
		PrintWriter out_littable = new PrintWriter(new FileWriter("littable.txt"), true);
		PrintWriter out_pooltable = new PrintWriter(new FileWriter("pooltable.txt"), true);
		
		String s;
		lc=0;
		while((s = input.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(s, " ", false);
			String s_arr[] = new String[st.countTokens()];
			for(int i=0 ; i < s_arr.length ; i++) {
				s_arr[i] = st.nextToken();
			}

			if(s_arr.length == 0){
				continue;
			}
			
			int curIndex = 0;
			if(s_arr.length == 3){
				String label = s_arr[0];
				insertIntoSymTab(label,lc+"");
				curIndex = 1;	
			}
			String curToken = s_arr[curIndex];
			Tuple curTuple = MOT.get(curToken);
			String intermediateStr="";
			if(curTuple.m_class.equalsIgnoreCase("IS")){
				intermediateStr += lc + " (" + curTuple.m_class + "," + curTuple.opcode + ") ";
				lc += curTuple.length;
				intermediateStr += processOperands(s_arr[curIndex+1]);
			}
			else if(curTuple.m_class.equalsIgnoreCase("AD")){
				if(curTuple.mnemonic.equalsIgnoreCase("START")){
					intermediateStr += lc + " (" + curTuple.m_class + "," + curTuple.opcode + ") ";	
					lc = Integer.parseInt(s_arr[curIndex+1]);
					intermediateStr += "(C," + (s_arr[curIndex+1]) + ") ";
				}
				else if(curTuple.mnemonic.equalsIgnoreCase("LTORG")){
					intermediateStr +=processLTORG();
				}
				else if(curTuple.mnemonic.equalsIgnoreCase("END")){
					intermediateStr += lc + " (" + curTuple.m_class + "," + curTuple.opcode + ") \n";
					intermediateStr +=processLTORG();
				}
			}
			else if(curTuple.m_class.equalsIgnoreCase("DL")){
				intermediateStr += lc + " (" + curTuple.m_class + "," + curTuple.opcode + ") ";
				if(curTuple.mnemonic.equalsIgnoreCase("DS")){
					lc += Integer.parseInt(s_arr[curIndex+1]);
				}
				else if(curTuple.mnemonic.equalsIgnoreCase("DC")){
					lc += curTuple.length;
				}
				intermediateStr += "(C," + s_arr[curIndex+1] + ") ";
			}
			out_pass1.println(intermediateStr);
		}
		out_pass1.flush();
		out_pass1.close();

		SymTuple tuple;
		Iterator<SymTuple> it = symtable.values().iterator();
		String tableEntry;
		while(it.hasNext()){
			tuple = it.next();
			tableEntry = tuple.symbol + "\t" + tuple.address ;
			out_symtable.println(tableEntry);
		}
		out_symtable.flush();
		out_symtable.close();
		LitTuple litTuple;
		tableEntry = "";
		for(int i=0; i<littable.size(); i++){
			litTuple = littable.get(i);
			tableEntry = litTuple.literal + "\t" + litTuple.address ;
			out_littable.println(tableEntry);
		}
		for(int iLoop=0;iLoop<PoolTabPtr-1;iLoop++){
			out_pooltable.println(poolTable[iLoop]);
		}
		out_littable.flush();
		out_littable.close();
	}
	static String processLTORG(){
	LitTuple litTuple;
	String intermediateStr = "";
	for(int i=poolTable[PoolTabPtr-1]; i<littable.size(); i++){
		litTuple = littable.get(i);
		litTuple.address = lc+"";
		intermediateStr += lc + " (DL,02) (C," + litTuple.literal + ") \n";
		lc++;
	}
	poolTable[PoolTabPtr] = LitTabPtr;
	PoolTabPtr++;
	return intermediateStr;
	}

	 static String processOperands(String operands){
		StringTokenizer st = new StringTokenizer(operands, ",", false);
		String s_arr[] = new String[st.countTokens()];
		for(int i=0 ; i < s_arr.length ; i++) {
			s_arr[i] = st.nextToken();
		}
		String intermediateStr = "", curToken;
		for(int i=0; i <s_arr.length; i++){
			curToken = s_arr[i];
			if(curToken.startsWith("=")){
				StringTokenizer str = new StringTokenizer(curToken, "'", false);
				String tokens[] = new String[str.countTokens()];
				for(int j=0 ; j < tokens.length ; j++) {
					tokens[j] = str.nextToken();
				}	
				String literal = tokens[1];
				insertIntoLitTab(literal,"");
				intermediateStr += "(L," + (LitTabPtr -1) + ")";
			}
			else if(regAddressTable.containsKey(curToken)){
				intermediateStr += "(RG," + regAddressTable.get(curToken) + ") ";
			}
			else{
				insertIntoSymTab(curToken,"");
				intermediateStr += "(S," + (SymTabPtr -1) + ")";
			}
		}
		
		return intermediateStr;
	}

	 static void insertIntoSymTab(String symbol, String address){
		if(symtable.containsKey(symbol)== true){
			SymTuple s = symtable.get(symbol);
			s.address = address;
		}
		else{
			symtable.put(symbol, new SymTuple(symbol, address, 1));
		}
		SymTabPtr++;
	}

	static void insertIntoLitTab(String literal, String address){
		littable.add(LitTabPtr, new LitTuple(literal, address, 1));
		LitTabPtr++;
	}	
	
	static void initializeTables() throws Exception {
		symtable = new LinkedHashMap<>();
		littable = new ArrayList<>();
		regAddressTable = new HashMap<>();
		MOT = new HashMap<>();
		String s,mnemonic;
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(new FileInputStream("MOT.txt")));
		while((s = br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(s, " ", false);
			mnemonic = st.nextToken();
			MOT.put(mnemonic, (new Tuple(mnemonic, st.nextToken(), st.nextToken(), st.nextToken())));
		}
		
		br.close();
		regAddressTable.put("AREG", "1");
		regAddressTable.put("BREG", "2");
		regAddressTable.put("CREG", "3");
		regAddressTable.put("DREG", "4");
		poolTable[PoolTabPtr] = LitTabPtr;
		PoolTabPtr++;
	}
}