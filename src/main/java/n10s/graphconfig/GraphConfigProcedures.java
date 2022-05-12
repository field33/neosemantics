package n10s.graphconfig;

import n10s.graphconfig.GraphConfig.GraphConfigNotFound;
import n10s.graphconfig.GraphConfig.InvalidParamException;
import n10s.result.GraphConfigItemResult;
import org.apache.commons.collections.iterators.IteratorChain;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class GraphConfigProcedures {

  @Context
  public GraphDatabaseService db;

  @Context
  public Transaction tx;

  @Context
  public Log log;

  @Procedure(mode = Mode.WRITE)
  @Description("Initialises the config that drives the behavior of the graph")
  public Stream<GraphConfigItemResult> init(
      @Name(value = "params", defaultValue = "{}") Map<String, Object> props)
      throws GraphConfigException {
    //create or overwite
    if (graphIsEmpty()) {
      try {
        GraphConfig currentGraphConfig = new GraphConfig(props);
        Map<String, Object> queryParams = new HashMap<>();
        Map config = currentGraphConfig.serialiseConfig();
        Map nodeProps = (Map<String, Object>) config.remove("_forciblyAssignedOnImportNodeProperties");

        queryParams.put("props", config);
        queryParams.put("nodeProps", nodeProps);

        tx.execute("MERGE (gc:_GraphConfig)-[:HAS]-(np:_ForciblyAssignedOnImportNodeProperties) SET gc += $props, np += $nodeProps", queryParams);
        return currentGraphConfig.getAsGraphConfigResults().stream();
      } catch (InvalidParamException ipe) {
        throw new GraphConfigException(ipe.getMessage());
      }
    } else {
      throw new GraphConfigException("The graph is non-empty. Config cannot be changed.");
    }
  }

  @Procedure(mode = Mode.WRITE)
  @Description("sets specific params to the config that drives the behavior of the graph")
  public Stream<GraphConfigItemResult> set(
      @Name(value = "params", defaultValue = "{}") Map<String, Object> props)
      throws GraphConfigException {
    //update
    //TODO: identify config changes that are acceptable (additive) when graph is not empty?
    if ((props.containsKey("force") && props.get("force").equals(Boolean.TRUE)) ||graphIsEmpty()) {
      GraphConfig currentGraphConfig;
      try {
        try {
          currentGraphConfig = new GraphConfig(tx);
          currentGraphConfig.add(props);
        } catch (GraphConfigNotFound e) {
          throw new GraphConfigException("Graph config not found. Call 'init' method first.");
        }
      } catch (InvalidParamException ipe) {
        throw new GraphConfigException(ipe.getMessage());
      }
      Map<String, Object> queryParams = new HashMap<>();
      Map config = currentGraphConfig.serialiseConfig();
      Map nodeProps = (Map<String, Object>) config.remove("_forciblyAssignedOnImportNodeProperties");

      queryParams.put("props", config);
      queryParams.put("nodeProps", nodeProps);

      tx.execute("MERGE (gc:_GraphConfig)-[:HAS]-(np:_ForciblyAssignedOnImportNodeProperties) SET gc += $props, np += $nodeProps", queryParams);
      return currentGraphConfig.getAsGraphConfigResults().stream();
    } else {
      throw new GraphConfigException("The graph is non-empty. Config cannot be changed.");
    }
  }

  @Procedure(mode = Mode.READ)
  @Description("Shows the current graph config")
  public Stream<GraphConfigItemResult> show() throws GraphConfigException {
    try {
      return new GraphConfig(tx).getAsGraphConfigResults().stream();
    } catch (GraphConfigNotFound e) {
      return Stream.empty();
    }
  }

  @Procedure(mode = Mode.WRITE)
  @Description("removes the current graph config")
  public Stream<GraphConfigItemResult> drop() throws GraphConfigException {
    if (!graphIsEmpty()) {
      throw new GraphConfigException("The graph is non-empty. Config cannot be changed.");
    }

    ArrayList<String> labels = new ArrayList();
    labels.add("_GraphConfig");
    labels.add("_ForciblyAssignedOnImportNodeProperties");

    IteratorChain chain = labels.stream()
      .map((label) -> tx.findNodes(Label.label(label)))
      .reduce(
        new IteratorChain(),
        (iteratorChain, nodesIterator) -> {
          iteratorChain.addIterator(nodesIterator);
          return iteratorChain;
        },
        (iteratorChain1, iteratorChain2) -> {
          iteratorChain1.addIterator(iteratorChain2);
          return iteratorChain1;
        });

    while (chain.hasNext()) {
      ((Node) chain.next()).delete();
    }

    return Stream.empty();

  }

  private boolean graphIsEmpty() {
    return !tx.execute("match (r:Resource) return id(r) limit 1").hasNext();
  }

  private class GraphConfigException extends Throwable {

    public GraphConfigException(String msg) {
      super(msg);
    }
  }
}
