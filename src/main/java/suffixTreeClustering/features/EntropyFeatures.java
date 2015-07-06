package suffixTreeClustering.features;

import java.util.ArrayList;
import java.util.List;

import suffixTreeClustering.data.Node;
import suffixTreeClustering.data.Type;
import suffixTreeClustering.st_interface.SuffixTreeInfo;

public class EntropyFeatures {

	private SuffixTreeInfo corpus;
	private Type document;

	public EntropyFeatures(final Type document, final SuffixTreeInfo corpus) {
		if (corpus.getNodes().size() == 0) {
			throw new IllegalArgumentException("Empty Corpus!");
		}
		this.document = document;
		this.corpus = corpus;
	}

	public FeatureVector vector() {
		// Ein Vektor für dieses Dokument ist...
		List<Double> values = new ArrayList<Double>();
		// ...für jeden Term im Vokabular... (=jeder Knoten im SuffixTree)
		List<Node> terms = corpus.getNodes();

		for (Node node : terms) {
			// der Entropiewert des Terms:
			Double entropy = weighting(node);
			values.add(entropy);
		}
		return new FeatureVector(values.toArray(new Double[values.size()]));
	}

	private Double weighting(Node node) {
		Integer tf = node.getTermfrequencyFor(document);
		tf = tf == null ? 0 : tf;

		final int nDocs = corpus.getNumberOfTypes();
		final int gf = node.getFrequency(); // global frequency of term
											// (substring) in document
											// collection

		double sum = 0.0;

		for (Type d : corpus.getTypes()) {
			Integer tf_dn = node.getTermfrequencyFor(d);
			double p = tf_dn / (double) gf;
			double log_p = Math.log10(p);
			double divident = p * log_p;
			double divisor = Math.log10(nDocs);
			sum += (divident / divisor);
		}

		double entropy = 1 - sum;
		double weight = (Math.log(tf) + 1) * (entropy);
		return weight;
	}

}
