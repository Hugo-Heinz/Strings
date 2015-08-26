package module.common;

import treeBuilder.Knoten;

public class SymbolBewerter {
	
	private double bewertungsAbfallFaktor;
	private boolean letzteBewertungMitEinbeziehen;

	public SymbolBewerter(double bewertungsAbfallFaktor, boolean letzteBewertungMitEinbeziehen) {
		super();
		this.bewertungsAbfallFaktor = bewertungsAbfallFaktor;
		this.letzteBewertungMitEinbeziehen = letzteBewertungMitEinbeziehen;
	}

	/**
	 * @return the bewertungsAbfallFaktor
	 */
	public double getBewertungsAbfallFaktor() {
		return bewertungsAbfallFaktor;
	}

	/**
	 * @param bewertungsAbfallFaktor the bewertungsAbfallFaktor to set
	 */
	public void setBewertungsAbfallFaktor(double bewertungsAbfallFaktor) {
		this.bewertungsAbfallFaktor = bewertungsAbfallFaktor;
	}

	/**
	 * Bewertet ein einzelnes Symbol (im Sinne, dass die Uebergangshuerde vom Elternknoten aus errechnet wird).
	 * @param symbol Symbol (Zeichenkette mit Laenge eins)
	 * @param elternKnoten Elternknoten im Suffixbaum
	 * @return bewertung (kleiner bedeutet geringere Uebergangshuerde)
	 */
	public double symbolBewerten(Character symbol, Knoten elternKnoten, double letzteBewertung){
		// Variable fuer das Gesamtergebnis, Standardwert ist die maximal moegliche Huerde
		double bewertung = 0d; //Double.MAX_VALUE;
		
		// Pruefen, ob der aktuelle Knoten des Suffixbaumes unter dem aktuellen Symbol der Zeichenkette einen Kindknoten fuehrt.
		if (symbol != null && elternKnoten != null && elternKnoten.getKinder().containsKey(symbol.toString())){
			
			// Knoten ermitteln
			Knoten kindKnoten = elternKnoten.getKinder().get(symbol.toString());
			
			// Ermitteln, welchen Wert der aktuelle Knoten hat
			int gesamtwert = elternKnoten.getZaehler();
			
			// Ermitteln, welchen Wert der Kindknoten hat
			int teilwert = kindKnoten.getZaehler();
			
			// Anteil des Kindknotenzaehlers am Zaehler seines Elternknoten ermitteln
			double anteil = new Double(teilwert)/new Double(gesamtwert); // 0 < anteil <= 1
			
			// Bewertung fuer diesen Kindknoten
			bewertung = anteil;
			
			// Ggf. Abfall in der Bewertung miteinbeziehen (deutet auf paradigmatische Grenze hin)
			if (this.letzteBewertungMitEinbeziehen && letzteBewertung>bewertung)
				bewertung = bewertung/(letzteBewertung * this.bewertungsAbfallFaktor);
			
		}
		
		// Ergebnis zurueckgeben
		return bewertung;
	}
}
