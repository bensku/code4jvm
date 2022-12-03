package fi.benjami.code4jvm.internal.node;

import fi.benjami.code4jvm.internal.DebugNames;

public sealed interface Node permits CodeNode, EdgeNode, StoreNode, VarMarkerNode {

	String toString(DebugNames.Counting debugNameGen);
}
