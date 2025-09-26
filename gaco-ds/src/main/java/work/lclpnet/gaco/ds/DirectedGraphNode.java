package work.lclpnet.gaco.ds;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DirectedGraphNode<T extends DirectedGraphNode<T>> {

    @NotNull
    List<T> children();
}
