package net.miscfolder.bojiti.parser.jsoup;

import net.miscfolder.bojiti.parser.ParserException;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Node;

public class MysteryDataNodeException extends ParserException{
	private final DataNode node;
	private final Node parent;

	MysteryDataNodeException(DataNode node, Node parent){
		super("Data node not expected type!  " +
				(parent == null ? "No parent node." : "Parent type: " + parent.nodeName()) +
				"\n\tNode content: " + node.getWholeData() +
				(parent != null ? "\n\tParent content: " + parent.outerHtml() : ""));
		this.node = node;
		this.parent = parent;
	}

	public DataNode getNode(){
		return node;
	}

	public Node getParent(){
		return parent;
	}
}
