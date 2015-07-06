package suffixTreeClustering.svd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import suffixTreeClustering.data.Type;
import suffixTreeClustering.features.FeatureVector;
import suffixTreeClustering.st_interface.SuffixTreeInfo;
import util.ArrayConverter;

import com.aliasi.matrix.SvdMatrix;

public class DimensionReducer {

	public static List<Type> reduce(SuffixTreeInfo corpus) {
		List<Type> toReturn = new ArrayList<Type>(corpus.getTypes());

		double[][] matrix = createMatrix(corpus);

		double[][] docVectors = svd(matrix, toReturn);

		for (int i = 0; i < docVectors.length; i++) {
			double[] typeVector = docVectors[i];
			toReturn.get(i).setFeatureVector(new FeatureVector(ArrayConverter.fromPrimitive(typeVector)));
			/*
			 * so einfach kann das eigentlich nicht sein, man muss ja sehen,
			 * dass die ursprünglichen Types die richtigen Vektoren zugewiesen
			 * bekommen. Das wird aber schwierig, weil die Types ursprünglich
			 * ein Set sind. Lässt sich das noch ändern?
			 */
		}

		/*
		 * TODOs: * create matrix from type vectors * give matrix zu svd
		 * algorithm * get back the term vectors * set the type's vectors
		 */

		return toReturn;
	}

	private static double[][] svd(double[][] matrix, List<Type> types) {
		// TODO: determine factor k

		int maxFactors = 200; // number of factors = latent semantic dimensions
		double featureInit = 0.01;
		double initialLearningRate = 0.005;
		int annealingRate = 1000;
		double regularization = 0.00;
		double minImprovement = 0.0000;
		int minEpochs = 10;
		int maxEpochs = 50000;

		SvdMatrix svd = SvdMatrix.svd(matrix, maxFactors, featureInit,
				initialLearningRate, annealingRate, regularization, null,
				minImprovement, minEpochs, maxEpochs);

		double[] scales = svd.singularValues(); // =Sigma
		double[][] termVectors = svd.leftSingularVectors(); // =U
		double[][] docVectors = svd.rightSingularVectors(); // =V

		return termVectors;
	}

	private static double[][] createMatrix(SuffixTreeInfo corpus) {
		double[][] matrix = new double[corpus.getNumberOfTypes()][corpus
				.getNumberOfNodes()];

		int row = 0;
		for (Type type : corpus.getTypes()) {
			FeatureVector vector = type.getVector();
			double[] values = ArrayUtils.toPrimitive(vector.getValues());
			matrix[row] = values;

		}
		return matrix;
	}

}
