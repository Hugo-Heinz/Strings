package modules.basemodules;

import java.util.Properties;
import java.util.Scanner;

import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;

import common.parallelization.CallbackReceiver;

public class LongestSHUSPathModule extends ModuleImpl {
	
	// Define property keys (every setting has to have a unique key to associate it with)
	//public static final String PROPERTYKEY_INPUTDELIMITER = "delimiter A";
	
	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_INPUT = "SHUS list";
	private static final String ID_OUTPUT = "longest SHUSs";
	
	// Local variables
	private String inputdelimiter = "\n";
	private String outputdelimiter;

	public LongestSHUSPathModule(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {
		
		// Call parent constructor
		super(callbackReceiver, properties);
		
		// Add module description
		this.setDescription("<html>Determines a path of longest possible consecutive<br/>"
				+ "SHUS from a list of all SHUS over a string.<br/>"
				+ "Expects input like from http://guanine.evolbio.mpg.de/cgi-bin/shustring/shustring.cgi.pl<br/>"
				+ "<i>shustring -l '.*'</i></html>");

		// Add property descriptions (obligatory for every property!)
		//this.getPropertyDescriptions().put(PROPERTYKEY_INPUTDELIMITER, "Regular expression to use as segmentation delimiter for input A");
		
		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Determine longest SHUS path"); // Property key for module name is defined in parent class
		//this.getPropertyDefaultValues().put(PROPERTYKEY_INPUTDELIMITER, "[\\s]+");
		
		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~).
		 * Every port can support a range of pipe types (currently
		 * byte or character pipes). Output ports can provide data
		 * to multiple pipe instances at once, input ports can
		 * in contrast only obtain data from one pipe instance.
		 */
		InputPort inputPort = new InputPort(ID_INPUT, "Complete SHUS list.", this);
		inputPort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(ID_OUTPUT, "Longest SHUS list.", this);
		outputPort.addSupportedPipe(CharPipe.class);
		
		// Add I/O ports to instance (don't forget...)
		super.addInputPort(inputPort);
		super.addOutputPort(outputPort);
		
	}

	@Override
	public boolean process() throws Exception {
		
		// Construct scanner instances for input segmentation
		Scanner inputAScanner = new Scanner(this.getInputPorts().get(ID_INPUT).getInputReader());
		inputAScanner.useDelimiter(this.inputdelimiter);
		
		// Input read loop
		while (inputAScanner.hasNext()) {

			// Check for interrupt signal
			if (Thread.interrupted()) {
				inputAScanner.close();
				this.closeAllOutputs();
				throw new InterruptedException("Thread has been interrupted.");
			}
			// Determine next line
			String line = inputAScanner.next();
			
			// Skip lines starting with hash signs.
			if (line.startsWith("#"))
				continue;
			
			// Explode the line into separate elements
			String[] lineElements = line.split("\\s+");
			
			// Determine whether the line has the right format -- if not, abort
			if (lineElements.length != 3){
				inputAScanner.close();
				this.closeAllOutputs();
				throw new Exception("Invalid SHUS input line: "+line);
			}
			
			// hmmm ...
			// Maxlen = currpos.shulen
			// tatsaechlicheLaenge = wie oft (naechstezeile.shulen == currpos.shulen-1) ist
			// wenn tatsaechlichelaenge >= maxlen/2, dann wird der aktuelle shustr ausgewaehlt 
			
			
			// Write to outputs
			this.getOutputPorts()
					.get(ID_OUTPUT)
					.outputToAllCharPipes(line.concat(outputdelimiter));
		}

		/*
		 * Close input scanner. NOTE: A module should not attempt to close its
		 * inputs before the module providing it has done so itself! Please
		 * either leave open any readers that would close the underlying
		 * this.getInputPorts().get().getInputReader() (or getInputStream()
		 * respectively) or only do so after you can be sure that the providing
		 * module has already closed them (like in this instance).
		 */
		inputAScanner.close();
		
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
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}
