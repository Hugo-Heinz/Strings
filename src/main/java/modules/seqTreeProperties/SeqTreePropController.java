package modules.seqTreeProperties;

import java.util.Properties;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.PipedReader;

import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;
import modules.seqSuffixTrie2SuffixTree.SeqReducedTrieNode;

import com.google.gson.Gson;

import common.parallelization.CallbackReceiver;

//TODO: instead of different node classes I need to create different node classes which implement the interface "common.TreeNode"

/**
 * Reads trees from I/O pipe via JSON format.
 * JSON format must represent reduced suffix tries or trees.
 * Extracts tree properties such as 
 * 	- height of the tree (h = length of the longest path)
 * 	- average length of all paths
 * 	- ratio of subtree sites 
 * 	- average of subtree sites
 * 	- Sackin index and other tree balance measures for non-binary trees
 * 
 * Writes output in form of a string into the char output pipe.
 * 
 * @author Christopher Kraus
 * 
 */

public class SeqTreePropController extends ModuleImpl {
		
	//property keys:
		/*no property keys*/
	//end keys

	//variables:
	
	//HashMap to save the properties of each inner node of the tree 
	private HashMap<String, SeqProperties> seqProperties;
	
	//sequence properties output string
	private String seqPropertiesOutput;
	
	//sorted ArrayList of properties for each node by path length
	private ArrayList<SeqProperties> seqPropertiesSorted;
	
	private ArrayList<Integer> averagePathLength;
	private double avPathLen;
	
	// total number of leaves of the tree
	private int totalNumOfLeaves;
	
	//longestPath = height of the tree
	private int longestPath;
	
	//varaibles for calculating the cophenetic index
	private HashMap<String,SeqCopheneticIndex> copheneticIndex;
	private ArrayList<Integer> copheneticIndexBinomList;
	private double avCopheneticIndex;
	// cophenetic index
	private int copheneticIndexVal;
	// cophenetic indexes for subtrees
	HashMap<String, Integer> subCophTrees;
	
	//variables for calculating the Sackin index
	private HashMap<String, SeqSackinIndex> sackinIndex;
	private ArrayList<Integer> sackinIndexLeavesList;
	private double avSackinIndex; 
	//sackin index
	private double sackinIndexVal;
	//sackin indexes for subtrees
	HashMap<String, Integer> subSackinTrees;
	
	//sackin variance
	//private double sackinVar;
	
	//normalized Sackin index
	//private double sackinIndexNorm;
	
	private SeqReducedTrieNode mainNode;
	private Gson gson;
	private SeqPropertyNode rootNode;
	private final String INPUTID = "input";
	private final String OUTPUTID = "output";
	//private HashMap<String, SeqPropertyNode> seqNodes;
	//end variables
	
	//constructors:
	public SeqTreePropController(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {
		super(callbackReceiver, properties);

		// Add property descriptions
			/*no property keys */
		
		// Add property defaults
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Sequence Tree Properties");
		
		
		// set up newickOutput
		this.seqPropertiesOutput = new String();
		
		// Define I/O
		InputPort inputPort = new InputPort(INPUTID, "TODO.", this);
		inputPort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(OUTPUTID, "TODO.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		super.addInputPort(inputPort);
		super.addOutputPort(outputPort);
		
	}
	//end constructors
	
	//setters:
		
	public void setGson(PipedReader reader) {
		gson = new Gson();
		mainNode = gson.fromJson(reader, SeqReducedTrieNode.class);
	}
			
	//end setters
	
	//getters:
			
	public String getSeqProperties() {
		return seqPropertiesOutput;
	}
	
	//end getters
	
	@Override
	public boolean process() throws Exception {
		
		//create mainNode by reading JSON input
		this.setGson(this.getInputPorts().get(INPUTID).getInputReader());
					
		//iterate over the tree and get parameters
		this.displayAllTreeProperties();
		
		//write the tree properties into the output
		this.getOutputPorts().get(OUTPUTID).outputToAllCharPipes(this.getSeqProperties());
								
		// Close outputs (important!)
		this.closeAllOutputs();
		
		// Done
		return true;
	}
	
	@Override
	public void applyProperties() throws Exception {
		super.setDefaultsIfMissing();
		
		// Apply own properties
			/*no property keys*/
		// Apply parent object's properties
		super.applyProperties();
	}
		
	//display all tree properties
	public void displayAllTreeProperties () {
		
		//create rootNode tree and initialize seqProperties
		iteratePropMainNode();
		
		//create indexes
		//iterateIndexRootNode();
		
		//create index HashMaps
		createIndexHashMaps();
		
		sortByPathLength();
		calculateProps();
		seqPropertiesOutput = "Longest Path for inner nodes:\t" + longestPath + "\n";
		seqPropertiesOutput = seqPropertiesOutput + "Longest Path (for leaves):\t" + (longestPath + 1) + "\n";
		seqPropertiesOutput = seqPropertiesOutput + "Average length of paths:\t" + avPathLen + "\n";
		
		seqPropertiesOutput = seqPropertiesOutput + "Average Sackin index of paths:\t" + avSackinIndex + "\n";
		seqPropertiesOutput = seqPropertiesOutput + "Average cophenetic index of paths:\t" + this.avCopheneticIndex + "\n";
		seqPropertiesOutput = seqPropertiesOutput + "Total number of leaves:\t" + totalNumOfLeaves + "\n";
		seqPropertiesOutput = seqPropertiesOutput + "Sackin index:\t" + this.sackinIndexVal + "\n";
		seqPropertiesOutput = seqPropertiesOutput + "Cophenetic index:\t" + this.copheneticIndexVal + "\n";
		
		seqPropertiesOutput = seqPropertiesOutput + "Sequence\tpath length\tSackin index\tcophenetic index\n";
		for (SeqProperties i : seqPropertiesSorted) {
			if (i.getNodeName().equals("^")) {
				seqPropertiesOutput = seqPropertiesOutput + i.getNodeName() + "\t" + i.getPathLength() + "\t" + this.sackinIndexVal + "\t" + this.copheneticIndexVal + "\n";
			} else {
				seqPropertiesOutput = seqPropertiesOutput + i.getNodeName() + "\t" + i.getPathLength() + "\t" + this.subSackinTrees.get(i.getNodeName()) + "\t" + this.subCophTrees.get(i.getNodeName()) + "\n";
			}
		}
	}
	
	//identification of all properties of the tree
	public void iteratePropMainNode() { 

		// instantiate new root node
		rootNode = new SeqPropertyNode("^", mainNode.getCounter(),0);  
		
		// set total number of leaves for this tree
		this.totalNumOfLeaves = rootNode.getCounter();
		
		//name of the current leaf of the root node
		String innerNodeName = rootNode.getValue();
		
		//define the properties of the root node
		seqProperties = new HashMap<String, SeqProperties>();
		seqProperties.put(innerNodeName, new SeqProperties(innerNodeName, rootNode.getValue(), 0, rootNode.getCounter()));
		
		Iterator<Entry<String, SeqReducedTrieNode>> it = mainNode.getNodeHash().entrySet().iterator();
		
		while (it.hasNext()) {
			HashMap.Entry<String, SeqReducedTrieNode> pair = (HashMap.Entry<String, SeqReducedTrieNode>)it.next();
			
			if(pair.getValue().getNodeHash().isEmpty()) {
				
				//end node on first level reached. Create terminal node.
				SeqPropertyNode node = new SeqPropertyNode(pair.getKey(), pair.getValue().getCounter(),1);
				rootNode.addNode(pair.getKey(), node);
				
			} else {
				
				if (pair.getValue().getNodeHash().size() == 1) {
						SeqPropertyNode node = new SeqPropertyNode(pair.getKey(), pair.getValue().getCounter(), 1);
						
						SeqPropertyNode childNode = deepPropIteration(pair.getValue(), node, innerNodeName, node.getNodeDepth());
											
						//get node directly beneath child node
						rootNode.addNode(childNode.getValue(), childNode); 
						
				} else if(pair.getValue().getNodeHash().size() > 1) {
					
					//increase the "name" for each iteration
					String pairNodeName = innerNodeName + pair.getValue().getValue();
					
					Iterator<Entry<String, SeqReducedTrieNode>> subIt = pair.getValue().getNodeHash().entrySet().iterator();
					
					SeqPropertyNode node = new SeqPropertyNode(pair.getKey(), pair.getValue().getCounter(), 1);
					
					//create properties of inner node
					
					seqProperties.put(pairNodeName, new SeqProperties(pairNodeName, node.getValue(), seqProperties.get("^").getPathLength() + 1, node.getCounter()));
					
					while (subIt.hasNext()) {
						
						HashMap.Entry<String, SeqReducedTrieNode> subPair = (HashMap.Entry<String, SeqReducedTrieNode>)subIt.next();
						SeqPropertyNode subNode = new SeqPropertyNode(subPair.getKey(), subPair.getValue().getCounter(), 2);
						
						//increase the "name" for each iteration
						String subPairNodeName = pairNodeName;
						//create properties for an inner node
						if (subPair.getValue().getNodeHash().size() > 1 ) {
							subPairNodeName += subPair.getValue().getValue();
							//seqProperties.put(subPairNodeName, new SeqProperties(subPairNodeName, subNode.getValue(), seqProperties.get(pairNodeName).getPathLength() + 1, (((double)subNode.getCounter())/((double)node.getCounter()))));
							seqProperties.put(subPairNodeName, new SeqProperties(subPairNodeName, subNode.getValue(), seqProperties.get(pairNodeName).getPathLength() + 1, subNode.getCounter()));
						}
						
						SeqPropertyNode childNode = deepPropIteration(subPair.getValue(), subNode, pairNodeName, subNode.getNodeDepth());
										
						node.addNode(childNode.getValue(), childNode);
						subIt.remove(); // avoids a ConcurrentModificationException
					}
					
					rootNode.addNode(node.getValue(), node);
					
				}
			}
			
		    it.remove(); // avoids a ConcurrentModificationException 
		}
	}
	
	private SeqPropertyNode deepPropIteration(SeqReducedTrieNode Node, SeqPropertyNode propNode, String propNodeName, int depth) {
		
		SeqReducedTrieNode currentNode = Node;
		int nodeDepth = depth;
		SeqPropertyNode currPropNode = new SeqPropertyNode(currentNode.getValue(), currentNode.getCounter(), nodeDepth);
		String lastPropNodeName = propNodeName;
		
		// reaching a terminal node adds the sequence to the previous node
		if (currentNode.getNodeHash().isEmpty()) {
			
			return currPropNode;
		
		} else {
			Iterator<Entry<String, SeqReducedTrieNode>> deepIt = currentNode.getNodeHash().entrySet().iterator();
			while (deepIt.hasNext()) {
				HashMap.Entry<String, SeqReducedTrieNode> deepPair = (HashMap.Entry<String, SeqReducedTrieNode>)deepIt.next();
				
				if(deepPair.getValue().getNodeHash().size() == 0) {
					SeqPropertyNode newPropNode = new SeqPropertyNode(deepPair.getKey(),deepPair.getValue().getCounter(), nodeDepth + 1);				
					
					currPropNode.addNode(newPropNode.getValue(), newPropNode);
					
				} else if(deepPair.getValue().getNodeHash().size() == 1) { //if child has one grand child
					
					Object[] key = deepPair.getValue().getNodeHash().keySet().toArray(); //What is the name of the grand child?
					
					//Let's continue with the grand child
					SeqPropertyNode newPropNode = deepPropIteration(deepPair.getValue().getNodeHash().get(key[0]),currPropNode, lastPropNodeName, nodeDepth + 1); 
										
					if(newPropNode.getNodeHash().size() == 1) { //What the grand child also has only one child?
						Object[] newChild = newPropNode.getNodeHash().keySet().toArray();//What is the name of this new child?
						currPropNode.concatValue(newPropNode.getValue()); //remember the name of the grand child
						currPropNode.addNode(newPropNode.getNodeHash().get(newChild[0]).getValue(), newPropNode.getNodeHash().get(newChild[0]));//continue with this new child
					} else { // so the grand child has several or no children, let's remember that
						currPropNode.addNode(newPropNode.getValue(), newPropNode); 
					}
				} else if (deepPair.getValue().getNodeHash().size() > 1) { // if there are more nodes remember the zaehler value
					
					String currPropNodeName = lastPropNodeName + currPropNode.getValue();
					SeqPropertyNode newNode = deepPropIteration(deepPair.getValue(),currPropNode, currPropNodeName, nodeDepth + 1);
					
					//increase the "name" for each iteration
					String innerNodeName = currPropNodeName + newNode.getValue();
					
					//create properties of inner node
					//seqProperties.put(innerNodeName, new SeqProperties(innerNodeName, newNode.getValue(), nodeDepth + 1, (((double)newNode.getCounter())/((double)currPropNode.getCounter()))));
					seqProperties.put(innerNodeName, new SeqProperties(innerNodeName, newNode.getValue(), nodeDepth + 1, newNode.getCounter()));					
					currPropNode.addNode(newNode.getValue(), newNode);
					
				}
			}
			deepIt.remove(); // avoids a ConcurrentModificationException
			return currPropNode;
		}
	}
	
	// create the Sackin and the cophenetic indexes
	public void createIndexHashMaps() {
		Iterator<Entry<String, SeqProperties>> iter = seqProperties.entrySet().iterator();
		
		sackinIndex = new HashMap<String, SeqSackinIndex> ();
		
		copheneticIndex = new HashMap<String,SeqCopheneticIndex>();
		
		while(iter.hasNext()) {
			HashMap.Entry<String, SeqProperties> pair = (HashMap.Entry<String, SeqProperties>)iter.next();
			
			/* get the number of descending leaves which equals the number of inner nodes beneath a specific node
			and save this data as a node for the Sackin index */
			
			SeqSackinIndex sIndex = new SeqSackinIndex (pair.getValue().getSequence(),pair.getValue().getLeafNum());
			sackinIndex.put(pair.getKey(),sIndex);
			
			/*get the number of descending leaves and save the data as a node for the cophenetic index*/
			SeqCopheneticIndex cIndex = new SeqCopheneticIndex (pair.getValue().getSequence(),pair.getValue().getLeafNum());
			copheneticIndex.put(pair.getKey(), cIndex);
		}
	}
	
	
	//sort the different paths through the tree by their length
	public void sortByPathLength() {
		
		//initiate the sorted ArrayList
		this.seqPropertiesSorted = new ArrayList<SeqProperties>();
			
		Iterator<Entry<String, SeqProperties>> iter = this.seqProperties.entrySet().iterator();
		
		//remember the length of the last path
		int lastLength = this.seqProperties.get("^").getPathLength();
		
		//instantiate variables to remember information about path length and indexes
		this.averagePathLength = new ArrayList<Integer>();
		this.sackinIndexLeavesList = new ArrayList<Integer>();
		this.copheneticIndexBinomList = new ArrayList<Integer>();
		
		while(iter.hasNext()) {
			
			HashMap.Entry<String, SeqProperties> pair = (HashMap.Entry<String, SeqProperties>)iter.next();
			
			//get information to calculate the average path length and the average path ratio
			this.averagePathLength.add(pair.getValue().getPathLength());
			
			this.sackinIndexLeavesList.add(this.sackinIndex.get(pair.getKey()).getNodeNumber());
			
			if (!(pair.getKey() == "^")) { // do not include the root; 
				this.copheneticIndexBinomList.add(this.copheneticIndex.get(pair.getKey()).getBinomialCoeff());
			}
			
			
			
			if (pair.getValue().getPathLength() >= lastLength) {
				this.seqPropertiesSorted.add(0, pair.getValue()); //put it to the top
				lastLength = pair.getValue().getPathLength();
			} else {
				//iterate over the ArrayList seqPropertiesSorted and add this sequence
				for(SeqProperties i : this.seqPropertiesSorted) {

					if (i.getPathLength() <= pair.getValue().getPathLength()) {
						this.seqPropertiesSorted.add(this.seqPropertiesSorted.indexOf(i), pair.getValue());
						break;
					}
					
					// if this is the smallest value add it to the end of the list
					if (this.seqPropertiesSorted.indexOf(i) == this.seqPropertiesSorted.size() -1 ) { 
						this.seqPropertiesSorted.add(pair.getValue());
						break;
					}
				}
			}
		}
		this.longestPath = lastLength;
	}
	
	//calculate properties of the tree
	public void calculateProps() {

		int totalPathLen = 0;
		
		for (Integer i : this.averagePathLength) {
			totalPathLen = totalPathLen + i;  
		}
		
		this.avPathLen = (double) totalPathLen / (double) this.averagePathLength.size();
		
		this.sackinIndexVal = 0;
		
		// calculate the Sackin index
		for (int i : this.sackinIndexLeavesList) {
			this.sackinIndexVal = this.sackinIndexVal + i; 
		}
		
		// calculate the cophenetic index
		for (int i : this.copheneticIndexBinomList) {
			this.copheneticIndexVal = this.copheneticIndexVal + i;
		}
		
		//calculate Sackin and cophenetic indexes for subtrees
		this.subCophTrees = new HashMap<String, Integer> ();
		this.subSackinTrees = new HashMap<String, Integer> ();
		
		ArrayList <SeqProperties> seqPropertiesSortedInverted = new ArrayList <SeqProperties> ();
		
		for (SeqProperties i : this.seqPropertiesSorted) {
			if (!(i.getNodeName() == "^")) {
				seqPropertiesSortedInverted.add(0, i);
			}
		}
		
		String lastStr = "";
		int lastCophVal = 0;
		int lastSackinVal = 0;
		boolean termNode = true;
		for (SeqProperties j : seqPropertiesSortedInverted) {
			lastStr = j.getNodeName();
			
			for (SeqProperties i : seqPropertiesSortedInverted) {
				/*
				 * Increase cophenetic index only if the lastStr is a substring of the current sequence.
				 * This avoids calculating the n choose k value for the root of the tree
				 * */ 
				if (i.getNodeName().length() > lastStr.length() && !(i.getNodeName().equals(lastStr)) && i.getNodeName().substring(0, lastStr.length()).equals(lastStr)) {
					lastCophVal += this.copheneticIndex.get(i.getNodeName()).getBinomialCoeff();
					termNode = false;
				}
				
				//increase Sackin index only if the lastStr is a substring of the current sequence OR if it is equal
				if (i.getNodeName().length() >= lastStr.length() && (i.getNodeName().equals(lastStr) || i.getNodeName().substring(0, lastStr.length()).equals(lastStr))) {
					lastSackinVal += this.sackinIndex.get(i.getNodeName()).getNodeNumber(); 
				}
			}
			
			if (termNode) {
				lastCophVal = 1;
			}
			this.subCophTrees.put(lastStr, lastCophVal);
			this.subSackinTrees.put(lastStr,lastSackinVal);
			lastCophVal = 0;
			lastSackinVal = 0;
			termNode = true;
		}
				
		this.avSackinIndex = (double) this.sackinIndexVal / (double) this.averagePathLength.size();
		this.avCopheneticIndex = (double) this.copheneticIndexVal / (double) this.averagePathLength.size();
	}
}
