package modules.suffixTree.suffixTree.applications;

import modules.suffixTree.suffixTree.SuffixTree;

public class TreeWalker {
	
	// overwrite derfault constructor. this does not need to be instantiated
	private TreeWalker() {}

	/**
	 * Walks the tree's nodes recursively (depth-first) and executes the
	 * listener's actions for each node.
	 * 
	 * @param nodeNr
	 *            The start with
	 * @param st
	 *            The SuffixTree to walk on
	 * @param listener
	 *            The listener defining the actions to take on each node
	 */
	public static void walk(int nodeNr, SuffixTree st, ITreeWalkerListener listener) {
		walk(nodeNr, st, listener, 0);
	}

	/**
	 * Walks the tree's nodes recursively (depth-first) and executes the
	 * listener's actions for each node, while also sending the current depth
	 * relative to the start node to the listener
	 * 
	 * @param nodeNr
	 *            The start with
	 * @param st
	 *            The SuffixTree to walk on
	 * @param listener
	 *            The listener defining the actions to take on each node
	 * @param level
	 *            The current depth while walking
	 */
	private static void walk(int nodeNr, SuffixTree st, ITreeWalkerListener listener, int level) {
		listener.entryaction(nodeNr, level);
		for (int childNr : st.nodes[nodeNr].children.values()) {
			walk(childNr, st, listener, level + 1);
		}
		listener.exitaction(nodeNr, level);
	}
}
