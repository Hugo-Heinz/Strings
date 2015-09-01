package module;

import java.util.Properties;

import modularization.CharPipe;
import modularization.ModuleImpl;
import module.common.SymbolBewerter;
import parallelization.CallbackReceiver;
import treeBuilder.Knoten;

public class OnlineParadigmenErmittlerModul extends ModuleImpl {

	// Property keys
	public static final String PROPERTYKEY_DIVIDER = "Token divider";
	public static final String PROPERTYKEY_MINDESTKOSTENPROEBENE = "Minimal cost";
	public static final String PROPERTYKEY_BEWERTUNGSABFALLFAKTOR = "Bewertungsabfallfaktor";
	public static final String PROPERTYKEY_BEWERTUNGAUSGEBEN = "Bewertung mit in Ausgabe schreiben";

	// Local variables
	private String divider = "\t";
	private double mindestKostenProSymbolEbene;
	private double bewertungsAbfallFaktor;
	private boolean bewertungAusgeben = false;

	public OnlineParadigmenErmittlerModul(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {
		super(callbackReceiver, properties);

		// define I/O
		this.getSupportedInputs().add(CharPipe.class);
		this.getSupportedOutputs().add(CharPipe.class);

		// Add description for properties
		this.getPropertyDescriptions().put(PROPERTYKEY_DIVIDER,
				"Divider that is inserted in between the tokens on output");
		this.getPropertyDescriptions().put(PROPERTYKEY_BEWERTUNGSABFALLFAKTOR,
				"Faktor zur Gewichtung eines Abfalls der Bewertung von einem auf das naechste Symbol [double, >0, 1=neutral]");
		this.getPropertyDescriptions().put(PROPERTYKEY_BEWERTUNGAUSGEBEN,
				"Uebergangsbewertungen mit in die Ausgabe schreiben");

		// Add default values
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME,
				"ParadigmSegmenterModule");
		this.getPropertyDefaultValues().put(PROPERTYKEY_DIVIDER, "\t");
		this.getPropertyDefaultValues().put(PROPERTYKEY_MINDESTKOSTENPROEBENE, "1");
		this.getPropertyDefaultValues().put(PROPERTYKEY_BEWERTUNGSABFALLFAKTOR, "1");
		this.getPropertyDefaultValues().put(PROPERTYKEY_BEWERTUNGAUSGEBEN, "false");

		// Add module description
		this.setDescription("Liest Text ein und segmentiert diesen anhand der ermittelten paradigmatischen Grenzen.");
	}

	@Override
	public boolean process() throws Exception {

		/*
		 * Segmentierung des Eingabedatenstroms
		 */
		
		// Symbolbewerter instanziieren
		SymbolBewerter symbolBewerter = new SymbolBewerter(this.mindestKostenProSymbolEbene, this.bewertungsAbfallFaktor);
		
		// Erstes Zeichen einlesen
		int zeichenCode = this.getInputCharPipe().getInput().read();
		
		// Suffix-Trie Wurzelknoten
		Knoten wurzelKnoten = new Knoten("^");
		
		// Variable fuer aktuellen Knoten im Suffix-Trie
		Knoten aktuellerKnoten = wurzelKnoten;
		
		// Variable fuer letzte Bewertung
		double letzteBewertung = Double.MAX_VALUE;
		
		// Daten Zeichen fuer Zeichen einlesen
		while (zeichenCode != -1) {

			// Check for interrupt signal
			if (Thread.interrupted()) {
				this.closeAllOutputs();
				throw new InterruptedException("Thread has been interrupted.");
			}

			// Zeichen einlesen
			Character symbol = Character.valueOf((char) zeichenCode);
			
			/*
			 *  Ermitteln, ob das Zeichen dem aktuellen Muster folgt, oder es eher ein neues ist.
			 */
			
			// Baum weiter bauen
			
			// Kindknoten existiert nicht
			if (!aktuellerKnoten.getKinder().containsKey(symbol.toString())){
				Knoten kindKnoten = new Knoten(symbol.toString());
				kindKnoten.inkZaehler();
				aktuellerKnoten.getKinder().put(symbol.toString(), kindKnoten);
				this.outputToAllCharPipes(symbol.toString()+this.divider);
				letzteBewertung = Double.MAX_VALUE;
				aktuellerKnoten = wurzelKnoten;
			} else {
				// Bewertungen ermitteln
				double bewertungTrennen = symbolBewerter.symbolBewerten(symbol, wurzelKnoten, Double.MAX_VALUE);
				double bewertungVerbinden = symbolBewerter.symbolBewerten(symbol, aktuellerKnoten, letzteBewertung);
				// Wenn die Bewertung der Verbindungsfortsetzung besser (== kleiner) ist, als die der Trennung, wird fortgesetzt.
				if (bewertungTrennen > bewertungVerbinden){
					
				} else {
					
				}
				
			}
			
			
			
			
			
			
			
			// Read next char
			zeichenCode = this.getInputCharPipe().getInput().read();
		}
		
		// Close relevant I/O instances
		this.closeAllOutputs();

		// Success
		return true;
	}

	@Override
	public void applyProperties() throws Exception {
		
		if (this.getProperties().containsKey(PROPERTYKEY_DIVIDER))
			this.divider = this.getProperties().getProperty(PROPERTYKEY_DIVIDER);
		else if (this.getPropertyDefaultValues() != null && this.getPropertyDefaultValues().containsKey(PROPERTYKEY_DIVIDER))
				this.divider = this.getPropertyDefaultValues().get(PROPERTYKEY_DIVIDER);
		
		if (this.getProperties().containsKey(PROPERTYKEY_MINDESTKOSTENPROEBENE))
			this.mindestKostenProSymbolEbene = Double.parseDouble(this.getProperties().getProperty(PROPERTYKEY_MINDESTKOSTENPROEBENE));
		else if (this.getPropertyDefaultValues() != null && this.getPropertyDefaultValues().containsKey(PROPERTYKEY_MINDESTKOSTENPROEBENE))
			this.mindestKostenProSymbolEbene = Double.parseDouble(this.getPropertyDefaultValues().get(PROPERTYKEY_MINDESTKOSTENPROEBENE));
	
		if (this.getProperties().containsKey(PROPERTYKEY_BEWERTUNGSABFALLFAKTOR))
			this.bewertungsAbfallFaktor = Double.parseDouble(this.getProperties().getProperty(PROPERTYKEY_BEWERTUNGSABFALLFAKTOR));
		else if (this.getPropertyDefaultValues() != null && this.getPropertyDefaultValues().containsKey(PROPERTYKEY_BEWERTUNGSABFALLFAKTOR))
			this.bewertungsAbfallFaktor = Double.parseDouble(this.getPropertyDefaultValues().get(PROPERTYKEY_BEWERTUNGSABFALLFAKTOR));
		
		if (this.getProperties().containsKey(PROPERTYKEY_BEWERTUNGAUSGEBEN))
			this.bewertungAusgeben = Boolean.parseBoolean(this.getProperties().getProperty(PROPERTYKEY_BEWERTUNGAUSGEBEN));
			
		super.applyProperties();
	}

}
