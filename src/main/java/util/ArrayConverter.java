package util;

import org.apache.commons.lang3.ArrayUtils;

public class ArrayConverter {

	public static double[] fromObject(Double[] array) {
		double[] values = ArrayUtils.toPrimitive(array);
		return values;
	}

	public static Double[] fromPrimitive(double[] array) {
		Double[] values = ArrayUtils.toObject(array);
		return values;
	}
}
