package work.lclpnet.gaco.ds;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface UndirectedGraphNode<T extends UndirectedGraphNode<T>> {

    @NotNull
    List<T> neighbours();
}
