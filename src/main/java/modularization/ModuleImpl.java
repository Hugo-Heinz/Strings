package modularization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import parallelization.CallbackReceiver;

public abstract class ModuleImpl implements Module {

	public static final String PROPERTYKEY_NAME = "name";
	private BytePipe byteInput = null;
	private List<BytePipe> byteOutputs = new ArrayList<BytePipe>();
	private CallbackReceiver callbackReceiver;
	private CharPipe charInput = null;
	private List<CharPipe> charOutputs = new ArrayList<CharPipe>();
	private String name;
	private Properties properties = new Properties();
	private Map<String, String> propertyDescriptions = new HashMap<String, String>();
	private Map<String, String> propertyDefaultValues = new HashMap<String, String>();
	private int status = Module.STATUSCODE_NOTYETRUN;
	private List<Class<?>> supportedInputs = new ArrayList<Class<?>>();
	private List<Class<?>> supportedOutputs = new ArrayList<Class<?>>();
	private String description = "(no description)";
	private int percentComplete = 0;

	public ModuleImpl(CallbackReceiver callbackReceiver, Properties properties)
			throws Exception {
		super();
		this.callbackReceiver = callbackReceiver;
		this.setProperties(properties);
		this.getPropertyDescriptions().put(PROPERTYKEY_NAME,
				"The module instance's name");
		// Add default values
		this.getPropertyDefaultValues().put(PROPERTYKEY_NAME, "(unnamed module)");
	}

	@Override
	public boolean addOutputPipe(Pipe pipe) throws NotSupportedException {
		if (!this.supportsOutputPipe(pipe))
			throw new NotSupportedException("Excuse me, but this module cannot output to that pipe.");
		if (pipe.getClass().equals(BytePipe.class))
			this.byteOutputs.add((BytePipe) pipe);
		else if (pipe.getClass().equals(CharPipe.class))
			this.charOutputs.add((CharPipe) pipe);
		else
			throw new NotSupportedException("Excuse me, but I do not recognize the type of that pipe.");
		return true;
	}

	@Override
	public void applyProperties() throws Exception {
		if (this.getProperties().containsKey(PROPERTYKEY_NAME))
			this.name = this.getProperties().getProperty(PROPERTYKEY_NAME, "unnamed module");
	}

	public void closeAllOutputs() throws IOException {
		this.closeAllOutputStreams();
		this.closeAllOutputWriters();
	}

	/**
	 * Closes all output streams.
	 * @throws IOException If an I/O error occurs
	 */
	public void closeAllOutputStreams() throws IOException {

		// Loop over the defined outputs
		Iterator<BytePipe> outputStreams = this.byteOutputs.iterator();
		while (outputStreams.hasNext()) {

			// Close output
			outputStreams.next().writeClose();
		}
	}

	/**
	 * Closes all output writers.
	 * @throws IOException If an I/O error occurs
	 */
	public void closeAllOutputWriters() throws IOException {

		// Loop over the defined outputs
		Iterator<CharPipe> outputWriters = this.charOutputs.iterator();
		while (outputWriters.hasNext()) {

			// Close output
			outputWriters.next().writeClose();
		}
	}

	/*
	 * @see parallelization.CallbackProcess#getRueckmeldungsEmpfaenger()
	 */
	@Override
	public CallbackReceiver getCallbackReceiver() {
		return callbackReceiver;
	}

	@Override
	public BytePipe getInputBytePipe() {
		return this.byteInput;
	}

	@Override
	public CharPipe getInputCharPipe() {
		return this.charInput;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public List<BytePipe> getOutputBytePipes() {
		return this.byteOutputs;
	}
	

	
	@Override
	public List<CharPipe> getOutputCharPipes() {
		return this.charOutputs;
	}
	
	@Override
	public List<Pipe> getOutputPipes() {
		// Concatenate both output pipe lists and return the result
		List<Pipe> pipes = new ArrayList<Pipe>();
		pipes.addAll(charOutputs);
		pipes.addAll(byteOutputs);
		return pipes;
	}
	
	@Override
	public Properties getProperties() {
		return properties;
	}
	
	@Override
	public Map<String, String> getPropertyDescriptions() {
		return propertyDescriptions;
	}
	
	@Override
	public Map<String, String> getPropertyDefaultValues() {
		return propertyDefaultValues;
	}
	
	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public List<Class<?>> getSupportedInputs(){
		return this.supportedInputs;
	}

	@Override
	public List<Class<?>> getSupportedOutputs(){
		return this.supportedOutputs;
	}

	/**
	 * Writes the given byte array to all outputs.
	 * @param data Data to write
	 * @throws IOException Thrown if an I/O problem occurs
	 */
	public void outputToAllBytePipes(byte[] data) throws IOException {
		this.outputToAllBytePipes(data, 0, data.length);
	}

	/**
	 * Writes the given byte array to all stream outputs.
	 * @param data Data to write
	 * @param offset The start offset in the data
	 * @param bytesToWrite The number of bytes to write
	 * @throws IOException Thrown if an I/O problem occurs
	 */
	public void outputToAllBytePipes(byte[] data, int offset, int bytesToWrite) throws IOException {
		// Loop over the defined outputs
		Iterator<BytePipe> outputStreams = this.byteOutputs.iterator();
		while (outputStreams.hasNext()) {

			// Determine the next output on the list
			BytePipe outputStream = outputStreams.next();

			// Write file list JSON to output
			outputStream.write(data, offset, bytesToWrite);
		}
	}

	/**
	 * Writes the given data to all char output pipes.
	 * @param data Data to write
	 * @param offset The start offset in the data
	 * @param charsToWrite The number of chars to write
	 * @throws IOException Thrown if an I/O problem occurs
	 */
	public void outputToAllCharPipes(char[] data, int offset, int charsToWrite) throws IOException {
		// Loop over the defined outputs
		Iterator<CharPipe> outputPipes = this.charOutputs.iterator();
		while (outputPipes.hasNext()) {

			// Determine the next output on the list
			CharPipe outputPipe = outputPipes.next();

			// Write file list JSON to output
			outputPipe.write(data, offset, charsToWrite);
		}
	}

	/**
	 * Writes the given String to all char output pipes.
	 * @param data Data to write
	 * @throws IOException Thrown if an I/O problem occurs
	 */
	public void outputToAllCharPipes(String data) throws IOException {
		this.outputToAllCharPipes(data.toCharArray(), 0, data.length());
	}

	@Override
	public abstract boolean process() throws Exception;

	@Override
	public boolean removeOutputPipe(Pipe pipe) {
		if (pipe.getClass().equals(BytePipe.class))
			return this.byteOutputs.remove((BytePipe) pipe);
		else if (pipe.getClass().equals(CharPipe.class))
			return this.charOutputs.remove((CharPipe) pipe);
		return false;
	}

	/*
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		try {
			// Check whether expected inputs are present (and fail if they are not)
			if (!this.getSupportedInputs().isEmpty()
					&& (this.getInputCharPipe() == null || this.getInputCharPipe().getInput() == null)
					&& (this.getInputBytePipe() == null || this.getInputBytePipe().getInput() == null))
				throw new Exception("There does not seem to be any input for me.");
			
			// Update status
			this.status = Module.STATUSCODE_RUNNING;
			this.percentComplete = 0;

			// Log message
			Logger.getLogger("").log(
					Level.INFO,
					"Running module "
							+ this.getProperties().getProperty(
									ModuleImpl.PROPERTYKEY_NAME));

			// Run process and determine result
			Boolean result = this.process();

			// Log message
			Logger.getLogger("")
					.log(Level.INFO,
							"Module "
									+ this.getProperties().getProperty(
											ModuleImpl.PROPERTYKEY_NAME)
									+ " finished.");

			// Update status
			if (result){
				this.status = Module.STATUSCODE_SUCCESS;
				this.percentComplete = 100;
			}
			else
				this.status = Module.STATUSCODE_FAILURE;

			// Return result
			this.callbackReceiver.receiveCallback(Thread.currentThread(), result);

		} catch (Exception e) {
			this.status = Module.STATUSCODE_FAILURE;
			this.callbackReceiver.receiveException(Thread.currentThread(), e);
		}
	}

	/*
	 * @see
	 * parallelization.CallbackProcess#setRueckmeldungsEmpfaenger(parallelization
	 * .CallbackReceiver)
	 */
	@Override
	public void setCallbackReceiver(CallbackReceiver callbackReceiver) {
		this.callbackReceiver = callbackReceiver;
	}

	@Override
	public void setInputBytePipe(BytePipe pipe) throws NotSupportedException {
		if (!this.supportsInputPipe(pipe))
			throw new NotSupportedException("Excuse me, but this module cannot take its input from that pipe.");
		this.byteInput = pipe;
	}

	@Override
	public void setInputCharPipe(CharPipe pipe) throws NotSupportedException {
		if (!this.supportsInputPipe(pipe))
			throw new NotSupportedException("Excuse me, but this module cannot take its input from that pipe.");
		this.charInput = pipe;
	}

	@Override
	public void setInputPipe(Pipe pipe) throws NotSupportedException {
		if (pipe.getClass().equals(BytePipe.class))
			this.setInputBytePipe((BytePipe) pipe);
		else if (pipe.getClass().equals(CharPipe.class))
			this.setInputCharPipe((CharPipe) pipe);
		else
			throw new NotSupportedException("Excuse me, but I do not recognize the type of that pipe.");
	}

	@Override
	public void setName(String name) {
		this.name = name;
		if (this.name != null)
			this.getProperties().setProperty(PROPERTYKEY_NAME, name);
		else
			this.getProperties().remove(PROPERTYKEY_NAME);
	}

	@Override
	public void setProperties(Properties properties) throws Exception {
		if (properties == null)
			throw new Exception(this.getClass().getSimpleName()
					+ " cannot handle null value as properties, sorry.");
		this.properties = properties;
		this.applyProperties();
	}

	@Override
	public boolean supportsInputPipe(Pipe pipe) {
		if (this.supportedInputs.contains(pipe.getClass()))
			return true;
		return false;
	}

	@Override
	public boolean supportsOutputPipe(Pipe pipe) {
		if (this.supportedOutputs.contains(pipe.getClass()))
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see modularization.Module#resetOutputs()
	 */
	@Override
	public void resetOutputs() throws IOException {
		
		// Cycle through all output pipes & reset them
		Iterator<Pipe> pipes = this.getOutputPipes().iterator();
		while (pipes.hasNext()){
			pipes.next().reset();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see modularization.Module#getDescription()
	 */
	@Override
	public String getDescription() {
		return this.description;
	}

	/* (non-Javadoc)
	 * @see modularization.Module#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String desc) {
		this.description = desc;
	}

	/* (non-Javadoc)
	 * @see modularization.Module#getPercentComplete()
	 */
	@Override
	public int getPercentComplete() {
		return percentComplete;
	}

	/**
	 * @param percentComplete the percentComplete to set
	 */
	public void setPercentComplete(int percentComplete) {
		this.percentComplete = Math.max(0,Math.min(percentComplete,100));
	}

}
