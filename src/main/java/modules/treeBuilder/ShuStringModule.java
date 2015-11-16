package modules.treeBuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.gson.Gson;
import common.parallelization.CallbackReceiver;

public class ShuStringModule extends ModuleImpl {
	
	// Define property keys (every setting has to have a unique key to associate it with)
	public static final String PROPERTYKEY_DELIMITER_INPUT_TOKEN = "input token delimiter";
	public static final String PROPERTYKEY_DELIMITER_OUTPUT = "output delimiter";
	
	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_INPUT_TREE = "json tree";
	private static final String ID_INPUT_TEXT = "text";
	private static final String ID_OUTPUT = "shus-nrs";
	
	// Local variables
	private String inputTokenDelimiter;
	private String outputdelimiter;

	public ShuStringModule(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {
		
		// Call parent constructor
		super(callbackReceiver, properties);
		
		// Add module description
		this.setDescription("Calculates shortest unique substing.");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_DELIMITER_INPUT_TOKEN, "Delimiter which segments the input tokens, default is whitespace-class");
		this.getPropertyDescriptions().put(PROPERTYKEY_DELIMITER_OUTPUT, "String to insert as segmentation delimiter into the output, default is newline");
		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "SHUS Module"); // Property key for module name is defined in parent class
		this.getPropertyDefaultValues().put(PROPERTYKEY_DELIMITER_INPUT_TOKEN, "\\s");
		this.getPropertyDefaultValues().put(PROPERTYKEY_DELIMITER_OUTPUT, "\\n");
		
		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~).
		 * Every port can support a range of pipe types (currently
		 * byte or character pipes). Output ports can provide data
		 * to multiple pipe instances at once, input ports can
		 * in contrast only obtain data from one pipe instance.
		 */
		InputPort treeInputPort = new InputPort(ID_INPUT_TREE, "JSON-encoded suffix tree (based on "+Knoten.class.getCanonicalName()+").", this);
		treeInputPort.addSupportedPipe(CharPipe.class);
		InputPort textInputPort = new InputPort(ID_INPUT_TEXT, "Plain text character input, segmented via the specified delimiters.", this);
		textInputPort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(ID_OUTPUT, "Plain text character output using the specified delimiter.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addInputPort(treeInputPort);
		super.addInputPort(textInputPort);
		super.addOutputPort(outputPort);
		
	}

	@Override
	public boolean process() throws Exception {
		
		// Instantiate JSON parser
		Gson gson = new Gson();
		
		// Read suffix tree
		Knoten rootNode = gson.fromJson(this.getInputPorts().get(ID_INPUT_TREE).getInputReader(), Knoten.class);
		
		// Construct scanner instance for text input segmentation
		Scanner textInputScanner = new Scanner(this.getInputPorts().get(ID_INPUT_TEXT).getInputReader());
		textInputScanner.useDelimiter(this.inputTokenDelimiter);
		
		// Queue for read segments
		Deque<String> segments = new ArrayDeque<String>();
		
		// Variable for current SHU value
		int shuValue = -1;
		
		// Current node within the tree
		Knoten currentNode = rootNode;
		
		// Input read loop
		while (textInputScanner.hasNext()){
			
			// Check for interrupt signal
			if (Thread.interrupted()) {
				textInputScanner.close();
				this.closeAllOutputs();
				throw new InterruptedException("Thread has been interrupted.");
			}
			
			// Determine next segment
			String textInputSegment = textInputScanner.next();
			
			// Put segment into queue
			segments.addLast(textInputSegment);
			
			shuValue++;
			
			// Node is not a leaf
			if (currentNode.getKinder().size() > 1){
				
				// Check whether the leaf we need to follow is present
				currentNode = currentNode.getKinder().get(textInputSegment);
				if (currentNode == null){
					
					// Issue warning
					Logger.getLogger(Thread.currentThread().getName()).log(Level.WARNING, "The tree provided does not seem to match the text input.");
					
					// Output invalid SHU values for all segments in queue
					for (int i=0; i<segments.size(); i++)
						this.getOutputPorts().get(ID_OUTPUT).outputToAllCharPipes("-1".concat(outputdelimiter));

					// Remove segments from queue
					segments.clear();
					
					// Reset SHU value
					shuValue = -1;
					
					// Reset current node in tree
					currentNode = rootNode;
					
				}
			} else {
				// Node is a leaf
				
				// Remove first segment from queue
				segments.removeFirst();
				
				// Output SHUS value
				this.getOutputPorts().get(ID_OUTPUT).outputToAllCharPipes(shuValue+"");
				
				// Decrease SHUS value
				shuValue--;
				
				// Reset current node in tree
				currentNode = rootNode;
				
				// Climb down within the tree on the branch that describes the remaining queue
				Iterator<String> segmentIterator = segments.iterator();
				while (segmentIterator.hasNext()){
					currentNode = currentNode.getKinder().get(segmentIterator.next());
					
					// Check if null (shouldn't be, lest the tree is broken)
					if (currentNode == null) {
						textInputScanner.close();
						this.closeAllOutputs();
						throw new Exception("The tree seems to be broken -- some substrings are not present, yet they should be.");
					}
				}
			}
		}
		
		// TODO Process remaining queue contents

		/*
		 * Close input scanners. NOTE: A module should not attempt to close its
		 * inputs before the module providing it has done so itself! Please
		 * either leave open any readers that would close the underlying
		 * this.getInputPorts().get().getInputReader() (or getInputStream()
		 * respectively) or only do so after you can be sure that the providing
		 * module has already closed them (like in this instance).
		 */
		textInputScanner.close();
		
		// Close outputs (important!)
		this.closeAllOutputs();
		
		
		
		// Done
		return true;
	}
	
	@Override
	public void applyProperties() throws Exception {
		
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();
		
		// Apply own properties
		this.inputTokenDelimiter = StringEscapeUtils.unescapeJava(this.getProperties().getProperty(PROPERTYKEY_DELIMITER_INPUT_TOKEN, this.getPropertyDefaultValues().get(PROPERTYKEY_DELIMITER_INPUT_TOKEN)));
		this.outputdelimiter = this.getProperties().getProperty(PROPERTYKEY_DELIMITER_OUTPUT, this.getPropertyDefaultValues().get(PROPERTYKEY_DELIMITER_OUTPUT));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}
