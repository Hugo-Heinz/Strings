package modules.seqNewickExporter;

import java.util.Map;
import java.util.HashMap;

/**
 * Helper class for Newick Exporter module(s)
 * 
 * @author Christopher Kraus
 *
 */
public class SeqNewickNodeV2 {
	//variables:
	private String nodeValue; //string saved in the node
	private int nodeCounter; //Zaehler value
	HashMap<String, SeqNewickNodeV2> childNodes;
	
	//end variables
	
	//constructors:
	
	public SeqNewickNodeV2(String value, int counter) {
		nodeValue = value;
		nodeCounter = counter;
		childNodes = new HashMap<String, SeqNewickNodeV2>();
	
	}
	
	public SeqNewickNodeV2(String value, int counter, SeqNewickNodeV2 node) {
		nodeValue = value;
		nodeCounter = counter;
		childNodes = new HashMap<String, SeqNewickNodeV2>();
		childNodes.put(value, node);
		
	}
	//end constructors
	
	//methods:
	//setters:
	public void setValue (String val) {
		nodeValue = val;
	}
	
	public void concatValue (String val) {
		nodeValue += val;
	}
	
	public void setCount (int count) {
		nodeCounter = count;
	}
	
	public void addCount (int count) {
		nodeCounter += count;
	}
	
	public void addNode (String value, SeqNewickNodeV2 node) {
		childNodes.put(value, node);
	}
	
	public void setNodeValue(String nodeValue) {
		this.nodeValue = nodeValue;
		
	}
	
	public void setNodeCounter(int nodeCounter) {
		this.nodeCounter = nodeCounter;
	}
	
	public int incNodeCounter() {
		return ++this.nodeCounter;
	}
		
	//end setters
	
	//getters:
	public String getValue ()  {
		return nodeValue;
	}
	
	public int getCounter ()  {
		return nodeCounter;
	}
	
	public Map<String, SeqNewickNodeV2> getNodeHash () {
		return childNodes;
	}
	
	public String getNodeValue() {
		return this.nodeValue;
	}
	
	public int getNodeCounter() {
		return this.nodeCounter;
	}
	
	public Map<String, SeqNewickNodeV2> getChildNodes() {
		return this.childNodes;
	}
		
	//end getters
	//end methods
}
